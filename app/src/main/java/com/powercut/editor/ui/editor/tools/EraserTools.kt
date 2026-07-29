package com.powercut.editor.ui.editor.tools

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
 * Eraser Tools Panel
 * Features: Background Eraser, Object Eraser, Area Eraser with brush size & tolerance controls
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
    onUndoEraser: () -> Unit,
    onResetEraser: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
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
                Triple("background", "🖼️", "BG Eraser"),
                Triple("object", "🎯", "Object"),
                Triple("area", "✂️", "Area")
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

        // Brush Size
        Text("BRUSH SIZE: ${eraserBrushSize.toInt()}px", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Slider(
                value = eraserBrushSize,
                onValueChange = onUpdateBrushSize,
                valueRange = 5f..100f,
                colors = SliderDefaults.colors(activeTrackColor = CyberCyan, thumbColor = CyberCyan),
                modifier = Modifier.weight(1f).height(24.dp)
            )
            // Brush preview circle
            Box(
                modifier = Modifier
                    .size((eraserBrushSize / 3).dp.coerceIn(12.dp, 40.dp))
                    .background(CyberCyan.copy(alpha = 0.3f), CircleShape)
                    .border(1.dp, CyberCyan, CircleShape)
            )
        }

        // Quick brush sizes
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            listOf(10f, 20f, 30f, 50f, 80f).forEach { size ->
                val isSel = eraserBrushSize == size
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(24.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isSel) CyberCyan.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.04f))
                        .clickable { onUpdateBrushSize(size) },
                    contentAlignment = Alignment.Center
                ) {
                    Text("${size.toInt()}", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = if (isSel) CyberCyan else Color.White)
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

        // Action Buttons
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
                    .clickable { onUndoEraser() },
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(Icons.Default.Undo, contentDescription = "Undo", tint = Color.White, modifier = Modifier.size(12.dp))
                    Text("UNDO", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
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
                "background" -> "🎯 Automatically detect and erase the video background. Tap areas to remove."
                "object" -> "🖌️ Paint over objects to remove them from the video frame."
                "area" -> "✂️ Select a rectangular area to erase or replace."
                else -> "Select an eraser mode to begin editing."
            }
            Text(desc, fontSize = 9.sp, color = Color.Gray, lineHeight = 13.sp)
        }
    }
}
