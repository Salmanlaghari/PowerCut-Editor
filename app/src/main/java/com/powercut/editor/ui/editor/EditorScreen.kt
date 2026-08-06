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
    // EditorScreen is deprecated — app uses NextGenEditorScreen.
    // This stub prevents MethodTooLarge compilation error.
    NextGenEditorScreen(
        project = project,
        language = language,
        onBack = onBack,
        onUpdateTrim = onUpdateTrim,
        onUpdateResolution = onUpdateResolution,
        onUpdateFilter = onUpdateFilter,
        onToggleMute = onToggleMute,
        onExport = onExport,
        onDurationRetrieved = onDurationRetrieved,
        onUpdateSpeed = onUpdateSpeed,
        onUpdateAspectPreset = onUpdateAspectPreset,
        onUpdateTransition = onUpdateTransition,
        onUpdateBackgroundMusic = onUpdateBackgroundMusic,
        onUpdateMusicVolume = onUpdateMusicVolume,
        onUpdateVideoVolume = onUpdateVideoVolume,
        onUpdateAutoCaptions = onUpdateAutoCaptions,
        onToggleSilenceRemover = onToggleSilenceRemover,
        onUpdateRotation = onUpdateRotation,
        onToggleFlipHorizontal = onToggleFlipHorizontal,
        onToggleFlipVertical = onToggleFlipVertical,
        onUpdateCropPreset = onUpdateCropPreset,
        onUpdateSpeedCurve = onUpdateSpeedCurve,
        onUpdateTextOverlay = onUpdateTextOverlay,
        onUpdateTextAnimation = onUpdateTextAnimation,
        onUpdateTextStyle = onUpdateTextStyle,
        onUpdateTextPositionX = onUpdateTextPositionX,
        onUpdateTextPositionY = onUpdateTextPositionY,
        onUpdateTextColor = onUpdateTextColor,
        onUpdateTextFontSize = onUpdateTextFontSize,
        onUpdateStickerType = onUpdateStickerType,
        onUpdateTemplate = onUpdateTemplate,
        onUpdateVisualizerStyle = onUpdateVisualizerStyle,
        onToggleBeatSync = onToggleBeatSync,
        onUpdate3DShapeMask = onUpdate3DShapeMask,
        onAddClip = onAddClip,
        onSaveDraft = onSaveDraft,
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
        onAiHub = onAiHub,
        onSocialPresets = onSocialPresets,
        onProTier = onProTier,
        onPremiumStudio = onPremiumStudio,
        onOpenEffects = onOpenEffects,
        onOpenStickers = onOpenStickers
    )
}
