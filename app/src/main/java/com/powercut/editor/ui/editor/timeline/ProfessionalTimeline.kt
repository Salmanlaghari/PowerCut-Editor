package com.powercut.editor.ui.editor.timeline

import androidx.compose.ui.graphics.toArgb
import android.graphics.Paint
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.powercut.editor.data.*
import com.powercut.editor.ui.theme.*
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToLong

/**
 * PowerCut Professional Timeline
 * A high-performance, interactive multi-track timeline for professional video editing.
 */
@Composable
fun ProfessionalTimeline(
    project: VideoProject,
    currentTimeMs: Long,
    onSeek: (Long) -> Unit,
    onClipSelected: (TimelineClip) -> Unit,
    onClipMoved: (TimelineClip, Long) -> Unit,
    onClipTrimmed: (TimelineClip, Long, Long) -> Unit,
    onZoomChanged: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val haptic = LocalHapticFeedback.current
    
    // Zoom factor: pixels per millisecond. Base is 1ms = 0.1dp at zoom 1.0
    val basePxPerMs = with(density) { 0.1.dp.toPx() }
    val pxPerMs = basePxPerMs * project.timeline.zoomLevel
    
    val scrollState = rememberScrollState()
    
    // Synchronization: Update scroll position when currentTimeMs changes (e.g., during playback)
    LaunchedEffect(currentTimeMs, pxPerMs) {
        val targetScroll = (currentTimeMs * pxPerMs).toInt()
        if (abs(scrollState.value - targetScroll) > 1) {
            scrollState.scrollTo(targetScroll)
        }
    }
    
    // Synchronization: Update currentTimeMs when user scrolls
    LaunchedEffect(scrollState.value) {
        val newTime = (scrollState.value / pxPerMs).toLong()
        if (newTime != currentTimeMs) {
            onSeek(newTime)
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(180.dp)
            .background(BackgroundPrimary)
            .pointerInput(Unit) {
                detectTransformGestures { _, _, zoom, _ ->
                    if (zoom != 1f) {
                        onZoomChanged((project.timeline.zoomLevel * zoom).coerceIn(0.1f, 10f))
                    }
                }
            }
    ) {
        val screenWidthPx = constraints.maxWidth.toFloat()
        val centerX = screenWidthPx / 2f
        val paddingStart = with(density) { centerX.toDp() }
        
        // Horizontal scrolling container
        Row(
            modifier = Modifier
                .fillMaxSize()
                .horizontalScroll(scrollState)
        ) {
            // Start Padding: Aligns time 0 with the center playhead
            Spacer(modifier = Modifier.width(paddingStart))
            
            Column(modifier = Modifier.fillMaxHeight()) {
                // 1. High-Precision Time Ruler
                TimelineRuler(
                    durationMs = 600_000, // Show up to 10 mins (extend as needed)
                    pxPerMs = pxPerMs,
                    modifier = Modifier
                        .width(with(density) { (600_000 * pxPerMs).toDp() })
                        .height(30.dp)
                )
                
                // 2. Multiple Tracks
                Box(modifier = Modifier.weight(1f)) {
                    // Vertical Grid Lines
                    TimelineGrid(
                        durationMs = 600_000,
                        pxPerMs = pxPerMs,
                        modifier = Modifier.fillMaxSize()
                    )
                    
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        project.timeline.tracks.forEach { track ->
                            TimelineTrackRow(
                                track = track,
                                pxPerMs = pxPerMs,
                                allClips = project.timeline.tracks.flatMap { it.clips },
                                onClipSelected = onClipSelected,
                                onClipMoved = onClipMoved,
                                onClipTrimmed = onClipTrimmed,
                                snappingThresholdMs = (40 / project.timeline.zoomLevel).toLong()
                            )
                        }
                    }
                }
            }
            
            // End Padding: Allows last second to reach the center playhead
            Spacer(modifier = Modifier.width(paddingStart))
        }
        
        // 3. Centered Fixed Playhead
        Playhead(modifier = Modifier.align(Alignment.Center))
    }
}

