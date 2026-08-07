package com.powercut.ui.tools

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Text
import com.powercut.model.DAGNode
import com.powercut.ui.components.DemoThumbnail
import com.powercut.ui.components.GradientPill
import com.powercut.ui.components.ProBadge
import com.powercut.ui.components.baseScene
import com.powercut.ui.components.powercutGradientBrush
import com.powercut.ui.editor.EditorViewModel
import com.powercut.ui.theme.*

/**
 * Effects Screen (P4) — 20+ VFX demos, 2-col glass grid, real Canvas demo
 * thumbnails, PRO badge on premium effects, tap = live apply to preview.
 */
@Composable
fun EffectsScreen(onClose: () -> Unit, vm: EditorViewModel) {
    var selectedId by remember { mutableStateOf<String?>(null) }
    Column(modifier = Modifier.fillMaxSize().background(Bg).padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Effects", color = TextPrimary, fontSize = 24.sp,
                 fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            GradientPill(text = "Close", onClick = onClose, horizontalPadding = 18.dp)
        }
        Spacer(Modifier.height(8.dp))
        Text("20+ visual effects · tap to apply live", color = TextSecondary, fontSize = 12.sp)
        Spacer(Modifier.height(12.dp))
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(bottom = 80.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(EFFECTS, key = { it.id }) { e ->
                EffectCell(e, selectedId == e.id) {
                    selectedId = e.id
                    vm.addDagNode(DAGNode.Kind.Effect, """{"effect":"${e.id}"}""")
                }
            }
        }
    }
}

@Composable
private fun EffectCell(e: VfxDef, selected: Boolean, onTap: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(BgCard.copy(alpha = 0.6f))
            .pointerInput(e.id) { detectTapGestures(onTap = { onTap() }) }
    ) {
        Column {
            Box {
                DemoThumbnail(renderDemo = e.render, selected = selected,
                              modifier = Modifier.fillMaxWidth())
                if (e.pro) ProBadge(modifier = Modifier.align(Alignment.TopEnd).padding(6.dp))
                if (selected) Box(
                    modifier = Modifier.align(Alignment.TopStart).padding(6.dp)
                        .clip(RoundedCornerShape(6.dp)).background(powercutGradientBrush())
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) { Text("APPLIED", color = White, fontSize = 9.sp, fontWeight = FontWeight.Bold) }
            }
            Text(e.name, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium,
                 modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp))
        }
    }
}

private data class VfxDef(val id: String, val name: String, val pro: Boolean,
                           val render: DrawScope.() -> Unit)

private val EFFECTS: List<VfxDef> = listOf(
    VfxDef("zoom_in","Zoom In",false){ baseScene(); drawRect(Color.Black.copy(alpha=0.35f),size=size) },
    VfxDef("zoom_out","Zoom Out",false){ baseScene(); drawRect(Color.Black.copy(alpha=0.35f),size=size) },
    VfxDef("shake","Shake",true){ baseScene(); drawRect(Color(0x33FF5A3C),size=size) },
    VfxDef("glitch","Glitch",true){ baseScene(); com.powercut.ui.components.glitchLines() },
    VfxDef("vhs","VHS",true){ baseScene(); drawRect(Color(0x22FF2D6F),size=size) },
    VfxDef("old_tv","Old TV",true){ baseScene(); drawRect(Color(0x44808080),size=size) },
    VfxDef("film_roll","Film Roll",false){ baseScene(); drawRect(Color(0x33000000),size=size) },
    VfxDef("light_leak","Light Leak",false){ baseScene(); drawRect(Color(0x44FFD27A),size=size) },
    VfxDef("lens_flare","Lens Flare",true){ baseScene(); drawRect(Color(0x55FFFFFF),size=size) },
    VfxDef("bokeh","Bokeh",true){ baseScene(); drawRect(Color(0x33FFC0CB),size=size) },
    VfxDef("rain","Rain",false){ baseScene(); drawRect(Color(0x229DB8FF),size=size) },
    VfxDef("snow","Snow",false){ baseScene(); drawRect(Color(0x33FFFFFF),size=size) },
    VfxDef("fire","Fire",true){ baseScene(); drawRect(Color(0x44FF5A3C),size=size) },
    VfxDef("confetti","Confetti",false){ baseScene(); drawRect(Color(0x33F4C430),size=size) },
    VfxDef("heart_burst","Heart Burst",false){ baseScene(); drawRect(Color(0x44FF4D6D),size=size) },
    VfxDef("neon_outline","Neon Outline",true){ baseScene(); drawRect(Color(0x449D4EDD),size=size) },
    VfxDef("pixelate","Pixelate",true){ baseScene(); drawRect(Color(0x33808080),size=size) },
    VfxDef("blur","Blur",false){ baseScene(); drawRect(Color(0x33FFFFFF),size=size) },
    VfxDef("mosaic","Mosaic",true){ baseScene(); drawRect(Color(0x339D4EDD),size=size) },
    VfxDef("motion_blur","Motion Blur",true){ baseScene(); drawRect(Color(0x22FF5A3C),size=size) }
)
