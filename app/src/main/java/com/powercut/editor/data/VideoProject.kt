package com.powercut.editor.data

data class VideoProject(
    val videoPath: String,
    val durationMs: Long = 0L,
    val trimStartMs: Long = 0L,
    val trimEndMs: Long = 0L,
    val targetResolution: String = "1080p", // "1080p", "4k", "8k"
    val selectedFilter: String = "none",   // "none", "sepia", "grayscale", "invert"
    val isMuted: Boolean = false,

    // High-priority features
    val speedFactor: Float = 1.0f, // 0.1x to 16.0x
    val aspectPreset: String = "16:9", // "16:9", "9:16", "1:1", "4:5", "custom"
    val transitionType: String = "none", // 50+ cinematic transitions supported
    val backgroundMusicPath: String? = null,
    val backgroundMusicVolume: Float = 0.5f, // 0.0f to 1.0f
    val videoVolume: Float = 1.0f, // 0.0f to 1.0f
    val autoCaptionsLanguage: String = "off", // "off", "en", "ur"
    val isSilenceRemoverEnabled: Boolean = false,

    // Professional Editing Features
    val rotationDegrees: Float = 0f, // 0, 90, 180, 270
    val isFlippedHorizontal: Boolean = false,
    val isFlippedVertical: Boolean = false,
    val cropPreset: String = "free", // "free", "16:9", "9:16", "1:1", "4:5"
    val speedCurve: String = "constant", // "constant", "montage", "hero", "flash"
    val activeTextOverlay: String? = null,
    val textAnimationType: String = "fade",
    val stickerType: String = "none", // Overlays & stickers

    // Advanced Exclusive features
    val activeTemplateId: String = "none", // 50+ templates
    val visualizerStyle: String = "none", // Audio visualizer neon wave styles
    val isBeatSyncEnabled: Boolean = false,
    val active3DShapeMask: String = "none", // 50+ shape masks

    // NextGen Pro features
    val imageOverlayPath: String? = null, // Image overlay on video
    val imageOverlayOpacity: Float = 1.0f,
    val imageOverlayScale: Float = 1.0f,
    val imageOverlayX: Float = 0.5f, // 0-1 normalized
    val imageOverlayY: Float = 0.5f,
    val selectedEffect: String = "none", // Active visual effect
    val activeLayers: List<String> = emptyList(), // Multi-layer support

    // Green Screen / Chroma Key
    val greenScreenEnabled: Boolean = false,
    val greenScreenColor: String = "green", // "green", "blue", "black", "white", "custom"
    val greenScreenThreshold: Float = 0.4f, // 0.0-1.0 sensitivity
    val greenScreenBackgroundPath: String? = null, // replacement background
    val greenScreenAutoBgIndex: Int = -1, // index into 20+ auto backgrounds

    // Eraser Tools
    val eraserMode: String = "none", // "none", "background", "object", "area"
    val eraserBrushSize: Float = 30f, // brush size in pixels
    val eraserTolerance: Float = 0.5f, // color tolerance 0-1
    val eraserSoftEdge: Boolean = true,

    // Image Editor
    val imageEditorBrightness: Float = 0f, // -1 to 1
    val imageEditorContrast: Float = 1f, // 0 to 2
    val imageEditorSaturation: Float = 1f, // 0 to 2
    val imageEditorBlur: Float = 0f, // 0 to 25
    val imageEditorSharpen: Float = 0f, // 0 to 1
    val imageEditorTemperature: Float = 0f, // -1 to 1 (cool/warm)
    val imageEditorVignette: Float = 0f, // 0 to 1
    val imageEditorGrain: Float = 0f, // 0 to 1
    val imageEditorFade: Float = 0f, // 0 to 1
    val imageEditorHighlights: Float = 0f, // -1 to 1
    val imageEditorShadows: Float = 0f, // -1 to 1
    val imageEditorExposure: Float = 0f, // -1 to 1

    // Video Orientation Tools
    val orientationMode: String = "free", // "free", "vertical", "horizontal", "square"
    val verticalSafeZone: Boolean = false, // show safe zone guides for 9:16
    val horizontalLetterbox: Boolean = false, // letterbox for 16:9
    val autoReframeEnabled: Boolean = false // AI auto-reframe for target orientation
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
} {
    val isTrimmed: Boolean
        get() = trimStartMs > 0L || trimEndMs < durationMs && trimEndMs > 0L

    val isSpeedChanged: Boolean
        get() = speedFactor != 1.0f

    val hasBackgroundMusic: Boolean
        get() = !backgroundMusicPath.isNullOrBlank()
}
