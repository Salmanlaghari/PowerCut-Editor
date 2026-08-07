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
import com.powercut.model.DAGNode
import com.powercut.ui.components.DemoThumbnail
import com.powercut.ui.components.ProBadge
import com.powercut.ui.components.baseScene
import com.powercut.ui.components.powercutGradientBrush
import com.powercut.ui.editor.EditorViewModel
import com.powercut.ui.theme.*

/**
 * Presets — social / platform aspect-ratio presets. Tapping a preset writes
 * a Filter DAG node carrying the crop + aspect params so the native resolver
 * applies it during export.
 */
@Composable
fun PresetsScreen(
    onClose: () -> Unit,
    vm: EditorViewModel
) {
    var selectedId by remember { mutableStateOf<String?>(null) }

    Box(modifier = Modifier.fillMaxSize().background(Bg)) {
        Column(
            modifier = Modifier.fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()) {
                Text("Presets", color = TextPrimary, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.weight(1f))
                GradientPillCompact(text = "Close", onClick = onClose)
            }
            Spacer(Modifier.height(6.dp))
            Text("Aspect ratios & platform presets · tap to apply",
                color = TextSecondary, fontSize = 13.sp)
            Spacer(Modifier.height(20.dp))

            val rows = PRESET_DEFS.chunked(2)
            rows.forEach { rowItems ->
                Row(modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    rowItems.forEach { p ->
                        PresetCard(
                            preset = p,
                            selected = selectedId == p.id,
                            modifier = Modifier.weight(1f),
                            onTap = {
                                selectedId = p.id
                                vm.addDagNode(
                                    DAGNode.Kind.Filter,
                                    """{"preset":"${p.id}","aspect":"${p.aspect}"}"""
                                )
                            }
                        )
                    }
                    if (rowItems.size < 2) Spacer(Modifier.weight(1f))
                }
                Spacer(Modifier.height(12.dp))
            }
            Spacer(Modifier.height(80.dp))
        }
    }
}

@Composable
private fun PresetCard(
    preset: PresetDef,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onTap: () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(BgCard)
            .then(
                if (selected) Modifier.border(2.dp, powercutGradientBrush(), RoundedCornerShape(18.dp))
                else Modifier.border(1.dp, GlassStroke, RoundedCornerShape(18.dp))
            )
            .pointerInput(preset.id) { detectTapGestures(onTap = { onTap() }) }
    ) {
        Column(modifier = Modifier.padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            AspectPreview(preset.aspect, preset.id, Modifier.fillMaxWidth().height(90.dp))
            Spacer(Modifier.height(10.dp))
            Text(preset.name, color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            Text(preset.desc, color = TextSecondary, fontSize = 12.sp)
        }
        if (preset.pro) ProBadge(modifier = Modifier.align(Alignment.TopEnd).padding(6.dp))
    }
}

/** Draws a correctly-proportioned frame box so the user sees the real aspect. */
@Composable
private fun AspectPreview(aspect: Float, id: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .background(PureBlack)
            .clip(RoundedCornerShape(10.dp)),
        contentAlignment = Alignment.Center
    ) {
        androidx.compose.foundation.Canvas(
            modifier = Modifier.fillMaxSize()
        ) {
            val w: Float
            val h: Float
            val maxW = size.width * 0.9f
            val maxH = size.height * 0.9f
            if (maxW / aspect <= maxH) {
                w = maxW; h = maxW / aspect
            } else {
                h = maxH; w = maxH * aspect
            }
            val x = (size.width - w) / 2f
            val y = (size.height - h) / 2f
            // simulated scene inside the frame
            drawRect(
                androidx.compose.ui.graphics.Brush.verticalGradient(
                    listOf(androidx.compose.ui.graphics.Color(0xFF2A2A40),
                        androidx.compose.ui.graphics.Color(0xFF1C1C2E))
                ),
                androidx.compose.ui.geometry.Offset(x, y),
                androidx.compose.ui.geometry.Size(w, h)
            )
            // frame border
            drawRect(
                androidx.compose.ui.graphics.Color(0xFFFF5A3C),
                androidx.compose.ui.geometry.Offset(x, y),
                androidx.compose.ui.geometry.Size(w, h),
                style = androidx.compose.ui.graphics.drawscope.Stroke(
                    width = size.minDimension * 0.012f
                )
            )
        }
    }
}

private data class PresetDef(
    val id: String, val name: String, val desc: String,
    val aspect: Float, val pro: Boolean = false
)

private val PRESET_DEFS = listOf(
    PresetDef("ig_square", "Instagram Square", "1:1 feed post", 1f),
    PresetDef("ig_story", "IG Story / Reels", "9:16 vertical", 9f / 16f, pro = true),
    PresetDef("yt_short", "YouTube Shorts", "9:16 vertical", 9f / 16f),
    PresetDef("tiktok", "TikTok", "9:16 full screen", 9f / 16f),
    PresetDef("yt_wide", "YouTube Wide", "16:9 landscape", 16f / 9f),
    PresetDef("cinema", "Cinematic", "2.35:1 anamorphic", 2.35f, pro = true),
    PresetDef("portrait", "Portrait 4:5", "Feed portrait", 4f / 5f),
    PresetDef("twitter", "X / Twitter", "16:9 timeline", 16f / 9f)
)
