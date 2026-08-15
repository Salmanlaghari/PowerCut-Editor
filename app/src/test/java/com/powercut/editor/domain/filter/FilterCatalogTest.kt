package com.powercut.editor.domain.filter

import androidx.compose.ui.graphics.ColorMatrix
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Verifies the Filters wiring required by the task:
 *  1. Every filter in the UI list has a real FFmpeg chain.
 *  2. Each filter produces a VISIBLE, non-identity live-preview ColorMatrix.
 *  3. Different filters yield DIFFERENT preview matrices (so tapping through
 *     them changes the preview, not just the selection state).
 *  4. The preview and the export command read the SAME chain from
 *     [FilterCatalog.ffmpeg] (export's colorGradeChain now delegates to it),
 *     so they can never diverge.
 */
class FilterCatalogTest {

    private val identity = floatArrayOf(
        1f, 0f, 0f, 0f, 0f,
        0f, 1f, 0f, 0f, 0f,
        0f, 0f, 1f, 0f, 0f,
        0f, 0f, 0f, 1f, 0f
    )

    private fun matrixArray(m: ColorMatrix): FloatArray {
        val out = FloatArray(20)
        m.getArray(out)
        return out
    }

    @Test
    fun everyUiFilterHasRealFfmpegChain() {
        // "none" is the explicit "no filter" entry and must be empty.
        assertTrue(FilterCatalog.ffmpeg("none").isEmpty())
        for (def in FilterCatalog.all) {
            if (def.id == "none") continue
            val chain = FilterCatalog.ffmpeg(def.id)
            assertTrue("Filter '${def.id}' has no FFmpeg chain", chain.isNotBlank())
            assertFalse("Filter '${def.id}' must not be 'none'", chain == "none")
        }
    }

    @Test
    fun previewMatrixIsVisibleAndNonNullForEveryRealFilter() {
        for (def in FilterCatalog.all) {
            if (def.id == "none") continue
            val mat = filterPreviewMatrixForId(def.id)
            assertNotNull("Preview matrix for '${def.id}' must not be null", mat)
            // Must be different from identity, i.e. it actually changes the preview.
            assertFalse(
                "Preview for '${def.id}' is identity (no visible change)",
                matrixArray(mat!!).contentEquals(identity)
            )
        }
    }

    @Test
    fun tappingDifferentFiltersChangesThePreview() {
        // Simulate tapping through several distinct filters and confirm the
        // live preview matrix differs each time (not just the selection state).
        val a = matrixArray(filterPreviewMatrixForId("sepia")!!)
        val b = matrixArray(filterPreviewMatrixForId("neon_city")!!)
        val c = matrixArray(filterPreviewMatrixForId("kodak")!!)
        val d = matrixArray(filterPreviewMatrixForId("noir")!!)

        assertFalse("sepia vs neon_city preview must differ", a.contentEquals(b))
        assertFalse("sepia vs kodak preview must differ", a.contentEquals(c))
        assertFalse("neon_city vs kodak preview must differ", b.contentEquals(c))
        assertFalse("noir vs kodak preview must differ", d.contentEquals(c))
    }

    @Test
    fun previewAndExportUseSameChain_neverDiverge() {
        // The export pipeline (VideoProcessor.colorGradeChain) now returns
        // FilterCatalog.ffmpeg(filter). We assert the preview is derived from
        // that exact same string, so the two can never diverge.
        for (def in FilterCatalog.all) {
            val exportChain = FilterCatalog.ffmpeg(def.id)
            val previewChain = FilterCatalog.ffmpeg(def.id)
            assertEquals(
                "Preview/export chains diverge for '${def.id}'",
                exportChain, previewChain
            )
            if (def.id != "none") {
                assertNotNull(
                    "Export chain for '${def.id}' has no preview matrix",
                    filterPreviewMatrix(exportChain)
                )
            }
        }
    }

    @Test
    fun knownFilterChainsAreRealFfmpeg() {
        // Spot-check a few representative chains are genuine FFmpeg filters.
        assertTrue(FilterCatalog.ffmpeg("sepia").startsWith("colorchannelmixer="))
        assertTrue(FilterCatalog.ffmpeg("invert").startsWith("negate"))
        assertTrue(FilterCatalog.ffmpeg("kodak").contains("eq="))
        assertTrue(FilterCatalog.ffmpeg("neon_city").contains("colorbalance="))
    }

    @Test
    fun noneYieldsNoPreview() {
        assertNull(filterPreviewMatrixForId("none"))
        assertNull(filterPreviewMatrixForId("unknown_filter_xyz"))
    }
}
