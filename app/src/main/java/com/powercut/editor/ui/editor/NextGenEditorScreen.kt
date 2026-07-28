package com.powercut.editor.ui.editor

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
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
import androidx.compose.foundation.layout.PaddingValues
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

// ═══════════════════════════════════════════════════════════════
//  NEXT GEN EDITOR — CapCut-Level Premium 2026 Video Editor
//  Bottom toolbar MOVED UP, multi-file select, all tools
// ═══════════════════════════════════════════════════════════════

private fun formatTime(ms: Long): String {
    val totalSecs = ms / 1000
    val minutes = totalSecs / 60
    val seconds = totalSecs % 60
    return String.format(Locale.US, "%02d:%02d", minutes, seconds)
}

// Tool categories for the bottom bar
enum class EditorTool(val label: String, val emoji: String) {
    HOME("Home", "🏠"),
    LAYERS("Layers", "📑"),
    TRIM("Trim", "✂️"),
    SPLIT("Split", "🎞️"),
    SPEED("Speed", "⚡"),
    CROP("Crop", "📐"),
    AUDIO("Audio", "🔊"),
    TEXT("Text", "🔤"),
    FILTERS("Filters", "🎨"),
    EFFECTS("Effects", "✨"),
    STICKERS("Stickers", "😄"),
    TRANSITIONS("Trans", "🔀")
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

    // ── State ──────────────────────────────────────────────────
    var selectedTool by remember { mutableStateOf(EditorTool.TRIM) }
    var isPanelExpanded by remember { mutableStateOf(true) }
    var isPlaying by remember { mutableStateOf(false) }
    var currentPlaybackTime by remember { mutableStateOf(0L) }

    // ── Multi-file picker ──────────────────────────────────────
    val multiFilePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        uris.forEach { uri ->
            onAddClip(uri)
        }
        if (uris.isNotEmpty()) {
            android.widget.Toast.makeText(context, "${uris.size} clip(s) added!", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    val clipPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            onAddClip(uri)
            android.widget.Toast.makeText(context, "Clip added!", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    val musicPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            val audioPath = UriHelper.getPathFromUri(context, uri)
            onUpdateBackgroundMusic(audioPath)
        }
    }

    // ── ExoPlayer ──────────────────────────────────────────────
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply { repeatMode = Player.REPEAT_MODE_ONE }
    }

