package com.powercut.editor.domain.processing

import android.opengl.Matrix
import android.util.Log
import androidx.media3.common.Effect
import androidx.media3.common.util.GlUtil
import androidx.media3.effect.RgbMatrix
import com.powercut.editor.domain.filter.FilterCatalog
import com.powercut.editor.domain.look.PremiumLooks
import javax.inject.Inject
import javax.inject.Singleton

class ColorEffect(
    val brightness: Float,
    val contrast: Float,
    val saturation: Float,
    val temperature: Float,
    val tint: Float,
    private val matrix: FloatArray
) : RgbMatrix {
    override fun getMatrix(presentationTimeUs: Long, useHdr: Boolean): FloatArray = matrix
}

@Singleton
class Media3EffectPipeline @Inject constructor() {
    companion object { private const val TAG = "Media3EffectPipeline" }

    fun buildEffects(
        filterId: String = "none",
        premiumLookId: String = "none",
        imageEditorBrightness: Float = 0f,
        imageEditorContrast: Float = 1f,
        imageEditorSaturation: Float = 1f,
        imageEditorTemperature: Float = 0f,
        imageEditorExposure: Float = 0f,
        colorLift: Float = 0f,
        colorGamma: Float = 0f,
        colorGain: Float = 0f
    ): List<ColorEffect> {
        val effects = mutableListOf<ColorEffect>()
        if (filterId != "none" && filterId.isNotBlank()) buildFilterEffect(filterId)?.let(effects::add)
        if (premiumLookId != "none" && premiumLookId.isNotBlank()) buildPremiumLookEffect(premiumLookId)?.let(effects::add)
        buildImageEditorEffect(imageEditorBrightness, imageEditorContrast, imageEditorSaturation, imageEditorTemperature, imageEditorExposure)?.let(effects::add)
        buildColorCurvesEffect(colorLift, colorGamma, colorGain)?.let(effects::add)
        Log.d(TAG, "Built ${effects.size} Media3 color effects for filter=$filterId, look=$premiumLookId")
        return effects
    }

    private fun buildFilterEffect(filterId: String): ColorEffect? {
        val chain = FilterCatalog.ffmpeg(filterId)
        if (chain.isBlank()) return null
        val params = parseFfmpegChain(chain)
        if (params.isEmpty()) return null
        val rs = params["rs"] ?: 0f; val gs = params["gs"] ?: 0f; val bs = params["bs"] ?: 0f
        val rm = params["rm"] ?: 0f; val gm = params["gm"] ?: 0f; val bm = params["bm"] ?: 0f
        return createRgbAdjustment(
            brightness = (params["brightness"] ?: 0f).coerceIn(-1f, 1f),
            contrast = (params["contrast"] ?: 1f).coerceIn(0f, 4f),
            saturation = (params["saturation"] ?: 1f).coerceIn(0f, 4f),
            temperature = (((rs + rm) - (bs + bm)) * 50f).coerceIn(-100f, 100f),
            tint = (((gs + gm) - ((rs + rm + bs + bm) / 2f)) * 50f).coerceIn(-100f, 100f)
        )
    }

    private fun buildPremiumLookEffect(lookId: String): ColorEffect? {
        val chain = PremiumLooks.chainFor(lookId)
        if (chain.isBlank()) return null
        val params = parseFfmpegChain(chain)
        if (params.isEmpty()) return null
        val rs = params["rs"] ?: 0f; val gs = params["gs"] ?: 0f; val bs = params["bs"] ?: 0f
        val rm = params["rm"] ?: 0f; val gm = params["gm"] ?: 0f; val bm = params["bm"] ?: 0f
        return createRgbAdjustment(
            brightness = (params["brightness"] ?: 0f).coerceIn(-1f, 1f),
            contrast = (params["contrast"] ?: 1f).coerceIn(0f, 4f),
            saturation = (params["saturation"] ?: 1f).coerceIn(0f, 4f),
            temperature = (((rs + rm) - (bs + bm)) * 50f).coerceIn(-100f, 100f),
            tint = (((gs + gm) - ((rs + rm + bs + bm) / 2f)) * 50f).coerceIn(-100f, 100f)
        )
    }

    private fun buildImageEditorEffect(brightness: Float, contrast: Float, saturation: Float, temperature: Float, exposure: Float): ColorEffect? {
        if (brightness == 0f && contrast == 1f && saturation == 1f && temperature == 0f && exposure == 0f) return null
        return createRgbAdjustment(
            brightness = (brightness / 100f + exposure / 200f).coerceIn(-1f, 1f),
            contrast = contrast.coerceIn(0f, 4f), saturation = saturation.coerceIn(0f, 4f),
            temperature = temperature.coerceIn(-100f, 100f), tint = 0f
        )
    }