@Composable
fun TimelineRuler(
    durationMs: Long,
    pxPerMs: Float,
    modifier: Modifier = Modifier
) {
    val textPrimary = OnPrimary
    val textSecondary = OnSurfaceSecondary
    
    Canvas(modifier = modifier) {
        val stepMs = when {
            pxPerMs > 0.8f -> 500L
            pxPerMs > 0.2f -> 1000L
            pxPerMs > 0.05f -> 5000L
            else -> 10000L
        }
        
        for (time in 0..durationMs step stepMs) {
            val x = time * pxPerMs
            val isMajor = time % (stepMs * 5) == 0L || time == 0L
            
            drawLine(
                color = if (isMajor) textPrimary.copy(alpha = 0.4f) else textSecondary.copy(alpha = 0.2f),
                start = Offset(x, if (isMajor) 0f else 18f),
                end = Offset(x, size.height),
                strokeWidth = if (isMajor) 2f else 1f
            )
            
            if (isMajor) {
                val minutes = time / 60000
                val seconds = (time % 60000) / 1000
                val timeStr = "%02d:%02d".format(minutes, seconds)
                
                drawContext.canvas.nativeCanvas.drawText(
                    timeStr,
                    x + 8f,
                    24f,
                    Paint().apply {
                        color = textSecondary.toArgb()
                        textSize = 24f
                        isAntiAlias = true
                    }
                )
            }
        }
    }
}

@Composable
fun TimelineGrid(
    durationMs: Long,
    pxPerMs: Float,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val stepMs = 5000L // Vertical grid line every 5 seconds
        for (time in 0..durationMs step stepMs) {
            val x = time * pxPerMs
            drawLine(
                color = Color.White.copy(alpha = 0.03f),
                start = Offset(x, 0f),
                end = Offset(x, size.height),
                strokeWidth = 1f
            )
        }
    }
}

@Composable
fun TimelineTrackRow(
    track: TimelineTrack,
    pxPerMs: Float,
    allClips: List<TimelineClip>,
    onClipSelected: (TimelineClip) -> Unit,
    onClipMoved: (TimelineClip, Long) -> Unit,
    onClipTrimmed: (TimelineClip, Long, Long) -> Unit,
    snappingThresholdMs: Long
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .background(SurfaceVariant.copy(alpha = 0.2f))
            .drawBehind {
                drawLine(
                    color = OutlineColor,
                    start = Offset(0f, size.height),
                    end = Offset(size.width, size.height),
                    strokeWidth = 1f
                )
            }
    ) {
        // Track Label Overlay (Subtle)
        Text(
            text = track.label.uppercase(),
            modifier = Modifier.padding(start = 8.dp, top = 2.dp),
            color = OnSurfaceSecondary.copy(alpha = 0.3f),
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold
        )

        track.clips.forEach { clip ->
            TimelineClipItem(
                clip = clip,
                pxPerMs = pxPerMs,
                allClips = allClips,
                onSelected = { onClipSelected(clip) },
                onMoved = { newStartMs -> onClipMoved(clip, newStartMs) },
                onTrimmed = { newTrimStart, newTrimEnd -> 
                    onClipTrimmed(clip, newTrimStart, newTrimEnd) 
                },
                snappingThresholdMs = snappingThresholdMs
            )
        }
    }
}

