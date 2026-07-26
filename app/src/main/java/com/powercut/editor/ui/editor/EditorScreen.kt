package com.powercut.editor.ui.editor

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.filled.ArrowBackIos
import androidx.compose.material.icons.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.Flip
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Redo
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material.icons.filled.ShapeLine
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material.icons.filled.VolumeMute
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.powercut.editor.R
import com.powercut.editor.core.utils.LanguageHelper
import com.powercut.editor.core.utils.UriHelper
import com.powercut.editor.data.VideoProject
import com.powercut.editor.domain.ai.AIFilter
import com.powercut.editor.domain.timeline.TimelineHelper
import com.powercut.editor.ui.theme.CyberCyan
import com.powercut.editor.ui.theme.NeonOrange
import com.powercut.editor.ui.theme.glassmorphic
import com.powercut.editor.ui.theme.neonGlow
import com.powercut.editor.ui.theme.tactileClick
import java.io.File
import java.util.Locale

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
    onUpdate3DShapeMask: (String) -> Unit
) {
    val context = LocalContext.current

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
        val mediaItem = MediaItem.fromUri(project.videoPath)
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

    // Monitor Playhead position updates
    LaunchedEffect(isPlaying) {
        while (isPlaying) {
            currentPlaybackTime = exoPlayer.currentPosition
            kotlinx.coroutines.delay(100)
        }
    }

    // React to changes in Mute state and Volume
    LaunchedEffect(project.isMuted, project.videoVolume) {
        exoPlayer.volume = if (project.isMuted) 0f else project.videoVolume
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
                        .tactileClick(onClick = onBack),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBackIos,
                        contentDescription = "Back",
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
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
                        text = "00:00:10 / 00:00:30",
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
                    Icon(imageVector = Icons.Default.Undo, contentDescription = "Undo", tint = Color.LightGray, modifier = Modifier.size(16.dp))
                }
                IconButton(onClick = { /* Redo trigger */ }, modifier = Modifier.size(28.dp)) {
                    Icon(imageVector = Icons.Default.Redo, contentDescription = "Redo", tint = Color.LightGray, modifier = Modifier.size(16.dp))
                }
                Box(
                    modifier = Modifier
                        .neonGlow(color = NeonOrange, shape = RoundedCornerShape(10.dp), glowWidth = 1.dp)
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(NeonOrange, Color(0xFFE64A19))
                            ),
                            shape = RoundedCornerShape(10.dp)
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

        // 2. PREVIEW CONTAINER AREA (Strictly 16:9 Aspect)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(16.dp))
                .background(Color.Black)
                .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center
        ) {
            // Media3 / ExoPlayer View
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        player = exoPlayer
                        useController = false
                    }
                },
                modifier = Modifier.fillMaxSize()
            )

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
                    text = "PowerCut ✨",
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

            // Speed indicator chip (1.0x)
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

        // 4. TOOL CATEGORIES LIST (Horizontal Scrolling)
        val categories = listOf("Edit", "Audio", "Text", "Stickers", "Overlay", "AI")
        var activeCategory by remember { mutableStateOf("Edit") }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            categories.forEach { cat ->
                val isSel = activeCategory == cat
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isSel) NeonOrange.copy(alpha = 0.18f) else Color.Transparent)
                        .border(1.dp, if (isSel) NeonOrange else Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
                        .clickable { activeCategory = cat }
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = cat.uppercase(),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isSel) NeonOrange else Color.LightGray
                    )
                }
            }
        }

        // DYNAMIC CONTEXTUAL OPTIONS PANEL BASED ON ACTIVE CATEGORY
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp)
                .height(96.dp)
                .glassmorphic(shape = RoundedCornerShape(14.dp)),
            contentAlignment = Alignment.Center
        ) {
            when (activeCategory) {
                "Edit" -> {
                    // Trimming, crop, rotation & flips
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onUpdateRotation) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(imageVector = Icons.Default.RotateRight, contentDescription = "Rot", tint = Color.White, modifier = Modifier.size(16.dp))
                                Text("ROTATE", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                        IconButton(onClick = onToggleFlipHorizontal) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(imageVector = Icons.Default.Flip, contentDescription = "FlipH", tint = Color.White, modifier = Modifier.size(16.dp))
                                Text("FLIP H", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                        IconButton(onClick = onToggleFlipVertical) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(imageVector = Icons.Default.Flip, contentDescription = "FlipV", tint = Color.White, modifier = Modifier.size(16.dp))
                                Text("FLIP V", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                        IconButton(onClick = { onUpdateCropPreset("1:1") }) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(imageVector = Icons.Default.Crop, contentDescription = "Crop", tint = Color.White, modifier = Modifier.size(16.dp))
                                Text("CROP 1:1", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                    }
                }
                "Audio" -> {
                    // Audio mixer, volumes, adding background track
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("VIDEO VOLUME", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            Slider(
                                value = project.videoVolume,
                                onValueChange = onUpdateVideoVolume,
                                valueRange = 0.0f..1.0f,
                                colors = SliderDefaults.colors(activeTrackColor = NeonOrange, thumbColor = NeonOrange)
                            )
                        }
                        Box(
                            modifier = Modifier
                                .background(CyberCyan.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                                .clickable { musicPickerLauncher.launch("audio/*") }
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Text("ADD BGM", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = CyberCyan)
                        }
                    }
                }
                "Text" -> {
                    // Text overlays input field
                    var textInput by remember { mutableStateOf(project.activeTextOverlay ?: "") }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = textInput,
                            onValueChange = {
                                textInput = it
                                onUpdateTextOverlay(if (it.isBlank()) null else it)
                            },
                            placeholder = { Text("Burn Subtitle Text...", fontSize = 11.sp, color = Color.Gray) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = NeonOrange,
                                unfocusedBorderColor = Color.White.copy(alpha = 0.1f),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        )
                    }
                }
                "Stickers" -> {
                    // 3D shape mask overlay list
                    val masksList = listOf("none", "circle", "heart", "star", "hexagon", "vignette")
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(horizontal = 12.dp)
                    ) {
                        items(masksList) { mask ->
                            val isSel = project.active3DShapeMask == mask
                            Box(
                                modifier = Modifier
                                    .background(if (isSel) NeonOrange.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.04f), RoundedCornerShape(8.dp))
                                    .clickable { onUpdate3DShapeMask(mask) }
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Text(mask.uppercase(), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = if (isSel) NeonOrange else Color.White)
                            }
                        }
                    }
                }
                "Overlay" -> {
                    // Templates carousel selections
                    val templates = listOf("none", "spark", "bloom", "vlog", "poetry", "beats", "glitch")
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(horizontal = 12.dp)
                    ) {
                        items(templates) { temp ->
                            val isSel = project.activeTemplateId == temp
                            Box(
                                modifier = Modifier
                                    .background(if (isSel) CyberCyan.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.04f), RoundedCornerShape(8.dp))
                                    .clickable { onUpdateTemplate(temp) }
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Text(temp.uppercase(), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = if (isSel) CyberCyan else Color.White)
                            }
                        }
                    }
                }
                "AI" -> {
                    // AI corrections, auto captions, transitions, silence removers
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onToggleSilenceRemover) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(imageVector = Icons.Default.ElectricBolt, contentDescription = "Silence", tint = if (project.isSilenceRemoverEnabled) CyberCyan else Color.White, modifier = Modifier.size(16.dp))
                                Text("DE-SILENCE", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                        IconButton(onClick = { onUpdateAutoCaptions(if (project.autoCaptionsLanguage == "en") "ur" else "en") }) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(imageVector = Icons.Default.Subtitles, contentDescription = "Caps", tint = if (project.autoCaptionsLanguage != "off") NeonOrange else Color.White, modifier = Modifier.size(16.dp))
                                Text("CAPTIONS", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                        IconButton(onClick = { onUpdateFilter(if (project.selectedFilter == "grayscale") "none" else "grayscale") }) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(imageVector = Icons.Default.Movie, contentDescription = "Filt", tint = if (project.selectedFilter != "none") NeonOrange else Color.White, modifier = Modifier.size(16.dp))
                                Text("AI FILTER", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color.White)
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

        // 6. BOTTOM TOOLBAR (Trim, Split, Filter, Speed, Crop)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(68.dp)
                .glassmorphic(shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp), backColor = Color(0xFF0F0F14))
                .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            BottomToolItem("✂️", "Trim", true) { /* Trim trigger */ }
            BottomToolItem("🎞️", "Split", false) { /* Split trigger */ }
            BottomToolItem("🎨", "Filter", false) { /* Filter trigger */ }
            BottomToolItem("⚡", "Speed", false) { /* Speed trigger */ }
            BottomToolItem("📐", "Crop", false) { /* Crop trigger */ }
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
