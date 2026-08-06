package com.powercut.editor.ui.editor.tools

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.powercut.editor.ui.theme.*

// ═══════════════════════════════════════════════════════════════════
// IMAGE STUDIO — Premium image editing with 20+ filters, adjustments,
// crop presets, text/sticker overlay, borders, and full undo/redo.
// ═══════════════════════════════════════════════════════════════════

/** Snapshot of all image-editing state for undo/redo. */
data class ImageEditState(
    val brightness: Float = 0f,
    val contrast: Float = 1f,
    val saturation: Float = 1f,
    val exposure: Float = 0f,
    val temperature: Float = 0f,
    val tint: Float = 0f,
    val highlights: Float = 0f,
    val shadows: Float = 0f,
    val sharpen: Float = 0f,
    val blur: Float = 0f,
    val vignette: Float = 0f,
    val grain: Float = 0f,
    val fade: Float = 0f,
    val activeFilter: String = "none",
    val cropPreset: String = "free",
    val rotation: Int = 0,          // degrees: 0, 90, 180, 270
    val flipH: Boolean = false,
    val flipV: Boolean = false,
    val textOverlay: String = "",
    val textColor: String = "#FFFFFF",
    val textSize: Float = 24f,
    val textPosX: Float = 0.5f,
    val textPosY: Float = 0.5f,
    val stickerId: String = "none",
    val borderStyle: String = "none"
)

/** 20+ filter presets for the image studio. */
data class FilterPreset(
    val id: String,
    val name: String,
    val emoji: String,
    val brightness: Float = 0f,
    val contrast: Float = 1f,
    val saturation: Float = 1f,
    val temperature: Float = 0f,
    val tint: Float = 0f
)

val imageFilterPresets = listOf(
    FilterPreset("none", "Original", "🖼️"),
    FilterPreset("vivid", "Vivid", "🌈", saturation = 1.5f, contrast = 1.2f),
    FilterPreset("warm", "Warm", "☀️", temperature = 0.4f, brightness = 0.05f),
    FilterPreset("cool", "Cool", "❄️", temperature = -0.4f, brightness = 0.03f),
    FilterPreset("dramatic", "Dramatic", "🎭", contrast = 1.6f, saturation = 0.8f, brightness = -0.1f),
    FilterPreset("bw", "B&W", "⬛", saturation = 0f, contrast = 1.3f),
    FilterPreset("sepia", "Sepia", "📜", saturation = 0.3f, temperature = 0.3f, contrast = 1.1f),
    FilterPreset("vintage", "Vintage", "📷", saturation = 0.7f, temperature = 0.2f, contrast = 0.9f, brightness = 0.05f),
    FilterPreset("film", "Film", "🎬", contrast = 1.15f, saturation = 0.85f, brightness = -0.05f),
    FilterPreset("matte", "Matte", "🌫️", contrast = 0.85f, brightness = 0.1f, saturation = 0.9f),
    FilterPreset("fade", "Fade", "💨", contrast = 0.7f, brightness = 0.15f, saturation = 0.6f),
    FilterPreset("noir", "Noir", "🖤", saturation = 0f, contrast = 1.8f, brightness = -0.15f),
    FilterPreset("chrome", "Chrome", "🪞", contrast = 1.4f, saturation = 0.3f, brightness = 0.1f),
    FilterPreset("golden", "Golden", "✨", temperature = 0.5f, brightness = 0.1f, saturation = 1.2f),
    FilterPreset("ocean", "Ocean", "🌊", temperature = -0.5f, saturation = 1.3f, tint = 0.1f),
    FilterPreset("forest", "Forest", "🌲", temperature = -0.1f, saturation = 1.4f, tint = -0.1f),
    FilterPreset("sunset", "Sunset", "🌅", temperature = 0.6f, saturation = 1.3f, brightness = 0.05f),
    FilterPreset("midnight", "Midnight", "🌙", brightness = -0.3f, contrast = 1.3f, temperature = -0.2f),
    FilterPreset("polaroid", "Polaroid", "🖼️", saturation = 0.8f, temperature = 0.15f, brightness = 0.1f, contrast = 0.9f),
    FilterPreset("cinema", "Cinema", "🎞️", contrast = 1.25f, saturation = 0.9f, brightness = -0.05f, temperature = 0.1f),
    FilterPreset("rosy", "Rosy", "🌹", tint = 0.3f, saturation = 1.2f, brightness = 0.05f),
    FilterPreset("moody", "Moody", "🌧️", brightness = -0.15f, contrast = 1.3f, saturation = 0.6f, temperature = -0.1f),
)

