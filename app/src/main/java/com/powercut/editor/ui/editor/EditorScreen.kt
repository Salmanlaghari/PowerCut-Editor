package com.powercut.editor.ui.editor

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.filled.VolumeMute
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
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

    // High-priority features callbacks
    onUpdateSpeed: (Float) -> Unit = {},
    onUpdateAspectPreset: (String) -> Unit = {},
    onUpdateTransition: (String) -> Unit = {},
    onUpdateBackgroundMusic: (String?) -> Unit = {},
    onUpdateMusicVolume: (Float) -> Unit = {},
    onUpdateVideoVolume: (Float) -> Unit = {},
    onUpdateAutoCaptions: (String) -> Unit = {},
    onToggleSilenceRemover: () -> Unit = {}
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    // BGM Local Audio Picker
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // TOP ACTION BAR
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }

            Text(
                text = LanguageHelper.getString(R.string.editor, language),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Button(
                onClick = onExport,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = LanguageHelper.getString(R.string.export_video, language),
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // SCROLLABLE AREA
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp)
        ) {
            // VIDEO PREVIEW BOX WITH ASYNC FILTER OVERLAY & PRESET ASPECT RATIO WRAPPING
            val aspectFloat = when (project.aspectPreset) {
                "9:16" -> 9f / 16f
                "1:1" -> 1f
                "4:5" -> 4f / 5f
                else -> 16f / 9f
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f) // Always fit the viewport 16:9 box safely
                    .clip(RoundedCornerShape(12.dp))
                    .border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp))
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                // Wrapper scaled container representing the active preset aspect ratio
                Box(
                    modifier = Modifier
                        .aspectRatio(aspectFloat)
                        .fillMaxSize()
                ) {
                    // Media3 Player View
                    AndroidView(
                        factory = { ctx ->
                            PlayerView(ctx).apply {
                                player = exoPlayer
                                useController = false
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )

                    // Compose Filter paint overlay
                    if (composeColorFilter != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.05f))
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

                    // Burn in captions preview on screen if active
                    if (project.autoCaptionsLanguage != "off") {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            val capText = if (project.autoCaptionsLanguage == "ur") "پاور کٹ: سب سے تیز، سب سے طاقتور!" else "[PowerCut]: Fastest & Most Powerful!"
                            Text(
                                text = capText,
                                color = Color.Yellow,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }

                // Play icon overlay
                if (!isPlaying) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .background(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                                RoundedCornerShape(50)
                            )
                            .align(Alignment.Center)
                            .clickable { isPlaying = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Play",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // DUAL SLIDER FOR TIMELINE RANGE (TRIM)
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
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
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 15.sp
                        )

                        IconButton(onClick = onToggleMute) {
                            Icon(
                                imageVector = if (project.isMuted) Icons.Default.VolumeMute else Icons.Default.VolumeUp,
                                contentDescription = "Mute",
                                tint = MaterialTheme.colorScheme.secondary
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
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                        Text(
                            text = "${LanguageHelper.getString(R.string.end_time, language)}: ${TimelineHelper.formatMillis(project.trimEndMs)}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
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
                            activeTrackColor = MaterialTheme.colorScheme.primary,
                            inactiveTrackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f),
                            thumbColor = MaterialTheme.colorScheme.secondary
                        )
                    )

                    Text(
                        text = LanguageHelper.getString(R.string.instant_trim_desc, language),
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.secondary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ASPECT RATIO PRESETS
            Text(
                text = LanguageHelper.getString(R.string.aspect_ratio, language),
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 15.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            val aspectPresets = listOf("16:9", "9:16", "1:1", "4:5")
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
                            containerColor = if (isSel) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface
                        ),
                        border = BorderStroke(
                            width = if (isSel) 2.dp else 1.dp,
                            color = if (isSel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
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
                                    tint = if (isSel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(preset, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // SPEED CONTROL SLIDER (0.1X to 16X) WITH RAMPS
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = LanguageHelper.getString(R.string.video_speed, language),
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 15.sp
                        )
                        Text(
                            text = String.format(Locale.getDefault(), "%.1fx", project.speedFactor),
                            color = MaterialTheme.colorScheme.secondary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }

                    Slider(
                        value = project.speedFactor,
                        onValueChange = onUpdateSpeed,
                        valueRange = 0.1f..16.0f,
                        steps = 159, // steps every 0.1
                        colors = SliderDefaults.colors(
                            activeTrackColor = MaterialTheme.colorScheme.secondary,
                            thumbColor = MaterialTheme.colorScheme.primary
                        )
                    )

                    // Quick speed ramping shortcuts
                    val speeds = listOf(0.25f, 0.5f, 1.0f, 2.0f, 4.0f, 8.0f, 16.0f)
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(speeds) { sp ->
                            val isSel = project.speedFactor == sp
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isSel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                                    .border(1.dp, if (isSel) MaterialTheme.colorScheme.primary else Color.Transparent, RoundedCornerShape(6.dp))
                                    .clickable { onUpdateSpeed(sp) }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "${sp}x",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSel) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // AUDIO MIXER CARD (Main volume, Background Music, Adding track)
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = LanguageHelper.getString(R.string.audio_mixer, language),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 15.sp,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    // Video clip volume
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(LanguageHelper.getString(R.string.video_volume, language), fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        Text(String.format(Locale.getDefault(), "%d%%", (project.videoVolume * 100).toInt()), fontSize = 12.sp, color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Bold)
                    }
                    Slider(
                        value = project.videoVolume,
                        onValueChange = onUpdateVideoVolume,
                        valueRange = 0.0f..1.0f,
                        colors = SliderDefaults.colors(activeTrackColor = MaterialTheme.colorScheme.primary)
                    )

                    // BGM Track volume (only if added)
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
                                Icon(imageVector = Icons.Default.MusicNote, contentDescription = "BGM", tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(fileName, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                            }
                            IconButton(onClick = { onUpdateBackgroundMusic(null) }) {
                                Icon(imageVector = Icons.Default.VolumeMute, contentDescription = "Remove BGM", tint = Color.Red, modifier = Modifier.size(16.dp))
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(LanguageHelper.getString(R.string.bgm_volume, language), fontSize = 12.sp, fontWeight = FontWeight.Medium)
                            Text(String.format(Locale.getDefault(), "%d%%", (project.backgroundMusicVolume * 100).toInt()), fontSize = 12.sp, color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Bold)
                        }
                        Slider(
                            value = project.backgroundMusicVolume,
                            onValueChange = onUpdateMusicVolume,
                            valueRange = 0.0f..1.0f,
                            colors = SliderDefaults.colors(activeTrackColor = MaterialTheme.colorScheme.secondary)
                        )
                    } else {
                        OutlinedButton(
                            onClick = { musicPickerLauncher.launch("audio/*") },
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.secondary),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Audiotrack, contentDescription = "Add audio", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(LanguageHelper.getString(R.string.add_bgm, language), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // TRANSITIONS ROW
            Text(
                text = LanguageHelper.getString(R.string.transitions, language),
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 15.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            val transitionItems = listOf(
                "none" to R.string.transition_none,
                "fade" to R.string.transition_fade,
                "slide" to R.string.transition_slide,
                "dissolve" to R.string.transition_dissolve
            )
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(transitionItems) { (typeKey, typeLabelRes) ->
                    val isSel = project.transitionType.lowercase() == typeKey.lowercase()
                    Card(
                        modifier = Modifier
                            .width(110.dp)
                            .clickable { onUpdateTransition(typeKey) },
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSel) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface
                        ),
                        border = BorderStroke(
                            width = if (isSel) 2.dp else 1.dp,
                            color = if (isSel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = typeKey,
                                tint = if (isSel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(LanguageHelper.getString(typeLabelRes, language), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // AUTO-CAPTIONS & SILENCE REMOVER CARDS
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Subtitles, contentDescription = "Captions", tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(LanguageHelper.getString(R.string.auto_captions, language), fontWeight = FontWeight.Bold, fontSize = 14.sp)
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
                                        .background(if (isSel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                                        .clickable { onUpdateAutoCaptions(langKey) }
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = LanguageHelper.getString(labelRes, language),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSel) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
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
                            Icon(imageVector = Icons.Default.ElectricBolt, contentDescription = "Silence", tint = MaterialTheme.colorScheme.secondary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(LanguageHelper.getString(R.string.silence_remover, language), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }

                        Switch(
                            checked = project.isSilenceRemoverEnabled,
                            onCheckedChange = { onToggleSilenceRemover() },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colorScheme.secondary,
                                checkedTrackColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f)
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
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 15.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(AIFilter.all) { filter ->
                    val isSelected = project.selectedFilter.lowercase() == filter.id.lowercase()
                    Card(
                        modifier = Modifier
                            .width(110.dp)
                            .clickable { onUpdateFilter(filter.id) },
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface
                        ),
                        border = BorderStroke(
                            width = if (isSelected) 2.dp else 1.dp,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .padding(10.dp)
                                .fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .background(
                                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f),
                                        RoundedCornerShape(50)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Movie,
                                    contentDescription = filter.id,
                                    tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = LanguageHelper.getString(filter.nameResId, language),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // RESOLUTIONS SELECTION (4K / 8K)
            Text(
                text = LanguageHelper.getString(R.string.resolution, language),
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
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
                            containerColor = if (isSelected) MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface
                        ),
                        border = BorderStroke(
                            width = if (isSelected) 2.dp else 1.dp,
                            color = if (isSelected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
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
                                color = MaterialTheme.colorScheme.onSurface
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
