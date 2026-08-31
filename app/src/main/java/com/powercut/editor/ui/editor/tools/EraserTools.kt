package com.powercut.editor.ui.editor.tools

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
 * Eraser Tools Panel
 * Phase C: reduced to what is REALLY implemented — background removal via
 * chroma-key of the estimated background color, baked into the export with
 * the tolerance slider as key similarity. The previous fake "object"/"area"
 * paint-to-remove modes (no content-aware backend) were removed per audit.
 */

@Composable
fun EraserToolsPanel(
    eraserMode: String,
    eraserBrushSize: Float,
    eraserTolerance: Float,
    eraserSoftEdge: Boolean,
    onUpdateEraserMode: (String) -> Unit,
    onUpdateBrushSize: (Float) -> Unit,
    onUpdateTolerance: (Float) -> Unit,
    onToggleSoftEdge: () -> Unit,
    onUndoEraser: () -> Unit = {},
    onResetEraser: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Eraser Mode Selection
        Text("ERASER MODE", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            listOf(
                Triple("none", "🚫", "Off"),
                Triple("background", "🖼️", "BG Eraser")
            ).forEach { (mode, emoji, label) ->
                val isSel = eraserMode == mode
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isSel) NeonOrange.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.04f))
                        .border(1.dp, if (isSel) NeonOrange else Color.White.copy(alpha = 0.08f), RoundedCornerShape(10.dp))
                        .clickable { onUpdateEraserMode(mode) },
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(emoji, fontSize = 14.sp)
                        Text(label, fontSize = 7.sp, fontWeight = FontWeight.Bold, color = if (isSel) NeonOrange else Color.White)
                    }
                }
            }
        }

        // Tolerance
        Text("COLOR TOLERANCE: ${(eraserTolerance * 100).toInt()}%", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
        Slider(
            value = eraserTolerance,
            onValueChange = onUpdateTolerance,
            valueRange = 0.1f..0.9f,
            colors = SliderDefaults.colors(activeTrackColor = NeonOrange, thumbColor = NeonOrange),
            modifier = Modifier.height(24.dp)
        )

        // Soft Edge Toggle
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(Color.White.copy(alpha = 0.03f))
                .padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("SOFT EDGE BLEND", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Switch(
                checked = eraserSoftEdge,
                onCheckedChange = { onToggleSoftEdge() },
                colors = SwitchDefaults.colors(checkedThumbColor = CyberCyan),
                modifier = Modifier.height(20.dp)
            )
        }

        // Action Button (Reset only — erasing is baked into the export)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(32.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.White.copy(alpha = 0.04f))
                    .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                    .clickable { onResetEraser() },
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(Icons.Default.Refresh, contentDescription = "Reset", tint = NeonOrange, modifier = Modifier.size(12.dp))
                    Text("RESET", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = NeonOrange)
                }
            }
        }

        // Mode description
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(Color.White.copy(alpha = 0.02f))
                .padding(8.dp)
        ) {
            val desc = when (eraserMode) {
                "background" -> "🎯 Removes the background by keying out its dominant color. The tolerance slider controls how much is erased. Applied to the exported video."
                else -> "Select BG Eraser and adjust the tolerance — the removal is applied when you export."
            }
            Text(desc, fontSize = 9.sp, color = Color.Gray, lineHeight = 13.sp)
        }
    }
}
