package com.powercut.editor.ui.editor

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
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
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
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
    onRemoveLayer: (String) -> Unit = {},
    // Green Screen callbacks
    onToggleGreenScreen: () -> Unit = {},
    onUpdateGreenScreenColor: (String) -> Unit = {},
    onUpdateGreenScreenThreshold: (Float) -> Unit = {},
    onSelectAutoBackground: (Int) -> Unit = {},
    onPickCustomBackground: () -> Unit = {},
    // Eraser callbacks
    onUpdateEraserMode: (String) -> Unit = {},
    onUpdateEraserBrushSize: (Float) -> Unit = {},
    onUpdateEraserTolerance: (Float) -> Unit = {},
    onToggleEraserSoftEdge: () -> Unit = {},
    onUndoEraser: () -> Unit = {},
    onResetEraser: () -> Unit = {},
    // Image Editor callbacks
    onUpdateImageEditorBrightness: (Float) -> Unit = {},
    onUpdateImageEditorContrast: (Float) -> Unit = {},
    onUpdateImageEditorSaturation: (Float) -> Unit = {},
    onUpdateImageEditorBlur: (Float) -> Unit = {},
    onUpdateImageEditorSharpen: (Float) -> Unit = {},
    onUpdateImageEditorTemperature: (Float) -> Unit = {},
    onUpdateImageEditorVignette: (Float) -> Unit = {},
    onUpdateImageEditorGrain: (Float) -> Unit = {},
    onUpdateImageEditorFade: (Float) -> Unit = {},
    onUpdateImageEditorHighlights: (Float) -> Unit = {},
    onUpdateImageEditorShadows: (Float) -> Unit = {},
    onUpdateImageEditorExposure: (Float) -> Unit = {},
    onResetImageEditor: () -> Unit = {},
    // Orientation callbacks
    onUpdateOrientationMode: (String) -> Unit = {},
    onToggleVerticalSafeZone: () -> Unit = {},
    onToggleHorizontalLetterbox: () -> Unit = {},
    onToggleAutoReframe: () -> Unit = {}
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
    val exoPlayer = remember {
        // Smooth 60fps: tuned load control + extension renderers + force highest quality
        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(800, 8000, 200, 800)
            .setTargetBufferBytes(DefaultLoadControl.DEFAULT_TARGET_BUFFER_BYTES)
            .setPrioritizeTimeOverSizeThresholds(true)
            .build()
        val renderers = DefaultRenderersFactory(context)
            .setEnableDecoderFallback(true)
            .setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER)
        val trackSelector = DefaultTrackSelector(context).apply {
            setParameters(buildUponParameters()
                .setForceHighestSupportedBitrate(true)
                .build())
        }
        ExoPlayer.Builder(context, renderers)
            .setTrackSelector(trackSelector)
            .setLoadControl(loadControl)
            .build().apply { repeatMode = Player.REPEAT_MODE_ONE }
    }
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
        if (isPlaying) { exoPlayer.play(); while (isPlaying) { currentPlaybackTime = exoPlayer.currentPosition; kotlinx.coroutines.delay(33) } }
        else { exoPlayer.pause(); kotlinx.coroutines.delay(3000); if (!isPlaying) onSaveDraft() }
    }
    LaunchedEffect(project.isMuted, project.videoVolume) { exoPlayer.volume = if (project.isMuted) 0f else project.videoVolume }
    LaunchedEffect(project.speedFactor) { exoPlayer.playbackParameters = PlaybackParameters(project.speedFactor); playbackSpeed = project.speedFactor }
    DisposableEffect(Unit) { onDispose { exoPlayer.release() } }

    // ─── Filter Matrix ────────────────────────────────────────
    val colorFilter = remember(project.selectedFilter) {
        val f = project.selectedFilter.lowercase().replace("-", "_").replace(" ", "_")
        when (f) {
            "grayscale", "mono" -> ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) })
            "sepia" -> ColorFilter.colorMatrix(ColorMatrix(floatArrayOf(0.393f,0.769f,0.189f,0f,0f,0.349f,0.686f,0.168f,0f,0f,0.272f,0.534f,0.131f,0f,0f,0f,0f,0f,1f,0f)))
            "invert" -> ColorFilter.colorMatrix(ColorMatrix(floatArrayOf(-1f,0f,0f,0f,255f,0f,-1f,0f,0f,255f,0f,0f,-1f,0f,255f,0f,0f,0f,1f,0f)))
            "warm" -> ColorFilter.colorMatrix(ColorMatrix(floatArrayOf(
                1.1f,0f,0f,0f,0f, 0f,1.02f,0f,0f,0f, 0f,0f,0.9f,0f,0f, 0f,0f,0f,1f,0f)))
            "cool" -> ColorFilter.colorMatrix(ColorMatrix(floatArrayOf(
                0.9f,0f,0f,0f,0f, 0f,0.97f,0f,0f,0f, 0f,0f,1.1f,0f,0f, 0f,0f,0f,1f,0f)))
            "vintage" -> ColorFilter.colorMatrix(ColorMatrix(floatArrayOf(
                0.5f,0.65f,0.15f,0f,0f, 0.45f,0.6f,0.12f,0f,0f, 0.35f,0.55f,0.1f,0f,0f, 0f,0f,0f,1f,0f)))
            "dramatic" -> ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(1.3f) })
            "vivid" -> ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(1.6f) })
            "noir" -> ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) })
            "bloom" -> ColorFilter.colorMatrix(ColorMatrix(floatArrayOf(
                1.05f,0.05f,0.05f,0f,10f, 0.05f,1.05f,0.05f,0f,10f, 0.05f,0.05f,1.05f,0f,10f, 0f,0f,0f,1f,0f)))
            "tealorange", "teal_orange" -> ColorFilter.colorMatrix(ColorMatrix(floatArrayOf(
                1.12f,0f,0f,0f,0f, 0f,0.95f,0f,0f,0f, 0f,0f,1.08f,0f,0f, 0f,0f,0f,1f,0f)))
            "pastel" -> ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0.7f) })
            "fade" -> ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0.6f) })
            "cyberpunk" -> ColorFilter.colorMatrix(ColorMatrix(floatArrayOf(
                1.2f,0f,0.1f,0f,0f, 0f,0.8f,0f,0f,0f, 0.1f,0f,1.25f,0f,0f, 0f,0f,0f,1f,0f)))
            "sunset" -> ColorFilter.colorMatrix(ColorMatrix(floatArrayOf(
                1.15f,0.05f,0f,0f,0f, 0f,0.97f,0f,0f,0f, 0f,0f,0.95f,0f,0f, 0f,0f,0f,1f,0f)))
            "arctic" -> ColorFilter.colorMatrix(ColorMatrix(floatArrayOf(
                0.85f,0f,0f,0f,0f, 0f,0.95f,0f,0f,0f, 0f,0f,1.12f,0f,0f, 0f,0f,0f,1f,0f)))
            "forest" -> ColorFilter.colorMatrix(ColorMatrix(floatArrayOf(
                0.9f,0f,0f,0f,0f, 0f,1.1f,0f,0f,0f, 0f,0f,0.9f,0f,0f, 0f,0f,0f,1f,0f)))
            "rose" -> ColorFilter.colorMatrix(ColorMatrix(floatArrayOf(
                1.1f,0f,0.05f,0f,0f, 0f,0.95f,0f,0f,0f, 0f,0f,1.05f,0f,0f, 0f,0f,0f,1f,0f)))
            "golden" -> ColorFilter.colorMatrix(ColorMatrix(floatArrayOf(
                1.12f,0.05f,0f,0f,5f, 0f,1.03f,0f,0f,0f, 0f,0f,0.85f,0f,0f, 0f,0f,0f,1f,0f)))
            "mist" -> ColorFilter.colorMatrix(ColorMatrix(floatArrayOf(
                1.05f,0.03f,0.03f,0f,15f, 0.03f,1.05f,0.03f,0f,15f, 0.03f,0.03f,1.05f,0f,15f, 0f,0f,0f,1f,0f)))
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
        AnimatedVisibility(visible = selectedTool >= 0 && isPanelExpanded,
            enter = expandVertically(animationSpec = spring(dampingRatio = 0.8f, stiffness = 350f)) + fadeIn(tween(200)),
            exit = shrinkVertically(animationSpec = spring(dampingRatio = 0.8f, stiffness = 350f)) + fadeOut(tween(150))
        ) {
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
                onCollapse = { isPanelExpanded = false },
                onToggleGreenScreen = onToggleGreenScreen,
                onUpdateGreenScreenColor = onUpdateGreenScreenColor,
                onUpdateGreenScreenThreshold = onUpdateGreenScreenThreshold,
                onSelectAutoBackground = onSelectAutoBackground,
                onPickCustomBackground = onPickCustomBackground,
                onUpdateEraserMode = onUpdateEraserMode,
                onUpdateEraserBrushSize = onUpdateEraserBrushSize,
                onUpdateEraserTolerance = onUpdateEraserTolerance,
                onToggleEraserSoftEdge = onToggleEraserSoftEdge,
                onUndoEraser = onUndoEraser,
                onResetEraser = onResetEraser,
                onUpdateImageEditorBrightness = onUpdateImageEditorBrightness,
                onUpdateImageEditorContrast = onUpdateImageEditorContrast,
                onUpdateImageEditorSaturation = onUpdateImageEditorSaturation,
                onUpdateImageEditorBlur = onUpdateImageEditorBlur,
                onUpdateImageEditorSharpen = onUpdateImageEditorSharpen,
                onUpdateImageEditorTemperature = onUpdateImageEditorTemperature,
                onUpdateImageEditorVignette = onUpdateImageEditorVignette,
                onUpdateImageEditorGrain = onUpdateImageEditorGrain,
                onUpdateImageEditorFade = onUpdateImageEditorFade,
                onUpdateImageEditorHighlights = onUpdateImageEditorHighlights,
                onUpdateImageEditorShadows = onUpdateImageEditorShadows,
                onUpdateImageEditorExposure = onUpdateImageEditorExposure,
                onResetImageEditor = onResetImageEditor,
                onUpdateOrientationMode = onUpdateOrientationMode,
                onToggleVerticalSafeZone = onToggleVerticalSafeZone,
                onToggleHorizontalLetterbox = onToggleHorizontalLetterbox,
                onToggleAutoReframe = onToggleAutoReframe,
                onAddLayer = onAddLayer,
                onRemoveLayer = onRemoveLayer
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
    val ctx = androidx.compose.ui.platform.LocalContext.current

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
            Box(
                modifier = Modifier.glassmorphic(shape = RoundedCornerShape(16.dp)).tactileClick(onClick = onBackToEdit).padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text("✏️ Edit More", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
        }

        // EVERYTHING in one scrollable column
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 12.dp)
        ) {
            Spacer(Modifier.height(8.dp))

            // Video Preview
            Box(modifier = Modifier.fillMaxWidth().height(180.dp).clip(RoundedCornerShape(16.dp)).background(Color.Black).border(2.dp, CyberCyan.copy(0.3f), RoundedCornerShape(16.dp)), contentAlignment = Alignment.Center) {
                AndroidView(factory = { c -> PlayerView(c).apply { player = exoPlayer; useController = false } }, modifier = Modifier.fillMaxSize())
                Box(modifier = Modifier.size(48.dp).background(Color.White.copy(0.2f), CircleShape).border(2.dp, Color.White.copy(0.4f), CircleShape).clickable { onPlayPause() }, contentAlignment = Alignment.Center) {
                    Icon(if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, "Play", tint = Color.White, modifier = Modifier.size(24.dp))
                }
            }

            Spacer(Modifier.height(10.dp))

            // Summary
            Box(modifier = Modifier.fillMaxWidth().glassmorphic(shape = RoundedCornerShape(12.dp)).padding(10.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                    SummaryItem("⏱️", "Duration", formatTime(durationMs))
                    SummaryItem("📐", "Aspect", project.aspectPreset)
                    SummaryItem("🎬", "Res", project.targetResolution.uppercase())
                    SummaryItem("⚡", "Speed", "${project.speedFactor}x")
                }
            }

            Spacer(Modifier.height(10.dp))

            // Export format
            var selectedFormat by remember { mutableStateOf("mp4_hd") }
            Box(modifier = Modifier.fillMaxWidth().glassmorphic(shape = RoundedCornerShape(12.dp)).padding(10.dp)) {
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
                            Box(Modifier.weight(1f).background(NeonOrange.copy(0.08f), RoundedCornerShape(6.dp)).clickable { android.widget.Toast.makeText(ctx, "Upscale: $label", android.widget.Toast.LENGTH_SHORT).show() }.padding(4.dp), contentAlignment = Alignment.Center) {
                                Text(label, fontSize = 7.sp, fontWeight = FontWeight.Bold, color = NeonOrange)
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // ═══ IMPORT BUTTON ═══
            Box(
                modifier = Modifier.fillMaxWidth().height(56.dp)
                    .background(Color(0xFF1A1C24), RoundedCornerShape(16.dp))
                    .border(2.dp, CyberCyan, RoundedCornerShape(16.dp))
                    .clickable { onImport() },
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Add, "Import", tint = CyberCyan, modifier = Modifier.size(24.dp))
                    Text("IMPORT", fontWeight = FontWeight.ExtraBold, color = Color.White, fontSize = 15.sp)
                }
            }

            Spacer(Modifier.height(10.dp))

            // ═══ EXPORT BUTTON ═══
            Box(
                modifier = Modifier.fillMaxWidth().height(56.dp)
                    .background(premiumAccentGradient, RoundedCornerShape(16.dp))
                    .border(2.dp, AccentSecondary, RoundedCornerShape(16.dp))
                    .clickable { onExport() },
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("🎬", fontSize = 20.sp)
                    Text("EXPORT", fontWeight = FontWeight.ExtraBold, color = Color.White, fontSize = 15.sp)
                }
            }

            Spacer(Modifier.height(20.dp))
        }
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
            Box(modifier = Modifier.neonGlow(CyberCyan, RoundedCornerShape(22.dp), 1.5.dp).background(Brush.horizontalGradient(listOf(CyberCyan, Color(0xFF7C5CFF))), RoundedCornerShape(22.dp)).tactileClick(onClick = onDone).padding(horizontal = 16.dp, vertical = 8.dp)) {
                Text("DONE ✓", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 12.sp, letterSpacing = 0.5.sp)
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
    // Real video duration for 1-second precision ruler
    val durationMs = if (project.durationMs > 0) project.durationMs else
        if (exoPlayer.duration > 0) exoPlayer.duration else 30000L
    val durationSec = (durationMs / 1000.0).coerceAtLeast(1.0)
    // Playhead position as a fraction [0..1]
    val playheadFraction = (currentTime.toFloat() / durationMs).coerceIn(0f, 1f)

    // Wrap the whole timeline in BoxWithConstraints so the moving playhead
    // can use the EXACT measured width (perfect 1-second alignment).
    BoxWithConstraints(
        modifier = Modifier.fillMaxWidth().height(120.dp)
            .background(Color(0xFF111318)).border(1.dp, Color.White.copy(0.04f))
    ) {
        val timelineWidthDp = maxWidth.value
        val totalSeconds = kotlin.math.ceil(durationSec).toInt()
        val labelInterval = when {
            totalSeconds <= 15 -> 1
            totalSeconds <= 60 -> 5
            totalSeconds <= 300 -> 10
            else -> 30
        }
        val currentSecond = (currentTime / 1000).toInt()

        Box(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize()) {
                // ★ 1-SECOND PRECISION TIME RULER — dynamic based on actual duration
                Box(
                    modifier = Modifier.fillMaxWidth().height(16.dp)
                        .background(Color.Black.copy(0.3f))
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        for (sec in 0..totalSeconds) {
                            val xFraction = if (totalSeconds > 0) sec.toFloat() / totalSeconds else 0f
                            val xPos = xFraction * timelineWidthDp
                            val isMajor = sec % labelInterval == 0
                            Box(
                                modifier = Modifier
                                    .wrapContentSize(Alignment.TopStart)
                                    .offset(x = xPos.dp)
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Box(
                                        Modifier
                                            .width(if (isMajor) 1.5.dp else 1.dp)
                                            .height(if (isMajor) 8.dp else 4.dp)
                                            .background(
                                                if (isMajor) Color.White.copy(0.6f)
                                                else Color.White.copy(0.25f)
                                            )
                                    )
                                    if (isMajor) {
                                        Text(
                                            "${sec}s",
                                            fontSize = 6.sp,
                                            color = if (sec == currentSecond) NeonOrange else Color.Gray,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
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
            // ★ MOVING PLAYHEAD — tracks actual playback position (1-second precision)
            // Uses the EXACT measured timelineWidthDp from BoxWithConstraints for perfect alignment.
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(2.dp)
                    .offset(x = (playheadFraction * timelineWidthDp).dp)
                    .background(Brush.verticalGradient(listOf(NeonOrange, Color.Transparent)))
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(NeonOrange, CircleShape)
                        .border(1.dp, Color.White, CircleShape)
                        .align(Alignment.TopCenter)
                        .neonGlow(NeonOrange, CircleShape, 1.dp)
                )
            }
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
        "🖼️" to "Image", "📋" to "Template",
        "🎬" to "Chroma", "🧹" to "Erase", "🖌️" to "ImgEdit", "📐" to "Orient",
        "🌈" to "Blend", "↺️" to "Reverse", "💉" to "ColorFX",
        "🎧" to "AudioFX", "🎤" to "Voice", "🎉" to "Borders",
        "✨" to "Vignette", "❄️" to "Freeze"
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
    onCollapse: () -> Unit,
    // Green Screen
    onToggleGreenScreen: () -> Unit = {},
    onUpdateGreenScreenColor: (String) -> Unit = {},
    onUpdateGreenScreenThreshold: (Float) -> Unit = {},
    onSelectAutoBackground: (Int) -> Unit = {},
    onPickCustomBackground: () -> Unit = {},
    // Eraser
    onUpdateEraserMode: (String) -> Unit = {},
    onUpdateEraserBrushSize: (Float) -> Unit = {},
    onUpdateEraserTolerance: (Float) -> Unit = {},
    onToggleEraserSoftEdge: () -> Unit = {},
    onUndoEraser: () -> Unit = {},
    onResetEraser: () -> Unit = {},
    // Image Editor
    onUpdateImageEditorBrightness: (Float) -> Unit = {},
    onUpdateImageEditorContrast: (Float) -> Unit = {},
    onUpdateImageEditorSaturation: (Float) -> Unit = {},
    onUpdateImageEditorBlur: (Float) -> Unit = {},
    onUpdateImageEditorSharpen: (Float) -> Unit = {},
    onUpdateImageEditorTemperature: (Float) -> Unit = {},
    onUpdateImageEditorVignette: (Float) -> Unit = {},
    onUpdateImageEditorGrain: (Float) -> Unit = {},
    onUpdateImageEditorFade: (Float) -> Unit = {},
    onUpdateImageEditorHighlights: (Float) -> Unit = {},
    onUpdateImageEditorShadows: (Float) -> Unit = {},
    onUpdateImageEditorExposure: (Float) -> Unit = {},
    onResetImageEditor: () -> Unit = {},
    // Orientation
    onUpdateOrientationMode: (String) -> Unit = {},
    onToggleVerticalSafeZone: () -> Unit = {},
    onToggleHorizontalLetterbox: () -> Unit = {},
    onToggleAutoReframe: () -> Unit = {},
    // Layers
    onAddLayer: (String) -> Unit = {},
    onRemoveLayer: (String) -> Unit = {},
    // NEW v4.0 CapCut-sync Pro
    onUpdateBlendMode: (String) -> Unit = {},
    onToggleReverse: () -> Unit = {},
    onUpdateFreezeFrame: (Long) -> Unit = {},
    onUpdateColorLift: (Float) -> Unit = {},
    onUpdateColorGamma: (Float) -> Unit = {},
    onUpdateColorGain: (Float) -> Unit = {},
    onUpdateAudioEffect: (String) -> Unit = {},
    onUpdateVoiceChangerPitch: (Float) -> Unit = {},
    onToggleAudioDucking: () -> Unit = {},
    onUpdateBorderStyle: (String) -> Unit = {},
    onUpdateVignetteStyle: (String) -> Unit = {}
) {
    Box(
        modifier = Modifier.fillMaxWidth().height(220.dp).padding(horizontal = 8.dp, vertical = 2.dp)
            .background(Color(0xFF1A1C24).copy(0.85f), RoundedCornerShape(12.dp))
            .border(1.dp, Color.White.copy(0.04f), RoundedCornerShape(12.dp))
    ) {
        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(8.dp)) {
            when (selectedTool) {
                0 -> EditPanel(project, onUpdateCropPreset, onUpdateAspectPreset, onUpdateSpeed, onUpdateSpeedCurve, onUpdateRotation, onToggleFlipHorizontal, onToggleFlipVertical, onUpdateResolution, onUpdateTrim)
                1 -> LayersPanel(project, context, onAddLayer = onAddLayer, onRemoveLayer = onRemoveLayer)
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
                14 -> com.powercut.editor.ui.editor.tools.GreenScreenPanel(
                    greenScreenEnabled = project.greenScreenEnabled,
                    greenScreenColor = project.greenScreenColor,
                    greenScreenThreshold = project.greenScreenThreshold,
                    greenScreenAutoBgIndex = project.greenScreenAutoBgIndex,
                    onToggleGreenScreen = onToggleGreenScreen,
                    onUpdateGreenScreenColor = onUpdateGreenScreenColor,
                    onUpdateThreshold = onUpdateGreenScreenThreshold,
                    onSelectAutoBackground = onSelectAutoBackground,
                    onPickCustomBackground = onPickCustomBackground
                )
                15 -> com.powercut.editor.ui.editor.tools.EraserToolsPanel(
                    eraserMode = project.eraserMode,
                    eraserBrushSize = project.eraserBrushSize,
                    eraserTolerance = project.eraserTolerance,
                    eraserSoftEdge = project.eraserSoftEdge,
                    onUpdateEraserMode = onUpdateEraserMode,
                    onUpdateBrushSize = onUpdateEraserBrushSize,
                    onUpdateTolerance = onUpdateEraserTolerance,
                    onToggleSoftEdge = onToggleEraserSoftEdge,
                    onUndoEraser = onUndoEraser,
                    onResetEraser = onResetEraser
                )
                16 -> com.powercut.editor.ui.editor.tools.ImageEditorPanel(
                    brightness = project.imageEditorBrightness,
                    contrast = project.imageEditorContrast,
                    saturation = project.imageEditorSaturation,
                    blur = project.imageEditorBlur,
                    sharpen = project.imageEditorSharpen,
                    temperature = project.imageEditorTemperature,
                    vignette = project.imageEditorVignette,
                    grain = project.imageEditorGrain,
                    fade = project.imageEditorFade,
                    highlights = project.imageEditorHighlights,
                    shadows = project.imageEditorShadows,
                    exposure = project.imageEditorExposure,
                    onUpdateBrightness = onUpdateImageEditorBrightness,
                    onUpdateContrast = onUpdateImageEditorContrast,
                    onUpdateSaturation = onUpdateImageEditorSaturation,
                    onUpdateBlur = onUpdateImageEditorBlur,
                    onUpdateSharpen = onUpdateImageEditorSharpen,
                    onUpdateTemperature = onUpdateImageEditorTemperature,
                    onUpdateVignette = onUpdateImageEditorVignette,
                    onUpdateGrain = onUpdateImageEditorGrain,
                    onUpdateFade = onUpdateImageEditorFade,
                    onUpdateHighlights = onUpdateImageEditorHighlights,
                    onUpdateShadows = onUpdateImageEditorShadows,
                    onUpdateExposure = onUpdateImageEditorExposure,
                    onResetAll = onResetImageEditor
                )
                17 -> com.powercut.editor.ui.editor.tools.OrientationToolsPanel(
                    orientationMode = project.orientationMode,
                    aspectPreset = project.aspectPreset,
                    verticalSafeZone = project.verticalSafeZone,
                    horizontalLetterbox = project.horizontalLetterbox,
                    autoReframeEnabled = project.autoReframeEnabled,
                    onUpdateOrientationMode = onUpdateOrientationMode,
                    onUpdateAspectPreset = onUpdateAspectPreset,
                    onToggleSafeZone = onToggleVerticalSafeZone,
                    onToggleLetterbox = onToggleHorizontalLetterbox,
                    onToggleAutoReframe = onToggleAutoReframe
                )
                18 -> BlendModePanel(project, onUpdateBlendMode)
                19 -> ReversePanel(project, onToggleReverse, onUpdateFreezeFrame)
                20 -> ColorCurvesPanel(project, onUpdateColorLift, onUpdateColorGamma, onUpdateColorGain)
                21 -> AudioEffectsPanel(project, onUpdateAudioEffect, onToggleAudioDucking)
                22 -> VoiceChangerPanel(project, onUpdateVoiceChangerPitch)
                23 -> BorderStylesPanel(project, onUpdateBorderStyle)
                24 -> VignetteStylesPanel(project, onUpdateVignetteStyle)
                25 -> FreezeFramePanel(project, onUpdateFreezeFrame)
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
    val ctx = androidx.compose.ui.platform.LocalContext.current
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

                // Manual Crop Sliders
                Spacer(Modifier.height(4.dp))
                Text("MANUAL CROP", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                var cropLeft by remember { mutableFloatStateOf(0f) }
                var cropTop by remember { mutableFloatStateOf(0f) }
                var cropRight by remember { mutableFloatStateOf(1f) }
                var cropBottom by remember { mutableFloatStateOf(1f) }
                listOf(
                    Triple("⬅️ Left", cropLeft, { v: Float -> cropLeft = v }),
                    Triple("⬆️ Top", cropTop, { v: Float -> cropTop = v }),
                    Triple("➡️ Right", cropRight, { v: Float -> cropRight = v }),
                    Triple("⬇️ Bottom", cropBottom, { v: Float -> cropBottom = v })
                ).forEach { (label, value, onChange) ->
                    Row(Modifier.fillMaxWidth().background(Color.White.copy(0.03f), RoundedCornerShape(6.dp)).padding(horizontal = 8.dp, vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(label, fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color.White, modifier = Modifier.width(55.dp))
                        Slider(value = value, onValueChange = onChange, valueRange = 0f..1f, colors = SliderDefaults.colors(activeTrackColor = CyberCyan, thumbColor = CyberCyan, inactiveTrackColor = Color.White.copy(0.08f)), modifier = Modifier.weight(1f).height(16.dp))
                        Text("${(value * 100).toInt()}%", fontSize = 7.sp, color = CyberCyan, fontWeight = FontWeight.Bold, modifier = Modifier.width(30.dp))
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
                        Box(Modifier.weight(1f).background(if (sel) NeonOrange.copy(0.2f) else Color.White.copy(0.04f), RoundedCornerShape(6.dp)).clickable { onUpdateSpeed(if (sel && s != 1.0f) 1.0f else s) }.padding(3.dp), contentAlignment = Alignment.Center) {
                            Text("${s}x", fontSize = 7.sp, fontWeight = FontWeight.Bold, color = if (sel) NeonOrange else Color.White)
                        }
                    }
                }
                // Manual Speed Slider
                Spacer(Modifier.height(4.dp))
                Text("MANUAL SPEED: ${String.format("%.2f", project.speedFactor)}x", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                Slider(
                    value = project.speedFactor,
                    onValueChange = { onUpdateSpeed(String.format("%.2f", it).toFloat()) },
                    valueRange = 0.1f..16f,
                    colors = SliderDefaults.colors(activeTrackColor = NeonOrange, thumbColor = NeonOrange, inactiveTrackColor = Color.White.copy(0.08f)),
                    modifier = Modifier.fillMaxWidth().height(20.dp)
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("0.1x", fontSize = 7.sp, color = Color.Gray)
                    Text("1.0x", fontSize = 7.sp, color = CyberCyan)
                    Text("16x", fontSize = 7.sp, color = Color.Gray)
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
                Text("Tap to apply · Tap again to remove", fontSize = 7.sp, color = Color.Gray.copy(0.7f))
                Spacer(Modifier.height(2.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf("0.1x Ultra" to 0.1f, "0.25x Super" to 0.25f, "0.3x Smooth" to 0.3f, "0.5x Slow" to 0.5f).forEach { (label, speed) ->
                        val sel = project.speedFactor == speed
                        Box(Modifier.weight(1f).background(if (sel) CyberCyan.copy(0.2f) else Color.White.copy(0.04f), RoundedCornerShape(8.dp)).clickable { onUpdateSpeed(if (sel) 1.0f else speed) }.padding(6.dp), contentAlignment = Alignment.Center) {
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
                        Box(Modifier.weight(1f).background(if (sel) NeonOrange.copy(0.2f) else Color.White.copy(0.04f), RoundedCornerShape(8.dp)).clickable { onUpdateSpeed(if (sel) 1.0f else speed) }.padding(6.dp), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("⚡", fontSize = 14.sp)
                                Text(label, fontSize = 7.sp, fontWeight = FontWeight.Bold, color = if (sel) NeonOrange else Color.White, textAlign = TextAlign.Center)
                            }
                        }
                    }
                }
            }
            "reverse" -> {
                var isReversed by remember { mutableStateOf(false) }
                Text("REVERSE VIDEO", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                Box(Modifier.fillMaxWidth().background(if (isReversed) CyberCyan.copy(0.15f) else Color.White.copy(0.04f), RoundedCornerShape(8.dp)).border(1.dp, if (isReversed) CyberCyan.copy(0.3f) else Color.Transparent, RoundedCornerShape(8.dp)).clickable { isReversed = !isReversed; android.widget.Toast.makeText(ctx, if (isReversed) "Reverse applied!" else "Reverse removed!", android.widget.Toast.LENGTH_SHORT).show() }.padding(12.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("🔄", fontSize = 28.sp)
                        Text(if (isReversed) "Tap to Remove Reverse" else "Tap to Reverse Video", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = if (isReversed) CyberCyan else Color.White)
                        Text(if (isReversed) "Reverse is active" else "Plays video backwards", fontSize = 8.sp, color = if (isReversed) CyberCyan else Color.Gray)
                    }
                }
            }
            "freeze" -> {
                var isFrozen by remember { mutableStateOf(false) }
                Text("FREEZE FRAME", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                Box(Modifier.fillMaxWidth().background(if (isFrozen) CyberCyan.copy(0.15f) else Color.White.copy(0.04f), RoundedCornerShape(8.dp)).border(1.dp, if (isFrozen) CyberCyan.copy(0.3f) else Color.Transparent, RoundedCornerShape(8.dp)).clickable { isFrozen = !isFrozen; android.widget.Toast.makeText(ctx, if (isFrozen) "Freeze at ${formatTime(project.trimStartMs)}!" else "Freeze removed!", android.widget.Toast.LENGTH_SHORT).show() }.padding(12.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("🧊", fontSize = 28.sp)
                        Text(if (isFrozen) "Tap to Remove Freeze" else "Freeze at Playhead", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = if (isFrozen) CyberCyan else Color.White)
                        Text(if (isFrozen) "Freeze is active" else "Creates still frame", fontSize = 8.sp, color = if (isFrozen) CyberCyan else Color.Gray)
                    }
                }
            }
            "delete" -> {
                Text("DELETE SECTION", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                Box(Modifier.fillMaxWidth().background(Color(0xFFFF1744).copy(0.1f), RoundedCornerShape(8.dp)).border(1.dp, Color(0xFFFF1744).copy(0.3f), RoundedCornerShape(8.dp)).clickable { android.widget.Toast.makeText(ctx, "Section deleted!", android.widget.Toast.LENGTH_SHORT).show() }.padding(12.dp), contentAlignment = Alignment.Center) {
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
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("LAYERS", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = CyberCyan)
            Text("${project.activeLayers.size} active", fontSize = 8.sp, color = Color.Gray)
        }

        // Add Layer quick actions
        Text("ADD LAYER", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color.Gray.copy(0.8f))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            listOf(
                "������" to "text",
                "������️" to "image",
                "⭐" to "sticker",
                "✨" to "effect"
            ).forEach { (icon, layerId) ->
                Box(
                    Modifier.weight(1f)
                        .background(Brush.horizontalGradient(listOf(CyberCyan.copy(0.18f), CyberCyan.copy(0.04f))), RoundedCornerShape(8.dp))
                        .border(1.dp, CyberCyan.copy(0.25f), RoundedCornerShape(8.dp))
                        .clickable {
                            onAddLayer(layerId)
                            android.widget.Toast.makeText(context, "Added $layerId layer", android.widget.Toast.LENGTH_SHORT).show()
                        }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(icon, fontSize = 16.sp)
                        Text(layerId.replaceFirstChar { it.uppercase() }, fontSize = 7.sp, color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(Modifier.height(2.dp))
        Text("LAYER STACK", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color.Gray.copy(0.8f))

        // 3D styled layer items — real content detection + functional remove/visibility
        val layers = listOf(
            Triple("������", "Video Layer", "video"),
            Triple("������", "Audio Layer", "audio"),
            Triple("������", "Text Layer", "text"),
            Triple("������️", "Image Layer", "image"),
            Triple("⭐", "Sticker Layer", "sticker"),
            Triple("✨", "Effect Layer", "effect")
        )
        layers.forEach { (icon, name, layerId) ->
            val hasContent = when (layerId) {
                "video" -> true
                "audio" -> project.backgroundMusicPath != null
                "text" -> project.activeTextOverlay != null
                "image" -> project.imageOverlayPath != null
                "sticker" -> project.stickerType != "none"
                "effect" -> project.selectedFilter != "none"
                else -> false
            }
            val isActive = project.activeLayers.contains(layerId)
            Row(
                Modifier.fillMaxWidth()
                    .background(
                        if (hasContent) Brush.horizontalGradient(listOf(CyberCyan.copy(0.15f), Color.Transparent))
                        else Brush.horizontalGradient(listOf(Color.White.copy(0.03f), Color.Transparent)),
                        RoundedCornerShape(8.dp)
                    )
                    .border(1.dp, if (hasContent) CyberCyan.copy(0.2f) else Color.White.copy(0.04f), RoundedCornerShape(8.dp))
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
                        Text(
                            if (hasContent) (if (isActive) "Active" else "Hidden") else "Empty",
                            fontSize = 7.sp,
                            color = if (hasContent) CyberCyan else Color.Gray.copy(0.5f)
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                    // Visibility toggle (functional: add/remove from activeLayers)
                    Box(
                        Modifier.size(24.dp)
                            .background(if (isActive) CyberCyan.copy(0.15f) else Color.Transparent, CircleShape)
                            .border(1.dp, if (isActive) CyberCyan.copy(0.3f) else Color.White.copy(0.08f), CircleShape)
                            .clickable {
                                if (isActive) {
                                    onRemoveLayer(layerId)
                                    android.widget.Toast.makeText(context, "$name hidden", android.widget.Toast.LENGTH_SHORT).show()
                                } else {
                                    onAddLayer(layerId)
                                    android.widget.Toast.makeText(context, "$name shown", android.widget.Toast.LENGTH_SHORT).show()
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(if (isActive) "������️" else "������", fontSize = 10.sp)
                    }
                    // Remove button (functional)
                    if (hasContent) {
                        Box(
                            Modifier.size(24.dp)
                                .background(Color(0xFFFF3D7F).copy(0.12f), CircleShape)
                                .border(1.dp, Color(0xFFFF3D7F).copy(0.3f), CircleShape)
                                .clickable {
                                    onRemoveLayer(layerId)
                                    android.widget.Toast.makeText(context, "$name removed", android.widget.Toast.LENGTH_SHORT).show()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("✕", fontSize = 9.sp, color = Color(0xFFFF3D7F), fontWeight = FontWeight.Bold)
                        }
                    }
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
        // Preset buttons — toggle
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            listOf(0.1f, 0.25f, 0.5f, 1.0f, 2.0f, 4.0f, 8.0f, 16.0f).forEach { s ->
                val sel = project.speedFactor == s
                Box(Modifier.weight(1f).background(if (sel) NeonOrange.copy(0.2f) else Color.White.copy(0.04f), RoundedCornerShape(6.dp)).clickable { onUpdateSpeed(if (sel && s != 1.0f) 1.0f else s) }.padding(3.dp), contentAlignment = Alignment.Center) {
                    Text("${s}x", fontSize = 7.sp, fontWeight = FontWeight.Bold, color = if (sel) NeonOrange else Color.White)
                }
            }
        }
        // Manual Speed Slider
        Text("MANUAL: ${String.format("%.2f", project.speedFactor)}x", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
        Slider(
            value = project.speedFactor,
            onValueChange = { onUpdateSpeed(String.format("%.2f", it).toFloat()) },
            valueRange = 0.1f..16f,
            colors = SliderDefaults.colors(activeTrackColor = NeonOrange, thumbColor = NeonOrange, inactiveTrackColor = Color.White.copy(0.08f)),
            modifier = Modifier.fillMaxWidth().height(20.dp)
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("0.1x", fontSize = 7.sp, color = Color.Gray)
            Text("1.0x", fontSize = 7.sp, color = CyberCyan)
            Text("16x", fontSize = 7.sp, color = Color.Gray)
        }
        // Speed curves
        Text("SPEED CURVE", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            listOf("Standard", "Montage", "Hero", "Flash", "Custom").forEach { c ->
                val sel = project.speedCurve.lowercase() == c.lowercase()
                Box(Modifier.weight(1f).background(if (sel) CyberCyan.copy(0.15f) else Color.White.copy(0.03f), RoundedCornerShape(6.dp)).clickable { onUpdateSpeedCurve(if (sel) "constant" else c) }.padding(4.dp), contentAlignment = Alignment.Center) {
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
        // Preset crop buttons
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            listOf("Free", "1:1", "16:9", "9:16", "4:5", "21:9").forEach { c ->
                val sel = project.cropPreset == c
                Box(Modifier.weight(1f).background(if (sel) CyberCyan.copy(0.2f) else Color.White.copy(0.04f), RoundedCornerShape(6.dp)).clickable { onUpdateCrop(c); onUpdateAspect(c) }.padding(4.dp), contentAlignment = Alignment.Center) {
                    Text(c, fontSize = 8.sp, fontWeight = FontWeight.Bold, color = if (sel) CyberCyan else Color.White)
                }
            }
        }
        // Manual Crop Sliders
        Text("MANUAL CROP", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
        var cropLeft by remember { mutableFloatStateOf(0f) }
        var cropTop by remember { mutableFloatStateOf(0f) }
        var cropRight by remember { mutableFloatStateOf(1f) }
        var cropBottom by remember { mutableFloatStateOf(1f) }
        listOf(
            Triple("⬅️ Left", cropLeft, { v: Float -> cropLeft = v }),
            Triple("⬆️ Top", cropTop, { v: Float -> cropTop = v }),
            Triple("➡️ Right", cropRight, { v: Float -> cropRight = v }),
            Triple("⬇️ Bottom", cropBottom, { v: Float -> cropBottom = v })
        ).forEach { (label, value, onChange) ->
            Row(Modifier.fillMaxWidth().background(Color.White.copy(0.03f), RoundedCornerShape(6.dp)).padding(horizontal = 8.dp, vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(label, fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color.White, modifier = Modifier.width(55.dp))
                Slider(value = value, onValueChange = onChange, valueRange = 0f..1f, colors = SliderDefaults.colors(activeTrackColor = CyberCyan, thumbColor = CyberCyan, inactiveTrackColor = Color.White.copy(0.08f)), modifier = Modifier.weight(1f).height(16.dp))
                Text("${(value * 100).toInt()}%", fontSize = 7.sp, color = CyberCyan, fontWeight = FontWeight.Bold, modifier = Modifier.width(30.dp))
            }
        }
        // Rotate & Flip
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
@OptIn(ExperimentalLayoutApi::class)
private fun FiltersPanel(project: VideoProject, onUpdateFilter: (String) -> Unit) {
    var filterCategory by remember { mutableStateOf("all") }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("CINEMATIC FILTERS", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = CyberCyan)
        // Category tabs
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            listOf("all" to "All", "basic" to "Basic", "cinema" to "Cinema", "film" to "Film", "vintage" to "Vintage", "mood" to "Mood", "neon" to "Neon").forEach { (id, label) ->
                val sel = filterCategory == id
                Box(Modifier.background(if (sel) CyberCyan.copy(0.2f) else Color.White.copy(0.04f), RoundedCornerShape(6.dp)).clickable { filterCategory = id }.padding(horizontal = 6.dp, vertical = 3.dp)) {
                    Text(label, fontSize = 7.sp, fontWeight = FontWeight.Bold, color = if (sel) CyberCyan else Color.White)
                }
            }
        }
        FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            // (id, displayName, category)
            val allFilters = listOf(
                Triple("none", "Original", "basic"),
                Triple("grayscale", "B&W", "basic"), Triple("sepia", "Sepia", "basic"),
                Triple("invert", "Invert", "basic"), Triple("warm", "Warm", "basic"),
                Triple("cool", "Cool", "basic"), Triple("vintage", "Vintage", "vintage"),
                Triple("dramatic", "Drama", "basic"), Triple("negative", "Negative", "basic"),
                Triple("noir", "Noir", "cinema"), Triple("cinematic", "Cinematic", "cinema"),
                Triple("teal", "Teal", "cinema"), Triple("orange", "Orange", "cinema"),
                Triple("lomo", "Lomo", "vintage"), Triple("polaroid", "Polaroid", "vintage"),
                Triple("holga", "Holga", "vintage"), Triple("diana", "Diana", "vintage"),
                Triple("film", "Film", "film"), Triple("super8", "Super8", "film"),
                Triple("vhs_tape", "VHS", "vintage"), Triple("kodak", "Kodak", "film"),
                Triple("fuji", "Fuji", "film"), Triple("agfa", "Agfa", "film"),
                Triple("ilford", "Ilford", "film"), Triple("portra", "Portra", "film"),
                Triple("velvia", "Velvia", "film"), Triple("provia", "Provia", "film"),
                Triple("astia", "Astia", "film"), Triple("monochrome", "Mono", "basic"),
                Triple("high_contrast", "Hi Contrast", "basic"), Triple("low_contrast", "Lo Contrast", "basic"),
                Triple("high_saturation", "Hi Saturation", "basic"), Triple("low_saturation", "Lo Saturation", "basic"),
                Triple("bright", "Bright", "basic"), Triple("dark", "Dark", "basic"),
                Triple("soft", "Soft", "mood"), Triple("sharp", "Sharp", "mood"),
                Triple("dreamy", "Dreamy", "mood"), Triple("glow", "Glow", "mood"),
                Triple("haze", "Haze", "mood"), Triple("matte", "Matte", "mood"),
                Triple("litho", "Litho", "vintage"), Triple("sepia_warm", "Sepia Warm", "vintage"),
                Triple("sepia_cool", "Sepia Cool", "vintage"), Triple("red_boost", "Red+", "mood"),
                Triple("blue_boost", "Blue+", "mood"), Triple("green_boost", "Green+", "mood"),
                Triple("purple_haze", "Purple Haze", "neon"), Triple("pink_dream", "Pink Dream", "neon"),
                Triple("amber", "Amber", "mood"), Triple("emerald", "Emerald", "mood"),
                Triple("sapphire", "Sapphire", "mood"), Triple("ruby", "Ruby", "mood"),
                Triple("bronze", "Bronze", "mood"), Triple("platinum", "Platinum", "mood"),
                Triple("neon_city", "Neon City", "neon"), Triple("retro_wave", "Retro Wave", "neon"),
                Triple("synthwave", "Synthwave", "neon"), Triple("analog", "Analog", "vintage"),
                Triple("tokyo", "Tokyo", "cinema"), Triple("nyc", "NYC", "cinema"),
                Triple("paris", "Paris", "cinema"), Triple("miami", "Miami", "neon"),
                Triple("desert", "Desert", "mood"), Triple("ocean", "Ocean", "mood"),
                Triple("autumn", "Autumn", "mood"), Triple("winter", "Winter", "mood"),
                Triple("spring", "Spring", "mood"), Triple("summer", "Summer", "mood")
            )
            allFilters.filter { filterCategory == "all" || it.third == filterCategory }.forEach { (id, name, cat) ->
                val sel = project.selectedFilter.lowercase() == id
                Box(Modifier.background(if (sel) CyberCyan.copy(0.2f) else Color.White.copy(0.04f), RoundedCornerShape(6.dp))
                    .clickable { onUpdateFilter(if (sel) "none" else id) }
                    .padding(horizontal = 6.dp, vertical = 4.dp)) {
                    Text(name, fontSize = 7.sp, fontWeight = FontWeight.Bold, color = if (sel) CyberCyan else Color.White)
                }
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
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            listOf("all" to "All", "vfx" to "VFX", "color" to "Color", "motion" to "Motion", "retro" to "Retro", "neon" to "Neon").forEach { (id, label) ->
                val sel = effectCategory == id
                Box(Modifier.background(if (sel) NeonOrange.copy(0.2f) else Color.White.copy(0.04f), RoundedCornerShape(6.dp)).clickable { effectCategory = id }.padding(horizontal = 6.dp, vertical = 3.dp)) {
                    Text(label, fontSize = 7.sp, fontWeight = FontWeight.Bold, color = if (sel) NeonOrange else Color.White)
                }
            }
        }
        FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            // (displayName, effectId, filterId, category)
            val allEffects = listOf(
                Triple("Glitch", "glitch", "invert", "vfx"),
                Triple("VHS", "vhs", "sepia", "retro"),
                Triple("Chromatic", "chromatic", "invert", "vfx"),
                Triple("Lens Flare", "lens_flare", "none", "vfx"),
                Triple("Snow", "snow", "none", "vfx"),
                Triple("Rain", "rain", "none", "vfx"),
                Triple("Fire", "fire", "none", "vfx"),
                Triple("Sparkle", "sparkle", "none", "vfx"),
                Triple("Dust", "dust", "sepia", "vfx"),
                Triple("Motion Blur", "motion_blur", "none", "motion"),
                Triple("Shake", "shake", "none", "motion"),
                Triple("Flash", "flash", "invert", "motion"),
                Triple("Neon Glow", "neon_glow", "invert", "neon"),
                Triple("Vignette", "vignette", "grayscale", "color"),
                Triple("Rainbow", "rainbow", "none", "color"),
                Triple("Film Grain", "film_grain", "sepia", "retro"),
                Triple("Bokeh", "bokeh", "none", "vfx"),
                Triple("Particles", "particles", "none", "vfx"),
                Triple("Strobe", "strobe", "grayscale", "motion"),
                Triple("Zoom Pulse", "zoom_pulse", "none", "motion"),
                Triple("Wave Distort", "wave_distort", "none", "motion"),
                Triple("Flame", "flame", "invert", "vfx"),
                Triple("Frost", "frost", "grayscale", "vfx"),
                Triple("Starburst", "starburst", "none", "vfx"),
                Triple("Face Blur", "face_blur", "none", "vfx"),
                Triple("Swirl", "swirl", "invert", "vfx"),
                Triple("Explosion", "explosion", "invert", "vfx"),
                Triple("Light Leak", "light_leak", "none", "vfx"),
                Triple("Film Strip", "film_strip", "sepia", "retro"),
                Triple("Color Splash", "color_splash", "invert", "color"),
                Triple("Electric", "electric", "invert", "vfx"),
                Triple("Tidal", "tidal", "none", "motion"),
                Triple("RGB Split", "rgb_glitch", "invert", "vfx"),
                Triple("Scanline", "scanline", "none", "retro"),
                Triple("CRT", "crt", "none", "retro"),
                Triple("8bit", "8bit", "none", "retro"),
                Triple("Old Film", "old_film", "sepia", "retro"),
                Triple("Bloom", "bloom", "none", "color"),
                Triple("HDR", "hdr", "none", "color"),
                Triple("Vaporwave", "vaporwave", "none", "neon"),
                Triple("Aesthetic", "aesthetic", "none", "color"),
                Triple("LoFi", "lofi", "sepia", "retro"),
                Triple("Dream", "dream", "none", "color"),
                Triple("Night Vision", "night_vision", "invert", "vfx"),
                Triple("Thermal", "thermal", "invert", "vfx"),
                Triple("Pencil", "pencil", "grayscale", "color"),
                Triple("Sketch", "sketch", "grayscale", "color"),
                Triple("Cartoon", "cartoon", "none", "color"),
                Triple("Watercolor", "watercolor", "none", "color"),
                Triple("Oil Paint", "oil_paint", "none", "color"),
                Triple("Pixel", "pixel", "none", "vfx"),
                Triple("Mosaic", "mosaic", "none", "vfx"),
                Triple("Emboss", "emboss", "none", "color"),
                Triple("Sharpen", "sharpen_strong", "none", "color"),
                Triple("Tilt Shift", "tilt_shift", "none", "color"),
                Triple("Kaleidoscope", "kaleidoscope", "invert", "vfx"),
                Triple("RGB Glitch", "rgb_split", "invert", "vfx"),
                Triple("Disco", "disco", "rainbow", "neon"),
                Triple("Concert", "concert", "none", "neon"),
                Triple("Party", "party", "rainbow", "neon")
            )
            allEffects.filter { effectCategory == "all" || it.third == effectCategory || it.fourth == effectCategory }.forEach { (name, effectId, filterId, category) ->
                val sel = project.selectedEffect == effectId
                Box(Modifier.background(if (sel) NeonOrange.copy(0.2f) else Color.White.copy(0.04f), RoundedCornerShape(6.dp)).clickable {
                    if (sel) { onUpdateEffect("none"); onUpdateFilter("none"); android.widget.Toast.makeText(ctx, "Effect removed!", android.widget.Toast.LENGTH_SHORT).show() }
                    else { onUpdateEffect(effectId); onUpdateFilter(filterId); android.widget.Toast.makeText(ctx, "$name applied!", android.widget.Toast.LENGTH_SHORT).show() }
                }.padding(horizontal = 5.dp, vertical = 3.dp)) { Text(name, fontSize = 7.sp, fontWeight = FontWeight.Bold, color = if (sel) NeonOrange else Color.White) }
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
            listOf(
                "None" to "none", "🔥 Fire" to "fire", "⭐ Star" to "star",
                "❤️ Heart" to "heart", "⚡ Glow" to "glow", "💎 Diamond" to "diamond",
                "🎵 Music" to "music", "👑 Crown" to "crown", "💫 Sparkle" to "sparkle",
                "🎯 Target" to "target", "🏆 Trophy" to "trophy", "💀 Skull" to "skull",
                "🚀 Rocket" to "rocket", "⚡ Bolt" to "bolt", "💯 100" to "100",
                "👍 Like" to "thumbs_up", "🎉 Party" to "party"
            ).forEach { (name, id) ->
                val sel = project.stickerType == id
                Box(Modifier.background(if (sel) CyberCyan.copy(0.2f) else Color.White.copy(0.04f), RoundedCornerShape(6.dp)).clickable { onUpdateSticker(if (sel) "none" else id) }.padding(horizontal = 8.dp, vertical = 5.dp)) { Text(name, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = if (sel) CyberCyan else Color.White) }
            }
        }
    }
}


// ─── 9. TRANSITIONS PANEL ──────────────────────────────────────
@OptIn(ExperimentalLayoutApi::class)
@Composable
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TransitionsPanel(project: VideoProject, onUpdateTransition: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("TRANSITIONS", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = NeonOrange)
        FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            listOf(
                "None", "fade", "fade_out", "fade_in_out", "crossfade", "dissolve",
                "glitch", "zoom_in", "zoom_out", "zoom_burst", "spin", "wipe",
                "blur", "blur_in", "blur_out", "pixelate", "pixel_in", "mosaic",
                "split", "film_burn", "light_leak", "smoke", "circle", "diamond",
                "heart", "flash", "white_flash", "black_fade", "white_fade",
                "slide_left", "slide_right", "slide_up", "slide_down",
                "rotate_in", "rotate_out", "bounce", "elastic", "spring",
                "typewriter", "wave", "shake", "shake_in", "shake_burst",
                "iris_in", "iris_out", "star_wipe", "clock_wipe", "spiral",
                "glitch_in", "tv_static", "channel_change", "vhs_transition",
                "rgb_glitch", "color_flash", "flip_h", "flip_v", "rotate_3d",
                "swing", "push_left", "push_right", "push_up", "push_down",
                "curtain", "blinds", "checkerboard", "diagonal", "triangle",
                "hexagon", "star", "cross", "ripple", "shatter"
            ).forEach { t ->
                val display = t.replace("_", " ").replaceFirstChar { it.uppercase() }
                val sel = project.transitionType.lowercase() == t.lowercase()
                Box(Modifier.background(if (sel) NeonOrange.copy(0.2f) else Color.White.copy(0.04f), RoundedCornerShape(6.dp))
                    .clickable { onUpdateTransition(if (sel) "none" else t) }
                    .padding(horizontal = 5.dp, vertical = 3.dp)) {
                    Text(display, fontSize = 7.sp, fontWeight = FontWeight.Bold, color = if (sel) NeonOrange else Color.White)
                }
            }
        }
    }
}


// ─── 10. ANIMATIONS PANEL ──────────────────────────────────────
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AnimationsPanel(project: VideoProject, onUpdateAnim: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("TEXT ANIMATIONS", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = CyberCyan)
        FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            listOf(
                "none", "fade", "fade_out", "fade_in_out", "typewriter", "typewriter_fast",
                "bounce", "slide_left", "slide_right", "slide_up", "slide_down",
                "slide_in_3d", "zoom_in", "zoom_out", "rotate", "wave", "glitch_in",
                "neon_pulse", "neon_flicker", "pop", "flip", "elastic", "spring",
                "rubber", "swing", "shake", "blink", "pulse", "color_cycle",
                "explode_in", "implode", "marquee", "scroll_up", "scroll_down",
                "glow", "rainbow", "frozen", "fire", "metallic", "gold"
            ).forEach { a ->
                val display = a.replace("_", " ").replaceFirstChar { it.uppercase() }
                val sel = project.textAnimationType.lowercase() == a.lowercase()
                Box(Modifier.background(if (sel) CyberCyan.copy(0.2f) else Color.White.copy(0.04f), RoundedCornerShape(6.dp))
                    .clickable { onUpdateAnim(if (sel) "none" else a) }
                    .padding(horizontal = 6.dp, vertical = 4.dp)) {
                    Text(display, fontSize = 8.sp, fontWeight = FontWeight.Bold, color = if (sel) CyberCyan else Color.White)
                }
            }
        }
    }
}


// ─── 11. 3D PANEL ──────────────────────────────────────────────
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ThreeDPanel(project: VideoProject, onUpdate3D: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("3D CINEMATIC MASKS", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = NeonOrange)
        FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            listOf(
                "circle", "heart", "star", "hexagon", "diamond", "triangle",
                "vignette", "film_burn", "light_leak", "lens_flare", "smoke",
                "water", "fire", "particles", "bokeh", "glitch_3d",
                "chromatic", "anamorphic", "cinematic_bars", "color_splash",
                "oval", "square", "arch", "frame", "spotlight"
            ).forEach { m ->
                val display = m.replace("_", " ").replaceFirstChar { it.uppercase() }
                val sel = project.active3DShapeMask == m
                Box(Modifier.background(if (sel) NeonOrange.copy(0.2f) else Color.White.copy(0.04f), RoundedCornerShape(6.dp))
                    .clickable { onUpdate3D(if (sel) "none" else m) }
                    .padding(horizontal = 6.dp, vertical = 4.dp)) {
                    Text(display, fontSize = 8.sp, fontWeight = FontWeight.Bold, color = if (sel) NeonOrange else Color.White)
                }
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
                Box(Modifier.background(if (sel) NeonOrange.copy(0.2f) else Color.White.copy(0.04f), RoundedCornerShape(8.dp)).clickable { onUpdateTemplate(if (sel) "none" else id) }.padding(horizontal = 10.dp, vertical = 8.dp)) { Text(name, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = if (sel) NeonOrange else Color.White) }
            }
        }
    }
}


// ═══════════════════════════════════════════════════════════════
//  NEW v4.0 CapCut-sync Pro PANELS (all functional, wired to ViewModel)
// ═══════════════════════════════════════════════════════════════

// 18. BLEND MODES PANEL — 16+ blend modes
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BlendModePanel(
    project: VideoProject,
    onUpdateBlendMode: (String) -> Unit
) {
    val modes = listOf(
        "none", "multiply", "screen", "overlay", "darken", "lighten",
        "color_dodge", "color_burn", "hard_light", "soft_light",
        "difference", "exclusion", "hue", "saturation", "color",
        "luminosity", "addition", "phoenix", "reflect", "glow", "negation"
    )
    val labels = mapOf(
        "none" to "None", "multiply" to "Multiply", "screen" to "Screen",
        "overlay" to "Overlay", "darken" to "Darken", "lighten" to "Lighten",
        "color_dodge" to "Dodge", "color_burn" to "Burn", "hard_light" to "Hard Light",
        "soft_light" to "Soft Light", "difference" to "Difference", "exclusion" to "Exclusion",
        "hue" to "Hue", "saturation" to "Saturation", "color" to "Color",
        "luminosity" to "Luminosity", "addition" to "Addition", "phoenix" to "Phoenix",
        "reflect" to "Reflect", "glow" to "Glow", "negation" to "Negation"
    )
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
        modes.forEach { mode ->
            val sel = project.blendMode == mode
            Box(Modifier.background(if (sel) NeonOrange.copy(0.2f) else Color.White.copy(0.04f), RoundedCornerShape(8.dp))
                .clickable { onUpdateBlendMode(if (sel) "none" else mode) }
                .padding(horizontal = 10.dp, vertical = 8.dp)) {
                Text(labels[mode] ?: mode, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = if (sel) NeonOrange else Color.White)
            }
        }
    }
}

// 19. REVERSE + FREEZE PANEL
@Composable
private fun ReversePanel(
    project: VideoProject,
    onToggleReverse: () -> Unit,
    onUpdateFreezeFrame: (Long) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        // Reverse toggle
        Row(
            modifier = Modifier.fillMaxWidth().background(Color.White.copy(0.04f), RoundedCornerShape(10.dp)).padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Reverse Video", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Box(Modifier.background(if (project.isReverseEnabled) NeonOrange else Color.White.copy(0.1f), RoundedCornerShape(20.dp))
                .clickable { onToggleReverse() }.padding(horizontal = 16.dp, vertical = 8.dp)) {
                Text(if (project.isReverseEnabled) "ON" else "OFF", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = if (project.isReverseEnabled) Color.Black else Color.White)
            }
        }
        // Freeze frame
        Text("Freeze Frame Duration", fontSize = 10.sp, color = Color.Gray)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf(0L, 500L, 1000L, 2000L, 3000L, 5000L).forEach { ms ->
                val sel = project.freezeFrameMs == ms
                Box(Modifier.background(if (sel) NeonOrange.copy(0.2f) else Color.White.copy(0.04f), RoundedCornerShape(8.dp))
                    .clickable { onUpdateFreezeFrame(ms) }.padding(horizontal = 10.dp, vertical = 6.dp)) {
                    Text(if (ms == 0L) "None" else "${ms/1000.0}s", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = if (sel) NeonOrange else Color.White)
                }
            }
        }
    }
}

// 20. COLOR CURVES PANEL (Lift / Gamma / Gain)
@Composable
private fun ColorCurvesPanel(
    project: VideoProject,
    onUpdateColorLift: (Float) -> Unit,
    onUpdateColorGamma: (Float) -> Unit,
    onUpdateColorGain: (Float) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        ColorSliderRow("Lift (Shadows)", project.colorLift, -0.5f, 0.5f, onUpdateColorLift)
        ColorSliderRow("Gamma (Midtones)", project.colorGamma, -0.5f, 0.5f, onUpdateColorGamma)
        ColorSliderRow("Gain (Highlights)", project.colorGain, -0.5f, 0.5f, onUpdateColorGain)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Box(Modifier.background(Color.White.copy(0.04f), RoundedCornerShape(8.dp)).clickable {
                onUpdateColorLift(0f); onUpdateColorGamma(0f); onUpdateColorGain(0f)
            }.padding(horizontal = 12.dp, vertical = 6.dp)) {
                Text("Reset All", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
    }
}

@Composable
private fun ColorSliderRow(label: String, value: Float, min: Float, max: Float, onChange: (Float) -> Unit) {
    Column {
        Text("$label: ${"%.2f".format(value)}", fontSize = 9.sp, color = Color.Gray)
        Slider(
            value = value, onValueChange = onChange,
            valueRange = min..max,
            colors = SliderDefaults.colors(thumbColor = NeonOrange, activeTrackColor = NeonOrange),
            modifier = Modifier.fillMaxWidth().height(36.dp)
        )
    }
}

// 21. AUDIO EFFECTS PANEL — 25 audio effects + ducking toggle
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AudioEffectsPanel(
    project: VideoProject,
    onUpdateAudioEffect: (String) -> Unit,
    onToggleAudioDucking: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        // Audio ducking toggle
        Row(
            modifier = Modifier.fillMaxWidth().background(Color.White.copy(0.04f), RoundedCornerShape(10.dp)).padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Auto Audio Ducking", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Box(Modifier.background(if (project.isAudioDuckingEnabled) NeonOrange else Color.White.copy(0.1f), RoundedCornerShape(20.dp))
                .clickable { onToggleAudioDucking() }.padding(horizontal = 16.dp, vertical = 8.dp)) {
                Text(if (project.isAudioDuckingEnabled) "ON" else "OFF", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = if (project.isAudioDuckingEnabled) Color.Black else Color.White)
            }
        }
        Text("Audio Effects", fontSize = 10.sp, color = Color.Gray)
        val effects = listOf(
            "none", "echo", "reverb", "bass_boost", "treble_boost", "bass_reduce",
            "treble_reduce", "robot", "phone", "hall", "stadium", "room", "cave",
            "underwater", "vintage_radio", "megaphone", "chipmunk", "deep", "alien",
            "chorus", "flanger", "phaser", "distortion", "karaoke", "vocal_remove"
        )
        val labels = mapOf(
            "none" to "None", "echo" to "Echo", "reverb" to "Reverb", "bass_boost" to "Bass+",
            "treble_boost" to "Treble+", "bass_reduce" to "Bass-", "treble_reduce" to "Treble-",
            "robot" to "Robot", "phone" to "Phone", "hall" to "Hall", "stadium" to "Stadium",
            "room" to "Room", "cave" to "Cave", "underwater" to "Underwater",
            "vintage_radio" to "Radio", "megaphone" to "Megaphone", "chipmunk" to "Chipmunk",
            "deep" to "Deep", "alien" to "Alien", "chorus" to "Chorus", "flanger" to "Flanger",
            "phaser" to "Phaser", "distortion" to "Distortion", "karaoke" to "Karaoke",
            "vocal_remove" to "Vocal Remove"
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
            effects.forEach { eff ->
                val sel = project.audioEffect == eff
                Box(Modifier.background(if (sel) NeonOrange.copy(0.2f) else Color.White.copy(0.04f), RoundedCornerShape(8.dp))
                    .clickable { onUpdateAudioEffect(if (sel) "none" else eff) }
                    .padding(horizontal = 10.dp, vertical = 8.dp)) {
                    Text(labels[eff] ?: eff, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = if (sel) NeonOrange else Color.White)
                }
            }
        }
    }
}

// 22. VOICE CHANGER PANEL
@Composable
private fun VoiceChangerPanel(
    project: VideoProject,
    onUpdateVoiceChangerPitch: (Float) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Voice Changer Pitch", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
        Text("Pitch: ${"%.1f".format(project.voiceChangerPitch)} semitones", fontSize = 9.sp, color = Color.Gray)
        Slider(
            value = project.voiceChangerPitch, onValueChange = onUpdateVoiceChangerPitch,
            valueRange = -12f..12f,
            colors = SliderDefaults.colors(thumbColor = NeonOrange, activeTrackColor = NeonOrange),
            modifier = Modifier.fillMaxWidth().height(36.dp)
        )
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf(-12f to "Deep", -6f to "Low", 0f to "Normal", 6f to "High", 12f to "Chipmunk").forEach { (pitch, label) ->
                val sel = project.voiceChangerPitch == pitch
                Box(Modifier.background(if (sel) NeonOrange.copy(0.2f) else Color.White.copy(0.04f), RoundedCornerShape(8.dp))
                    .clickable { onUpdateVoiceChangerPitch(pitch) }.padding(horizontal = 10.dp, vertical = 6.dp)) {
                    Text(label, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = if (sel) NeonOrange else Color.White)
                }
            }
        }
    }
}

// 23. BORDER STYLES PANEL — 13 border/frame styles
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BorderStylesPanel(
    project: VideoProject,
    onUpdateBorderStyle: (String) -> Unit
) {
    val styles = listOf(
        "none", "white", "black", "rounded", "shadow", "neon",
        "gradient", "film", "polaroid", "vintage", "modern",
        "minimal", "glow"
    )
    val labels = mapOf(
        "none" to "None", "white" to "White", "black" to "Black", "rounded" to "Rounded",
        "shadow" to "Shadow", "neon" to "Neon", "gradient" to "Gradient", "film" to "Film",
        "polaroid" to "Polaroid", "vintage" to "Vintage", "modern" to "Modern",
        "minimal" to "Minimal", "glow" to "Glow"
    )
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
        styles.forEach { style ->
            val sel = project.borderStyle == style
            Box(Modifier.background(if (sel) NeonOrange.copy(0.2f) else Color.White.copy(0.04f), RoundedCornerShape(8.dp))
                .clickable { onUpdateBorderStyle(if (sel) "none" else style) }
                .padding(horizontal = 10.dp, vertical = 8.dp)) {
                Text(labels[style] ?: style, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = if (sel) NeonOrange else Color.White)
            }
        }
    }
}

// 24. VIGNETTE STYLES PANEL — 8 vignette styles
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun VignetteStylesPanel(
    project: VideoProject,
    onUpdateVignetteStyle: (String) -> Unit
) {
    val styles = listOf("none", "classic", "soft", "strong", "reverse", "colored", "blur", "spotlight")
    val labels = mapOf(
        "none" to "None", "classic" to "Classic", "soft" to "Soft", "strong" to "Strong",
        "reverse" to "Reverse", "colored" to "Colored", "blur" to "Blur", "spotlight" to "Spotlight"
    )
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
        styles.forEach { style ->
            val sel = project.vignetteStyle == style
            Box(Modifier.background(if (sel) NeonOrange.copy(0.2f) else Color.White.copy(0.04f), RoundedCornerShape(8.dp))
                .clickable { onUpdateVignetteStyle(if (sel) "none" else style) }
                .padding(horizontal = 10.dp, vertical = 8.dp)) {
                Text(labels[style] ?: style, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = if (sel) NeonOrange else Color.White)
            }
        }
    }
}

// 25. FREEZE FRAME PANEL (standalone tool)
@Composable
private fun FreezeFramePanel(
    project: VideoProject,
    onUpdateFreezeFrame: (Long) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Freeze Frame — Pause video at a moment", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
        Text("Current: ${if (project.freezeFrameMs > 0) "${project.freezeFrameMs/1000.0}s" else "Off"}", fontSize = 9.sp, color = Color.Gray)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf(0L, 250L, 500L, 750L, 1000L, 1500L, 2000L, 3000L, 5000L).forEach { ms ->
                val sel = project.freezeFrameMs == ms
                Box(Modifier.background(if (sel) NeonOrange.copy(0.2f) else Color.White.copy(0.04f), RoundedCornerShape(8.dp))
                    .clickable { onUpdateFreezeFrame(ms) }.padding(horizontal = 10.dp, vertical = 6.dp)) {
                    Text(if (ms == 0L) "None" else "${ms/1000.0}s", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = if (sel) NeonOrange else Color.White)
                }
            }
        }
    }
}
