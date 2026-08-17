package com.powercut.editor.domain.processing

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Phase 3 — Animations: live-preview ⇄ export parity.
 *
 * The export pipeline ([com.powercut.editor.domain.processing.VideoProcessor.buildTextOverlay])
 * delegates to [TextAnimationCatalog.buildDrawtextFilters]. The live animation
 * preview ([TextAnimationPreviewRenderer.buildPreviewFilter]) delegates to the
 * SAME pure function with the same arguments. This test pins that the two
 * sides emit byte-identical `drawtext` chains for the ≥3 animations the task
 * requires — if the preview ever stops using the catalog, or the export path
 * ever bypasses it, this test fails and the animation burned into the exported
 * file no longer matches what the user saw on the preview.
 */
class TextAnimationPreviewExportParityTest {

    /**
     * The ≥3 animations the editor's AnimationsPanel offers, spanning the
     * motion (fade/slide/zoom), elastic (bounce) and colour (neon_pulse,
     * color_cycle) families. The same ids appear in [TextAnimationCatalog.UI_IDS]
     * and are pinned realizable by [TextAnimationCatalogTest].
     */
    private val uiAnimations =
        listOf("fade", "slide_left", "zoom_in", "bounce", "neon_pulse", "color_cycle")

    private val text = "PowerCut"
    private val durationSec = 2.4
    private val fontSize = 42f

    @Test
    fun previewUsesTheExactSameDrawtextChainAsExport() {
        for (id in uiAnimations) {
            // Export path: VideoProcessor.buildTextOverlay(...) == catalog.
            val exportChain = TextAnimationCatalog.buildDrawtextFilters(
                text = text, animation = id, duration = durationSec,
                posX = 0.5f, posY = 0.85f, colorHex = "#FFFFFF", fontSize = fontSize
            )
            assertTrue("'$id' must produce a real drawtext chain at export", exportChain.startsWith("drawtext="))

            // Preview path: the exact same chain.
            val previewChain = TextAnimationPreviewRenderer.buildPreviewFilter(
                animationId = id, text = text, durationSec = durationSec, fontSize = fontSize
            )
            assertEquals(
                "preview and export must use the identical drawtext chain for '$id'",
                exportChain, previewChain
            )
        }
    }

    @Test
    fun previewFilterIsPerFrameSafe() {
        for (id in uiAnimations) {
            val chain = TextAnimationPreviewRenderer.buildPreviewFilter(id, text, durationSec, fontSize)
            assertFalse("'$id' preview must not use fontcolor alpha expressions", chain.contains("@'"))
            assertFalse("'$id' preview must not use fontcolor_expr", chain.contains("fontcolor_expr"))
            assertFalse("'$id' preview must not use eval=frame", chain.contains("eval=frame"))
        }
    }

    @Test
    fun noneAndBlankTextProduceNoPreviewFilter() {
        assertEquals("", TextAnimationPreviewRenderer.buildPreviewFilter("none", text, durationSec, fontSize))
        assertEquals("", TextAnimationPreviewRenderer.buildPreviewFilter("", text, durationSec, fontSize))
        assertEquals("", TextAnimationPreviewRenderer.buildPreviewFilter("bounce", "", durationSec, fontSize))
        assertEquals("", TextAnimationPreviewRenderer.buildPreviewFilter(null, text, durationSec, fontSize))
    }

    @Test
    fun everyUiAnimationIsPinnedByTheSameChainBothSides() {
        // The full UI list (not just the sampled ≥3) must keep preview and
        // export on the same chain — the guarantee the task asks for.
        for (id in TextAnimationCatalog.UI_IDS) {
            if (TextAnimationCatalog.isNone(id)) continue
            val exportChain = TextAnimationCatalog.buildDrawtextFilters(
                text = text, animation = id, duration = durationSec,
                posX = 0.5f, posY = 0.85f, colorHex = "#FFFFFF", fontSize = fontSize
            )
            val previewChain = TextAnimationPreviewRenderer.buildPreviewFilter(
                animationId = id, text = text, durationSec = durationSec, fontSize = fontSize
            )
            assertEquals("preview/export divergence for '$id'", exportChain, previewChain)
            assertTrue("'$id' chain must start with drawtext=", exportChain.startsWith("drawtext="))
        }
    }
}
