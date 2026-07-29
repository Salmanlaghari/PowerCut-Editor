package com.powercut.editor.ui.editor.tools

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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

    data class Param(val key: String, val emoji: String, val label: String, val value: Float, val range: ClosedFloatingPointRange<Float>, val onUpdate: (Float) -> Unit)

    val params = listOf(
        Param("brightness", "☀️", "Brightness", brightness, -1f..1f, onUpdateBrightness),
        Param("contrast", "🔲", "Contrast", contrast, 0f..2f, onUpdateContrast),
        Param("saturation", "🎨", "Saturation", saturation, 0f..2f, onUpdateSaturation),
        Param("exposure", "💡", "Exposure", exposure, -1f..1f, onUpdateExposure),
        Param("highlights", "✨", "Highlights", highlights, -1f..1f, onUpdateHighlights),
        Param("shadows", "🌑", "Shadows", shadows, -1f..1f, onUpdateShadows),
        Param("temperature", "🌡️", "Temperature", temperature, -1f..1f, onUpdateTemperature),
        Param("blur", "🔵", "Blur", blur, 0f..25f, onUpdateBlur),
        Param("sharpen", "🔪", "Sharpen", sharpen, 0f..1f, onUpdateSharpen),
        Param("vignette", "⭕", "Vignette", vignette, 0f..1f, onUpdateVignette),
        Param("grain", "📺", "Grain", grain, 0f..1f, onUpdateGrain),
        Param("fade", "🌫️", "Fade", fade, 0f..1f, onUpdateFade),
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
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

        val rows = params.chunked(2)
        rows.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                row.forEach { param ->
                    val isSel = activeParam == param.key
                    val displayVal = if (param.key == "blur" || param.key == "grain" || param.key == "fade" || param.key == "vignette" || param.key == "sharpen") {
                        "${(param.value * 100).toInt()}%"
                    } else {
                        String.format("%.1f", param.value)
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSel) NeonOrange.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.03f))
                            .border(1.dp, if (isSel) NeonOrange else Color.White.copy(alpha = 0.06f), RoundedCornerShape(8.dp))
                            .clickable { activeParam = param.key }
                            .padding(horizontal = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("${param.emoji} ${param.label}", fontSize = 7.sp, fontWeight = FontWeight.Bold, color = if (isSel) NeonOrange else Color.White)
                            Text(displayVal, fontSize = 8.sp, fontWeight = FontWeight.Bold, color = if (isSel) CyberCyan else Color.Gray)
                        }
                    }
                }
                if (row.size == 1) Spacer(modifier = Modifier.weight(1f))
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        val activeParamData = params.find { it.key == activeParam }
        if (activeParamData != null) {
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
                        Text("${activeParamData.emoji} ${activeParamData.label}", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Text(
                            if (activeParamData.key == "blur" || activeParamData.key == "grain" || activeParamData.key == "fade" || activeParamData.key == "vignette" || activeParamData.key == "sharpen")
                                "${(activeParamData.value * 100).toInt()}%"
                            else
                                String.format("%.2f", activeParamData.value),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = CyberCyan
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Slider(
                        value = activeParamData.value,
                        onValueChange = activeParamData.onUpdate,
                        valueRange = activeParamData.range,
                        colors = SliderDefaults.colors(activeTrackColor = NeonOrange, thumbColor = NeonOrange),
                        modifier = Modifier.height(24.dp)
                    )
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color.White.copy(alpha = 0.04f))
                            .clickable { activeParamData.onUpdate((activeParamData.range.start + activeParamData.range.endInclusive) / 2f) }
                            .padding(horizontal = 12.dp, vertical = 3.dp)
                    ) {
                        Text("RESET ${activeParamData.label.uppercase()}", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                    }
                }
            }
        }
    }
}
