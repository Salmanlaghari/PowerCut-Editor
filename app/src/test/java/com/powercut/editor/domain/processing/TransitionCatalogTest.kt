package com.powercut.editor.domain.processing

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for [TransitionCatalog] — the mapping from editor transition ids to
 * REAL FFmpeg `xfade` transitions.
 *
 * These tests cover the LOGIC (mapping, clamping, duration arithmetic, locale
 * safety). Whether the emitted xfade names actually exist in FFmpeg, and whether
 * the resulting graphs really render, is proven separately against a real ffmpeg
 * binary by `scripts/validate_transitions_ffmpeg.py`.
 */
class TransitionCatalogTest {

    // ── Name validity ────────────────────────────────────────────────────

    @Test
    fun everyMappedTransitionUsesANameFromTheValidatedBaseSet() {
        for (id in TransitionCatalog.supportedIds) {
            val name = TransitionCatalog.xfadeNameFor(id)
            assertNotNull("$id produced no xfade name", name)
            assertTrue(
                "$id maps to '$name', which is not in the validated 4.4-floor set",
                name in TransitionCatalog.BASE_XFADE_NAMES
            )
        }
    }

    @Test
    fun theInvalidFdissolveNameIsNeverEmitted() {
        // Regression guard: the old buildXfadeTransition emitted `fdissolve`,
        // which exists in no FFmpeg release and hard-failed the export.
        assertFalse("fdissolve" in TransitionCatalog.BASE_XFADE_NAMES)
        for (id in TransitionCatalog.supportedIds) {
            assertTrue(TransitionCatalog.xfadeNameFor(id) != "fdissolve")
        }
        assertEquals("dissolve", TransitionCatalog.xfadeNameFor("dissolve"))
    }

    @Test
    fun baseSetAndExtendedSetAreDisjointAndCorrectlySized() {
        // FFmpeg 4.3/4.4 xfade enum 0..42 minus `custom` == 43 transitions.
        assertEquals(43, TransitionCatalog.BASE_XFADE_NAMES.size)
        // The 7.1+ additions must not leak into the floor set.
        val overlap = TransitionCatalog.BASE_XFADE_NAMES
            .intersect(TransitionCatalog.XFADE_ONLY_IN_FFMPEG_71_PLUS)
        assertTrue("floor set must not contain 7.1+ only names: $overlap", overlap.isEmpty())
        assertEquals(
            TransitionCatalog.BASE_XFADE_NAMES.size +
                TransitionCatalog.XFADE_ONLY_IN_FFMPEG_71_PLUS.size,
            TransitionCatalog.EXTENDED_XFADE_NAMES.size
        )
    }

    @Test
    fun newerFfmpegOnlyNamesAreNeverEmitted() {
        // Emitting e.g. `zoomin` would break on a 4.4-era FFmpeg.
        for (id in TransitionCatalog.supportedIds) {
            val name = TransitionCatalog.xfadeNameFor(id)
            assertFalse(
                "$id emits '$name', which requires FFmpeg 7.1+",
                name in TransitionCatalog.XFADE_ONLY_IN_FFMPEG_71_PLUS
            )
        }
    }

    // ── None / hard cut ──────────────────────────────────────────────────

    @Test
    fun noneMeansNoTransition() {
        for (raw in listOf("none", "None", "NONE", "", "  ", "hard_cut", "none_transition")) {
            assertTrue("'$raw' should be treated as none", TransitionCatalog.isNone(raw))
            assertNull(TransitionCatalog.xfadeNameFor(raw))
            assertNull(TransitionCatalog.specFor(raw))
        }
        assertTrue(TransitionCatalog.isNone(null))
    }

    // ── Normalisation ────────────────────────────────────────────────────

    @Test
    fun idsAreNormalisedAcrossSpacesDashesAndCase() {
        val expected = TransitionCatalog.xfadeNameFor("slide_left")
        assertEquals("slideleft", expected)
        for (variant in listOf("Slide Left", "slide-left", "SLIDE_LEFT", " slide left ")) {
            assertEquals(
                "variant '$variant' should normalise to slide_left",
                expected, TransitionCatalog.xfadeNameFor(variant)
            )
        }
    }

    @Test
    fun premiumTrPrefixedIdsResolve() {
        assertEquals("slideleft", TransitionCatalog.xfadeNameFor("tr_slide_left"))
        assertEquals("dissolve", TransitionCatalog.xfadeNameFor("tr_dissolve"))
    }

