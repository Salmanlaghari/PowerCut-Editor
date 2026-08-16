package com.powercut.editor.domain.processing

import android.util.Log
import androidx.media3.effect.RgbAdjustment
import com.powercut.editor.domain.filter.FilterCatalog
import com.powercut.editor.domain.look.PremiumLooks
import com.powercut.editor.domain.filter.filterPreviewMatrixForId
import com.powercut.editor.domain.look.PremiumLook

/**
 * Media3EffectPipeline - Builds a list of Media3 Effect objects based on
 * current filter/look selection state (colorFilterType and related fields).
 *
 * This is Phase 1, Step A - Foundation only. It creates the Effect objects
 * but does NOT attach them to ExoPlayer or Transformer (that's Step B).
 *
 * Maps filter/look parameters to RgbAdjustment:
 * - brightness, contrast, saturation, exposure
 * - color temperature/tint
 */
class Media3EffectPipeline {

    companion object {
        private const val TAG = "Media3EffectPipeline"
    }

    /**
     * Builds a list of Media3 Effects for the given project state.
     *
     * @param filterId The active filter ID from FilterCatalog (e.g., "warm", "cool", "cinematic")
     * @param premiumLookId The active premium look ID from PremiumLooks (e.g., "hdr_vivid", "iphone_cinematic")
     * @param imageEditorBrightness Brightness adjustment from image editor (-100 to 100)
     * @param imageEditorContrast Contrast adjustment from image editor (0.0 to 2.0+)
     * @param imageEditorSaturation Saturation adjustment from image editor (0.0 to 2.0+)
     * @param imageEditorTemperature Temperature adjustment from image editor (-100 to 100)
     * @param imageEditorExposure Exposure adjustment from image editor (-100 to 100)
     * @param colorLift Color lift from curves (-100 to 100)
     * @param colorGamma Color gamma from curves (-100 to 100)
     * @param colorGain Color gain from curves (-100 to 100)
     * @return List of Media3 Effect objects (RgbAdjustment instances)
     */
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
    ): List<RgbAdjustment> {
        val effects = mutableListOf<RgbAdjustment>()

        // 1. Apply filter effects from FilterCatalog
        if (filterId != "none" && filterId.isNotBlank()) {
            val filterEffect = buildFilterEffect(filterId)
            filterEffect?.let { effects.add(it) }
        }

        // 2. Apply premium look effects from PremiumLooks
        if (premiumLookId != "none" && premiumLookId.isNotBlank()) {
            val lookEffect = buildPremiumLookEffect(premiumLookId)
            lookEffect?.let { effects.add(it) }
        }

        // 3. Apply image editor adjustments
        val editorEffect = buildImageEditorEffect(
            brightness = imageEditorBrightness,
            contrast = imageEditorContrast,
            saturation = imageEditorSaturation,
            temperature = imageEditorTemperature,
            exposure = imageEditorExposure
        )
        editorEffect?.let { effects.add(it) }

        // 4. Apply color curves (lift/gamma/gain)
        val curvesEffect = buildColorCurvesEffect(
            lift = colorLift,
            gamma = colorGamma,
            gain = colorGain
        )
        curvesEffect?.let { effects.add(it) }

        Log.d(TAG, "Built ${effects.size} Media3 effects for filter=$filterId, look=$premiumLookId")
        return effects
    }

    /**
     * Builds a single RgbAdjustment from a FilterCatalog filter ID.
     * Parses the FFmpeg chain to extract eq/colorbalance parameters.
     */
    private fun buildFilterEffect(filterId: String): RgbAdjustment? {
        val chain = FilterCatalog.ffmpeg(filterId)
        if (chain.isBlank()) return null

        // Parse the FFmpeg chain to extract RGB adjustment parameters
        val params = parseFfmpegChain(chain)
        if (params.isEmpty()) return null

        // Convert parsed parameters to RgbAdjustment
        // Note: RgbAdjustment uses brightness (-1 to 1), contrast (0 to 2+), saturation (0 to 2+)
        // FFmpeg eq uses: brightness (-1 to 1), contrast (0 to 2+), saturation (0 to 2+)
        // colorbalance uses rs/gs/bs (shadows), rm/gm/bm (midtones), rh/gh/bh (highlights)
        // Map colorbalance to RgbAdjustment's temperature/tint equivalent
        val brightness = params["brightness"] ?: 0f
        val contrast = params["contrast"] ?: 1f
        val saturation = params["saturation"] ?: 1f

        // Temperature/tint from colorbalance: warm = positive temperature, cool = negative
        val rs = params["rs"] ?: 0f
        val gs = params["gs"] ?: 0f
        val bs = params["bs"] ?: 0f
        val rm = params["rm"] ?: 0f
        val gm = params["gm"] ?: 0f
        val bm = params["bm"] ?: 0f

        // Approximate temperature from red-blue balance in shadows+midtones
        val tempShift = ((rs + rm) - (bs + bm)) * 50f // Scale to -100..100 range
        // Approximate tint from green-magenta balance
        val tintShift = ((gs + gm) - ((rs + rm + bs + bm) / 2f)) * 50f

        return RgbAdjustment(
            brightness = brightness.coerceIn(-1f, 1f),
            contrast = contrast.coerceIn(0f, 4f),
            saturation = saturation.coerceIn(0f, 4f),
            temperature = tempShift.coerceIn(-100f, 100f),
            tint = tintShift.coerceIn(-100f, 100f)
        )
    }

    /**
     * Builds a single RgbAdjustment from a PremiumLooks look ID.
     * Parses the FFmpeg chain to extract eq/colorbalance parameters.
     */
    private fun buildPremiumLookEffect(lookId: String): RgbAdjustment? {
        val chain = PremiumLooks.chainFor(lookId)
        if (chain.isBlank()) return null

        val params = parseFfmpegChain(chain)
        if (params.isEmpty()) return null

        val brightness = params["brightness"] ?: 0f
        val contrast = params["contrast"] ?: 1f
        val saturation = params["saturation"] ?: 1f

        val rs = params["rs"] ?: 0f
        val gs = params["gs"] ?: 0f
        val bs = params["bs"] ?: 0f
        val rm = params["rm"] ?: 0f
        val gm = params["gm"] ?: 0f
        val bm = params["bm"] ?: 0f

        val tempShift = ((rs + rm) - (bs + bm)) * 50f
        val tintShift = ((gs + gm) - ((rs + rm + bs + bm) / 2f)) * 50f

        return RgbAdjustment(
            brightness = brightness.coerceIn(-1f, 1f),
            contrast = contrast.coerceIn(0f, 4f),
            saturation = saturation.coerceIn(0f, 4f),
            temperature = tempShift.coerceIn(-100f, 100f),
            tint = tintShift.coerceIn(-100f, 100f)
        )
    }

    /**
     * Builds a single RgbAdjustment from image editor parameters.
     * These are direct user adjustments.
     */
    private fun buildImageEditorEffect(
        brightness: Float,
        contrast: Float,
        saturation: Float,
        temperature: Float,
        exposure: Float
    ): RgbAdjustment? {
        // Only create if there are actual adjustments
        if (brightness == 0f && contrast == 1f && saturation == 1f &&
            temperature == 0f && exposure == 0f) {
            return null
        }

        // Map image editor ranges to RgbAdjustment ranges:
        // brightness: -100..100 -> -1..1
        // contrast: 0..2+ -> 0..4 (coerced)
        // saturation: 0..2+ -> 0..4 (coerced)
        // temperature: -100..100 -> -100..100 (direct)
        // exposure: -100..100 -> affects brightness, scaled to -1..1

        val rgbBrightness = (brightness / 100f + exposure / 200f).coerceIn(-1f, 1f)
        val rgbContrast = contrast.coerceIn(0f, 4f)
        val rgbSaturation = saturation.coerceIn(0f, 4f)
        val rgbTemperature = temperature.coerceIn(-100f, 100f)
        val rgbTint = 0f // Image editor doesn't have separate tint control

        return RgbAdjustment(
            brightness = rgbBrightness,
            contrast = rgbContrast,
            saturation = rgbSaturation,
            temperature = rgbTemperature,
            tint = rgbTint
        )
    }

    /**
     * Builds a single RgbAdjustment from color curves (lift/gamma/gain).
     * Approximates lift/gamma/gain using brightness/contrast/temperature.
     */
    private fun buildColorCurvesEffect(
        lift: Float,
        gamma: Float,
        gain: Float
    ): RgbAdjustment? {
        if (lift == 0f && gamma == 0f && gain == 0f) return null

        // Approximate lift/gamma/gain:
        // lift (-100..100) -> brightness offset (-1..1)
        // gamma (-100..100) -> contrast adjustment (1.0 +/- gamma/100)
        // gain (-100..100) -> exposure/brightness boost
        val brightness = (lift / 100f + gain / 200f).coerceIn(-1f, 1f)
        val contrast = (1f + gamma / 100f).coerceIn(0.1f, 4f)
        val temperature = 0f // Lift/gamma/gain doesn't directly map to temperature
        val tint = 0f

        return RgbAdjustment(
            brightness = brightness,
            contrast = contrast,
            saturation = 1f, // Lift/gamma/gain preserves saturation
            temperature = temperature,
            tint = tint
        )
    }

    /**
     * Parses an FFmpeg filter chain string and extracts eq/colorbalance parameters.
     * Returns a map of parameter names to float values.
     */
    private fun parseFfmpegChain(chain: String): Map<String, Float> {
        val params = mutableMapOf<String, Float>()

        for (subFilter in chain.split(",")) {
            val filter = subFilter.trim()
            when {
                filter.startsWith("eq=") -> {
                    // Parse eq=brightness=0.1:contrast=1.2:saturation=1.5
                    val eqParams = filter.removePrefix("eq=").split(":")
                    for (kv in eqParams) {
                        val parts = kv.split("=")
                        if (parts.size == 2) {
                            val value = parts[1].trim().toFloatOrNull()
                            value?.let { params[parts[0].trim()] = it }
                        }
                    }
                }
                filter.startsWith("colorbalance=") -> {
                    // Parse colorbalance=rs=0.1:gs=-0.05:bs=0.08:rm=0.05:gm=0.02:bm=0.03
                    val cbParams = filter.removePrefix("colorbalance=").split(":")
                    for (kv in cbParams) {
                        val parts = kv.split("=")
                        if (parts.size == 2) {
                            val value = parts[1].trim().toFloatOrNull()
                            value?.let { params[parts[0].trim()] = it }
                        }
                    }
                }
            }
        }

        return params
    }

    /**
     * Convenience method to build effects from a VideoProject.
     */
    fun buildEffectsFromProject(project: com.powercut.editor.data.VideoProject): List<RgbAdjustment> {
        return buildEffects(
            filterId = project.selectedFilter,
            premiumLookId = project.activePremiumLook,
            imageEditorBrightness = project.imageEditorBrightness,
            imageEditorContrast = project.imageEditorContrast,
            imageEditorSaturation = project.imageEditorSaturation,
            imageEditorTemperature = project.imageEditorTemperature,
            imageEditorExposure = project.imageEditorExposure,
            colorLift = project.colorLift,
            colorGamma = project.colorGamma,
            colorGain = project.colorGain
        )
    }
}