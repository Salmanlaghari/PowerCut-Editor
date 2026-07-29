package com.powercut.editor.ui.editor.tools

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.powercut.editor.ui.theme.*

/**
 * Green Screen / Chroma Key Tool Panel
 * Features: Green/Blue/Black/White screen removal, 20+ auto backgrounds, threshold control
 */

data class AutoBackground(
    val name: String,
    val emoji: String,
    val gradient: List<Color>,
    val category: String
)

val autoBackgrounds = listOf(
    // Nature
    AutoBackground("Forest", "🌲", listOf(Color(0xFF1B5E20), Color(0xFF4CAF50)), "Nature"),
    AutoBackground("Ocean", "🌊", listOf(Color(0xFF0D47A1), Color(0xFF42A5F5)), "Nature"),
    AutoBackground("Sunset", "🌅", listOf(Color(0xFFFF6F00), Color(0xFFFFCA28)), "Nature"),
    AutoBackground("Mountain", "⛰️", listOf(Color(0xFF37474F), Color(0xFF78909C)), "Nature"),
    AutoBackground("Sky Blue", "🌤️", listOf(Color(0xFF1565C0), Color(0xFF64B5F6)), "Nature"),
    AutoBackground("Aurora", "🌌", listOf(Color(0xFF4A148C), Color(0xFF00E676)), "Nature"),
    AutoBackground("Desert", "🏜️", listOf(Color(0xFFBF360C), Color(0xFFFFB74D)), "Nature"),
    AutoBackground("Snow", "❄️", listOf(Color(0xFFE3F2FD), Color(0xFFFFFFFF)), "Nature"),

    // Studio
    AutoBackground("White Studio", "⬜", listOf(Color(0xFFFAFAFA), Color(0xFFEEEEEE)), "Studio"),
    AutoBackground("Black Studio", "⬛", listOf(Color(0xFF212121), Color(0xFF424242)), "Studio"),
    AutoBackground("Gray Gradient", "🔘", listOf(Color(0xFF616161), Color(0xFFBDBDBD)), "Studio"),
    AutoBackground("Warm Studio", "💡", listOf(Color(0xFFFFF3E0), Color(0xFFFFE0B2)), "Studio"),
    AutoBackground("Cool Studio", "❄️", listOf(Color(0xFFE3F2FD), Color(0xFFBBDEFB)), "Studio"),
    AutoBackground("Red Studio", "🔴", listOf(Color(0xFFB71C1C), Color(0xFFE53935)), "Studio"),
    AutoBackground("Blue Studio", "🔵", listOf(Color(0xFF0D47A1), Color(0xFF1E88E5)), "Studio"),
    AutoBackground("Green Studio", "🟢", listOf(Color(0xFF1B5E20), Color(0xFF43A047)), "Studio"),

    // Creative
    AutoBackground("Neon City", "🌃", listOf(Color(0xFF000000), Color(0xFF7C4DFF)), "Creative"),
    AutoBackground("Fire", "🔥", listOf(Color(0xFFBF360C), Color(0xFFFF6D00)), "Creative"),
    AutoBackground("Galaxy", "🚀", listOf(Color(0xFF0A0E27), Color(0xFF6C63FF)), "Creative"),
    AutoBackground("Matrix", "💚", listOf(Color(0xFF000000), Color(0xFF00E676)), "Creative"),
    AutoBackground("Retro", "📼", listOf(Color(0xFF4A148C), Color(0xFFFF6F00)), "Creative"),
    AutoBackground("Pastel", "🎨", listOf(Color(0xFFFCE4EC), Color(0xFFE1BEE7)), "Creative"),
    AutoBackground("Golden", "✨", listOf(Color(0xFFFFD700), Color(0xFFFFA000)), "Creative"),
    AutoBackground("Midnight", "🌙", listOf(Color(0xFF0D1B2A), Color(0xFF1B2838)), "Creative"),

    // Gradient
    AutoBackground("Gradient Pink", "💗", listOf(Color(0xFFE91E63), Color(0xFFFF5252)), "Gradient"),
    AutoBackground("Gradient Teal", "💎", listOf(Color(0xFF009688), Color(0xFF4DB6AC)), "Gradient"),
    AutoBackground("Gradient Purple", "💜", listOf(Color(0xFF7B1FA2), Color(0xFFCE93D8)), "Gradient"),
    AutoBackground("Gradient Orange", "🧡", listOf(Color(0xFFE65100), Color(0xFFFFAB40)), "Gradient"),
    AutoBackground("Gradient Cyan", "🩵", listOf(Color(0xFF006064), Color(0xFF00BCD4)), "Gradient"),
    AutoBackground("Gradient Lime", "💚", listOf(Color(0xFF827717), Color(0xFFC6FF00)), "Gradient"),
)

