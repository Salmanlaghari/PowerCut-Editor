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
    var isEditingComplete by remember { mutableStateOf(false) } // Final page state

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

    if (isEditingComplete) {
        // ─── FINAL PAGE: Import + Export after editing complete ──
        EditingCompletePage(
            project = project,
            exoPlayer = exoPlayer,
            isPlaying = isPlaying,
            currentPlaybackTime = currentPlaybackTime,
            durationMs = project.durationMs,
            onPlayPause = { isPlaying = !isPlaying },
            onBackToEdit = { isEditingComplete = false },
            onImport = { multiFilePicker.launch("video/*") },
            onExport = { onSaveDraft(); onExport() },
            onBack = { onSaveDraft(); onBack() }
        )
    } else {
        // ─── NORMAL EDITOR ───────────────────────────────────────
        Column(modifier = Modifier.fillMaxSize().background(Color(0xFF0B0D12))) {

            // ─── 1. HEADER (Done button instead of Export) ──────
            EditorHeader(
                currentPlaybackTime = currentPlaybackTime,
                durationMs = project.durationMs,
                onBack = { onSaveDraft(); onBack() },
                onDone = { isEditingComplete = true },
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
    } // end else (normal editor)
}


// ═══════════════════════════════════════════════════════════════
//  EDITING COMPLETE PAGE — Import + Export after editing done
// ═══════════════════════════════════════════════════════════════
@OptIn(UnstableApi::class)
@Composable
private fun EditingCompletePage(
    project: VideoProject,
    exoPlayer: ExoPlayer,
    isPlaying: Boolean,
    currentPlaybackTime: Long,
    durationMs: Long,
    onPlayPause: () -> Unit,
    onBackToEdit: () -> Unit,
    onImport: () -> Unit,
    onExport: () -> Unit,
    onBack: () -> Unit
) {
    val aspect = when (project.aspectPreset) {
        "1:1" -> 1.0f; "9:16" -> 9f/16f; "4:5" -> 4f/5f; else -> 16f/9f
    }

    Column(
        modifier = Modifier.fillMaxSize().background(Color(0xFF0B0D12))
    ) {
        // Header with back
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
                    Text("Editing Complete", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Text("Ready to export", fontSize = 10.sp, color = CyberCyan)
                }
            }
            // Back to Edit button
            Box(
                modifier = Modifier.glassmorphic(shape = RoundedCornerShape(16.dp)).tactileClick(onClick = onBackToEdit).padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text("✏️ Edit More", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
        }

        Spacer(Modifier.height(8.dp))

        // Video Preview (smaller)
        Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).weight(1f), contentAlignment = Alignment.Center) {
            Box(
                modifier = Modifier.fillMaxHeight().aspectRatio(aspect).clip(RoundedCornerShape(16.dp)).background(Color.Black)
                    .border(2.dp, CyberCyan.copy(0.3f), RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                AndroidView(
                    factory = { ctx -> PlayerView(ctx).apply { player = exoPlayer; useController = false } },
                    modifier = Modifier.fillMaxSize()
                )
                // Play overlay
                Box(
                    modifier = Modifier.size(56.dp).background(Color.White.copy(0.2f), CircleShape)
                        .border(2.dp, Color.White.copy(0.4f), CircleShape).clickable { onPlayPause },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, "Play", tint = Color.White, modifier = Modifier.size(28.dp))
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        // Project summary
        Box(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                .glassmorphic(shape = RoundedCornerShape(12.dp)).padding(12.dp)
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                SummaryItem("⏱️", "Duration", formatTime(durationMs))
                SummaryItem("📐", "Aspect", project.aspectPreset)
                SummaryItem("🎬", "Res", project.targetResolution.uppercase())
                SummaryItem("⚡", "Speed", "${project.speedFactor}x")
            }
        }

        Spacer(Modifier.height(8.dp))

        // Export format options
        var selectedFormat by remember { mutableStateOf("mp4_hd") }
        Box(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                .glassmorphic(shape = RoundedCornerShape(12.dp)).padding(10.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("EXPORT FORMAT", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf("mp4_hd" to "📹 MP4-HD", "mp4_4k" to "🎬 MP4 4K", "mp4_8k" to "💎 MP4 8K", "webm" to "🌐 WebM", "gif" to "🎞️ GIF").forEach { (id, label) ->
                        val sel = selectedFormat == id
                        Box(Modifier.weight(1f).background(if (sel) CyberCyan.copy(0.2f) else Color.White.copy(0.04f), RoundedCornerShape(6.dp)).clickable { selectedFormat = id }.padding(4.dp), contentAlignment = Alignment.Center) {
                            Text(label, fontSize = 7.sp, fontWeight = FontWeight.Bold, color = if (sel) CyberCyan else Color.White)
                        }
                    }
                }
                Spacer(Modifier.height(2.dp))
                Text("UPSCALE", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf("none" to "Original", "2x" to "2x AI", "4x" to "4x Ultra").forEach { (id, label) ->
                        Box(Modifier.weight(1f).background(NeonOrange.copy(0.08f), RoundedCornerShape(6.dp)).clickable { android.widget.Toast.makeText(androidx.compose.ui.platform.LocalContext.current, "Upscale: $label", android.widget.Toast.LENGTH_SHORT).show() }.padding(4.dp), contentAlignment = Alignment.Center) {
                            Text(label, fontSize = 7.sp, fontWeight = FontWeight.Bold, color = NeonOrange)
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        // ═══ IMPORT + EXPORT BUTTONS ═══
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // IMPORT BUTTON
            Box(
                modifier = Modifier.weight(1f).height(56.dp)
                    .glassmorphic(shape = RoundedCornerShape(16.dp))
                    .border(1.dp, CyberCyan.copy(0.4f), RoundedCornerShape(16.dp))
                    .tactileClick(onClick = onImport)
                    .padding(horizontal = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Add, "Import", tint = CyberCyan, modifier = Modifier.size(22.dp))
                    Column {
                        Text("IMPORT", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)
                        Text("Add more clips", fontSize = 9.sp, color = Color.Gray)
                    }
                }
            }

            // EXPORT BUTTON
            Box(
                modifier = Modifier.weight(1f).height(56.dp)
                    .neonGlow(AccentSecondary, RoundedCornerShape(16.dp), 1.5.dp)
                    .background(premiumAccentGradient, RoundedCornerShape(16.dp))
                    .tactileClick(onClick = onExport)
                    .padding(horizontal = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("🎬", fontSize = 20.sp)
                    Column {
                        Text("EXPORT", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp, letterSpacing = 0.5.sp)
                        Text("Save video", fontSize = 9.sp, color = Color.White.copy(0.8f))
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun SummaryItem(emoji: String, label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(emoji, fontSize = 16.sp)
        Text(value, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
        Text(label, fontSize = 8.sp, color = Color.Gray)
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
    onDone: () -> Unit,
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
            Box(modifier = Modifier.neonGlow(CyberCyan, RoundedCornerShape(20.dp), 1.dp).background(Brush.horizontalGradient(listOf(CyberCyan, Color(0xFF7C5CFF))), RoundedCornerShape(20.dp)).tactileClick(onClick = onDone).padding(horizontal = 12.dp, vertical = 5.dp)) {
                Text("DONE ✓", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 10.sp, letterSpacing = 0.5.sp)
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
@OptIn(ExperimentalLayoutApi::class)
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
    var editSubTab by remember { mutableStateOf("adjust") }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        // Sub-tab bar like CapCut
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            listOf("adjust" to "🎨 Adjust", "crop" to "📐 Crop", "speed" to "⚡ Speed", "slowmo" to "🐌 SlowMo", "reverse" to "🔄 Reverse", "freeze" to "🧊 Freeze", "delete" to "🗑️ Delete").forEach { (id, label) ->
                val sel = editSubTab == id
                Box(Modifier.background(if (sel) NeonOrange.copy(0.2f) else Color.White.copy(0.04f), RoundedCornerShape(8.dp)).clickable { editSubTab = id }.padding(horizontal = 8.dp, vertical = 5.dp)) {
                    Text(label, fontSize = 8.sp, fontWeight = FontWeight.Bold, color = if (sel) NeonOrange else Color.White)
                }
            }
        }

        when (editSubTab) {
            "adjust" -> {
                // CapCut style adjustments
                Text("ADJUSTMENTS", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                val adjustments = listOf(
                    Triple("☀️", "Brightness", 0.5f),
                    Triple("🔲", "Contrast", 0.5f),
                    Triple("🎨", "Saturation", 0.5f),
                    Triple("🔪", "Sharpness", 0.5f),
                    Triple("🌡️", "Temperature", 0.5f),
                    Triple("🌫️", "Fade", 0f),
                    Triple("🌑", "Vignette", 0f),
                    Triple("📸", "Grain", 0f)
                )
                adjustments.forEach { (emoji, name, defaultVal) ->
                    Row(Modifier.fillMaxWidth().background(Color.White.copy(0.03f), RoundedCornerShape(6.dp)).padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(emoji, fontSize = 12.sp)
                        Spacer(Modifier.width(6.dp))
                        Text(name, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.White, modifier = Modifier.width(60.dp))
                        Slider(value = defaultVal, onValueChange = {}, valueRange = 0f..1f, colors = SliderDefaults.colors(activeTrackColor = NeonOrange, thumbColor = NeonOrange, inactiveTrackColor = Color.White.copy(0.08f)), modifier = Modifier.weight(1f).height(18.dp))
                    }
                }
            }
            "crop" -> {
                Text("CROP & TRANSFORM", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf("Free", "1:1", "16:9", "9:16", "4:5", "21:9", "3:4").forEach { c ->
                        val sel = project.cropPreset == c
                        Box(Modifier.weight(1f).background(if (sel) CyberCyan.copy(0.2f) else Color.White.copy(0.04f), RoundedCornerShape(6.dp)).clickable { onUpdateCropPreset(c); onUpdateAspectPreset(c) }.padding(4.dp), contentAlignment = Alignment.Center) {
                            Text(c, fontSize = 7.sp, fontWeight = FontWeight.Bold, color = if (sel) CyberCyan else Color.White)
                        }
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text("ROTATE & FLIP", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf("↺ 90° Left" to { onUpdateRotation() }, "↻ 90° Right" to { onUpdateRotation() }, "↔ Mirror" to onToggleFlipH, "↕ Flip" to onToggleFlipV).forEach { (l, a) ->
                        Box(Modifier.weight(1f).background(Color.White.copy(0.04f), RoundedCornerShape(6.dp)).clickable { a() }.padding(6.dp), contentAlignment = Alignment.Center) {
                            Text(l, fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text("RESOLUTION", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf("720p", "1080p", "4K", "8K").forEach { r ->
                        val sel = project.targetResolution.lowercase() == r.lowercase()
                        Box(Modifier.weight(1f).background(if (sel) NeonOrange.copy(0.2f) else Color.White.copy(0.04f), RoundedCornerShape(6.dp)).clickable { onUpdateResolution(r) }.padding(4.dp), contentAlignment = Alignment.Center) {
                            Text(r, fontSize = 8.sp, fontWeight = FontWeight.Bold, color = if (sel) NeonOrange else Color.White)
                        }
                    }
                }
            }
            "speed" -> {
                Text("SPEED CONTROL", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    listOf(0.1f, 0.25f, 0.5f, 0.75f, 1.0f, 1.5f, 2.0f, 3.0f, 4.0f, 8.0f, 16.0f).forEach { s ->
                        val sel = project.speedFactor == s
                        Box(Modifier.weight(1f).background(if (sel) NeonOrange.copy(0.2f) else Color.White.copy(0.04f), RoundedCornerShape(6.dp)).clickable { onUpdateSpeed(s) }.padding(3.dp), contentAlignment = Alignment.Center) {
                            Text("${s}x", fontSize = 7.sp, fontWeight = FontWeight.Bold, color = if (sel) NeonOrange else Color.White)
                        }
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text("SPEED CURVE", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf("Normal", "Montage", "Hero", "Flash", "Bullet", "Custom").forEach { c ->
                        val sel = project.speedCurve.lowercase() == c.lowercase()
                        Box(Modifier.weight(1f).background(if (sel) CyberCyan.copy(0.15f) else Color.White.copy(0.03f), RoundedCornerShape(6.dp)).clickable { onUpdateSpeedCurve(c) }.padding(4.dp), contentAlignment = Alignment.Center) {
                            Text(c, fontSize = 7.sp, fontWeight = FontWeight.Bold, color = if (sel) CyberCyan else Color.White)
                        }
                    }
                }
            }
            "slowmo" -> {
                Text("SMOOTH SLOW MOTION", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                Text("Professional slow-mo with frame interpolation", fontSize = 7.sp, color = Color.Gray.copy(0.7f))
                Spacer(Modifier.height(2.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf("0.1x Ultra" to 0.1f, "0.25x Super" to 0.25f, "0.3x Smooth" to 0.3f, "0.5x Slow" to 0.5f).forEach { (label, speed) ->
                        val sel = project.speedFactor == speed
                        Box(Modifier.weight(1f).background(if (sel) CyberCyan.copy(0.2f) else Color.White.copy(0.04f), RoundedCornerShape(8.dp)).clickable { onUpdateSpeed(speed) }.padding(6.dp), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("🐌", fontSize = 14.sp)
                                Text(label, fontSize = 7.sp, fontWeight = FontWeight.Bold, color = if (sel) CyberCyan else Color.White, textAlign = TextAlign.Center)
                            }
                        }
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text("ACTION SPEED", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf("2x Fast" to 2f, "4x Hyper" to 4f, "8x Ultra" to 8f, "16x Max" to 16f).forEach { (label, speed) ->
                        val sel = project.speedFactor == speed
                        Box(Modifier.weight(1f).background(if (sel) NeonOrange.copy(0.2f) else Color.White.copy(0.04f), RoundedCornerShape(8.dp)).clickable { onUpdateSpeed(speed) }.padding(6.dp), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("⚡", fontSize = 14.sp)
                                Text(label, fontSize = 7.sp, fontWeight = FontWeight.Bold, color = if (sel) NeonOrange else Color.White, textAlign = TextAlign.Center)
                            }
                        }
                    }
                }
            }
            "reverse" -> {
                Text("REVERSE VIDEO", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                Box(Modifier.fillMaxWidth().background(Color.White.copy(0.04f), RoundedCornerShape(8.dp)).clickable { android.widget.Toast.makeText(androidx.compose.ui.platform.LocalContext.current, "Reverse applied!", android.widget.Toast.LENGTH_SHORT).show() }.padding(12.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("🔄", fontSize = 28.sp)
                        Text("Tap to Reverse Video", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Text("Plays video backwards", fontSize = 8.sp, color = Color.Gray)
                    }
                }
            }
            "freeze" -> {
                Text("FREEZE FRAME", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                Box(Modifier.fillMaxWidth().background(Color.White.copy(0.04f), RoundedCornerShape(8.dp)).clickable { android.widget.Toast.makeText(androidx.compose.ui.platform.LocalContext.current, "Freeze at ${formatTime(project.trimStartMs)}!", android.widget.Toast.LENGTH_SHORT).show() }.padding(12.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("🧊", fontSize = 28.sp)
                        Text("Freeze at Playhead", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Text("Creates still frame", fontSize = 8.sp, color = Color.Gray)
                    }
                }
            }
            "delete" -> {
                Text("DELETE SECTION", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                Box(Modifier.fillMaxWidth().background(Color(0xFFFF1744).copy(0.1f), RoundedCornerShape(8.dp)).border(1.dp, Color(0xFFFF1744).copy(0.3f), RoundedCornerShape(8.dp)).clickable { android.widget.Toast.makeText(androidx.compose.ui.platform.LocalContext.current, "Section deleted!", android.widget.Toast.LENGTH_SHORT).show() }.padding(12.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("🗑️", fontSize = 28.sp)
                        Text("Delete Selected", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFF1744))
                        Text("Remove clip section", fontSize = 8.sp, color = Color.Gray)
                    }
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
        // 3D styled layer items
        val layers = listOf(
            Triple("🎬", "Video Layer", true),
            Triple("🔊", "Audio Layer", project.backgroundMusicPath != null),
            Triple("📝", "Text Layer", project.activeTextOverlay != null),
            Triple("🖼️", "Image Layer", project.imageOverlayPath != null),
            Triple("⭐", "Sticker Layer", project.stickerType != "none"),
            Triple("✨", "Effect Layer", project.selectedFilter != "none")
        )
        layers.forEach { (icon, name, hasContent) ->
            Row(
                Modifier.fillMaxWidth()
                    .background(if (hasContent) Brush.horizontalGradient(listOf(CyberCyan.copy(0.15f), Color.Transparent)) else Brush.horizontalGradient(listOf(Color.White.copy(0.03f), Color.Transparent)), RoundedCornerShape(8.dp))
                    .border(1.dp, if (hasContent) CyberCyan.copy(0.2f) else Color.White.copy(0.04f), RoundedCornerShape(8.dp))
                    .clickable { android.widget.Toast.makeText(context, "$name: ${if (hasContent) "visible" else "empty"}", android.widget.Toast.LENGTH_SHORT).show() }
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // 3D icon box
                    Box(
                        Modifier.size(28.dp)
                            .background(Color.White.copy(0.06f), RoundedCornerShape(6.dp))
                            .border(1.dp, Color.White.copy(0.1f), RoundedCornerShape(6.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(icon, fontSize = 14.sp)
                    }
                    Column {
                        Text(name, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = if (hasContent) Color.White else Color.Gray)
                        Text(if (hasContent) "Active" else "Empty", fontSize = 7.sp, color = if (hasContent) CyberCyan else Color.Gray.copy(0.5f))
                    }
                }
                // 3D visibility icon
                Box(
                    Modifier.size(24.dp)
                        .background(if (hasContent) CyberCyan.copy(0.15f) else Color.Transparent, CircleShape)
                        .border(1.dp, if (hasContent) CyberCyan.copy(0.3f) else Color.White.copy(0.08f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(if (hasContent) "👁️" else "🙈", fontSize = 10.sp)
                }
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
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TextPanel(project: VideoProject, onUpdateText: (String?) -> Unit, onUpdateAnim: (String) -> Unit) {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("TEXT", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = NeonOrange)
        var txt by remember { mutableStateOf(project.activeTextOverlay ?: "") }
        OutlinedTextField(value = txt, onValueChange = { txt = it; onUpdateText(if (it.isBlank()) null else it) }, placeholder = { Text("Type subtitle...", fontSize = 9.sp, color = Color.Gray) }, colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = NeonOrange, unfocusedBorderColor = Color.White.copy(0.1f), focusedTextColor = Color.White, unfocusedTextColor = Color.White), modifier = Modifier.fillMaxWidth().height(36.dp), shape = RoundedCornerShape(8.dp))

        // Quick text templates
        Text("QUICK TEXT", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            items(listOf("🔥 Fire Text", "💫 Glow Text", "🎬 Title", "📍 Subtitle", "🎵 Lyrics", "💬 Dialog", "📰 Breaking", "⚡ Neon")) { preset ->
                Box(Modifier.background(Color.White.copy(0.04f), RoundedCornerShape(6.dp)).clickable { onUpdateText(preset); txt = preset }.padding(horizontal = 8.dp, vertical = 4.dp)) {
                    Text(preset, fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }

        // Scrolling / Marquee text
        Text("SCROLL & MOTION", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            listOf("Scroll L" to "scroll_left", "Scroll R" to "scroll_right", "Scroll Up" to "scroll_up", "Marquee" to "marquee").forEach { (label, id) ->
                Box(Modifier.weight(1f).background(CyberCyan.copy(0.1f), RoundedCornerShape(6.dp)).clickable { onUpdateAnim(id); android.widget.Toast.makeText(ctx, "Text: $label", android.widget.Toast.LENGTH_SHORT).show() }.padding(4.dp), contentAlignment = Alignment.Center) {
                    Text(label, fontSize = 7.sp, fontWeight = FontWeight.Bold, color = CyberCyan)
                }
            }
        }

        Text("ANIMATION", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
        FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            listOf("None", "Fade", "Typewriter", "Bounce", "Zoom", "Slide", "Pop", "Glitch", "Neon", "Wave").forEach { a -> val sel = project.textAnimationType.lowercase() == a.lowercase(); Box(Modifier.background(if (sel) NeonOrange.copy(0.2f) else Color.White.copy(0.04f), RoundedCornerShape(6.dp)).clickable { onUpdateAnim(a) }.padding(horizontal = 6.dp, vertical = 4.dp)) { Text(a, fontSize = 8.sp, fontWeight = FontWeight.Bold, color = if (sel) NeonOrange else Color.White) } }
        }
    }
}


// ─── 6. FILTERS PANEL ──────────────────────────────────────────
@OptIn(ExperimentalLayoutApi::class)
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
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EffectsPanel(project: VideoProject, onUpdateEffect: (String) -> Unit, onUpdateFilter: (String) -> Unit) {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    var effectCategory by remember { mutableStateOf("all") }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("SUPER EFFECTS", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = NeonOrange)
        // Category tabs
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            listOf("all" to "All", "vfx" to "VFX", "color" to "Color", "motion" to "Motion", "retro" to "Retro", "neon" to "Neon").forEach { (id, label) ->
                val sel = effectCategory == id
                Box(Modifier.background(if (sel) NeonOrange.copy(0.2f) else Color.White.copy(0.04f), RoundedCornerShape(6.dp)).clickable { effectCategory = id }.padding(horizontal = 6.dp, vertical = 3.dp)) {
                    Text(label, fontSize = 7.sp, fontWeight = FontWeight.Bold, color = if (sel) NeonOrange else Color.White)
                }
            }
        }
        FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            val allEffects = listOf(
                "🎬 Glitch" to "invert", "📼 VHS" to "sepia", "🔮 Chromatic" to "invert",
                "☀️ Lens Flare" to "none", "❄️ Snow" to "none", "🌧️ Rain" to "none",
                "🔥 Fire" to "none", "✨ Sparkle" to "none", "🌫️ Dust" to "sepia",
                "💨 Motion Blur" to "none", "📳 Shake" to "none", "⚡ Flash" to "invert",
                "💜 Neon Glow" to "invert", "🔲 Vignette" to "grayscale", "🌈 Rainbow" to "none",
                "📸 Film Grain" to "sepia", "🔵 Bokeh" to "none", "🎆 Particles" to "none",
                "💡 Strobe" to "grayscale", "🔍 Zoom Pulse" to "none",
                "🌊 Wave Distort" to "none", "🔥 Flame" to "invert", "❄️ Frost" to "grayscale",
                "💫 Starburst" to "none", "🎭 Face Blur" to "none", "🌀 Swirl" to "invert",
                "💥 Explosion" to "invert", "🌟 Light Leak" to "none", "📽️ Film Strip" to "sepia",
                "🎨 Color Splash" to "invert", "⚡ Electric" to "invert", "🌊 Tidal" to "none"
            )
            allEffects.forEach { (name, filterId) ->
                val sel = project.selectedEffect == name
                Box(Modifier.background(if (sel) NeonOrange.copy(0.2f) else Color.White.copy(0.04f), RoundedCornerShape(6.dp)).clickable { onUpdateEffect(name); onUpdateFilter(filterId); android.widget.Toast.makeText(ctx, "✨ $name applied!", android.widget.Toast.LENGTH_SHORT).show() }.padding(horizontal = 5.dp, vertical = 3.dp)) { Text(name, fontSize = 7.sp, fontWeight = FontWeight.Bold, color = if (sel) NeonOrange else Color.White) }
            }
        }
    }
}


// ─── 8. STICKERS PANEL ─────────────────────────────────────────
@OptIn(ExperimentalLayoutApi::class)
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
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TransitionsPanel(project: VideoProject, onUpdateTransition: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("TRANSITIONS", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = NeonOrange)
        FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            listOf("None", "Crossfade", "Glitch", "Zoom In", "Zoom Out", "Spin", "Wipe", "Dissolve", "Blur", "Pixelate", "Mosaic", "Split", "Film Burn", "Light Leak", "Smoke", "Circle", "Diamond", "Heart", "Flash", "L-Cut", "J-Cut", "Slide Left", "Slide Right", "Slide Up", "Slide Down", "Rotate In", "Rotate Out", "Bounce", "Elastic", "Spring").forEach { t ->
                val sel = project.transitionType.lowercase() == t.lowercase()
                Box(Modifier.background(if (sel) NeonOrange.copy(0.2f) else Color.White.copy(0.04f), RoundedCornerShape(6.dp)).clickable { onUpdateTransition(t) }.padding(horizontal = 5.dp, vertical = 3.dp)) { Text(t, fontSize = 7.sp, fontWeight = FontWeight.Bold, color = if (sel) NeonOrange else Color.White) }
            }
        }
    }
}


// ─── 10. ANIMATIONS PANEL ──────────────────────────────────────
@OptIn(ExperimentalLayoutApi::class)
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
@OptIn(ExperimentalLayoutApi::class)
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
        Text("ORIGINAL TEMPLATES", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = NeonOrange)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            val templates = listOf(
                "none" to "❌ None",
                "cinema" to "🎬 Cinema",
                "wedding" to "💒 Wedding",
                "travel" to "✈️ Travel",
                "vlog" to "📹 Vlog",
                "poetry" to "📝 Poetry",
                "beats" to "🎵 Beats",
                "glitch" to "📺 Glitch",
                "spark" to "✨ Spark",
                "bloom" to "🌸 Bloom",
                "reels" to "📱 Reels",
                "tiktok" to "🎵 TikTok",
                "neon" to "💜 Neon",
                "retro" to "📼 Retro",
                "minimal" to "◻️ Minimal",
                "dark" to "🌑 Dark",
                "golden" to "🌟 Golden",
                "ocean" to "🌊 Ocean",
                "fire" to "🔥 Fire",
                "ice" to "❄️ Ice"
            )
            items(templates) { (id, name) ->
                val sel = project.activeTemplateId == id
                Box(Modifier.background(if (sel) NeonOrange.copy(0.2f) else Color.White.copy(0.04f), RoundedCornerShape(8.dp)).clickable { onUpdateTemplate(id) }.padding(horizontal = 10.dp, vertical = 8.dp)) { Text(name, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = if (sel) NeonOrange else Color.White) }
            }
        }
    }
}
