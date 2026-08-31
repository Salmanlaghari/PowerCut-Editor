package com.powercut.editor.domain.ai

import android.opengl.GLES20
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

/**
 * Main orchestrator for the real-time GPU-accelerated AI filter pipeline.
 * Chains: Beauty -> Segmentation -> Color Grading -> Audio-Reactive.
 * All processing runs on GL thread for 60fps performance.
 */
class AIFilterPipeline {
    private val _pipelineState = MutableStateFlow(AIFilterPipelineState())
    val pipelineState: StateFlow<AIFilterPipelineState> = _pipelineState.asStateFlow()

    val faceTracking = FaceTrackingEngine()
    val beautyFilter = BeautyFilter()
    val depthSegmentation = DepthSegmentation()
    val colorGrading = ColorGradingPipeline()
    val audioReactive = AudioReactiveEngine()

    private var compositeProgram: Int = 0
    private var isInitialized = false

    fun initialize() {
        if (isInitialized) return
        compositeProgram = ShaderProgram.compileProgram(
            "attribute vec4 a_position; attribute vec2 a_texCoord; varying vec2 v_texCoord; void main(){gl_Position=a_position;v_texCoord=a_texCoord;}",
            "precision mediump float; uniform sampler2D u_texture; varying vec2 v_texCoord; void main(){gl_FragColor=texture2D(u_texture,v_texCoord);}"
        )
        faceTracking.initialize()
        beautyFilter.initialize()
        depthSegmentation.initialize()
        colorGrading.initialize()
        audioReactive.initialize()
        isInitialized = true
    }

    suspend fun processFrame(
        inputTextureId: Int, timestampMs: Long, width: Int, height: Int, config: AIFilterConfig
    ): Int = withContext(Dispatchers.Default) {
        if (!isInitialized) initialize()
        var current = inputTextureId

        val landmarks = if (config.beautyEnabled || config.segmentationEnabled)
            faceTracking.detectLandmarks(inputTextureId, width, height) else null

        if (config.beautyEnabled && landmarks != null)
            current = beautyFilter.apply(current, landmarks, config.smoothIntensity, config.brightenIntensity, config.reshapeIntensity, width, height)

        if (config.segmentationEnabled)
            current = depthSegmentation.apply(current, landmarks, config.backgroundBlurIntensity, config.neonGlowEnabled, config.neonGlowColor, config.neonGlowIntensity, width, height)

        if (config.colorGradingEnabled)
            current = colorGrading.apply(current, config.colorGradingPreset, config.colorGradingIntensity, config.customLutTextureId, width, height)

        if (config.audioReactiveEnabled)
            current = audioReactive.apply(current, timestampMs, config.audioPulseIntensity, config.audioGlowIntensity, width, height)

        _pipelineState.value = AIFilterPipelineState(config.getActiveFilterNames(), landmarks != null, true)
        current
    }

    fun release() {
        if (!isInitialized) return
        GLES20.glDeleteProgram(compositeProgram)
        faceTracking.release(); beautyFilter.release(); depthSegmentation.release()
        colorGrading.release(); audioReactive.release()
        isInitialized = false
    }
}

data class AIFilterConfig(
    val beautyEnabled: Boolean = false, val smoothIntensity: Float = 0f, val brightenIntensity: Float = 0f, val reshapeIntensity: Float = 0f,
    val segmentationEnabled: Boolean = false, val backgroundBlurIntensity: Float = 0f,
    val neonGlowEnabled: Boolean = false, val neonGlowColor: FloatArray = floatArrayOf(0f, 1f, 0.8f, 1f), val neonGlowIntensity: Float = 0f,
    val colorGradingEnabled: Boolean = false, val colorGradingPreset: ColorGradingPreset = ColorGradingPreset.NONE,
    val colorGradingIntensity: Float = 1f, val customLutTextureId: Int = -1,
    val audioReactiveEnabled: Boolean = false, val audioPulseIntensity: Float = 0f, val audioGlowIntensity: Float = 0f
) {
    fun getActiveFilterNames(): List<String> {
        val n = mutableListOf<String>()
        if (beautyEnabled) n.add("AI Beauty")
        if (segmentationEnabled) { n.add("Background Blur"); if (neonGlowEnabled) n.add("Neon Outline") }
        if (colorGradingEnabled) n.add(colorGradingPreset.displayName)
        if (audioReactiveEnabled) n.add("Audio Pulse")
        return n
    }
}

data class AIFilterPipelineState(
    val activeFilters: List<String> = emptyList(), val faceDetected: Boolean = false, val frameProcessed: Boolean = false
)