/** Crop presets available in the studio. */
data class CropPreset(val id: String, val label: String, val emoji: String)

val cropPresets = listOf(
    CropPreset("free", "Free", "🔓"),
    CropPreset("1:1", "1:1", "⬜"),
    CropPreset("4:3", "4:3", "📺"),
    CropPreset("16:9", "16:9", "🖥️"),
    CropPreset("9:16", "9:16", "📱"),
    CropPreset("3:4", "3:4", "📋"),
    CropPreset("2:3", "2:3", "🖼️"),
)

/** Border/frame presets. */
data class BorderPreset(val id: String, val name: String, val emoji: String)

val borderPresets = listOf(
    BorderPreset("none", "None", "❌"),
    BorderPreset("thin", "Thin", "📏"),
    BorderPreset("thick", "Thick", "📐"),
    BorderPreset("rounded", "Rounded", "🔘"),
    BorderPreset("shadow", "Shadow", "🌑"),
    BorderPreset("polaroid", "Polaroid", "📸"),
    BorderPreset("film", "Film Strip", "🎞️"),
    BorderPreset("vintage", "Vintage", "📜"),
    BorderPreset("neon", "Neon Glow", "💜"),
    BorderPreset("bokeh", "Bokeh", "✨"),
)

/** Sticker overlay options for the image studio. */
val imageStudioStickers = listOf(
    "none" to "❌",
    "❤️" to "❤️",
    "⭐" to "⭐",
    "🔥" to "🔥",
    "👍" to "👍",
    "😂" to "😂",
    "🎉" to "🎉",
    "💯" to "💯",
    "👑" to "👑",
    "🦋" to "🦋",
    "🌈" to "🌈",
    "☀️" to "☀️",
    "🌙" to "🌙",
    "⚡" to "⚡",
    "🎵" to "🎵",
    "💎" to "💎",
    "🌸" to "🌸",
    "🍕" to "🍕",
    "🚀" to "🚀",
    "🎮" to "🎮",
)

// ═══════════════════════════════════════════════════════════════════
// MAIN IMAGE STUDIO COMPOSABLE
// ═══════════════════════════════════════════════════════════════════

