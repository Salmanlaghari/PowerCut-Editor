package com.powercut.editor.ui.export

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.powercut.editor.R
import com.powercut.editor.core.base.Resource
import com.powercut.editor.core.utils.LanguageHelper
import com.powercut.editor.ui.theme.CyberCyan
import com.powercut.editor.ui.theme.NeonOrange
import com.powercut.editor.ui.theme.glassmorphic
import com.powercut.editor.ui.theme.neonGlow
import com.powercut.editor.ui.theme.tactileClick

@Composable
fun ExportScreen(
    exportState: Resource<String>,
    language: String,
    onDone: () -> Unit,
    onBackToEditor: () -> Unit,
    onImportNewVideo: (android.net.Uri) -> Unit,
    isWatermarkRemoved: Boolean,
    onRemoveWatermarkRequested: () -> Unit,
    onStartExport: (resolution: String, fps: Int, isNoWatermark: Boolean, isHardwareAcc: Boolean) -> Unit
) {
    val context = LocalContext.current
    val importPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { onImportNewVideo(it) }
    }

    var selectedResIndex by remember { mutableStateOf(2) } // default 1080p (FHD)
    var selectedFpsIndex by remember { mutableStateOf(1) } // default 30 fps

    var isHardwareAccEnabled by remember { mutableStateOf(true) }

    val resolutionsList = listOf("480p", "720p", "1080p", "4k")
    val fpsList = listOf(24, 30, 60, 120)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F0F14))
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            when (exportState) {
                is Resource.Idle -> {
                    // 1. HEADER BAR
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onBackToEditor) {
                            Icon(Icons.Default.ChevronLeft, "Back", tint = Color.White, modifier = Modifier.size(20.dp))
                        }
                        Column(modifier = Modifier.padding(start = 8.dp)) {
                            Text("Export Video", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            Text("Review settings and save to gallery", fontSize = 11.sp, color = Color.Gray)
                        }
                    }

                    // 2. VIDEO PREVIEW THUMBNAIL
                    Box(
                        modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f).clip(RoundedCornerShape(16.dp))
                            .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(16.dp))
                            .background(Brush.linearGradient(listOf(Color(0xFF0D47A1), Color(0xFF1565C0)))),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier.size(48.dp).background(Color.White.copy(alpha = 0.15f), CircleShape)
                                .border(1.dp, Color.White.copy(alpha = 0.25f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.PlayArrow, "Play", tint = Color.White, modifier = Modifier.size(20.dp))
                        }
                        Box(modifier = Modifier.align(Alignment.BottomStart).padding(12.dp)) {
                            Column {
                                Text("My Project", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                Text("Duration: 00:00:30", fontSize = 9.sp, color = Color.LightGray)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // 3. RESOLUTION SELECTION
                    Text("SELECT RESOLUTION", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Gray, modifier = Modifier.align(Alignment.Start))
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("480p (SD)", "720p (HD)").forEachIndexed { index, title ->
                            val isSel = selectedResIndex == index
                            Box(
                                modifier = Modifier.weight(1f).height(44.dp)
                                    .neonGlow(if (isSel) NeonOrange else Color.Transparent, RoundedCornerShape(10.dp), 1.dp)
                                    .glassmorphic(RoundedCornerShape(10.dp))
                                    .clickable { selectedResIndex = index }.padding(8.dp),
                                contentAlignment = Alignment.Center
                            ) { Text(title, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (isSel) NeonOrange else Color.White) }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("1080p (FHD)", "4K (UHD)").forEachIndexed { index, title ->
                            val actualIndex = index + 2
                            val isSel = selectedResIndex == actualIndex
                            Box(
                                modifier = Modifier.weight(1f).height(44.dp)
                                    .neonGlow(if (isSel) NeonOrange else Color.Transparent, RoundedCornerShape(10.dp), 1.dp)
                                    .glassmorphic(RoundedCornerShape(10.dp))
                                    .clickable { selectedResIndex = actualIndex }.padding(8.dp),
                                contentAlignment = Alignment.Center
                            ) { Text(title, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (isSel) NeonOrange else Color.White) }
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    // 4. FRAME RATE SELECTION
                    Text("SELECT FRAME RATE", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Gray, modifier = Modifier.align(Alignment.Start))
                    Spacer(Modifier.height(6.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(24, 30, 60, 120).forEachIndexed { index, fps ->
                            val isSel = selectedFpsIndex == index
                            Box(
                                modifier = Modifier.weight(1f).height(40.dp)
                                    .neonGlow(if (isSel) CyberCyan else Color.Transparent, RoundedCornerShape(8.dp), 1.dp)
                                    .glassmorphic(RoundedCornerShape(8.dp))
                                    .clickable { selectedFpsIndex = index }.padding(6.dp),
                                contentAlignment = Alignment.Center
                            ) { Text("${fps} FPS", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = if (isSel) CyberCyan else Color.White) }
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    // 5. INFO CARDS
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(Modifier.weight(1f).glassmorphic(RoundedCornerShape(10.dp)).padding(10.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Info, "Format", tint = CyberCyan, modifier = Modifier.size(14.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Format: MP4", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                        Box(Modifier.weight(1f).glassmorphic(RoundedCornerShape(10.dp)).padding(10.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Download, "Size", tint = NeonOrange, modifier = Modifier.size(14.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Est. Size: ~45 MB", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    // 6. TOGGLE OPTIONS
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        Row(
                            Modifier.fillMaxWidth().glassmorphic(RoundedCornerShape(12.dp)).padding(horizontal = 16.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("No Watermark", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                Text(if (isWatermarkRemoved) "Unlocked via Ad!" else "Watch ad to unlock watermark-free export", fontSize = 8.sp, color = if (isWatermarkRemoved) CyberCyan else Color.LightGray)
                            }
                            if (isWatermarkRemoved) {
                                Icon(Icons.Default.CheckCircle, "Unlocked", tint = CyberCyan, modifier = Modifier.size(24.dp))
                            } else {
                                Box(Modifier.background(NeonOrange.copy(0.15f), RoundedCornerShape(8.dp)).clickable { onRemoveWatermarkRequested() }.padding(horizontal = 10.dp, vertical = 6.dp)) {
                                    Text("REMOVE AD", fontSize = 9.sp, fontWeight = FontWeight.Black, color = NeonOrange)
                                }
                            }
                        }
                        Row(
                            Modifier.fillMaxWidth().glassmorphic(RoundedCornerShape(12.dp)).padding(horizontal = 16.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Hardware Acceleration", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            Switch(checked = isHardwareAccEnabled, onCheckedChange = { isHardwareAccEnabled = it }, colors = SwitchDefaults.colors(checkedThumbColor = CyberCyan))
                        }
                    }

                    Spacer(Modifier.height(20.dp))

                    // 7. IMPORT + EXPORT BUTTONS
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        // IMPORT — opens file picker
                        Box(
                            modifier = Modifier.weight(1f).height(54.dp)
                                .glassmorphic(RoundedCornerShape(16.dp))
                                .border(1.5.dp, CyberCyan, RoundedCornerShape(16.dp))
                                .tactileClick { importPicker.launch("video/*") },
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                                Text("➕", fontSize = 16.sp)
                                Spacer(Modifier.width(6.dp))
                                Text("IMPORT", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                        // EXPORT
                        Box(
                            modifier = Modifier.weight(1f).height(54.dp)
                                .neonGlow(NeonOrange, RoundedCornerShape(16.dp))
                                .background(Brush.verticalGradient(listOf(NeonOrange, Color(0xFFE64A19))), RoundedCornerShape(16.dp))
                                .border(1.dp, Color.White.copy(0.15f), RoundedCornerShape(16.dp))
                                .tactileClick { onStartExport(resolutionsList[selectedResIndex], fpsList[selectedFpsIndex], isWatermarkRemoved, isHardwareAccEnabled) },
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                                Icon(Icons.Default.Download, "Export", tint = Color.White, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("EXPORT", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White, letterSpacing = 0.5.sp)
                            }
                        }
                    }
                }

                is Resource.Loading -> {
                    Row(Modifier.fillMaxWidth().padding(bottom = 32.dp), verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onBackToEditor) {
                            Icon(Icons.Default.ChevronLeft, "Back", tint = Color.White, modifier = Modifier.size(20.dp))
                        }
                        Column(modifier = Modifier.padding(start = 8.dp)) {
                            Text("Export Video", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            Text("Processing high-speed output pipeline...", fontSize = 11.sp, color = Color.Gray)
                        }
                    }
                    CircularProgressIndicator(modifier = Modifier.size(72.dp), color = NeonOrange, strokeWidth = 6.dp)
                    Spacer(Modifier.height(32.dp))
                    Text(LanguageHelper.getString(R.string.video_exporting, language), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White, textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 24.dp))
                }

                is Resource.Success -> {
                    Icon(Icons.Default.CheckCircle, "Success", tint = CyberCyan, modifier = Modifier.size(72.dp))
                    Spacer(Modifier.height(16.dp))
                    Text(LanguageHelper.getString(R.string.export_success, language), fontSize = 20.sp, fontWeight = FontWeight.Black, color = Color.White)
                    Spacer(Modifier.height(8.dp))
                    Text(exportState.data, fontSize = 12.sp, color = CyberCyan, textAlign = TextAlign.Center, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 24.dp))
                    Spacer(Modifier.height(36.dp))

                    // IMPORT + DONE after success
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Box(
                            modifier = Modifier.weight(1f).height(50.dp)
                                .glassmorphic(RoundedCornerShape(14.dp))
                                .border(1.5.dp, CyberCyan, RoundedCornerShape(14.dp))
                                .tactileClick { importPicker.launch("video/*") },
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                                Text("➕", fontSize = 16.sp)
                                Spacer(Modifier.width(6.dp))
                                Text("IMPORT NEW", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                        Box(
                            modifier = Modifier.weight(1f).height(50.dp)
                                .background(NeonOrange, RoundedCornerShape(14.dp))
                                .tactileClick(onClick = onDone),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(LanguageHelper.getString(R.string.done, language).uppercase(), fontSize = 13.sp, fontWeight = FontWeight.Black, color = Color.White, letterSpacing = 0.5.sp)
                        }
                    }
                }

                is Resource.Error -> {
                    Icon(Icons.Default.Error, "Error", tint = NeonOrange, modifier = Modifier.size(72.dp))
                    Spacer(Modifier.height(16.dp))
                    Text(LanguageHelper.getString(R.string.export_failed, language), fontSize = 20.sp, fontWeight = FontWeight.Black, color = Color.White)
                    Spacer(Modifier.height(8.dp))
                    Text(exportState.message, fontSize = 12.sp, color = Color.LightGray, textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 24.dp))
                    Spacer(Modifier.height(36.dp))

                    // IMPORT + RETRY after error
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Box(
                            modifier = Modifier.weight(1f).height(50.dp)
                                .glassmorphic(RoundedCornerShape(14.dp))
                                .border(1.5.dp, CyberCyan, RoundedCornerShape(14.dp))
                                .tactileClick { importPicker.launch("video/*") },
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                                Text("➕", fontSize = 16.sp)
                                Spacer(Modifier.width(6.dp))
                                Text("IMPORT NEW", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                        Box(
                            modifier = Modifier.weight(1f).height(50.dp)
                                .background(Color.DarkGray, RoundedCornerShape(14.dp))
                                .tactileClick(onClick = onBackToEditor),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("BACK TO EDIT", fontSize = 13.sp, fontWeight = FontWeight.Black, color = Color.White, letterSpacing = 0.5.sp)
                        }
                    }
                }
            }
        }
    }
}
