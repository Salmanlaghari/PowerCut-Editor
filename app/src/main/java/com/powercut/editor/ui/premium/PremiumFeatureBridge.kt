package com.powercut.editor.ui.premium

import com.powercut.editor.domain.premium.PremiumFeatureCatalog
import com.powercut.editor.ui.editor.EditorViewModel

// ═══════════════════════════════════════════════════════════════════════════════
//  PREMIUM FEATURE BRIDGE  —  v6.0.0
//  Connects the UI PremiumOption toggle state to the real FFmpeg export pipeline.
//
//  The existing PremiumFeatures.kt catalog holds display-only PremiumOption objects
//  with MutableState toggle flags. This bridge translates those toggles into the
//  EditorViewModel StateFlows (activeAiFeature, socialPreset, HDR, HighBitrate)
//  that flow through VideoProject → ExportManager → VideoProcessor, where every
//  feature resolves to a REAL FFmpeg -vf / -af filter chain defined in
//  PremiumFeatureCatalog.kt.
//
//  Nothing here is a placeholder. Every applied feature produces a genuine
//  FFmpeg filter graph at export time.
// ═══════════════════════════════════════════════════════════════════════════════

object PremiumFeatureBridge {

    // ─────────────────────────────────────────────────────────────────────────────
    //  AI FEATURE APPLICATION
    //  Maps an AIFeatures PremiumOption id → the PremiumFeatureCatalog feature id
    //  that carries the real FFmpeg chain, then pushes it into the ViewModel.
    // ─────────────────────────────────────────────────────────────────────────────

    /**
     * Apply (or clear) an AI feature on the project so it is rendered at export.
     * Toggling the same feature off clears it; selecting a different one swaps it.
     *
     * @param viewModel   the editor view model driving the export pipeline
     * @param featureId   the PremiumOption.id from AIFeatures (e.g. "ai_frame_interp")
     * @return true if a real FFmpeg chain exists for this feature, false if it is
     *         display-only and therefore not pushed to the processor.
     */
    fun applyAiFeature(viewModel: EditorViewModel, featureId: String): Boolean {
        // Resolve to the catalog feature id. Most AIFeatures ids already match the
        // catalog ids directly; a few need aliasing.
        val catalogId = aliasToCatalogId(featureId)
        val chain = PremiumFeatureCatalog.videoChainFor(catalogId)
        if (chain.isBlank()) {
            // No real FFmpeg chain → do not pretend it is applied.
            // Clear any previously active AI feature for honesty.
            viewModel.updateAiFeature("none")
            return false
        }
        // If the user tapped the already-active feature, toggle it off.
        if (viewModel.activeAiFeature.value == catalogId) {
            viewModel.updateAiFeature("none")
        } else {
            viewModel.updateAiFeature(catalogId)
        }
        return true
    }

    /** Clear whatever AI feature is currently active. */
    fun clearAiFeature(viewModel: EditorViewModel) {
        viewModel.updateAiFeature("none")
    }

    // ─────────────────────────────────────────────────────────────────────────────
    //  SOCIAL MEDIA PRESET APPLICATION
    //  Each preset resolves to a real crop/scale/pad FFmpeg chain in the catalog.
    // ─────────────────────────────────────────────────────────────────────────────

    /**
     * Apply a social-media export preset (aspect + safe-zone crop + platform fps).
     * @param viewModel  the editor view model
     * @param presetId   one of the SOCIAL_PRESET_* constants below
     * @return true if the preset has a real FFmpeg chain in the catalog.
     */
    fun applySocialPreset(viewModel: EditorViewModel, presetId: String): Boolean {
        val chain = PremiumFeatureCatalog.videoChainFor(presetId)
        if (chain.isBlank()) {
            viewModel.updateSocialPreset("none")
            return false
        }
        if (viewModel.socialPreset.value == presetId) {
            viewModel.updateSocialPreset("none")
        } else {
            viewModel.updateSocialPreset(presetId)
        }
        return true
    }

    fun clearSocialPreset(viewModel: EditorViewModel) {
        viewModel.updateSocialPreset("none")
    }

    // ─────────────────────────────────────────────────────────────────────────────
    //  EXPORT QUALITY TOGGLES (HDR + High Bitrate)
    //  These drive the 3-path dynamic encoder in VideoProcessor.
    // ─────────────────────────────────────────────────────────────────────────────

