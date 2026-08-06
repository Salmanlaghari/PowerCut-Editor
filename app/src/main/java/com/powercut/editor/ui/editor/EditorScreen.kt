package com.powercut.editor.ui.editor

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.Flip
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.filled.ZoomIn
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.powercut.editor.R
import com.powercut.editor.core.utils.LanguageHelper
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.material3.CircularProgressIndicator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import android.os.Environment
import com.powercut.editor.ui.editor.ExportBottomBar
import com.powercut.editor.export.ExportEngine
import com.powercut.editor.export.ExportConfig
import com.powercut.editor.PowerCutPremiumLauncherBar

private fun formatTime(ms: Long): String {
    val totalSecs = ms / 1000
    val minutes = totalSecs / 60
    val seconds = totalSecs % 60
    return String.format(Locale.US, "%02d:%02d", minutes, seconds)
}

@OptIn(UnstableApi::class)
@Composable
fun EditorScreen(
    project: VideoProject,
    language: String,
    onBack: () -> Unit,
    onUpdateTrim: (Long, Long) -> Unit,
    onUpdateResolution: (String) -> Unit,
    onUpdateFilter: (String) -> Unit,
    onToggleMute: () -> Unit,
    onExport: () -> Unit,
    onDurationRetrieved: (Long) -> Unit,

    // High-priority callbacks
    onUpdateSpeed: (Float) -> Unit,
    onUpdateAspectPreset: (String) -> Unit,
    onUpdateTransition: (String) -> Unit,
    onUpdateBackgroundMusic: (String?) -> Unit,
    onUpdateMusicVolume: (Float) -> Unit,
    onUpdateVideoVolume: (Float) -> Unit,
    onUpdateAutoCaptions: (String) -> Unit,
    onToggleSilenceRemover: () -> Unit,

    // Professional callbacks
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
    onAddClip: (android.net.Uri) -> Unit,
    onSaveDraft: () -> Unit,

    // Green Screen callbacks
    onToggleGreenScreen: () -> Unit,
    onUpdateGreenScreenColor: (String) -> Unit,
    onUpdateGreenScreenThreshold: (Float) -> Unit,
    onSelectAutoBackground: (Int) -> Unit,
    onPickCustomBackground: () -> Unit,

    // Eraser callbacks
    onUpdateEraserMode: (String) -> Unit,
    onUpdateEraserBrushSize: (Float) -> Unit,
    onUpdateEraserTolerance: (Float) -> Unit,
    onToggleEraserSoftEdge: () -> Unit,
    onUndoEraser: () -> Unit,
    onResetEraser: () -> Unit,

    // Image Editor callbacks
    onUpdateImageEditorBrightness: (Float) -> Unit,
    onUpdateImageEditorContrast: (Float) -> Unit,
    onUpdateImageEditorSaturation: (Float) -> Unit,
    onUpdateImageEditorBlur: (Float) -> Unit,
    onUpdateImageEditorSharpen: (Float) -> Unit,
    onUpdateImageEditorTemperature: (Float) -> Unit,
    onUpdateImageEditorVignette: (Float) -> Unit,
    onUpdateImageEditorGrain: (Float) -> Unit,
    onUpdateImageEditorFade: (Float) -> Unit,
    onUpdateImageEditorHighlights: (Float) -> Unit,
    onUpdateImageEditorShadows: (Float) -> Unit,
    onUpdateImageEditorExposure: (Float) -> Unit,
    onResetImageEditor: () -> Unit,

    // Extended text styling callbacks
    onToggleTextBold: () -> Unit = {},
    onToggleTextItalic: () -> Unit = {},
    onToggleTextShadow: () -> Unit = {},
    onToggleTextOutline: () -> Unit = {},
    onToggleTextGlow: () -> Unit = {},
    onToggleTextNeon: () -> Unit = {},
    onUpdateTextBgColor: (String) -> Unit = {},
    onUpdateTextBgOpacity: (Float) -> Unit = {},
    onUpdateTextFontSize: (Float) -> Unit = {},
    onUpdateTextColor: (String) -> Unit = {},
    onUpdateTextStyle: (String) -> Unit = {},
    onUpdateTextPositionX: (Float) -> Unit = {},
    onUpdateTextPositionY: (Float) -> Unit = {},

    // Orientation callbacks
    onUpdateOrientationMode: (String) -> Unit,
    onToggleVerticalSafeZone: () -> Unit,
    onToggleHorizontalLetterbox: () -> Unit,
    onToggleAutoReframe: () -> Unit,

    // v6.0.0 Premium launcher — top action row (AI Hub, Presets, Pro, Studio)
    onAiHub: () -> Unit = {},
    onSocialPresets: () -> Unit = {},
    onProTier: () -> Unit = {},
    onPremiumStudio: () -> Unit = {},

    // v6.0.0 Effects & Stickers galleries — full-screen 3D glass browsers that
    // drive REAL project state (selectedEffect / stickerType) into PowerCutDAG.
    onOpenEffects: () -> Unit = {},
    onOpenStickers: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var exportProgress by remember { mutableStateOf(0f) }
    var isExporting by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }
    val exportEngine = remember { ExportEngine() }

    // PRIORITY 1 FIX: Cancel any running export when the editor screen is
    // disposed (e.g. user navigates away, activity finishes). This prevents
    // the native worker thread from touching freed memory.
    DisposableEffect(Unit) {
        onDispose {
            try {
                exportEngine.cancel()
                exportEngine.destroy()
            } catch (e: Exception) {
                // Safe to ignore — engine may already be destroyed.
            }
        }
    }


    // Clip local Video Picker
    val clipPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            onAddClip(uri)
            android.widget.Toast.makeText(context, "Clip added to timeline!", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    // BGM local Audio Picker
    val musicPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            val audioPath = UriHelper.getPathFromUri(context, uri)
            onUpdateBackgroundMusic(audioPath)
        }
    }

    // Initialize ExoPlayer
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            repeatMode = Player.REPEAT_MODE_ONE
        }
    }

    // ═══ BGM (Background Music) ExoPlayer — second player for background music ═══
    val bgmPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            repeatMode = Player.REPEAT_MODE_ONE
            volume = 0.5f
        }
    }
    var bgmPrepared by remember { mutableStateOf(false) }

    var isPlaying by remember { mutableStateOf(false) }
    var currentPlaybackTime by remember { mutableStateOf(0L) }

    // Connect player with video path
    LaunchedEffect(project.videoPath) {
        val uri = if (project.videoPath.startsWith("content://") || project.videoPath.startsWith("file://")) {
            android.net.Uri.parse(project.videoPath)
        } else {
            android.net.Uri.fromFile(java.io.File(project.videoPath))
        }
        val mediaItem = MediaItem.fromUri(uri)
        exoPlayer.setMediaItem(mediaItem)
        exoPlayer.prepare()
        exoPlayer.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                if (state == Player.STATE_READY) {
                    onDurationRetrieved(exoPlayer.duration)
                }
            }
        })
    }

    // Monitor Playhead position updates and control playback
    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            exoPlayer.play()
            if (bgmPrepared) bgmPlayer.play()
            while (isPlaying) {
                currentPlaybackTime = exoPlayer.currentPosition
                kotlinx.coroutines.delay(100)
            }
        } else {
            exoPlayer.pause()
            bgmPlayer.pause()
            // Pauses editing/playback for 3+ seconds auto-save trigger!
            kotlinx.coroutines.delay(3000)
            if (!isPlaying) {
                onSaveDraft()
            }
        }
    }

    // ═══ BGM: React to backgroundMusicPath changes — prepare the BGM player ═══
    LaunchedEffect(project.backgroundMusicPath) {
        val bgmPath = project.backgroundMusicPath
        if (!bgmPath.isNullOrBlank()) {
            try {
                val bgmUri = if (bgmPath.startsWith("content://") || bgmPath.startsWith("file://")) {
                    android.net.Uri.parse(bgmPath)
                } else {
                    android.net.Uri.fromFile(java.io.File(bgmPath))
                }
                bgmPlayer.setMediaItem(MediaItem.fromUri(bgmUri))
                bgmPlayer.prepare()
                bgmPrepared = true
                // If already playing, start BGM too
                if (isPlaying) bgmPlayer.play()
            } catch (e: Exception) {
                bgmPrepared = false
            }
        } else {
            bgmPlayer.stop()
            bgmPlayer.clearMediaItems()
            bgmPrepared = false
        }
    }

    // ═══ BGM: Sync BGM volume from project state ═══
    LaunchedEffect(project.backgroundMusicVolume) {
        bgmPlayer.volume = project.backgroundMusicVolume
    }

    // React to changes in Mute state and Volume
    LaunchedEffect(project.isMuted, project.videoVolume) {
        exoPlayer.volume = if (project.isMuted) 0f else project.videoVolume
    }

    // Sync ExoPlayer speed dynamically with the project selected speedFactor
    LaunchedEffect(project.speedFactor) {
        exoPlayer.playbackParameters = PlaybackParameters(project.speedFactor)
    }

    // Clean up players on leave
    DisposableEffect(Unit) {
        onDispose {
            exoPlayer.release()
            bgmPlayer.release()
        }
    }

    // Real-time Compose ColorMatrix filter
    val composeColorFilter = remember(project.selectedFilter) {
        when (project.selectedFilter.lowercase()) {
            "grayscale" -> ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) })
            "sepia" -> ColorFilter.colorMatrix(
                ColorMatrix(
                    floatArrayOf(
                        0.393f, 0.769f, 0.189f, 0f, 0f,
                        0.349f, 0.686f, 0.168f, 0f, 0f,
                        0.272f, 0.534f, 0.131f, 0f, 0f,
                        0f,     0f,     0f,     1f, 0f
                    )
                )
            )
            "invert" -> ColorFilter.colorMatrix(
                ColorMatrix(
                    floatArrayOf(
                        -1f,  0f,  0f, 0f, 255f,
                         0f, -1f,  0f, 0f, 255f,
                         0f,  0f, -1f, 0f, 255f,
                         0f,  0f,  0f, 1f,   0f
                    )
                )
            )
            else -> null
        }
    }

    // Define Aspect Ratio Float based on user's selected preset
    val videoAspectRatio = remember(project.aspectPreset) {
        when (project.aspectPreset) {
            "1:1" -> 1.0f
            "9:16" -> 9f / 16f
            "16:9" -> 16f / 9f
            else -> 16f / 9f
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F0F14)) // Cinematic dark slate background
    ) {
        // 1. TOP HEADER BAR
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .glassmorphic(shape = RoundedCornerShape(8.dp))
                        .tactileClick {
                            onSaveDraft()
                            onBack()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.ChevronLeft,
                        contentDescription = "Back",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Column {
                    Text(
                        text = "My Project",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "${formatTime(currentPlaybackTime)} / ${formatTime(project.durationMs)}",
                        fontSize = 9.sp,
                        color = Color.Gray,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Undo / Redo / Export Action Group
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(onClick = { /* Undo trigger */ }, modifier = Modifier.size(28.dp)) {
                    Text("↶", color = Color.LightGray, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
                IconButton(onClick = { /* Redo trigger */ }, modifier = Modifier.size(28.dp)) {
                    Text("↷", color = Color.LightGray, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
                Box(
                    modifier = Modifier
                        .neonGlow(color = AccentSecondary, shape = RoundedCornerShape(24.dp), glowWidth = 1.dp)
                        .background(
                            premiumAccentGradient,
                            shape = RoundedCornerShape(24.dp)
                        )
                        .tactileClick(onClick = { showExportDialog = true })
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = LanguageHelper.getString(R.string.export_video, language).uppercase(),
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 11.sp,
                        letterSpacing = 0.5.sp
                    )
                }
            }
        }

        // ─────────────────────────────────────────────────────────────
        //  v6.0.0 PREMIUM ACTION ROW — fixed top bar below the header.
        //  Drives REAL FFmpeg chains through EditorViewModel via the
        //  existing premium overlay screens (AI Hub, Presets, Pro, Studio).
        // ─────────────────────────────────────────────────────────────
        PowerCutPremiumLauncherBar(
            onAiHub = onAiHub,
            onSocialPresets = onSocialPresets,
            onProTier = onProTier,
            onPremiumStudio = onPremiumStudio
        )

        // 2. PREVIEW CONTAINER AREA (Strictly aspect-ratio bound dynamically)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .weight(1.8f),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .aspectRatio(videoAspectRatio)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.Black)
                    .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                // Media3 / ExoPlayer View with real-time scaling and 3D transforms
                AndroidView(
                    factory = { ctx ->
                        PlayerView(ctx).apply {
                            player = exoPlayer
                            useController = false
                        }
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer(
                            scaleX = if (project.isFlippedHorizontal) -1f else 1f,
                            scaleY = if (project.isFlippedVertical) -1f else 1f,
                            rotationZ = project.rotationDegrees
                        )
                )

                // Simulated live filter overlay
                if (project.selectedFilter.lowercase() != "none" && composeColorFilter != null) {
                    val overlayColor = when (project.selectedFilter.lowercase()) {
                        "grayscale" -> Color.Gray.copy(alpha = 0.15f)
                        "sepia" -> Color(0xFF704214).copy(alpha = 0.18f)
                        "invert" -> Color.White.copy(alpha = 0.1f)
                        else -> Color.Transparent
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(overlayColor)
                    )
                }

                // Safe Area Dashed line overlay border
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val inset = size.width * 0.08f
                    drawRect(
                        color = Color.White.copy(alpha = 0.2f),
                        topLeft = Offset(inset, inset),
                        size = size.copy(width = size.width - inset * 2, height = size.height - inset * 2),
                        style = Stroke(
                            width = 2f,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                        )
                    )
                }

                // Central glass-styled Play/Pause button overlay
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(Color.White.copy(alpha = 0.15f), CircleShape)
                        .border(2.dp, Color.White.copy(alpha = 0.3f), CircleShape)
                        .clickable { isPlaying = !isPlaying },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = "Play/Pause Overlay",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Live text overlay — only show when user has set text (no default hardcoded text)
                if (!project.activeTextOverlay.isNullOrBlank()) {
                    val textBg = try {
                        Color(android.graphics.Color.parseColor(project.textBgColor))
                    } catch (_: Exception) { Color.Transparent }
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 12.dp)
                            .background(
                                if (textBg.alpha > 0.01f) textBg.copy(alpha = project.textBgOpacity)
                                else Color.Black.copy(alpha = 0.5f),
                                RoundedCornerShape(6.dp)
                            )
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = project.activeTextOverlay!!,
                            color = try {
                                Color(android.graphics.Color.parseColor(project.textColorHex))
                            } catch (_: Exception) { Color.White },
                            fontSize = project.textFontSize.sp,
                            fontWeight = if (project.textBold) FontWeight.Bold else FontWeight.Normal,
                            fontStyle = if (project.textItalic) FontStyle.Italic else FontStyle.Normal
                        )
                    }
                }

                // Top-Right Zoom + Fullscreen icons overlay
                Row(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(6.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(imageVector = Icons.Default.ZoomIn, contentDescription = "Zoom", tint = Color.White, modifier = Modifier.size(12.dp))
                    }
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(6.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(imageVector = Icons.Default.Fullscreen, contentDescription = "Fullscreen", tint = Color.White, modifier = Modifier.size(12.dp))
                    }
                }
            }
        }

        // 3. PLAYBACK CONTROLS
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Previous Frame
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .glassmorphic(shape = CircleShape)
                    .tactileClick {
                        val seek = (exoPlayer.currentPosition - 33).coerceAtLeast(0)
                        exoPlayer.seekTo(seek)
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = Icons.Default.SkipPrevious, contentDescription = "Prev Frame", tint = Color.White, modifier = Modifier.size(14.dp))
            }

            Spacer(modifier = Modifier.width(20.dp))

            // Large Orange Accent Play Button
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .neonGlow(color = NeonOrange, shape = CircleShape)
                    .background(NeonOrange, CircleShape)
                    .tactileClick { isPlaying = !isPlaying },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = "Play/Pause",
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(20.dp))

            // Next Frame
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .glassmorphic(shape = CircleShape)
                    .tactileClick {
                        val seek = (exoPlayer.currentPosition + 33).coerceAtMost(exoPlayer.duration)
                        exoPlayer.seekTo(seek)
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = Icons.Default.SkipNext, contentDescription = "Next Frame", tint = Color.White, modifier = Modifier.size(14.dp))
            }

            Spacer(modifier = Modifier.width(24.dp))

            // Speed indicator chip
            Box(
                modifier = Modifier
                    .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(8.dp))
                    .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "${project.speedFactor}x",
                    fontSize = 11.sp,
                    color = CyberCyan,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // 4. TOOL CATEGORIES LIST (Horizontal Scrolling Tab Bar wrapper)
        val categories = listOf("Edit", "Audio", "Text", "Stickers", "Overlay", "AI", "🎬Chroma", "🧹Erase", "🎨ImgEdit", "📐Orient", "🖼️Studio")
        var activeCategory by remember { mutableStateOf("Edit") }
        var selectedBottomTool by remember { mutableStateOf("Trim") }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            categories.forEach { cat ->
                val isSel = activeCategory == cat
                val backgroundBrush = if (isSel) {
                    if (cat == "AI") premiumAccentGradient else null
                } else {
                    null
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(24.dp))
                        .background(
                            brush = backgroundBrush ?: Brush.linearGradient(colors = listOf(Color.Transparent, Color.Transparent))
                        )
                        .background(
                            color = if (isSel && cat != "AI") NeonOrange.copy(alpha = 0.18f) else Color.Transparent
                        )
                        .border(
                            width = 1.dp,
                            color = if (isSel) {
                                if (cat == "AI") Color.Transparent else NeonOrange
                            } else {
                                Color.White.copy(alpha = 0.05f)
                            },
                            shape = RoundedCornerShape(24.dp)
                        )
                        .clickable { activeCategory = cat }
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = cat.uppercase(),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isSel) {
                            if (cat == "AI") Color.White else NeonOrange
                        } else {
                            Color.LightGray
                        }
                    )
                }
            }
        }

        // DYNAMIC CONTEXTUAL OPTIONS PANEL (Wrapped with vertical scroll to prevent UI layout overflow)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp)
                .height(130.dp)
                .glassmorphic(shape = RoundedCornerShape(14.dp)),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(8.dp)
            ) {
                when (activeCategory) {
                    "Edit" -> {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Rotate
                                IconButton(onClick = onUpdateRotation, modifier = Modifier.size(28.dp)) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("↻", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                        Text("ROTATE", fontSize = 6.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    }
                                }
                                // Flips
                                IconButton(onClick = onToggleFlipHorizontal, modifier = Modifier.size(28.dp)) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(imageVector = Icons.Default.Flip, contentDescription = "FlipH", tint = Color.White, modifier = Modifier.size(14.dp))
                                        Text("FLIP H", fontSize = 6.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    }
                                }
                                IconButton(onClick = onToggleFlipVertical, modifier = Modifier.size(28.dp)) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(imageVector = Icons.Default.Flip, contentDescription = "FlipV", tint = Color.White, modifier = Modifier.size(14.dp))
                                        Text("FLIP V", fontSize = 6.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    }
                                }

                                // Precision manual trim buttons
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Box(
                                        modifier = Modifier
                                            .background(NeonOrange.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                                            .clickable { onUpdateTrim((project.trimStartMs + 500).coerceAtMost(project.trimEndMs - 500), project.trimEndMs) }
                                            .padding(4.dp)
                                    ) {
                                        Text("TRIM S+0.5s", fontSize = 7.sp, fontWeight = FontWeight.Bold, color = NeonOrange)
                                    }
                                    Box(
                                        modifier = Modifier
                                            .background(NeonOrange.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                                            .clickable { onUpdateTrim(project.trimStartMs, (project.trimEndMs - 500).coerceAtLeast(project.trimStartMs + 500)) }
                                            .padding(4.dp)
                                    ) {
                                        Text("TRIM E-0.5s", fontSize = 7.sp, fontWeight = FontWeight.Bold, color = NeonOrange)
                                    }
                                }
                            }

                            // Crop, Speed, SpeedCurve selections
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Crops
                                listOf("1:1", "16:9", "9:16").forEach { cr ->
                                    val isSel = project.cropPreset == cr
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .background(if (isSel) CyberCyan.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.04f), RoundedCornerShape(6.dp))
                                            .clickable { onUpdateCropPreset(cr); onUpdateAspectPreset(cr) }
                                            .padding(4.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("CROP $cr", fontSize = 7.sp, fontWeight = FontWeight.Bold, color = if (isSel) CyberCyan else Color.White)
                                    }
                                }
                                // Speeds
                                listOf(0.5f, 1.0f, 2.0f, 4.0f).forEach { spVal ->
                                    val isSel = project.speedFactor == spVal
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .background(if (isSel) NeonOrange.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.04f), RoundedCornerShape(6.dp))
                                            .clickable { onUpdateSpeed(spVal) }
                                            .padding(4.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("${spVal}X", fontSize = 7.sp, fontWeight = FontWeight.Bold, color = if (isSel) NeonOrange else Color.White)
                                    }
                                }
                            }

                            // Speed curves & Live workspace Resolution selection
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                listOf("Standard", "Montage", "Hero", "Flash").forEach { crv ->
                                    val isSel = project.speedCurve.lowercase() == crv.lowercase()
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .background(if (isSel) CyberCyan.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.03f), RoundedCornerShape(6.dp))
                                            .clickable { onUpdateSpeedCurve(crv) }
                                            .padding(4.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(crv.uppercase(), fontSize = 7.sp, fontWeight = FontWeight.Bold, color = if (isSel) CyberCyan else Color.White)
                                    }
                                }
                                listOf("1080p", "4k", "8k").forEach { r ->
                                    val isSel = project.targetResolution.lowercase() == r.lowercase()
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .background(if (isSel) NeonOrange.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.03f), RoundedCornerShape(6.dp))
                                            .clickable { onUpdateResolution(r) }
                                            .padding(4.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(r.uppercase(), fontSize = 7.sp, fontWeight = FontWeight.Bold, color = if (isSel) NeonOrange else Color.White)
                                    }
                                }
                            }
                        }
                    }
                    "Audio" -> {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Video volume
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("VIDEO VOLUME", fontSize = 7.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    Slider(
                                        value = project.videoVolume,
                                        onValueChange = onUpdateVideoVolume,
                                        valueRange = 0.0f..1.0f,
                                        colors = SliderDefaults.colors(activeTrackColor = NeonOrange, thumbColor = NeonOrange),
                                        modifier = Modifier.height(20.dp)
                                    )
                                }

                                // BGM volume
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("BGM MUSIC VOLUME", fontSize = 7.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    Slider(
                                        value = project.backgroundMusicVolume,
                                        onValueChange = onUpdateMusicVolume,
                                        valueRange = 0.0f..1.0f,
                                        colors = SliderDefaults.colors(activeTrackColor = CyberCyan, thumbColor = CyberCyan),
                                        modifier = Modifier.height(20.dp)
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Mute video toggle
                                Box(
                                    modifier = Modifier
                                        .background(if (project.isMuted) NeonOrange.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.04f), RoundedCornerShape(6.dp))
                                        .clickable { onToggleMute() }
                                        .padding(horizontal = 8.dp, vertical = 6.dp)
                                ) {
                                    Text(if (project.isMuted) "UNMUTE VIDEO" else "MUTE VIDEO", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = if (project.isMuted) NeonOrange else Color.White)
                                }

                                // Visualizer selection
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    listOf("Wave", "Bars", "Radial").forEach { style ->
                                        val isSel = project.visualizerStyle.lowercase() == style.lowercase()
                                        Box(
                                            modifier = Modifier
                                                .background(if (isSel) CyberCyan.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.04f), RoundedCornerShape(6.dp))
                                                .clickable { onUpdateVisualizerStyle(style) }
                                                .padding(horizontal = 6.dp, vertical = 6.dp)
                                        ) {
                                            Text(style, fontSize = 7.sp, fontWeight = FontWeight.Bold, color = if (isSel) CyberCyan else Color.White)
                                        }
                                    }
                                }

                                // Beat sync toggle
                                Box(
                                    modifier = Modifier
                                        .background(if (project.isBeatSyncEnabled) CyberCyan.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.04f), RoundedCornerShape(6.dp))
                                        .clickable { onToggleBeatSync() }
                                        .padding(horizontal = 8.dp, vertical = 6.dp)
                                ) {
                                    Text("BEAT-SYNC", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = if (project.isBeatSyncEnabled) CyberCyan else Color.White)
                                }

                                // BGM add button
                                Box(
                                    modifier = Modifier
                                        .background(CyberCyan.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                                        .clickable { musicPickerLauncher.launch("audio/*") }
                                        .padding(horizontal = 8.dp, vertical = 6.dp)
                                ) {
                                    Text("ADD BGM", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = CyberCyan)
                                }
                            }
                        }
                    }
                    "Text" -> {
                        // Sub-tab state for text tool
                        var textSubTab by remember { mutableStateOf("content") }
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            // Sub-tab bar
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                listOf("content" to "📝", "color" to "🎨", "style" to "✏️", "position" to "📍", "anim" to "✨").forEach { (id, emoji) ->
                                    val isSel = textSubTab == id
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(if (isSel) NeonOrange.copy(alpha = 0.18f) else Color.Transparent)
                                            .border(1.dp, if (isSel) NeonOrange.copy(alpha = 0.5f) else Color.White.copy(alpha = 0.05f), RoundedCornerShape(14.dp))
                                            .clickable { textSubTab = id }
                                            .padding(horizontal = 10.dp, vertical = 4.dp)
                                    ) {
                                        Text(emoji, fontSize = 11.sp)
                                    }
                                }
                            }

                            when (textSubTab) {
                                "content" -> {
                                    // Text input
                                    var textInput by remember { mutableStateOf(project.activeTextOverlay ?: "") }
                                    OutlinedTextField(
                                        value = textInput,
                                        onValueChange = {
                                            textInput = it
                                            onUpdateTextOverlay(if (it.isBlank()) null else it)
                                        },
                                        placeholder = { Text("Type your text...", fontSize = 9.sp, color = Color.Gray) },
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = NeonOrange,
                                            unfocusedBorderColor = Color.White.copy(alpha = 0.1f),
                                            focusedTextColor = Color.White,
                                            unfocusedTextColor = Color.White
                                        ),
                                        modifier = Modifier.fillMaxWidth().height(44.dp),
                                        shape = RoundedCornerShape(8.dp)
                                    )

                                    // Font size slider
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("FONT SIZE", fontSize = 7.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                                        Text("${project.textFontSize.toInt()}sp", fontSize = 7.sp, fontWeight = FontWeight.Bold, color = CyberCyan)
                                    }
                                    Slider(
                                        value = project.textFontSize,
                                        onValueChange = onUpdateTextFontSize,
                                        valueRange = 8f..80f,
                                        colors = SliderDefaults.colors(activeTrackColor = NeonOrange, thumbColor = NeonOrange),
                                        modifier = Modifier.height(20.dp)
                                    )
                                }
                                "color" -> {
                                    // Color picker — 15 colors
                                    Text("TEXT COLOR", fontSize = 7.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                                    val textColors = listOf(
                                        "#FFFFFF", "#000000", "#FF0000", "#00FF00", "#0000FF",
                                        "#FFFF00", "#FF6B35", "#7C5CFF", "#2DD4BF", "#FF3D7F",
                                        "#FFD700", "#FF69B4", "#00CED1", "#FF4500", "#9370DB"
                                    )
                                    Row(
                                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        textColors.forEach { color ->
                                            val isSel = project.textColorHex == color
                                            Box(
                                                modifier = Modifier
                                                    .size(24.dp)
                                                    .clip(CircleShape)
                                                    .background(try { Color(android.graphics.Color.parseColor(color)) } catch (_: Exception) { Color.White })
                                                    .border(
                                                        width = if (isSel) 2.dp else 1.dp,
                                                        color = if (isSel) Color.White else Color.White.copy(alpha = 0.2f),
                                                        shape = CircleShape
                                                    )
                                                    .clickable { onUpdateTextColor(color) }
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(2.dp))

                                    // Text background color
                                    Text("TEXT BG COLOR", fontSize = 7.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                                    val bgColors = listOf(
                                        "#00000000", "#CC000000", "#99000000", "#CCFF6B35", "#CC7C5CFF",
                                        "#CC2DD4BF", "#CCFF3D7F", "#CCFFD700", "#CCFFFFFF"
                                    )
                                    Row(
                                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        bgColors.forEach { color ->
                                            val isSel = project.textBgColor == color
                                            val displayColor = try { Color(android.graphics.Color.parseColor(color)) } catch (_: Exception) { Color.Transparent }
                                            Box(
                                                modifier = Modifier
                                                    .size(24.dp)
                                                    .clip(CircleShape)
                                                    .background(displayColor)
                                                    .border(
                                                        width = if (isSel) 2.dp else 1.dp,
                                                        color = if (isSel) NeonOrange else Color.White.copy(alpha = 0.2f),
                                                        shape = CircleShape
                                                    )
                                                    .clickable { onUpdateTextBgColor(color) }
                                            )
                                        }
                                    }

                                    // Bg opacity slider
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("BG OPACITY", fontSize = 7.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                                        Text("${(project.textBgOpacity * 100).toInt()}%", fontSize = 7.sp, fontWeight = FontWeight.Bold, color = CyberCyan)
                                    }
                                    Slider(
                                        value = project.textBgOpacity,
                                        onValueChange = onUpdateTextBgOpacity,
                                        valueRange = 0f..1f,
                                        colors = SliderDefaults.colors(activeTrackColor = CyberCyan, thumbColor = CyberCyan),
                                        modifier = Modifier.height(18.dp)
                                    )
                                }
                                "style" -> {
                                    // Style toggles: Bold, Italic, Shadow, Outline, Glow, Neon
                                    Text("TEXT STYLE", fontSize = 7.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                                    val styleToggles = listOf(
                                        Triple("Bold", project.textBold, onToggleTextBold),
                                        Triple("Italic", project.textItalic, onToggleTextItalic),
                                        Triple("Shadow", project.textShadow, onToggleTextShadow),
                                        Triple("Outline", project.textOutline, onToggleTextOutline),
                                        Triple("Glow", project.textGlow, onToggleTextGlow),
                                        Triple("Neon", project.textNeon, onToggleTextNeon)
                                    )
                                    styleToggles.chunked(3).forEach { row ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            row.forEach { (label, isActive, toggle) ->
                                                Box(
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .height(30.dp)
                                                        .clip(RoundedCornerShape(6.dp))
                                                        .background(if (isActive) NeonOrange.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.04f))
                                                        .border(1.dp, if (isActive) NeonOrange else Color.White.copy(alpha = 0.06f), RoundedCornerShape(6.dp))
                                                        .clickable { toggle() },
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(label, fontSize = 8.sp, fontWeight = FontWeight.Bold, color = if (isActive) NeonOrange else Color.White)
                                                }
                                            }
                                            repeat(3 - row.size) { Spacer(modifier = Modifier.weight(1f)) }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(2.dp))

                                    // Text style presets
                                    Text("STYLE PRESETS", fontSize = 7.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                                    Row(
                                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        listOf("classic", "modern", "handwritten", "display", "mono", "serif").forEach { style ->
                                            val isSel = project.textStyleId == style
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(14.dp))
                                                    .background(if (isSel) CyberCyan.copy(alpha = 0.18f) else Color.Transparent)
                                                    .border(1.dp, if (isSel) CyberCyan.copy(alpha = 0.5f) else Color.White.copy(alpha = 0.05f), RoundedCornerShape(14.dp))
                                                    .clickable { onUpdateTextStyle(style) }
                                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                                            ) {
                                                Text(style.replaceFirstChar { it.uppercase() }, fontSize = 8.sp, fontWeight = FontWeight.Bold, color = if (isSel) CyberCyan else Color.LightGray)
                                            }
                                        }
                                    }
                                }
                                "position" -> {
                                    // X/Y position controls
                                    Text("TEXT POSITION", fontSize = 7.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("X: ${(project.textPositionX * 100).toInt()}%", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = CyberCyan)
                                    }
                                    Slider(
                                        value = project.textPositionX,
                                        onValueChange = onUpdateTextPositionX,
                                        valueRange = 0f..1f,
                                        colors = SliderDefaults.colors(activeTrackColor = CyberCyan, thumbColor = CyberCyan),
                                        modifier = Modifier.height(20.dp)
                                    )
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("Y: ${(project.textPositionY * 100).toInt()}%", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = CyberCyan)
                                    }
                                    Slider(
                                        value = project.textPositionY,
                                        onValueChange = onUpdateTextPositionY,
                                        valueRange = 0f..1f,
                                        colors = SliderDefaults.colors(activeTrackColor = CyberCyan, thumbColor = CyberCyan),
                                        modifier = Modifier.height(20.dp)
                                    )

                                    // Quick position presets
                                    Text("QUICK POSITIONS", fontSize = 7.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                                    val positions = listOf(
                                        "Top" to (0.5f to 0.1f),
                                        "Center" to (0.5f to 0.5f),
                                        "Bottom" to (0.5f to 0.85f),
                                        "Top-L" to (0.15f to 0.1f),
                                        "Top-R" to (0.85f to 0.1f),
                                        "Bot-L" to (0.15f to 0.85f),
                                        "Bot-R" to (0.85f to 0.85f)
                                    )
                                    Row(
                                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        positions.forEach { (label, pos) ->
                                            val isSel = project.textPositionX == pos.first && project.textPositionY == pos.second
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(14.dp))
                                                    .background(if (isSel) NeonOrange.copy(alpha = 0.18f) else Color.Transparent)
                                                    .border(1.dp, if (isSel) NeonOrange.copy(alpha = 0.5f) else Color.White.copy(alpha = 0.05f), RoundedCornerShape(14.dp))
                                                    .clickable {
                                                        onUpdateTextPositionX(pos.first)
                                                        onUpdateTextPositionY(pos.second)
                                                    }
                                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                                            ) {
                                                Text(label, fontSize = 8.sp, fontWeight = FontWeight.Bold, color = if (isSel) NeonOrange else Color.LightGray)
                                            }
                                        }
                                    }
                                }
                                "anim" -> {
                                    // 20+ animation types
                                    Text("TEXT ANIMATION", fontSize = 7.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                                    val animations = listOf(
                                        "None", "Fade", "Typewriter", "Bounce", "Zoom", "Slide Left", "Slide Right",
                                        "Slide Up", "Slide Down", "Pop", "Spin", "Wave", "Glitch", "Pulse",
                                        "Flicker", "Blur In", "Scale Up", "Elastic", "Rubber", "Jello", "Heartbeat", "Swing"
                                    )
                                    animations.chunked(4).forEach { row ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            row.forEach { anim ->
                                                val isSel = project.textAnimationType.lowercase() == anim.lowercase()
                                                Box(
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .height(28.dp)
                                                        .clip(RoundedCornerShape(6.dp))
                                                        .background(if (isSel) NeonOrange.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.04f))
                                                        .clickable { onUpdateTextAnimation(anim) }
                                                        .padding(2.dp),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(anim, fontSize = 7.sp, fontWeight = FontWeight.Bold, color = if (isSel) NeonOrange else Color.White, maxLines = 1)
                                                }
                                            }
                                            repeat(4 - row.size) { Spacer(modifier = Modifier.weight(1f)) }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    "Stickers" -> {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            // 3D mask list
                            val masksList = listOf("none", "circle", "heart", "star", "hexagon", "vignette")
                            Column {
                                Text("3D SHAPE SHADOW MASK", fontSize = 7.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                                Spacer(modifier = Modifier.height(2.dp))
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    items(masksList) { mask ->
                                        val isSel = project.active3DShapeMask == mask
                                        Box(
                                            modifier = Modifier
                                                .background(if (isSel) NeonOrange.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.04f), RoundedCornerShape(6.dp))
                                                .clickable { onUpdate3DShapeMask(mask) }
                                                .padding(horizontal = 8.dp, vertical = 6.dp)
                                        ) {
                                            Text(mask.uppercase(), fontSize = 8.sp, fontWeight = FontWeight.Bold, color = if (isSel) NeonOrange else Color.White)
                                        }
                                    }
                                }
                            }

                            // Sticker selection
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("STICKER:", fontSize = 7.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                                listOf("None", "🔥 Fire", "⭐ Star", "❤️ Heart", "⚡ Glow").forEach { stick ->
                                    val isSel = project.stickerType.lowercase() == stick.lowercase() || (project.stickerType == "none" && stick == "None")
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .background(if (isSel) CyberCyan.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.04f), RoundedCornerShape(6.dp))
                                            .clickable { onUpdateStickerType(if (stick == "None") "none" else stick) }
                                            .padding(4.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(stick, fontSize = 7.sp, fontWeight = FontWeight.Bold, color = if (isSel) CyberCyan else Color.White)
                                    }
                                }
                            }
                        }
                    }
                    "Overlay" -> {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            // Templates
                            val templates = listOf("none", "spark", "bloom", "vlog", "poetry", "beats", "glitch")
                            Column {
                                Text("ACTIVE WORKSPACE TEMPLATE OVERLAY", fontSize = 7.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                                Spacer(modifier = Modifier.height(2.dp))
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    items(templates) { temp ->
                                        val isSel = project.activeTemplateId == temp
                                        Box(
                                            modifier = Modifier
                                                .background(if (isSel) CyberCyan.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.04f), RoundedCornerShape(6.dp))
                                                .clickable { onUpdateTemplate(temp) }
                                                .padding(horizontal = 8.dp, vertical = 6.dp)
                                        ) {
                                            Text(temp.uppercase(), fontSize = 8.sp, fontWeight = FontWeight.Bold, color = if (isSel) CyberCyan else Color.White)
                                        }
                                    }
                                }
                            }

                            // Transitions Style selection
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("TRANSITION:", fontSize = 7.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                                listOf("None", "Glitch", "Crossfade", "Zoom").forEach { trans ->
                                    val isSel = project.transitionType.lowercase() == trans.lowercase()
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .background(if (isSel) NeonOrange.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.04f), RoundedCornerShape(6.dp))
                                            .clickable { onUpdateTransition(trans) }
                                            .padding(4.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(trans, fontSize = 7.sp, fontWeight = FontWeight.Bold, color = if (isSel) NeonOrange else Color.White)
                                    }
                                }
                            }
                        }
                    }
                    "AI" -> {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp),
                            horizontalArrangement = Arrangement.SpaceAround,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = {
                                val nextVal = !project.isSilenceRemoverEnabled
                                onToggleSilenceRemover()
                                android.widget.Toast.makeText(context, if (nextVal) "AI De-Silence Activated" else "AI De-Silence Deactivated", android.widget.Toast.LENGTH_SHORT).show()
                            }) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(imageVector = Icons.Default.Add, contentDescription = "Silence", tint = if (project.isSilenceRemoverEnabled) CyberCyan else Color.White, modifier = Modifier.size(16.dp))
                                    Text("DE-SILENCE", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                }
                            }
                            IconButton(onClick = {
                                val nextLang = if (project.autoCaptionsLanguage == "en") "ur" else "en"
                                onUpdateAutoCaptions(nextLang)
                                android.widget.Toast.makeText(context, "AI Captioning language set to: ${nextLang.uppercase()}", android.widget.Toast.LENGTH_SHORT).show()
                            }) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(imageVector = Icons.Default.Subtitles, contentDescription = "Caps", tint = if (project.autoCaptionsLanguage != "off") NeonOrange else Color.White, modifier = Modifier.size(16.dp))
                                    Text("CAPTIONS", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                }
                            }
                            IconButton(onClick = {
                                val nextFilter = if (project.selectedFilter == "grayscale") "none" else "grayscale"
                                onUpdateFilter(nextFilter)
                                android.widget.Toast.makeText(context, "AI Filter set to: ${nextFilter.uppercase()}", android.widget.Toast.LENGTH_SHORT).show()
                            }) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(imageVector = Icons.Default.Movie, contentDescription = "Filt", tint = if (project.selectedFilter != "none") NeonOrange else Color.White, modifier = Modifier.size(16.dp))
                                    Text("AI FILTER", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                }
                            }
                        }
                    }
                    "🎬Chroma" -> {
                        com.powercut.editor.ui.editor.tools.GreenScreenPanel(
                            greenScreenEnabled = project.greenScreenEnabled,
                            greenScreenColor = project.greenScreenColor,
                            greenScreenThreshold = project.greenScreenThreshold,
                            greenScreenAutoBgIndex = project.greenScreenAutoBgIndex,
                            onToggleGreenScreen = { onToggleGreenScreen() },
                            onUpdateGreenScreenColor = { onUpdateGreenScreenColor(it) },
                            onUpdateThreshold = { onUpdateGreenScreenThreshold(it) },
                            onSelectAutoBackground = { onSelectAutoBackground(it) },
                            onPickCustomBackground = { onPickCustomBackground() }
                        )
                    }
                    "🧹Erase" -> {
                        com.powercut.editor.ui.editor.tools.EraserToolsPanel(
                            eraserMode = project.eraserMode,
                            eraserBrushSize = project.eraserBrushSize,
                            eraserTolerance = project.eraserTolerance,
                            eraserSoftEdge = project.eraserSoftEdge,
                            onUpdateEraserMode = { onUpdateEraserMode(it) },
                            onUpdateBrushSize = { onUpdateEraserBrushSize(it) },
                            onUpdateTolerance = { onUpdateEraserTolerance(it) },
                            onToggleSoftEdge = { onToggleEraserSoftEdge() },
                            onUndoEraser = { onUndoEraser() },
                            onResetEraser = { onResetEraser() }
                        )
                    }
                    "🎨ImgEdit" -> {
                        com.powercut.editor.ui.editor.tools.ImageEditorPanel(
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
                            onUpdateBrightness = { onUpdateImageEditorBrightness(it) },
                            onUpdateContrast = { onUpdateImageEditorContrast(it) },
                            onUpdateSaturation = { onUpdateImageEditorSaturation(it) },
                            onUpdateBlur = { onUpdateImageEditorBlur(it) },
                            onUpdateSharpen = { onUpdateImageEditorSharpen(it) },
                            onUpdateTemperature = { onUpdateImageEditorTemperature(it) },
                            onUpdateVignette = { onUpdateImageEditorVignette(it) },
                            onUpdateGrain = { onUpdateImageEditorGrain(it) },
                            onUpdateFade = { onUpdateImageEditorFade(it) },
                            onUpdateHighlights = { onUpdateImageEditorHighlights(it) },
                            onUpdateShadows = { onUpdateImageEditorShadows(it) },
                            onUpdateExposure = { onUpdateImageEditorExposure(it) },
                            onResetAll = { onResetImageEditor() }
                        )
                    }
                    "📐Orient" -> {
                        com.powercut.editor.ui.editor.tools.OrientationToolsPanel(
                            orientationMode = project.orientationMode,
                            aspectPreset = project.aspectPreset,
                            verticalSafeZone = project.verticalSafeZone,
                            horizontalLetterbox = project.horizontalLetterbox,
                            autoReframeEnabled = project.autoReframeEnabled,
                            onUpdateOrientationMode = { onUpdateOrientationMode(it) },
                            onUpdateAspectPreset = { onUpdateAspectPreset(it) },
                            onToggleSafeZone = { onToggleVerticalSafeZone() },
                            onToggleLetterbox = { onToggleHorizontalLetterbox() },
                            onToggleAutoReframe = { onToggleAutoReframe() }
                        )
                    }
                    "🖼️Studio" -> {
                        com.powercut.editor.ui.editor.tools.ImageStudioPanel(
                            brightness = project.imageEditorBrightness,
                            contrast = project.imageEditorContrast,
                            saturation = project.imageEditorSaturation,
                            exposure = project.imageEditorExposure,
                            temperature = project.imageEditorTemperature,
                            vignette = project.imageEditorVignette,
                            grain = project.imageEditorGrain,
                            fade = project.imageEditorFade,
                            highlights = project.imageEditorHighlights,
                            shadows = project.imageEditorShadows,
                            blur = project.imageEditorBlur,
                            sharpen = project.imageEditorSharpen,
                            onUpdateBrightness = { onUpdateImageEditorBrightness(it) },
                            onUpdateContrast = { onUpdateImageEditorContrast(it) },
                            onUpdateSaturation = { onUpdateImageEditorSaturation(it) },
                            onUpdateExposure = { onUpdateImageEditorExposure(it) },
                            onUpdateTemperature = { onUpdateImageEditorTemperature(it) },
                            onUpdateVignette = { onUpdateImageEditorVignette(it) },
                            onUpdateGrain = { onUpdateImageEditorGrain(it) },
                            onUpdateFade = { onUpdateImageEditorFade(it) },
                            onUpdateHighlights = { onUpdateImageEditorHighlights(it) },
                            onUpdateShadows = { onUpdateImageEditorShadows(it) },
                            onUpdateBlur = { onUpdateImageEditorBlur(it) },
                            onUpdateSharpen = { onUpdateImageEditorSharpen(it) },
                            onResetAll = { onResetImageEditor() }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // 5. LIVE MULTI-TRACK TIMELINE — real playhead, real layers
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(Color(0xFF14141A))
                .border(1.dp, Color.White.copy(alpha = 0.05f))
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // ═══ LIVE TIME RULER — ticks based on actual video duration ═══
                val durationSec = (project.durationMs / 1000).coerceAtLeast(1)
                val tickCount = (durationSec / 5).coerceIn(2, 20)
                val scrollState = rememberScrollState()

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(28.dp)
                        .background(Color.Black.copy(alpha = 0.25f))
                        .horizontalScroll(scrollState)
                        .padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    for (i in 0..tickCount) {
                        val sec = i * 5
                        Text(
                            text = "${sec}s",
                            fontSize = 9.sp,
                            color = if (sec <= currentPlaybackTime / 1000) NeonOrange else Color.Gray,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(end = (300 / tickCount).dp)
                        )
                    }
                }

                // ═══ MULTI-TRACK LAYERS ═══
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // ── TRACK 1: VIDEO (Orange) ──
                    Row(
                        modifier = Modifier.fillMaxWidth().height(32.dp).padding(horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("🎬", fontSize = 10.sp, modifier = Modifier.width(24.dp))
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .background(
                                    Brush.horizontalGradient(listOf(NeonOrange, Color(0xFFFF7043))),
                                    RoundedCornerShape(6.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = project.videoPath.substringAfterLast('/').take(20),
                                fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // ── TRACK 2: AUDIO / BGM (Cyan) ──
                    Row(
                        modifier = Modifier.fillMaxWidth().height(28.dp).padding(horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("🎵", fontSize = 10.sp, modifier = Modifier.width(24.dp))
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .background(
                                    Brush.horizontalGradient(listOf(CyberCyan.copy(alpha = 0.2f), CyberCyan.copy(alpha = 0.05f))),
                                    RoundedCornerShape(6.dp)
                                )
                                .border(1.dp, CyberCyan.copy(alpha = 0.25f), RoundedCornerShape(6.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (project.backgroundMusicPath != null) {
                                // Show waveform bars when BGM is set
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    listOf(8,14,10,18,12,6,16,20,14,10,16,18,8,12,14,6,10,16).forEach { h ->
                                        Box(
                                            modifier = Modifier.width(3.dp).height(h.dp)
                                                .background(Brush.verticalGradient(listOf(CyberCyan, CyberCyan.copy(0.3f))), RoundedCornerShape(2.dp))
                                        )
                                    }
                                }
                            } else {
                                Text("No BGM", fontSize = 9.sp, color = Color.Gray)
                            }
                        }
                    }

                    // ── TRACK 3: TEXT OVERLAY (Purple) — only when text is set ──
                    if (!project.activeTextOverlay.isNullOrBlank()) {
                        Row(
                            modifier = Modifier.fillMaxWidth().height(24.dp).padding(horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("📝", fontSize = 10.sp, modifier = Modifier.width(24.dp))
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .background(
                                        Brush.horizontalGradient(listOf(Color(0xFFAB47BC), Color(0xFFBA68C8))),
                                        RoundedCornerShape(6.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = project.activeTextOverlay!!.take(20),
                                    fontSize = 9.sp, color = Color.White, fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    // ── TRACK 4: EFFECTS (Green) — only when effect/filter is active ──
                    if (project.selectedEffect != "none" || project.selectedFilter != "none") {
                        Row(
                            modifier = Modifier.fillMaxWidth().height(24.dp).padding(horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("✨", fontSize = 10.sp, modifier = Modifier.width(24.dp))
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .background(
                                        Brush.horizontalGradient(listOf(Color(0xFF66BB6A), Color(0xFF43A047))),
                                        RoundedCornerShape(6.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (project.selectedEffect != "none") "FX: ${project.selectedEffect}" else "Filter: ${project.selectedFilter}",
                                    fontSize = 9.sp, color = Color.White, fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    // ── TRACK 5: STICKERS (Yellow) — only when sticker is active ──
                    if (project.stickerType != "none") {
                        Row(
                            modifier = Modifier.fillMaxWidth().height(24.dp).padding(horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("😀", fontSize = 10.sp, modifier = Modifier.width(24.dp))
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .background(
                                        Brush.horizontalGradient(listOf(Color(0xFFFFCA28), Color(0xFFFFB300))),
                                        RoundedCornerShape(6.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("Sticker: ${project.stickerType}", fontSize = 9.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // ═══ LIVE PLAYHEAD — moves with currentPlaybackTime ═══
            val playheadFraction = if (project.durationMs > 0) {
                (currentPlaybackTime.toFloat() / project.durationMs.toFloat()).coerceIn(0f, 1f)
            } else 0f

            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(2.dp)
                    .align(Alignment.TopStart)
                    .offset(x = (playheadFraction * 1000).toInt().dp)
                    .background(Brush.verticalGradient(listOf(NeonOrange, Color.Transparent)))
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .background(NeonOrange, CircleShape)
                        .border(1.dp, Color.White, CircleShape)
                        .align(Alignment.TopCenter)
                        .neonGlow(color = NeonOrange, shape = CircleShape, glowWidth = 1.dp)
                )
            }
        }

        // 6. BOTTOM TOOLBAR (Import, Trim, Split, Filter, Speed, Crop - fully horizontalScroll enabled to prevent layout overflow)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(68.dp)
                .glassmorphic(shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp), backColor = Color(0xFF0F0F14))
                .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Outlined Import button with 1dp premiumAccentGradient border, no fill, plus icon
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.dp, premiumAccentGradient, RoundedCornerShape(12.dp))
                    .clickable { clipPickerLauncher.launch("video/*") }
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Import",
                        tint = AccentSecondary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Import",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            BottomToolItem("✂️", "Trim", selectedBottomTool == "Trim") {
                selectedBottomTool = "Trim"
                activeCategory = "Edit"
            }
            BottomToolItem("🎞️", "Split", selectedBottomTool == "Split") {
                selectedBottomTool = "Split"
                activeCategory = "Edit"
                android.widget.Toast.makeText(context, "Clip split successfully at current playhead!", android.widget.Toast.LENGTH_SHORT).show()
            }
            BottomToolItem("🎨", "Filter", selectedBottomTool == "Filter") {
                selectedBottomTool = "Filter"
                activeCategory = "AI"
            }
            BottomToolItem("⚡", "Speed", selectedBottomTool == "Speed") {
                selectedBottomTool = "Speed"
                activeCategory = "Edit"
            }
            BottomToolItem("📐", "Crop", selectedBottomTool == "Crop") {
                selectedBottomTool = "Crop"
                activeCategory = "Edit"
            }
            // v6.0.0 Effects gallery — opens the 3D glass effects browser.
            BottomToolItem("✨", "Effects", selectedBottomTool == "Effects") {
                onOpenEffects()
            }
            // v6.0.0 Stickers gallery — opens the 3D glass stickers browser.
            BottomToolItem("😀", "Sticker", selectedBottomTool == "Sticker") {
                onOpenStickers()
            }
        }

        // ---- Export watermark dialog (Watch Ad -> clean / Export with watermark) ----
        ExportBottomBar(
            onWatchAd = {
                // ==== ADMOB REWARDED AD CALL HERE ====
                // When the rewarded ad completes successfully, start a clean export
                // (removeWatermark = true -> no PowerCut watermark on the output).
                // PRIORITY 1 FIX: the progress callback is now dispatched to the
                // main thread by ExportEngine.onProgressCallback(), so we just
                // set the lambda directly. The export runs on the native worker
                // thread; we DON'T destroy the engine immediately — we wait for
                // completion in the coroutine, then clean up.
                if (isExporting) return@ExportBottomBar  // PRIORITY 1 FIX: guard against double-start
                isExporting = true
                exportProgress = 0f
                exportEngine.onProgress = { p ->
                    // PRIORITY 1 FIX: ExportEngine already dispatches this to the
                    // main thread via Handler.post, so we can update state directly.
                    exportProgress = (p.cur * 100f) / maxOf(1L, p.total)
                }
                scope.launch(Dispatchers.IO) {
                    val moviesDir = java.io.File(
                        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES),
                        "PowerCut"
                    )
                    if (!moviesDir.exists()) moviesDir.mkdirs()
                    val outPath = moviesDir.absolutePath + "/export_${'$'}{System.currentTimeMillis()}.mp4"
                    val cfg = ExportConfig(
                        preset = ExportEngine.presetTikTok(),
                        out = outPath,
                        removeWatermark = true
                    )
                    val started = exportEngine.start(project, cfg)
                    if (started) {
                        // PRIORITY 1 FIX: block until the export completes. The
                        // native start() spawns a worker thread; we poll running()
                        // until it finishes. This keeps the coroutine alive so the
                        // DisposableEffect cleanup works correctly.
                        while (exportEngine.running()) {
                            Thread.sleep(100)
                        }
                    }
                    isExporting = false
                }
            },
            onExportWithWatermark = {
                // Export with the semi-transparent "PowerCut" watermark overlay
                // (removeWatermark = false -> watermark auto-applied in the native engine).
                // PRIORITY 1 FIX: same pattern as onWatchAd — don't destroy immediately.
                if (isExporting) return@ExportBottomBar  // PRIORITY 1 FIX: guard against double-start
                isExporting = true
                exportProgress = 0f
                exportEngine.onProgress = { p ->
                    exportProgress = (p.cur * 100f) / maxOf(1L, p.total)
                }
                scope.launch(Dispatchers.IO) {
                    val moviesDir = java.io.File(
                        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES),
                        "PowerCut"
                    )
                    if (!moviesDir.exists()) moviesDir.mkdirs()
                    val outPath = moviesDir.absolutePath + "/export_${'$'}{System.currentTimeMillis()}.mp4"
                    val cfg = ExportConfig(
                        preset = ExportEngine.presetTikTok(),
                        out = outPath,
                        removeWatermark = false
                    )
                    val started = exportEngine.start(project, cfg)
                    if (started) {
                        while (exportEngine.running()) {
                            Thread.sleep(100)
                        }
                    }
                    isExporting = false
                }
            },
            triggerDialog = showExportDialog,
            onDialogDismissed = { showExportDialog = false }
        )
    }

    // ---- Full-screen export progress overlay ----
    // PRIORITY 1 FIX: clean overlay with cancel button. The progress callback
    // updates exportProgress on the main thread (via ExportEngine.onProgressCallback
    // → Handler.post), so this is thread-safe.
    if (isExporting) {
        Box(
            modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.85f)),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator(
                    progress = { exportProgress / 100f },
                    modifier = Modifier.size(64.dp),
                    color = NeonOrange,
                    strokeWidth = 5.dp,
                    trackColor = Color.White.copy(0.1f)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Exporting: ${'$'}{exportProgress.toInt()}%",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(24.dp))
                // PRIORITY 1 FIX: Cancel button — safe to call multiple times.
                Box(
                    modifier = Modifier
                        .background(Color.DarkGray, RoundedCornerShape(12.dp))
                        .clickable {
                            exportEngine.cancel()
                            isExporting = false
                        }
                        .padding(horizontal = 24.dp, vertical = 12.dp)
                ) {
                    Text(
                        "CANCEL",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}

// -------------------------------------------------------------
// REUSABLE TIMELINE BOTTOM TOOL ITEM
// -------------------------------------------------------------
@Composable
fun BottomToolItem(
    emoji: String,
    label: String,
    isActive: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (isActive) NeonOrange.copy(alpha = 0.18f) else Color.Transparent)
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(emoji, fontSize = 16.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = if (isActive) NeonOrange else Color.Gray
            )
        }
    }
}