    @Test
    fun unknownTransitionsDegradeToARealCrossfadeRatherThanBeingDropped() {
        // A transition must never silently disappear — that is the PART 2 rule.
        val name = TransitionCatalog.xfadeNameFor("some_future_transition_v9")
        assertEquals("fade", name)
        assertTrue(TransitionCatalog.isRealizable("some_future_transition_v9"))
    }

    // ── The specific families PART 2 had to cover ────────────────────────

    @Test
    fun coreTransitionFamiliesMapToTheirRealEquivalents() {
        // fade family
        assertEquals("fade", TransitionCatalog.xfadeNameFor("fade"))
        assertEquals("fade", TransitionCatalog.xfadeNameFor("crossfade"))
        assertEquals("fadeblack", TransitionCatalog.xfadeNameFor("black_fade"))
        assertEquals("fadewhite", TransitionCatalog.xfadeNameFor("white_fade"))
        // wipe family
        assertEquals("wipeleft", TransitionCatalog.xfadeNameFor("wipe_left"))
        assertEquals("wiperight", TransitionCatalog.xfadeNameFor("wipe_right"))
        assertEquals("wipeup", TransitionCatalog.xfadeNameFor("wipe_up"))
        assertEquals("wipedown", TransitionCatalog.xfadeNameFor("wipe_down"))
        // slide family
        assertEquals("slideleft", TransitionCatalog.xfadeNameFor("slide_left"))
        assertEquals("slidedown", TransitionCatalog.xfadeNameFor("slide_down"))
        // push family -> xfade slide* genuinely pushes the outgoing clip out
        assertEquals("slideleft", TransitionCatalog.xfadeNameFor("push_left"))
        assertEquals("slideup", TransitionCatalog.xfadeNameFor("push_up"))
        // pixel / blur family
        assertEquals("pixelize", TransitionCatalog.xfadeNameFor("pixelate"))
        assertEquals("pixelize", TransitionCatalog.xfadeNameFor("mosaic"))
        assertEquals("hblur", TransitionCatalog.xfadeNameFor("blur"))
        // zoom, rotate, cube — approximated but still REAL inter-clip transitions
        assertTrue(TransitionCatalog.isRealizable("zoom_in"))
        assertTrue(TransitionCatalog.isRealizable("rotate_3d"))
        assertTrue(TransitionCatalog.isRealizable("cube"))
        assertTrue(TransitionCatalog.isRealizable("spin"))
    }

    @Test
    fun approximatedTransitionsAreFlaggedAndDocumented() {
        // We must not pretend an approximation is exact.
        val cube = TransitionCatalog.specFor("cube")!!
        assertTrue(cube.approximated)
        assertTrue("approximated specs must explain themselves", cube.note.isNotBlank())

        for (id in TransitionCatalog.supportedIds) {
            val spec = TransitionCatalog.specFor(id)!!
            if (spec.approximated) {
                assertTrue(
                    "approximated spec '$id' must document why",
                    spec.note.isNotBlank()
                )
            }
        }

        // Exact ones are not falsely flagged.
        assertFalse(TransitionCatalog.specFor("fade")!!.approximated)
        assertFalse(TransitionCatalog.specFor("dissolve")!!.approximated)
        assertFalse(TransitionCatalog.specFor("slide_left")!!.approximated)
    }

    // ── Duration clamping ────────────────────────────────────────────────

    @Test
    fun durationIsClampedToHalfTheShorterClip() {
        // 10s + 10s clips can host the full request.
        assertEquals(1.0, TransitionCatalog.clampDuration(1.0, 10.0, 10.0), 1e-9)
        // A 2s clip caps the transition at 1s.
        assertEquals(1.0, TransitionCatalog.clampDuration(5.0, 2.0, 10.0), 1e-9)
        // A 0.6s clip caps at 0.3s.
        assertEquals(0.3, TransitionCatalog.clampDuration(2.0, 0.6, 4.0), 1e-9)
    }

    @Test
    fun impossiblyShortClipsYieldZeroSoTheCutStaysAHardCut() {
        // Half of 0.1s is 0.05s, below MIN_DURATION_SEC -> no transition at all,
        // which the pipeline turns into a real hard cut rather than a broken xfade.
        assertEquals(0.0, TransitionCatalog.clampDuration(1.0, 0.1, 5.0), 1e-9)
        assertEquals(0.0, TransitionCatalog.clampDuration(1.0, 0.0, 5.0), 1e-9)
    }

    @Test
    fun durationNeverExceedsEitherClip() {
        val clips = listOf(0.4, 0.9, 1.0, 2.0, 3.5, 10.0)
        for (a in clips) for (b in clips) {
            val d = TransitionCatalog.clampDuration(4.0, a, b)
            assertTrue("clamped $d must not exceed clip A ($a)", d <= a)
            assertTrue("clamped $d must not exceed clip B ($b)", d <= b)
        }
    }

