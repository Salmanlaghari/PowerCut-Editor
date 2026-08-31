package com.powercut.editor.domain.processing

import android.opengl.GLES20
import kotlin.math.abs
import com.powercut.editor.domain.ai.ShaderProgram

/**
 * AI Speed Ramp engine with optical flow frame interpolation.
 * Supports ultra-slow-motion up to 1000 FPS feel with customizable speed curves.
 */
class SpeedRampEngine {
    private var isInitialized = false
    private var interpolationShaderProgram: Int = 0
    private var fboId: Int = 0
    private var fboTextureId: Int = 0

    fun initialize() {
        if (isInitialized) return
        
        interpolationShaderProgram = ShaderProgram.compileProgram(
            VERTEX_SHADER,
            INTERPOLATION_FRAGMENT_SHADER
        )
        
        createFramebuffer()
        isInitialized = true
    }

    /**
     * Apply speed ramp effect to a video clip.
     */
    fun applySpeedRamp(
        currentFrame: Int,
        totalFrames: Int,
        speedCurve: SpeedCurve
    ): SpeedRampResult {
        val progress = currentFrame.toFloat() / totalFrames
        val speed = speedCurve.getSpeedAt(progress)
        val interpolatedFrame = calculateInterpolatedFrame(currentFrame, speed, totalFrames)
        
        return SpeedRampResult(
            outputFrame = interpolatedFrame,
            speedMultiplier = speed,
            shouldInterpolate = abs(speed) < 0.5f || abs(speed) > 2f
        )
    }

    /**
     * Generate frame interpolation for smooth slow-motion.
     */
    fun interpolateFrames(
        frame1: Int,
        frame2: Int,
        t: Float,
        width: Int,
        height: Int
    ): Int {
        if (!isInitialized) return frame1
        
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, fboId)
        GLES20.glViewport(0, 0, width, height)
        
        GLES20.glUseProgram(interpolationShaderProgram)
        
        // Bind frame textures
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, frame1)
        GLES20.glUniform1i(GLES20.glGetUniformLocation(interpolationShaderProgram, "u_frame1"), 0)
        
        GLES20.glActiveTexture(GLES20.GL_TEXTURE1)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, frame2)
        GLES20.glUniform1i(GLES20.glGetUniformLocation(interpolationShaderProgram, "u_frame2"), 1)
        
        GLES20.glUniform1f(GLES20.glGetUniformLocation(interpolationShaderProgram, "u_t"), t)
        GLES20.glUniform2f(
            GLES20.glGetUniformLocation(interpolationShaderProgram, "u_resolution"),
            width.toFloat(), height.toFloat()
        )
        
        drawQuad()
        
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0)
        
        return fboTextureId
    }

    /**
     * Calculate optical flow between two frames.
     */
    fun calculateOpticalFlow(
        frame1: Int,
        frame2: Int,
        width: Int,
        height: Int
    ): OpticalFlowField {
        // Simplified optical flow calculation
        // In production, this would use GPU compute shaders
        return OpticalFlowField(
            width = width,
            height = height,
            flowX = FloatArray(width * height) { 0f },
            flowY = FloatArray(width * height) { 0f }
        )
    }

    fun release() {
        if (!isInitialized) return
        GLES20.glDeleteProgram(interpolationShaderProgram)
        GLES20.glDeleteFramebuffers(1, intArrayOf(fboId), 0)
        GLES20.glDeleteTextures(1, intArrayOf(fboTextureId), 0)
        isInitialized = false
    }

    private fun calculateInterpolatedFrame(currentFrame: Int, speed: Float, totalFrames: Int): Int {
        val targetFrame = (currentFrame * speed).toInt().coerceIn(0, totalFrames - 1)
        return targetFrame
    }

    private fun createFramebuffer() {
        val fb = IntArray(1)
        val tx = IntArray(1)
        GLES20.glGenFramebuffers(1, fb, 0)
        GLES20.glGenTextures(1, tx, 0)
        
        fboId = fb[0]
        fboTextureId = tx[0]
        
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, fboTextureId)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexImage2D(
            GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA,
            1920, 1080, 0,
            GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, null
        )
        
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, fboId)
        GLES20.glFramebufferTexture2D(
            GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0,
            GLES20.GL_TEXTURE_2D, fboTextureId, 0
        )
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0)
    }

    private fun drawQuad() {
        val vertices = floatArrayOf(
            -1f, -1f, 0f, 1f,
             1f, -1f, 1f, 1f,
            -1f,  1f, 0f, 0f,
             1f,  1f, 1f, 0f
        )
        
        val vertexBuffer = java.nio.ByteBuffer.allocateDirect(64)
            .order(java.nio.ByteOrder.nativeOrder())
            .asFloatBuffer()
            .apply {
                put(vertices)
                position(0)
            }
        
        val posLoc = GLES20.glGetAttribLocation(interpolationShaderProgram, "a_position")
        val texLoc = GLES20.glGetAttribLocation(interpolationShaderProgram, "a_texCoord")
        
        GLES20.glEnableVertexAttribArray(posLoc)
        GLES20.glVertexAttribPointer(posLoc, 2, GLES20.GL_FLOAT, false, 16, vertexBuffer)
        
        vertexBuffer.position(2)
        GLES20.glEnableVertexAttribArray(texLoc)
        GLES20.glVertexAttribPointer(texLoc, 2, GLES20.GL_FLOAT, false, 16, vertexBuffer)
        
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)
        
        GLES20.glDisableVertexAttribArray(posLoc)
        GLES20.glDisableVertexAttribArray(texLoc)
    }

    companion object {
        private const val VERTEX_SHADER = """
            attribute vec4 a_position;
            attribute vec2 a_texCoord;
            varying vec2 v_texCoord;
            void main() {
                gl_Position = a_position;
                v_texCoord = a_texCoord;
            }
        """

        /**
         * Frame interpolation shader using optical flow.
         */
        private const val INTERPOLATION_FRAGMENT_SHADER = """
            precision mediump float;
            
            uniform sampler2D u_frame1;
            uniform sampler2D u_frame2;
            uniform float u_t;
            uniform vec2 u_resolution;
            
            varying vec2 v_texCoord;
            
            // Simple optical flow estimation
            vec2 estimateFlow(vec2 uv) {
                vec2 flow = vec2(0.0);
                float minDiff = 1.0;
                
                for (int x = -4; x <= 4; x++) {
                    for (int y = -4; y <= 4; y++) {
                        vec2 offset = vec2(float(x), float(y)) / u_resolution;
                        float diff = distance(texture2D(u_frame1, uv).rgb, texture2D(u_frame2, uv + offset).rgb);
                        if (diff < minDiff) {
                            minDiff = diff;
                            flow = offset;
                        }
                    }
                }
                
                return flow;
            }
            
            void main() {
                vec2 uv = v_texCoord;
                
                // Estimate optical flow
                vec2 flow = estimateFlow(uv);
                
                // Warp frame1 towards frame2 using flow
                vec2 warpedUV = uv + flow * u_t;
                
                // Sample both frames
                vec4 color1 = texture2D(u_frame1, warpedUV);
                vec4 color2 = texture2D(u_frame2, uv - flow * (1.0 - u_t));
                
                // Blend frames based on time
                gl_FragColor = mix(color1, color2, u_t);
            }
        """
    }
}