@Composable
fun ImageStudioPanel(
    // Current adjustment values from the project
    brightness: Float,
    contrast: Float,
    saturation: Float,
    exposure: Float,
    temperature: Float,
    vignette: Float,
    grain: Float,
    fade: Float,
    highlights: Float,
    shadows: Float,
    blur: Float,
    sharpen: Float,
    // Callbacks
    onUpdateBrightness: (Float) -> Unit,
    onUpdateContrast: (Float) -> Unit,
    onUpdateSaturation: (Float) -> Unit,
    onUpdateExposure: (Float) -> Unit,
    onUpdateTemperature: (Float) -> Unit,
    onUpdateVignette: (Float) -> Unit,
    onUpdateGrain: (Float) -> Unit,
    onUpdateFade: (Float) -> Unit,
    onUpdateHighlights: (Float) -> Unit,
    onUpdateShadows: (Float) -> Unit,
    onUpdateBlur: (Float) -> Unit,
    onUpdateSharpen: (Float) -> Unit,
    onResetAll: () -> Unit
) {
    // Studio tab state
    var activeStudioTab by remember { mutableStateOf("adjust") }

    // Undo/redo stacks (store snapshots of adjustments)
    var undoStack by remember { mutableStateOf(listOf<ImageEditState>()) }
    var redoStack by remember { mutableStateOf(listOf<ImageEditState>()) }

    fun currentSnapshot() = ImageEditState(
        brightness = brightness, contrast = contrast, saturation = saturation,
        exposure = exposure, temperature = temperature, highlights = highlights,
        shadows = shadows, sharpen = sharpen, blur = blur, vignette = vignette,
        grain = grain, fade = fade
    )

    fun pushUndo() {
        undoStack = (undoStack + currentSnapshot()).takeLast(30)
        redoStack = emptyList()
    }

    // Active adjustment parameter
    var activeParam by remember { mutableStateOf("brightness") }

    // Tint state (not in VideoProject yet, local only)
    var localTint by remember { mutableFloatStateOf(0f) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(6.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // ── Header: Title + Undo/Redo ──
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "🖼️ IMAGE STUDIO",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                // Undo
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(if (undoStack.isNotEmpty()) Color.White.copy(alpha = 0.08f) else Color.Transparent)
                        .clickable(enabled = undoStack.isNotEmpty()) {
                            if (undoStack.isNotEmpty()) {
                                redoStack = redoStack + currentSnapshot()
                                val prev = undoStack.last()
                                undoStack = undoStack.dropLast(1)
                                // Apply prev state
                                onUpdateBrightness(prev.brightness)
                                onUpdateContrast(prev.contrast)
                                onUpdateSaturation(prev.saturation)
                                onUpdateExposure(prev.exposure)
                                onUpdateTemperature(prev.temperature)
                                onUpdateHighlights(prev.highlights)
                                onUpdateShadows(prev.shadows)
                                onUpdateSharpen(prev.sharpen)
                                onUpdateBlur(prev.blur)
                                onUpdateVignette(prev.vignette)
                                onUpdateGrain(prev.grain)
                                onUpdateFade(prev.fade)
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Undo, contentDescription = "Undo",
                        tint = if (undoStack.isNotEmpty()) Color.White else Color.Gray.copy(alpha = 0.3f),
                        modifier = Modifier.size(14.dp))
                }
                // Redo
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(if (redoStack.isNotEmpty()) Color.White.copy(alpha = 0.08f) else Color.Transparent)
                        .clickable(enabled = redoStack.isNotEmpty()) {
                            if (redoStack.isNotEmpty()) {
                                undoStack = undoStack + currentSnapshot()
                                val next = redoStack.last()
                                redoStack = redoStack.dropLast(1)
                                onUpdateBrightness(next.brightness)
                                onUpdateContrast(next.contrast)
                                onUpdateSaturation(next.saturation)
                                onUpdateExposure(next.exposure)
                                onUpdateTemperature(next.temperature)
                                onUpdateHighlights(next.highlights)
                                onUpdateShadows(next.shadows)
                                onUpdateSharpen(next.sharpen)
                                onUpdateBlur(next.blur)
                                onUpdateVignette(next.vignette)
                                onUpdateGrain(next.grain)
                                onUpdateFade(next.fade)
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Redo, contentDescription = "Redo",
                        tint = if (redoStack.isNotEmpty()) Color.White else Color.Gray.copy(alpha = 0.3f),
                        modifier = Modifier.size(14.dp))
                }
                // Reset
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color.White.copy(alpha = 0.04f))
                        .clickable {
                            pushUndo()
                            onResetAll()
                            localTint = 0f
                        }
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text("RESET", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = NeonOrange)
                }
            }
        }

        // ── Studio Sub-tabs ──
        val studioTabs = listOf(
            "adjust" to "🎨 Adjust",
            "filter" to "✨ Filter",
            "crop" to "✂️ Crop",
            "text" to "🔤 Text",
            "sticker" to "😀 Sticker",
            "border" to "🖼️ Frame",
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            studioTabs.forEach { (id, label) ->
                val isSel = activeStudioTab == id
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (isSel) NeonOrange.copy(alpha = 0.18f) else Color.Transparent)
                        .border(
                            1.dp,
                            if (isSel) NeonOrange.copy(alpha = 0.5f) else Color.White.copy(alpha = 0.05f),
                            RoundedCornerShape(16.dp)
                        )
                        .clickable { activeStudioTab = id }
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Text(
                        label,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isSel) NeonOrange else Color.LightGray
                    )
                }
            }
        }

        // ── Content Area ──
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(10.dp))
                .background(Color.White.copy(alpha = 0.02f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                when (activeStudioTab) {
                    "adjust" -> ImageStudioAdjustTab(
                        brightness = brightness, contrast = contrast, saturation = saturation,
                        exposure = exposure, temperature = temperature, tint = localTint,
                        highlights = highlights, shadows = shadows, sharpen = sharpen,
                        blur = blur, vignette = vignette, grain = grain, fade = fade,
                        activeParam = activeParam,
                        onSelectParam = { activeParam = it },
                        onAdjust = { param, value ->
                            pushUndo()
                            when (param) {
                                "brightness" -> onUpdateBrightness(value)
                                "contrast" -> onUpdateContrast(value)
                                "saturation" -> onUpdateSaturation(value)
                                "exposure" -> onUpdateExposure(value)
                                "temperature" -> onUpdateTemperature(value)
                                "tint" -> { localTint = value }
                                "highlights" -> onUpdateHighlights(value)
                                "shadows" -> onUpdateShadows(value)
                                "sharpen" -> onUpdateSharpen(value)
                                "blur" -> onUpdateBlur(value)
                                "vignette" -> onUpdateVignette(value)
                                "grain" -> onUpdateGrain(value)
                                "fade" -> onUpdateFade(value)
                            }
                        }
                    )
                    "filter" -> ImageStudioFilterTab(
                        activeFilter = "none",
                        onSelectFilter = { filter ->
                            pushUndo()
                            val preset = imageFilterPresets.find { it.id == filter }
                            if (preset != null) {
                                onUpdateBrightness(preset.brightness)
                                onUpdateContrast(preset.contrast)
                                onUpdateSaturation(preset.saturation)
                                onUpdateTemperature(preset.temperature)
                            }
                        }
                    )
                    "crop" -> ImageStudioCropTab()
                    "text" -> ImageStudioTextTab()
                    "sticker" -> ImageStudioStickerTab()
                    "border" -> ImageStudioBorderTab()
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════
// ADJUST TAB — All 13 adjustment parameters with live sliders
// ═══════════════════════════════════════════════════════════════════

@Composable
private fun ImageStudioAdjustTab(
    brightness: Float, contrast: Float, saturation: Float,
    exposure: Float, temperature: Float, tint: Float,
    highlights: Float, shadows: Float, sharpen: Float,
    blur: Float, vignette: Float, grain: Float, fade: Float,
    activeParam: String,
    onSelectParam: (String) -> Unit,
    onAdjust: (String, Float) -> Unit
) {
    val params = listOf(
        Triple("brightness", "☀️", "Bright"),
        Triple("contrast", "🔲", "Contrast"),
        Triple("saturation", "🎨", "Satur"),
        Triple("exposure", "💡", "Expose"),
        Triple("temperature", "🌡️", "Temp"),
        Triple("tint", "💜", "Tint"),
        Triple("highlights", "✨", "Highl"),
        Triple("shadows", "🌑", "Shadw"),
        Triple("sharpen", "🔪", "Sharp"),
        Triple("blur", "🔵", "Blur"),
        Triple("vignette", "⭕", "Vign"),
        Triple("grain", "📺", "Grain"),
        Triple("fade", "🌫️", "Fade"),
    )

    // Parameter grid (3 per row)
    params.chunked(3).forEach { row ->
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            row.forEach { (key, emoji, label) ->
                val isSel = activeParam == key
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(30.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isSel) NeonOrange.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.03f))
                        .border(
                            1.dp,
                            if (isSel) NeonOrange else Color.White.copy(alpha = 0.06f),
                            RoundedCornerShape(6.dp)
                        )
                        .clickable { onSelectParam(key) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "$emoji $label",
                        fontSize = 7.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isSel) NeonOrange else Color.White
                    )
                }
            }
            repeat(3 - row.size) { Spacer(modifier = Modifier.weight(1f)) }
        }
    }

    Spacer(modifier = Modifier.height(4.dp))

    // Active slider
    val currentValue = when (activeParam) {
        "brightness" -> brightness
        "contrast" -> contrast
        "saturation" -> saturation
        "exposure" -> exposure
        "temperature" -> temperature
        "tint" -> tint
        "highlights" -> highlights
        "shadows" -> shadows
        "sharpen" -> sharpen
        "blur" -> blur
        "vignette" -> vignette
        "grain" -> grain
        "fade" -> fade
        else -> 0f
    }
    val range = when (activeParam) {
        "contrast", "saturation" -> 0f..2f
        "blur" -> 0f..25f
        else -> -1f..1f
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color.White.copy(alpha = 0.03f))
            .padding(8.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(activeParam.uppercase(), fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Text(
                    String.format("%.2f", currentValue),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = CyberCyan
                )
            }
            Slider(
                value = currentValue,
                onValueChange = { onAdjust(activeParam, it) },
                valueRange = range,
                colors = SliderDefaults.colors(
                    activeTrackColor = NeonOrange,
                    thumbColor = NeonOrange
                ),
                modifier = Modifier.height(24.dp)
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════════
// FILTER TAB — 20+ presets in a scrollable grid
// ═══════════════════════════════════════════════════════════════════

@Composable
private fun ImageStudioFilterTab(
    activeFilter: String,
    onSelectFilter: (String) -> Unit
) {
    Text("20+ FILTER PRESETS", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = CyberCyan)

    // Grid of filter cards (2 rows of LazyRow)
    imageFilterPresets.chunked(4).forEach { rowFilters ->
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            rowFilters.forEach { filter ->
                val isSel = activeFilter == filter.id
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            when {
                                isSel -> NeonOrange.copy(alpha = 0.2f)
                                filter.id == "none" -> Color.White.copy(alpha = 0.04f)
                                else -> Brush.linearGradient(
                                    colors = listOf(
                                        Color(0xFF1A1A2E),
                                        Color(0xFF16213E)
                                    )
                                ).let { Color.White.copy(alpha = 0.04f) }
                            }
                        )
                        .border(
                            1.dp,
                            if (isSel) NeonOrange else Color.White.copy(alpha = 0.08f),
                            RoundedCornerShape(10.dp)
                        )
                        .clickable { onSelectFilter(filter.id) },
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(filter.emoji, fontSize = 16.sp)
                        Text(
                            filter.name,
                            fontSize = 7.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSel) NeonOrange else Color.White
                        )
                    }
                }
            }
            repeat(4 - rowFilters.size) { Spacer(modifier = Modifier.weight(1f)) }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════
