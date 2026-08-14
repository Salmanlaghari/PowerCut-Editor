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
        val catalogId = aliasToCatalogId(featureId)
        val chain = PremiumFeatureCatalog.videoChainFor(catalogId)
        if (chain.isBlank()) {
            viewModel.updateAiFeature("none")
            return false
        }
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
    //  EXPORT QUALITY TOGGLES (HDR + High Bitrate + Pro)
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
    //  VIDEO EFFECTS & COLOR GRADING
    //  Uses the selectedFilter field which maps to FFmpeg -vf chains.
    // ─────────────────────────────────────────────────────────────────────────────

    /**
     * Apply a video effect / color filter (e.g. "vivid", "cinematic", "tealorange").
     * The filterId must match one of the VisualEffect ids from EffectCatalog.
     */
    fun applyVideoEffect(viewModel: EditorViewModel, filterId: String) {
        viewModel.updateFilter(filterId)
    }

    /**
     * Apply a color grading preset.
     * Uses the same selectedFilter field as video effects.
     */
    fun applyColorGrading(viewModel: EditorViewModel, filterId: String) {
        viewModel.updateFilter(filterId)
    }

    // ─────────────────────────────────────────────────────────────────────────────
    //  AUDIO EFFECTS
    //  Uses the audioEffect field which maps to PremiumFeatureCatalog audioChain.
    // ─────────────────────────────────────────────────────────────────────────────

    /**
     * Apply an audio effect (e.g. "au_echo", "au_reverb", "au_chorus").
     */
    fun applyAudioEffect(viewModel: EditorViewModel, effectId: String) {
        viewModel.updateAudioEffect(effectId)
    }

    // ─────────────────────────────────────────────────────────────────────────────
    //  TRANSITIONS
    //  Uses the transitionType field.
    // ─────────────────────────────────────────────────────────────────────────────

    /**
     * Apply a transition type (e.g. "crossfade", "wipe_left", "slide_up").
     */
    fun applyTransition(viewModel: EditorViewModel, transitionId: String) {
        viewModel.updateTransition(transitionId)
    }

    // ─────────────────────────────────────────────────────────────────────────────
    //  RESOLUTION & FPS
    // ─────────────────────────────────────────────────────────────────────────────

    /** Update the export resolution (e.g. "1080p", "720p", "4k"). */
    fun updateResolution(viewModel: EditorViewModel, resolution: String) {
        viewModel.updateResolution(resolution)
    }

    /** Update the frame rate for export (24, 30, 60, 120). */
    fun updateFps(viewModel: EditorViewModel, fps: Int) {
        viewModel.updateTargetFps(fps)
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
    //  TRANSITION IDS
    // ─────────────────────────────────────────────────────────────────────────────
    const val TRANSITION_NONE = "none"
    const val TRANSITION_CROSSFADE = "crossfade"
    const val TRANSITION_WIPE_LEFT = "wipe_left"
    const val TRANSITION_WIPE_RIGHT = "wipe_right"
    const val TRANSITION_SLIDE_UP = "slide_up"
    const val TRANSITION_SLIDE_DOWN = "slide_down"
    const val TRANSITION_ZOOM = "zoom"
    const val TRANSITION_PUSH_LEFT = "push_left"
    const val TRANSITION_PUSH_RIGHT = "push_right"
    const val TRANSITION_ROTATE = "rotate"
    const val TRANSITION_CUBE = "cube"

    // ─────────────────────────────────────────────────────────────────────────────
    //  COLOR GRADING/VIDEO FILTER PRESET IDS
    // ─────────────────────────────────────────────────────────────────────────────
    const val FILTER_NONE = "none"
    const val FILTER_VIVID = "vivid"
    const val FILTER_CINEMATIC = "cinematic"
    const val FILTER_TEALORANGE = "tealorange"
    const val FILTER_NOIR = "noir"
    const val FILTER_VINTAGE = "vintage"
    const val FILTER_FADE = "fade"
    const val FILTER_WARM = "warm"
    const val FILTER_COOL = "cool"
    const val FILTER_PUNCHY = "punchy"
    const val FILTER_MUTED = "muted"
    const val FILTER_LOMO = "lomo"
    const val FILTER_PASTEL = "pastel"
    const val FILTER_MONO = "mono"
    const val FILTER_SEPIA = "sepia"
    const val FILTER_INVERT = "invert"
    const val FILTER_POLAROID = "polaroid"
    const val FILTER_KODAK = "kodak"

    // ─────────────────────────────────────────────────────────────────────────────
    //  AUDIO EFFECT IDS
    // ─────────────────────────────────────────────────────────────────────────────
    const val AUDIO_NONE = "none"
    const val AUDIO_ECHO = "au_echo"
    const val AUDIO_REVERB = "au_reverb"
    const val AUDIO_CHORUS = "au_chorus"
    const val AUDIO_ROOM = "au_room"
    const val AUDIO_VIRTUAL = "au_virtual"
    const val AUDIO_DRUMS = "au_drums"
    const val AUDIO_SILENCE_REMOVER = "au_noise_removal"
    const val AUDIO_VOICE_CHANGER = "au_voice_changer"
    const val AUDIO_DUCKING = "au_ducking"
    const val AUDIO_BATTERY = "au_battery"

    // ─────────────────────────────────────────────────────────────────────────────
    //  ALIAS MAP  —  PremiumOption.id → PremiumFeatureCatalog id
    // ─────────────────────────────────────────────────────────────────────────────
    private fun aliasToCatalogId(displayId: String): String = when (displayId) {
        "ai_auto_stabilize" -> "be_stabilize"
        "ai_super_res" -> "ai_super_res"
        "ai_denoise" -> "ai_denoise"
        "ai_slow_motion" -> "ai_slow_motion"
        "ai_frame_interp" -> "ai_frame_interp"
        "ai_hdr_enhance" -> "be_hdr"
        "ai_sharpen" -> "ai_deblur"
        "ai_bg_remove" -> "ai_bg_remove"
        "ai_bg_blur" -> "ai_enhance"
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