    LaunchedEffect(project.videoPath) {
        val uri = if (project.videoPath.startsWith("content://") || project.videoPath.startsWith("file://")) {
            Uri.parse(project.videoPath)
        } else {
            Uri.fromFile(java.io.File(project.videoPath))
        }
        exoPlayer.setMediaItem(MediaItem.fromUri(uri))
        exoPlayer.prepare()
        exoPlayer.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                if (state == Player.STATE_READY) onDurationRetrieved(exoPlayer.duration)
            }
        })
    }

    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            exoPlayer.play()
            while (isPlaying) {
                currentPlaybackTime = exoPlayer.currentPosition
                kotlinx.coroutines.delay(100)
            }
        } else {
            exoPlayer.pause()
            kotlinx.coroutines.delay(3000)
            if (!isPlaying) onSaveDraft()
        }
    }

    LaunchedEffect(project.isMuted, project.videoVolume) {
        exoPlayer.volume = if (project.isMuted) 0f else project.videoVolume
    }

    LaunchedEffect(project.speedFactor) {
        exoPlayer.playbackParameters = PlaybackParameters(project.speedFactor)
    }

    DisposableEffect(Unit) { onDispose { exoPlayer.release() } }

    // ── Color filter ───────────────────────────────────────────
    val composeColorFilter = remember(project.selectedFilter) {
        when (project.selectedFilter.lowercase()) {
            "grayscale" -> ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) })
            "sepia" -> ColorFilter.colorMatrix(ColorMatrix(floatArrayOf(
                0.393f, 0.769f, 0.189f, 0f, 0f, 0.349f, 0.686f, 0.168f, 0f, 0f,
                0.272f, 0.534f, 0.131f, 0f, 0f, 0f, 0f, 0f, 1f, 0f
            )))
            "invert" -> ColorFilter.colorMatrix(ColorMatrix(floatArrayOf(
                -1f, 0f, 0f, 0f, 255f, 0f, -1f, 0f, 0f, 255f, 0f, 0f, -1f, 0f, 255f, 0f, 0f, 0f, 1f, 0f
            )))
            else -> null
        }
    }

    val videoAspectRatio = remember(project.aspectPreset) {
        when (project.aspectPreset) {
            "1:1" -> 1.0f; "9:16" -> 9f / 16f; "16:9" -> 16f / 9f; else -> 16f / 9f
        }
    }

    // ═══════════════════════════════════════════════════════════
    //  LAYOUT — Preview | Timeline | Tools (MUCH HIGHER!)
    // ═══════════════════════════════════════════════════════════
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0B0D12))
    ) {
        // ── 1. MINI HEADER ─────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    modifier = Modifier.size(30.dp).glassmorphic(shape = RoundedCornerShape(8.dp))
                        .tactileClick { onSaveDraft(); onBack() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.ChevronLeft, "Back", tint = Color.White, modifier = Modifier.size(18.dp))
                }
                Column {
                    Text("PowerCut Pro", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Text("${formatTime(currentPlaybackTime)} / ${formatTime(project.durationMs)}", fontSize = 9.sp, color = Color.Gray)
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                // Undo/Redo
                IconButton(onClick = { }, modifier = Modifier.size(26.dp)) {
                    Text("↶", color = Color.LightGray, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
                IconButton(onClick = { }, modifier = Modifier.size(26.dp)) {
                    Text("↷", color = Color.LightGray, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
                // Export
                Box(
                    modifier = Modifier.neonGlow(AccentSecondary, RoundedCornerShape(20.dp), 1.dp)
                        .background(premiumAccentGradient, RoundedCornerShape(20.dp))
                        .tactileClick(onClick = onExport)
                        .padding(horizontal = 12.dp, vertical = 5.dp)
                ) {
                    Text("EXPORT", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 10.sp, letterSpacing = 0.5.sp)
                }
            }
        }

        // ── 2. VIDEO PREVIEW (compact) ─────────────────────────
        Box(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp).weight(1.2f),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier.fillMaxHeight().aspectRatio(videoAspectRatio)
                    .clip(RoundedCornerShape(14.dp)).background(Color.Black)
                    .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center
            ) {
                AndroidView(
                    factory = { ctx -> PlayerView(ctx).apply { player = exoPlayer; useController = false } },
                    modifier = Modifier.fillMaxSize().graphicsLayer(
                        scaleX = if (project.isFlippedHorizontal) -1f else 1f,
                        scaleY = if (project.isFlippedVertical) -1f else 1f,
                        rotationZ = project.rotationDegrees
                    )
                )
                if (project.selectedFilter.lowercase() != "none" && composeColorFilter != null) {
                    val overlayColor = when (project.selectedFilter.lowercase()) {
                        "grayscale" -> Color.Gray.copy(alpha = 0.15f)
                        "sepia" -> Color(0xFF704214).copy(alpha = 0.18f)
                        "invert" -> Color.White.copy(alpha = 0.1f)
                        else -> Color.Transparent
                    }
                    Box(Modifier.fillMaxSize().background(overlayColor))
                }
                // Play/Pause overlay
                Box(
                    modifier = Modifier.size(48.dp).background(Color.White.copy(alpha = 0.15f), CircleShape)
                        .border(2.dp, Color.White.copy(alpha = 0.3f), CircleShape)
                        .clickable { isPlaying = !isPlaying },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, "Play", tint = Color.White, modifier = Modifier.size(22.dp))
                }
                // Text overlay
                Box(
                    modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 8.dp)
                        .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 10.dp, vertical = 3.dp)
                ) {
                    Text(project.activeTextOverlay ?: "PowerCut 2026 ✨", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // ── 3. PLAYBACK CONTROLS (compact) ─────────────────────
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.size(32.dp).glassmorphic(CircleShape)
                .tactileClick { exoPlayer.seekTo((exoPlayer.currentPosition - 33).coerceAtLeast(0)) },
                contentAlignment = Alignment.Center
            ) { Icon(Icons.Default.SkipPrevious, "Prev", tint = Color.White, modifier = Modifier.size(14.dp)) }

            Spacer(Modifier.width(16.dp))

            Box(modifier = Modifier.size(44.dp).neonGlow(NeonOrange, CircleShape)
                .background(NeonOrange, CircleShape).tactileClick { isPlaying = !isPlaying },
                contentAlignment = Alignment.Center
            ) { Icon(if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, "Play", tint = Color.White, modifier = Modifier.size(20.dp)) }

            Spacer(Modifier.width(16.dp))

            Box(modifier = Modifier.size(32.dp).glassmorphic(CircleShape)
                .tactileClick { exoPlayer.seekTo((exoPlayer.currentPosition + 33).coerceAtMost(exoPlayer.duration)) },
                contentAlignment = Alignment.Center
            ) { Icon(Icons.Default.SkipNext, "Next", tint = Color.White, modifier = Modifier.size(14.dp)) }

            Spacer(Modifier.width(16.dp))

            Box(modifier = Modifier.background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(6.dp))
                .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(6.dp))
                .padding(horizontal = 6.dp, vertical = 3.dp)
            ) { Text("${project.speedFactor}x", fontSize = 10.sp, color = CyberCyan, fontWeight = FontWeight.Bold) }
        }

        // ── 4. TIMELINE (compact) ───────────────────────────────
        Box(
            modifier = Modifier.fillMaxWidth().height(90.dp)
                .background(Color(0xFF14141A)).border(1.dp, Color.White.copy(alpha = 0.05f))
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Time ruler
                Row(modifier = Modifier.fillMaxWidth().height(20.dp)
                    .background(Color.Black.copy(alpha = 0.25f)).padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    listOf("0s", "5s", "10s", "15s", "20s", "25s", "30s").forEach {
                        Text(it, fontSize = 8.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                    }
                }
                // Tracks
                Column(modifier = Modifier.fillMaxWidth().weight(1f).padding(vertical = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    // Video track
                    Row(modifier = Modifier.fillMaxWidth().height(24.dp).padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.weight(0.45f).fillMaxHeight()
                            .background(Brush.horizontalGradient(listOf(NeonOrange, Color(0xFFFF7043))), RoundedCornerShape(4.dp)),
                            contentAlignment = Alignment.Center
                        ) { Text("Clip 1.mp4", fontSize = 8.sp, color = Color.White, fontWeight = FontWeight.Bold) }
                        Box(modifier = Modifier.size(14.dp).background(Color.White.copy(alpha = 0.15f), RoundedCornerShape(3.dp)),
                            contentAlignment = Alignment.Center
                        ) { Icon(Icons.Default.AutoAwesome, "Trans", tint = Color.White, modifier = Modifier.size(8.dp)) }
                        Box(modifier = Modifier.weight(0.45f).fillMaxHeight()
                            .background(Brush.horizontalGradient(listOf(Color(0xFFFF7043), NeonOrange)), RoundedCornerShape(4.dp)),
                            contentAlignment = Alignment.Center
                        ) { Text("Clip 2.mp4", fontSize = 8.sp, color = Color.White, fontWeight = FontWeight.Bold) }
                    }
                    // Audio track
                    Box(modifier = Modifier.fillMaxWidth().height(18.dp).padding(horizontal = 12.dp)
                        .background(Brush.horizontalGradient(listOf(CyberCyan.copy(alpha = 0.15f), CyberCyan.copy(alpha = 0.05f))), RoundedCornerShape(4.dp))
                        .border(1.dp, CyberCyan.copy(alpha = 0.25f), RoundedCornerShape(4.dp)),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Row(modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(3.dp), verticalAlignment = Alignment.CenterVertically) {
                            listOf(8, 14, 10, 18, 12, 6, 16, 20, 14, 10, 16, 18, 8, 12, 16, 6, 14, 20).forEach { h ->
                                Box(modifier = Modifier.width(3.dp).height(h.dp)
                                    .background(Brush.verticalGradient(listOf(CyberCyan, CyberCyan.copy(alpha = 0.3f))), RoundedCornerShape(1.dp)))
                            }
                        }
                    }
                }
            }
            // Playhead
            Box(modifier = Modifier.fillMaxHeight().width(2.dp).align(Alignment.Center)
                .background(Brush.verticalGradient(listOf(NeonOrange, Color.Transparent)))) {
                Box(modifier = Modifier.size(8.dp).background(NeonOrange, CircleShape)
                    .border(1.dp, Color.White, CircleShape).align(Alignment.TopCenter)
                    .neonGlow(NeonOrange, CircleShape, 1.dp))
            }
        }

        // ═══════════════════════════════════════════════════════
        //  5. MAIN TOOLBAR — MOVED UP! Right below timeline!
        //  This is the key CapCut-like change
        // ═══════════════════════════════════════════════════════
        Column(
            modifier = Modifier.fillMaxWidth()
                .glassmorphic(shape = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp), backColor = Color(0xFF111318))
                .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp))
        ) {
            // ── Quick Tools Row ─────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Multi-file import button
                Box(
                    modifier = Modifier.clip(RoundedCornerShape(10.dp))
                        .border(1.dp, premiumAccentGradient, RoundedCornerShape(10.dp))
                        .clickable { multiFilePickerLauncher.launch("video/*") }
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Add, "Multi Import", tint = AccentSecondary, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(3.dp))
                        Text("Import+", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }

                // Single import
                Box(
                    modifier = Modifier.clip(RoundedCornerShape(10.dp))
                        .background(Color.White.copy(alpha = 0.04f), RoundedCornerShape(10.dp))
                        .clickable { clipPickerLauncher.launch("video/*") }
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Add, "Single", tint = Color.LightGray, modifier = Modifier.size(12.dp))
                        Spacer(Modifier.width(2.dp))
                        Text("Single", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.LightGray)
                    }
                }

                // Tool buttons
                EditorTool.values().forEach { tool ->
                    val isActive = selectedTool == tool
                    Box(
                        modifier = Modifier.clip(RoundedCornerShape(10.dp))
                            .background(if (isActive) NeonOrange.copy(alpha = 0.18f) else Color.Transparent)
                            .clickable {
                                selectedTool = tool
                                isPanelExpanded = true
                            }
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(tool.emoji, fontSize = 14.sp)
                            Text(tool.label, fontSize = 8.sp, fontWeight = FontWeight.Bold,
                                color = if (isActive) NeonOrange else Color.Gray)
                        }
                    }
                }
            }

            // ── Expandable Options Panel ────────────────────────
            AnimatedVisibility(
                visible = isPanelExpanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Box(
                    modifier = Modifier.fillMaxWidth().height(140.dp)
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                        .background(Color(0xFF1A1C24).copy(alpha = 0.7f), RoundedCornerShape(12.dp))
                        .border(1.dp, Color.White.copy(alpha = 0.04f), RoundedCornerShape(12.dp))
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(8.dp)
                    ) {
                        when (selectedTool) {
                            EditorTool.HOME -> { Text("Use back button to exit", fontSize = 10.sp, color = Color.Gray) }
                            EditorTool.LAYERS -> LayersPanel()
                            EditorTool.TRIM -> TrimPanel(project, onUpdateTrim)
                            EditorTool.SPLIT -> SplitPanel(context)
                            EditorTool.SPEED -> SpeedPanel(project, onUpdateSpeed, onUpdateSpeedCurve)
                            EditorTool.CROP -> CropPanel(project, onUpdateCropPreset, onUpdateAspectPreset, onUpdateRotation, onToggleFlipHorizontal, onToggleFlipVertical)
                            EditorTool.AUDIO -> AudioPanel(project, onUpdateVideoVolume, onUpdateMusicVolume, onToggleMute, musicPickerLauncher, onUpdateVisualizerStyle, onToggleBeatSync)
                            EditorTool.TEXT -> TextPanel(project, onUpdateTextOverlay, onUpdateTextAnimation)
                            EditorTool.FILTERS -> FiltersPanel(project, onUpdateFilter)
                            EditorTool.EFFECTS -> EffectsPanel()
                            EditorTool.STICKERS -> StickersPanel(project, onUpdateStickerType, onUpdate3DShapeMask)
                            EditorTool.TRANSITIONS -> TransitionsPanel(project, onUpdateTransition)
                        }
                    }
                }
            }

            // ── Collapse toggle ─────────────────────────────────
            Box(
                modifier = Modifier.fillMaxWidth().clickable { isPanelExpanded = !isPanelExpanded }
                    .padding(vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(modifier = Modifier.width(36.dp).height(4.dp)
                    .background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(2.dp)))
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════
//  TOOL PANELS — Each tool's options
// ═══════════════════════════════════════════════════════════════

@Composable
private fun LayersPanel() {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("LAYERS", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = CyberCyan)
        val layers = listOf("🎬 Video Layer 1" to true, "🎬 Video Layer 2" to false, "🔊 Audio Track" to true, "📝 Text Layer" to false, "⭐ Sticker Layer" to false)
        layers.forEach { (name, active) ->
            Row(modifier = Modifier.fillMaxWidth().background(if (active) CyberCyan.copy(alpha = 0.1f) else Color.White.copy(alpha = 0.03f), RoundedCornerShape(6.dp)).padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(name, fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Medium)
                Text(if (active) "👁️ Visible" else "👁️‍🗨️ Hidden", fontSize = 8.sp, color = if (active) CyberCyan else Color.Gray)
            }
        }
    }
}

