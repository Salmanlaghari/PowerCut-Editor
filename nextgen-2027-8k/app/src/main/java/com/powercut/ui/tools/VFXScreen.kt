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
import com.powercut.ui.components.GradientPill
import com.powercut.ui.components.ProBadge
import com.powercut.ui.components.baseScene
import com.powercut.ui.editor.EditorViewModel
import com.powercut.ui.theme.*

/**
 * VFX Screen (NEW, P4) — time/speed effects: Slow/Fast Motion, Reverse,
 * Freeze Frame, Time Remap, Speed Ramp. Each has a real demo thumbnail
 * (motion-direction hint) and applies a VFX DAG node.
 */
@Composable
fun VFXScreen(onClose: () -> Unit, vm: EditorViewModel) {
    var selectedId by remember { mutableStateOf<String?>(null) }
    Column(modifier = Modifier.fillMaxSize().background(Bg).padding(16.dp)
        .verticalScroll(rememberScrollState())) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("VFX", color = TextPrimary, fontSize = 24.sp,
                 fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            GradientPill(text = "Close", onClick = onClose, horizontalPadding = 18.dp)
        }
        Spacer(Modifier.height(8.dp))
        Text("Time & speed effects · all PRO", color = TextSecondary, fontSize = 12.sp)
        Spacer(Modifier.height(12.dp))
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            VFX_DEFS.forEach { v ->
                Box(
                    modifier = Modifier.fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp)).background(BgCard.copy(alpha = 0.6f))
                        .pointerInput(v.id) { detectTapGestures(onTap = {
                            selectedId = v.id
                            vm.addDagNode(DAGNode.Kind.VFX, """{"vfx":"${v.id}"}""")
                        }) }
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(10.dp)) {
                        DemoThumbnail(renderDemo = {
                            baseScene()
                            // motion hint overlay
                            drawRect(
                                color = when(v.id){
                                    "slow_motion","freeze" -> Color(0x33000000)
                                    "fast_motion","speed_ramp" -> Color(0x33FF5A3C)
                                    "reverse" -> Color(0x339D4EDD)
                                    "time_remap" -> Color(0x33FFFFFF)
                                    else -> Color.Transparent
                                },
                                topLeft = androidx.compose.ui.geometry.Offset.Zero,
                                size = size
                            )
                        }, selected = selectedId == v.id, modifier = Modifier.width(120.dp))
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(v.name, color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                            Text(v.desc, color = TextSecondary, fontSize = 12.sp)
                        }
                        ProBadge()
                    }
                }
            }
        }
        Spacer(Modifier.height(80.dp))
    }
}

private data class VfxTime(val id: String, val name: String, val desc: String)
private val VFX_DEFS = listOf(
    VfxTime("slow_motion","Slow Motion","0.25x cinematic slow-mo"),
    VfxTime("fast_motion","Fast Motion","4x speed ramp"),
    VfxTime("reverse","Reverse","Play clip backward"),
    VfxTime("freeze","Freeze Frame","Hold a frame mid-clip"),
    VfxTime("time_remap","Time Remap","Curve-based speed control"),
    VfxTime("speed_ramp","Speed Ramp","Ease in/out variable speed")
)

// NOTE: drawRect is the built-in androidx.compose.ui.graphics.drawscope.DrawScope.drawRect,
// invoked with explicit (color, topLeft, size) — no custom extension needed.
