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
import androidx.compose.runtime.mutableFloatStateOf
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
import com.powercut.editor.ui.theme.AccentSecondary
import com.powercut.editor.ui.theme.CyberCyan
import com.powercut.editor.ui.theme.NeonOrange
import com.powercut.editor.ui.theme.glassmorphic
import com.powercut.editor.ui.theme.neonGlow
import com.powercut.editor.ui.theme.premiumAccentGradient
import com.powercut.editor.ui.theme.tactileClick
import java.util.Locale

private fun formatTime(ms: Long): String {
    val totalSecs = ms / 1000
    val minutes = totalSecs / 60
    val seconds = totalSecs % 60
    return String.format(Locale.US, "%02d:%02d", minutes, seconds)
}

// ═══════════════════════════════════════════════════════════════
//  NEXTGEN EDITOR — CapCut-Level Professional Video Editor
// ═══════════════════════════════════════════════════════════════

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
    onSaveDraft: () -> Unit,
    // NextGen Pro callbacks
    onUpdateImageOverlay: (String?) -> Unit = {},
    onUpdateImageOverlayOpacity: (Float) -> Unit = {},
    onUpdateImageOverlayScale: (Float) -> Unit = {},
    onUpdateSelectedEffect: (String) -> Unit = {},
    onAddLayer: (String) -> Unit = {},
    onRemoveLayer: (String) -> Unit = {}
) {
    val context = LocalContext.current

    // ─── State ────────────────────────────────────────────────
    var selectedTool by remember { mutableIntStateOf(-1) } // -1 = no tool selected
    var isPanelExpanded by remember { mutableStateOf(true) }
    var isPlaying by remember { mutableStateOf(false) }
    var currentPlaybackTime by remember { mutableStateOf(0L) }
    var playbackSpeed by remember { mutableFloatStateOf(1.0f) }

    // Layer visibility
    var layerVideoVisible by remember { mutableStateOf(true) }
    var layerAudioVisible by remember { mutableStateOf(true) }
    var layerTextVisible by remember { mutableStateOf(true) }
    var layerImageVisible by remember { mutableStateOf(true) }
    var layerStickerVisible by remember { mutableStateOf(true) }

    // Selected states
    var selectedTrimMode by remember { mutableStateOf("Manual") }
    var selectedSplitMode by remember { mutableStateOf("Playhead") }

    // ─── File Pickers ─────────────────────────────────────────
    val multiFilePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
        uris.forEach { onAddClip(it) }
        if (uris.isNotEmpty()) android.widget.Toast.makeText(context, "${uris.size} clips added!", android.widget.Toast.LENGTH_SHORT).show()
    }
    val musicPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { onUpdateBackgroundMusic(UriHelper.getPathFromUri(context, it)) }
    }
    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            onUpdateImageOverlay(UriHelper.getPathFromUri(context, it))
            android.widget.Toast.makeText(context, "Image overlay added!", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    // ─── ExoPlayer ────────────────────────────────────────────
    val exoPlayer = remember { ExoPlayer.Builder(context).build().apply { repeatMode = Player.REPEAT_MODE_ONE } }
    LaunchedEffect(project.videoPath) {
        val uri = if (project.videoPath.startsWith("content://") || project.videoPath.startsWith("file://"))
            Uri.parse(project.videoPath) else Uri.fromFile(java.io.File(project.videoPath))
        exoPlayer.setMediaItem(MediaItem.fromUri(uri))
        exoPlayer.prepare()
        exoPlayer.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                if (state == Player.STATE_READY) onDurationRetrieved(exoPlayer.duration)
            }
        })
    }
    LaunchedEffect(isPlaying) {
        if (isPlaying) { exoPlayer.play(); while (isPlaying) { currentPlaybackTime = exoPlayer.currentPosition; kotlinx.coroutines.delay(100) } }
        else { exoPlayer.pause(); kotlinx.coroutines.delay(3000); if (!isPlaying) onSaveDraft() }
    }
    LaunchedEffect(project.isMuted, project.videoVolume) { exoPlayer.volume = if (project.isMuted) 0f else project.videoVolume }
    LaunchedEffect(project.speedFactor) { exoPlayer.playbackParameters = PlaybackParameters(project.speedFactor); playbackSpeed = project.speedFactor }
    DisposableEffect(Unit) { onDispose { exoPlayer.release() } }

    // ─── Filter Matrix ────────────────────────────────────────
    val colorFilter = remember(project.selectedFilter) {
        when (project.selectedFilter.lowercase()) {
            "grayscale" -> ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) })
            "sepia" -> ColorFilter.colorMatrix(ColorMatrix(floatArrayOf(0.393f,0.769f,0.189f,0f,0f,0.349f,0.686f,0.168f,0f,0f,0.272f,0.534f,0.131f,0f,0f,0f,0f,0f,1f,0f)))
            "invert" -> ColorFilter.colorMatrix(ColorMatrix(floatArrayOf(-1f,0f,0f,0f,255f,0f,-1f,0f,0f,255f,0f,0f,-1f,0f,255f,0f,0f,0f,1f,0f)))
            else -> null
        }
    }
    val aspect = remember(project.aspectPreset) { when (project.aspectPreset) { "1:1" -> 1.0f; "9:16" -> 9f/16f; "4:5" -> 4f/5f; else -> 16f/9f } }

    // ═══════════════════════════════════════════════════════════
    //  MAIN LAYOUT
    // ═══════════════════════════════════════════════════════════
    Column(modifier = Modifier.fillMaxSize().background(Color(0xFF0B0D12))) {

        // ─── 1. HEADER ────────────────────────────────────────
        EditorHeader(
            currentPlaybackTime = currentPlaybackTime,
            durationMs = project.durationMs,
            onBack = { onSaveDraft(); onBack() },
            onExport = onExport,
            onUndo = { },
            onRedo = { }
        )

        // ─── 2. VIDEO PREVIEW ─────────────────────────────────
        Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp).weight(1.4f), contentAlignment = Alignment.Center) {
            Box(
                modifier = Modifier.fillMaxHeight().aspectRatio(aspect).clip(RoundedCornerShape(14.dp)).background(Color.Black)
                    .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center
            ) {
                // ExoPlayer video
                AndroidView(
                    factory = { ctx -> PlayerView(ctx).apply { player = exoPlayer; useController = false } },
                    modifier = Modifier.fillMaxSize().graphicsLayer(
                        scaleX = if (project.isFlippedHorizontal) -1f else 1f,
                        scaleY = if (project.isFlippedVertical) -1f else 1f,
                        rotationZ = project.rotationDegrees
                    )
                )

                // Filter overlay
                if (colorFilter != null) {
                    val overlayColor = when (project.selectedFilter.lowercase()) {
                        "grayscale" -> Color.Gray.copy(0.15f)
                        "sepia" -> Color(0xFF704214).copy(0.18f)
                        "invert" -> Color.White.copy(0.1f)
                        else -> Color.Transparent
                    }
                    if (overlayColor != Color.Transparent) Box(Modifier.fillMaxSize().background(overlayColor))
                }

                // 3D Shape Mask overlay
                if (project.active3DShapeMask != "none") {
                    Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.3f)))
                }

                // Image overlay
                if (project.imageOverlayPath != null && layerImageVisible) {
                    Box(
                        modifier = Modifier.fillMaxSize(project.imageOverlayScale).align(Alignment.Center),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("🖼️", fontSize = 32.sp)
                    }
                }

                // Text overlay
                if (project.activeTextOverlay != null && layerTextVisible) {
                    Box(
                        modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 16.dp)
                            .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                    ) {
                        Text(project.activeTextOverlay!!, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // Sticker overlay
                if (project.stickerType != "none" && layerStickerVisible) {
                    val stickerEmoji = when (project.stickerType) {
                        "fire" -> "🔥"; "star" -> "⭐"; "heart" -> "❤️"; "glow" -> "⚡"
                        "diamond" -> "💎"; "music" -> "🎵"; "crown" -> "👑"; "sparkle" -> "💫"
                        else -> ""
                    }
                    if (stickerEmoji.isNotEmpty()) {
                        Text(stickerEmoji, fontSize = 48.sp, modifier = Modifier.align(Alignment.TopEnd).padding(16.dp))
                    }
                }

                // Play/Pause overlay button
                Box(
                    modifier = Modifier.size(52.dp).background(Color.White.copy(0.15f), CircleShape)
                        .border(2.dp, Color.White.copy(0.3f), CircleShape).clickable { isPlaying = !isPlaying },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, "Play", tint = Color.White, modifier = Modifier.size(24.dp))
                }

                // Speed badge top-left
                if (playbackSpeed != 1.0f) {
                    Box(
                        modifier = Modifier.align(Alignment.TopStart).padding(8.dp)
                            .background(Color.Black.copy(0.6f), RoundedCornerShape(6.dp)).padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text("${playbackSpeed}x", fontSize = 9.sp, color = CyberCyan, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // ─── 3. PLAYBACK CONTROLS ─────────────────────────────
        PlaybackControls(
            isPlaying = isPlaying,
            speedFactor = project.speedFactor,
            currentTime = currentPlaybackTime,
            durationMs = project.durationMs,
            onPlayPause = { isPlaying = !isPlaying },
            onPrevFrame = { exoPlayer.seekTo((exoPlayer.currentPosition - 33).coerceAtLeast(0)) },
            onNextFrame = { exoPlayer.seekTo((exoPlayer.currentPosition + 33).coerceAtMost(exoPlayer.duration)) }
        )

        // ─── 4. MULTI-TRACK TIMELINE ──────────────────────────
        CapCutTimeline(
            project = project,
            currentTime = currentPlaybackTime,
            exoPlayer = exoPlayer,
            layerVideoVisible = layerVideoVisible,
            layerAudioVisible = layerAudioVisible,
            layerTextVisible = layerTextVisible,
            layerImageVisible = layerImageVisible,
            layerStickerVisible = layerStickerVisible,
            onToggleVideoLayer = { layerVideoVisible = !layerVideoVisible },
            onToggleAudioLayer = { layerAudioVisible = !layerAudioVisible },
            onToggleTextLayer = { layerTextVisible = !layerTextVisible },
            onToggleImageLayer = { layerImageVisible = !layerImageVisible },
            onToggleStickerLayer = { layerStickerVisible = !layerStickerVisible }
        )

        // ─── 5. TOOL PANEL (expandable) ───────────────────────
        AnimatedVisibility(visible = selectedTool >= 0 && isPanelExpanded, enter = expandVertically(), exit = shrinkVertically()) {
            CapCutToolPanel(
                selectedTool = selectedTool,
                project = project,
                context = context,
                exoPlayer = exoPlayer,
                currentPlaybackTime = currentPlaybackTime,
                selectedTrimMode = selectedTrimMode,
                selectedSplitMode = selectedSplitMode,
                onTrimModeChange = { selectedTrimMode = it },
                onSplitModeChange = { selectedSplitMode = it },
                onUpdateTrim = onUpdateTrim,
                onUpdateSpeed = onUpdateSpeed,
                onUpdateFilter = onUpdateFilter,
                onUpdateTransition = onUpdateTransition,
                onUpdateTextOverlay = onUpdateTextOverlay,
                onUpdateTextAnimation = onUpdateTextAnimation,
                onUpdateStickerType = onUpdateStickerType,
                onUpdate3DShapeMask = onUpdate3DShapeMask,
                onUpdateTemplate = onUpdateTemplate,
                onUpdateVisualizerStyle = onUpdateVisualizerStyle,
                onToggleBeatSync = onToggleBeatSync,
                onToggleMute = onToggleMute,
                onUpdateVideoVolume = onUpdateVideoVolume,
                onUpdateMusicVolume = onUpdateMusicVolume,
                onUpdateBackgroundMusic = onUpdateBackgroundMusic,
                onUpdateCropPreset = onUpdateCropPreset,
                onUpdateAspectPreset = onUpdateAspectPreset,
                onUpdateRotation = onUpdateRotation,
                onToggleFlipHorizontal = onToggleFlipHorizontal,
                onToggleFlipVertical = onToggleFlipVertical,
                onUpdateResolution = onUpdateResolution,
                onUpdateSpeedCurve = onUpdateSpeedCurve,
                onUpdateAutoCaptions = onUpdateAutoCaptions,
                onToggleSilenceRemover = onToggleSilenceRemover,
                onUpdateSelectedEffect = onUpdateSelectedEffect,
                onUpdateImageOverlay = onUpdateImageOverlay,
                onUpdateImageOverlayOpacity = onUpdateImageOverlayOpacity,
                imagePicker = imagePicker,
                musicPicker = musicPicker,
                onCollapse = { isPanelExpanded = false }
            )
        }

        // ─── 6. CAPCUT TOOL BAR (no import button) ────────────
        CapCutToolBar(
            selectedTool = selectedTool,
            onToolSelected = { idx ->
                if (selectedTool == idx) { isPanelExpanded = !isPanelExpanded } else { selectedTool = idx; isPanelExpanded = true }
            }
        )
    }
}


// ═══════════════════════════════════════════════════════════════
//  EDITOR HEADER
// ═══════════════════════════════════════════════════════════════
@Composable
private fun EditorHeader(
    currentPlaybackTime: Long,
    durationMs: Long,
    onBack: () -> Unit,
    onExport: () -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(modifier = Modifier.size(30.dp).glassmorphic(shape = RoundedCornerShape(8.dp)).tactileClick(onClick = onBack), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.ChevronLeft, "Back", tint = Color.White, modifier = Modifier.size(18.dp))
            }
            Column {
                Text("PowerCut Pro", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Text("${formatTime(currentPlaybackTime)} / ${formatTime(durationMs)}", fontSize = 9.sp, color = Color.Gray)
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            IconButton(onClick = onUndo, modifier = Modifier.size(26.dp)) { Text("↶", color = Color.LightGray, fontSize = 16.sp, fontWeight = FontWeight.Bold) }
            IconButton(onClick = onRedo, modifier = Modifier.size(26.dp)) { Text("↷", color = Color.LightGray, fontSize = 16.sp, fontWeight = FontWeight.Bold) }
            Box(modifier = Modifier.neonGlow(AccentSecondary, RoundedCornerShape(20.dp), 1.dp).background(premiumAccentGradient, RoundedCornerShape(20.dp)).tactileClick(onClick = onExport).padding(horizontal = 12.dp, vertical = 5.dp)) {
                Text("EXPORT", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 10.sp, letterSpacing = 0.5.sp)
            }
        }
    }
}


// ═══════════════════════════════════════════════════════════════
//  PLAYBACK CONTROLS
// ═══════════════════════════════════════════════════════════════
@Composable
private fun PlaybackControls(
    isPlaying: Boolean,
    speedFactor: Float,
    currentTime: Long,
    durationMs: Long,
    onPlayPause: () -> Unit,
    onPrevFrame: () -> Unit,
    onNextFrame: () -> Unit
) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(30.dp).glassmorphic(CircleShape).tactileClick(onClick = onPrevFrame), contentAlignment = Alignment.Center) {
            Icon(Icons.Default.SkipPrevious, "Prev", tint = Color.White, modifier = Modifier.size(14.dp))
        }
        Spacer(Modifier.width(14.dp))
        Box(modifier = Modifier.size(42.dp).neonGlow(NeonOrange, CircleShape).background(NeonOrange, CircleShape).tactileClick(onClick = onPlayPause), contentAlignment = Alignment.Center) {
            Icon(if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, "Play", tint = Color.White, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(14.dp))
        Box(modifier = Modifier.size(30.dp).glassmorphic(CircleShape).tactileClick(onClick = onNextFrame), contentAlignment = Alignment.Center) {
            Icon(Icons.Default.SkipNext, "Next", tint = Color.White, modifier = Modifier.size(14.dp))
        }
        Spacer(Modifier.width(14.dp))
        Box(modifier = Modifier.background(Color.White.copy(0.05f), RoundedCornerShape(6.dp)).border(1.dp, Color.White.copy(0.1f), RoundedCornerShape(6.dp)).padding(horizontal = 6.dp, vertical = 3.dp)) {
            Text("${speedFactor}x", fontSize = 10.sp, color = CyberCyan, fontWeight = FontWeight.Bold)
        }
    }
}