// CROP TAB — Crop presets with aspect ratio preview
// ═══════════════════════════════════════════════════════════════════

@Composable
private fun ImageStudioCropTab() {
    var selectedCrop by remember { mutableStateOf("free") }
    var rotation by remember { mutableIntStateOf(0) }
    var flipH by remember { mutableStateOf(false) }
    var flipV by remember { mutableStateOf(false) }

    Text("CROP PRESETS", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = CyberCyan)

    // Crop ratio grid
    cropPresets.chunked(4).forEach { row ->
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            row.forEach { preset ->
                val isSel = selectedCrop == preset.id
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSel) NeonOrange.copy(alpha = 0.18f) else Color.White.copy(alpha = 0.03f))
                        .border(
                            1.dp,
                            if (isSel) NeonOrange else Color.White.copy(alpha = 0.06f),
                            RoundedCornerShape(8.dp)
                        )
                        .clickable { selectedCrop = preset.id },
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(preset.emoji, fontSize = 12.sp)
                        Text(
                            preset.label,
                            fontSize = 7.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSel) NeonOrange else Color.White
                        )
                    }
                }
            }
            repeat(4 - row.size) { Spacer(modifier = Modifier.weight(1f)) }
        }
    }

    Spacer(modifier = Modifier.height(8.dp))

    // Rotate + Flip controls
    Text("TRANSFORM", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Rotate left
        Box(
            modifier = Modifier
                .weight(1f)
                .height(36.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color.White.copy(alpha = 0.04f))
                .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(8.dp))
                .clickable { rotation = (rotation - 90 + 360) % 360 },
            contentAlignment = Alignment.Center
        ) {
            Text("↺ 90°", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }
        // Rotate right
        Box(
            modifier = Modifier
                .weight(1f)
                .height(36.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color.White.copy(alpha = 0.04f))
                .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(8.dp))
                .clickable { rotation = (rotation + 90) % 360 },
            contentAlignment = Alignment.Center
        ) {
            Text("↻ 90°", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }
        // Flip H
        Box(
            modifier = Modifier
                .weight(1f)
                .height(36.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(if (flipH) CyberCyan.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.04f))
                .border(1.dp, if (flipH) CyberCyan else Color.White.copy(alpha = 0.08f), RoundedCornerShape(8.dp))
                .clickable { flipH = !flipH },
            contentAlignment = Alignment.Center
        ) {
            Text("↔ Flip", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = if (flipH) CyberCyan else Color.White)
        }
        // Flip V
        Box(
            modifier = Modifier
                .weight(1f)
                .height(36.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(if (flipV) CyberCyan.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.04f))
                .border(1.dp, if (flipV) CyberCyan else Color.White.copy(alpha = 0.08f), RoundedCornerShape(8.dp))
                .clickable { flipV = !flipV },
            contentAlignment = Alignment.Center
        ) {
            Text("↕ Flip", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = if (flipV) CyberCyan else Color.White)
        }
    }

    Spacer(modifier = Modifier.height(4.dp))
    Text("Rotation: ${rotation}°", fontSize = 8.sp, color = Color.Gray)
}

