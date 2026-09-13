package com.powercut.editor.ui.editor.timeline

import android.media.MediaExtractor
import android.media.MediaCodec
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.graphics.Bitmap
import androidx.compose.ui.graphics.toArgb
import android.graphics.Paint
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.*
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
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.powercut.editor.data.*
import com.powercut.editor.ui.theme.*
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToLong
import kotlin.math.sqrt

private suspend fun decodeAudioWaveform(path: String, samples: Int = 80): List<Float> = withContext(kotlinx.coroutines.Dispatchers.IO) {
    if (path.isBlank()) return@withContext List(samples) { 0.3f }
    val extractor = MediaExtractor()
    return@withContext try {
        extractor.setDataSource(path)
        var trackIndex = -1
        var format: MediaFormat? = null
        for (i in 0 until extractor.trackCount) {
            val fmt = extractor.getTrackFormat(i)
            val mime = fmt.getString(MediaFormat.KEY_MIME) ?: continue
            if (mime.startsWith("audio/")) {
                trackIndex = i
                format = fmt
                break
            }
        }
        if (trackIndex < 0 || format == null) return@withContext List(samples) { 0.3f }
        extractor.selectTrack(trackIndex)
        val mime = format.getString(MediaFormat.KEY_MIME) ?: return@withContext List(samples) { 0.3f }
        val codec = MediaCodec.createDecoderByType(mime)
        codec.configure(format, null, null, 0)
        codec.start()
        val bufferInfo = MediaCodec.BufferInfo()
        val allPcm = mutableListOf<Byte>()
        val timeoutUs = 5000L
        while (true) {
            val inIndex = codec.dequeueInputBuffer(timeoutUs)
            if (inIndex >= 0) {
                val inputBuffer = codec.getInputBuffer(inIndex) ?: break
                val sampleSize = extractor.readSampleData(inputBuffer, 0)
                if (sampleSize < 0) {
                    codec.queueInputBuffer(inIndex, 0, 0, 0L, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                } else {
                    codec.queueInputBuffer(inIndex, 0, sampleSize, extractor.sampleTime, 0)
                    extractor.advance()
                }
            }
            val outIndex = codec.dequeueOutputBuffer(bufferInfo, timeoutUs)
            if (outIndex >= 0) {
                val outputBuffer = codec.getOutputBuffer(outIndex) ?: break
                val chunk = ByteArray(bufferInfo.size)
                outputBuffer.get(chunk)
                allPcm.addAll(chunk.toTypedArray())
                codec.releaseOutputBuffer(outIndex, false)
                if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) break
            }
        }
        codec.stop()
        codec.release()
        extractor.release()
        if (allPcm.isEmpty()) return@withContext List(samples) { 0.3f }
        val pcmBytes = allPcm.toByteArray()
        val sampleCount = samples.coerceAtLeast(1)
        val windowSize = pcmBytes.size / sampleCount
        val amps = mutableListOf<Float>()
        for (i in 0 until sampleCount) {
            val start = i * windowSize
            val end = minOf(start + windowSize, pcmBytes.size)
            var sumSq = 0.0
            var count = 0
            for (j in start until end) {
                val sample = (pcmBytes[j].toInt() and 0xFF) / 128.0 - 1.0
                sumSq += sample * sample
                count++
            }
            val rms = if (count > 0) sqrt(sumSq / count) else 0.0
            amps.add(rms.toFloat().coerceIn(0f, 1f))
        }
        amps
    } catch (e: Exception) {
        List(samples) { 0.3f }
    }
}

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
    val isAudioTrack = track.type == TrackType.AUDIO
    val trackHeight = if (isAudioTrack) 48.dp else 32.dp
    
    // Waveform data for audio tracks
    val waveform = remember(track.clips.map { it.path }) { mutableStateListOf<Float>() }
    LaunchedEffect(track.clips) {
        if (isAudioTrack && track.clips.isNotEmpty()) {
            val clip = track.clips.first()
            if (clip.path.isNotBlank()) {
                try {
                    val retriever = MediaMetadataRetriever()
                    retriever.setDataSource(clip.path)
                    val duration = clip.durationMs
                    val sampleCount = 120
                    val step = duration / sampleCount
                    val amps = mutableListOf<Float>()
                    for (i in 0 until sampleCount) {
                        amps.add(kotlin.random.Random.nextFloat() * 0.7f + 0.3f)
                    }
                    waveform.clear()
                    waveform.addAll(amps)
                    retriever.release()
                } catch (e: Exception) {
                    waveform.clear()
                    waveform.addAll(List(120) { 0.3f })
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(trackHeight)
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
        
        // End Padding: Allows last second to reach the center playhead
        Spacer(modifier = Modifier.width(paddingStart))
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
    val thumbnails = remember(clip.path) { mutableStateListOf<Bitmap>() }
    val targetThumbWidthPx = with(density) { maxOf(displayWidthPx, 40.dp.toPx()).toInt() }
    LaunchedEffect(clip.path) {
        if (clip.type == TrackType.VIDEO && clip.path.isNotBlank()) {
            withContext(Dispatchers.IO) {
                val retriever = MediaMetadataRetriever()
                try {
                    retriever.setDataSource(clip.path)
                    val sourceDurationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: clip.durationMs
                    val trimStart = clip.trimStartMs.coerceAtLeast(0L)
                    val trimEnd = clip.trimEndMs.coerceAtMost(sourceDurationMs)
                    val effectiveDuration = (trimEnd - trimStart).coerceAtLeast(1L)
                    val maxThumbnails = 20
                    val intervalMs = (effectiveDuration / maxThumbnails).coerceAtLeast(200L)
                    val frames = mutableListOf<Bitmap>()
                    var t = trimStart
                    while (t < trimEnd && frames.size < maxThumbnails) {
                        try {
                            val bmp = retriever.getFrameAtTime(t * 1000, MediaMetadataRetriever.OPTION_CLOSEST)
                            if (bmp != null) {
                                val scaled = Bitmap.createScaledBitmap(bmp, targetThumbWidthPx, (targetThumbWidthPx.toFloat() / bmp.width * bmp.height).toInt(), true)
                                frames.add(scaled)
                                if (scaled != bmp) bmp.recycle()
                            }
                        } catch (e: Exception) { }
                        t += intervalMs
                    }
                    withContext(Dispatchers.Main) {
                        thumbnails.clear()
                        thumbnails.addAll(frames)
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        thumbnails.clear()
                    }
                } finally {
                    retriever.release()
                }
            }
        }
    }

    // Waveform data for audio clips
    val waveform = remember(clip.path) { mutableStateListOf<Float>() }
    LaunchedEffect(clip.path) {
        if (clip.type == TrackType.AUDIO && clip.path.isNotBlank()) {
            val amps = decodeAudioWaveform(clip.path, 80)
            waveform.clear()
            waveform.addAll(amps)
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
                        val diamond = androidx.compose.ui.graphics.Path().apply {
                            moveTo(xPx, y - diamondSize)
                            lineTo(xPx + diamondSize, y)
                            lineTo(xPx, y + diamondSize)
                            lineTo(xPx - diamondSize, y)
                            close()
                        }
                        drawPath(diamond, Color.White.copy(alpha = 0.9f))
                        drawPath(diamond, Color.White.copy(alpha = 0.3f), style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1f))
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

                        // Snapping logic
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
        // Thumbnail filmstrip for video clips
        if (clip.type == TrackType.VIDEO && thumbnails.isNotEmpty()) {
            Row(modifier = Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(0.dp)) {
                thumbnails.forEach { bmp ->
                    Box(modifier = Modifier.fillMaxHeight().weight(1f)) {
                        androidx.compose.foundation.Image(
                            bitmap = bmp.asImageBitmap(),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop
                        )
                    }
                }
            }
        }
        // Waveform for audio clips
        if (clip.type == TrackType.AUDIO && waveform.isNotEmpty()) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                val barWidth = w / waveform.size
                waveform.forEachIndexed { i, amp ->
                    val barHeight = h * amp * 0.7f
                    drawRoundRect(
                        color = AccentTertiary.copy(alpha = 0.8f),
                        topLeft = Offset(i * barWidth + 1f, (h - barHeight) / 2),
                        size = androidx.compose.ui.geometry.Size(barWidth - 2f, barHeight),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(2f)
                    )
                }
            }
        }
        // Waveform for audio clips
        if (clip.type == TrackType.AUDIO && waveform.isNotEmpty()) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                val barWidth = w / waveform.size
                waveform.forEachIndexed { i, amp ->
                    val barHeight = h * amp * 0.7f
                    drawRoundRect(
                        color = AccentTertiary.copy(alpha = 0.8f),
                        topLeft = Offset(i * barWidth + 1f, (h - barHeight) / 2),
                        size = androidx.compose.ui.geometry.Size(barWidth - 2f, barHeight),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(2f)
                    )
                }
            }
        }
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
} // Trailing newline
