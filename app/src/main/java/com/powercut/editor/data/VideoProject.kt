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
    val aspectPreset: String = "16:9", // "16:9", "9:16", "1:1", "4:5"
    val transitionType: String = "none", // "none", "fade", "slide", "dissolve"
    val backgroundMusicPath: String? = null,
    val backgroundMusicVolume: Float = 0.5f, // 0.0f to 1.0f
    val videoVolume: Float = 1.0f, // 0.0f to 1.0f
    val autoCaptionsLanguage: String = "off", // "off", "en", "ur"
    val isSilenceRemoverEnabled: Boolean = false
) {
    val isTrimmed: Boolean
        get() = trimStartMs > 0L || trimEndMs < durationMs && trimEndMs > 0L

    val isSpeedChanged: Boolean
        get() = speedFactor != 1.0f

    val hasBackgroundMusic: Boolean
        get() = !backgroundMusicPath.isNullOrBlank()
}
