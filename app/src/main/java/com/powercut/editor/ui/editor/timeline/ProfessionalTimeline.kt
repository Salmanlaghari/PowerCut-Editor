package com.powercut.editor.ui.editor.timeline

import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.media.MediaExtractor
import android.media.MediaCodec
import android.media.MediaFormat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.powercut.editor.data.*
import com.powercut.editor.ui.theme.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect
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
    
    val timelineDurationMs = maxOf(
        project.durationMs,
        project.timeline.tracks.flatMap { it.clips.map { clip -> clip.startTimeMs + clip.durationMs } }.maxOrNull() ?: 0L,
        600_000L
    )

    // Synchronization: Update scroll position when currentTimeMs changes (e.g., during playback).
    // Do not fight the user while they are actively dragging/scrolling.
    LaunchedEffect(currentTimeMs, pxPerMs) {
        if (!scrollState.isScrollInProgress) {
            val safeTimeMs = currentTimeMs.coerceIn(0L, timelineDurationMs)
            val targetScroll = (safeTimeMs * pxPerMs).toInt().coerceIn(0, scrollState.maxValue)
            if (abs(scrollState.value - targetScroll) > 2) {
                scrollState.scrollTo(targetScroll)
            }
        }
    }

    // Synchronization: Update currentTimeMs only while the user is actively scrolling.
    LaunchedEffect(scrollState, pxPerMs) {
        snapshotFlow { scrollState.value }
            .collect { scrollValue ->
                if (scrollState.isScrollInProgress) {
                    val newTime = (scrollValue / pxPerMs).toLong().coerceIn(0L, timelineDurationMs)
                    if (newTime != currentTimeMs) {
                        onSeek(newTime)
                    }
                }
            }
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(108.dp)
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
                    durationMs = timelineDurationMs,
                    pxPerMs = pxPerMs,
                    modifier = Modifier
                        .width(with(density) { (timelineDurationMs * pxPerMs).toDp() })
                        .height(18.dp)
                )
                
                // 2. Multiple Tracks
                Box(modifier = Modifier.weight(1f)) {
                    // Vertical Grid Lines
                    TimelineGrid(
                        durationMs = timelineDurationMs,
                        pxPerMs = pxPerMs,
                        modifier = Modifier.fillMaxSize()
                    )
                    
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(vertical = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        project.timeline.tracks.forEach { track ->
                            TimelineTrackRow(
                                track = track,
                                pxPerMs = pxPerMs,
                                allClips = project.timeline.tracks.flatMap { it.clips },
                                keyframeTracks = project.keyframeTracks,
                                onClipSelected = onClipSelected,
                                onClipMoved = onClipMoved,
                                onClipTrimmed = onClipTrimmed,
                                // Phase B: magnetic snap is a REAL setting — when
                                // disabled the threshold becomes 0 so findSnapPoint
                                // never engages and clips drag freely.
                                snappingThresholdMs =
                                    if (com.powercut.editor.core.utils.AppSettings.magneticSnap) {
                                        (40 / project.timeline.zoomLevel).toLong()
                                    } else 0L
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
    keyframeTracks: List<KeyframeTrack>,
    onClipSelected: (TimelineClip) -> Unit,
    onClipMoved: (TimelineClip, Long) -> Unit,
    onClipTrimmed: (TimelineClip, Long, Long) -> Unit,
    snappingThresholdMs: Long
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(32.dp)
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
            val clipKeyframes = keyframeTracks.find { it.clipId == clip.id }?.keyframes ?: emptyList()
            TimelineClipItem(
                clip = clip,
                pxPerMs = pxPerMs,
                allClips = allClips,
                keyframes = clipKeyframes,
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
    keyframes: List<Keyframe>,
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

    val currentStartMs = (clip.startTimeMs + dragOffsetMs).coerceAtLeast(0L)
    val currentDurationMs = maxOf(clip.durationMs + trimEndOffsetMs - trimStartOffsetMs, 1L)

    val displayStartPx = currentStartMs * pxPerMs
    val displayWidthPx = maxOf(displayStartPx + currentDurationMs * pxPerMs, displayStartPx + with(density) { 20.dp.toPx() }) - displayStartPx

    // Thumbnail cache for video clips
    var thumbnails by remember(clip.id, clip.path) { mutableStateOf<List<Bitmap>?>(null) }
    LaunchedEffect(clip.id, clip.path) {
        if (clip.type == TrackType.VIDEO && thumbnails == null) {
            thumbnails = withContext(Dispatchers.IO) {
                extractThumbnails(clip.path, clip.durationMs)
            }
        }
    }

    // Waveform cache for audio clips
    var waveform by remember(clip.id, clip.path) { mutableStateOf<List<Float>?>(null) }
    LaunchedEffect(clip.id, clip.path) {
        if (clip.type == TrackType.AUDIO && waveform == null) {
            waveform = withContext(Dispatchers.IO) {
                extractWaveform(clip.path, clip.durationMs)
            }
        }
    }

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
            .drawBehind {
                val clipW = size.width
                val clipH = size.height
                keyframes.forEach { kf ->
                    val localMs = (kf.timeMs - clip.startTimeMs).coerceIn(0, clip.durationMs)
                    val xPx = (localMs / clip.durationMs.toFloat()) * clipW
                    if (xPx in 0f..clipW) {
                        val diamondSize = 6f
                        val y = 8f
                        val diamond = Path().apply {
                            moveTo(xPx, y - diamondSize)
                            lineTo(xPx + diamondSize, y)
                            lineTo(xPx, y + diamondSize)
                            lineTo(xPx - diamondSize, y)
                            close()
                        }
                        drawPath(diamond, Color.White.copy(alpha = 0.9f))
                        drawPath(diamond, Color.White.copy(alpha = 0.3f), style = Stroke(width = 1f))
                    }
                }
                // Thumbnail filmstrip for video clips
                if (clip.type == TrackType.VIDEO) {
                    val thumbs = thumbnails
                    if (thumbs != null && thumbs.isNotEmpty()) {
                        val thumbWidth = clipW / thumbs.size
                        thumbs.forEachIndexed { index, bitmap ->
                            val left = index * thumbWidth
                            val top = 0f
                            val right = left + thumbWidth
                            val bottom = clipH
                            drawImage(
                                image = bitmap.asImageBitmap(),
                                dstOffset = androidx.compose.ui.geometry.IntOffset(left.toInt(), top.toInt()),
                                dstSize = androidx.compose.ui.geometry.Size(right - left, bottom - top)
                            )
                        }
                    }
                }
                // Waveform for audio clips
                if (clip.type == TrackType.AUDIO) {
                    val wave = waveform
                    if (wave != null && wave.isNotEmpty()) {
                        val centerY = clipH / 2f
                        val maxBarHeight = clipH * 0.7f
                        val barWidth = clipW / wave.size
                        wave.forEachIndexed { index, amplitude ->
                            val x = index * barWidth
                            val barHeight = amplitude * maxBarHeight
                            drawLine(
                                color = clipColor.copy(alpha = 0.8f),
                                start = Offset(x, centerY - barHeight / 2f),
                                end = Offset(x, centerY + barHeight / 2f),
                                strokeWidth = maxOf(barWidth - 0.5f, 1f)
                            )
                        }
                    }
                }
            }
            .pointerInput(clip.id) {
                detectTapGestures { onSelected() }
            }
            .pointerInput(clip.id) {
                detectDragGestures(
                    onDragStart = { dragOffsetMs = 0L },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        val requestedDeltaMs = (dragAmount.x / pxPerMs).toLong()
                        val nextStartMs = (clip.startTimeMs + dragOffsetMs + requestedDeltaMs).coerceAtLeast(0L)
                        dragOffsetMs = nextStartMs - clip.startTimeMs

                        val snappedTime = findSnapPoint(nextStartMs, allClips, clip.id, snappingThresholdMs)
                        if (snappedTime != null) {
                            val snapDelta = snappedTime - nextStartMs
                            if (abs(snapDelta) < snappingThresholdMs) {
                                dragOffsetMs += snapDelta
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            }
                        }
                    },
                    onDragEnd = {
                        onMoved((clip.startTimeMs + dragOffsetMs).coerceAtLeast(0L))
                        dragOffsetMs = 0L
                    }
                )
            }
    ) {
        Text(
            text = clip.name,
            modifier = Modifier
                .padding(horizontal = 12.dp)
                .align(Alignment.CenterStart)
                .then(
                    if (clip.type == TrackType.VIDEO && thumbnails != null && thumbnails!!.isNotEmpty()) {
                        Modifier.background(Color.Black.copy(alpha = 0.4f))
                    } else Modifier
                ),
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1
        )
        
        if (clip.isSelected) {
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
                                val requestedDeltaMs = (dragAmount.x / pxPerMs).toLong()
                                val nextStart = clip.trimStartMs + trimStartOffsetMs + requestedDeltaMs
                                trimStartOffsetMs = (nextStart.coerceAtLeast(0L).coerceAtMost(clip.trimEndMs - 1L) - clip.trimStartMs)
                            },
                            onDragEnd = {
                                val newTrimStart = (clip.trimStartMs + trimStartOffsetMs).coerceAtLeast(0L).coerceAtMost(clip.trimEndMs - 1L)
                                onTrimmed(newTrimStart, clip.trimEndMs)
                                trimStartOffsetMs = 0L
                            }
                        )
                    }
            ) {
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
                                val requestedDeltaMs = (dragAmount.x / pxPerMs).toLong()
                                val nextEnd = clip.trimEndMs + trimEndOffsetMs + requestedDeltaMs
                                trimEndOffsetMs = (nextEnd.coerceIn(clip.trimStartMs + 1L, clip.mediaDurationMs) - clip.trimEndMs)
                            },
                            onDragEnd = {
                                val newTrimEnd = (clip.trimEndMs + trimEndOffsetMs).coerceIn(clip.trimStartMs + 1L, clip.mediaDurationMs)
                                onTrimmed(clip.trimStartMs, newTrimEnd)
                                trimEndOffsetMs = 0L
                            }
                        )
                    }
            ) {
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

private fun extractThumbnails(path: String, durationMs: Long): List<Bitmap>? {
    return try {
        val retriever = MediaMetadataRetriever()
        retriever.setDataSource(path)
        val thumbnails = mutableListOf<Bitmap>()
        val interval = if (durationMs < 5000) 500L else 1000L
        var timeUs = 0L
        while (timeUs < durationMs * 1000) {
            retriever.getFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)?.let { frame ->
                thumbnails.add(Bitmap.createScaledBitmap(frame, 80, 45, true))
            }
            timeUs += interval * 1000
        }
        retriever.release()
        if (thumbnails.isEmpty()) null else thumbnails
    } catch (e: Exception) {
        null
    }
}

