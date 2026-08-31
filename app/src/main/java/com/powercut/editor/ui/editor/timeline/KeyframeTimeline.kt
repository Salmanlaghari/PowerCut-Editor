package com.powercut.editor.ui.editor.timeline

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.powercut.editor.domain.keyframe.Keyframe
import com.powercut.editor.domain.keyframe.KeyframeEngine
import com.powercut.editor.domain.keyframe.KeyframeProperty

// ── Colors ──
private val BgDark = Color(0xFF0A0A0F)
private val TrackBg = Color(0xFF141420)
private val TrackBorder = Color(0xFF1E1E2E)
private val DiamondOrange = Color(0xFFFF9500)
private val DiamondCyan = Color(0xFF00D4FF)
private val DiamondGold = Color(0xFFFFD700)
private val DiamondPink = Color(0xFFFF2D55)
private val DiamondPurple = Color(0xFF8B5CF6)
private val DiamondGreen = Color(0xFF34C759)
private val PlayheadWhite = Color.White
private val TextWhite = Color(0xFFFFFFFF)
private val TextGray = Color(0xFF7A7A8E)
private val AddButton = Color(0xFF00D4FF)
private val CurveHandle = Color(0xFFFFD700)

/**
 * Keyframe Timeline UI — thin orange bar between playback controls and main timeline.
 * Shows second tick marks, diamond keyframe markers, playhead, and add button.
 */
@Composable
fun KeyframeTimeline(
    keyframeEngine: KeyframeEngine,
    selectedProperty: KeyframeProperty?,
    currentTimeMs: Long,
    totalDurationMs: Long,
    onAddKeyframe: (KeyframeProperty, Long) -> Unit,
    onRemoveKeyframe: (KeyframeProperty, Long) -> Unit,
    onKeyframeSelected: (KeyframeProperty, Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedKeyframe by remember { mutableStateOf<Pair<KeyframeProperty, Long>?>(null) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(BgDark)
    ) {
        // Property labels row
        PropertyLabelsRow(
            selectedProperty = selectedProperty,
            onPropertySelected = { /* Handle property selection */ }
        )

        // Keyframe bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .background(TrackBg)
        ) {
            // Second tick marks
            SecondTickMarks(totalDurationMs = totalDurationMs)

            // Keyframe diamonds for all active properties
            keyframeEngine.getActiveProperties().forEach { property ->
                val keyframes = keyframeEngine.getKeyframes(property)
                keyframes.forEach { keyframe ->
                    val isSelected = selectedKeyframe?.first == property && selectedKeyframe?.second == keyframe.timeMs
                    KeyframeDiamond(
                        property = property,
                        keyframe = keyframe,
                        totalDurationMs = totalDurationMs,
                        isSelected = isSelected,
                        onClick = {
                            selectedKeyframe = if (isSelected) null else Pair(property, keyframe.timeMs)
                            onKeyframeSelected(property, keyframe.timeMs)
                        },
                        onLongPress = {
                            onRemoveKeyframe(property, keyframe.timeMs)
                            selectedKeyframe = null
                        }
                    )
                }
            }

            // Playhead line
            PlayheadLine(currentTimeMs = currentTimeMs, totalDurationMs = totalDurationMs)

            // Add keyframe button
            if (selectedProperty != null) {
                AddKeyframeButton(
                    onClick = { onAddKeyframe(selectedProperty, currentTimeMs) }
                )
            }
        }

        // Keyframe count chip
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.End
        ) {
            val totalKeyframes = keyframeEngine.getActiveProperties().sumOf {
                keyframeEngine.getKeyframes(it).size
            }
            KeyframeCountChip(count = totalKeyframes)
        }
    }
}

@Composable
private fun PropertyLabelsRow(
    selectedProperty: KeyframeProperty?,
    onPropertySelected: (KeyframeProperty) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(24.dp)
            .background(BgDark)
            .padding(horizontal = 40.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        KeyframeProperty.entries.take(5).forEach { property ->
            val isSelected = property == selectedProperty
            Text(
                text = property.displayName.take(3),
                color = if (isSelected) property.color else TextGray,
                fontSize = 8.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .clickable { onPropertySelected(property) }
                    .padding(horizontal = 4.dp)
            )
        }
    }
}

@Composable
private fun SecondTickMarks(totalDurationMs: Long) {
    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .padding(start = 40.dp)
    ) {
        val width = size.width
        val height = size.height
        val totalSeconds = totalDurationMs / 1000

        // Draw tick marks every second
        for (sec in 0..totalSeconds) {
            val x = (sec.toFloat() / totalSeconds) * width
            val isMajor = sec % 5 == 0

            drawLine(
                color = if (isMajor) TextGray.copy(alpha = 0.6f) else TextGray.copy(alpha = 0.3f),
                start = Offset(x, height - if (isMajor) 12f else 6f),
                end = Offset(x, height),
                strokeWidth = if (isMajor) 1.5f else 0.75f
            )

            if (isMajor) {
                // Draw second label
                drawContext.canvas.nativeCanvas.apply {
                    // Note: In production, use Compose Text for proper rendering
                }
            }
        }
    }
}

@Composable
private fun KeyframeDiamond(
    property: KeyframeProperty,
    keyframe: Keyframe,
    totalDurationMs: Long,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongPress: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "diamond")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(600),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow"
    )

    val posX = ((keyframe.timeMs.toFloat() / totalDurationMs) * 0.85f) + 0.05f

    Box(
        modifier = Modifier
            .fillMaxHeight()
            .fillMaxWidth(fraction = posX)
            .padding(end = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        // Glow behind diamond
        if (isSelected) {
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .rotate(45f)
                    .clip(RoundedCornerShape(3.dp))
                    .background(property.color.copy(alpha = glowAlpha * 0.4f))
            )
        }

        // Diamond shape (rotated square)
        Canvas(
            modifier = Modifier
                .size(if (isSelected) 14.dp else 10.dp)
                .rotate(45f)
                .clip(RoundedCornerShape(2.dp))
                .clickable(onClick = onClick)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onLongPress = { onLongPress() },
                        onTap = { onClick() }
                    )
                }
        ) {
            drawRect(
                color = property.color,
                size = size
            )

            if (isSelected) {
                drawRect(
                    color = Color.White,
                    size = size * 0.4f,
                    topLeft = Offset(size.width * 0.3f, size.height * 0.3f)
                )
            }
        }
    }
}

@Composable
private fun PlayheadLine(currentTimeMs: Long, totalDurationMs: Long) {
    val posX = ((currentTimeMs.toFloat() / totalDurationMs) * 0.85f) + 0.05f

    Box(
        modifier = Modifier
            .fillMaxHeight()
            .fillMaxWidth(fraction = posX)
            .padding(end = 2.dp),
        contentAlignment = Alignment.Center
    ) {
        // Vertical playhead line
        Canvas(modifier = Modifier.fillMaxHeight().width(2.dp)) {
            drawLine(
                color = PlayheadWhite,
                start = Offset(1f, 0f),
                end = Offset(1f, size.height),
                strokeWidth = 2f
            )
        }
    }
}

@Composable
private fun AddKeyframeButton(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxHeight()
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        contentAlignment = Alignment.CenterEnd
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(AddButton.copy(alpha = 0.2f))
                .border(1.dp, AddButton, RoundedCornerShape(6.dp))
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Add Keyframe",
                tint = AddButton,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
private fun KeyframeCountChip(count: Int) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(DiamondOrange.copy(alpha = 0.15f))
            .padding(horizontal = 8.dp, vertical = 2.dp)
    ) {
        Text(
            text = "◆ $count KF",
            color = DiamondOrange,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
