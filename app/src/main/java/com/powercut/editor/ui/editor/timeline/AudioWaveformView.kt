package com.powercut.editor.ui.editor.timeline

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.sin
import kotlin.math.abs

// ── Colors ──
private val WaveformBlue = Color(0xFF00B4D8)
private val WaveformOrange = Color(0xFFFF9500)
private val BeatMarkerColor = Color(0xFFFF9500)
private val SectionLabel = Color(0xFF7A7A8E)
private val BgDark = Color(0xFF0A0A0F)
private val SyncBar = Color(0xFF00D4FF)

data class BeatInfo(
    val timeMs: Long,
    val label: String = "",
    val isDownbeat: Boolean = false
)

data class AudioSection(
    val startTimeMs: Long,
    val endTimeMs: Long,
    val label: String
)

@Composable
fun AudioWaveformView(
    waveformData: FloatArray,
    beats: List<BeatInfo>,
    sections: List<AudioSection>,
    currentTimeMs: Long,
    totalDurationMs: Long,
    audioSyncDurationMs: Long,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(BgDark)
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        // Section labels
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            sections.forEach { section ->
                Text(
                    text = section.label,
                    color = SectionLabel,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Waveform canvas
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF0D0D18))
        ) {
            drawWaveform(waveformData, beats, currentTimeMs, totalDurationMs)
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Audio sync bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(36.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF141420))
                .padding(horizontal = 8.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Audio icon
                Canvas(modifier = Modifier.size(16.dp)) {
                    drawCircle(
                        color = SyncBar,
                        radius = size.minDimension / 2
                    )
                }

                Text(
                    text = "Audio Sync (${formatTimeMs(audioSyncDurationMs)})",
                    color = SyncBar,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

private fun DrawScope.drawWaveform(
    data: FloatArray,
    beats: List<BeatInfo>,
    currentTimeMs: Long,
    totalDurationMs: Long
) {
    val width = size.width
    val height = size.height
    val centerY = height / 2

    // Draw waveform bars
    val barWidth = width / data.size
    data.forEachIndexed { index, amplitude ->
        val x = index * barWidth
        val barHeight = amplitude * height * 0.8f

        // Color gradient: blue for normal, orange for beats
        val barColor = if (beats.any { kotlin.math.abs(it.timeMs - (index.toFloat() / data.size * totalDurationMs).toLong()) < 500 }) {
            WaveformOrange
        } else {
            WaveformBlue
        }

        drawRect(
            color = barColor.copy(alpha = 0.8f),
            topLeft = Offset(x, centerY - barHeight / 2),
            size = Size(barWidth * 0.8f, barHeight)
        )
    }

    // Draw beat markers
    beats.forEach { beat ->
        val x = (beat.timeMs.toFloat() / totalDurationMs) * width
        drawLine(
            color = BeatMarkerColor,
            start = Offset(x, 0f),
            end = Offset(x, height),
            strokeWidth = if (beat.isDownbeat) 3f else 1.5f
        )

        // Beat dot
        if (beat.isDownbeat) {
            drawCircle(
                color = BeatMarkerColor,
                radius = 6f,
                center = Offset(x, height - 8f)
            )
        }
    }

    // Draw playhead
    val playheadX = (currentTimeMs.toFloat() / totalDurationMs) * width
    drawLine(
        color = Color.White,
        start = Offset(playheadX, 0f),
        end = Offset(playheadX, height),
        strokeWidth = 2f
    )
}

private fun formatTimeMs(ms: Long): String {
    val totalSecs = ms / 1000
    val minutes = totalSecs / 60
    val seconds = totalSecs % 60
    return String.format("%dm %ds", minutes, seconds)
}
