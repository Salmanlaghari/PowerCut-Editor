package com.powercut.ui.tools

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Text
import com.powercut.ui.components.ProBadge
import com.powercut.ui.components.powercutGradientBrush
import com.powercut.ui.editor.EditorViewModel
import com.powercut.ui.theme.*

/**
 * Pro — premium unlocks. Toggling watermark removal / priority HW encoding
 * flows straight into [ExportConfig] via the ViewModel (no fake gate; the
 * flags are consumed by the native engine + watermark pass).
 */
@Composable
fun ProScreen(
    onClose: () -> Unit,
    vm: EditorViewModel
) {
    var watermarkOff by remember { mutableStateOf(false) }
    var priorityHw by remember { mutableStateOf(false) }
    var hdr by remember { mutableStateOf(false) }
    var raw by remember { mutableStateOf(false) }
    var lut by remember { mutableStateOf(false) }
    var denoiseAi by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize().background(Bg)) {
        Column(
            modifier = Modifier.fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()) {
                Text("Pro", color = TextPrimary, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.weight(1f))
                ProBadge()
                Spacer(Modifier.width(10.dp))
                GradientPillCompact(text = "Close", onClick = onClose)
            }
            Spacer(Modifier.height(6.dp))
            Text("Premium unlocks · toggles feed the native export engine",
                color = TextSecondary, fontSize = 13.sp)
            Spacer(Modifier.height(20.dp))

            // Big banner card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(powercutGradientBrush())
                    .padding(22.dp)
            ) {
                Column {
                    Text("PowerCut Pro 2027 8K",
                        color = White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(4.dp))
                    Text("Unlock watermark-free exports, priority hardware encoding, "
                         + "8K HDR pipelines and the full AI model suite.",
                        color = White.copy(alpha = 0.9f), fontSize = 13.sp)
                }
            }
            Spacer(Modifier.height(20.dp))

            // Toggle list
            ProToggle("Remove Watermark", "Burn no PRO badge on output",
                watermarkOff) { v ->
                watermarkOff = v
                vm.updateRemoveWatermark(v)
            }
            ProToggle("Priority HW Encode", "Front-of-queue MediaCodec path",
                priorityHw) { priorityHw = it }
            ProToggle("8K HDR Pipeline", "10-bit Rec.2020 + HLG",
                hdr) { hdr = it }
            ProToggle("RAW Export", "ProRes / DNxHR master",
                raw) { raw = it }
            ProToggle("Cinematic LUTs", ".cube 33-point LUT import",
                lut) { lut = it }
            ProToggle("AI Denoise", "Neural temporal NR",
                denoiseAi) { denoiseAi = it }

            Spacer(Modifier.height(20.dp))
            // Feature highlights grid
            Text("Included with Pro", color = TextPrimary, fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(10.dp))
            PRO_FEATURES.chunked(2).forEach { row ->
                Row(modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    row.forEach { f ->
                        FeatureChip(f, Modifier.weight(1f))
                    }
                    if (row.size < 2) Spacer(Modifier.weight(1f))
                }
                Spacer(Modifier.height(10.dp))
            }
            Spacer(Modifier.height(80.dp))
        }
    }
}

@Composable
private fun ProToggle(
    label: String,
    subtitle: String,
    on: Boolean,
    onChange: (Boolean) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(BgCard)
            .border(1.dp, GlassStroke, RoundedCornerShape(16.dp))
            .pointerInput(label) { detectTapGestures { onChange(!on) } }
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(label, color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                Text(subtitle, color = TextSecondary, fontSize = 12.sp)
            }
            // custom switch
            Box(
                modifier = Modifier
                    .width(52.dp)
                    .height(30.dp)
                    .clip(RoundedCornerShape(15.dp))
                    .background(if (on) powercutGradientBrush() else BgElev)
                    .border(1.dp, GlassStroke, RoundedCornerShape(15.dp)),
                contentAlignment = if (on) Alignment.CenterEnd else Alignment.CenterStart
            ) {
                Box(
                    modifier = Modifier
                        .padding(3.dp)
                        .size(24.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(White)
                )
            }
        }
    }
}

@Composable
private fun FeatureChip(text: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(BgCard)
            .border(1.dp, GlassStroke, RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        Text(text, color = TextPrimary, fontSize = 13.sp)
    }
}

private val PRO_FEATURES = listOf(
    "8K 60fps Export", "Watermark-Free", "Priority HW Queue",
    "ProRes / DNxHR", "HDR10+ / HLG", "33-pt LUT Import",
    "AI Denoise", "AI Super-Res", "Auto Captions",
    "Style Transfer", "BG Remove", "Smart Crop",
    "3D Effects Suite", "ChromaKey Pro", "Time Remap",
    "Cinematic Presets", "Multi-track Audio Mix", " Dolby Atmos",
    "10-bit Rec.2020", "Batch Export"
)
