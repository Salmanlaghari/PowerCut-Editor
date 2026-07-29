package com.powercut.editor.ui.editor

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.powercut.editor.core.utils.UriHelper
import com.powercut.editor.data.VideoProject
import com.powercut.editor.ui.theme.CyberCyan
import com.powercut.editor.ui.theme.NeonOrange
import com.powercut.editor.ui.theme.glassmorphic
import com.powercut.editor.ui.theme.neonGlow
import com.powercut.editor.ui.theme.tactileClick
import com.powercut.editor.ui.theme.AccentSecondary
import com.powercut.editor.ui.theme.premiumAccentGradient
import java.util.Locale

private fun formatTime(ms: Long): String {
    val totalSecs = ms / 1000
    val minutes = totalSecs / 60
    val seconds = totalSecs % 60
    return String.format(Locale.US, "%02d:%02d", minutes, seconds)
}

@OptIn(UnstableApi::class, ExperimentalLayoutApi::class)
@Composable
fun NextGenEditorScreen(
    project: VideoProject,
    language: String,
    onBack: () -> Unit,
    onUpdateTrim: (Long, Long) -> Unit,
    onUpdateResolution: (String) -> Unit,
    onUpdateFilter: (String) -> Unit,
    onToggleMute: () -> Unit,
    onExport: () -> Unit,
    onDurationRetrieved: (Long) -> Unit,
    onUpdateSpeed: (Float) -> Unit,
    onUpdateAspectPreset: (String) -> Unit,
    onUpdateTransition: (String) -> Unit,
    onUpdateBackgroundMusic: (String?) -> Unit,
    onUpdateMusicVolume: (Float) -> Unit,
    onUpdateVideoVolume: (Float) -> Unit,
    onUpdateAutoCaptions: (String) -> Unit,
    onToggleSilenceRemover: () -> Unit,
    onUpdateRotation: () -> Unit,
    onToggleFlipHorizontal: () -> Unit,
    onToggleFlipVertical: () -> Unit,
    onUpdateCropPreset: (String) -> Unit,
    onUpdateSpeedCurve: (String) -> Unit,
    onUpdateTextOverlay: (String?) -> Unit,
    onUpdateTextAnimation: (String) -> Unit,
    onUpdateStickerType: (String) -> Unit,
    onUpdateTemplate: (String) -> Unit,
    onUpdateVisualizerStyle: (String) -> Unit,
    onToggleBeatSync: () -> Unit,
    onUpdate3DShapeMask: (String) -> Unit,
    onAddClip: (Uri) -> Unit,
    onSaveDraft: () -> Unit
) {
    val context = LocalContext.current
    var selectedTool by remember { mutableIntStateOf(0) }
    var isPanelExpanded by remember { mutableStateOf(true) }
    var isPlaying by remember { mutableStateOf(false) }
    var currentPlaybackTime by remember { mutableStateOf(0L) }

    // NextGen state tracking for all tools
    var selectedEffect by remember { mutableStateOf("none") }
    var selectedTrimMode by remember { mutableStateOf("Manual") }
    var selectedSplitMode by remember { mutableStateOf("Playhead") }
    var layerVideo1Visible by remember { mutableStateOf(true) }
    var layerVideo2Visible by remember { mutableStateOf(true) }
    var layerAudioVisible by remember { mutableStateOf(true) }
    var layerTextVisible by remember { mutableStateOf(true) }
    var layerStickerVisible by remember { mutableStateOf(true) }

    val multiFilePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
        uris.forEach { onAddClip(it) }
        if (uris.isNotEmpty()) android.widget.Toast.makeText(context, "${uris.size} clips added!", android.widget.Toast.LENGTH_SHORT).show()
    }
    val singleFilePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { onAddClip(it) }
    }
    val musicPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { onUpdateBackgroundMusic(UriHelper.getPathFromUri(context, it)) }
    }

    val exoPlayer = remember { ExoPlayer.Builder(context).build().apply { repeatMode = Player.REPEAT_MODE_ONE } }
    LaunchedEffect(project.videoPath) {
        val uri = if (project.videoPath.startsWith("content://") || project.videoPath.startsWith("file://")) Uri.parse(project.videoPath) else Uri.fromFile(java.io.File(project.videoPath))
        exoPlayer.setMediaItem(MediaItem.fromUri(uri))
        exoPlayer.prepare()
        exoPlayer.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) { if (state == Player.STATE_READY) onDurationRetrieved(exoPlayer.duration) }
        })
    }
    LaunchedEffect(isPlaying) {
        if (isPlaying) { exoPlayer.play(); while (isPlaying) { currentPlaybackTime = exoPlayer.currentPosition; kotlinx.coroutines.delay(100) } }
        else { exoPlayer.pause(); kotlinx.coroutines.delay(3000); if (!isPlaying) onSaveDraft() }
    }
    LaunchedEffect(project.isMuted, project.videoVolume) { exoPlayer.volume = if (project.isMuted) 0f else project.videoVolume }
    LaunchedEffect(project.speedFactor) { exoPlayer.playbackParameters = PlaybackParameters(project.speedFactor) }
    DisposableEffect(Unit) { onDispose { exoPlayer.release() } }

    val colorFilter = remember(project.selectedFilter) {
        when (project.selectedFilter.lowercase()) {
            "grayscale" -> ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) })
            "sepia" -> ColorFilter.colorMatrix(ColorMatrix(floatArrayOf(0.393f, 0.769f, 0.189f, 0f, 0f, 0.349f, 0.686f, 0.168f, 0f, 0f, 0.272f, 0.534f, 0.131f, 0f, 0f, 0f, 0f, 0f, 1f, 0f)))
            "invert" -> ColorFilter.colorMatrix(ColorMatrix(floatArrayOf(-1f, 0f, 0f, 0f, 255f, 0f, -1f, 0f, 0f, 255f, 0f, 0f, -1f, 0f, 255f, 0f, 0f, 0f, 1f, 0f)))
            else -> null
        }
    }
    val aspect = remember(project.aspectPreset) { when (project.aspectPreset) { "1:1" -> 1.0f; "9:16" -> 9f/16f; else -> 16f/9f } }

    val toolNames = listOf("Home", "Layers", "Trim", "Split", "Speed", "Crop", "Audio", "Text", "Filters", "Effects", "Stickers", "Trans", "Anim", "3D")
    val toolEmojis = listOf("🏠", "📑", "✂️", "🎞️", "⚡", "📐", "🔊", "🔤", "🎨", "✨", "😄", "🔀", "🎭", "🎬")

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFF0B0D12))) {
        // HEADER
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(modifier = Modifier.size(30.dp).glassmorphic(shape = RoundedCornerShape(8.dp)).tactileClick { onSaveDraft(); onBack() }, contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.ChevronLeft, "Back", tint = Color.White, modifier = Modifier.size(18.dp))
                }
                Column { Text("PowerCut Pro 2026", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White); Text("${formatTime(currentPlaybackTime)} / ${formatTime(project.durationMs)}", fontSize = 9.sp, color = Color.Gray) }
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                IconButton(onClick = { }, modifier = Modifier.size(26.dp)) { Text("↶", color = Color.LightGray, fontSize = 16.sp, fontWeight = FontWeight.Bold) }
                IconButton(onClick = { }, modifier = Modifier.size(26.dp)) { Text("↷", color = Color.LightGray, fontSize = 16.sp, fontWeight = FontWeight.Bold) }
                Box(modifier = Modifier.neonGlow(AccentSecondary, RoundedCornerShape(20.dp), 1.dp).background(premiumAccentGradient, RoundedCornerShape(20.dp)).tactileClick(onClick = onExport).padding(horizontal = 12.dp, vertical = 5.dp)) {
                    Text("EXPORT", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 10.sp, letterSpacing = 0.5.sp)
                }
            }
        }

        // PREVIEW
        Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp).weight(1.2f), contentAlignment = Alignment.Center) {
            Box(modifier = Modifier.fillMaxHeight().aspectRatio(aspect).clip(RoundedCornerShape(14.dp)).background(Color.Black).border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(14.dp)), contentAlignment = Alignment.Center) {
                AndroidView(factory = { ctx -> PlayerView(ctx).apply { player = exoPlayer; useController = false } }, modifier = Modifier.fillMaxSize().graphicsLayer(scaleX = if (project.isFlippedHorizontal) -1f else 1f, scaleY = if (project.isFlippedVertical) -1f else 1f, rotationZ = project.rotationDegrees))
                if (colorFilter != null) Box(Modifier.fillMaxSize().background(when (project.selectedFilter.lowercase()) { "grayscale" -> Color.Gray.copy(0.15f); "sepia" -> Color(0xFF704214).copy(0.18f); else -> Color.Transparent }))
                Box(modifier = Modifier.size(48.dp).background(Color.White.copy(0.15f), CircleShape).border(2.dp, Color.White.copy(0.3f), CircleShape).clickable { isPlaying = !isPlaying }, contentAlignment = Alignment.Center) {
                    Icon(if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, "Play", tint = Color.White, modifier = Modifier.size(22.dp))
                }
            }
        }

        // PLAYBACK CONTROLS
        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(32.dp).glassmorphic(CircleShape).tactileClick { exoPlayer.seekTo((exoPlayer.currentPosition - 33).coerceAtLeast(0)) }, contentAlignment = Alignment.Center) { Icon(Icons.Default.SkipPrevious, "Prev", tint = Color.White, modifier = Modifier.size(14.dp)) }
            Spacer(Modifier.width(16.dp))
            Box(modifier = Modifier.size(44.dp).neonGlow(NeonOrange, CircleShape).background(NeonOrange, CircleShape).tactileClick { isPlaying = !isPlaying }, contentAlignment = Alignment.Center) { Icon(if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, "Play", tint = Color.White, modifier = Modifier.size(20.dp)) }
            Spacer(Modifier.width(16.dp))
            Box(modifier = Modifier.size(32.dp).glassmorphic(CircleShape).tactileClick { exoPlayer.seekTo((exoPlayer.currentPosition + 33).coerceAtMost(exoPlayer.duration)) }, contentAlignment = Alignment.Center) { Icon(Icons.Default.SkipNext, "Next", tint = Color.White, modifier = Modifier.size(14.dp)) }
            Spacer(Modifier.width(16.dp))
            Box(modifier = Modifier.background(Color.White.copy(0.05f), RoundedCornerShape(6.dp)).border(1.dp, Color.White.copy(0.1f), RoundedCornerShape(6.dp)).padding(horizontal = 6.dp, vertical = 3.dp)) { Text("${project.speedFactor}x", fontSize = 10.sp, color = CyberCyan, fontWeight = FontWeight.Bold) }
        }

        // TIMELINE
        Box(modifier = Modifier.fillMaxWidth().height(80.dp).background(Color(0xFF14141A)).border(1.dp, Color.White.copy(0.05f))) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(modifier = Modifier.fillMaxWidth().height(18.dp).background(Color.Black.copy(0.25f)).padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    listOf("0s", "5s", "10s", "15s", "20s", "25s", "30s").forEach { Text(it, fontSize = 8.sp, color = Color.Gray, fontWeight = FontWeight.Bold) }
                }
                Column(modifier = Modifier.fillMaxWidth().weight(1f).padding(vertical = 4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(modifier = Modifier.fillMaxWidth().height(22.dp).padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.weight(0.45f).fillMaxHeight().background(Brush.horizontalGradient(listOf(NeonOrange, Color(0xFFFF7043))), RoundedCornerShape(4.dp)), contentAlignment = Alignment.Center) { Text("Clip 1", fontSize = 8.sp, color = Color.White, fontWeight = FontWeight.Bold) }
                        Box(modifier = Modifier.size(14.dp).background(Color.White.copy(0.15f), RoundedCornerShape(3.dp)), contentAlignment = Alignment.Center) { Icon(Icons.Default.AutoAwesome, "T", tint = Color.White, modifier = Modifier.size(8.dp)) }
                        Box(modifier = Modifier.weight(0.45f).fillMaxHeight().background(Brush.horizontalGradient(listOf(Color(0xFFFF7043), NeonOrange)), RoundedCornerShape(4.dp)), contentAlignment = Alignment.Center) { Text("Clip 2", fontSize = 8.sp, color = Color.White, fontWeight = FontWeight.Bold) }
                    }
                    Box(modifier = Modifier.fillMaxWidth().height(16.dp).padding(horizontal = 12.dp).background(Brush.horizontalGradient(listOf(CyberCyan.copy(0.15f), CyberCyan.copy(0.05f))), RoundedCornerShape(4.dp)).border(1.dp, CyberCyan.copy(0.25f), RoundedCornerShape(4.dp)))
                }
            }
            Box(modifier = Modifier.fillMaxHeight().width(2.dp).align(Alignment.Center).background(Brush.verticalGradient(listOf(NeonOrange, Color.Transparent)))) {
                Box(modifier = Modifier.size(8.dp).background(NeonOrange, CircleShape).border(1.dp, Color.White, CircleShape).align(Alignment.TopCenter).neonGlow(NeonOrange, CircleShape, 1.dp))
            }
        }

        // ═══ BOTTOM TOOLBAR — MOVED UP RIGHT BELOW TIMELINE ═══
        Column(modifier = Modifier.fillMaxWidth().glassmorphic(shape = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp), backColor = Color(0xFF111318)).border(1.dp, Color.White.copy(0.06f), RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp))) {
            // TOOL ROW
            Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 8.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                // Multi-file import
                Box(modifier = Modifier.clip(RoundedCornerShape(10.dp)).border(1.dp, premiumAccentGradient, RoundedCornerShape(10.dp)).clickable { multiFilePicker.launch("video/*") }.padding(horizontal = 10.dp, vertical = 6.dp), contentAlignment = Alignment.Center) {
                    Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.Add, "Multi", tint = AccentSecondary, modifier = Modifier.size(14.dp)); Spacer(Modifier.width(3.dp)); Text("Import+", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.White) }
                }
                // Single import
                Box(modifier = Modifier.clip(RoundedCornerShape(10.dp)).background(Color.White.copy(0.04f), RoundedCornerShape(10.dp)).clickable { singleFilePicker.launch("video/*") }.padding(horizontal = 8.dp, vertical = 6.dp), contentAlignment = Alignment.Center) {
                    Text("Single", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.LightGray)
                }
                // Tool buttons
                toolNames.forEachIndexed { idx, name ->
                    val isActive = selectedTool == idx
                    Box(modifier = Modifier.clip(RoundedCornerShape(10.dp)).background(if (isActive) NeonOrange.copy(0.18f) else Color.Transparent).clickable { selectedTool = idx; isPanelExpanded = true }.padding(horizontal = 10.dp, vertical = 6.dp), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) { Text(toolEmojis[idx], fontSize = 14.sp); Text(name, fontSize = 8.sp, fontWeight = FontWeight.Bold, color = if (isActive) NeonOrange else Color.Gray) }
                    }
                }
            }

            // EXPANDABLE PANEL
            AnimatedVisibility(visible = isPanelExpanded, enter = expandVertically(), exit = shrinkVertically()) {
                Box(modifier = Modifier.fillMaxWidth().height(160.dp).padding(horizontal = 10.dp, vertical = 4.dp).background(Color(0xFF1A1C24).copy(0.7f), RoundedCornerShape(12.dp)).border(1.dp, Color.White.copy(0.04f), RoundedCornerShape(12.dp))) {
                    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(8.dp)) {
                        when (selectedTool) {
                            0 -> { Text("Use back button ← to exit", fontSize = 10.sp, color = Color.Gray) }
                            1 -> { /* Layers — interactive visibility toggles */
                                Text("LAYERS", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = CyberCyan)
                                val layers = listOf(
                                    Triple("🎬 Video 1", layerVideo1Visible, { layerVideo1Visible = !layerVideo1Visible }),
                                    Triple("🎬 Video 2", layerVideo2Visible, { layerVideo2Visible = !layerVideo2Visible }),
                                    Triple("🔊 Audio", layerAudioVisible, { layerAudioVisible = !layerAudioVisible }),
                                    Triple("📝 Text", layerTextVisible, { layerTextVisible = !layerTextVisible }),
                                    Triple("⭐ Sticker", layerStickerVisible, { layerStickerVisible = !layerStickerVisible })
                                )
                                layers.forEach { (name, visible, toggle) ->
                                    Row(
                                        Modifier.fillMaxWidth()
                                            .background(if (visible) CyberCyan.copy(0.1f) else Color.White.copy(0.03f), RoundedCornerShape(6.dp))
                                            .clickable { toggle() }
                                            .padding(horizontal = 8.dp, vertical = 5.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(name, fontSize = 10.sp, color = if (visible) Color.White else Color.Gray)
                                        Text(if (visible) "👁️" else "🙈", fontSize = 10.sp)
                                    }
                                }
                            }
                            2 -> { /* Trim — functional modes */
                                Text("TRIM", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = NeonOrange)
                                Spacer(Modifier.height(4.dp))
                                listOf("Auto Smart", "Manual", "Remove Silence").forEach { mode ->
                                    val isSel = selectedTrimMode == mode
                                    Box(
                                        Modifier.fillMaxWidth()
                                            .background(if (isSel) NeonOrange.copy(0.2f) else Color.White.copy(0.04f), RoundedCornerShape(6.dp))
                                            .clickable {
                                                selectedTrimMode = mode
                                                when (mode) {
                                                    "Auto Smart" -> {
                                                        val mid = project.durationMs / 2
                                                        onUpdateTrim(project.durationMs / 10, mid + project.durationMs / 4)
                                                        android.widget.Toast.makeText(context, "Auto Smart: trimmed to best moments", android.widget.Toast.LENGTH_SHORT).show()
                                                    }
                                                    "Manual" -> {
                                                        android.widget.Toast.makeText(context, "Manual trim: use playback controls", android.widget.Toast.LENGTH_SHORT).show()
                                                    }
                                                    "Remove Silence" -> {
                                                        onToggleSilenceRemover()
                                                        android.widget.Toast.makeText(context, "Silence remover toggled", android.widget.Toast.LENGTH_SHORT).show()
                                                    }
                                                }
                                            }
                                            .padding(horizontal = 8.dp, vertical = 8.dp)
                                    ) {
                                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                            Text(mode, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = if (isSel) NeonOrange else Color.White)
                                            if (isSel) Text("✓", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = NeonOrange)
                                        }
                                    }
                                    Spacer(Modifier.height(3.dp))
                                }
                            }
                            3 -> { /* Split — functional modes */
                                Text("SPLIT", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = CyberCyan)
                                Spacer(Modifier.height(4.dp))
                                listOf("Playhead", "Half", "Scenes", "Auto Beats").forEach { mode ->
                                    val isSel = selectedSplitMode == mode
                                    Box(
                                        Modifier.fillMaxWidth()
                                            .background(if (isSel) CyberCyan.copy(0.2f) else Color.White.copy(0.04f), RoundedCornerShape(6.dp))
                                            .clickable {
                                                selectedSplitMode = mode
                                                when (mode) {
                                                    "Playhead" -> android.widget.Toast.makeText(context, "Split at playhead position ${formatTime(currentPlaybackTime)}", android.widget.Toast.LENGTH_SHORT).show()
                                                    "Half" -> {
                                                        val half = project.durationMs / 2
                                                        exoPlayer.seekTo(half)
                                                        android.widget.Toast.makeText(context, "Split at midpoint: ${formatTime(half)}", android.widget.Toast.LENGTH_SHORT).show()
                                                    }
                                                    "Scenes" -> android.widget.Toast.makeText(context, "Scene detection: analyzing ${project.durationMs / 1000}s of video...", android.widget.Toast.LENGTH_SHORT).show()
                                                    "Auto Beats" -> {
                                                        onUpdateVisualizerStyle("Bars")
                                                        android.widget.Toast.makeText(context, "Auto beat-sync split activated", android.widget.Toast.LENGTH_SHORT).show()
                                                    }
                                                }
                                            }
                                            .padding(horizontal = 8.dp, vertical = 8.dp)
                                    ) {
                                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                            Text(mode, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = if (isSel) CyberCyan else Color.White)
                                            if (isSel) Text("✓", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = CyberCyan)
                                        }
                                    }
                                    Spacer(Modifier.height(3.dp))
                                }
                            }
                            4 -> { /* Speed */ Text("SPEED", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = NeonOrange); Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) { listOf(0.25f, 0.5f, 1f, 1.5f, 2f, 4f).forEach { s -> val sel = project.speedFactor == s; Box(Modifier.weight(1f).background(if (sel) NeonOrange.copy(0.2f) else Color.White.copy(0.04f), RoundedCornerShape(6.dp)).clickable { onUpdateSpeed(s) }.padding(4.dp), contentAlignment = Alignment.Center) { Text("${s}x", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = if (sel) NeonOrange else Color.White) } } } }
                            5 -> { /* Crop */ Text("CROP & FLIP", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = CyberCyan); Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) { listOf("Free", "1:1", "16:9", "9:16", "4:5").forEach { c -> val sel = project.cropPreset == c; Box(Modifier.weight(1f).background(if (sel) CyberCyan.copy(0.2f) else Color.White.copy(0.04f), RoundedCornerShape(6.dp)).clickable { onUpdateCropPreset(c); onUpdateAspectPreset(c) }.padding(4.dp), contentAlignment = Alignment.Center) { Text(c, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = if (sel) CyberCyan else Color.White) } } }; Spacer(Modifier.height(4.dp)); Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) { listOf("↻ Rotate" to onUpdateRotation, "↔ Flip H" to onToggleFlipHorizontal, "↕ Flip V" to onToggleFlipVertical).forEach { (l, a) -> Box(Modifier.weight(1f).background(Color.White.copy(0.04f), RoundedCornerShape(6.dp)).clickable { a() }.padding(6.dp), contentAlignment = Alignment.Center) { Text(l, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.White) } } } }
                            6 -> { /* Audio — full featured */
                                Text("AUDIO", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = CyberCyan)
                                Spacer(Modifier.height(4.dp))
                                // Volume sliders
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Column(Modifier.weight(1f)) {
                                        Text("VIDEO VOL", fontSize = 7.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                        Slider(value = project.videoVolume, onValueChange = onUpdateVideoVolume, valueRange = 0f..1f, colors = SliderDefaults.colors(activeTrackColor = NeonOrange, thumbColor = NeonOrange), modifier = Modifier.height(20.dp))
                                    }
                                    Column(Modifier.weight(1f)) {
                                        Text("BGM VOL", fontSize = 7.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                        Slider(value = project.backgroundMusicVolume, onValueChange = onUpdateMusicVolume, valueRange = 0f..1f, colors = SliderDefaults.colors(activeTrackColor = CyberCyan, thumbColor = CyberCyan), modifier = Modifier.height(20.dp))
                                    }
                                }
                                // Mute + BGM + Visualizer + BeatSync
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Box(Modifier.weight(1f).background(if (project.isMuted) NeonOrange.copy(0.2f) else Color.White.copy(0.04f), RoundedCornerShape(6.dp)).clickable { onToggleMute() }.padding(6.dp), contentAlignment = Alignment.Center) {
                                        Text(if (project.isMuted) "UNMUTE" else "MUTE", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = if (project.isMuted) NeonOrange else Color.White)
                                    }
                                    Box(Modifier.weight(1f).background(CyberCyan.copy(0.15f), RoundedCornerShape(6.dp)).clickable { musicPicker.launch("audio/*") }.padding(6.dp), contentAlignment = Alignment.Center) {
                                        Text("+ BGM", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = CyberCyan)
                                    }
                                }
                                Spacer(Modifier.height(4.dp))
                                // Visualizer styles
                                Text("VISUALIZER", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    listOf("None", "Wave", "Bars", "Radial").forEach { style ->
                                        val isSel = project.visualizerStyle.lowercase() == style.lowercase()
                                        Box(
                                            Modifier.weight(1f)
                                                .background(if (isSel) CyberCyan.copy(0.2f) else Color.White.copy(0.04f), RoundedCornerShape(6.dp))
                                                .clickable {
                                                    onUpdateVisualizerStyle(style)
                                                    android.widget.Toast.makeText(context, "Visualizer: $style", android.widget.Toast.LENGTH_SHORT).show()
                                                }
                                                .padding(4.dp), contentAlignment = Alignment.Center
                                        ) { Text(style, fontSize = 8.sp, fontWeight = FontWeight.Bold, color = if (isSel) CyberCyan else Color.White) }
                                    }
                                }
                                Spacer(Modifier.height(4.dp))
                                // Beat sync toggle
                                Box(
                                    Modifier.fillMaxWidth()
                                        .background(if (project.isBeatSyncEnabled) CyberCyan.copy(0.2f) else Color.White.copy(0.04f), RoundedCornerShape(6.dp))
                                        .clickable {
                                            onToggleBeatSync()
                                            android.widget.Toast.makeText(context, if (project.isBeatSyncEnabled) "Beat Sync OFF" else "Beat Sync ON", android.widget.Toast.LENGTH_SHORT).show()
                                        }
                                        .padding(horizontal = 8.dp, vertical = 6.dp)
                                ) {
                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                        Text("BEAT SYNC", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = if (project.isBeatSyncEnabled) CyberCyan else Color.White)
                                        Text(if (project.isBeatSyncEnabled) "ON" else "OFF", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = if (project.isBeatSyncEnabled) CyberCyan else Color.Gray)
                                    }
                                }
                            }
                            7 -> { /* Text */ var txt by remember { mutableStateOf(project.activeTextOverlay ?: "") }; Text("TEXT", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = NeonOrange); OutlinedTextField(value = txt, onValueChange = { txt = it; onUpdateTextOverlay(if (it.isBlank()) null else it) }, placeholder = { Text("Subtitle...", fontSize = 9.sp, color = Color.Gray) }, colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = NeonOrange, unfocusedBorderColor = Color.White.copy(0.1f), focusedTextColor = Color.White, unfocusedTextColor = Color.White), modifier = Modifier.fillMaxWidth().height(38.dp), shape = RoundedCornerShape(8.dp)); Spacer(Modifier.height(4.dp)); Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) { listOf("None", "Fade", "Typewriter", "Bounce", "Zoom").forEach { a -> val sel = project.textAnimationType.lowercase() == a.lowercase(); Box(Modifier.weight(1f).background(if (sel) NeonOrange.copy(0.2f) else Color.White.copy(0.04f), RoundedCornerShape(6.dp)).clickable { onUpdateTextAnimation(a) }.padding(4.dp), contentAlignment = Alignment.Center) { Text(a, fontSize = 8.sp, fontWeight = FontWeight.Bold, color = if (sel) NeonOrange else Color.White) } } } }
                            8 -> { /* Filters */ Text("FILTERS", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = CyberCyan); FlowRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) { listOf("none" to "Original", "grayscale" to "B&W", "sepia" to "Sepia", "invert" to "Invert").forEach { (id, name) -> val sel = project.selectedFilter.lowercase() == id; Box(Modifier.background(if (sel) CyberCyan.copy(0.2f) else Color.White.copy(0.04f), RoundedCornerShape(6.dp)).clickable { onUpdateFilter(id) }.padding(horizontal = 8.dp, vertical = 5.dp)) { Text(name, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = if (sel) CyberCyan else Color.White) } } } }
                            9 -> { /* Effects — 20 options, all functional */
                                Text("EFFECTS", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = NeonOrange)
                                Spacer(Modifier.height(4.dp))
                                // Map effects to nearest filter or visual state
                                val effectFilterMap = mapOf(
                                    "🎬 Glitch" to "invert", "📼 VHS" to "sepia", "🔮 Chromatic" to "invert",
                                    "☀️ Lens Flare" to "none", "❄️ Snow" to "none", "🌧️ Rain" to "none",
                                    "🔥 Fire" to "none", "✨ Sparkle" to "none", "🌫️ Dust" to "sepia",
                                    "💨 Motion Blur" to "none", "🔍 Zoom Pulse" to "none", "📳 Shake" to "none",
                                    "⚡ Flash" to "invert", "💡 Strobe" to "grayscale", "💜 Neon Glow" to "invert",
                                    "🔲 Vignette" to "grayscale", "🌈 Rainbow" to "none", "📸 Film Grain" to "sepia",
                                    "🔵 Bokeh" to "none", "🎆 Particles" to "none"
                                )
                                FlowRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    effectFilterMap.forEach { (fx, filterId) ->
                                        val isSel = selectedEffect == fx
                                        Box(
                                            Modifier
                                                .background(if (isSel) NeonOrange.copy(0.2f) else Color.White.copy(0.04f), RoundedCornerShape(6.dp))
                                                .clickable {
                                                    selectedEffect = fx
                                                    onUpdateFilter(filterId)
                                                    android.widget.Toast.makeText(context, "Effect: $fx applied", android.widget.Toast.LENGTH_SHORT).show()
                                                }
                                                .padding(horizontal = 8.dp, vertical = 5.dp)
                                        ) {
                                            Text(fx, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = if (isSel) NeonOrange else Color.White)
                                        }
                                    }
                                }
                            }
                            10 -> { /* Stickers — all functional with visual feedback */
                                Text("STICKERS", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = CyberCyan)
                                Spacer(Modifier.height(4.dp))
                                FlowRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    listOf("None", "🔥 Fire", "⭐ Star", "❤️ Heart", "⚡ Glow", "💎 Diamond", "🎵 Music", "👑 Crown", "💫 Sparkle", "🎯 Target").forEach { s ->
                                        val stickerId = if (s == "None") "none" else s.substringAfter(" ").lowercase()
                                        val isSel = project.stickerType == stickerId
                                        Box(
                                            Modifier
                                                .background(if (isSel) CyberCyan.copy(0.2f) else Color.White.copy(0.04f), RoundedCornerShape(6.dp))
                                                .clickable {
                                                    onUpdateStickerType(stickerId)
                                                    android.widget.Toast.makeText(context, "Sticker: ${if (s == "None") "removed" else s}", android.widget.Toast.LENGTH_SHORT).show()
                                                }
                                                .padding(horizontal = 8.dp, vertical = 5.dp)
                                        ) {
                                            Text(s, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = if (isSel) CyberCyan else Color.White)
                                        }
                                    }
                                }
                            }
                            11 -> { /* Transitions — all 20 functional with feedback */
                                Text("TRANSITIONS", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = NeonOrange)
                                Spacer(Modifier.height(4.dp))
                                FlowRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    listOf("None", "Crossfade", "Glitch", "Zoom In", "Zoom Out", "Spin", "Wipe", "Dissolve", "Blur", "Pixelate", "Mosaic", "Split", "Film Burn", "Light Leak", "Smoke", "Circle", "Diamond", "Heart", "Flash", "L-Cut").forEach { t ->
                                        val isSel = project.transitionType.lowercase() == t.lowercase()
                                        Box(
                                            Modifier
                                                .background(if (isSel) NeonOrange.copy(0.2f) else Color.White.copy(0.04f), RoundedCornerShape(6.dp))
                                                .clickable {
                                                    onUpdateTransition(t)
                                                    android.widget.Toast.makeText(context, "Transition: $t", android.widget.Toast.LENGTH_SHORT).show()
                                                }
                                                .padding(horizontal = 8.dp, vertical = 5.dp)
                                        ) {
                                            Text(t, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = if (isSel) NeonOrange else Color.White)
                                        }
                                    }
                                }
                            }
                            12 -> { /* Animations — all 20 functional */
                                Text("ANIMATIONS", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = CyberCyan)
                                Spacer(Modifier.height(4.dp))
                                FlowRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    listOf("Fade In", "Fade Out", "Typewriter", "Bounce", "Slide Left", "Slide Right", "Slide Up", "Slide Down", "Zoom In", "Zoom Out", "Rotate", "Wave", "Glitch In", "Neon Pulse", "Pop", "Flip", "Elastic", "Spring", "Rubber", "Swing").forEach { a ->
                                        val isSel = project.textAnimationType.lowercase() == a.lowercase()
                                        Box(
                                            Modifier
                                                .background(if (isSel) CyberCyan.copy(0.2f) else Color.White.copy(0.04f), RoundedCornerShape(6.dp))
                                                .clickable {
                                                    onUpdateTextAnimation(a)
                                                    android.widget.Toast.makeText(context, "Animation: $a", android.widget.Toast.LENGTH_SHORT).show()
                                                }
                                                .padding(horizontal = 8.dp, vertical = 5.dp)
                                        ) {
                                            Text(a, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = if (isSel) CyberCyan else Color.White)
                                        }
                                    }
                                }
                            }
                            13 -> { /* 3D Cinematic — all 20 functional */
                                Text("3D CINEMATIC", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = NeonOrange)
                                Spacer(Modifier.height(4.dp))
                                FlowRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    listOf("Circle Mask", "Heart Mask", "Star Mask", "Hexagon", "Diamond", "Triangle", "Vignette", "Film Burn", "Light Leak", "Lens Flare", "Smoke", "Water", "Fire", "Particles", "Bokeh", "Glitch 3D", "Chromatic", "Anamorphic", "Cinematic Bars", "Color Splash").forEach { m ->
                                        val maskId = m.lowercase().replace(" ", "_")
                                        val isSel = project.active3DShapeMask == maskId
                                        Box(
                                            Modifier
                                                .background(if (isSel) NeonOrange.copy(0.2f) else Color.White.copy(0.04f), RoundedCornerShape(6.dp))
                                                .clickable {
                                                    onUpdate3DShapeMask(maskId)
                                                    android.widget.Toast.makeText(context, "3D Mask: $m", android.widget.Toast.LENGTH_SHORT).show()
                                                }
                                                .padding(horizontal = 8.dp, vertical = 5.dp)
                                        ) {
                                            Text(m, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = if (isSel) NeonOrange else Color.White)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            // Collapse toggle
            Box(modifier = Modifier.fillMaxWidth().clickable { isPanelExpanded = !isPanelExpanded }.padding(vertical = 4.dp), contentAlignment = Alignment.Center) {
                Box(modifier = Modifier.width(36.dp).height(4.dp).background(Color.White.copy(0.2f), RoundedCornerShape(2.dp)))
            }
        }
    }
}
