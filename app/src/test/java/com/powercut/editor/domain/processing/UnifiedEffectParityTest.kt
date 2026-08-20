package com.powercut.editor.domain.processing

import com.powercut.editor.domain.filter.FilterCatalog
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Regression tests for the unified filter/effect preview + export model.
 *
 * Catches the original bug ("UI selection changes but the video remains
 * unchanged") for BOTH filters and visual effects:
 *
 *  - Every EffectsScreen effect must have a real export chain
 *    ([VideoProcessor.EXACT_EFFECT_CHAINS]) so export actually changes the video.
 *  - Every NON-GPU effect (blur / noise / edges / pixelate / emboss / convolution
 *    / geometric distortion) must produce a real FFmpeg preview
 *    ([FilterPreviewRenderer.buildPreviewFilter]) so the live preview also
 *    changes — not just the selection state.
 *  - GPU-representable effects (pure eq/colorbalance/hue/vignette/curves/negate)
 *    are allowed to preview on the GPU pipeline instead.
 *  - Filters and effects must combine into ONE FFmpeg chain so filter + effect
 *    appears together and matches export.
 */
class UnifiedEffectParityTest {

    /** Every effect in the export map has a non-blank, real FFmpeg chain. */
    @Test
    fun everyExportEffectChainIsReal() {
        assertTrue("EXACT_EFFECT_CHAINS must not be empty", VideoProcessor.EXACT_EFFECT_CHAINS.isNotEmpty())
        for ((id, chain) in VideoProcessor.EXACT_EFFECT_CHAINS) {
            assertTrue("Effect '$id' has a blank export chain", chain.isNotBlank())
            assertTrue("Effect '$id' export chain must not be 'none'", chain != "none")
        }
    }

    /**
     * No effect may be silently dropped: a non-GPU effect MUST have a real
     * FFmpeg preview, otherwise the live preview would show the raw video while
     * pretending the effect was applied.
     */
    @Test
    fun nonGpuEffectsAlwaysHaveFfmpegPreview() {
        for (id in VideoProcessor.EXACT_EFFECT_CHAINS.keys) {
            if (VideoProcessor.isGpuRepresentableEffect(id)) continue
            val preview = FilterPreviewRenderer.buildPreviewFilter("none", id)
            assertNotNull(
                "Non-GPU effect '$id' must produce a real FFmpeg preview " +
                    "(otherwise the live preview shows the raw, unmodified video)",
                preview
            )
        }
    }

    /** GPU-representable effects are allowed to preview on the Media3 pipeline. */
    @Test
    fun gpuRepresentableEffectsAreClassifiedCorrectly() {
        val gpuEffects = listOf(
            "vivid", "cinematic", "noir", "vintage", "fade", "warm", "cool", "punchy",
            "muted", "lomo", "pastel", "mono", "sepia", "invert", "polaroid", "kodak",
            "highkey", "lowkey", "vignette", "hdr", "dramatic", "clarity", "matte",
            "colorpop", "golden", "midnight", "tealorange", "forest", "ocean"
        )
        for (id in gpuEffects) {
            assertTrue("Effect '$id' should be GPU-representable", VideoProcessor.isGpuRepresentableEffect(id))
        }
    }

    /** Complex effects must NOT be classified as GPU-representable. */
    @Test
    fun complexEffectsAreNotGpuRepresentable() {
        val complex = listOf(
            "posterize", "edge", "pixelate", "emboss", "filmgrain", "glow", "bloom",
            "blur", "glitch", "sketch", "neon", "cartoon", "xray", "thermal",
            "datamosh", "scanlines", "vhs", "crt", "distort", "kaleido",
            "watercolor", "oilpaint", "dust", "scratch", "grunge", "echo", "trail",
            "strobe", "8mm", "16mm", "35mm", "lightleak", "rgbshift", "motionblur",
            "tiltshift", "radialblur", "duotone"
        )
        for (id in complex) {
            val representable = if (VideoProcessor.EXACT_EFFECT_CHAINS.containsKey(id))
                VideoProcessor.isGpuRepresentableEffect(id) else false
            // Only assert for effects that actually exist in the export map.
            if (VideoProcessor.EXACT_EFFECT_CHAINS.containsKey(id)) {
                assertTrue(
                    "Complex effect '$id' must NOT be GPU-representable " +
                        "(it needs the real FFmpeg path)",
                    !representable
                )
            }
        }
    }

    /** Filter + effect must combine into a single, visibly-different FFmpeg chain. */
    @Test
    fun filterPlusEffectCombinesIntoOneChain() {
        val combined = FilterPreviewRenderer.buildPreviewFilter("sepia", "posterize")
        assertNotNull("sepia + posterize must produce a preview chain", combined)
        assertTrue("combined chain must contain the filter (colorchannelmixer)", combined!!.contains("colorchannelmixer"))
        assertTrue("combined chain must contain the effect (lutrgb)", combined.contains("lutrgb"))
    }

    /** GPU effects don't need a FFmpeg preview clip (the GPU pipeline draws them). */
    @Test
    fun gpuEffectNeedsNoFfmpegPreviewClip() {
        assertNull(
            "GPU effect 'vivid' should NOT need a separate FFmpeg preview clip",
            FilterPreviewRenderer.buildPreviewFilter("none", "vivid")
        )
    }

    /** Non-GPU effects DO need a FFmpeg preview clip. */
    @Test
    fun nonGpuEffectNeedsFfmpegPreviewClip() {
        assertNotNull(
            "Non-GPU effect 'posterize' must produce a FFmpeg preview clip",
            FilterPreviewRenderer.buildPreviewFilter("none", "posterize")
        )
    }

    /** The required filter set resolves to real, non-blank FFmpeg chains. */
    @Test
    fun requiredFiltersResolveToRealChains() {
        val required = listOf(
            "posterize_8", "posterize_4", "solarize", "solarize_strong",
            "edge_detect", "emboss_filter", "film_grain", "film_grain_heavy",
            "pixelate", "pixelate_medium", "color_swap"
        )
        for (f in required) {
            val chain = FilterCatalog.ffmpeg(f)
            assertTrue("Required filter '$f' has no real FFmpeg chain", chain.isNotBlank())
        }
        // Spot-check a few actually differ from each other (visible change).
        assertTrue(FilterCatalog.ffmpeg("posterize_8").startsWith("posterize="))
        assertTrue(FilterCatalog.ffmpeg("color_swap").startsWith("colorchannelmixer="))
        assertTrue(FilterCatalog.ffmpeg("edge_detect").startsWith("edgedetect"))
    }
}
