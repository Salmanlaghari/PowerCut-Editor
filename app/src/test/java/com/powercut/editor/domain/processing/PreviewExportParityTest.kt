package com.powercut.editor.domain.processing

import com.powercut.editor.data.VideoProject
import org.junit.Assert.*
import org.junit.Test

/**
 * Preview ⇄ export colour-grade parity.
 *
 * The live preview (Phase 1, Step B) and the Media3 Transformer export
 * (Phase 1, Step C) both ask the **same** [Media3EffectPipeline] for their
 * effect list. The preview path calls [Media3EffectPipeline.buildEffects]
 * with the editor's current parameter values; the export path
 * ([Media3TransformerExporter]) calls [Media3EffectPipeline.buildEffectsFromProject].
 * Because `buildEffectsFromProject` simply forwards the project's fields into
 * `buildEffects`, the two lists must be identical: same effect count, the same
 * per-effect parameters, and the same 4x4 matrices byte-for-byte.
 *
 * If this ever stops being true the exported colour grade silently diverges
 * from what the user saw on screen — so this test pins the parity guarantee.
 */
class PreviewExportParityTest {

    private val pipeline = Media3EffectPipeline()

    /**
     * The case the user asked for: a project whose only active adjustments are
     * the image-editor colour controls — brightness / contrast / saturation /
     * temperature. Filter, premium look and curves are off, so both paths must
     * yield exactly one editor effect with identical state and matrix.
     */
    @Test
    fun colorGradeOnly_exportEffectsEqualPreviewEffects() {
        val project = VideoProject(
            selectedFilter = "none",
            activePremiumLook = "none",
            imageEditorBrightness = 40f,
            imageEditorContrast = 1.25f,
            imageEditorSaturation = 1.3f,
            imageEditorTemperature = 25f,
            imageEditorExposure = 0f,
            colorLift = 0f,
            colorGamma = 0f,
            colorGain = 0f
        )

        // Export path (Step C): Media3TransformerExporter calls this.
        val exportEffects = pipeline.buildEffectsFromProject(project)

        // Preview path (Step B): NextGenEditorScreen calls buildEffects with the
        // same values the project holds.
        val previewEffects = pipeline.buildEffects(
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

        assertEffectListsEqual("color-grade-only", exportEffects, previewEffects)
        // Sanity: a single non-default editor effect is what we expect here.
        assertEquals(
            "colour-grade-only should yield exactly one editor effect",
            1,
            exportEffects.size
        )
    }

    /**
     * Belt-and-suspenders: filter + premium look + editor + curves all active.
     * Both paths must still agree on every effect and every matrix element.
     */
    @Test
    fun fullStack_exportEffectsEqualPreviewEffects() {
        val project = VideoProject(
            selectedFilter = "warm",
            activePremiumLook = "hdr_vivid",
            imageEditorBrightness = 15f,
            imageEditorContrast = 1.1f,
            imageEditorSaturation = 1.2f,
            imageEditorTemperature = 20f,
            imageEditorExposure = 5f,
            colorLift = 8f,
            colorGamma = 6f,
            colorGain = 10f
        )

        val exportEffects = pipeline.buildEffectsFromProject(project)
        val previewEffects = pipeline.buildEffects(
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

        assertEffectListsEqual("full-stack", exportEffects, previewEffects)
    }

    /**
     * No colour state at all: both paths must return an empty list — nothing to
     * attach to the preview and nothing to hand to the Transformer.
     */
    @Test
    fun noAdjustments_exportAndPreviewBothEmpty() {
        val project = VideoProject(
            selectedFilter = "none",
            activePremiumLook = "none"
        )

        val exportEffects = pipeline.buildEffectsFromProject(project)
        val previewEffects = pipeline.buildEffects(
            filterId = project.selectedFilter,
            premiumLookId = project.activePremiumLook
        )

        assertTrue("export list should be empty", exportEffects.isEmpty())
        assertTrue("preview list should be empty", previewEffects.isEmpty())
        assertEquals(
            "empty lists must match in size",
            exportEffects.size,
            previewEffects.size
        )
    }

    /**
     * Asserts two effect lists are identical: same size, and for each index the
     * human-readable parameters (brightness / contrast / saturation /
     * temperature / tint) and the full returned 4x4 matrix match. This is the
     * parity guarantee — if any element differs, export diverges from preview.
     */
    private fun assertEffectListsEqual(
        label: String,
        exportEffects: List<ColorEffect>,
        previewEffects: List<ColorEffect>
    ) {
        assertEquals(
            "[$label] effect count must match (export vs preview)",
            exportEffects.size,
            previewEffects.size
        )
        for (i in exportEffects.indices) {
            val exp = exportEffects[i]
            val prev = previewEffects[i]
            assertEquals("[$label][$i] brightness", exp.brightness, prev.brightness, 0f)
            assertEquals("[$label][$i] contrast", exp.contrast, prev.contrast, 0f)
            assertEquals("[$label][$i] saturation", exp.saturation, prev.saturation, 0f)
            assertEquals("[$label][$i] temperature", exp.temperature, prev.temperature, 0f)
            assertEquals("[$label][$i] tint", exp.tint, prev.tint, 0f)

            val expMatrix = exp.getMatrix(0L, false)
            val prevMatrix = prev.getMatrix(0L, false)
            assertEquals(
                "[$label][$i] matrix length",
                expMatrix.size,
                prevMatrix.size
            )
            for (m in expMatrix.indices) {
                assertEquals(
                    "[$label][$i] matrix[$m]",
                    expMatrix[m],
                    prevMatrix[m],
                    0f
                )
            }
        }
    }
}