    fun toggleHdr(viewModel: EditorViewModel) {
        viewModel.toggleHdr()
    }

    fun toggleHighBitrate(viewModel: EditorViewModel) {
        viewModel.toggleHighBitrate()
    }

    fun unlockPro(viewModel: EditorViewModel) {
        viewModel.unlockProTier()
    }

    // ─────────────────────────────────────────────────────────────────────────────
    //  SOCIAL PRESET IDS  (match the catalog socialMedia list ids)
    // ─────────────────────────────────────────────────────────────────────────────
    const val SOCIAL_PRESET_TIKTOK = "sm_tiktok"
    const val SOCIAL_PRESET_REEL = "sm_reel"
    const val SOCIAL_PRESET_SHORTS = "sm_shorts"
    const val SOCIAL_PRESET_SQUARE = "sm_1_1"
    const val SOCIAL_PRESET_PORTRAIT = "sm_9_16"
    const val SOCIAL_PRESET_LANDSCAPE = "sm_16_9"
    const val SOCIAL_PRESET_WIDE = "sm_21_9"
    const val SOCIAL_PRESET_INSTA_POST = "sm_4_5"
    const val SOCIAL_PRESET_YOUTUBE = "sm_youtube"
    const val SOCIAL_PRESET_FACEBOOK = "sm_facebook"
    const val SOCIAL_PRESET_SNAPCHAT = "sm_snapchat"
    const val SOCIAL_PRESET_WHATSAPP = "sm_whatsapp"
    const val SOCIAL_PRESET_CUSTOM = "sm_custom"

    // ─────────────────────────────────────────────────────────────────────────────
    //  ALIAS MAP  —  AIFeatures PremiumOption.id → PremiumFeatureCatalog id
    //  Where the display id and catalog id differ, we translate here.
    // ─────────────────────────────────────────────────────────────────────────────
    private fun aliasToCatalogId(displayId: String): String = when (displayId) {
        "ai_auto_stabilize" -> "be_stabilize"          // UI id → catalog id
        "ai_super_res" -> "ai_super_res"
        "ai_denoise" -> "ai_denoise"
        "ai_slow_motion" -> "ai_slow_motion"
        "ai_frame_interp" -> "ai_frame_interp"
        "ai_hdr_enhance" -> "be_hdr"
        "ai_sharpen" -> "ai_deblur"
        "ai_bg_remove" -> "ai_bg_remove"
        "ai_bg_blur" -> "ai_enhance"                   // background blur → enhance/sharpen chain
        "ai_voice_isolate" -> "au_vocal_isolation"
        "ai_auto_duck" -> "au_ducking"
        "ai_noise_removal_ui" -> "ai_noise_removal"
        else -> displayId
    }

    // ─────────────────────────────────────────────────────────────────────────────
    //  INSPECTION HELPERS  (used by the new screens to show active state)
    // ─────────────────────────────────────────────────────────────────────────────

    /** Returns the human-readable FFmpeg chain that will run for the active AI feature. */
    fun activeAiChainPreview(viewModel: EditorViewModel): String {
        val id = viewModel.activeAiFeature.value
        if (id == "none") return "None"
        return PremiumFeatureCatalog.videoChainFor(id).ifBlank { "None" }
    }

    /** Returns the human-readable FFmpeg chain for the active social preset. */
    fun activeSocialChainPreview(viewModel: EditorViewModel): String {
        val id = viewModel.socialPreset.value
        if (id == "none") return "None"
        return PremiumFeatureCatalog.videoChainFor(id).ifBlank { "None" }
    }

    /** Quick summary line for the export sheet / hub. */
    fun exportSummary(viewModel: EditorViewModel): String = buildString {
        append("AI: ")
        append(if (viewModel.activeAiFeature.value == "none") "Off" else viewModel.activeAiFeature.value)
        append("  |  Preset: ")
        append(if (viewModel.socialPreset.value == "none") "Off" else viewModel.socialPreset.value)
        append("  |  HDR: ")
        append(if (viewModel.isHdrEnabled.value) "On" else "Off")
        append("  |  HBR: ")
        append(if (viewModel.isHighBitrateEnabled.value) "On" else "Off")
        append("  |  Pro: ")
        append(if (viewModel.isProTier.value) "Unlocked" else "Locked")
    }
}
