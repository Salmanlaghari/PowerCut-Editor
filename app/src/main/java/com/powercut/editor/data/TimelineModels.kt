package com.powercut.editor.data

import java.util.UUID

enum class TrackType {
    VIDEO, AUDIO, TEXT, STICKER, OVERLAY, EFFECT, TRANSITION
}

data class TimelineClip(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val path: String,
    val type: TrackType,
    val startTimeMs: Long,      // Position on the timeline
    val durationMs: Long,       // Actual duration on timeline (after speed/trim)
    val mediaDurationMs: Long,  // Original duration of the media file
    val trimStartMs: Long = 0L,
    val trimEndMs: Long = mediaDurationMs,
    val speedFactor: Float = 1.0f,
    val layerIndex: Int = 0,
    val isLocked: Boolean = false,
    val isVisible: Boolean = true,
    val isSelected: Boolean = false,
    // Clip-specific properties (can be expanded)
    val volume: Float = 1.0f,
    val opacity: Float = 1.0f,
    val rotation: Float = 0f,
    val scale: Float = 1.0f,
    val posX: Float = 0.5f,
    val posY: Float = 0.5f
)

data class TimelineTrack(
    val id: String = UUID.randomUUID().toString(),
    val type: TrackType,
    val clips: List<TimelineClip> = emptyList(),
    val isLocked: Boolean = false,
    val isVisible: Boolean = true,
    val label: String = type.name
)

data class VideoTimeline(
    val tracks: List<TimelineTrack> = listOf(
        TimelineTrack(type = TrackType.VIDEO, label = "Main Video"),
        TimelineTrack(type = TrackType.AUDIO, label = "Background Music"),
        TimelineTrack(type = TrackType.TEXT, label = "Text & Titles"),
        TimelineTrack(type = TrackType.STICKER, label = "Stickers"),
        TimelineTrack(type = TrackType.OVERLAY, label = "Overlays")
    ),
    val zoomLevel: Float = 1.0f,
    val playheadPosMs: Long = 0L
)