    private fun buildColorCurvesEffect(lift: Float, gamma: Float, gain: Float): ColorEffect? {
        if (lift == 0f && gamma == 0f && gain == 0f) return null
        return createRgbAdjustment(
            brightness = (lift / 100f + gain / 200f).coerceIn(-1f, 1f),
            contrast = (1f + gamma / 100f).coerceIn(0.1f, 4f), saturation = 1f, temperature = 0f, tint = 0f
        )
    }

    private fun createRgbAdjustment(brightness: Float, contrast: Float, saturation: Float, temperature: Float, tint: Float): ColorEffect {
        val brightnessM = GlUtil.create4x4IdentityMatrix().also { if (brightness != 0f) Matrix.translateM(it, 0, brightness, brightness, brightness) }
        val contrastM = GlUtil.create4x4IdentityMatrix().also {
            if (contrast != 1f) { Matrix.scaleM(it, 0, contrast, contrast, contrast); val mid = 0.5f - 0.5f * contrast; it[12] = mid; it[13] = mid; it[14] = mid }
        }
        val saturationM = GlUtil.create4x4IdentityMatrix().also {
            if (saturation != 1f) { val r = 0.2126f; val g = 0.7152f; val b = 0.0722f; val inv = 1f - saturation; it[0] = saturation + inv*r; it[4] = inv*g; it[8] = inv*b; it[1] = inv*r; it[5] = saturation + inv*g; it[9] = inv*b; it[2] = inv*r; it[6] = inv*g; it[10] = saturation + inv*b }
        }
        val temperatureM = GlUtil.create4x4IdentityMatrix().also { if (temperature != 0f) { val t = (temperature / 100f).coerceIn(-1f, 1f); Matrix.scaleM(it, 0, (1f+t).coerceAtLeast(0f), 1f, (1f-t).coerceAtLeast(0f)) } }
        val tintM = GlUtil.create4x4IdentityMatrix().also { if (tint != 0f) { val t = (tint / 100f).coerceIn(-1f, 1f); Matrix.scaleM(it, 0, (1f+t*0.5f).coerceAtLeast(0f), (1f-t).coerceAtLeast(0f), (1f+t*0.5f).coerceAtLeast(0f)) } }
        var combined = multiplyMatrices(contrastM, brightnessM)
        combined = multiplyMatrices(saturationM, combined); combined = multiplyMatrices(temperatureM, combined); combined = multiplyMatrices(tintM, combined)
        return ColorEffect(brightness, contrast, saturation, temperature, tint, combined)
    }

    private fun multiplyMatrices(lhs: FloatArray, rhs: FloatArray): FloatArray = FloatArray(16).also { Matrix.multiplyMM(it, 0, lhs, 0, rhs, 0) }

    private fun parseFfmpegChain(chain: String): Map<String, Float> {
        val params = mutableMapOf<String, Float>()
        for (filter in chain.split(",").map(String::trim)) {
            val body = when { filter.startsWith("eq=") -> filter.removePrefix("eq="); filter.startsWith("colorbalance=") -> filter.removePrefix("colorbalance="); else -> continue }
            for (kv in body.split(":")) {
                val parts = kv.split("=", limit = 2)
                if (parts.size == 2) parts[0].trim().let { key -> parts[1].trim().toFloatOrNull()?.let { params[key] = it } }
            }
        }
        return params
    }

    fun buildEffectsFromProject(project: com.powercut.editor.data.VideoProject): List<ColorEffect> = buildEffects(
        filterId = project.selectedFilter, premiumLookId = project.activePremiumLook,
        imageEditorBrightness = project.imageEditorBrightness, imageEditorContrast = project.imageEditorContrast,
        imageEditorSaturation = project.imageEditorSaturation, imageEditorTemperature = project.imageEditorTemperature,
        imageEditorExposure = project.imageEditorExposure, colorLift = project.colorLift,
        colorGamma = project.colorGamma, colorGain = project.colorGain
    )

    /** Builds color effects plus the aspect-aware Crop effect for live preview. */
    fun buildAllEffects(selectedEffect: String = "none", project: com.powercut.editor.data.VideoProject): List<Effect> {
        val allEffects = mutableListOf<Effect>()
        allEffects.addAll(buildEffectsFromProject(project))
        if (selectedEffect != "none" && selectedEffect.isNotBlank()) allEffects.addAll(buildVisualEffect(selectedEffect))
        Media3CropEffect.forProject(project)?.let(allEffects::add)
        Log.d(TAG, "Built ${allEffects.size} total live effects (visual=$selectedEffect, crop=${project.cropPreset})")
        return allEffects
    }

    private fun buildVisualEffect(effectId: String): List<ColorEffect> {
        if (!VideoProcessor.isGpuRepresentableEffect(effectId)) return emptyList()
        val chain = VideoProcessor.EXACT_EFFECT_CHAINS[effectId.lowercase().replace(" ", "_").replace("-", "_")] ?: return emptyList()
        return if (chain.isBlank()) emptyList() else EffectGLConverter.convertChain(chain)
    }
}
