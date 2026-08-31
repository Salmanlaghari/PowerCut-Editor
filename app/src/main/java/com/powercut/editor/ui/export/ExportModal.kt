package com.powercut.editor.ui.export

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlin.math.cos
import kotlin.math.sin

// ── Colors ──
private val BgDark = Color(0xFF0A0A0F)
private val GlassBg = Color(0xE0141420)
private val GlassBorder = Color(0xFF2A2A3A)
private val AccentCyan = Color(0xFF00D4FF)
private val AccentGold = Color(0xFFFFD700)
private val AccentPurple = Color(0xFF8B5CF6)
private val TextWhite = Color(0xFFFFFFFF)
private val TextGray = Color(0xFF7A7A8E)
private val TextDim = Color(0xFF555566)
private val ToggleActive = Color(0xFF00D4FF)
private val ToggleInactive = Color(0xFF2A2A3A)

data class ExportSettings(
    val resolution: String = "4K Ultra HD (3840x2160)",
    val frameRate: String = "60 FPS",
    val hdrEnabled: Boolean = true,
    val bitrateMbps: Int = 80,
    val format: String = "MP4 / H.265"
)

@Composable
fun ExportModal(
    settings: ExportSettings,
    progress: Float, // 0.0 - 1.0
    estimatedTimeRemaining: String,
    isExporting: Boolean,
    onSettingsChanged: (ExportSettings) -> Unit,
    onStartExport: () -> Unit,
    onCancel: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.7f))
                .clickable(onClick = onDismiss),
            contentAlignment = Alignment.Center
        ) {
            // Glassmorphism modal
            Column(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .clip(RoundedCornerShape(24.dp))
                    .background(GlassBg)
                    .border(1.dp, GlassBorder, RoundedCornerShape(24.dp))
                    .shadow(24.dp, RoundedCornerShape(24.dp))
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "EXPORT SETTINGS",
                        color = TextWhite,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )

                    // PRO badge
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(AccentGold, AccentCyan)
                                )
                            )
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "POWERCUT PRO",
                            color = BgDark,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Circular progress ring
                if (isExporting) {
                    CircularProgressRing(
                        progress = progress,
                        estimatedTime = estimatedTimeRemaining
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Export options
                Text(
                    text = "EXPORT OPTIONS",
                    color = TextGray,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Resolution
                ExportOptionRow(
                    label = "Resolution",
                    value = settings.resolution,
                    onValueClick = { /* Cycle resolution */ }
                )

                // Frame Rate
                ExportOptionRow(
                    label = "Frame Rate",
                    value = settings.frameRate,
                    onValueClick = { /* Cycle frame rate */ }
                )

                // HDR Toggle
                ExportToggleRow(
                    label = "Color Depth",
                    value = "HDR Color (Dolby Vision)",
                    isEnabled = settings.hdrEnabled,
                    onToggle = { onSettingsChanged(settings.copy(hdrEnabled = it)) }
                )

                // Bitrate
                ExportBitrateRow(
                    bitrateMbps = settings.bitrateMbps,
                    onBitrateChanged = { onSettingsChanged(settings.copy(bitrateMbps = it)) }
                )

                // Format
                ExportOptionRow(
                    label = "Format",
                    value = settings.format,
                    onValueClick = { /* Cycle format */ }
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Estimated file size
                Text(
                    text = "Estimated file size: ${String.format("%.2f", settings.bitrateMbps * 0.018)} GB",
                    color = TextGray,
                    fontSize = 11.sp,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Export button
                Button(
                    onClick = if (isExporting) onCancel else onStartExport,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isExporting) AccentGold else AccentCyan
                    )
                ) {
                    if (isExporting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = BgDark,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "FINALIZING EXPORT...",
                            color = BgDark,
                            fontWeight = FontWeight.Bold
                        )
                    } else {
                        Text(
                            text = "START EXPORT",
                            color = BgDark,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Cancel button
                TextButton(
                    onClick = onCancel,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Cancel",
                        color = TextGray
                    )
                }
            }
        }
    }
}

@Composable
private fun CircularProgressRing(
    progress: Float,
    estimatedTime: String
) {
    val infiniteTransition = rememberInfiniteTransition(label = "progress")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(180.dp)
    ) {
        // Glow effect
        Box(
            modifier = Modifier
                .size(160.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            AccentCyan.copy(alpha = glowAlpha * 0.3f),
                            Color.Transparent
                        )
                    )
                )
        )

        // Progress ring
        Canvas(modifier = Modifier.size(160.dp)) {
            val strokeWidth = 8.dp.toPx()
            val radius = (size.minDimension - strokeWidth) / 2

            // Background ring
            drawCircle(
                color = ToggleInactive,
                radius = radius,
                style = Stroke(strokeWidth, cap = StrokeCap.Round)
            )

            // Progress arc
            drawArc(
                color = AccentCyan,
                startAngle = -90f,
                sweepAngle = 360f * progress,
                useCenter = false,
                style = Stroke(strokeWidth, cap = StrokeCap.Round),
                topLeft = Offset(strokeWidth / 2, strokeWidth / 2),
                size = Size(radius * 2, radius * 2)
            )

            // Gold accent arc
            drawArc(
                color = AccentGold,
                startAngle = -90f + 360f * progress,
                sweepAngle = 30f,
                useCenter = false,
                style = Stroke(strokeWidth * 0.6f, cap = StrokeCap.Round),
                topLeft = Offset(strokeWidth / 2, strokeWidth / 2),
                size = Size(radius * 2, radius * 2)
            )
        }

        // Center text
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Rendering",
                color = TextGray,
                fontSize = 12.sp
            )
            Text(
                text = "${(progress * 100).toInt()}%",
                color = TextWhite,
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Estimated: $estimatedTime",
                color = TextGray,
                fontSize = 10.sp
            )
            Text(
                text = "remaining",
                color = TextDim,
                fontSize = 10.sp
            )
        }
    }
}

@Composable
private fun ExportOptionRow(
    label: String,
    value: String,
    onValueClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = TextGray,
            fontSize = 14.sp
        )

        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(ToggleInactive)
                .clickable(onClick = onValueClick)
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = value,
                color = TextWhite,
                fontSize = 12.sp
            )
            Icon(
                imageVector = Icons.Default.ArrowDropDown,
                contentDescription = null,
                tint = TextGray,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
private fun ExportToggleRow(
    label: String,
    value: String,
    isEnabled: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = TextGray,
            fontSize = 14.sp
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = value,
                color = if (isEnabled) ToggleActive else TextDim,
                fontSize = 12.sp
            )

            Switch(
                checked = isEnabled,
                onCheckedChange = onToggle,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = ToggleActive,
                    checkedTrackColor = ToggleActive.copy(alpha = 0.3f),
                    uncheckedThumbColor = TextGray,
                    uncheckedTrackColor = ToggleInactive
                )
            )
        }
    }
}

@Composable
private fun ExportBitrateRow(
    bitrateMbps: Int,
    onBitrateChanged: (Int) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Bitrate",
                color = TextGray,
                fontSize = 14.sp
            )

            Text(
                text = "Custom Bitrate (${bitrateMbps} Mbps)",
                color = TextWhite,
                fontSize = 12.sp
            )
        }

        Slider(
            value = bitrateMbps.toFloat(),
            onValueChange = { onBitrateChanged(it.toInt()) },
            valueRange = 10f..200f,
            colors = SliderDefaults.colors(
                thumbColor = AccentCyan,
                activeTrackColor = AccentCyan,
                inactiveTrackColor = ToggleInactive
            )
        )
    }
}
