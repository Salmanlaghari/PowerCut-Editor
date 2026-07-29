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
    val activeLayers: List<String> = emptyList() // Multi-layer support
) {
    val isTrimmed: Boolean
        get() = trimStartMs > 0L || trimEndMs < durationMs && trimEndMs > 0L

    val isSpeedChanged: Boolean
        get() = speedFactor != 1.0f

    val hasBackgroundMusic: Boolean
        get() = !backgroundMusicPath.isNullOrBlank()
}
