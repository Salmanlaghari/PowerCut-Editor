package com.powercut.editor.ui.editor.timeline

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ── Colors ──
private val BgDark = Color(0xFF0A0A0F)
private val AccentCyan = Color(0xFF00D4FF)
private val AccentPurple = Color(0xFF8B5CF6)
private val AccentOrange = Color(0xFFFF9500)
private val AccentPink = Color(0xFFFF2D55)
private val TextWhite = Color(0xFFFFFFFF)
private val TextGray = Color(0xFF7A7A8E)
private val ButtonBg = Color(0xFF141420)
private val ButtonActiveBg = Color(0xFF1A2A3A)

enum class BeatSyncTool(
    val displayName: String,
    val icon: ImageVector,
    val color: Color
) {
    BEAT_DETECT("Beat Detect", Icons.Default.GraphicEq, AccentCyan),
    VOCAL_ISOLATOR("Vocal Isolator", Icons.Default.Mic, AccentPurple),
    AUDIO_FX("Audio FX", Icons.Default.Equalizer, AccentOrange),
    AUTO_SYNC("Auto-Sync", Icons.Default.Sync, AccentPink)
}

@Composable
fun BeatSyncToolbar(
    selectedTool: BeatSyncTool?,
    onToolSelected: (BeatSyncTool) -> Unit,
    isProcessing: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(BgDark)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        BeatSyncTool.entries.forEach { tool ->
            BeatSyncButton(
                tool = tool,
                isSelected = tool == selectedTool,
                isProcessing = isProcessing && tool == selectedTool,
                onClick = { onToolSelected(tool) }
            )
        }
    }
}

@Composable
private fun BeatSyncButton(
    tool: BeatSyncTool,
    isSelected: Boolean,
    isProcessing: Boolean,
    onClick: () -> Unit
) {
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) tool.color else Color.Transparent,
        label = "border"
    )

    val backgroundColor = if (isSelected) ButtonActiveBg else ButtonBg

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(72.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(backgroundColor)
            .border(
                width = if (isSelected) 2.dp else 0.dp,
                color = borderColor,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 4.dp)
    ) {
        // Icon with glow effect
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(32.dp)
        ) {
            if (isProcessing) {
                // Processing indicator
                CircularProgressIndicator(
                    modifier = Modifier.size(28.dp),
                    color = tool.color,
                    strokeWidth = 2.dp
                )
            }

            Icon(
                imageVector = tool.icon,
                contentDescription = tool.displayName,
                tint = if (isSelected) tool.color else TextGray,
                modifier = Modifier.size(22.dp)
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Label
        Text(
            text = tool.displayName,
            color = if (isSelected) tool.color else TextGray,
            fontSize = 10.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            maxLines = 1
        )
    }
}
