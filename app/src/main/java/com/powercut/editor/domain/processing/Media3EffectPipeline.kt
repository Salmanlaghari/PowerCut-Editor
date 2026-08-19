package com.powercut.editor.domain.processing

import android.opengl.Matrix
import android.util.Log
import androidx.media3.common.util.GlUtil
import androidx.media3.effect.RgbMatrix
import com.powercut.editor.domain.filter.FilterCatalog
import com.powercut.editor.domain.look.PremiumLooks
import com.powercut.editor.domain.filter.filterPreviewMatrixForId
import com.powercut.editor.domain.look.PremiumLook
import javax.inject.Inject
import javax.inject.Singleton

/**
 * A color-adjustment effect that is both a Media3 [RgbMatrix] (so it can be applied
 * to ExoPlayer live preview via `setVideoEffects` and to a Media3 `Transformer`
 * export via `Effects`) and a carrier of the human-readable parameters it was
 * built from. Exposing brightness/contrast/saturation/temperature/tint lets the
 * live preview and the export pipeline share one [Media3EffectPipeline] and lets
 * tests assert parity without re-deriving the matrix.
 */
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
@Singleton
class Media3EffectPipeline @Inject constructor() {

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
     * @return List of Media3 Effect objects (ColorEffect/RgbMatrix instances)
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
    ): List<ColorEffect> {
        val effects = mutableListOf<ColorEffect>()

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
    private fun buildFilterEffect(filterId: String): ColorEffect? {
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

        return createRgbAdjustment(
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
    private fun buildPremiumLookEffect(lookId: String): ColorEffect? {
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

        return createRgbAdjustment(
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
    ): ColorEffect? {
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

        return createRgbAdjustment(
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
    ): ColorEffect? {
        if (lift == 0f && gamma == 0f && gain == 0f) return null

        // Approximate lift/gamma/gain:
        // lift (-100..100) -> brightness offset (-1..1)
        // gamma (-100..100) -> contrast adjustment (1.0 +/- gamma/100)
        // gain (-100..100) -> exposure/brightness boost
        val brightness = (lift / 100f + gain / 200f).coerceIn(-1f, 1f)
        val contrast = (1f + gamma / 100f).coerceIn(0.1f, 4f)
        val temperature = 0f // Lift/gamma/gain doesn't directly map to temperature
        val tint = 0f

        return createRgbAdjustment(
            brightness = brightness,
            contrast = contrast,
            saturation = 1f, // Lift/gamma/gain preserves saturation
            temperature = temperature,
            tint = tint
        )
    }

    /**
     * Creates an RgbMatrix encoding the given color adjustments as a full 4x4 matrix.
     *
     * Media3 1.4.1's RgbAdjustment exposes only a private FloatArray constructor and a
     * Builder limited to R/G/B channel scaling — neither can represent brightness, contrast,
     * saturation, temperature, or tint. Instead we implement RgbMatrix directly with a
     * 4x4 column-major matrix (the 4th column holds additive RGB offsets for opaque A=1
     * pixels), composing: brightness → contrast → saturation → temperature → tint.
     */
    private fun createRgbAdjustment(
        brightness: Float,
        contrast: Float,
        saturation: Float,
        temperature: Float,
        tint: Float
    ): ColorEffect {
        // Brightness: additive offset on RGB (out = in + b).
        val brightnessM = GlUtil.create4x4IdentityMatrix()
        if (brightness != 0f) {
            Matrix.translateM(brightnessM, 0, brightness, brightness, brightness)
        }

        // Contrast: scale around 0.5 mid-gray (out = c·in + (0.5 − 0.5c)).
        val contrastM = GlUtil.create4x4IdentityMatrix()
        if (contrast != 1f) {
            Matrix.scaleM(contrastM, 0, contrast, contrast, contrast)
            val mid = 0.5f - 0.5f * contrast
            contrastM[12] = mid
            contrastM[13] = mid
            contrastM[14] = mid
        }

        // Saturation: luminance-weighted channel mixing (Rec.709 weights).
        val saturationM = GlUtil.create4x4IdentityMatrix()
        if (saturation != 1f) {
            val r = 0.2126f
            val g = 0.7152f
            val b = 0.0722f
            val inv = 1f - saturation
            saturationM[0] = saturation + inv * r
            saturationM[4] = inv * g
            saturationM[8] = inv * b
            saturationM[1] = inv * r
            saturationM[5] = saturation + inv * g
            saturationM[9] = inv * b
            saturationM[2] = inv * r
            saturationM[6] = inv * g
            saturationM[10] = saturation + inv * b
        }

        // Temperature: warm/cool → +R/−B diagonal scaling.
        val temperatureM = GlUtil.create4x4IdentityMatrix()
        if (temperature != 0f) {
            val t = (temperature / 100f).coerceIn(-1f, 1f)
            Matrix.scaleM(
                temperatureM, 0,
                (1f + t).coerceAtLeast(0f),
                1f,
                (1f - t).coerceAtLeast(0f)
            )
        }

        // Tint: green/magenta → −G, +R/+B.
        val tintM = GlUtil.create4x4IdentityMatrix()
        if (tint != 0f) {
            val ti = (tint / 100f).coerceIn(-1f, 1f)
            Matrix.scaleM(
                tintM, 0,
                (1f + ti * 0.5f).coerceAtLeast(0f),
                (1f - ti).coerceAtLeast(0f),
                (1f + ti * 0.5f).coerceAtLeast(0f)
            )
        }

        // Compose: M = tintM · tempM · satM · contrastM · brightnessM
        // (brightness applied first, tint last).
        var combined = multiplyMatrices(contrastM, brightnessM)
        combined = multiplyMatrices(saturationM, combined)
        combined = multiplyMatrices(temperatureM, combined)
        combined = multiplyMatrices(tintM, combined)

        val matrix = combined
        return ColorEffect(
            brightness = brightness,
            contrast = contrast,
            saturation = saturation,
            temperature = temperature,
            tint = tint,
            matrix = matrix
        )
    }

    /** Multiplies two 4x4 column-major matrices: result = lhs · rhs. */
    private fun multiplyMatrices(lhs: FloatArray, rhs: FloatArray): FloatArray {
        val result = FloatArray(16)
        Matrix.multiplyMM(result, 0, lhs, 0, rhs, 0)
        return result
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
    fun buildEffectsFromProject(project: com.powercut.editor.data.VideoProject): List<ColorEffect> {
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

    /**
     * Builds the COMPLETE list of Media3 Effects for live preview, including:
     *  1. Filter/Look color effects (from FilterCatalog / PremiumLooks)
     *  2. Image editor adjustments (brightness, contrast, etc.)
     *  3. **Visual effects** from EffectCatalog (the EffectsScreen)
     *
     * This is the method that should be used in NextGenEditorScreen to ensure
     * ALL effects are visible in the live ExoPlayer preview — not just filters.
     *
     * @param selectedEffect The active visual effect ID from EffectCatalog (e.g., "hdr", "vivid")
     * @param project The full VideoProject for all other parameters
     * @return Combined list of ColorEffect objects for ExoPlayer.setVideoEffects()
     */
    fun buildAllEffects(
        selectedEffect: String = "none",
        project: com.powercut.editor.data.VideoProject
    ): List<ColorEffect> {
        val allEffects = mutableListOf<ColorEffect>()

        // 1. Color effects from filters/looks/editor
        val colorEffects = buildEffectsFromProject(project)
        allEffects.addAll(colorEffects)

        // 2. Visual effect from EffectCatalog (the EffectsScreen effects)
        //    Converts FFmpeg chain → ColorEffect via EffectGLConverter
        if (selectedEffect != "none" && selectedEffect.isNotBlank()) {
            val visualEffects = buildVisualEffect(selectedEffect)
            allEffects.addAll(visualEffects)
        }

        Log.d(TAG, "Built ${allEffects.size} total effects (color=${colorEffects.size}, visual=$selectedEffect)")
        return allEffects
    }

    /**
     * Converts an EffectCatalog visual effect ID to ColorEffect objects
     * by parsing its FFmpeg chain through EffectGLConverter.
     *
     * This bridges the gap: EffectCatalog stores FFmpeg chains (for export),
     * and this method converts them to OpenGL color matrices (for live preview).
     */
    private fun buildVisualEffect(effectId: String): List<ColorEffect> {
        // Look up the FFmpeg chain from EffectCatalog
        val effect = com.powercut.editor.ui.premium.EffectCatalog.effects
            .firstOrNull { it.id == effectId }
        if (effect == null || effect.ffmpegChain.isBlank()) return emptyList()

        // Convert FFmpeg chain → ColorEffect objects
        return EffectGLConverter.convertChain(effect.ffmpegChain)
    }
}