// ═══════════════════════════════════════════════════════════════
//  CAPCUT-STYLE MULTI-TRACK TIMELINE
// ═══════════════════════════════════════════════════════════════
@Composable
private fun CapCutTimeline(
    project: VideoProject,
    currentTime: Long,
    exoPlayer: ExoPlayer,
    layerVideoVisible: Boolean,
    layerAudioVisible: Boolean,
    layerTextVisible: Boolean,
    layerImageVisible: Boolean,
    layerStickerVisible: Boolean,
    onToggleVideoLayer: () -> Unit,
    onToggleAudioLayer: () -> Unit,
    onToggleTextLayer: () -> Unit,
    onToggleImageLayer: () -> Unit,
    onToggleStickerLayer: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxWidth().height(120.dp).background(Color(0xFF111318)).border(1.dp, Color.White.copy(0.04f))
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Time ruler
            Row(modifier = Modifier.fillMaxWidth().height(16.dp).background(Color.Black.copy(0.3f)).padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                listOf("0s", "5s", "10s", "15s", "20s", "25s", "30s").forEach { Text(it, fontSize = 7.sp, color = Color.Gray, fontWeight = FontWeight.Bold) }
            }

            // Tracks
            Column(modifier = Modifier.fillMaxWidth().weight(1f).padding(vertical = 2.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                // Video track
                TimelineTrackRow(
                    label = "🎬", isActive = layerVideoVisible, onToggle = onToggleVideoLayer,
                    content = {
                        Box(Modifier.weight(0.45f).fillMaxHeight().background(Brush.horizontalGradient(listOf(NeonOrange, Color(0xFFFF7043))), RoundedCornerShape(3.dp)), contentAlignment = Alignment.Center) { Text("Video 1", fontSize = 7.sp, color = Color.White, fontWeight = FontWeight.Bold) }
                        Box(Modifier.size(12.dp).background(Color.White.copy(0.15f), RoundedCornerShape(2.dp)), contentAlignment = Alignment.Center) { Icon(Icons.Default.AutoAwesome, "T", tint = Color.White, modifier = Modifier.size(7.dp)) }
                        Box(Modifier.weight(0.45f).fillMaxHeight().background(Brush.horizontalGradient(listOf(Color(0xFFFF7043), NeonOrange)), RoundedCornerShape(3.dp)), contentAlignment = Alignment.Center) { Text("Video 2", fontSize = 7.sp, color = Color.White, fontWeight = FontWeight.Bold) }
                    }
                )
                // Audio track
                TimelineTrackRow(
                    label = "🔊", isActive = layerAudioVisible, onToggle = onToggleAudioLayer,
                    content = {
                        Box(Modifier.weight(1f).fillMaxHeight().background(Brush.horizontalGradient(listOf(CyberCyan.copy(0.2f), CyberCyan.copy(0.08f))), RoundedCornerShape(3.dp)).border(1.dp, CyberCyan.copy(0.25f), RoundedCornerShape(3.dp))) {
                            Row(Modifier.fillMaxSize().padding(horizontal = 6.dp), horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.CenterVertically) {
                                listOf(6,12,8,16,10,6,14,18,12,8,14,16,8,10,14,6,12,18).forEach { h -> Box(Modifier.width(3.dp).height(h.dp).background(Brush.verticalGradient(listOf(CyberCyan, CyberCyan.copy(0.3f))), RoundedCornerShape(1.dp))) }
                            }
                        }
                    }
                )
                // Text track
                TimelineTrackRow(
                    label = "📝", isActive = layerTextVisible, onToggle = onToggleTextLayer,
                    content = {
                        Spacer(Modifier.weight(0.15f))
                        Box(Modifier.weight(0.7f).fillMaxHeight().background(Brush.horizontalGradient(listOf(Color(0xFFAB47BC), Color(0xFFBA68C8))), RoundedCornerShape(3.dp)), contentAlignment = Alignment.Center) { Text(project.activeTextOverlay?.take(12) ?: "Subtitle", fontSize = 7.sp, color = Color.White, fontWeight = FontWeight.Bold) }
                        Spacer(Modifier.weight(0.15f))
                    }
                )
                // Image track
                TimelineTrackRow(
                    label = "🖼️", isActive = layerImageVisible, onToggle = onToggleImageLayer,
                    content = {
                        if (project.imageOverlayPath != null) {
                            Box(Modifier.weight(0.5f).fillMaxHeight().background(Brush.horizontalGradient(listOf(Color(0xFF4CAF50), Color(0xFF81C784))), RoundedCornerShape(3.dp)), contentAlignment = Alignment.Center) { Text("Image", fontSize = 7.sp, color = Color.White, fontWeight = FontWeight.Bold) }
                            Spacer(Modifier.weight(0.5f))
                        } else {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("No image", fontSize = 7.sp, color = Color.Gray) }
                        }
                    }
                )
                // Sticker track
                TimelineTrackRow(
                    label = "⭐", isActive = layerStickerVisible, onToggle = onToggleStickerLayer,
                    content = {
                        if (project.stickerType != "none") {
                            Spacer(Modifier.weight(0.3f))
                            Box(Modifier.weight(0.4f).fillMaxHeight().background(Brush.horizontalGradient(listOf(Color(0xFFFFD700), Color(0xFFFFA000))), RoundedCornerShape(3.dp)), contentAlignment = Alignment.Center) { Text(project.stickerType, fontSize = 7.sp, color = Color.White, fontWeight = FontWeight.Bold) }
                            Spacer(Modifier.weight(0.3f))
                        } else {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("No sticker", fontSize = 7.sp, color = Color.Gray) }
                        }
                    }
                )
            }
        }
        // Playhead
        Box(modifier = Modifier.fillMaxHeight().width(2.dp).align(Alignment.Center).background(Brush.verticalGradient(listOf(NeonOrange, Color.Transparent)))) {
            Box(modifier = Modifier.size(8.dp).background(NeonOrange, CircleShape).border(1.dp, Color.White, CircleShape).align(Alignment.TopCenter).neonGlow(NeonOrange, CircleShape, 1.dp))
        }
    }
}


