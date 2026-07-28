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
    onSaveDraft: () -> Unit
) {
    val context = LocalContext.current

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
            while (isPlaying) {
                currentPlaybackTime = exoPlayer.currentPosition
                kotlinx.coroutines.delay(100)
            }
        } else {
            exoPlayer.pause()
            // Pauses editing/playback for 3+ seconds auto-save trigger!
            kotlinx.coroutines.delay(3000)
            if (!isPlaying) {
                onSaveDraft()
            }
        }
    }

    // React to changes in Mute state and Volume
    LaunchedEffect(project.isMuted, project.videoVolume) {
        exoPlayer.volume = if (project.isMuted) 0f else project.videoVolume
    }

    // Sync ExoPlayer speed dynamically with the project selected speedFactor
    LaunchedEffect(project.speedFactor) {
        exoPlayer.playbackParameters = PlaybackParameters(project.speedFactor)
    }

    // Clean up player on leave
    DisposableEffect(Unit) {
        onDispose {
            exoPlayer.release()
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
                        .tactileClick(onClick = onExport)
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

                // Sample text overlay centered at bottom
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 12.dp)
                        .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = project.activeTextOverlay ?: "PowerCut ✨",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
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
        val categories = listOf("Edit", "Audio", "Text", "Stickers", "Overlay", "AI")
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
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            var textInput by remember { mutableStateOf(project.activeTextOverlay ?: "") }
                            OutlinedTextField(
                                value = textInput,
                                onValueChange = {
                                    textInput = it
                                    onUpdateTextOverlay(if (it.isBlank()) null else it)
                                },
                                placeholder = { Text("Burn Subtitle Text...", fontSize = 9.sp, color = Color.Gray) },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = NeonOrange,
                                    unfocusedBorderColor = Color.White.copy(alpha = 0.1f),
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                ),
                                modifier = Modifier.fillMaxWidth().height(48.dp),
                                shape = RoundedCornerShape(8.dp)
                            )

                            // Text animations Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("ANIMATION:", fontSize = 7.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                                listOf("None", "Fade", "Typewriter", "Bounce", "Zoom").forEach { anim ->
                                    val isSel = project.textAnimationType.lowercase() == anim.lowercase()
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .background(if (isSel) NeonOrange.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.04f), RoundedCornerShape(6.dp))
                                            .clickable { onUpdateTextAnimation(anim) }
                                            .padding(4.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(anim, fontSize = 7.sp, fontWeight = FontWeight.Bold, color = if (isSel) NeonOrange else Color.White)
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
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // 5. PROFESSIONAL MULTI-TRACK TIMELINE
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(Color(0xFF14141A)) // Dark slate timeline background
                .border(1.dp, Color.White.copy(alpha = 0.05f))
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Time Ruler (0s to 30s ticks)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(28.dp)
                        .background(Color.Black.copy(alpha = 0.25f))
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    listOf("0s", "5s", "10s", "15s", "20s", "25s", "30s").forEach { tick ->
                        Text(tick, fontSize = 9.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                    }
                }

                // Scrolling Tracks wrapper
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Track 1: Video Track (Orange gradient clips with transitions)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(34.dp)
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Clip 1
                        Box(
                            modifier = Modifier
                                .weight(0.45f)
                                .fillMaxHeight()
                                .background(
                                    Brush.horizontalGradient(listOf(NeonOrange, Color(0xFFFF7043))),
                                    RoundedCornerShape(6.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Clip 1.mp4", fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold)
                        }

                        // Transition Block
                        Box(
                            modifier = Modifier
                                .size(18.dp)
                                .background(Color.White.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(4.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = "Trans", tint = Color.White, modifier = Modifier.size(10.dp))
                        }

                        // Clip 2
                        Box(
                            modifier = Modifier
                                .weight(0.45f)
                                .fillMaxHeight()
                                .background(
                                    Brush.horizontalGradient(listOf(Color(0xFFFF7043), NeonOrange)),
                                    RoundedCornerShape(6.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Clip 2.mp4", fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }

                    // Track 2: Audio Track (Cyan gradient with simulated waveform bars)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(28.dp)
                            .padding(horizontal = 16.dp)
                            .background(
                                Brush.horizontalGradient(listOf(CyberCyan.copy(alpha = 0.15f), CyberCyan.copy(alpha = 0.05f))),
                                RoundedCornerShape(6.dp)
                            )
                            .border(1.dp, CyberCyan.copy(alpha = 0.25f), RoundedCornerShape(6.dp))
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Waveform bars
                            listOf(10, 18, 12, 22, 14, 8, 20, 24, 16, 12, 18, 22, 10, 14, 20, 8, 16, 24).forEach { height ->
                                Box(
                                    modifier = Modifier
                                        .width(4.dp)
                                        .height(height.dp)
                                        .background(
                                            Brush.verticalGradient(
                                                listOf(CyberCyan, CyberCyan.copy(alpha = 0.3f))
                                            ),
                                            RoundedCornerShape(2.dp)
                                        )
                                )
                            }
                        }
                    }

                    // Track 3: Text track (Purple gradient clip)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(24.dp)
                            .padding(horizontal = 16.dp)
                    ) {
                        Spacer(modifier = Modifier.weight(0.2f))
                        Box(
                            modifier = Modifier
                                .weight(0.6f)
                                .fillMaxHeight()
                                .background(
                                    Brush.horizontalGradient(listOf(Color(0xFFAB47BC), Color(0xFFBA68C8))),
                                    RoundedCornerShape(6.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Subtitle.srt", fontSize = 9.sp, color = Color.White, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.weight(0.2f))
                    }
                }
            }

            // Vertical Playhead Line with glow head
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(2.dp)
                    .align(Alignment.Center)
                    .background(
                        Brush.verticalGradient(
                            listOf(NeonOrange, Color.Transparent)
                        )
                    )
            ) {
                // Playhead head knob
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
