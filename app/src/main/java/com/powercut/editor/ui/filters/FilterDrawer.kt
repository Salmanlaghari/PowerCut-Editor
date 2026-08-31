package com.powercut.editor.ui.filters

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.powercut.editor.domain.ai.ColorGradingPreset

/**
 * Bottom filter drawer UI matching the reference design:
 * - Top 70%: Video preview canvas
 * - Middle bar: Active filter intensity slider
 * - Bottom 30%: Horizontally scrollable filter carousel with category tabs
 */
@Composable
fun FilterDrawer(
    activeFilter: FilterPreset?,
    onFilterSelected: (FilterPreset) -> Unit,
    onIntensityChanged: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedCategory by remember { mutableStateOf(FilterCategory.AI_FX) }
    var isDrawerExpanded by remember { mutableStateOf(true) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color.Transparent, Color(0xCC0A0A0F), Color(0xF00A0A0F))
                )
            )
    ) {
        // Active filter intensity slider (middle bar)
        AnimatedVisibility(
            visible = activeFilter != null,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
        ) {
            activeFilter?.let { filter ->
                FilterIntensitySlider(
                    filterName = filter.displayName,
                    onIntensityChanged = onIntensityChanged
                )
            }
        }

        // Category tabs
        FilterCategoryTabs(
            selectedCategory = selectedCategory,
            onCategorySelected = { selectedCategory = it }
        )

        // Filter carousel
        FilterCarousel(
            category = selectedCategory,
            activeFilter = activeFilter,
            onFilterSelected = onFilterSelected,
            modifier = Modifier.height(140.dp)
        )

        // Spacer for safe area
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun FilterIntensitySlider(
    filterName: String,
    onIntensityChanged: (Float) -> Unit
) {
    var intensity by remember { mutableFloatStateOf(0.75f) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = filterName,
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = "${(intensity * 100).toInt()}%",
                color = Color(0xFF00E5FF),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Slider(
            value = intensity,
            onValueChange = {
                intensity = it
                onIntensityChanged(it)
            },
            valueRange = 0f..1f,
            colors = SliderDefaults.colors(
                thumbColor = Color(0xFF00E5FF),
                activeTrackColor = Color(0xFF00E5FF),
                inactiveTrackColor = Color(0xFF2A2A3A)
            ),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun FilterCategoryTabs(
    selectedCategory: FilterCategory,
    onCategorySelected: (FilterCategory) -> Unit
) {
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(FilterCategory.entries) { category ->
            FilterCategoryChip(
                category = category,
                isSelected = category == selectedCategory,
                onClick = { onCategorySelected(category) }
            )
        }
    }
}

@Composable
private fun FilterCategoryChip(
    category: FilterCategory,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (isSelected) Color(0xFF00E5FF) else Color(0xFF1A1A2E),
        label = "chipBg"
    )
    val textColor by animateColorAsState(
        targetValue = if (isSelected) Color.Black else Color(0xFF8888AA),
        label = "chipText"
    )

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = backgroundColor,
        modifier = Modifier
            .shadow(
                elevation = if (isSelected) 8.dp else 0.dp,
                shape = RoundedCornerShape(20.dp),
                ambientColor = Color(0xFF00E5FF).copy(alpha = 0.3f)
            )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = category.icon,
                contentDescription = null,
                tint = textColor,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = category.displayName,
                color = textColor,
                fontSize = 12.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
            )
        }
    }
}

@Composable
private fun FilterCarousel(
    category: FilterCategory,
    activeFilter: FilterPreset?,
    onFilterSelected: (FilterPreset) -> Unit,
    modifier: Modifier = Modifier
) {
    val filters = FilterPreset.entries.filter { it.category == category }

    LazyRow(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(filters) { filter ->
            FilterThumbnailCard(
                filter = filter,
                isActive = filter == activeFilter,
                onClick = { onFilterSelected(filter) }
            )
        }
    }
}