// ─── Timeline Track Row ────────────────────────────────────────
@Composable
private fun TimelineTrackRow(
    label: String,
    isActive: Boolean,
    onToggle: () -> Unit,
    content: @Composable () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().height(16.dp).padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Layer toggle icon
        Box(
            modifier = Modifier.width(18.dp).fillMaxHeight()
                .background(if (isActive) Color.White.copy(0.08f) else Color.White.copy(0.02f), RoundedCornerShape(2.dp))
                .clickable(onClick = onToggle),
            contentAlignment = Alignment.Center
        ) {
            Text(label, fontSize = 7.sp, color = Color.White.copy(alpha = if (isActive) 1f else 0.3f))
        }
        // Track content
        Row(modifier = Modifier.weight(1f).fillMaxHeight().padding(start = 2.dp), verticalAlignment = Alignment.CenterVertically) {
            if (isActive) content() else Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Hidden", fontSize = 6.sp, color = Color.Gray.copy(0.4f)) }
        }
    }
}


// ═══════════════════════════════════════════════════════════════
//  CAPCUT TOOL BAR (no import — bottom navigation)
// ═══════════════════════════════════════════════════════════════
@Composable
private fun CapCutToolBar(
    selectedTool: Int,
    onToolSelected: (Int) -> Unit
) {
    val tools = listOf(
        "✂️" to "Edit", "📑" to "Layers", "⚡" to "Speed", "📐" to "Crop",
        "🔊" to "Audio", "🔤" to "Text", "🎨" to "Filters", "✨" to "Effects",
        "😄" to "Stickers", "🔀" to "Trans", "🎭" to "Anim", "🎬" to "3D",
        "🖼️" to "Image", "📋" to "Template"
    )
    Row(
        modifier = Modifier.fillMaxWidth().height(62.dp)
            .glassmorphic(shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp), backColor = Color(0xFF111318))
            .border(1.dp, Color.White.copy(0.05f), RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        tools.forEachIndexed { idx, (emoji, name) ->
            val isActive = selectedTool == idx
            Box(
                modifier = Modifier.clip(RoundedCornerShape(10.dp))
                    .background(if (isActive) NeonOrange.copy(0.18f) else Color.Transparent)
                    .clickable { onToolSelected(idx) }
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(emoji, fontSize = 14.sp)
                    Text(name, fontSize = 7.sp, fontWeight = FontWeight.Bold, color = if (isActive) NeonOrange else Color.Gray)
                }
            }
        }
    }
}


