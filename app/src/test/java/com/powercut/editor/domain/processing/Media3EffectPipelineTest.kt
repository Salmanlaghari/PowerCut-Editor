package com.powercut.editor.domain.processing

import com.powercut.editor.data.VideoProject
import com.powercut.editor.domain.filter.FilterCatalog
import com.powercut.editor.domain.look.PremiumLooks
import org.junit.Test
import org.junit.Assert.*

/**
 * Unit tests for Media3EffectPipeline - Phase 1, Step A Foundation.
 * Tests that the pipeline correctly builds RgbAdjustment effects for
 * various filter/look combinations.
 */
class Media3EffectPipelineTest {

    private val pipeline = Media3EffectPipeline()

    @Test
    fun testBuildEffects_noneFilterAndLook_returnsEmpty() {
        val effects = pipeline.buildEffects(
            filterId = "none",
            premiumLookId = "none"
        )
        assertTrue("Should return empty list for 'none' filter and look", effects.isEmpty())
    }

    @Test
    fun testBuildEffects_warmFilter_createsRgbAdjustment() {
        val effects = pipeline.buildEffects(
            filterId = "warm",
            premiumLookId = "none"
        )
        assertFalse("Should create at least one effect for 'warm' filter", effects.isEmpty())
        val effect = effects[0]
        assertNotNull(effect)
        // Warm filter should have positive temperature (warmth)
        assertTrue("Warm filter should have positive temperature", effect.temperature > 0f)
        assertTrue("Warm filter should have slight saturation boost", effect.saturation > 1f)
    }

    @Test
    fun testBuildEffects_coolFilter_createsRgbAdjustment() {
        val effects = pipeline.buildEffects(
            filterId = "cool",
            premiumLookId = "none"
        )
        assertFalse("Should create at least one effect for 'cool' filter", effects.isEmpty())
        val effect = effects[0]
        assertNotNull(effect)
        // Cool filter should have negative temperature (coolness)
        assertTrue("Cool filter should have negative temperature", effect.temperature < 0f)
    }

    @Test
    fun testBuildEffects_grayscaleFilter_createsRgbAdjustment() {
        val effects = pipeline.buildEffects(
            filterId = "grayscale",
            premiumLookId = "none"
        )
        assertFalse("Should create effect for 'grayscale' filter", effects.isEmpty())
        val effect = effects[0]
        assertNotNull(effect)
        // Grayscale should have saturation = 0
        assertEquals("Grayscale should have zero saturation", 0f, effect.saturation, 0.01f)
    }

    @Test
    fun testBuildEffects_cinematicFilter_createsRgbAdjustment() {
        val effects = pipeline.buildEffects(
            filterId = "cinematic",
            premiumLookId = "none"
        )
        assertFalse("Should create effect for 'cinematic' filter", effects.isEmpty())
        val effect = effects[0]
        assertNotNull(effect)
        // Cinematic typically has reduced saturation, increased contrast
        assertTrue("Cinematic should have contrast > 1", effect.contrast > 1f)
        assertTrue("Cinematic should have saturation < 1", effect.saturation < 1f)
    }

    @Test
    fun testBuildEffects_premiumLook_hdrVivid_createsRgbAdjustment() {
        val effects = pipeline.buildEffects(
            filterId = "none",
            premiumLookId = "hdr_vivid"
        )
        assertFalse("Should create effect for 'hdr_vivid' look", effects.isEmpty())
        val effect = effects[0]
        assertNotNull(effect)
        // HDR Vivid should have high contrast and saturation
        assertTrue("HDR Vivid should have high contrast", effect.contrast > 1.2f)
        assertTrue("HDR Vivid should have high saturation", effect.saturation > 1.2f)
    }

    @Test
    fun testBuildEffects_premiumLook_iphoneCinematic_createsRgbAdjustment() {
        val effects = pipeline.buildEffects(
            filterId = "none",
            premiumLookId = "iphone_cinematic"
        )
        assertFalse("Should create effect for 'iphone_cinematic' look", effects.isEmpty())
        val effect = effects[0]
        assertNotNull(effect)
        // iPhone Cinematic should have increased contrast
        assertTrue("iPhone Cinematic should have contrast > 1", effect.contrast > 1f)
    }

