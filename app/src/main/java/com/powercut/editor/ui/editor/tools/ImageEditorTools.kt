package com.powercut.editor.ui.editor.tools

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.powercut.editor.ui.theme.*

@Composable
fun ImageEditorPanel(
    brightness: Float,
    contrast: Float,
    saturation: Float,
    blur: Float,
    sharpen: Float,
    temperature: Float,
    vignette: Float,
    grain: Float,
    fade: Float,
    highlights: Float,
    shadows: Float,
    exposure: Float,
    onUpdateBrightness: (Float) -> Unit,
    onUpdateContrast: (Float) -> Unit,
    onUpdateSaturation: (Float) -> Unit,
    onUpdateBlur: (Float) -> Unit,
    onUpdateSharpen: (Float) -> Unit,
    onUpdateTemperature: (Float) -> Unit,
    onUpdateVignette: (Float) -> Unit,
    onUpdateGrain: (Float) -> Unit,
    onUpdateFade: (Float) -> Unit,
    onUpdateHighlights: (Float) -> Unit,
    onUpdateShadows: (Float) -> Unit,
    onUpdateExposure: (Float) -> Unit,
    onResetAll: () -> Unit
) {
    var activeParam by remember { mutableStateOf("brightness") }

    Column(
        modifier = Modifier.fillMaxSize().padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("IMAGE EDITOR", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Box(
                modifier = Modifier.clip(RoundedCornerShape(6.dp))
                    .background(Color.White.copy(alpha = 0.04f))
                    .clickable { onResetAll() }
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text("RESET ALL", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = NeonOrange)
            }
        }

        // Param buttons in rows of 3
        val paramKeys = listOf("brightness", "contrast", "saturation", "exposure", "highlights", "shadows", "temperature", "blur", "sharpen", "vignette", "grain", "fade")
        val paramEmojis = listOf("☀️", "🔲", "🎨", "💡", "✨", "🌑", "🌡️", "🔵", "🔪", "⭕", "📺", "🌫️")
        val paramLabels = listOf("Bright", "Contrast", "Satur", "Expose", "High", "Shadow", "Temp", "Blur", "Sharp", "Vign", "Grain", "Fade")

        val rows = paramKeys.indices.toList().chunked(3)
        rows.forEach { rowIndices ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                rowIndices.forEach { idx ->
                    val key = paramKeys[idx]
                    val isSel = activeParam == key
                    Box(
                        modifier = Modifier.weight(1f).height(32.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isSel) NeonOrange.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.03f))
                            .border(1.dp, if (isSel) NeonOrange else Color.White.copy(alpha = 0.06f), RoundedCornerShape(6.dp))
                            .clickable { activeParam = key },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("${paramEmojis[idx]} ${paramLabels[idx]}", fontSize = 7.sp, fontWeight = FontWeight.Bold, color = if (isSel) NeonOrange else Color.White)
                    }
                }
                repeat(3 - rowIndices.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Active slider
        val (currentValue, currentRange, currentOnUpdate) = when (activeParam) {
            "brightness" -> Triple(brightness, -1f..1f, onUpdateBrightness)
            "contrast" -> Triple(contrast, 0f..2f, onUpdateContrast)
            "saturation" -> Triple(saturation, 0f..2f, onUpdateSaturation)
            "exposure" -> Triple(exposure, -1f..1f, onUpdateExposure)
            "highlights" -> Triple(highlights, -1f..1f, onUpdateHighlights)
            "shadows" -> Triple(shadows, -1f..1f, onUpdateShadows)
            "temperature" -> Triple(temperature, -1f..1f, onUpdateTemperature)
            "blur" -> Triple(blur, 0f..25f, onUpdateBlur)
            "sharpen" -> Triple(sharpen, 0f..1f, onUpdateSharpen)
            "vignette" -> Triple(vignette, 0f..1f, onUpdateVignette)
            "grain" -> Triple(grain, 0f..1f, onUpdateGrain)
            "fade" -> Triple(fade, 0f..1f, onUpdateFade)
            else -> Triple(brightness, -1f..1f, onUpdateBrightness)
        }

        Box(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
                .background(Color.White.copy(alpha = 0.03f))
                .padding(8.dp)
        ) {
            Column {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(activeParam.uppercase(), fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Text(String.format("%.2f", currentValue), fontSize = 9.sp, fontWeight = FontWeight.Bold, color = CyberCyan)
                }
                Slider(
                    value = currentValue,
                    onValueChange = currentOnUpdate,
                    valueRange = currentRange,
                    colors = SliderDefaults.colors(activeTrackColor = NeonOrange, thumbColor = NeonOrange),
                    modifier = Modifier.height(24.dp)
                )
            }
        }
    }
}