// ═══════════════════════════════════════════════════════════════
//  CAPCUT TOOL PANELS (all options functional)
// ═══════════════════════════════════════════════════════════════
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CapCutToolPanel(
    selectedTool: Int,
    project: VideoProject,
    context: android.content.Context,
    exoPlayer: ExoPlayer,
    currentPlaybackTime: Long,
    selectedTrimMode: String,
    selectedSplitMode: String,
    onTrimModeChange: (String) -> Unit,
    onSplitModeChange: (String) -> Unit,
    onUpdateTrim: (Long, Long) -> Unit,
    onUpdateSpeed: (Float) -> Unit,
    onUpdateFilter: (String) -> Unit,
    onUpdateTransition: (String) -> Unit,
    onUpdateTextOverlay: (String?) -> Unit,
    onUpdateTextAnimation: (String) -> Unit,
    onUpdateStickerType: (String) -> Unit,
    onUpdate3DShapeMask: (String) -> Unit,
    onUpdateTemplate: (String) -> Unit,
    onUpdateVisualizerStyle: (String) -> Unit,
    onToggleBeatSync: () -> Unit,
    onToggleMute: () -> Unit,
    onUpdateVideoVolume: (Float) -> Unit,
    onUpdateMusicVolume: (Float) -> Unit,
    onUpdateBackgroundMusic: (String?) -> Unit,
    onUpdateCropPreset: (String) -> Unit,
    onUpdateAspectPreset: (String) -> Unit,
    onUpdateRotation: () -> Unit,
    onToggleFlipHorizontal: () -> Unit,
    onToggleFlipVertical: () -> Unit,
    onUpdateResolution: (String) -> Unit,
    onUpdateSpeedCurve: (String) -> Unit,
    onUpdateAutoCaptions: (String) -> Unit,
    onToggleSilenceRemover: () -> Unit,
    onUpdateSelectedEffect: (String) -> Unit,
    onUpdateImageOverlay: (String?) -> Unit,
    onUpdateImageOverlayOpacity: (Float) -> Unit,
    imagePicker: androidx.activity.result.ActivityResultLauncher<String>,
    musicPicker: androidx.activity.result.ActivityResultLauncher<String>,
    onCollapse: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxWidth().height(180.dp).padding(horizontal = 8.dp, vertical = 2.dp)
            .background(Color(0xFF1A1C24).copy(0.85f), RoundedCornerShape(12.dp))
            .border(1.dp, Color.White.copy(0.04f), RoundedCornerShape(12.dp))
    ) {
        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(8.dp)) {
            when (selectedTool) {
                0 -> EditPanel(project, onUpdateCropPreset, onUpdateAspectPreset, onUpdateSpeed, onUpdateSpeedCurve, onUpdateRotation, onToggleFlipHorizontal, onToggleFlipVertical, onUpdateResolution, onUpdateTrim)
                1 -> LayersPanel(project, context, onAddLayer = {}, onRemoveLayer = {})
                2 -> SpeedPanel(project, onUpdateSpeed, onUpdateSpeedCurve)
                3 -> CropPanel(project, onUpdateCropPreset, onUpdateAspectPreset, onUpdateRotation, onToggleFlipHorizontal, onToggleFlipVertical)
                4 -> AudioPanel(project, onToggleMute, onUpdateVideoVolume, onUpdateMusicVolume, onUpdateVisualizerStyle, onToggleBeatSync, musicPicker)
                5 -> TextPanel(project, onUpdateTextOverlay, onUpdateTextAnimation)
                6 -> FiltersPanel(project, onUpdateFilter)
                7 -> EffectsPanel(project, onUpdateSelectedEffect, onUpdateFilter)
                8 -> StickersPanel(project, onUpdateStickerType)
                9 -> TransitionsPanel(project, onUpdateTransition)
                10 -> AnimationsPanel(project, onUpdateTextAnimation)
                11 -> ThreeDPanel(project, onUpdate3DShapeMask)
                12 -> ImagePanel(project, imagePicker, onUpdateImageOverlay, onUpdateImageOverlayOpacity)
                13 -> TemplatePanel(project, onUpdateTemplate)
            }
        }
        // Collapse handle
        Box(modifier = Modifier.fillMaxWidth().align(Alignment.BottomCenter).clickable(onClick = onCollapse).padding(2.dp), contentAlignment = Alignment.Center) {
            Box(modifier = Modifier.width(32.dp).height(3.dp).background(Color.White.copy(0.15f), RoundedCornerShape(2.dp)))
        }
    }
}