/**
 * Speed curve for speed ramp effects.
 */
sealed class SpeedCurve(val displayName: String) {
    abstract fun getSpeedAt(progress: Float): Float
    
    data object CONSTANT : SpeedCurve("Constant") {
        override fun getSpeedAt(progress: Float): Float = 1f
    }
    
    data object EASE_IN : SpeedCurve("Ease In") {
        override fun getSpeedAt(progress: Float): Float = progress * 2f
    }
    
    data object EASE_OUT : SpeedCurve("Ease Out") {
        override fun getSpeedAt(progress: Float): Float = 2f - progress * 2f
    }
    
    data object HERO : SpeedCurve("Hero") {
        override fun getSpeedAt(progress: Float): Float {
            return when {
                progress < 0.3f -> 0.25f  // Slow-mo buildup
                progress < 0.5f -> 2f      // Fast action
                progress < 0.7f -> 0.1f    // Ultra slow-mo
                else -> 1f                  // Normal speed
            }
        }
    }
    
    data object MONTAGE : SpeedCurve("Montage") {
        override fun getSpeedAt(progress: Float): Float {
            return when {
                progress < 0.2f -> 2f
                progress < 0.4f -> 0.5f
                progress < 0.6f -> 1.5f
                progress < 0.8f -> 0.3f
                else -> 1f
            }
        }
    }
    
    data object BULLET_TIME : SpeedCurve("Bullet Time") {
        override fun getSpeedAt(progress: Float): Float {
            return when {
                progress < 0.4f -> 1f
                progress < 0.6f -> 0.05f  // Ultra slow-mo
                else -> 1.5f
            }
        }
    }
    
    data class CUSTOM(val keyframes: List<Pair<Float, Float>>) : SpeedCurve("Custom") {
        override fun getSpeedAt(progress: Float): Float {
            if (keyframes.isEmpty()) return 1f
            if (keyframes.size == 1) return keyframes[0].second
            
            // Find surrounding keyframes
            val before = keyframes.lastOrNull { it.first <= progress } ?: keyframes.first()
            val after = keyframes.firstOrNull { it.first > progress } ?: keyframes.last()
            
            if (before.first == after.first) return before.second
            
            // Interpolate
            val t = (progress - before.first) / (after.first - before.first)
            return before.second + (after.second - before.second) * t
        }
    }
}

/**
 * Speed ramp result.
 */
data class SpeedRampResult(
    val outputFrame: Int,
    val speedMultiplier: Float,
    val shouldInterpolate: Boolean
)

/**
 * Optical flow field.
 */
data class OpticalFlowField(
    val width: Int,
    val height: Int,
    val flowX: FloatArray,
    val flowY: FloatArray
)