@Composable
private fun TrimPanel(project: VideoProject, onUpdateTrim: (Long, Long) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("TRIM & CUT", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = NeonOrange)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf("Auto Smart", "Manual", "Remove Silence").forEach { mode ->
                Box(modifier = Modifier.weight(1f).background(NeonOrange.copy(alpha = 0.12f), RoundedCornerShape(6.dp))
                    .clickable { }.padding(6.dp), contentAlignment = Alignment.Center) {
                    Text(mode, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = NeonOrange, textAlign = TextAlign.Center)
                }
            }
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Box(modifier = Modifier.weight(1f).background(Color.White.copy(alpha = 0.04f), RoundedCornerShape(6.dp))
                .clickable { onUpdateTrim((project.trimStartMs + 500).coerceAtMost(project.trimEndMs - 500), project.trimEndMs) }
                .padding(6.dp), contentAlignment = Alignment.Center) {
                Text("START +0.5s", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = NeonOrange)
            }
            Box(modifier = Modifier.weight(1f).background(Color.White.copy(alpha = 0.04f), RoundedCornerShape(6.dp))
                .clickable { onUpdateTrim(project.trimStartMs, (project.trimEndMs - 500).coerceAtLeast(project.trimStartMs + 500)) }
                .padding(6.dp), contentAlignment = Alignment.Center) {
                Text("END -0.5s", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = NeonOrange)
            }
            Box(modifier = Modifier.weight(1f).background(Color.White.copy(alpha = 0.04f), RoundedCornerShape(6.dp))
                .clickable { onUpdateTrim(0, project.durationMs) }
                .padding(6.dp), contentAlignment = Alignment.Center) {
                Text("RESET", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = CyberCyan)
            }
        }
    }
}