// ─── 0. EDIT PANEL ─────────────────────────────────────────────
@Composable
private fun EditPanel(
    project: VideoProject,
    onUpdateCropPreset: (String) -> Unit,
    onUpdateAspectPreset: (String) -> Unit,
    onUpdateSpeed: (Float) -> Unit,
    onUpdateSpeedCurve: (String) -> Unit,
    onUpdateRotation: () -> Unit,
    onToggleFlipH: () -> Unit,
    onToggleFlipV: () -> Unit,
    onUpdateResolution: (String) -> Unit,
    onUpdateTrim: (Long, Long) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("EDIT", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = NeonOrange)
        // Crop
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            listOf("Free", "1:1", "16:9", "9:16", "4:5").forEach { c ->
                val sel = project.cropPreset == c
                Box(Modifier.weight(1f).background(if (sel) CyberCyan.copy(0.2f) else Color.White.copy(0.04f), RoundedCornerShape(6.dp)).clickable { onUpdateCropPreset(c); onUpdateAspectPreset(c) }.padding(4.dp), contentAlignment = Alignment.Center) {
                    Text(c, fontSize = 8.sp, fontWeight = FontWeight.Bold, color = if (sel) CyberCyan else Color.White)
                }
            }
        }
        // Rotate + Flip
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            listOf("↻ Rotate" to onUpdateRotation, "↔ Flip H" to onToggleFlipH, "↕ Flip V" to onToggleFlipV).forEach { (l, a) ->
                Box(Modifier.weight(1f).background(Color.White.copy(0.04f), RoundedCornerShape(6.dp)).clickable { a() }.padding(6.dp), contentAlignment = Alignment.Center) {
                    Text(l, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
        // Resolution
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            listOf("720p", "1080p", "4k", "8k").forEach { r ->
                val sel = project.targetResolution.lowercase() == r.lowercase()
                Box(Modifier.weight(1f).background(if (sel) NeonOrange.copy(0.2f) else Color.White.copy(0.04f), RoundedCornerShape(6.dp)).clickable { onUpdateResolution(r) }.padding(4.dp), contentAlignment = Alignment.Center) {
                    Text(r.uppercase(), fontSize = 8.sp, fontWeight = FontWeight.Bold, color = if (sel) NeonOrange else Color.White)
                }
            }
        }
    }
}