@Composable
fun TimelineClipItem(
    clip: TimelineClip,
    pxPerMs: Float,
    allClips: List<TimelineClip>,
    onSelected: () -> Unit,
    onMoved: (Long) -> Unit,
    onTrimmed: (Long, Long) -> Unit,
    snappingThresholdMs: Long
) {
    val density = LocalDensity.current
    val haptic = LocalHapticFeedback.current
    
    val clipWidthPx = clip.durationMs * pxPerMs
    val clipStartPx = clip.startTimeMs * pxPerMs
    
    val clipColor = when (clip.type) {
        TrackType.VIDEO -> SignaturePurple
        TrackType.AUDIO -> AccentTertiary
        TrackType.TEXT -> AccentSecondary
        TrackType.STICKER -> AccentRose
        TrackType.OVERLAY -> PremiumGold
        else -> SurfaceTertiary
    }

    // Local state for interactive dragging to avoid excessive recompositions of the whole timeline
    var dragOffsetMs by remember { mutableStateOf(0L) }
    var trimStartOffsetMs by remember { mutableStateOf(0L) }
    var trimEndOffsetMs by remember { mutableStateOf(0L) }

    val currentStartMs = clip.startTimeMs + dragOffsetMs
    val currentDurationMs = clip.durationMs + trimEndOffsetMs - trimStartOffsetMs
    
    val displayStartPx = currentStartMs * pxPerMs
    val displayWidthPx = currentDurationMs * pxPerMs

    Box(
        modifier = Modifier
            .offset(x = with(density) { displayStartPx.toDp() })
            .width(with(density) { displayWidthPx.toDp() })
            .fillMaxHeight()
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(clipColor.copy(alpha = if (clip.isSelected) 0.95f else 0.7f))
            .border(
                width = if (clip.isSelected) 2.dp else 0.dp,
                color = if (clip.isSelected) Color.White else Color.Transparent,
                shape = RoundedCornerShape(8.dp)
            )
            .pointerInput(clip.id) {
                detectTapGestures { onSelected() }
            }
            .pointerInput(clip.id) {
                detectDragGestures(
                    onDragStart = { dragOffsetMs = 0L },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        val deltaMs = (dragAmount.x / pxPerMs).toLong()
                        dragOffsetMs += deltaMs
                        
                        // Snapping logic
                        val snappedTime = findSnapPoint(currentStartMs, allClips, clip.id, snappingThresholdMs)
                        if (snappedTime != null) {
                            val snapDelta = snappedTime - currentStartMs
                            if (abs(snapDelta) < snappingThresholdMs) {
                                dragOffsetMs += snapDelta
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            }
                        }
                    },
                    onDragEnd = {
                        onMoved(clip.startTimeMs + dragOffsetMs)
                        dragOffsetMs = 0L
                    }
                )
            }
    ) {
        Text(
            text = clip.name,
            modifier = Modifier
                .padding(horizontal = 12.dp)
                .align(Alignment.CenterStart),
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1
        )
        
        if (clip.isSelected) {
            // Trim Handles
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(16.dp)
                    .align(Alignment.CenterStart)
                    .background(Color.White.copy(alpha = 0.2f))
                    .pointerInput(clip.id) {
                        detectDragGestures(
                            onDrag = { change, dragAmount ->
                                change.consume()
                                val deltaMs = (dragAmount.x / pxPerMs).toLong()
                                trimStartOffsetMs += deltaMs
                            },
                            onDragEnd = {
                                onTrimmed(clip.trimStartMs + trimStartOffsetMs, clip.trimEndMs)
                                trimStartOffsetMs = 0L
                            }
                        )
                    }
            ) {
                // Handle Icon
                Box(Modifier.size(2.dp, 12.dp).background(Color.White).align(Alignment.Center))
            }
            
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(16.dp)
                    .align(Alignment.CenterEnd)
                    .background(Color.White.copy(alpha = 0.2f))
                    .pointerInput(clip.id) {
                        detectDragGestures(
                            onDrag = { change, dragAmount ->
                                change.consume()
                                val deltaMs = (dragAmount.x / pxPerMs).toLong()
                                trimEndOffsetMs += deltaMs
                            },
                            onDragEnd = {
                                onTrimmed(clip.trimStartMs, clip.trimEndMs + trimEndOffsetMs)
                                trimEndOffsetMs = 0L
                            }
                        )
                    }
            ) {
                // Handle Icon
                Box(Modifier.size(2.dp, 12.dp).background(Color.White).align(Alignment.Center))
            }
        }
    }
}

@Composable
fun Playhead(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxHeight()
            .width(2.dp)
            .background(SignatureOrange)
    ) {
        // Playhead Cap (Aurora Accent)
        Box(
            modifier = Modifier
                .size(14.dp)
                .align(Alignment.TopCenter)
                .offset(y = (-4).dp)
                .clip(RoundedCornerShape(3.dp))
                .background(SignatureOrange)
                .border(1.dp, Color.White.copy(alpha = 0.5f), RoundedCornerShape(3.dp))
        )
    }
}

private fun findSnapPoint(
    currentTime: Long,
    allClips: List<TimelineClip>,
    excludeId: String,
    threshold: Long
): Long? {
    for (clip in allClips) {
        if (clip.id == excludeId) continue
        
        // Snap to start of another clip
        if (abs(currentTime - clip.startTimeMs) < threshold) return clip.startTimeMs
        
        // Snap to end of another clip
        val clipEnd = clip.startTimeMs + clip.durationMs
        if (abs(currentTime - clipEnd) < threshold) return clipEnd
    }
    return null
}