@Composable
private fun SplitPanel(context: android.content.Context) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("SPLIT CLIP", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = CyberCyan)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf("Split at Playhead", "Split in Half", "Split by Scenes", "Auto Split Beats").forEach { mode ->
                Box(modifier = Modifier.weight(1f).background(CyberCyan.copy(alpha = 0.12f), RoundedCornerShape(6.dp))
                    .clickable { android.widget.Toast.makeText(context, "$mode activated!", android.widget.Toast.LENGTH_SHORT).show() }
                    .padding(6.dp), contentAlignment = Alignment.Center) {
                    Text(mode, fontSize = 8.sp, fontWeight = FontWeight.Bold, color = CyberCyan, textAlign = TextAlign.Center)
                }
            }
        }
    }
}

@Composable
private fun SpeedPanel(project: VideoProject, onUpdateSpeed: (Float) -> Unit, onUpdateSpeedCurve: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("SPEED CONTROL", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = NeonOrange)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            listOf(0.25f, 0.5f, 0.75f, 1.0f, 1.5f, 2.0f, 3.0f, 4.0f).forEach { sp ->
                val isSel = project.speedFactor == sp
                Box(modifier = Modifier.weight(1f).background(if (isSel) NeonOrange.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.04f), RoundedCornerShape(6.dp))
                    .clickable { onUpdateSpeed(sp) }.padding(4.dp), contentAlignment = Alignment.Center) {
                    Text("${sp}x", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = if (isSel) NeonOrange else Color.White)
                }
            }
        }
        Text("SPEED CURVES", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            listOf("Standard", "Montage", "Hero", "Flash", "Epic", "Custom").forEach { crv ->
                val isSel = project.speedCurve.lowercase() == crv.lowercase()
                Box(modifier = Modifier.weight(1f).background(if (isSel) CyberCyan.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.03f), RoundedCornerShape(6.dp))
                    .clickable { onUpdateSpeedCurve(crv) }.padding(4.dp), contentAlignment = Alignment.Center) {
                    Text(crv, fontSize = 8.sp, fontWeight = FontWeight.Bold, color = if (isSel) CyberCyan else Color.White)
                }
            }
        }
    }
}