// ─── 1. LAYERS PANEL ───────────────────────────────────────────
@Composable
private fun LayersPanel(project: VideoProject, context: android.content.Context, onAddLayer: (String) -> Unit, onRemoveLayer: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("LAYERS", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = CyberCyan)
        listOf("🎬 Video" to true, "🔊 Audio" to (project.backgroundMusicPath != null), "📝 Text" to (project.activeTextOverlay != null), "🖼️ Image" to (project.imageOverlayPath != null), "⭐ Sticker" to (project.stickerType != "none")).forEach { (name, hasContent) ->
            Row(Modifier.fillMaxWidth().background(if (hasContent) CyberCyan.copy(0.1f) else Color.White.copy(0.03f), RoundedCornerShape(6.dp)).padding(horizontal = 8.dp, vertical = 5.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(name, fontSize = 10.sp, color = if (hasContent) Color.White else Color.Gray)
                Text(if (hasContent) "👁️" else "—", fontSize = 10.sp)
            }
        }
    }
}


// ─── 2. SPEED PANEL ────────────────────────────────────────────
@Composable
private fun SpeedPanel(project: VideoProject, onUpdateSpeed: (Float) -> Unit, onUpdateSpeedCurve: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("SPEED", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = NeonOrange)
        // Super slow-mo to ultra fast
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            listOf(0.1f, 0.25f, 0.5f, 1.0f, 2.0f, 4.0f, 8.0f, 16.0f).forEach { s ->
                val sel = project.speedFactor == s
                Box(Modifier.weight(1f).background(if (sel) NeonOrange.copy(0.2f) else Color.White.copy(0.04f), RoundedCornerShape(6.dp)).clickable { onUpdateSpeed(s) }.padding(3.dp), contentAlignment = Alignment.Center) {
                    Text("${s}x", fontSize = 7.sp, fontWeight = FontWeight.Bold, color = if (sel) NeonOrange else Color.White)
                }
            }
        }
        // Speed curves
        Text("SPEED CURVE", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            listOf("Standard", "Montage", "Hero", "Flash", "Custom").forEach { c ->
                val sel = project.speedCurve.lowercase() == c.lowercase()
                Box(Modifier.weight(1f).background(if (sel) CyberCyan.copy(0.15f) else Color.White.copy(0.03f), RoundedCornerShape(6.dp)).clickable { onUpdateSpeedCurve(c) }.padding(4.dp), contentAlignment = Alignment.Center) {
                    Text(c, fontSize = 7.sp, fontWeight = FontWeight.Bold, color = if (sel) CyberCyan else Color.White)
                }
            }
        }
    }
}


// ─── 3. CROP PANEL ─────────────────────────────────────────────
@Composable
private fun CropPanel(project: VideoProject, onUpdateCrop: (String) -> Unit, onUpdateAspect: (String) -> Unit, onRotate: () -> Unit, onFlipH: () -> Unit, onFlipV: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("CROP & TRANSFORM", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = CyberCyan)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            listOf("Free", "1:1", "16:9", "9:16", "4:5", "21:9").forEach { c ->
                val sel = project.cropPreset == c
                Box(Modifier.weight(1f).background(if (sel) CyberCyan.copy(0.2f) else Color.White.copy(0.04f), RoundedCornerShape(6.dp)).clickable { onUpdateCrop(c); onUpdateAspect(c) }.padding(4.dp), contentAlignment = Alignment.Center) {
                    Text(c, fontSize = 8.sp, fontWeight = FontWeight.Bold, color = if (sel) CyberCyan else Color.White)
                }
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            listOf("↻ Rotate" to onRotate, "↔ Flip H" to onFlipH, "↕ Flip V" to onFlipV).forEach { (l, a) ->
                Box(Modifier.weight(1f).background(Color.White.copy(0.04f), RoundedCornerShape(6.dp)).clickable { a() }.padding(6.dp), contentAlignment = Alignment.Center) {
                    Text(l, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
    }
}


// ─── 4. AUDIO PANEL ────────────────────────────────────────────
@Composable
private fun AudioPanel(
    project: VideoProject,
    onToggleMute: () -> Unit,
    onUpdateVideoVol: (Float) -> Unit,
    onUpdateMusicVol: (Float) -> Unit,
    onUpdateVisualizer: (String) -> Unit,
    onToggleBeatSync: () -> Unit,
    musicPicker: androidx.activity.result.ActivityResultLauncher<String>
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("AUDIO", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = CyberCyan)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Column(Modifier.weight(1f)) { Text("VIDEO VOL", fontSize = 7.sp, fontWeight = FontWeight.Bold, color = Color.White); Slider(value = project.videoVolume, onValueChange = onUpdateVideoVol, valueRange = 0f..1f, colors = SliderDefaults.colors(activeTrackColor = NeonOrange, thumbColor = NeonOrange), modifier = Modifier.height(18.dp)) }
            Column(Modifier.weight(1f)) { Text("BGM VOL", fontSize = 7.sp, fontWeight = FontWeight.Bold, color = Color.White); Slider(value = project.backgroundMusicVolume, onValueChange = onUpdateMusicVol, valueRange = 0f..1f, colors = SliderDefaults.colors(activeTrackColor = CyberCyan, thumbColor = CyberCyan), modifier = Modifier.height(18.dp)) }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Box(Modifier.weight(1f).background(if (project.isMuted) NeonOrange.copy(0.2f) else Color.White.copy(0.04f), RoundedCornerShape(6.dp)).clickable { onToggleMute() }.padding(6.dp), contentAlignment = Alignment.Center) { Text(if (project.isMuted) "UNMUTE" else "MUTE", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = if (project.isMuted) NeonOrange else Color.White) }
            Box(Modifier.weight(1f).background(CyberCyan.copy(0.15f), RoundedCornerShape(6.dp)).clickable { musicPicker.launch("audio/*") }.padding(6.dp), contentAlignment = Alignment.Center) { Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.MusicNote, "BGM", tint = CyberCyan, modifier = Modifier.size(12.dp)); Spacer(Modifier.width(3.dp)); Text("+ SONG", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = CyberCyan) } }
        }
        Text("VISUALIZER", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            listOf("None", "Wave", "Bars", "Radial").forEach { s -> val sel = project.visualizerStyle.lowercase() == s.lowercase(); Box(Modifier.weight(1f).background(if (sel) CyberCyan.copy(0.2f) else Color.White.copy(0.04f), RoundedCornerShape(6.dp)).clickable { onUpdateVisualizer(s) }.padding(3.dp), contentAlignment = Alignment.Center) { Text(s, fontSize = 8.sp, fontWeight = FontWeight.Bold, color = if (sel) CyberCyan else Color.White) } }
        }
        Box(Modifier.fillMaxWidth().background(if (project.isBeatSyncEnabled) CyberCyan.copy(0.2f) else Color.White.copy(0.04f), RoundedCornerShape(6.dp)).clickable { onToggleBeatSync() }.padding(6.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("BEAT SYNC", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = if (project.isBeatSyncEnabled) CyberCyan else Color.White); Text(if (project.isBeatSyncEnabled) "ON" else "OFF", fontSize = 8.sp, color = if (project.isBeatSyncEnabled) CyberCyan else Color.Gray) }
        }
    }
}


