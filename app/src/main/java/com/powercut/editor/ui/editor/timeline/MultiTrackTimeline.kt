package com.powercut.editor.ui.editor.timeline

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.powercut.editor.data.TimelineClip
import com.powercut.editor.data.TrackType as DataTrackType

// ── Colors ──
private val BgDark = Color(0xFF0A0A0F)
private val TrackBg = Color(0xFF141420)
private val TrackBorder = Color(0xFF1E1E2E)
private val PlayheadRed = Color(0xFFFF3B30)
private val ClipVideo = Color(0xFF1A3A5C)
private val ClipAudio = Color(0xFF0D4A4A)
private val ClipText = Color(0xFF3A1A4A)
private val TextWhite = Color(0xFFFFFFFF)
private val TextGray = Color(0xFF7A7A8E)
private val AccentCyan = Color(0xFF00D4FF)
private val KeyframeOrange = Color(0xFFFF9500)
private val BeatMarker = Color(0xFFFF9500)

// Use existing TimelineClip and TrackType from data package
// TrackType mapping for display labels
private fun DataTrackType.displayName(): String = when (this) {
    DataTrackType.VIDEO -> "V1"
    DataTrackType.AUDIO -> "A1"
    DataTrackType.TEXT -> "Text"
    DataTrackType.STICKER -> "Sticker"
    DataTrackType.OVERLAY -> "Overlay"
    DataTrackType.EFFECT -> "Effect"
    DataTrackType.TRANSITION -> "Transition"
}

private fun DataTrackType.displayIcon(): androidx.compose.ui.graphics.vector.ImageVector = when (this) {
    DataTrackType.VIDEO -> Icons.Default.Videocam
    DataTrackType.AUDIO -> Icons.Default.MusicNote
    DataTrackType.TEXT -> Icons.Default.TextFields
    DataTrackType.STICKER -> Icons.Default.EmojiEmotions
    DataTrackType.OVERLAY -> Icons.Default.Layers
    DataTrackType.EFFECT -> Icons.Default.AutoAwesome
    DataTrackType.TRANSITION -> Icons.Default.Transition
}

private fun DataTrackType.clipColor(): Color = when (this) {
    DataTrackType.VIDEO -> ClipVideo
    DataTrackType.AUDIO -> ClipAudio
    DataTrackType.TEXT -> ClipText
    else -> ClipVideo
}

@Composable
fun MultiTrackTimeline(
    clips: List<TimelineClip>,
    currentTimeMs: Long,
    totalDurationMs: Long,
    onSeek: (Long) -> Unit,
    selectedClipId: String?,
    onClipSelected: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    val pixelsPerMs = 0.15f // zoom level

    // Get unique track types from clips
    val trackTypes = clips.map { it.type }.distinct()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(BgDark)
    ) {
        // Time ruler
        TimeRuler(
            totalDurationMs = totalDurationMs,
            currentTimeMs = currentTimeMs,
            pixelsPerMs = pixelsPerMs
        )

        // Tracks
        Box(modifier = Modifier.fillMaxWidth()) {
            // Track rows
            Column {
                trackTypes.forEach { trackType ->
                    TrackRow(
                        trackType = trackType,
                        clips = clips.filter { it.type == trackType },
                        currentTimeMs = currentTimeMs,
                        totalDurationMs = totalDurationMs,
                        pixelsPerMs = pixelsPerMs,
                        selectedClipId = selectedClipId,
                        onClipSelected = onClipSelected
                    )
                }
            }

            // Red playhead
            Playhead(
                currentTimeMs = currentTimeMs,
                pixelsPerMs = pixelsPerMs
            )
        }
    }
}

@Composable
private fun TimeRuler(
    totalDurationMs: Long,
    currentTimeMs: Long,
    pixelsPerMs: Float
) {
    val totalWidth = (totalDurationMs * pixelsPerMs).dp

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(24.dp)
            .background(BgDark)
            .horizontalScroll(rememberScrollState()),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Time markers
        val intervalMs = 10000L // 10 second intervals
        var t = 0L
        while (t <= totalDurationMs) {
            Box(
                modifier = Modifier
                    .width((t * pixelsPerMs).dp)
                    .height(0.dp)
            )
            Text(
                text = formatTimeShort(t),
                color = TextGray,
                fontSize = 10.sp,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
            Spacer(modifier = Modifier.width((intervalMs * pixelsPerMs - 40).dp.coerceAtLeast(0.dp)))
            t += intervalMs
        }
    }
}

@Composable
private fun TrackRow(
    trackType: DataTrackType,
    clips: List<TimelineClip>,
    currentTimeMs: Long,
    totalDurationMs: Long,
    pixelsPerMs: Float,
    selectedClipId: String?,
    onClipSelected: (String?) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .border(0.5.dp, TrackBorder)
    ) {
        // Track label
        Box(
            modifier = Modifier
                .width(40.dp)
                .fillMaxHeight()
                .background(TrackBg)
                .padding(4.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = trackType.displayName(),
                color = TextGray,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
        }

        // Clips area
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .horizontalScroll(rememberScrollState())
        ) {
            Row {
                clips.forEach { clip ->
                    ClipCard(
                        clip = clip,
                        pixelsPerMs = pixelsPerMs,
                        isSelected = clip.id == selectedClipId,
                        onClick = { onClipSelected(if (selectedClipId == clip.id) null else clip.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ClipCard(
    clip: TimelineClip,
    pixelsPerMs: Float,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val width = (clip.durationMs * pixelsPerMs).dp.coerceIn(60.dp, 400.dp)
    val clipColor = clip.type.clipColor()

    Box(
        modifier = Modifier
            .width(width)
            .fillMaxHeight()
            .padding(horizontal = 1.dp, vertical = 2.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(
                Brush.horizontalGradient(
                    colors = listOf(
                        clipColor,
                        clipColor.copy(alpha = 0.7f)
                    )
                )
            )
            .border(
                width = if (isSelected) 2.dp else 0.dp,
                color = if (isSelected) AccentCyan else Color.Transparent,
                shape = RoundedCornerShape(4.dp)
            )
            .clickable(onClick = onClick)
            .padding(4.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Column {
            Text(
                text = clip.name,
                color = TextWhite,
                fontSize = 9.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun Playhead(
    currentTimeMs: Long,
    pixelsPerMs: Float
) {
    val infiniteTransition = rememberInfiniteTransition(label = "playhead")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "playheadAlpha"
    )

    Box(
        modifier = Modifier
            .fillMaxHeight()
            .width(2.dp)
            .offset(x = (currentTimeMs * pixelsPerMs).dp)
            .background(PlayheadRed.copy(alpha = alpha))
    ) {
        // Playhead handle
        Box(
            modifier = Modifier
                .size(12.dp)
                .align(Alignment.TopCenter)
                .clip(RoundedCornerShape(2.dp))
                .background(PlayheadRed)
        )
    }
}

private fun formatTimeShort(ms: Long): String {
    val totalSecs = ms / 1000
    val minutes = totalSecs / 60
    val seconds = totalSecs % 60
    return String.format("%d:%02d", minutes, seconds)
}