private fun extractWaveform(path: String, durationMs: Long): List<Float>? {
    return try {
        val extractor = MediaExtractor()
        extractor.setDataSource(path)
        var trackIndex = -1
        var format: MediaFormat? = null
        for (i in 0 until extractor.trackCount) {
            val f = extractor.getTrackFormat(i)
            val mime = f.getString(MediaFormat.KEY_MIME) ?: continue
            if (mime.startsWith("audio/")) {
                trackIndex = i
                format = f
                break
            }
        }
        if (trackIndex < 0 || format == null) {
            extractor.release()
            return null
        }
        extractor.selectTrack(trackIndex)
        val mime = format.getString(MediaFormat.KEY_MIME)!!
        val codec = MediaCodec.createDecoderByType(mime)
        codec.configure(format, null, null, 0)
        codec.start()
        val sampleCount = 64
        val amplitudes = FloatArray(sampleCount)
        val bufferInfo = MediaCodec.BufferInfo()
        var chunkIndex = 0
        var sawEos = false
        while (!sawEos && chunkIndex < sampleCount) {
            val inputId = codec.dequeueInputBuffer(10000)
            if (inputId >= 0) {
                val inputBuf = codec.getInputBuffer(inputId)!!
                val size = extractor.readSampleData(inputBuf, 0)
                if (size >= 0) {
                    codec.queueInputBuffer(inputId, 0, size, extractor.sampleTime, 0)
                    extractor.advance()
                } else {
                    codec.queueInputBuffer(inputId, 0, 0, 0L, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                }
            }
            val outputId = codec.dequeueOutputBuffer(bufferInfo, 10000)
            if (outputId >= 0) {
                val outputBuf = codec.getOutputBuffer(outputId)!!
                val size = bufferInfo.size
                if (size > 0 && chunkIndex < sampleCount) {
                    val chunk = ByteArray(size)
                    outputBuf.get(chunk)
                    var peak = 0f
                    var i = 0
                    while (i < chunk.size - 1) {
                        val sample = ((chunk[i + 1].toInt() shl 8) or (chunk[i].toInt() and 0xFF)).toShort()
                        val absSample = kotlin.math.abs(sample.toFloat()) / 32768f
                        if (absSample > peak) peak = absSample
                        i += 2
                    }
                    amplitudes[chunkIndex] = peak.coerceIn(0.05f, 1f)
                    chunkIndex++
                }
                codec.releaseOutputBuffer(outputId, false)
                if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                    sawEos = true
                }
            }
        }
        codec.stop()
        codec.release()
        extractor.release()
        amplitudes.toList()
    } catch (e: Exception) {
        null
    }
}