@Composable
private fun CropPanel(project: VideoProject, onUpdateCropPreset: (String) -> Unit, onUpdateAspectPreset: (String) -> Unit,
                      onUpdateRotation: () -> Unit, onToggleH: () -> Unit, onToggleV: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("CROP & TRANSFORM", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = CyberCyan)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            listOf("Free", "1:1", "16:9", "9:16", "4:5", "4:3").forEach { cr ->
                val isSel = project.cropPreset == cr
                Box(modifier = Modifier.weight(1f).background(if (isSel) CyberCyan.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.04f), RoundedCornerShape(6.dp))
                    .clickable { onUpdateCropPreset(cr); onUpdateAspectPreset(cr) }.padding(4.dp), contentAlignment = Alignment.Center) {
                    Text(cr, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = if (isSel) CyberCyan else Color.White)
                }
            }
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf("↻ Rotate" to onUpdateRotation, "↔ Flip H" to onToggleH, "↕ Flip V" to onToggleV).forEach { (label, action) ->
                Box(modifier = Modifier.weight(1f).background(Color.White.copy(alpha = 0.04f), RoundedCornerShape(6.dp))
                    .clickable { action() }.padding(6.dp), contentAlignment = Alignment.Center) {
                    Text(label, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
    }
}

@Composable
private fun AudioPanel(project: VideoProject, onUpdateVideoVol: (Float) -> Unit, onUpdateMusicVol: (Float) -> Unit,
                       onToggleMute: () -> Unit, musicPicker: androidx.activity.result.ActivityResultLauncher<String>,
                       onUpdateVis: (String) -> Unit, onToggleBeat: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("AUDIO STUDIO", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = CyberCyan)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Column(modifier = Modifier.weight(1f)) {
                Text("VIDEO VOL", fontSize = 7.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Slider(value = project.videoVolume, onValueChange = onUpdateVideoVol, valueRange = 0f..1f,
                    colors = SliderDefaults.colors(activeTrackColor = NeonOrange, thumbColor = NeonOrange), modifier = Modifier.height(20.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text("BGM VOL", fontSize = 7.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Slider(value = project.backgroundMusicVolume, onValueChange = onUpdateMusicVol, valueRange = 0f..1f,
                    colors = SliderDefaults.colors(activeTrackColor = CyberCyan, thumbColor = CyberCyan), modifier = Modifier.height(20.dp))
            }
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Box(modifier = Modifier.weight(1f).background(if (project.isMuted) NeonOrange.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.04f), RoundedCornerShape(6.dp))
                .clickable { onToggleMute() }.padding(6.dp), contentAlignment = Alignment.Center) {
                Text(if (project.isMuted) "UNMUTE" else "MUTE", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = if (project.isMuted) NeonOrange else Color.White)
            }
            Box(modifier = Modifier.weight(1f).background(CyberCyan.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                .clickable { musicPicker.launch("audio/*") }.padding(6.dp), contentAlignment = Alignment.Center) {
                Text("+ ADD BGM", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = CyberCyan)
            }
            Box(modifier = Modifier.weight(1f).background(if (project.isBeatSyncEnabled) CyberCyan.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.04f), RoundedCornerShape(6.dp))
                .clickable { onToggleBeat() }.padding(6.dp), contentAlignment = Alignment.Center) {
                Text("BEAT SYNC", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = if (project.isBeatSyncEnabled) CyberCyan else Color.White)
            }
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("VISUALIZER:", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
            listOf("Wave", "Bars", "Radial").forEach { v ->
                val isSel = project.visualizerStyle.lowercase() == v.lowercase()
                Box(modifier = Modifier.background(if (isSel) CyberCyan.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.04f), RoundedCornerShape(6.dp))
                    .clickable { onUpdateVis(v) }.padding(horizontal = 6.dp, vertical = 4.dp)) {
                    Text(v, fontSize = 8.sp, fontWeight = FontWeight.Bold, color = if (isSel) CyberCyan else Color.White)
                }
            }
        }
    }
}

@Composable
private fun TextPanel(project: VideoProject, onUpdateText: (String?) -> Unit, onUpdateAnim: (String) -> Unit) {
    var textInput by remember { mutableStateOf(project.activeTextOverlay ?: "") }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("TEXT & SUBTITLES", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = NeonOrange)
        OutlinedTextField(value = textInput, onValueChange = { textInput = it; onUpdateText(if (it.isBlank()) null else it) },
            placeholder = { Text("Enter subtitle text...", fontSize = 9.sp, color = Color.Gray) },
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = NeonOrange, unfocusedBorderColor = Color.White.copy(alpha = 0.1f),
                focusedTextColor = Color.White, unfocusedTextColor = Color.White),
            modifier = Modifier.fillMaxWidth().height(40.dp), shape = RoundedCornerShape(8.dp))
        Text("ANIMATION:", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            listOf("None", "Fade", "Typewriter", "Bounce", "Zoom", "Slide", "Wave", "Glitch").forEach { anim ->
                val isSel = project.textAnimationType.lowercase() == anim.lowercase()
                Box(modifier = Modifier.weight(1f).background(if (isSel) NeonOrange.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.04f), RoundedCornerShape(6.dp))
                    .clickable { onUpdateAnim(anim) }.padding(4.dp), contentAlignment = Alignment.Center) {
                    Text(anim, fontSize = 7.sp, fontWeight = FontWeight.Bold, color = if (isSel) NeonOrange else Color.White)
                }
            }
        }
    }
}

