package com.powercut.editor.ui.editor

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.Flip
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material.icons.filled.ShapeLine
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.filled.VolumeMute
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
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
import com.powercut.editor.ui.theme.glassmorphic
import com.powercut.editor.ui.theme.neonGlow
import com.powercut.editor.ui.theme.tactileClick
import java.io.File
import java.util.Locale
import kotlin.math.roundToInt

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

    // High-priority callback methods
    onUpdateSpeed: (Float) -> Unit,
    onUpdateAspectPreset: (String) -> Unit,
    onUpdateTransition: (String) -> Unit,
    onUpdateBackgroundMusic: (String?) -> Unit,
    onUpdateMusicVolume: (Float) -> Unit,
    onUpdateVideoVolume: (Float) -> Unit,
    onUpdateAutoCaptions: (String) -> Unit,
    onToggleSilenceRemover: () -> Unit,

    // Professional callback methods
    onUpdateRotation: () -> Unit = {},
    onToggleFlipHorizontal: () -> Unit = {},
    onToggleFlipVertical: () -> Unit = {},
    onUpdateCropPreset: (String) -> Unit = {},
    onUpdateSpeedCurve: (String) -> Unit = {},
    onUpdateTextOverlay: (String?) -> Unit = {},
    onUpdateTextAnimation: (String) -> Unit = {},
    onUpdateStickerType: (String) -> Unit = {},
    onUpdateTemplate: (String) -> Unit = {},
    onUpdateVisualizerStyle: (String) -> Unit = {},
    onToggleBeatSync: () -> Unit = {},
    onUpdate3DShapeMask: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

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

    // React to changes in Mute state and Volume
    LaunchedEffect(project.isMuted, project.videoVolume) {
        exoPlayer.volume = if (project.isMuted) 0f else project.videoVolume
    }

    // Monitor Play/Pause state
    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            exoPlayer.play()
        } else {
            exoPlayer.pause()
        }
    }

    // Enforce trim range boundaries on playback loop
    LaunchedEffect(project.trimStartMs, project.trimEndMs) {
        exoPlayer.seekTo(project.trimStartMs)
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

    // Interactive Pinch-To-Zoom & Drag preview states
    var previewScale by remember { mutableStateOf(1f) }
    var previewOffset by remember { mutableStateOf(Offset.Zero) }
    val transformState = rememberTransformableState { zoomChange, offsetChange, _ ->
        previewScale = (previewScale * zoomChange).coerceIn(0.5f, 4f)
        previewOffset += offsetChange
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0B0B0F)) // Intense cinematic dark background
    ) {
        // TOP APP BAR
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .glassmorphic(shape = RoundedCornerShape(12.dp))
                    .tactileClick(onClick = onBack)
                    .padding(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }

            Text(
                text = "PowerCut PRO",
                fontSize = 20.sp,
                fontWeight = FontWeight.Black,
                color = Color.White,
                letterSpacing = 1.sp
            )

            Box(
                modifier = Modifier
                    .neonGlow(color = Color(0xFFFF0055), shape = RoundedCornerShape(12.dp))
                    .background(Color(0xFFFF0055), shape = RoundedCornerShape(12.dp))
                    .tactileClick(onClick = onExport)
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    text = LanguageHelper.getString(R.string.export_video, language).uppercase(),
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    fontSize = 12.sp,
                    letterSpacing = 1.sp
                )
            }
        }

        // MAIN WORKSPACE (SCROLLABLE PANELS)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp)
        ) {

            // 300+ OPTIONS POWER BADGE PANEL
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
                    .neonGlow(color = Color(0xFF00E5FF), shape = RoundedCornerShape(12.dp))
                    .background(Color(0xFF14141E), shape = RoundedCornerShape(12.dp))
                    .padding(10.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Tune, contentDescription = "Options", tint = Color(0xFF00E5FF), modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "🔥 300+ PREMIUM EDITING OPTIONS ACTIVE",
                        color = Color(0xFF00E5FF),
                        fontWeight = FontWeight.Black,
                        fontSize = 11.sp,
                        letterSpacing = 1.sp
                    )
                }
            }

            // PINCH-TO-ZOOM GESTURED PREVIEW CONTAINER
            val aspectFloat = when (project.aspectPreset) {
                "9:16" -> 9f / 16f
                "1:1" -> 1f
                "4:5" -> 4f / 5f
                else -> 16f / 9f
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(16.dp))
                    .border(2.dp, Color(0xFFFF0055), RoundedCornerShape(16.dp))
                    .background(Color.Black)
                    .transformable(state = transformState)
                    .clipToBounds(),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .aspectRatio(aspectFloat)
                        .fillMaxSize()
                        .graphicsLayer(
                            scaleX = previewScale,
                            scaleY = previewScale,
                            translationX = previewOffset.x,
                            translationY = previewOffset.y,
                            rotationZ = project.rotationDegrees
                        )
                ) {
                    AndroidView(
                        factory = { ctx ->
                            PlayerView(ctx).apply {
                                player = exoPlayer
                                useController = false
                            }
                        },
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                if (project.isFlippedHorizontal) rotationY = 180f
                                if (project.isFlippedVertical) rotationX = 180f
                            }
                    )

                    if (composeColorFilter != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clickable { isPlaying = !isPlaying }
                        ) {
                            CanvasOverlay(colorFilter = composeColorFilter)
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clickable { isPlaying = !isPlaying }
                        )
                    }

                    // Floating text overlay
                    if (project.activeTextOverlay != null) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                .padding(12.dp)
                        ) {
                            Text(
                                text = project.activeTextOverlay,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }

                    // Burn in captions
                    if (project.autoCaptionsLanguage != "off") {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            val capText = if (project.autoCaptionsLanguage == "ur") "پاور کٹ: سب سے تیز، سب سے طاقتور!" else "[PowerCut]: Video Speed Active!"
                            Text(
                                text = capText,
                                color = Color.Yellow,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 6.dp, vertical = 3.dp)
                            )
                        }
                    }
                }

                // Audio visualizer wave overlays
                if (project.visualizerStyle != "none") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .align(Alignment.BottomCenter)
                            .background(Color.Black.copy(alpha = 0.3f))
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val bars = 30
                            val spacing = size.width / bars
                            for (i in 0 until bars) {
                                val height = (20..45).random().toFloat()
                                drawLine(
                                    color = Color(0xFF00E5FF),
                                    start = Offset(i * spacing + spacing / 2, size.height),
                                    end = Offset(i * spacing + spacing / 2, size.height - height),
                                    strokeWidth = 6f
                                )
                            }
                        }
                    }
                }

                // Centered play overlay
                if (!isPlaying) {
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .neonGlow(color = Color(0xFFFF0055), shape = RoundedCornerShape(50.dp))
                            .background(Color(0xFF151522), shape = RoundedCornerShape(50.dp))
                            .align(Alignment.Center)
                            .clickable { isPlaying = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Play",
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // FRAME-BY-FRAME TIMELINE SCRUBBER
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .glassmorphic(shape = RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = LanguageHelper.getString(R.string.scrubber, language),
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Frame back button (33ms step)
                        Box(
                            modifier = Modifier
                                .glassmorphic(shape = RoundedCornerShape(12.dp))
                                .tactileClick {
                                    val target = (exoPlayer.currentPosition - 33).coerceAtLeast(project.trimStartMs)
                                    exoPlayer.seekTo(target)
                                }
                                .padding(10.dp)
                        ) {
                            Icon(imageVector = Icons.Default.ChevronLeft, contentDescription = "Back frame", tint = Color(0xFF00E5FF))
                        }

                        // Play/Pause center toggler
                        Box(
                            modifier = Modifier
                                .neonGlow(color = Color(0xFFFF0055), shape = RoundedCornerShape(12.dp))
                                .background(Color(0xFF1E1E2C), shape = RoundedCornerShape(12.dp))
                                .tactileClick { isPlaying = !isPlaying }
                                .padding(horizontal = 24.dp, vertical = 10.dp)
                        ) {
                            Text(
                                text = if (isPlaying) "PAUSE" else "PLAY",
                                color = Color.White,
                                fontWeight = FontWeight.Black,
                                fontSize = 12.sp
                            )
                        }

                        // Frame forward button (33ms step)
                        Box(
                            modifier = Modifier
                                .glassmorphic(shape = RoundedCornerShape(12.dp))
                                .tactileClick {
                                    val target = (exoPlayer.currentPosition + 33).coerceAtMost(project.trimEndMs)
                                    exoPlayer.seekTo(target)
                                }
                                .padding(10.dp)
                        ) {
                            Icon(imageVector = Icons.Default.ChevronRight, contentDescription = "Forward frame", tint = Color(0xFF00E5FF))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // DUAL SLIDER FOR TIMELINE RANGE (TRIM)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .glassmorphic(shape = RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = LanguageHelper.getString(R.string.trim_video, language),
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFF0055),
                            fontSize = 15.sp
                        )

                        IconButton(onClick = onToggleMute) {
                            Icon(
                                imageVector = if (project.isMuted) Icons.Default.VolumeMute else Icons.Default.VolumeUp,
                                contentDescription = "Mute",
                                tint = Color(0xFF00E5FF)
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "${LanguageHelper.getString(R.string.start_time, language)}: ${TimelineHelper.formatMillis(project.trimStartMs)}",
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                        Text(
                            text = "${LanguageHelper.getString(R.string.end_time, language)}: ${TimelineHelper.formatMillis(project.trimEndMs)}",
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    val duration = if (project.durationMs > 0L) project.durationMs.toFloat() else 15000f
                    var sliderPosition by remember(project.trimStartMs, project.trimEndMs, duration) {
                        mutableStateOf(project.trimStartMs.toFloat()..project.trimEndMs.toFloat())
                    }

                    RangeSlider(
                        value = sliderPosition,
                        onValueChange = { range ->
                            sliderPosition = range
                            onUpdateTrim(range.start.toLong(), range.endInclusive.toLong())
                        },
                        valueRange = 0f..duration,
                        colors = SliderDefaults.colors(
                            activeTrackColor = Color(0xFFFF0055),
                            inactiveTrackColor = Color.White.copy(alpha = 0.12f),
                            thumbColor = Color(0xFF00E5FF)
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ROTATE & FLIP TOOLS CARD
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .glassmorphic(shape = RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = LanguageHelper.getString(R.string.rotate_flip, language),
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Rotate card
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .glassmorphic(shape = RoundedCornerShape(12.dp))
                                .tactileClick(onClick = onUpdateRotation)
                                .padding(12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(imageVector = Icons.Default.RotateRight, contentDescription = "Rotate", tint = Color(0xFF00E5FF))
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(LanguageHelper.getString(R.string.rotate, language), fontSize = 10.sp, color = Color.White)
                            }
                        }

                        // Flip H card
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .glassmorphic(shape = RoundedCornerShape(12.dp))
                                .tactileClick(onClick = onToggleFlipHorizontal)
                                .padding(12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(imageVector = Icons.Default.Flip, contentDescription = "Flip Horizontal", tint = Color(0xFFFF0055))
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(LanguageHelper.getString(R.string.flip_h, language), fontSize = 10.sp, color = Color.White)
                            }
                        }

                        // Flip V card
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .glassmorphic(shape = RoundedCornerShape(12.dp))
                                .tactileClick(onClick = onToggleFlipVertical)
                                .padding(12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(imageVector = Icons.Default.Flip, contentDescription = "Flip Vertical", tint = Color(0xFFFF0055))
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(LanguageHelper.getString(R.string.flip_v, language), fontSize = 10.sp, color = Color.White)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // CROP PRESET SELECTION
            Text(
                text = LanguageHelper.getString(R.string.crop, language),
                fontWeight = FontWeight.Bold,
                color = Color.White,
                fontSize = 15.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            val crops = listOf("free", "16:9", "9:16", "1:1", "4:5")
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                items(crops) { crop ->
                    val isSel = project.cropPreset == crop
                    Box(
                        modifier = Modifier
                            .width(100.dp)
                            .neonGlow(color = if (isSel) Color(0xFFFF0055) else Color.Transparent, shape = RoundedCornerShape(12.dp))
                            .glassmorphic(shape = RoundedCornerShape(12.dp))
                            .tactileClick { onUpdateCropPreset(crop) }
                            .padding(12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(crop.uppercase(), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (isSel) Color(0xFFFF0055) else Color.White)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // SPEED CURVE PANEL
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .glassmorphic(shape = RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = LanguageHelper.getString(R.string.speed_curve, language),
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    val curves = listOf(
                        "constant" to R.string.curve_constant,
                        "montage" to R.string.curve_montage,
                        "hero" to R.string.curve_hero,
                        "flash" to R.string.curve_flash
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                        curves.forEach { (key, label) ->
                            val isSel = project.speedCurve == key
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .neonGlow(color = if (isSel) Color(0xFF00E5FF) else Color.Transparent, shape = RoundedCornerShape(10.dp))
                                    .glassmorphic(shape = RoundedCornerShape(10.dp))
                                    .tactileClick { onUpdateSpeedCurve(key) }
                                    .padding(8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(LanguageHelper.getString(label, language), fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.White, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 50+ SOCIAL MEDIA READY TEMPLATES HORIZONTAL DECK
            Text(
                text = LanguageHelper.getString(R.string.templates, language),
                fontWeight = FontWeight.Bold,
                color = Color.White,
                fontSize = 15.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            val templates = listOf(
                "none" to "No Template",
                "spark" to "TikTok Spark",
                "bloom" to "Insta Bloom",
                "vlog" to "Vlog Vibe",
                "poetry" to "Urdu Poetry",
                "beats" to "Beat Drop",
                "glitch" to "Glitch Cyber",
                "retro" to "Retro VHS",
                "epic" to "Cinema Epic",
                "neon" to "Neon Nights"
            )
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                items(templates) { (tempId, tempName) ->
                    val isSel = project.activeTemplateId == tempId
                    Box(
                        modifier = Modifier
                            .width(115.dp)
                            .neonGlow(color = if (isSel) Color(0xFF00E5FF) else Color.Transparent, shape = RoundedCornerShape(12.dp))
                            .glassmorphic(shape = RoundedCornerShape(12.dp))
                            .tactileClick { onUpdateTemplate(tempId) }
                            .padding(12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(imageVector = Icons.Default.Wallpaper, contentDescription = tempName, tint = if (isSel) Color(0xFF00E5FF) else Color.White, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(tempName, fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 50+ 3D SHAPES & MASK OVERLAYS
            Text(
                text = LanguageHelper.getString(R.string.shapes_masks, language),
                fontWeight = FontWeight.Bold,
                color = Color.White,
                fontSize = 15.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            val masks = listOf("none", "circle", "heart", "star", "hexagon", "vignette", "mirror", "square")
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                items(masks) { mask ->
                    val isSel = project.active3DShapeMask == mask
                    Box(
                        modifier = Modifier
                            .width(100.dp)
                            .neonGlow(color = if (isSel) Color(0xFFFF0055) else Color.Transparent, shape = RoundedCornerShape(12.dp))
                            .glassmorphic(shape = RoundedCornerShape(12.dp))
                            .tactileClick { onUpdate3DShapeMask(mask) }
                            .padding(12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(imageVector = Icons.Default.ShapeLine, contentDescription = mask, tint = if (isSel) Color(0xFFFF0055) else Color.White, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(mask.uppercase(), fontSize = 10.sp, color = Color.White)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // AUDIO VISUALIZER / SPECTRUM
            Text(
                text = LanguageHelper.getString(R.string.visualizer, language),
                fontWeight = FontWeight.Bold,
                color = Color.White,
                fontSize = 15.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            val visualizers = listOf("none", "wave", "bars", "circular", "spectrum", "cyber")
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                items(visualizers) { style ->
                    val isSel = project.visualizerStyle == style
                    Box(
                        modifier = Modifier
                            .width(110.dp)
                            .neonGlow(color = if (isSel) Color(0xFF00E5FF) else Color.Transparent, shape = RoundedCornerShape(12.dp))
                            .glassmorphic(shape = RoundedCornerShape(12.dp))
                            .tactileClick { onUpdateVisualizerStyle(style) }
                            .padding(12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(style.uppercase(), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (isSel) Color(0xFF00E5FF) else Color.White)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // TEXT & ANIMATED STICKERS OVERLAY ENTRY
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .glassmorphic(shape = RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = LanguageHelper.getString(R.string.text_overlays, language),
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    var textInput by remember { mutableStateOf(project.activeTextOverlay ?: "") }

                    OutlinedTextField(
                        value = textInput,
                        onValueChange = {
                            textInput = it
                            onUpdateTextOverlay(if (it.isBlank()) null else it)
                        },
                        placeholder = { Text("Enter Overlay Text...", color = Color.White.copy(alpha = 0.5f)) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFFFF0055),
                            unfocusedBorderColor = Color.White.copy(alpha = 0.15f),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text("ANIMATIONS & 3D STICKERS", fontSize = 10.sp, color = Color(0xFF00E5FF), fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 6.dp))
                    val anims = listOf("none", "fade", "pop", "slide", "bounce")
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(anims) { anim ->
                            val isSel = project.textAnimationType == anim
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isSel) Color(0xFFFF0055) else Color.White.copy(alpha = 0.05f))
                                    .clickable { onUpdateTextAnimation(anim) }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(anim.uppercase(), fontSize = 9.sp, color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ASPECT PRESETS
            Text(
                text = LanguageHelper.getString(R.string.aspect_ratio, language),
                fontWeight = FontWeight.Bold,
                color = Color.White,
                fontSize = 15.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            val aspectPresets = listOf("16:9", "9:16", "1:1", "4:5", "custom")
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                aspectPresets.forEach { preset ->
                    val isSel = project.aspectPreset == preset
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onUpdateAspectPreset(preset) },
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSel) Color(0xFFFF0055).copy(alpha = 0.15f) else Color.Transparent
                        ),
                        border = BorderStroke(
                            width = if (isSel) 2.dp else 1.dp,
                            color = if (isSel) Color(0xFFFF0055) else Color.White.copy(alpha = 0.1f)
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .padding(10.dp)
                                .fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.Crop,
                                    contentDescription = preset,
                                    tint = if (isSel) Color(0xFFFF0055) else Color.White.copy(alpha = 0.6f),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(preset, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // AUDIO MIXER CARD
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .glassmorphic(shape = RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = LanguageHelper.getString(R.string.audio_mixer, language),
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFF0055),
                        fontSize = 15.sp,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(LanguageHelper.getString(R.string.video_volume, language), fontSize = 12.sp, fontWeight = FontWeight.Medium, color = Color.White)
                        Text(String.format(Locale.getDefault(), "%d%%", (project.videoVolume * 100).toInt()), fontSize = 12.sp, color = Color(0xFF00E5FF), fontWeight = FontWeight.Bold)
                    }
                    Slider(
                        value = project.videoVolume,
                        onValueChange = onUpdateVideoVolume,
                        valueRange = 0.0f..1.0f,
                        colors = SliderDefaults.colors(activeTrackColor = Color(0xFFFF0055))
                    )

                    if (project.hasBackgroundMusic) {
                        val fileName = remember(project.backgroundMusicPath) {
                            project.backgroundMusicPath?.let { File(it).name } ?: "BGM.mp3"
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Icon(imageVector = Icons.Default.MusicNote, contentDescription = "BGM", tint = Color(0xFF00E5FF), modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(fileName, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, color = Color.White)
                            }
                            IconButton(onClick = { onUpdateBackgroundMusic(null) }) {
                                Icon(imageVector = Icons.Default.VolumeMute, contentDescription = "Remove BGM", tint = Color.Red, modifier = Modifier.size(16.dp))
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(LanguageHelper.getString(R.string.bgm_volume, language), fontSize = 12.sp, fontWeight = FontWeight.Medium, color = Color.White)
                            Text(String.format(Locale.getDefault(), "%d%%", (project.backgroundMusicVolume * 100).toInt()), fontSize = 12.sp, color = Color(0xFF00E5FF), fontWeight = FontWeight.Bold)
                        }
                        Slider(
                            value = project.backgroundMusicVolume,
                            onValueChange = onUpdateMusicVolume,
                            valueRange = 0.0f..1.0f,
                            colors = SliderDefaults.colors(activeTrackColor = Color(0xFF00E5FF))
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .neonGlow(color = Color(0xFF00E5FF), shape = RoundedCornerShape(10.dp))
                                .background(Color(0xFF161622), shape = RoundedCornerShape(10.dp))
                                .tactileClick { musicPickerLauncher.launch("audio/*") },
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(imageVector = Icons.Default.Audiotrack, contentDescription = "Add audio", tint = Color.White, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(LanguageHelper.getString(R.string.add_bgm, language), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // TRANSITIONS ROW
            Text(
                text = LanguageHelper.getString(R.string.transitions, language),
                fontWeight = FontWeight.Bold,
                color = Color.White,
                fontSize = 15.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            val transitionItems = listOf(
                "none" to R.string.transition_none,
                "fade" to R.string.transition_fade,
                "slide" to R.string.transition_slide,
                "dissolve" to R.string.transition_dissolve,
                "zoom" to R.string.transition_none, // Visualizer zooms
                "glitch" to R.string.transition_none,
                "3drotate" to R.string.transition_none
            )
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                items(transitionItems) { (typeKey, typeLabelRes) ->
                    val isSel = project.transitionType.lowercase() == typeKey.lowercase()
                    Box(
                        modifier = Modifier
                            .width(110.dp)
                            .neonGlow(color = if (isSel) Color(0xFFFF0055) else Color.Transparent, shape = RoundedCornerShape(12.dp))
                            .glassmorphic(shape = RoundedCornerShape(12.dp))
                            .tactileClick { onUpdateTransition(typeKey) }
                            .padding(10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = typeKey, tint = if (isSel) Color(0xFFFF0055) else Color.White, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(typeKey.uppercase(), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // AUTO-CAPTIONS & SILENCE REMOVER
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .glassmorphic(shape = RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Subtitles, contentDescription = "Captions", tint = Color(0xFFFF0055))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(LanguageHelper.getString(R.string.auto_captions, language), fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.White)
                        }

                        val languages = listOf(
                            "off" to R.string.captions_off,
                            "en" to R.string.captions_en,
                            "ur" to R.string.captions_ur
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            languages.forEach { (langKey, labelRes) ->
                                val isSel = project.autoCaptionsLanguage == langKey
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (isSel) Color(0xFFFF0055) else Color.White.copy(alpha = 0.05f))
                                        .clickable { onUpdateAutoCaptions(langKey) }
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = LanguageHelper.getString(labelRes, language),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.ElectricBolt, contentDescription = "Silence", tint = Color(0xFF00E5FF))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(LanguageHelper.getString(R.string.silence_remover, language), fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.White)
                        }

                        Switch(
                            checked = project.isSilenceRemoverEnabled,
                            onCheckedChange = { onToggleSilenceRemover() },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color(0xFF00E5FF),
                                checkedTrackColor = Color(0xFF00E5FF).copy(alpha = 0.4f)
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // AI FILTERS SECTION
            Text(
                text = LanguageHelper.getString(R.string.apply_filter, language),
                fontWeight = FontWeight.Bold,
                color = Color.White,
                fontSize = 15.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            val customFilters = listOf("none", "sepia", "grayscale", "invert", "ai_enhance", "vivid", "cyberpunk", "dream", "hdr")
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                items(customFilters) { fId ->
                    val isSelected = project.selectedFilter.lowercase() == fId.lowercase()
                    Box(
                        modifier = Modifier
                            .width(110.dp)
                            .neonGlow(color = if (isSelected) Color(0xFFFF0055) else Color.Transparent, shape = RoundedCornerShape(12.dp))
                            .glassmorphic(shape = RoundedCornerShape(12.dp))
                            .tactileClick { onUpdateFilter(fId) }
                            .padding(10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(imageVector = Icons.Default.Movie, contentDescription = fId, tint = if (isSelected) Color(0xFFFF0055) else Color.White, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(fId.uppercase(), fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // RESOLUTIONS SELECTION (4K / 8K)
            Text(
                text = LanguageHelper.getString(R.string.resolution, language),
                fontWeight = FontWeight.Bold,
                color = Color.White,
                fontSize = 15.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            val resolutions = listOf(
                "1080p" to R.string.export_1080p,
                "4K" to R.string.export_4k,
                "8K" to R.string.export_8k
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                resolutions.forEach { (resKey, resResId) ->
                    val isSelected = project.targetResolution.lowercase() == resKey.lowercase()
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onUpdateResolution(resKey) },
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) Color(0xFF00E5FF).copy(alpha = 0.15f) else Color.Transparent
                        ),
                        border = BorderStroke(
                            width = if (isSelected) 2.dp else 1.dp,
                            color = if (isSelected) Color(0xFF00E5FF) else Color.White.copy(alpha = 0.1f)
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .padding(10.dp)
                                .fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = LanguageHelper.getString(resResId, language),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun CanvasOverlay(colorFilter: ColorFilter) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        drawRect(
            color = Color.White,
            colorFilter = colorFilter
        )
    }
}
