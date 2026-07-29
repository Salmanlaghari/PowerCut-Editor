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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.powercut.editor.ui.theme.*

/**
 * Image Editor Tools Panel
 * Full image editing suite: Brightness, Contrast, Saturation, Blur, Sharpen,
 * Temperature, Vignette, Grain, Fade, Highlights, Shadows, Exposure
 */

data class ImageEditParam(
    val label: String,
    val emoji: String,
    val value: Float,
    val range: ClosedFloatingPointRange<Float>,
    val defaultValue: Float,
    val color: Color
)

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

    val params = listOf(
        Triple("brightness", "☀️", "Brightness") to Triple(brightness, -1f..1f, onUpdateBrightness),
        Triple("contrast", "🔲", "Contrast") to Triple(contrast, 0f..2f, onUpdateContrast),
        Triple("saturation", "🎨", "Saturation") to Triple(saturation, 0f..2f, onUpdateSaturation),
        Triple("exposure", "💡", "Exposure") to Triple(exposure, -1f..1f, onUpdateExposure),
        Triple("highlights", "✨", "Highlights") to Triple(highlights, -1f..1f, onUpdateHighlights),
        Triple("shadows", "🌑", "Shadows") to Triple(shadows, -1f..1f, onUpdateShadows),
        Triple("temperature", "🌡️", "Temperature") to Triple(temperature, -1f..1f, onUpdateTemperature),
        Triple("blur", "🔵", "Blur") to Triple(blur, 0f..25f, onUpdateBlur),
        Triple("sharpen", "🔪", "Sharpen") to Triple(sharpen, 0f..1f, onUpdateSharpen),
        Triple("vignette", "⭕", "Vignette") to Triple(vignette, 0f..1f, onUpdateVignette),
        Triple("grain", "📺", "Grain") to Triple(grain, 0f..1f, onUpdateGrain),
        Triple("fade", "🌫️", "Fade") to Triple(fade, 0f..1f, onUpdateFade),
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Header with Reset
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("IMAGE EDITOR", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color.White.copy(alpha = 0.04f))
                    .clickable { onResetAll() }
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text("RESET ALL", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = NeonOrange)
            }
        }

        // Parameter Grid (2 columns)
        val rows = params.chunked(2)
        rows.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                row.forEach { (key, emoji, label) to (value, range, onUpdate) ->
                    val isSel = activeParam == key
                    val displayVal = if (key == "blur" || key == "grain" || key == "fade" || key == "vignette" || key == "sharpen") {
                        "${(value * 100).toInt()}%"
                    } else {
                        String.format("%.1f", value)
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSel) NeonOrange.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.03f))
                            .border(1.dp, if (isSel) NeonOrange else Color.White.copy(alpha = 0.06f), RoundedCornerShape(8.dp))
                            .clickable { activeParam = key }
                            .padding(horizontal = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("$emoji $label", fontSize = 7.sp, fontWeight = FontWeight.Bold, color = if (isSel) NeonOrange else Color.White)
                            Text(displayVal, fontSize = 8.sp, fontWeight = FontWeight.Bold, color = if (isSel) CyberCyan else Color.Gray)
                        }
                    }
                }
                // Fill empty space if odd number
                if (row.size == 1) Spacer(modifier = Modifier.weight(1f))
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Active Parameter Slider
        val activeParamData = params.find { (key, _, _) -> key == activeParam }
        if (activeParamData != null) {
            val (key, emoji, label) to (value, range, onUpdate) = activeParamData

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.White.copy(alpha = 0.03f))
                    .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(10.dp))
                    .padding(10.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("$emoji $label", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Text(
                            if (key == "blur" || key == "grain" || key == "fade" || key == "vignette" || key == "sharpen")
                                "${(value * 100).toInt()}%"
                            else
                                String.format("%.2f", value),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = CyberCyan
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Slider(
                        value = value,
                        onValueChange = onUpdate,
                        valueRange = range,
                        colors = SliderDefaults.colors(
                            activeTrackColor = NeonOrange,
                            thumbColor = NeonOrange
                        ),
                        modifier = Modifier.height(24.dp)
                    )
                    // Reset button for this param
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color.White.copy(alpha = 0.04f))
                            .clickable { onUpdate((range.start + range.endInclusive) / 2f) }
                            .padding(horizontal = 12.dp, vertical = 3.dp)
                    ) {
                        Text("RESET $label", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                    }
                }
            }
        }
    }
}
