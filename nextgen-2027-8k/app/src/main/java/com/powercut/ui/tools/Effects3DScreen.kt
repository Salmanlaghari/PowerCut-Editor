package com.powercut.ui.tools

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.pointerInput
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
 * 3D Effects Screen (NEW, P4) — 8 effects: 3D Rotate, Flip, Perspective,
 * Cube Spin, Parallax, Depth Map, 3D Text, Pop Out. Real Canvas demo
 * thumbnails showing the geometric transform. All PRO (premium 3D pipeline).
 */
@Composable
fun Effects3DScreen(onClose: () -> Unit, vm: EditorViewModel) {
    var selectedId by remember { mutableStateOf<String?>(null) }
    Column(modifier = Modifier.fillMaxSize().background(Bg).padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("3D Effects", color = TextPrimary, fontSize = 24.sp,
                 fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            GradientPill(text = "Close", onClick = onClose, horizontalPadding = 18.dp)
        }
        Spacer(Modifier.height(8.dp))
        Text("Premium 3D transforms · all PRO", color = TextSecondary, fontSize = 12.sp)
        Spacer(Modifier.height(12.dp))
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(bottom = 80.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(EFFECTS_3D, key = { it.id }) { e ->
                Box(
                    modifier = Modifier.fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp)).background(BgCard.copy(alpha = 0.6f))
                        .pointerInput(e.id) { detectTapGestures(onTap = {
                            selectedId = e.id
                            vm.addDagNode(DAGNode.Kind.Effect3D, """{"effect3d":"${e.id}"}""")
                        }) }
                ) {
                    Column {
                        Box {
                            DemoThumbnail(renderDemo = e.render, selected = selectedId == e.id,
                                          modifier = Modifier.fillMaxWidth())
                            ProBadge(modifier = Modifier.align(Alignment.TopEnd).padding(6.dp))
                        }
                        Text(e.name, color = TextPrimary, fontSize = 13.sp,
                             fontWeight = FontWeight.Medium,
                             modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp))
                    }
                }
            }
        }
    }
}

private data class E3d(val id: String, val name: String, val render: DrawScope.() -> Unit)
private val EFFECTS_3D: List<E3d> = listOf(
    E3d("rotate","3D Rotate"){ baseScene(); skewOverlay(0.18f,0f) },
    E3d("flip","Flip"){ baseScene(); skewOverlay(-0.18f,0f) },
    E3d("perspective","Perspective"){ baseScene(); skewOverlay(0.1f,0.1f) },
    E3d("cube_spin","Cube Spin"){ baseScene(); skewOverlay(0f,0.18f) },
    E3d("parallax","Parallax"){ baseScene(); drawRect(Color(0x339D4EDD),size=size) },
    E3d("depth_map","Depth Map"){ baseScene(); drawRect(Color(0x44808080),size=size) },
    E3d("3d_text","3D Text"){ baseScene(); drawRect(Color(0x44FF5A3C),size=size) },
    E3d("pop_out","Pop Out"){ baseScene(); drawRect(Color(0x55FFFFFF),size=size) }
)

// Visual hint for a skew/perspective transform on the demo thumbnail.
private fun DrawScope.skewOverlay(sx: Float, sy: Float) {
    val w = size.width; val h = size.height
    drawRect(
        color = Color(0xFF9D4EDD).copy(alpha = 0.18f),
        topLeft = Offset(w * sx, h * sy),
        size = androidx.compose.ui.geometry.Size(w * (1f - kotlin.math.abs(sx)), h * (1f - kotlin.math.abs(sy)))
    )
}