    @Test
    fun testBuildEffects_imageEditorAdjustments_createsRgbAdjustment() {
        val effects = pipeline.buildEffects(
            filterId = "none",
            premiumLookId = "none",
            imageEditorBrightness = 50f,
            imageEditorContrast = 1.2f,
            imageEditorSaturation = 1.3f,
            imageEditorTemperature = 30f,
            imageEditorExposure = 20f
        )
        assertFalse("Should create effect for image editor adjustments", effects.isEmpty())
        val effect = effects[0]
        assertNotNull(effect)
        assertTrue("Should have positive brightness", effect.brightness > 0f)
        assertEquals("Should have contrast 1.2", 1.2f, effect.contrast, 0.01f)
        assertEquals("Should have saturation 1.3", 1.3f, effect.saturation, 0.01f)
        assertEquals("Should have temperature 30", 30f, effect.temperature, 0.01f)
    }

    @Test
    fun testBuildEffects_colorCurves_createsRgbAdjustment() {
        val effects = pipeline.buildEffects(
            filterId = "none",
            premiumLookId = "none",
            colorLift = 20f,
            colorGamma = 10f,
            colorGain = 15f
        )
        assertFalse("Should create effect for color curves", effects.isEmpty())
        val effect = effects[0]
        assertNotNull(effect)
        // Lift + gain should increase brightness
        assertTrue("Should have positive brightness from lift+gain", effect.brightness > 0f)
        // Gamma should increase contrast
        assertTrue("Should have contrast > 1 from gamma", effect.contrast > 1f)
    }

    @Test
    fun testBuildEffects_combinedFilterAndLook_createsMultipleEffects() {
        val effects = pipeline.buildEffects(
            filterId = "warm",
            premiumLookId = "hdr_vivid",
            imageEditorBrightness = 10f
        )
        // Should have: filter effect + look effect + editor effect = 3 effects
        assertEquals("Should have 3 effects (filter + look + editor)", 3, effects.size)
    }

    @Test
    fun testBuildEffectsFromProject_usesProjectState() {
        val project = VideoProject(
            selectedFilter = "warm",
            activePremiumLook = "hdr_vivid",
            imageEditorBrightness = 25f,
            imageEditorContrast = 1.1f,
            imageEditorSaturation = 1.2f,
            imageEditorTemperature = 15f,
            imageEditorExposure = 10f,
            colorLift = 5f,
            colorGamma = 5f,
            colorGain = 5f
        )

        val effects = pipeline.buildEffectsFromProject(project)
        assertFalse("Should create effects from project", effects.isEmpty())
        // Filter + Look + Editor + Curves = 4 effects
        assertEquals("Should have 4 effects", 4, effects.size)
    }

    @Test
    fun testParseFfmpegChain_parsesEqAndColorbalance() {
        // Test via the public buildEffects method which internally parses the chain
        val effects = pipeline.buildEffects(
            filterId = "warm", // warm has eq and colorbalance
            premiumLookId = "none"
        )
        assertFalse("Should create effect for warm filter", effects.isEmpty())
        val effect = effects[0]
        assertNotNull(effect)
        // Warm filter from FilterCatalog: eq=saturation=1.1,colorbalance=rs=0.08:gs=0.02:rm=0.05
        assertTrue("Warm should have saturation > 1", effect.saturation > 1f)
        assertTrue("Warm should have positive temperature", effect.temperature > 0f)
    }

    @Test
    fun testFilterCatalog_containsKnownFilters() {
        // Verify FilterCatalog has the filters we test against
        assertTrue(FilterCatalog.isReal("warm"))
        assertTrue(FilterCatalog.isReal("cool"))
        assertTrue(FilterCatalog.isReal("grayscale"))
        assertTrue(FilterCatalog.isReal("cinematic"))
        assertTrue(FilterCatalog.isReal("vintage"))
        assertTrue(FilterCatalog.isReal("noir"))
    }

    @Test
    fun testPremiumLooks_containsKnownLooks() {
        // Verify PremiumLooks has the looks we test against
        assertNotNull(PremiumLooks.byId("hdr_vivid"))
        assertNotNull(PremiumLooks.byId("iphone_cinematic"))
        assertNotNull(PremiumLooks.byId("bright_lift"))
        assertNotNull(PremiumLooks.byId("cinema_teal"))
    }
}