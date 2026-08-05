package com.powercut.editor.data

data class VideoProject(
    val videoPath: String,
    val durationMs: Long = 0L,
    val trimStartMs: Long = 0L,
    val trimEndMs: Long = 0L,
    val targetResolution: String = "1080p",
    val selectedFilter: String = "none",
    val isMuted: Boolean = false,

    // High-priority features
    val speedFactor: Float = 1.0f,
    val aspectPreset: String = "16:9",
    val transitionType: String = "none",
    val backgroundMusicPath: String? = null,
    val backgroundMusicVolume: Float = 0.5f,
    val videoVolume: Float = 1.0f,
    val autoCaptionsLanguage: String = "off",
    val isSilenceRemoverEnabled: Boolean = false,

    // Professional Editing Features
    val rotationDegrees: Float = 0f,
    val isFlippedHorizontal: Boolean = false,
    val isFlippedVertical: Boolean = false,
    val cropPreset: String = "free",
    val speedCurve: String = "constant",
    val activeTextOverlay: String? = null,
    val textAnimationType: String = "fade",
    val textStyleId: String = "classic",
    val textPositionX: Float = 0.5f,
    val textPositionY: Float = 0.85f,
    val textColorHex: String = "#FFFFFF",
    val textFontSize: Float = 24f,
    val stickerType: String = "none",

    // Advanced Exclusive features
    val activeTemplateId: String = "none",
    val visualizerStyle: String = "none",
    val isBeatSyncEnabled: Boolean = false,
    val active3DShapeMask: String = "none",

    // NextGen Pro features
    val imageOverlayPath: String? = null,
    val imageOverlayOpacity: Float = 1.0f,
    val imageOverlayScale: Float = 1.0f,
    val imageOverlayX: Float = 0.5f,
    val imageOverlayY: Float = 0.5f,
    val selectedEffect: String = "none",
    val activeLayers: List<String> = emptyList(),

    // Green Screen / Chroma Key
    val greenScreenEnabled: Boolean = false,
    val greenScreenColor: String = "green",
    val greenScreenThreshold: Float = 0.4f,
    val greenScreenBackgroundPath: String? = null,
    val greenScreenAutoBgIndex: Int = -1,

    // Eraser Tools
    val eraserMode: String = "none",
    val eraserBrushSize: Float = 30f,
    val eraserTolerance: Float = 0.5f,
    val eraserSoftEdge: Boolean = true,

    // Image Editor
    val imageEditorBrightness: Float = 0f,
    val imageEditorContrast: Float = 1f,
    val imageEditorSaturation: Float = 1f,
    val imageEditorBlur: Float = 0f,
    val imageEditorSharpen: Float = 0f,
    val imageEditorTemperature: Float = 0f,
    val imageEditorVignette: Float = 0f,
    val imageEditorGrain: Float = 0f,
    val imageEditorFade: Float = 0f,
    val imageEditorHighlights: Float = 0f,
    val imageEditorShadows: Float = 0f,
    val imageEditorExposure: Float = 0f,

    // Video Orientation Tools
    val orientationMode: String = "free",
    val verticalSafeZone: Boolean = false,
    val horizontalLetterbox: Boolean = false,
    val autoReframeEnabled: Boolean = false,

    // ── v4.4.0 Premium Looks (50+ Brightness / HDR / iPhone grades) ──
    val activePremiumLook: String = "none",

    // ── NEW v4.0 CapCut-sync Pro features ──
    val blendMode: String = "none",
    val isReverseEnabled: Boolean = false,
    val freezeFrameMs: Long = 0L,
    val colorLift: Float = 0f,
    val colorGamma: Float = 0f,
    val colorGain: Float = 0f,
    val audioEffect: String = "none",
    val voiceChangerPitch: Float = 0f,
    val isAudioDuckingEnabled: Boolean = false,
    val borderStyle: String = "none",
    val watermarkPath: String? = null,
    val vignetteStyle: String = "none",

    // ── v6.0.0 PREMIUM EXPORT features ──
    /** Target frame rate for export (24/30/60/120). Replaces the old hardcoded fps=30. */
    val targetFps: Int = 30,
    /** 10-bit HDR (BT.2020 PQ) export pipeline. */
    val isHdrEnabled: Boolean = false,
    /** High-bitrate visually-lossless export (lower CRF + higher maxrate). */
    val isHighBitrateEnabled: Boolean = false,
    /** Batch export queue membership (Pro). */
    val isBatchExport: Boolean = false,

    // ── v6.0.0 AI feature pipeline ──
    /** Active AI feature id from PremiumFeatureCatalog (e.g. "ai_frame_interp"). "none" = disabled. */
    val activeAiFeature: String = "none",

    // ── v6.0.0 Social media export preset ──
    /** Social preset id from PremiumFeatureCatalog (e.g. "sm_tiktok"). "none" = use aspectPreset. */
    val socialPreset: String = "none",

    // ── v6.0.0 Pro tier ──
    /** Whether the user has unlocked Pro tier (disables watermark, unlocks Pro assets). */
    val isProTier: Boolean = false
) {
    val isTrimmed: Boolean
        get() = trimStartMs > 0L || trimEndMs < durationMs && trimEndMs > 0L

    val isSpeedChanged: Boolean
        get() = speedFactor != 1.0f

    val hasBackgroundMusic: Boolean
        get() = !backgroundMusicPath.isNullOrBlank()

    val isGreenScreenActive: Boolean
        get() = greenScreenEnabled

    val isEraserActive: Boolean
        get() = eraserMode != "none"

    val isImageEditorActive: Boolean
        get() = imageEditorBrightness != 0f || imageEditorContrast != 1f || imageEditorSaturation != 1f ||
                imageEditorBlur != 0f || imageEditorSharpen != 0f || imageEditorTemperature != 0f ||
                imageEditorVignette != 0f || imageEditorGrain != 0f || imageEditorFade != 0f ||
                imageEditorExposure != 0f

    val isVerticalMode: Boolean
        get() = orientationMode == "vertical" || aspectPreset == "9:16"

    val isHorizontalMode: Boolean
        get() = orientationMode == "horizontal" || aspectPreset == "16:9"

    val isBlendModeActive: Boolean
        get() = blendMode != "none"

    val isReversed: Boolean
        get() = isReverseEnabled

    val hasFreezeFrame: Boolean
        get() = freezeFrameMs > 0L

    val isColorCurvesActive: Boolean
        get() = colorLift != 0f || colorGamma != 0f || colorGain != 0f

    val isAudioEffectActive: Boolean
        get() = audioEffect != "none"

    val isVoiceChanged: Boolean
        get() = voiceChangerPitch != 0f

    // ── v6.0.0 computed properties ──
    val isAiFeatureActive: Boolean
        get() = activeAiFeature != "none"

    val isHdrExport: Boolean
        get() = isHdrEnabled

    val isHighBitrate: Boolean
        get() = isHighBitrateEnabled

    val hasSocialPreset: Boolean
        get() = socialPreset != "none"

    val isAudioDuckingActive: Boolean
        get() = isAudioDuckingEnabled

    val isBorderStyleActive: Boolean
        get() = borderStyle != "none"

    val hasWatermark: Boolean
        get() = !watermarkPath.isNullOrBlank()

    val isVignetteStyleActive: Boolean
        get() = vignetteStyle != "none"

    /** v4.4.0: true when a premium Brightness/HDR/iPhone look is active. */
    val isPremiumLookActive: Boolean
        get() = activePremiumLook != "none" && activePremiumLook.isNotBlank()
}