@Composable
fun rememberWaveform(path: String): List<Float>? {
    var waveform by remember(path) { mutableStateOf<List<Float>?>(null) }
    LaunchedEffect(path) {
        if (waveform == null) {
            waveform = withContext(Dispatchers.IO) {
                extractWaveform(path, 0L)
            }
        }
    }
    return waveform
}

@Composable
fun TimelineWaveform(
    clip: TimelineClip,
    pxPerMs: Float,
    modifier: Modifier = Modifier
) {
    var waveform by rememberWaveform(clip.path)
    Canvas(modifier = modifier.height(16.dp)) {
        val wave = waveform ?: return@Canvas
        val centerY = size.height / 2f
        val maxBarHeight = size.height * 0.8f
        val barWidth = size.width / wave.size
        wave.forEachIndexed { index, amplitude ->
            val x = index * barWidth
            val barHeight = amplitude * maxBarHeight
            drawLine(
                color = when (clip.type) {
                    TrackType.AUDIO -> AccentTertiary
                    else -> Color.Gray
                }.copy(alpha = 0.7f),
                start = Offset(x, centerY - barHeight / 2f),
                end = Offset(x, centerY + barHeight / 2f),
                strokeWidth = maxOf(barWidth - 0.5f, 1f)
            )
        }
    }
}