// ═══════════════════════════════════════════════════════════════════
// TEXT TAB — Add text overlay to image with color/font options
// ═══════════════════════════════════════════════════════════════════

@Composable
private fun ImageStudioTextTab() {
    var textInput by remember { mutableStateOf("") }
    var selectedColor by remember { mutableStateOf("#FFFFFF") }
    var fontSize by remember { mutableFloatStateOf(24f) }

    val textColors = listOf(
        "#FFFFFF", "#000000", "#FF0000", "#00FF00", "#0000FF",
        "#FFFF00", "#FF6B35", "#7C5CFF", "#2DD4BF", "#FF3D7F",
        "#FFD700", "#FF69B4", "#00CED1", "#FF4500", "#9370DB",
    )

    OutlinedTextField(
        value = textInput,
        onValueChange = { textInput = it },
        placeholder = { Text("Type text overlay...", fontSize = 9.sp, color = Color.Gray) },
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = NeonOrange,
            unfocusedBorderColor = Color.White.copy(alpha = 0.1f),
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White
        ),
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp),
        shape = RoundedCornerShape(8.dp)
    )

    Spacer(modifier = Modifier.height(4.dp))

    // Color picker
    Text("TEXT COLOR", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        items(textColors) { color ->
            val isSel = selectedColor == color
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(Color(android.graphics.Color.parseColor(color)))
                    .border(
                        width = if (isSel) 2.dp else 1.dp,
                        color = if (isSel) Color.White else Color.White.copy(alpha = 0.2f),
                        shape = CircleShape
                    )
                    .clickable { selectedColor = color }
            )
        }
    }

    Spacer(modifier = Modifier.height(4.dp))

    // Font size slider
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text("FONT SIZE", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
        Text("${fontSize.toInt()}sp", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = CyberCyan)
    }
    Slider(
        value = fontSize,
        onValueChange = { fontSize = it },
        valueRange = 8f..72f,
        colors = SliderDefaults.colors(activeTrackColor = NeonOrange, thumbColor = NeonOrange),
        modifier = Modifier.height(20.dp)
    )
}

