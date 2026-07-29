package com.powercut.editor.ui.editor.tools

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.powercut.editor.ui.theme.*

/**
 * Orientation Tools Panel
 * Dedicated tools for Vertical (9:16) and Horizontal (16:9) video editing
 * Features: Auto-reframe, Safe zone guides, Letterbox, Pillarbox, Platform presets
 */

data class PlatformPreset(
    val name: String,
    val emoji: String,
    val ratio: String,
    val desc: String,
    val width: Int,
    val height: Int
)

val verticalPresets = listOf(
    PlatformPreset("TikTok", "🎵", "9:16", "1080×1920", 1080, 1920),
    PlatformPreset("Reels", "📸", "9:16", "1080×1920", 1080, 1920),
    PlatformPreset("Shorts", "▶️", "9:16", "1080×1920", 1080, 1920),
    PlatformPreset("Stories", "📱", "9:16", "1080×1920", 1080, 1920),
    PlatformPreset("Snapchat", "👻", "9:16", "1080×1920", 1080, 1920),
    PlatformPreset("Portrait", "🧑", "3:4", "1080×1440", 1080, 1440),
)

val horizontalPresets = listOf(
    PlatformPreset("YouTube", "▶️", "16:9", "1920×1080", 1920, 1080),
    PlatformPreset("Cinema", "🎬", "21:9", "2560×1080", 2560, 1080),
    PlatformPreset("Widescreen", "🖥️", "16:9", "1920×1080", 1920, 1080),
    PlatformPreset("Facebook", "📘", "16:9", "1920×1080", 1920, 1080),
    PlatformPreset("Twitter", "🐦", "16:9", "1280×720", 1280, 720),
    PlatformPreset("LinkedIn", "💼", "16:9", "1920×1080", 1920, 1080),
)

val squarePresets = listOf(
    PlatformPreset("Instagram", "📸", "1:1", "1080×1080", 1080, 1080),
    PlatformPreset("Facebook", "📘", "1:1", "1080×1080", 1080, 1080),
    PlatformPreset("Profile", "👤", "1:1", "500×500", 500, 500),
)

@Composable
fun OrientationToolsPanel(
    orientationMode: String,
    aspectPreset: String,
    verticalSafeZone: Boolean,
    horizontalLetterbox: Boolean,
    autoReframeEnabled: Boolean,
    onUpdateOrientationMode: (String) -> Unit,
    onUpdateAspectPreset: (String) -> Unit,
    onToggleSafeZone: () -> Unit,
    onToggleLetterbox: () -> Unit,
    onToggleAutoReframe: () -> Unit
) {
    var selectedTab by remember { mutableStateOf("vertical") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Mode Tabs
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            listOf(
                Triple("vertical", "📱", "VERTICAL 9:16"),
                Triple("horizontal", "🖥️", "HORIZONTAL 16:9"),
                Triple("square", "⬜", "SQUARE 1:1")
            ).forEach { (tab, emoji, label) ->
                val isSel = selectedTab == tab
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            when {
                                isSel && tab == "vertical" -> NeonOrange.copy(alpha = 0.2f)
                                isSel && tab == "horizontal" -> CyberCyan.copy(alpha = 0.2f)
                                isSel -> Color(0xFFFFD700).copy(alpha = 0.2f)
                                else -> Color.White.copy(alpha = 0.04f)
                            }
                        )
                        .border(
                            1.dp,
                            when {
                                isSel && tab == "vertical" -> NeonOrange
                                isSel && tab == "horizontal" -> CyberCyan
                                isSel -> Color(0xFFFFD700)
                                else -> Color.White.copy(alpha = 0.08f)
                            },
                            RoundedCornerShape(10.dp)
                        )
                        .clickable {
                            selectedTab = tab
                            onUpdateOrientationMode(tab)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(emoji, fontSize = 12.sp)
                        Text(label, fontSize = 6.sp, fontWeight = FontWeight.Bold,
                            color = when {
                                isSel && tab == "vertical" -> NeonOrange
                                isSel && tab == "horizontal" -> CyberCyan
                                isSel -> Color(0xFFFFD700)
                                else -> Color.White
                            })
                    }
                }
            }
        }

        // Auto Reframe Toggle
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(Color.White.copy(alpha = 0.03f))
                .padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Default.AutoFixHigh, contentDescription = "Reframe", tint = CyberCyan, modifier = Modifier.size(14.dp))
                Column {
                    Text("AI AUTO REFRAME", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Text("Auto-track subject in frame", fontSize = 7.sp, color = Color.Gray)
                }
            }
            Switch(
                checked = autoReframeEnabled,
                onCheckedChange = { onToggleAutoReframe() },
                colors = SwitchDefaults.colors(checkedThumbColor = CyberCyan),
                modifier = Modifier.height(20.dp)
            )
        }

        // Platform Presets
        val presets = when (selectedTab) {
            "vertical" -> verticalPresets
            "horizontal" -> horizontalPresets
            "square" -> squarePresets
            else -> verticalPresets
        }

        Text("PLATFORM PRESETS", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color.Gray)

        presets.forEach { preset ->
            val isSel = aspectPreset == preset.ratio
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (isSel) NeonOrange.copy(alpha = 0.1f) else Color.White.copy(alpha = 0.02f))
                    .border(1.dp, if (isSel) NeonOrange else Color.White.copy(alpha = 0.05f), RoundedCornerShape(10.dp))
                    .clickable { onUpdateAspectPreset(preset.ratio) }
                    .padding(horizontal = 10.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(preset.emoji, fontSize = 16.sp)
                        Column {
                            Text(preset.name, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            Text("${preset.ratio} • ${preset.desc}", fontSize = 8.sp, color = Color.Gray)
                        }
                    }
                    if (isSel) {
                        Icon(Icons.Default.CheckCircle, contentDescription = "Selected", tint = NeonOrange, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Safe Zone / Letterbox toggles
        if (selectedTab == "vertical") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.White.copy(alpha = 0.03f))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("VERTICAL SAFE ZONE", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Text("Show guide lines for 9:16 safe area", fontSize = 7.sp, color = Color.Gray)
                }
                Switch(
                    checked = verticalSafeZone,
                    onCheckedChange = { onToggleSafeZone() },
                    colors = SwitchDefaults.colors(checkedThumbColor = NeonOrange),
                    modifier = Modifier.height(20.dp)
                )
            }
        }

        if (selectedTab == "horizontal") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.White.copy(alpha = 0.03f))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("LETTERBOX MODE", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Text("Add black bars for non-16:9 content", fontSize = 7.sp, color = Color.Gray)
                }
                Switch(
                    checked = horizontalLetterbox,
                    onCheckedChange = { onToggleLetterbox() },
                    colors = SwitchDefaults.colors(checkedThumbColor = CyberCyan),
                    modifier = Modifier.height(20.dp)
                )
            }
        }

        // Tips
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(Color.White.copy(alpha = 0.02f))
                .padding(8.dp)
        ) {
            val tip = when (selectedTab) {
                "vertical" -> "📱 Vertical mode optimizes for mobile viewing. Use AI Auto Reframe to keep subjects centered."
                "horizontal" -> "🖥️ Horizontal mode is ideal for YouTube and desktop viewing. Enable letterbox for mixed content."
                "square" -> "⬜ Square format works great for Instagram posts and profile videos."
                else -> ""
            }
            Text(tip, fontSize = 9.sp, color = Color.Gray, lineHeight = 13.sp)
        }
    }
}