@Composable
private fun FilterThumbnailCard(
    filter: FilterPreset,
    isActive: Boolean,
    onClick: () -> Unit
) {
    val borderColor by animateColorAsState(
        targetValue = if (isActive) Color(0xFF00E5FF) else Color.Transparent,
        label = "border"
    )
    val glowAlpha by animateFloatAsState(
        targetValue = if (isActive) 0.6f else 0f,
        label = "glow"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(80.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        filter.previewColor.copy(alpha = 0.4f),
                        filter.previewColor.copy(alpha = 0.15f)
                    )
                )
            )
            .border(
                width = 2.dp,
                color = borderColor,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onClick)
            .padding(8.dp)
    ) {
        // Filter preview icon
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(filter.previewColor.copy(alpha = 0.3f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = filter.icon,
                contentDescription = filter.displayName,
                tint = filter.previewColor,
                modifier = Modifier.size(28.dp)
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Filter name
        Text(
            text = filter.shortName,
            color = if (isActive) Color(0xFF00E5FF) else Color(0xFFAAAACC),
            fontSize = 10.sp,
            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
            textAlign = TextAlign.Center,
            maxLines = 1
        )

        // Active badge
        if (isActive) {
            Spacer(modifier = Modifier.height(2.dp))
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(Color(0xFF00E5FF))
            )
        }
    }
}

/**
 * Filter categories for the bottom drawer tabs.
 */
enum class FilterCategory(val displayName: String, val icon: ImageVector) {
    AI_FX("AI FX", Icons.Default.AutoAwesome),
    COLOR_LUTS("Color LUTs", Icons.Default.Palette),
    BEAUTY("Beauty", Icons.Default.Face),
    BACKGROUND("Background", Icons.Default.Landscape),
    AR_MASKS("AR Masks", Icons.Default.Theaters),
    AUDIO_FX("Audio FX", Icons.Default.GraphicEq)
}

/**
 * Filter presets organized by category.
 */
enum class FilterPreset(
    val displayName: String,
    val shortName: String,
    val category: FilterCategory,
    val icon: ImageVector,
    val previewColor: Color
) {
    // AI FX
    AI_BEAUTY("AI Beauty", "Beauty", FilterCategory.AI_FX, Icons.Default.Face, Color(0xFFFF6B9D)),
    CYBER_NEON("Cyber Neon", "Neon", FilterCategory.AI_FX, Icons.Default.Bolt, Color(0xFF00FF88)),
    DEPTH_BOKEH("Depth Bokeh", "Bokeh", FilterCategory.AI_FX, Icons.Default.BlurOn, Color(0xFF6366F1)),
    FACE_MESH("Face Mesh", "Mesh", FilterCategory.AI_FX, Icons.Default.GridOn, Color(0xFFEC4899)),
    AUDIO_PULSE("Audio Pulse", "Pulse", FilterCategory.AI_FX, Icons.Default.GraphicEq, Color(0xFF00E5FF)),

    // Color LUTs
    TEAL_ORANGE("Teal & Orange", "T&O", FilterCategory.COLOR_LUTS, Icons.Default.Tonality, Color(0xFF00897B)),
    VINTAGE_FILM("Vintage Film", "Vintage", FilterCategory.COLOR_LUTS, Icons.Default.Film, Color(0xFFD4A574)),
    CYBERPUNK_LUT("Cyberpunk", "Cyber", FilterCategory.COLOR_LUTS, Icons.Default.Nightlife, Color(0xFFFF00FF)),
    FILM_NOIR("Film Noir", "Noir", FilterCategory.COLOR_LUTS, Icons.Default.DarkMode, Color(0xFF424242)),
    GOLDEN_HOUR("Golden Hour", "Golden", FilterCategory.COLOR_LUTS, Icons.Default.WbSunny, Color(0xFFFFB300)),
    RETRO_FILM("Retro Film", "Retro", FilterCategory.COLOR_LUTS, Icons.Default.CameraAlt, Color(0xFFE65100)),
    COOL_TONE("Cool Tone", "Cool", FilterCategory.COLOR_LUTS, Icons.Default.AcUnit, Color(0xFF29B6F6)),
    DRAMATIC("Dramatic", "Dramatic", FilterCategory.COLOR_LUTS, Icons.Default.TheaterComedy, Color(0xFFB71C1C)),

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