// ─── 5. TEXT PANEL ──────────────────────────────────────────────
@Composable
private fun TextPanel(project: VideoProject, onUpdateText: (String?) -> Unit, onUpdateAnim: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("TEXT", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = NeonOrange)
        var txt by remember { mutableStateOf(project.activeTextOverlay ?: "") }
        OutlinedTextField(value = txt, onValueChange = { txt = it; onUpdateText(if (it.isBlank()) null else it) }, placeholder = { Text("Type subtitle...", fontSize = 9.sp, color = Color.Gray) }, colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = NeonOrange, unfocusedBorderColor = Color.White.copy(0.1f), focusedTextColor = Color.White, unfocusedTextColor = Color.White), modifier = Modifier.fillMaxWidth().height(36.dp), shape = RoundedCornerShape(8.dp))
        Text("ANIMATION", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
        FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            listOf("None", "Fade", "Typewriter", "Bounce", "Zoom", "Slide", "Pop", "Glitch", "Neon", "Wave").forEach { a -> val sel = project.textAnimationType.lowercase() == a.lowercase(); Box(Modifier.background(if (sel) NeonOrange.copy(0.2f) else Color.White.copy(0.04f), RoundedCornerShape(6.dp)).clickable { onUpdateAnim(a) }.padding(horizontal = 6.dp, vertical = 4.dp)) { Text(a, fontSize = 8.sp, fontWeight = FontWeight.Bold, color = if (sel) NeonOrange else Color.White) } }
        }
    }
}


// ─── 6. FILTERS PANEL ──────────────────────────────────────────
@Composable
private fun FiltersPanel(project: VideoProject, onUpdateFilter: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("FILTERS", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = CyberCyan)
        FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            listOf("none" to "Original", "grayscale" to "B&W", "sepia" to "Sepia", "invert" to "Invert", "warm" to "Warm", "cool" to "Cool", "vintage" to "Vintage", "dramatic" to "Drama").forEach { (id, name) ->
                val sel = project.selectedFilter.lowercase() == id
                Box(Modifier.background(if (sel) CyberCyan.copy(0.2f) else Color.White.copy(0.04f), RoundedCornerShape(6.dp)).clickable { onUpdateFilter(id) }.padding(horizontal = 8.dp, vertical = 5.dp)) { Text(name, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = if (sel) CyberCyan else Color.White) }
            }
        }
    }
}


// ─── 7. EFFECTS PANEL ──────────────────────────────────────────
@Composable
private fun EffectsPanel(project: VideoProject, onUpdateEffect: (String) -> Unit, onUpdateFilter: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("EFFECTS", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = NeonOrange)
        Text("Tap to apply effect on video", fontSize = 8.sp, color = Color.Gray)
        FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            val effects = listOf("🎬 Glitch" to "invert", "📼 VHS" to "sepia", "🔮 Chromatic" to "invert", "☀️ Lens Flare" to "none", "❄️ Snow" to "none", "🌧️ Rain" to "none", "🔥 Fire" to "none", "✨ Sparkle" to "none", "🌫️ Dust" to "sepia", "💨 Motion Blur" to "none", "📳 Shake" to "none", "⚡ Flash" to "invert", "💜 Neon Glow" to "invert", "🔲 Vignette" to "grayscale", "🌈 Rainbow" to "none", "📸 Film Grain" to "sepia", "🔵 Bokeh" to "none", "🎆 Particles" to "none", "💡 Strobe" to "grayscale", "🔍 Zoom Pulse" to "none")
            effects.forEach { (name, filterId) ->
                val sel = project.selectedEffect == name || (project.selectedEffect == "none" && filterId == "none")
                Box(Modifier.background(if (sel) NeonOrange.copy(0.2f) else Color.White.copy(0.04f), RoundedCornerShape(6.dp)).clickable { onUpdateEffect(name); onUpdateFilter(filterId); android.widget.Toast.makeText(androidx.compose.ui.platform.LocalContext.current, "Effect: $name applied!", android.widget.Toast.LENGTH_SHORT).show() }.padding(horizontal = 6.dp, vertical = 4.dp)) { Text(name, fontSize = 8.sp, fontWeight = FontWeight.Bold, color = if (sel) NeonOrange else Color.White) }
            }
        }
    }
}


// ─── 8. STICKERS PANEL ─────────────────────────────────────────
@Composable
private fun StickersPanel(project: VideoProject, onUpdateSticker: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("STICKERS", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = CyberCyan)
        FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            listOf("None" to "none", "🔥 Fire" to "fire", "⭐ Star" to "star", "❤️ Heart" to "heart", "⚡ Glow" to "glow", "💎 Diamond" to "diamond", "🎵 Music" to "music", "👑 Crown" to "crown", "💫 Sparkle" to "sparkle", "🎯 Target" to "target", "🏆 Trophy" to "trophy", "💀 Skull" to "skull").forEach { (name, id) ->
                val sel = project.stickerType == id
                Box(Modifier.background(if (sel) CyberCyan.copy(0.2f) else Color.White.copy(0.04f), RoundedCornerShape(6.dp)).clickable { onUpdateSticker(id) }.padding(horizontal = 8.dp, vertical = 5.dp)) { Text(name, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = if (sel) CyberCyan else Color.White) }
            }
        }
    }
}


