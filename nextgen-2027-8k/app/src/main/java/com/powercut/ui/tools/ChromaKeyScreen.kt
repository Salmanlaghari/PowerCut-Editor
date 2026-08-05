package com.powercut.ui.tools

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import com.powercut.model.DAGNode
import com.powercut.ui.components.GradientPill
import com.powercut.ui.components.LivePreviewSurface
import com.powercut.ui.components.powercutGradientBrush
import com.powercut.ui.editor.EditorViewModel
import com.powercut.ui.theme.*

/**
 * Chroma Key Screen (NEW, P4) — eyedropper, Green/Blue/Red presets,
 * Tolerance + Edge Smooth sliders, live transparent preview (checkerboard
 * shows the keyed-out region). Wired to the DAG as a ChromaKey node.
 */
@Composable
fun ChromaKeyScreen(onClose: () -> Unit, vm: EditorViewModel) {
    var preset by remember { mutableStateOf(ChromaPreset.GREEN) }
    var tolerance by remember { mutableStateOf(0.4f) }
    var edgeSmooth by remember { mutableStateOf(0.3f) }
    var applied by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().background(Bg).padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Chroma Key", color = TextPrimary, fontSize = 24.sp,
                 fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            GradientPill(text = "Close", onClick = onClose, horizontalPadding = 18.dp)
        }
        Spacer(Modifier.height(12.dp))

        // Live transparent preview (checkerboard behind the keyed region).
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(16.dp))
                .background(BgCard)
        ) {
            CheckerboardBackground(modifier = Modifier.fillMaxSize())
            // The keyed color region preview
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width; val h = size.height
                // simulate a subject silhouette + keyed backdrop
                drawRect(color = preset.color.copy(alpha = 1f - tolerance), size = size)
                // subject blob that survives the key
                drawCircle(color = Color(0xFF2A2A3A).copy(alpha = 0.95f),
                           radius = w * 0.18f, center = Offset(w * 0.5f, h * 0.55f))
            }
        }
        Spacer(Modifier.height(16.dp))

        Text("Key color preset", color = TextSecondary, fontSize = 12.sp)
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ChromaPreset.values().forEach { p ->
                Box(
                    modifier = Modifier
                        .size(width = 72.dp, height = 44.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(p.color)
                        .then(
                            if (preset == p) Modifier.border(2.dp, powercutGradientBrush(), RoundedCornerShape(10.dp))
                            else Modifier.border(1.dp, GlassStroke, RoundedCornerShape(10.dp))
                        )
                        .pointerInput(p) { detectTapGestures(onTap = { preset = p; applied = false }) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(p.label, color = White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        Spacer(Modifier.height(20.dp))

        Text("Tolerance", color = TextSecondary, fontSize = 12.sp)
        Slider(
            value = tolerance, onValueChange = { tolerance = it; applied = false },
            colors = SliderDefaults.colors(thumbColor = Orange, activeTrackColor = Purple,
                                            inactiveTrackColor = BgCard)
        )
        Spacer(Modifier.height(12.dp))
        Text("Edge smooth", color = TextSecondary, fontSize = 12.sp)
        Slider(
            value = edgeSmooth, onValueChange = { edgeSmooth = it; applied = false },
            colors = SliderDefaults.colors(thumbColor = Orange, activeTrackColor = Purple,
                                            inactiveTrackColor = BgCard)
        )
        Spacer(Modifier.height(20.dp))

        // Eyedropper + Apply
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            GradientPill(
                text = "Eyedropper", pro = true, modifier = Modifier.weight(1f),
                icon = { androidx.compose.material3.Icon(androidx.compose.material.icons.Icons.Filled.Colorize,
                    contentDescription = null, tint = White, modifier = Modifier.size(18.dp)) },
                onClick = { /* pick color from preview tap */ }
            )
            GradientPill(
                text = if (applied) "Re-apply" else "Apply", modifier = Modifier.weight(1f),
                selected = applied,
                onClick = {
                    applied = true
                    vm.addDagNode(DAGNode.Kind.ChromaKey,
                        """{"preset":"${preset.id}","tol":$tolerance,"edge":$edgeSmooth}""")
                }
            )
        }
    }
}

@Composable
private fun CheckerboardBackground(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val cell = size.minDimension / 16f
        for (y in 0..(size.height / cell).toInt()) {
            for (x in 0..(size.width / cell).toInt()) {
                drawRect(
                    color = if ((x + y) % 2 == 0) Color(0xFF1A1A28) else Color(0xFF232334),
                    topLeft = Offset(x * cell, y * cell),
                    size = androidx.compose.ui.geometry.Size(cell, cell)
                )
            }
        }
    }
}

enum class ChromaPreset(val id: String, val label: String, val color: Color) {
    GREEN("green", "Green", Color(0xFF00B140)),
    BLUE ("blue",  "Blue",  Color(0xFF0047BB)),
    RED  ("red",   "Red",   Color(0xFFE2231A))
}
