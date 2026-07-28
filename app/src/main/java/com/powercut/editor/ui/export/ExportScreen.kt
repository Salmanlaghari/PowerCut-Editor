package com.powercut.editor.ui.export

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
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
    isWatermarkRemoved: Boolean,
    onRemoveWatermarkRequested: () -> Unit,
    onStartExport: (resolution: String, fps: Int, isNoWatermark: Boolean, isHardwareAcc: Boolean) -> Unit
) {
    var selectedResIndex by remember { mutableStateOf(2) } // default 1080p (FHD)
    var selectedFpsIndex by remember { mutableStateOf(1) } // default 30 fps

    var isHardwareAccEnabled by remember { mutableStateOf(true) }

    val resolutionsList = listOf("480p", "720p", "1080p", "4k")
    val fpsList = listOf(24, 30, 60, 120)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F0F14)) // Cinematic dark theme background
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
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onBackToEditor) {
                            Icon(imageVector = Icons.Default.ChevronLeft, contentDescription = "Back", tint = Color.White, modifier = Modifier.size(20.dp))
                        }
                        Column(modifier = Modifier.padding(start = 8.dp)) {
                            Text("Export Video", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            Text("Review settings and save to gallery", fontSize = 11.sp, color = Color.Gray)
                        }
                    }

                    // 2. VIDEO PREVIEW THUMBNAIL (16:9 with play overlay)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(16f / 9f)
                            .clip(RoundedCornerShape(16.dp))
                            .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(16.dp))
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(Color(0xFF0D47A1), Color(0xFF1565C0))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        // Play button glass overlay
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .background(Color.White.copy(alpha = 0.15f), CircleShape)
                                .border(1.dp, Color.White.copy(alpha = 0.25f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(imageVector = Icons.Default.PlayArrow, contentDescription = "Play", tint = Color.White, modifier = Modifier.size(20.dp))
                        }

                        // Project Name + Duration overlay
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(12.dp)
                        ) {
                            Column {
                                Text("My Project", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                Text("Duration: 00:00:30", fontSize = 9.sp, color = Color.LightGray)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // 3. RESOLUTION SELECTION GRID (2x2)
                    Text("SELECT RESOLUTION", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Gray, modifier = Modifier.align(Alignment.Start))
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("480p (SD)", "720p (HD)").forEachIndexed { index, title ->
                            val isSel = selectedResIndex == index
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                                    .neonGlow(color = if (isSel) NeonOrange else Color.Transparent, shape = RoundedCornerShape(10.dp), glowWidth = 1.dp)
                                    .glassmorphic(shape = RoundedCornerShape(10.dp))
                                    .clickable { selectedResIndex = index }
                                    .padding(8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(title, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (isSel) NeonOrange else Color.White)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("1080p (FHD)", "4K (UHD)").forEachIndexed { index, title ->
                            val actualIndex = index + 2
                            val isSel = selectedResIndex == actualIndex
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                                    .neonGlow(color = if (isSel) NeonOrange else Color.Transparent, shape = RoundedCornerShape(10.dp), glowWidth = 1.dp)
                                    .glassmorphic(shape = RoundedCornerShape(10.dp))
                                    .clickable { selectedResIndex = actualIndex }
                                    .padding(8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(title, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (isSel) NeonOrange else Color.White)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // 4. FRAME RATE SELECTION GRID
                    Text("SELECT FRAME RATE", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Gray, modifier = Modifier.align(Alignment.Start))
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(24, 30, 60, 120).forEachIndexed { index, fps ->
                            val isSel = selectedFpsIndex == index
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(40.dp)
                                    .neonGlow(color = if (isSel) CyberCyan else Color.Transparent, shape = RoundedCornerShape(8.dp), glowWidth = 1.dp)
                                    .glassmorphic(shape = RoundedCornerShape(8.dp))
                                    .clickable { selectedFpsIndex = index }
                                    .padding(6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("${fps} FPS", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = if (isSel) CyberCyan else Color.White)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // 5. INFO CARDS (Format & Size)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .glassmorphic(shape = RoundedCornerShape(10.dp))
                                .padding(10.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(imageVector = Icons.Default.Info, contentDescription = "Format", tint = CyberCyan, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Format: MP4", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .glassmorphic(shape = RoundedCornerShape(10.dp))
                                .padding(10.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(imageVector = Icons.Default.Download, contentDescription = "Size", tint = NeonOrange, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Est. Size: ~45 MB", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // 6. TOGGLE OPTIONS (No Watermark, Hardware Accel)
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .glassmorphic(shape = RoundedCornerShape(12.dp))
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("No Watermark", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                Text(
                                    text = if (isWatermarkRemoved) "Unlocked via Ad!" else "Watch ad to unlock watermark-free export",
                                    fontSize = 8.sp,
                                    color = if (isWatermarkRemoved) CyberCyan else Color.LightGray
                                )
                            }
                            if (isWatermarkRemoved) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Unlocked",
                                    tint = CyberCyan,
                                    modifier = Modifier.size(24.dp)
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .background(NeonOrange.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                                        .clickable { onRemoveWatermarkRequested() }
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Text("REMOVE AD", fontSize = 9.sp, fontWeight = FontWeight.Black, color = NeonOrange)
                                }
                            }
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .glassmorphic(shape = RoundedCornerShape(12.dp))
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Hardware Acceleration", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            Switch(
                                checked = isHardwareAccEnabled,
                                onCheckedChange = { isHardwareAccEnabled = it },
                                colors = SwitchDefaults.colors(checkedThumbColor = CyberCyan)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // 7. MAIN EXPORT AND SAVE BUTTON (Large orange gradient with download icon)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .neonGlow(color = NeonOrange, shape = RoundedCornerShape(16.dp))
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(NeonOrange, Color(0xFFE64A19))
                                ),
                                shape = RoundedCornerShape(16.dp)
                            )
                            .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(16.dp))
                            .tactileClick {
                                onStartExport(
                                    resolutionsList[selectedResIndex],
                                    fpsList[selectedFpsIndex],
                                    isWatermarkRemoved,
                                    isHardwareAccEnabled
                                )
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                            Icon(imageVector = Icons.Default.Download, contentDescription = "Export icon", tint = Color.White, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("EXPORT & SAVE", fontSize = 14.sp, fontWeight = FontWeight.Black, color = Color.White, letterSpacing = 1.sp)
                        }
                    }
                }

                is Resource.Loading -> {
                    // Header Bar during loading
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 32.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onBackToEditor) {
                            Icon(imageVector = Icons.Default.ChevronLeft, contentDescription = "Back", tint = Color.White, modifier = Modifier.size(20.dp))
                        }
                        Column(modifier = Modifier.padding(start = 8.dp)) {
                            Text("Export Video", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            Text("Processing high-speed output pipeline...", fontSize = 11.sp, color = Color.Gray)
                        }
                    }

                    CircularProgressIndicator(
                        modifier = Modifier.size(72.dp),
                        color = NeonOrange,
                        strokeWidth = 6.dp
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    Text(
                        text = LanguageHelper.getString(R.string.video_exporting, language),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )
                }

                is Resource.Success -> {
                    // Success Screen layout
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Success",
                        tint = CyberCyan,
                        modifier = Modifier.size(72.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = LanguageHelper.getString(R.string.export_success, language),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = exportState.data,
                        fontSize = 12.sp,
                        color = CyberCyan,
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )

                    Spacer(modifier = Modifier.height(36.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .background(NeonOrange, RoundedCornerShape(12.dp))
                            .tactileClick(onClick = onDone),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = LanguageHelper.getString(R.string.done, language).uppercase(),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White,
                            letterSpacing = 1.sp
                        )
                    }
                }

                is Resource.Error -> {
                    // Failure Screen layout
                    Icon(
                        imageVector = Icons.Default.Error,
                        contentDescription = "Error",
                        tint = NeonOrange,
                        modifier = Modifier.size(72.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = LanguageHelper.getString(R.string.export_failed, language),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = exportState.message,
                        fontSize = 12.sp,
                        color = Color.LightGray,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )

                    Spacer(modifier = Modifier.height(36.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .background(Color.DarkGray, RoundedCornerShape(12.dp))
                            .tactileClick(onClick = onBackToEditor),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "BACK TO EDITOR",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White,
                            letterSpacing = 1.sp
                        )
                    }
                }
            }
        }
    }
}