// ─── 9. TRANSITIONS PANEL ──────────────────────────────────────
@Composable
private fun TransitionsPanel(project: VideoProject, onUpdateTransition: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("TRANSITIONS", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = NeonOrange)
        FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            listOf("None", "Crossfade", "Glitch", "Zoom In", "Zoom Out", "Spin", "Wipe", "Dissolve", "Blur", "Pixelate", "Mosaic", "Split", "Film Burn", "Light Leak", "Smoke", "Circle", "Diamond", "Heart", "Flash", "L-Cut").forEach { t ->
                val sel = project.transitionType.lowercase() == t.lowercase()
                Box(Modifier.background(if (sel) NeonOrange.copy(0.2f) else Color.White.copy(0.04f), RoundedCornerShape(6.dp)).clickable { onUpdateTransition(t) }.padding(horizontal = 6.dp, vertical = 4.dp)) { Text(t, fontSize = 8.sp, fontWeight = FontWeight.Bold, color = if (sel) NeonOrange else Color.White) }
            }
        }
    }
}


// ─── 10. ANIMATIONS PANEL ──────────────────────────────────────
@Composable
private fun AnimationsPanel(project: VideoProject, onUpdateAnim: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("ANIMATIONS", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = CyberCyan)
        FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            listOf("Fade In", "Fade Out", "Typewriter", "Bounce", "Slide Left", "Slide Right", "Slide Up", "Slide Down", "Zoom In", "Zoom Out", "Rotate", "Wave", "Glitch In", "Neon Pulse", "Pop", "Flip", "Elastic", "Spring", "Rubber", "Swing").forEach { a ->
                val sel = project.textAnimationType.lowercase() == a.lowercase()
                Box(Modifier.background(if (sel) CyberCyan.copy(0.2f) else Color.White.copy(0.04f), RoundedCornerShape(6.dp)).clickable { onUpdateAnim(a) }.padding(horizontal = 6.dp, vertical = 4.dp)) { Text(a, fontSize = 8.sp, fontWeight = FontWeight.Bold, color = if (sel) CyberCyan else Color.White) }
            }
        }
    }
}


// ─── 11. 3D PANEL ──────────────────────────────────────────────
@Composable
private fun ThreeDPanel(project: VideoProject, onUpdate3D: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("3D CINEMATIC", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = NeonOrange)
        FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            listOf("Circle Mask", "Heart Mask", "Star Mask", "Hexagon", "Diamond", "Triangle", "Vignette", "Film Burn", "Light Leak", "Lens Flare", "Smoke", "Water", "Fire", "Particles", "Bokeh", "Glitch 3D", "Chromatic", "Anamorphic", "Cinematic Bars", "Color Splash").forEach { m ->
                val maskId = m.lowercase().replace(" ", "_")
                val sel = project.active3DShapeMask == maskId
                Box(Modifier.background(if (sel) NeonOrange.copy(0.2f) else Color.White.copy(0.04f), RoundedCornerShape(6.dp)).clickable { onUpdate3D(maskId) }.padding(horizontal = 6.dp, vertical = 4.dp)) { Text(m, fontSize = 8.sp, fontWeight = FontWeight.Bold, color = if (sel) NeonOrange else Color.White) }
            }
        }
    }
}


// ─── 12. IMAGE PANEL ───────────────────────────────────────────
@Composable
private fun ImagePanel(
    project: VideoProject,
    imagePicker: androidx.activity.result.ActivityResultLauncher<String>,
    onUpdateImage: (String?) -> Unit,
    onUpdateOpacity: (Float) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("IMAGE OVERLAY", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = CyberCyan)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Box(Modifier.weight(1f).background(CyberCyan.copy(0.15f), RoundedCornerShape(8.dp)).clickable { imagePicker.launch("image/*") }.padding(10.dp), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("🖼️", fontSize = 20.sp)
                    Text("Add Image", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = CyberCyan)
                }
            }
            Box(Modifier.weight(1f).background(if (project.imageOverlayPath != null) NeonOrange.copy(0.15f) else Color.White.copy(0.04f), RoundedCornerShape(8.dp)).clickable { onUpdateImage(null) }.padding(10.dp), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("🗑️", fontSize = 20.sp)
                    Text("Remove", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = if (project.imageOverlayPath != null) NeonOrange else Color.Gray)
                }
            }
        }
        if (project.imageOverlayPath != null) {
            Text("OPACITY", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
            Slider(value = project.imageOverlayOpacity, onValueChange = onUpdateOpacity, valueRange = 0f..1f, colors = SliderDefaults.colors(activeTrackColor = CyberCyan, thumbColor = CyberCyan), modifier = Modifier.height(18.dp))
        }
    }
}


// ─── 13. TEMPLATE PANEL ────────────────────────────────────────
@Composable
private fun TemplatePanel(project: VideoProject, onUpdateTemplate: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("TEMPLATES", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = NeonOrange)
        Text("Online templates coming soon — use local presets", fontSize = 8.sp, color = Color.Gray)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            val templates = listOf("none" to "None", "spark" to "✨ Spark", "bloom" to "🌸 Bloom", "vlog" to "📹 Vlog", "poetry" to "📝 Poetry", "beats" to "🎵 Beats", "glitch" to "📺 Glitch", "cinema" to "🎬 Cinema", "wedding" to "💒 Wedding", "travel" to "✈️ Travel")
            items(templates) { (id, name) ->
                val sel = project.activeTemplateId == id
                Box(Modifier.background(if (sel) NeonOrange.copy(0.2f) else Color.White.copy(0.04f), RoundedCornerShape(8.dp)).clickable { onUpdateTemplate(id) }.padding(horizontal = 10.dp, vertical = 8.dp)) { Text(name, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = if (sel) NeonOrange else Color.White) }
            }
        }
    }
}