    // ── Timeline arithmetic ──────────────────────────────────────────────

    @Test
    fun transitionsOverlapAndThereforeShortenTheTimeline() {
        // 3 clips x 4s joined by 2 x 1s transitions == 10s, NOT 12s.
        val total = TransitionCatalog.totalDurationWithTransitions(
            listOf(4.0, 4.0, 4.0), listOf(1.0, 1.0)
        )
        assertEquals(10.0, total, 1e-9)
    }

    @Test
    fun hardCutTimelineKeepsFullLength() {
        val total = TransitionCatalog.totalDurationWithTransitions(
            listOf(4.0, 4.0, 4.0), emptyList()
        )
        assertEquals(12.0, total, 1e-9)
    }

    @Test
    fun totalDurationNeverGoesNegative() {
        val total = TransitionCatalog.totalDurationWithTransitions(
            listOf(1.0, 1.0), listOf(50.0)
        )
        assertEquals(0.0, total, 1e-9)
    }

    // ── Filter emission ──────────────────────────────────────────────────

    @Test
    fun xfadeFilterCarriesTheRealNameDurationAndOffset() {
        val f = TransitionCatalog.xfadeFilter("slide_left", 1.0, 3.0)!!
        assertTrue(f.startsWith("xfade=transition=slideleft"))
        assertTrue(f.contains("duration=1.000"))
        assertTrue(f.contains("offset=3.000"))
    }

    @Test
    fun xfadeFilterIsNullForNoneAndForSubFrameDurations() {
        assertNull(TransitionCatalog.xfadeFilter("none", 1.0, 0.0))
        assertNull(TransitionCatalog.xfadeFilter("fade", 0.0, 0.0))
        assertNull(TransitionCatalog.xfadeFilter("fade", 0.01, 0.0))
    }

    @Test
    fun numbersUseADotDecimalSeparatorRegardlessOfDefaultLocale() {
        // On a comma-decimal locale (de/fr/tr...), naive formatting emits "0,7"
        // and the ENTIRE filter graph fails to parse. Guard against it.
        val original = java.util.Locale.getDefault()
        try {
            java.util.Locale.setDefault(java.util.Locale.GERMANY)
            assertEquals("0.700", TransitionCatalog.fmt(0.7))
            val f = TransitionCatalog.xfadeFilter("fade", 0.7, 2.3)!!
            assertFalse("filter must not contain a comma decimal: $f", f.contains(","))
            assertTrue(f.contains("duration=0.700"))
            assertTrue(f.contains("offset=2.300"))
        } finally {
            java.util.Locale.setDefault(original)
        }
    }

    // ── UI contract ──────────────────────────────────────────────────────

    @Test
    fun everyTransitionOfferedByTheEditorUiIsRealizable() {
        // This list mirrors NextGenEditorScreen's TransitionsPanel. If a
        // transition is added to the UI without a mapping, this fails instead of
        // the transition silently doing nothing at export time.
        val uiTransitions = listOf(
            "fade", "fade_out", "fade_in_out", "crossfade", "dissolve",
            "glitch", "zoom_in", "zoom_out", "zoom_burst", "spin", "wipe",
            "blur", "blur_in", "blur_out", "pixelate", "pixel_in", "mosaic",
            "split", "film_burn", "light_leak", "smoke", "circle", "diamond",
            "heart", "flash", "white_flash", "black_fade", "white_fade",
            "slide_left", "slide_right", "slide_up", "slide_down",
            "rotate_in", "rotate_out", "bounce", "elastic", "spring",
            "typewriter", "wave", "shake", "shake_in", "shake_burst",
            "iris_in", "iris_out", "star_wipe", "clock_wipe", "spiral",
            "glitch_in", "tv_static", "channel_change", "vhs_transition",
            "rgb_glitch", "color_flash", "flip_h", "flip_v", "rotate_3d",
            "swing", "push_left", "push_right", "push_up", "push_down",
            "curtain", "blinds", "checkerboard", "diagonal", "triangle",
            "hexagon", "star", "cross", "ripple", "shatter"
        )
        assertEquals(71, uiTransitions.size)
        val unmapped = uiTransitions.filter { TransitionCatalog.specFor(it) == null }
        assertTrue("UI transitions with no explicit mapping: $unmapped", unmapped.isEmpty())
        val unrealizable = uiTransitions.filterNot { TransitionCatalog.isRealizable(it) }
        assertTrue("UI transitions that cannot render: $unrealizable", unrealizable.isEmpty())
    }
}