@Composable
private fun FiltersPanel(project: VideoProject, onUpdateFilter: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("COLOR FILTERS", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = CyberCyan)
        val filters = listOf("none" to "Original", "grayscale" to "B&W", "sepia" to "Sepia", "invert" to "Invert",
            "vivid" to "Vivid", "cinematic" to "Cinema", "vintage" to "Vintage", "noir" to "Noir",
            "warm" to "Warm", "cool" to "Cool", "dreamy" to "Dreamy", "moody" to "Moody")
        FlowRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            filters.forEach { (id, name) ->
                val isSel = project.selectedFilter.lowercase() == id
                Box(modifier = Modifier.background(if (isSel) CyberCyan.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.04f), RoundedCornerShape(6.dp))
                    .clickable { onUpdateFilter(id) }.padding(horizontal = 8.dp, vertical = 5.dp)) {
                    Text(name, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = if (isSel) CyberCyan else Color.White)
                }
            }
        }
    }
}

@Composable
private fun EffectsPanel() {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("VISUAL EFFECTS", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = NeonOrange)
        val effects = listOf("🎬 Glitch", "📼 VHS", "🔮 Chromatic", "☀️ Lens Flare", "❄️ Snow", "🌧️ Rain", "🔥 Fire", "✨ Sparkle",
            "💨 Motion Blur", "🔍 Zoom Pulse", "📳 Shake", "⚡ Flash", "💡 Strobe", "💜 Neon Glow", "🔲 Vignette")
        FlowRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            effects.forEach { fx ->
                Box(modifier = Modifier.background(Color.White.copy(alpha = 0.04f), RoundedCornerShape(6.dp))
                    .clickable { }.padding(horizontal = 8.dp, vertical = 5.dp)) {
                    Text(fx, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
    }
}

@Composable
private fun StickersPanel(project: VideoProject, onUpdateSticker: (String) -> Unit, onUpdateMask: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("STICKERS & MASKS", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = CyberCyan)
        val stickers = listOf("None", "🔥 Fire", "⭐ Star", "❤️ Heart", "⚡ Glow", "💎 Diamond", "🎵 Music", "👑 Crown", "🏆 Trophy", "💯 100%", "🎉 Party", "😎 Cool")
        FlowRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            stickers.forEach { s ->
                val isSel = project.stickerType.lowercase() == s.lowercase() || (project.stickerType == "none" && s == "None")
                Box(modifier = Modifier.background(if (isSel) CyberCyan.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.04f), RoundedCornerShape(6.dp))
                    .clickable { onUpdateSticker(if (s == "None") "none" else s) }.padding(horizontal = 8.dp, vertical = 5.dp)) {
                    Text(s, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = if (isSel) CyberCyan else Color.White)
                }
            }
        }
        Text("3D MASKS:", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            items(listOf("none", "circle", "heart", "star", "hexagon", "vignette", "diamond", "triangle")) { mask ->
                val isSel = project.active3DShapeMask == mask
                Box(modifier = Modifier.background(if (isSel) NeonOrange.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.04f), RoundedCornerShape(6.dp))
                    .clickable { onUpdateMask(mask) }.padding(horizontal = 8.dp, vertical = 5.dp)) {
                    Text(mask.uppercase(), fontSize = 8.sp, fontWeight = FontWeight.Bold, color = if (isSel) NeonOrange else Color.White)
                }
            }
        }
    }
}

@Composable
private fun TransitionsPanel(project: VideoProject, onUpdateTransition: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("TRANSITIONS", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = NeonOrange)
        val transitions = listOf("None", "Crossfade", "Glitch", "Zoom", "Spin", "Flip", "Cube", "Wipe", "Dissolve",
            "Blur", "Pixelate", "Mosaic", "Split", "Film Burn", "Light Leak", "Smoke", "Circle", "Diamond", "Heart")
        FlowRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            transitions.forEach { tr ->
                val isSel = project.transitionType.lowercase() == tr.lowercase()
                Box(modifier = Modifier.background(if (isSel) NeonOrange.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.04f), RoundedCornerShape(6.dp))
                    .clickable { onUpdateTransition(tr) }.padding(horizontal = 8.dp, vertical = 5.dp)) {
                    Text(tr, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = if (isSel) NeonOrange else Color.White)
                }
            }
        }
    }
}
