package com.powercut.editor.ui.filters

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.powercut.editor.domain.ai.ColorGradingPreset
import kotlin.math.roundToInt

// ──────────────────────────────────────────────────────────────────────────────
// Color palette matching reference image exactly
// ──────────────────────────────────────────────────────────────────────────────
private val BackgroundDark = Color(0xFF0A0A0F)
private val PanelDark = Color(0xFF141420)
private val PanelGlass = Color(0xE0141420)
private val SliderCyan = Color(0xFF00D4FF)
private val SliderTrack = Color(0xFF1E2A3A)
private val SliderInactive = Color(0xFF2A3040)
private val TextWhite = Color(0xFFFFFFFF)
private val TextGray = Color(0xFF7A7A8E)
private val TextDim = Color(0xFF555566)
private val ActiveGlow = Color(0xFF00D4FF)
private val TabActive = Color(0xFF00D4FF)
private val TabInactive = Color(0xFF555566)
private val DragHandle = Color(0xFF3A3A4A)

// ──────────────────────────────────────────────────────────────────────────────
// Main composable — full-screen editor layout matching reference
// ──────────────────────────────────────────────────────────────────────────────
@Composable
fun FilterDrawer(
    activeFilter: FilterPreset?,
    onFilterSelected: (FilterPreset) -> Unit,
    onIntensityChanged: (Float) -> Unit,
    onCancel: () -> Unit,
    onDone: () -> Unit,
    onTabSelected: (EditorTab) -> Unit,
    selectedTab: EditorTab,
    isPlaying: Boolean,
    onTogglePlayPause: () -> Unit,
    playbackPosition: Long,
    totalDuration: Long,
    playbackSpeed: Float,
    modifier: Modifier = Modifier
) {
    var intensity by remember { mutableFloatStateOf(0.75f) }
    var selectedCategory by remember { mutableStateOf(FilterCategory.COLOR_LUTS) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundDark)
    ) {
        // ── Top Bar: Cancel / PixelEdit / Done ──
        TopBar(onCancel = onCancel, onDone = onDone)

        // ── Video Preview Area (~55%) ──
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            // Video preview placeholder (dark rounded rect with gradient)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(9f / 12f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF1A1020),
                                Color(0xFF0D0D18)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                // Filter preview indicator
                if (activeFilter != null) {
                    Text(
                        text = activeFilter.displayName,
                        color = ActiveGlow.copy(alpha = 0.6f),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Play/Pause button overlay
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.4f))
                    .clickable(onClick = onTogglePlayPause),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    tint = TextWhite,
                    modifier = Modifier.size(32.dp)
                )
            }

            // Bottom controls overlay (timestamp + speed)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 8.dp)
            ) {
                // Timestamp row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${formatTime(playbackPosition)} / ${formatTime(totalDuration)}",
                        color = TextWhite.copy(alpha = 0.8f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "${playbackSpeed}x",
                        color = TextWhite.copy(alpha = 0.8f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Timeline scrubber
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(32.dp)
                        .padding(horizontal = 8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color.Black.copy(alpha = 0.3f))
                ) {
                    // Progress fill
                    val progress = if (totalDuration > 0) playbackPosition.toFloat() / totalDuration else 0f
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(fraction = progress)
                            .clip(RoundedCornerShape(4.dp))
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(SliderCyan.copy(alpha = 0.3f), SliderCyan.copy(alpha = 0.5f))
                                )
                            )
                    )
                    // Playhead
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .width(2.dp)
                            .align(Alignment.CenterStart)
                            .offset(x = (progress * 100).dp * 0.01f)
                            .clip(RoundedCornerShape(1.dp))
                            .background(SliderCyan)
                    )
                }
            }
        }

        // ── Bottom Panel (glassmorphic) ──
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.55f)
                .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                .background(PanelGlass)
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Drag handle
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .width(36.dp)
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(DragHandle)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // ── Intensity Slider ──
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                ) {
                    // Slider with percentage on right
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Slider(
                            value = intensity,
                            onValueChange = {
                                intensity = it
                                onIntensityChanged(it)
                            },
                            valueRange = 0f..1f,
                            modifier = Modifier.weight(1f),
                            colors = SliderDefaults.colors(
                                thumbColor = SliderCyan,
                                activeTrackColor = SliderCyan,
                                inactiveTrackColor = SliderInactive
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "${(intensity * 100).roundToInt()}%",
                            color = SliderCyan,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.width(40.dp)
                        )
                    }

                    // Filter name (centered below slider)
                    if (activeFilter != null) {
                        Text(
                            text = activeFilter.displayName,
                            color = TextWhite,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // ── Filter Carousel (circular thumbnails) ──
                val filters = FilterPreset.entries.filter { it.category == selectedCategory }
                LazyRow(
                    modifier = Modifier.height(120.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    items(filters) { filter ->
                        CircularFilterThumbnail(
                            filter = filter,
                            isActive = filter == activeFilter,
                            onClick = { onFilterSelected(filter) }
                        )
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                // ── Bottom Tab Bar: Filters | Effects | Adjust | Stickers | Music ──
                EditorTabBar(
                    selectedTab = selectedTab,
                    onTabSelected = onTabSelected
                )
            }
        }
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// Top Bar
// ──────────────────────────────────────────────────────────────────────────────
@Composable
private fun TopBar(onCancel: () -> Unit, onDone: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Cancel",
            color = TextWhite,
            fontSize = 16.sp,
            fontWeight = FontWeight.Normal,
            modifier = Modifier.clickable(onClick = onCancel)
        )
        Text(
            text = "PixelEdit",
            color = TextWhite,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Done",
            color = SliderCyan,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.clickable(onClick = onDone)
        )
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// Circular Filter Thumbnail (matching reference exactly)
// ──────────────────────────────────────────────────────────────────────────────
@Composable
private fun CircularFilterThumbnail(
    filter: FilterPreset,
    isActive: Boolean,
    onClick: () -> Unit
) {
    val borderWidth by animateDpAsState(
        targetValue = if (isActive) 3.dp else 0.dp,
        label = "borderWidth"
    )
    val glowRadius by animateFloatAsState(
        targetValue = if (isActive) 12f else 0f,
        label = "glowRadius"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(72.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center
        ) {
            // Glow effect behind active thumbnail
            if (isActive) {
                Box(
                    modifier = Modifier
                        .size(68.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    ActiveGlow.copy(alpha = 0.4f),
                                    ActiveGlow.copy(alpha = 0.0f)
                                )
                            )
                        )
                )
            }

            // Circular thumbnail
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .shadow(
                        elevation = if (isActive) 8.dp else 0.dp,
                        shape = CircleShape,
                        ambientColor = ActiveGlow.copy(alpha = 0.5f),
                        spotColor = ActiveGlow.copy(alpha = 0.5f)
                    )
                    .clip(CircleShape)
                    .border(
                        width = borderWidth,
                        color = if (isActive) ActiveGlow else Color.Transparent,
                        shape = CircleShape
                    )
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                filter.previewColor.copy(alpha = 0.5f),
                                filter.previewColor.copy(alpha = 0.2f)
                            )
                        )
                    )
                    .clickable(onClick = onClick),
                contentAlignment = Alignment.Center
            ) {
                // Filter icon
                Icon(
                    imageVector = filter.icon,
                    contentDescription = filter.displayName,
                    tint = if (isActive) ActiveGlow else filter.previewColor,
                    modifier = Modifier.size(28.dp)
                )

                // Active checkmark badge
                if (isActive) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .size(18.dp)
                            .clip(CircleShape)
                            .background(ActiveGlow),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Active",
                            tint = Color.Black,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Filter name below thumbnail
        Text(
            text = filter.shortName,
            color = if (isActive) ActiveGlow else TextGray,
            fontSize = 10.sp,
            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// Editor Tab Bar (Filters | Effects | Adjust | Stickers | Music)
// ──────────────────────────────────────────────────────────────────────────────
enum class EditorTab(val displayName: String, val icon: ImageVector) {
    MEDIA("Media", Icons.Default.VideoLibrary),
    EFFECTS("Effects", Icons.Default.AutoAwesome),
    FILTER("Filter", Icons.Default.Tune),
    AUDIO("Audio", Icons.Default.MusicNote),
    EXPORT("Export", Icons.Default.Upload)
}

@Composable
private fun EditorTabBar(
    selectedTab: EditorTab,
    onTabSelected: (EditorTab) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 8.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        EditorTab.entries.forEach { tab ->
            val isSelected = tab == selectedTab
            val color = if (isSelected) TabActive else TabInactive

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .weight(1f)
                    .clickable { onTabSelected(tab) }
                    .padding(vertical = 4.dp)
            ) {
                Icon(
                    imageVector = tab.icon,
                    contentDescription = tab.displayName,
                    tint = color,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = tab.displayName,
                    color = color,
                    fontSize = 10.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                )
            }
        }
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// Filter Categories (for carousel filtering)
// ──────────────────────────────────────────────────────────────────────────────
enum class FilterCategory(val displayName: String) {
    AI_FX("AI FX"),
    COLOR_LUTS("Color LUTs"),
    BEAUTY("Beauty"),
    BACKGROUND("Background"),
    AR_MASKS("AR Masks"),
    AUDIO_FX("Audio FX")
}

// ──────────────────────────────────────────────────────────────────────────────
// Filter Presets (matching reference thumbnail order)
// ──────────────────────────────────────────────────────────────────────────────
enum class FilterPreset(
    val displayName: String,
    val shortName: String,
    val category: FilterCategory,
    val icon: ImageVector,
    val previewColor: Color
) {
    // AI Filters (from reference image)
    AI_GLOW("AI Glow", "AI Glow", FilterCategory.COLOR_LUTS, Icons.Default.AutoAwesome, Color(0xFF00FF88)),
    CYBERPUNK("Cyberpunk", "Cyberpunk", FilterCategory.COLOR_LUTS, Icons.Default.Bolt, Color(0xFFFF00FF)),
    FILM_35MM("Film 35mm", "Film 35mm", FilterCategory.COLOR_LUTS, Icons.Default.CameraAlt, Color(0xFFD4A574)),
    BEAUTY_PRO("Beauty Pro", "Beauty Pro", FilterCategory.COLOR_LUTS, Icons.Default.Face, Color(0xFFFF6B9D)),

    // Additional filters
    TEAL_ORANGE("Teal & Orange", "T&O", FilterCategory.COLOR_LUTS, Icons.Default.Tonality, Color(0xFF00897B)),
    DRAMATIC("Dramatic", "Dramatic", FilterCategory.COLOR_LUTS, Icons.Default.TheaterComedy, Color(0xFFB71C1C)),
    CINEMATIC("Cinematic", "Cinema", FilterCategory.COLOR_LUTS, Icons.Default.Theaters, Color(0xFFD4A574)),
    RETRO_FILM("Retro Film", "Retro", FilterCategory.COLOR_LUTS, Icons.Default.PhotoFilter, Color(0xFFE65100)),
    BW_DRAMA("B&W Drama", "B&W", FilterCategory.COLOR_LUTS, Icons.Default.DarkMode, Color(0xFF424242)),
    GOLDEN_HOUR("Golden Hour", "Golden", FilterCategory.COLOR_LUTS, Icons.Default.WbSunny, Color(0xFFFFB300)),
    COOL_TONE("Cool Tone", "Cool", FilterCategory.COLOR_LUTS, Icons.Default.AcUnit, Color(0xFF29B6F6)),
    FILM_NOIR("Film Noir", "Noir", FilterCategory.COLOR_LUTS, Icons.Default.NoFlash, Color(0xFF212121)),

    // AI FX
    AI_BEAUTY("AI Beauty", "Beauty", FilterCategory.AI_FX, Icons.Default.Face, Color(0xFFFF6B9D)),
    DEPTH_BOKEH("Depth Bokeh", "Bokeh", FilterCategory.AI_FX, Icons.Default.BlurOn, Color(0xFF6366F1)),
    FACE_MESH("Face Mesh", "Mesh", FilterCategory.AI_FX, Icons.Default.GridOn, Color(0xFFEC4899)),
    AUDIO_PULSE("Audio Pulse", "Pulse", FilterCategory.AI_FX, Icons.Default.GraphicEq, Color(0xFF00E5FF)),

    // Beauty
    SKIN_SMOOTH("Skin Smooth", "Smooth", FilterCategory.BEAUTY, Icons.Default.AutoFixHigh, Color(0xFFFF80AB)),
    EYE_BRIGHTEN("Eye Brighten", "Eyes", FilterCategory.BEAUTY, Icons.Default.RemoveRedEye, Color(0xFF4FC3F7)),
    FACE_RESHAPE("Face Reshape", "Reshape", FilterCategory.BEAUTY, Icons.Default.AccountCircle, Color(0xFFCE93D8)),
    TEETH_WHITEN("Teeth Whiten", "Teeth", FilterCategory.BEAUTY, Icons.Default.EmojiEmotions, Color(0xFFFFF9C4)),

    // Background
    BG_BLUR("BG Blur", "Blur", FilterCategory.BACKGROUND, Icons.Default.BlurOn, Color(0xFF7C4DFF)),
    BG_REPLACE("BG Replace", "Replace", FilterCategory.BACKGROUND, Icons.Default.Landscape, Color(0xFF26A69A)),
    NEON_OUTLINE("Neon Outline", "Neon", FilterCategory.BACKGROUND, Icons.Default.Brightness7, Color(0xFF00E676)),
    RIM_LIGHT("Rim Light", "Rim", FilterCategory.BACKGROUND, Icons.Default.Highlight, Color(0xFFFFD54F)),

    // AR Masks
    AR_MASK_3D("3D Mask", "3D", FilterCategory.AR_MASKS, Icons.Default.Theaters, Color(0xFFFF7043)),
    AR_GLOW("Glow FX", "Glow", FilterCategory.AR_MASKS, Icons.Default.AutoAwesome, Color(0xFFAB47BC)),
    AR_SPARKLE("Sparkle", "Sparkle", FilterCategory.AR_MASKS, Icons.Default.Star, Color(0xFFFFEB3B)),
    AR_FOG("Fog FX", "Fog", FilterCategory.AR_MASKS, Icons.Default.Cloud, Color(0xFF90A4AE)),

    // Audio FX
    BEAT_SYNC("Beat Sync", "Beat", FilterCategory.AUDIO_FX, Icons.Default.MusicNote, Color(0xFFE040FB)),
    BASS_DROP("Bass Drop", "Bass", FilterCategory.AUDIO_FX, Icons.Default.Equalizer, Color(0xFFFF5252)),
    VOCAL_ECHO("Vocal Echo", "Echo", FilterCategory.AUDIO_FX, Icons.Default.RecordVoiceOver, Color(0xFF448AFF)),
    RHYTHM_ZOOM("Rhythm Zoom", "Zoom", FilterCategory.AUDIO_FX, Icons.Default.ZoomOutMap, Color(0xFF69F0AE))
}

// ──────────────────────────────────────────────────────────────────────────────
// Utility: format milliseconds to MM:SS
// ──────────────────────────────────────────────────────────────────────────────
private fun formatTime(ms: Long): String {
    val totalSecs = ms / 1000
    val minutes = totalSecs / 60
    val seconds = totalSecs % 60
    return String.format("%02d:%02d", minutes, seconds)
}
