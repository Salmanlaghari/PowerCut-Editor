package com.powercut.editor.domain.processing

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Phase 2 — Transitions: live-preview ⇄ export parity.
 *
 * The multi-clip export ([com.powercut.editor.domain.processing.VideoProcessor.processMultiClipTimeline])
 * resolves every cut with [TransitionCatalog] ([xfadeNameFor] + [clampDuration]
 * + cumulative offsets). The live transition preview
 * ([TransitionPreviewRenderer]) resolves its cut with the exact same
 * [TransitionCatalog.cutSpecs] math, then emits `[v0][v1]xfade=...`. If the
 * xfade name, clamped duration or cut offset ever diverge between the two
 * paths, the exported transition no longer matches what the user saw on the
 * preview — exactly the guarantee Phase 1's `PreviewExportParityTest` gives
 * for the colour grade.
 */
class TransitionPreviewExportParityTest {

    /**
     * The ≥3 transitions the editor's TransitionsPanel offers, spanning exact
     * (fade/dissolve), geometric (slide/circle/wipe) and approximated (glitch)
     * families. The same ids appear in the editor UI list at
     * `NextGenEditorScreen.TransitionsPanel` and are pinned realizable by
     * [TransitionCatalogTest.everyTransitionOfferedByTheEditorUiIsRealizable].
     */
    private val uiTransitions =
        listOf("fade", "slide_left", "circle", "dissolve", "wipe", "glitch")

    @Test
    fun previewUsesTheSameXfadeNameAsExport() {
        for (id in uiTransitions) {
            // Export path: the xfade name the multi-clip export would emit.
            val exportName = TransitionCatalog.xfadeNameFor(id)
            assertNotNull("'$id' must resolve at export", exportName)

            // Preview path: the same name must appear in the rendered graph.
            val filter = TransitionPreviewRenderer.buildPreviewFilter(id, 1.2, 0.7)
            assertNotNull("'$id' must produce a live preview filter", filter)
            assertTrue(
                "'$id' preview filter must use the export xfade name " +
                    "'$exportName' — got: $filter",
                filter!!.contains("transition=$exportName")
            )
        }
    }

    @Test
    fun previewDurationAndOffsetMatchTheExportCutMath() {
        val segmentSec = 1.2
        val requested = 0.7
        for (id in uiTransitions) {
            // Export path: resolve the cut exactly as processMultiClipTimeline
            // does for two clips of this length.
            val exportCuts = TransitionCatalog.cutSpecs(
                id, listOf(segmentSec, segmentSec), requested
            )
            assertEquals("'$id': two clips must yield exactly one cut", 1, exportCuts.size)
            val cut = exportCuts[0]
            assertNotNull("'$id': the cut must be a real xfade", cut.xfadeName)

            // Preview path: the rendered filter must carry the same numbers.
            val filter = TransitionPreviewRenderer.buildPreviewFilter(
                id, segmentSec, requested
            )!!
            assertTrue(
                "'$id': clamped duration ${cut.durationSec} missing from $filter",
                filter.contains("duration=${TransitionCatalog.fmt(cut.durationSec)}")
            )
            assertTrue(
                "'$id': cut offset ${cut.offsetSec} missing from $filter",
                filter.contains("offset=${TransitionCatalog.fmt(cut.offsetSec)}")
            )
        }
    }

    @Test
    fun previewClampsToHalfTheShorterSegmentExactlyLikeExport() {
        // 0.6s segments cap the transition at 0.3s on BOTH paths.
        val exportCuts = TransitionCatalog.cutSpecs("fade", listOf(0.6, 0.6), 2.0)
        assertEquals(0.3, exportCuts[0].durationSec, 1e-9)
        val filter = TransitionPreviewRenderer.buildPreviewFilter("fade", 0.6, 2.0)!!
        assertTrue("clamped duration missing from $filter", filter.contains("duration=0.300"))
    }

    @Test
    fun noneAndHardCutsProduceNoPreviewFilter() {
        assertNull(TransitionPreviewRenderer.buildPreviewFilter("none", 1.2, 0.7))
        assertNull(TransitionPreviewRenderer.buildPreviewFilter("", 1.2, 0.7))
        assertNull(TransitionPreviewRenderer.buildPreviewFilter("hard_cut", 1.2, 0.7))
    }

    @Test
    fun previewFilterIsAValidTwoInputXfadeGraph() {
        val filter = TransitionPreviewRenderer.buildPreviewFilter("slide_left", 1.2, 0.7)!!
        assertTrue("must join the two normalised inputs", filter.startsWith("[v0][v1]xfade="))
        assertTrue("must feed the composed output", filter.endsWith("[vout]"))
        assertFalse("must never emit the invalid fdissolve name", filter.contains("fdissolve"))
    }
}
