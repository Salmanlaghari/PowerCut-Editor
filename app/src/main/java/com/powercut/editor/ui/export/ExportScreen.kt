package com.powercut.editor.ui.export

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronLeft
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
import androidx.compose.runtime.mutableIntStateOf
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
    onStartExport: (resolution: String, fps: Int, isNoWatermark: Boolean, isHardwareAcc: Boolean, isHdr: Boolean, isHighBitrate: Boolean) -> Unit,
    exportProgress: Int = 0
) {
    val importPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { onImportNewVideo(it) }
    }

    var selectedResIndex by remember { mutableIntStateOf(2) }
    var selectedFpsIndex by remember { mutableIntStateOf(1) }
    var isHardwareAccEnabled by remember { mutableStateOf(true) }
    var isHdrEnabled by remember { mutableStateOf(false) }
    var isHighBitrateEnabled by remember { mutableStateOf(false) }

    // v6.0.0: Added 2K (QHD) to the resolution list.
    val resolutionsList = listOf("480p", "720p", "1080p", "2k", "4k")
    val fpsList = listOf(24, 30, 60, 120)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F0F14))
    ) {
        when (exportState) {
            is Resource.Idle -> {
                // ─── HEADER (fixed) ───
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
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

                // ─── SCROLLABLE CONTENT ───
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // VIDEO PREVIEW
                    Box(
                        modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f).clip(RoundedCornerShape(16.dp))
                            .border(1.dp, Color.White.copy(0.05f), RoundedCornerShape(16.dp))
                            .background(Brush.linearGradient(listOf(Color(0xFF0D47A1), Color(0xFF1565C0)))),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier.size(48.dp).background(Color.White.copy(0.15f), CircleShape)
                                .border(1.dp, Color.White.copy(0.25f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) { Icon(Icons.Default.PlayArrow, "Play", tint = Color.White, modifier = Modifier.size(20.dp)) }
                        Box(modifier = Modifier.align(Alignment.BottomStart).padding(12.dp)) {
                            Column {
                                Text("My Project", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                Text("Duration: 00:00:30", fontSize = 9.sp, color = Color.LightGray)
                            }
                        }
                    }

                    // RESOLUTION — v6.0.0: 5 options including 2K (QHD)
                    Text("SELECT RESOLUTION", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("480p (SD)" to 0, "720p (HD)" to 1, "1080p (FHD)" to 2).forEachIndexed { idx, (title, resIdx) ->
                            val isSel = selectedResIndex == resIdx
                            Box(
                                modifier = Modifier.weight(1f).height(44.dp)
                                    .neonGlow(if (isSel) NeonOrange else Color.Transparent, RoundedCornerShape(10.dp), 1.dp)
                                    .glassmorphic(RoundedCornerShape(10.dp))
                                    .clickable { selectedResIndex = resIdx }.padding(6.dp),
                                contentAlignment = Alignment.Center
                            ) { Text(title, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = if (isSel) NeonOrange else Color.White, textAlign = TextAlign.Center) }
                        }
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("2K (QHD)" to 3, "4K (UHD)" to 4).forEach { (title, resIdx) ->
                            val isSel = selectedResIndex == resIdx
                            Box(
                                modifier = Modifier.weight(1f).height(44.dp)
                                    .neonGlow(if (isSel) NeonOrange else Color.Transparent, RoundedCornerShape(10.dp), 1.dp)
                                    .glassmorphic(RoundedCornerShape(10.dp))
                                    .clickable { selectedResIndex = resIdx }.padding(8.dp),
                                contentAlignment = Alignment.Center
                            ) { Text(title, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (isSel) NeonOrange else Color.White) }
                        }
                    }

                    // FRAME RATE
                    Text("SELECT FRAME RATE", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
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

                    // INFO CARDS
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

                    // TOGGLE: No Watermark
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

                    // TOGGLE: Hardware Acceleration
                    Row(
                        Modifier.fillMaxWidth().glassmorphic(RoundedCornerShape(12.dp)).padding(horizontal = 16.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Hardware Acceleration", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            Text("Faster encoding when supported", fontSize = 8.sp, color = Color.LightGray)
                        }
                        Switch(checked = isHardwareAccEnabled, onCheckedChange = { isHardwareAccEnabled = it }, colors = SwitchDefaults.colors(checkedThumbColor = CyberCyan))
                    }

                    // v6.0.0 TOGGLE: HDR Export (10-bit BT.2020 PQ)
                    Row(
                        Modifier.fillMaxWidth().glassmorphic(RoundedCornerShape(12.dp)).padding(horizontal = 16.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("🌈 HDR Export", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            Text("10-bit BT.2020 PQ (HEVC) — richer colors & dynamic range", fontSize = 8.sp, color = Color.LightGray)
                        }
                        Switch(checked = isHdrEnabled, onCheckedChange = { isHdrEnabled = it }, colors = SwitchDefaults.colors(checkedThumbColor = NeonOrange))
                    }

                    // v6.0.0 TOGGLE: High Bitrate Export (visually-lossless)
                    Row(
                        Modifier.fillMaxWidth().glassmorphic(RoundedCornerShape(12.dp)).padding(horizontal = 16.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("💎 High Bitrate", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            Text("Near-lossless CRF 18 — maximum detail for mastering", fontSize = 8.sp, color = Color.LightGray)
                        }
                        Switch(checked = isHighBitrateEnabled, onCheckedChange = { isHighBitrateEnabled = it }, colors = SwitchDefaults.colors(checkedThumbColor = CyberCyan))
                    }

                    Spacer(Modifier.height(4.dp))
                } // end scrollable

                // ─── STICKY IMPORT + EXPORT BUTTONS (always visible) ───
                Box(
                    modifier = Modifier.fillMaxWidth()
                        .background(Color(0xFF0F0F14))
                        .border(1.dp, Color.White.copy(0.05f))
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
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
                                .tactileClick { onStartExport(resolutionsList[selectedResIndex], fpsList[selectedFpsIndex], isWatermarkRemoved, isHardwareAccEnabled, isHdrEnabled, isHighBitrateEnabled) },
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
            }

            is Resource.Loading -> {
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBackToEditor) {
                        Icon(Icons.Default.ChevronLeft, "Back", tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                    Column(modifier = Modifier.padding(start = 8.dp)) {
                        Text("Export Video", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Text("Processing high-speed output pipeline...", fontSize = 11.sp, color = Color.Gray)
                    }
                }
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(horizontal = 24.dp)) {
                        // v5.2.0: Live export progress with percentage (10%, 20%, 100%)
                        // User request: "Live 10% 20% 100% dekhao video process export"
                        val progressVal = exportProgress.coerceIn(0, 100)
                        val progressFraction = progressVal / 100f

                        // Stage label based on progress
                        val stageLabel = when {
                            progressVal < 10 -> "Initializing export..."
                            progressVal < 30 -> "Decoding video frames..."
                            progressVal < 50 -> "Applying filters & effects..."
                            progressVal < 70 -> "Encoding video stream..."
                            progressVal < 90 -> "Mixing audio & finalizing..."
                            progressVal < 100 -> "Writing output file..."
                            else -> "Export complete!"
                        }

                        // Animated circular progress with percentage
                        Box(contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(
                                progress = { progressFraction },
                                modifier = Modifier.size(96.dp),
                                color = NeonOrange,
                                strokeWidth = 8.dp,
                                trackColor = Color.White.copy(0.08f)
                            )
                            Text(
                                text = "$progressVal%",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White
                            )
                        }

                        Spacer(Modifier.height(24.dp))

                        // Linear progress bar
                        Box(
                            modifier = Modifier.fillMaxWidth().height(8.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color.White.copy(0.08f))
                        ) {
                            Box(
                                modifier = Modifier.fillMaxWidth(progressFraction).height(8.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Brush.horizontalGradient(listOf(NeonOrange, CyberCyan)))
                            )
                        }

                        Spacer(Modifier.height(16.dp))

                        // Stage label
                        Text(
                            text = stageLabel,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = CyberCyan,
                            textAlign = TextAlign.Center
                        )

                        Spacer(Modifier.height(8.dp))

                        Text(
                            text = LanguageHelper.getString(R.string.video_exporting, language),
                            fontSize = 11.sp,
                            color = Color.Gray,
                            textAlign = TextAlign.Center
                        )

                        // Progress milestones indicator
                        Spacer(Modifier.height(20.dp))
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            listOf(0, 25, 50, 75, 100).forEach { milestone ->
                                val reached = progressVal >= milestone
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Box(
                                        modifier = Modifier.size(if (reached) 10.dp else 8.dp)
                                            .clip(CircleShape)
                                            .background(if (reached) CyberCyan else Color.White.copy(0.15f))
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        text = "$milestone%",
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (reached) CyberCyan else Color.Gray
                                    )
                                }
                            }
                        }
                    }
                }
            }

            is Resource.Success -> {
                Spacer(Modifier.height(40.dp))
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(horizontal = 24.dp)) {
                        Icon(Icons.Default.CheckCircle, "Success", tint = CyberCyan, modifier = Modifier.size(72.dp))
                        Spacer(Modifier.height(16.dp))
                        Text(LanguageHelper.getString(R.string.export_success, language), fontSize = 20.sp, fontWeight = FontWeight.Black, color = Color.White)
                        Spacer(Modifier.height(8.dp))
                        Text(exportState.data, fontSize = 12.sp, color = CyberCyan, textAlign = TextAlign.Center, fontWeight = FontWeight.Bold)
                    }
                }

                // STICKY IMPORT + DONE
                Box(
                    modifier = Modifier.fillMaxWidth()
                        .background(Color(0xFF0F0F14))
                        .border(1.dp, Color.White.copy(0.05f))
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
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
            }

            is Resource.Error -> {
                Spacer(Modifier.height(40.dp))
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(horizontal = 24.dp)) {
                        Icon(Icons.Default.Error, "Error", tint = NeonOrange, modifier = Modifier.size(72.dp))
                        Spacer(Modifier.height(16.dp))
                        Text(LanguageHelper.getString(R.string.export_failed, language), fontSize = 20.sp, fontWeight = FontWeight.Black, color = Color.White)
                        Spacer(Modifier.height(8.dp))
                        Text((exportState as Resource.Error).message ?: "Video processing failed. Try lowering resolution or free up storage space.", fontSize = 12.sp, color = Color.LightGray, textAlign = TextAlign.Center)
                    }
                }

                // STICKY IMPORT + RETRY
                Box(
                    modifier = Modifier.fillMaxWidth()
                        .background(Color(0xFF0F0F14))
                        .border(1.dp, Color.White.copy(0.05f))
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
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