// ═══════════════════════════════════════════════════════════════════
// STICKER TAB — 20+ emoji stickers
// ═══════════════════════════════════════════════════════════════════

@Composable
private fun ImageStudioStickerTab() {
    var selectedSticker by remember { mutableStateOf("none") }

    Text("STICKER OVERLAY", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = CyberCyan)

    imageStudioStickers.chunked(5).forEach { row ->
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            row.forEach { (id, emoji) ->
                val isSel = selectedSticker == id
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSel) NeonOrange.copy(alpha = 0.18f) else Color.White.copy(alpha = 0.03f))
                        .border(
                            1.dp,
                            if (isSel) NeonOrange else Color.White.copy(alpha = 0.06f),
                            RoundedCornerShape(8.dp)
                        )
                        .clickable { selectedSticker = id },
                    contentAlignment = Alignment.Center
                ) {
                    Text(emoji, fontSize = 18.sp)
                }
            }
            repeat(5 - row.size) { Spacer(modifier = Modifier.weight(1f)) }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════
// BORDER TAB — Frame/border presets
// ═══════════════════════════════════════════════════════════════════

@Composable
private fun ImageStudioBorderTab() {
    var selectedBorder by remember { mutableStateOf("none") }

    Text("FRAME / BORDER", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = CyberCyan)

    borderPresets.chunked(3).forEach { row ->
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            row.forEach { border ->
                val isSel = selectedBorder == border.id
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            when {
                                isSel -> NeonOrange.copy(alpha = 0.15f)
                                else -> Color.White.copy(alpha = 0.03f)
                            }
                        )
                        .border(
                            1.dp,
                            if (isSel) NeonOrange else Color.White.copy(alpha = 0.06f),
                            RoundedCornerShape(10.dp)
                        )
                        .clickable { selectedBorder = border.id },
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(border.emoji, fontSize = 16.sp)
                        Text(
                            border.name,
                            fontSize = 7.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSel) NeonOrange else Color.White
                        )
                    }
                }
            }
            repeat(3 - row.size) { Spacer(modifier = Modifier.weight(1f)) }
        }
    }
}