@Composable
fun GreenScreenPanel(
    greenScreenEnabled: Boolean,
    greenScreenColor: String,
    greenScreenThreshold: Float,
    greenScreenAutoBgIndex: Int,
    onToggleGreenScreen: () -> Unit,
    onUpdateGreenScreenColor: (String) -> Unit,
    onUpdateThreshold: (Float) -> Unit,
    onSelectAutoBackground: (Int) -> Unit,
    onPickCustomBackground: () -> Unit
) {
    var selectedBgCategory by remember { mutableStateOf("Nature") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Toggle + Color Selection
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("GREEN SCREEN", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Switch(
                checked = greenScreenEnabled,
                onCheckedChange = { onToggleGreenScreen() },
                colors = SwitchDefaults.colors(checkedThumbColor = NeonOrange),
                modifier = Modifier.height(24.dp)
            )
        }

        // Chroma Key Color Selection
        Text("CHROMA KEY COLOR", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            listOf(
                "green" to Color(0xFF4CAF50),
                "blue" to Color(0xFF2196F3),
                "black" to Color(0xFF212121),
                "white" to Color(0xFFFAFAFA)
            ).forEach { (name, color) ->
                val isSel = greenScreenColor == name
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(color)
                        .border(
                            width = if (isSel) 2.dp else 1.dp,
                            color = if (isSel) NeonOrange else Color.White.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(8.dp)
                        )
                        .clickable { onUpdateGreenScreenColor(name) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        name.uppercase(),
                        fontSize = 7.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (name == "white") Color.Black else Color.White
                    )
                }
            }
        }

        // Threshold Slider
        Text("SENSITIVITY: ${(greenScreenThreshold * 100).toInt()}%", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
        Slider(
            value = greenScreenThreshold,
            onValueChange = onUpdateThreshold,
            valueRange = 0.1f..0.9f,
            colors = SliderDefaults.colors(activeTrackColor = NeonOrange, thumbColor = NeonOrange),
            modifier = Modifier.height(24.dp)
        )

        // Auto Background Category Tabs
        Text("20+ AUTO BACKGROUNDS", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = CyberCyan)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            items(listOf("Nature", "Studio", "Creative", "Gradient")) { cat ->
                val isSel = selectedBgCategory == cat
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (isSel) NeonOrange.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.04f))
                        .border(1.dp, if (isSel) NeonOrange else Color.White.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
                        .clickable { selectedBgCategory = cat }
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(cat, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = if (isSel) NeonOrange else Color.White)
                }
            }
        }

        // Background Grid
        val filteredBgs = autoBackgrounds.filter { it.category == selectedBgCategory }
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            itemsIndexed(filteredBgs) { index, bg ->
                val globalIndex = autoBackgrounds.indexOf(bg)
                val isSel = greenScreenAutoBgIndex == globalIndex
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Brush.horizontalGradient(bg.gradient))
                        .border(
                            width = if (isSel) 2.dp else 1.dp,
                            color = if (isSel) NeonOrange else Color.White.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .clickable { onSelectAutoBackground(globalIndex) },
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(bg.emoji, fontSize = 18.sp)
                        Text(bg.name, fontSize = 6.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }

        // Custom Background Button
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .border(1.dp, CyberCyan.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                .clickable { onPickCustomBackground() }
                .padding(10.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Default.Image, contentDescription = "Custom", tint = CyberCyan, modifier = Modifier.size(14.dp))
                Text("PICK CUSTOM BACKGROUND", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = CyberCyan)
            }
        }
    }
}
