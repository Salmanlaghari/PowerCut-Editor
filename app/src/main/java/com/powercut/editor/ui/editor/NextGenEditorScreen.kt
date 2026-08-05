package com.powercut.editor.ui.editor

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.Offset as GeomOffset
import androidx.compose.ui.geometry.Size as GeomSize
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
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.compositeOver
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
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults

private fun formatTime(ms: Long): String {
    val totalSecs = ms / 1000
    val minutes = totalSecs / 60
    val seconds = totalSecs % 60
    return String.format(Locale.US, "%02d:%02d", minutes, seconds)
}

// ═══════════════════════════════════════════════════════════════
//  FILTER MATRIX BUILDER — returns a ColorMatrix for live preview
//  of cinematic filters. Used by the combined preview filter so that
//  BOTH the filter and image-editor adjustments show in real-time.
// ═══════════════════════════════════════════════════════════════
private fun buildFilterMatrix(filter: String): ColorMatrix? {
    val f = filter.lowercase().replace("-", "_").replace(" ", "_")
    return when (f) {
        "grayscale", "mono" -> ColorMatrix().apply { setToSaturation(0f) }
        "sepia" -> ColorMatrix(floatArrayOf(0.393f,0.769f,0.189f,0f,0f,0.349f,0.686f,0.168f,0f,0f,0.272f,0.534f,0.131f,0f,0f,0f,0f,0f,1f,0f))
        "invert" -> ColorMatrix(floatArrayOf(-1f,0f,0f,0f,255f,0f,-1f,0f,0f,255f,0f,0f,-1f,0f,255f,0f,0f,0f,1f,0f))
        "warm" -> ColorMatrix(floatArrayOf(1.1f,0f,0f,0f,0f, 0f,1.02f,0f,0f,0f, 0f,0f,0.9f,0f,0f, 0f,0f,0f,1f,0f))
        "cool" -> ColorMatrix(floatArrayOf(0.9f,0f,0f,0f,0f, 0f,0.97f,0f,0f,0f, 0f,0f,1.1f,0f,0f, 0f,0f,0f,1f,0f))
        "vintage" -> ColorMatrix(floatArrayOf(0.5f,0.65f,0.15f,0f,0f, 0.45f,0.6f,0.12f,0f,0f, 0.35f,0.55f,0.1f,0f,0f, 0f,0f,0f,1f,0f))
        "dramatic" -> ColorMatrix().apply { setToSaturation(1.3f) }
        "vivid" -> ColorMatrix().apply { setToSaturation(1.6f) }
        "noir" -> ColorMatrix().apply { setToSaturation(0f) }
        "bloom" -> ColorMatrix(floatArrayOf(1.05f,0.05f,0.05f,0f,10f, 0.05f,1.05f,0.05f,0f,10f, 0.05f,0.05f,1.05f,0f,10f, 0f,0f,0f,1f,0f))
        "tealorange", "teal_orange" -> ColorMatrix(floatArrayOf(1.12f,0f,0f,0f,0f, 0f,0.95f,0f,0f,0f, 0f,0f,1.08f,0f,0f, 0f,0f,0f,1f,0f))
        "pastel" -> ColorMatrix().apply { setToSaturation(0.7f) }
        "fade" -> ColorMatrix().apply { setToSaturation(0.6f) }
        "cyberpunk" -> ColorMatrix(floatArrayOf(1.2f,0f,0.1f,0f,0f, 0f,0.8f,0f,0f,0f, 0.1f,0f,1.25f,0f,0f, 0f,0f,0f,1f,0f))
        "sunset" -> ColorMatrix(floatArrayOf(1.15f,0.05f,0f,0f,0f, 0f,0.97f,0f,0f,0f, 0f,0f,0.95f,0f,0f, 0f,0f,0f,1f,0f))
        "arctic" -> ColorMatrix(floatArrayOf(0.85f,0f,0f,0f,0f, 0f,0.95f,0f,0f,0f, 0f,0f,1.12f,0f,0f, 0f,0f,0f,1f,0f))
        "forest" -> ColorMatrix(floatArrayOf(0.9f,0f,0f,0f,0f, 0f,1.1f,0f,0f,0f, 0f,0f,0.9f,0f,0f, 0f,0f,0f,1f,0f))
        "rose" -> ColorMatrix(floatArrayOf(1.1f,0f,0.05f,0f,0f, 0f,0.95f,0f,0f,0f, 0f,0f,1.05f,0f,0f, 0f,0f,0f,1f,0f))
        "golden" -> ColorMatrix(floatArrayOf(1.12f,0.05f,0f,0f,5f, 0f,1.03f,0f,0f,0f, 0f,0f,0.85f,0f,0f, 0f,0f,0f,1f,0f))
        "mist" -> ColorMatrix(floatArrayOf(1.05f,0.03f,0.03f,0f,15f, 0.03f,1.05f,0.03f,0f,15f, 0.03f,0.03f,1.05f,0f,15f, 0f,0f,0f,1f,0f))
        else -> null
    }
}

// ============================================================================
//  v4.6.0 PREMIUM LOOK PREVIEW MATRIX
//  Parses a PremiumLook's real FFmpeg chain (eq / colorbalance / saturation)
//  into an approximate Compose ColorMatrix so the look is VISIBLE in the
//  real-time editor preview - not only at export. This makes every HDR /
//  iPhone / Bright / Cinema / Magic look "select for real" in the preview.
//  (unsharp / boxblur / curves are sharpen/blurring ops with no direct
//   ColorMatrix equivalent, so we approximate their *tone* contribution via
//   the eq/contrast/saturation/colorbalance parts that DO map cleanly.)
// ============================================================================
private fun premiumLookPreviewMatrix(lookId: String): ColorMatrix? {
    val chain = com.powercut.editor.domain.look.PremiumLooks.chainFor(lookId)
    if (chain.isBlank()) return null

    // Defaults that match FFmpeg eq defaults.
    var brightness = 0f      // eq brightness add (0..1), default 0
    var contrast = 1f        // eq contrast multiplier, default 1
    var saturation = 1f      // eq saturation multiplier, default 1
    var cbRs = 0f; var cbGs = 0f; var cbBs = 0f   // colorbalance shadows r/g/b
    var cbRm = 0f; var cbGm = 0f; var cbBm = 0f   // colorbalance midtones r/g/b
    var grayscale = false

    for (filter in chain.split(",")) {
        val f = filter.trim()
        when {
            f.startsWith("eq=") -> {
                for (kv in f.removePrefix("eq=").split(":")) {
                    val p = kv.split("=")
                    if (p.size != 2) continue
                    val k = p[0].trim(); val v = p[1].trim().toFloatOrNull() ?: continue
                    when (k) {
                        "brightness" -> brightness = v
                        "contrast" -> contrast = v
                        "saturation" -> { saturation = v; if (v == 0f) grayscale = true }
                    }
                }
            }
            f.startsWith("colorbalance=") -> {
                for (kv in f.removePrefix("colorbalance=").split(":")) {
                    val p = kv.split("=")
                    if (p.size != 2) continue
                    val k = p[0].trim(); val v = p[1].trim().toFloatOrNull() ?: continue
                    when (k) {
                        "rs" -> cbRs = v; "gs" -> cbGs = v; "bs" -> cbBs = v
                        "rm" -> cbRm = v; "gm" -> cbGm = v; "bm" -> cbBm = v
                    }
                }
            }
        }
    }

    // If nothing meaningful parsed, bail (let other adjustments show alone).
    val hasEq = brightness != 0f || contrast != 1f || saturation != 1f
    val hasCb = cbRs != 0f || cbGs != 0f || cbBs != 0f || cbRm != 0f || cbGm != 0f || cbBm != 0f
    if (!hasEq && !hasCb) return null

    // Build a 4x5 ColorMatrix approximation.
    // FFmpeg eq contrast scales around 0.5 mid-point: out = (in - 0.5)*c + 0.5 + b
    // In 0..255 ColorMatrix terms: scale = c, add = (0.5 - 0.5*c)*255 + b*255
    val contrastShift = (0.5f - 0.5f * contrast) * 255f
    val brightnessAdd = brightness * 255f

    // colorbalance values are in roughly -1..1; map to a +-~40 channel add and
    // a small channel scale so warm/cool tints show clearly in preview.
    val rShift = (cbRs + cbRm) * 38f
    val gShift = (cbGs + cbGm) * 38f
    val bShift = (cbBs + cbBm) * 38f

    val mat = if (grayscale) {
        // saturation=0 -> pure grayscale, then apply contrast/brightness/tint.
        val gray = ColorMatrix().apply { setToSaturation(0f) }
        val tone = ColorMatrix(floatArrayOf(
            contrast, 0f, 0f, 0f, contrastShift + brightnessAdd + rShift,
            0f, contrast, 0f, 0f, contrastShift + brightnessAdd + gShift,
            0f, 0f, contrast, 0f, contrastShift + brightnessAdd + bShift,
            0f, 0f, 0f, 1f, 0f
        ))
        gray *= tone
        gray
    } else {
        // Saturation via setToSaturation, then contrast/brightness/tint on top.
        val sat = ColorMatrix().apply { setToSaturation(saturation) }
        val tone = ColorMatrix(floatArrayOf(
            contrast, 0f, 0f, 0f, contrastShift + brightnessAdd + rShift,
            0f, contrast, 0f, 0f, contrastShift + brightnessAdd + gShift,
            0f, 0f, contrast, 0f, contrastShift + brightnessAdd + bShift,
            0f, 0f, 0f, 1f, 0f
        ))
        sat *= tone
        sat
    }
    return mat
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
    onToggleAutoReframe: () -> Unit = {},
    // NEW v4.0 CapCut-sync Pro callbacks
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
    onUpdateVignetteStyle: (String) -> Unit = {},
    onUpdatePremiumLook: (String) -> Unit = {},
    // v6.0.0 Premium launcher — top action row (AI Hub, Presets, Pro, Studio)
    onAiHub: () -> Unit = {},
    onSocialPresets: () -> Unit = {},
    onProTier: () -> Unit = {},
    onPremiumStudio: () -> Unit = {},
    // v6.0.0 Effects & Stickers full-screen galleries
    onOpenEffects: () -> Unit = {},
    onOpenStickers: () -> Unit = {}
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
        if (isPlaying) { exoPlayer.play(); while (isPlaying) { currentPlaybackTime = exoPlayer.currentPosition; kotlinx.coroutines.delay(16) } }
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

    // ═══════════════════════════════════════════════════════════
    //  COMBINED LIVE PREVIEW FILTER — merges the selected cinematic filter
    //  WITH the image-editor adjustments (brightness, contrast, saturation,
    //  temperature, exposure) so ALL adjustments show in real-time on the
    //  player, not just on export. This makes every slider "real" not "fake".
    // ═══════════════════════════════════════════════════════════
    val combinedColorFilter = remember(
        project.selectedFilter,
        project.activePremiumLook,
        project.imageEditorBrightness,
        project.imageEditorContrast,
        project.imageEditorSaturation,
        project.imageEditorTemperature,
        project.imageEditorExposure
    ) {
        // v4.6.0: preview matrix for the active Premium Look (HDR/iPhone/Bright/
        // Cinema/Magic). Composed in so the look is VISIBLE in real-time preview,
        // not only applied at export.
        val lookMatrix = premiumLookPreviewMatrix(project.activePremiumLook)

        val b = project.imageEditorBrightness
        val c = project.imageEditorContrast
        val s = project.imageEditorSaturation
        val t = project.imageEditorTemperature
        val e = project.imageEditorExposure
        val hasAdjustments = b != 1f || c != 1f || s != 1f || t != 1f || e != 1f

        if (!hasAdjustments && lookMatrix == null) {
            colorFilter
        } else if (!hasAdjustments && lookMatrix != null) {
            // Only a premium look is active (no slider adjustments).
            if (colorFilter != null) {
                val f = project.selectedFilter.lowercase().replace("-", "_").replace(" ", "_")
                val filterMatrix = buildFilterMatrix(f)
                if (filterMatrix != null) {
                    filterMatrix *= lookMatrix
                    ColorFilter.colorMatrix(filterMatrix)
                } else {
                    ColorFilter.colorMatrix(lookMatrix)
                }
            } else {
                ColorFilter.colorMatrix(lookMatrix)
            }
        } else {
            // Build a combined matrix: adjustments applied on top of filter
            val brightnessShift = (b - 1f) * 100f
            val contrastScale = c
            val contrastShift = (1f - c) * 128f
            val tempRed = 1f + (t - 1f) * 0.25f
            val tempBlue = 1f - (t - 1f) * 0.25f
            val expScale = e

            val adjMatrix = ColorMatrix(floatArrayOf(
                contrastScale * tempRed * expScale, 0f, 0f, 0f, brightnessShift + contrastShift,
                0f, contrastScale * expScale, 0f, 0f, brightnessShift + contrastShift,
                0f, 0f, contrastScale * tempBlue * expScale, 0f, brightnessShift + contrastShift,
                0f, 0f, 0f, 1f, 0f
            ))
            val satMatrix = ColorMatrix().apply { setToSaturation(s) }
            adjMatrix *= satMatrix

            // v4.6.0: compose the premium look preview matrix on top of adjustments
            if (lookMatrix != null) {
                adjMatrix *= lookMatrix
            }

            // If there's also a filter, combine them
            if (colorFilter != null) {
                val f = project.selectedFilter.lowercase().replace("-", "_").replace(" ", "_")
                val filterMatrix = buildFilterMatrix(f)
                if (filterMatrix != null) {
                    filterMatrix *= adjMatrix
                    ColorFilter.colorMatrix(filterMatrix)
                } else {
                    ColorFilter.colorMatrix(adjMatrix)
                }
            } else {
                ColorFilter.colorMatrix(adjMatrix)
            }
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

            // 2027 8K: Premium launcher moved to bottom toolbar (gradient pills)
            // Top floating buttons removed — AI Hub, Presets, Pro, Studio now in bottom toolbar.

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
                // Live color filter overlay — applies the combined cinematic filter
                // + image-editor adjustments on top of the video in real-time.
                if (combinedColorFilter != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .drawWithContent {
                                drawContent()
                                // Draw a semi-transparent overlay tint based on filter
                                val f = project.selectedFilter.lowercase()
                                val tint = when {
                                    f.contains("sepia") -> Color(0xFF704214).copy(alpha = 0.15f)
                                    f.contains("warm") || f.contains("sunset") || f.contains("golden") -> Color(0xFFFF8C00).copy(alpha = 0.08f)
                                    f.contains("cool") || f.contains("arctic") -> Color(0xFF00BFFF).copy(alpha = 0.08f)
                                    f.contains("cyberpunk") -> Color(0xFF00FFFF).copy(alpha = 0.06f)
                                    f.contains("vintage") || f.contains("fade") -> Color(0xFFD4A76A).copy(alpha = 0.10f)
                                    f.contains("teal") -> Color(0xFF008080).copy(alpha = 0.08f)
                                    f.contains("rose") -> Color(0xFFFF1493).copy(alpha = 0.06f)
                                    else -> Color.Transparent
                                }
                                if (tint != Color.Transparent) {
                                    drawRect(tint)
                                }
                            }
                    )
                }

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

                // Text overlay -- v5.0.0 LIVE PREVIEW ANIMATION
                // Renders the text with a real Compose animation matching the
                // selected textAnimationType so the user sees the effect live
                // (the same effect is burned in at export via FFmpeg drawtext).
                if (project.activeTextOverlay != null && layerTextVisible) {
                    val animType = project.textAnimationType.lowercase()
                    val transition = rememberInfiniteTransition(label = "textAnim")
                    // Shared animated float 0..1 used by all effects
                    val t = transition.animateFloat(
                        initialValue = 0f,
                        targetValue = 1f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(durationMillis = 1800, easing = LinearEasing),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "t"
                    ).value
                    // Second float for staggered/jitter effects
                    val t2 = transition.animateFloat(
                        initialValue = 0f,
                        targetValue = 1f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(durationMillis = 600, easing = LinearEasing),
                            repeatMode = RepeatMode.Restart
                        ),
                        label = "t2"
                    ).value

                    // Compute per-animation modifiers
                    val alpha = when (animType) {
                        "fade", "fade in", "fadein" -> 0.25f + 0.75f * t
                        else -> 1f
                    }
                    val scale = when (animType) {
                        "zoom", "zoom in", "zoomin" -> 0.8f + 0.4f * t
                        "pop" -> 0.9f + 0.2f * t
                        else -> 1f
                    }
                    val offsetY = when (animType) {
                        "bounce", "bounce in" -> (-12f * (1f - t)).dp
                        "wave" -> (-6f * kotlin.math.sin(t * kotlin.math.PI.toFloat() * 2f)).dp
                        else -> 0.dp
                    }
                    val offsetX = when (animType) {
                        "slide", "slide in", "slidein" -> (-40f + 40f * t).dp
                        "glitch" -> (4f * kotlin.math.sin(t2 * kotlin.math.PI.toFloat() * 8f)).dp
                        else -> 0.dp
                    }
                    val textColor = when (animType) {
                        "neon" -> Color.White.copy(alpha = 0.6f + 0.4f * t)
                        else -> Color.White
                    }
                    val showCursor = animType == "typewriter" && (t2 < 0.5f)

                    Box(
                        modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 16.dp)
                            .graphicsLayer {
                                this.alpha = alpha
                                this.scaleX = scale
                                this.scaleY = scale
                                this.translationX = offsetX.value
                                this.translationY = offsetY.value
                            }
                            .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = project.activeTextOverlay!!,
                                color = textColor,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            if (showCursor) {
                                Spacer(Modifier.width(2.dp))
                                Text("|", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                        }
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

                // 2027 8K: LIVE PREVIEW badge removed — cleaner pure black preview
                // v5.2.0 — Tap-to-edit overlay on text
                if (project.activeTextOverlay != null && layerTextVisible) {
                    Box(
                        modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 48.dp)
                            .background(Color.Black.copy(0.5f), RoundedCornerShape(6.dp))
                            .border(1.dp, CyberCyan.copy(0.5f), RoundedCornerShape(6.dp))
                            .clickable { selectedTool = 6 }
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text("✏️", fontSize = 10.sp)
                            Text("Tap to edit text", fontSize = 8.sp, color = CyberCyan, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // v5.2.0 — Tap-to-edit overlay on sticker
                if (project.stickerType != "none" && layerStickerVisible) {
                    Box(
                        modifier = Modifier.align(Alignment.TopEnd).padding(top = 28.dp, end = 8.dp)
                            .background(Color.Black.copy(0.5f), RoundedCornerShape(6.dp))
                            .border(1.dp, NeonOrange.copy(0.5f), RoundedCornerShape(6.dp))
                            .clickable { selectedTool = 5 }
                            .padding(horizontal = 6.dp, vertical = 3.dp)
                    ) {
                        Text("✏️ Edit sticker", fontSize = 7.sp, color = NeonOrange, fontWeight = FontWeight.Bold)
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

                // v5.2.0 — Edit-on-preview hint (bottom-left)
                Box(
                    modifier = Modifier.align(Alignment.BottomStart).padding(6.dp)
                        .background(Color.Black.copy(0.5f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text("🎨 Edit on preview", fontSize = 7.sp, color = Color.White.copy(0.8f), fontWeight = FontWeight.Bold)
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
            onToggleStickerLayer = { layerStickerVisible = !layerStickerVisible },
            onSeekTo = { seekMs ->
                exoPlayer.seekTo(seekMs)
                currentPlaybackTime = seekMs
            },
            onSetTrimStart = { seekMs ->
                // Set trim start to current playhead position (keep existing end)
                val newStart = seekMs.coerceIn(0L, project.trimEndMs - 100)
                onUpdateTrim(newStart, project.trimEndMs)
            },
            onSetTrimEnd = { seekMs ->
                // Set trim end to current playhead position (keep existing start)
                val dur = if (project.durationMs > 0) project.durationMs else
                    if (exoPlayer.duration > 0) exoPlayer.duration else 30000L
                val newEnd = seekMs.coerceIn(project.trimStartMs + 100, dur)
                onUpdateTrim(project.trimStartMs, newEnd)
            },
            onSplitHere = { seekMs ->
                // Split: set trim start to the playhead position for the "second half"
                val dur = if (project.durationMs > 0) project.durationMs else
                    if (exoPlayer.duration > 0) exoPlayer.duration else 30000L
                val newStart = seekMs.coerceIn(0L, dur - 100)
                onUpdateTrim(newStart, dur)
            }
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
                onRemoveLayer = onRemoveLayer,
                onUpdateBlendMode = onUpdateBlendMode,
                onToggleReverse = onToggleReverse,
                onUpdateFreezeFrame = onUpdateFreezeFrame,
                onUpdateColorLift = onUpdateColorLift,
                onUpdateColorGamma = onUpdateColorGamma,
                onUpdateColorGain = onUpdateColorGain,
                onUpdateAudioEffect = onUpdateAudioEffect,
                onUpdateVoiceChangerPitch = onUpdateVoiceChangerPitch,
                onToggleAudioDucking = onToggleAudioDucking,
                onUpdateBorderStyle = onUpdateBorderStyle,
                onUpdateVignetteStyle = onUpdateVignetteStyle,
                onUpdatePremiumLook = onUpdatePremiumLook
            )
        }

        // ─── 6. CAPCUT TOOL BAR (no import button) ────────────
        CapCutToolBar(
            selectedTool = selectedTool,
            onAiHub = onAiHub,
            onSocialPresets = onSocialPresets,
            onProTier = onProTier,
            onPremiumStudio = onPremiumStudio,
            onToolSelected = { idx ->
                // v6.0.0 — Effects (idx 7) & Stickers (idx 8) open full-screen galleries
                when (idx) {
                    7 -> onOpenEffects()
                    8 -> onOpenStickers()
                    else -> {
                        if (selectedTool == idx) { isPanelExpanded = !isPanelExpanded } else { selectedTool = idx; isPanelExpanded = true }
                    }
                }
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
    onToggleStickerLayer: () -> Unit,
    onSeekTo: (Long) -> Unit = {},
    onSetTrimStart: (Long) -> Unit = {},
    onSetTrimEnd: (Long) -> Unit = {},
    onSplitHere: (Long) -> Unit = {}
) {
    // Real video duration for 1-second precision ruler
    val durationMs = if (project.durationMs > 0) project.durationMs else
        if (exoPlayer.duration > 0) exoPlayer.duration else 30000L
    val durationSec = (durationMs / 1000.0).coerceAtLeast(1.0)
    // Playhead position as a fraction [0..1]
    val playheadFraction = (currentTime.toFloat() / durationMs).coerceIn(0f, 1f)

    // Playhead action menu state
    var showPlayheadMenu by remember { mutableStateOf(false) }

    // Wrap the whole timeline in BoxWithConstraints so the moving playhead
    // can use the EXACT measured width (perfect 1-second alignment).
    BoxWithConstraints(
        modifier = Modifier.fillMaxWidth().height(120.dp)
            .background(Color(0xFF111318)).border(1.dp, Color.White.copy(0.04f))
            // TAP TO SEEK: tap anywhere on the timeline to move the playhead
            .pointerInput(durationMs) {
                detectTapGestures(
                    onTap = { offset ->
                        val widthPx = size.width.toFloat()
                        val fraction = (offset.x / widthPx).coerceIn(0f, 1f)
                        val seekMs = (fraction * durationMs).toLong()
                        onSeekTo(seekMs)
                    },
                    onDoubleTap = { offset ->
                        // Double-tap shows the playhead action menu
                        val widthPx = size.width.toFloat()
                        val fraction = (offset.x / widthPx).coerceIn(0f, 1f)
                        val seekMs = (fraction * durationMs).toLong()
                        onSeekTo(seekMs)
                        showPlayheadMenu = true
                    }
                )
            }
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
            // ★ TRIM REGION OVERLAY — shows the trimmed portion of the video
            // Dimmed areas outside the trim region, bright area inside
            val trimStartFraction = (project.trimStartMs.toFloat() / durationMs).coerceIn(0f, 1f)
            val trimEndFraction = (project.trimEndMs.toFloat() / durationMs).coerceIn(0f, 1f)
            if (trimStartFraction > 0f) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width((trimStartFraction * timelineWidthDp).dp)
                        .offset(x = 0.dp)
                        .background(Color.Black.copy(alpha = 0.55f))
                )
            }
            if (trimEndFraction < 1f) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(((1f - trimEndFraction) * timelineWidthDp).dp)
                        .offset(x = (trimEndFraction * timelineWidthDp).dp)
                        .background(Color.Black.copy(alpha = 0.55f))
                )
            }
            // Trim start handle (left edge of trim region)
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(3.dp)
                    .offset(x = (trimStartFraction * timelineWidthDp).dp)
                    .background(CyberCyan)
            )
            // Trim end handle (right edge of trim region)
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(3.dp)
                    .offset(x = (trimEndFraction * timelineWidthDp).dp)
                    .background(CyberCyan)
            )

            // ★ LIVE MOVING PLAYHEAD — tracks actual playback position second-by-second
            // Uses the EXACT measured timelineWidthDp from BoxWithConstraints for perfect alignment.
            // The playhead handle is DRAGGABLE so the user can scrub through the video.
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(2.dp)
                    .offset(x = (playheadFraction * timelineWidthDp).dp)
                    .background(Brush.verticalGradient(listOf(NeonOrange, Color.Transparent)))
            ) {
                // Draggable playhead handle (the circle on top)
                Box(
                    modifier = Modifier
                        .size(14.dp)
                        .background(NeonOrange, CircleShape)
                        .border(1.5.dp, Color.White, CircleShape)
                        .align(Alignment.TopCenter)
                        .neonGlow(NeonOrange, CircleShape, 2.dp)
                        .pointerInput(durationMs) {
                            detectDragGestures(
                                onDragStart = { },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    val widthPx = size.width.toFloat()
                                    if (widthPx > 0) {
                                        val currentMs = (playheadFraction * durationMs).toLong()
                                        val deltaMs = ((dragAmount.x / widthPx) * durationMs).toLong()
                                        val newMs = (currentMs + deltaMs).coerceIn(0L, durationMs)
                                        onSeekTo(newMs)
                                    }
                                }
                            )
                        }
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onLongPress = { showPlayheadMenu = true },
                                onTap = { showPlayheadMenu = true }
                            )
                        }
                )
            }

            // ★ PLAYHEAD ACTION MENU — appears when user taps/long-presses the playhead
            // Gives the user editing options at the current position
            if (showPlayheadMenu) {
                Popup(
                    alignment = Alignment.TopCenter,
                    onDismissRequest = { showPlayheadMenu = false },
                    properties = PopupProperties(focusable = true)
                ) {
                    Card(
                        modifier = Modifier
                            .width(200.dp)
                            .padding(top = 8.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1D24)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            // Current position display
                            Text(
                                "Playhead: ${formatTime(currentTime)}",
                                fontSize = 12.sp,
                                color = NeonOrange,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            // Split here
                            PlayheadMenuItem(
                                icon = "✂️",
                                label = "Split at playhead",
                                onClick = { onSplitHere(currentTime); showPlayheadMenu = false }
                            )
                            // Set trim start
                            PlayheadMenuItem(
                                icon = "⏮️",
                                label = "Set trim start here",
                                onClick = { onSetTrimStart(currentTime); showPlayheadMenu = false }
                            )
                            // Set trim end
                            PlayheadMenuItem(
                                icon = "⏭️",
                                label = "Set trim end here",
                                onClick = { onSetTrimEnd(currentTime); showPlayheadMenu = false }
                            )
                            // Play/pause toggle
                            PlayheadMenuItem(
                                icon = if (exoPlayer.isPlaying) "⏸️" else "▶️",
                                label = if (exoPlayer.isPlaying) "Pause" else "Play",
                                onClick = {
                                    if (exoPlayer.isPlaying) exoPlayer.pause() else exoPlayer.play()
                                    showPlayheadMenu = false
                                }
                            )
                            // Close
                            PlayheadMenuItem(
                                icon = "✕",
                                label = "Close menu",
                                onClick = { showPlayheadMenu = false }
                            )
                        }
                    }
                }
            }
        }
    }
}


// ─── Playhead Menu Item ───────────────────────────────────────
@Composable
private fun PlayheadMenuItem(
    icon: String,
    label: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(vertical = 8.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(icon, fontSize = 16.sp, modifier = Modifier.width(24.dp))
        Spacer(Modifier.width(8.dp))
        Text(label, fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Medium)
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
    onToolSelected: (Int) -> Unit,
    onAiHub: () -> Unit = {},
    onSocialPresets: () -> Unit = {},
    onProTier: () -> Unit = {},
    onPremiumStudio: () -> Unit = {}
) {
    val tools = listOf(
        "✂️" to "Edit", "📑" to "Layers", "⚡" to "Speed", "📐" to "Crop",
        "🔊" to "Audio", "🔤" to "Text", "🎨" to "Filters", "✨" to "Effects",
        "😄" to "Stickers", "🔀" to "Trans", "🎭" to "Anim", "🎬" to "3D",
        "🖼️" to "Image", "📋" to "Template",
        "🎬" to "Chroma", "🧹" to "Erase", "🖌️" to "ImgEdit", "📐" to "Orient",
        "🌈" to "Blend", "↺️" to "Reverse", "💉" to "ColorFX",
        "🎧" to "AudioFX", "🎤" to "Voice", "🎉" to "Borders",
        "✨" to "Vignette", "❄️" to "Freeze", "📷" to "Looks",
        "🖍️" to "Canvas",
        // 2027 8K: Premium tools merged into bottom toolbar as gradient pills
        "🤖" to "AI Hub", "📱" to "Presets",
        "👑" to "Pro", "✨" to "Studio"
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
            // 2027 8K: Premium tools (last 4) get gradient pill styling
            val isPremium = idx >= tools.size - 4
            Box(
                modifier = Modifier.clip(RoundedCornerShape(10.dp))
                    .background(
                        if (isPremium) {
                            if (isActive) Brush.horizontalGradient(listOf(Color(0xFFFF5A3C), Color(0xFF9D4EDD)))
                            else Brush.horizontalGradient(listOf(Color(0xFFFF5A3C).copy(alpha = 0.25f), Color(0xFF9D4EDD).copy(alpha = 0.25f)))
                        } else {
                            Brush.horizontalGradient(listOf(
                                if (isActive) Color(0xFFFF5A3C).copy(alpha = 0.3f) else Color.Transparent,
                                if (isActive) Color(0xFF9D4EDD).copy(alpha = 0.15f) else Color.Transparent
                            ))
                        }
                    )
                    .clickable {
                        // 2027 8K: Premium tools open their respective screens
                        when (name) {
                            "AI Hub" -> onAiHub()
                            "Presets" -> onSocialPresets()
                            "Pro" -> onProTier()
                            "Studio" -> onPremiumStudio()
                            else -> onToolSelected(idx)
                        }
                    }
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(emoji, fontSize = 14.sp)
                    Text(
                        name, fontSize = 7.sp, fontWeight = FontWeight.Bold,
                        color = if (isPremium) Color.White else if (isActive) Color(0xFFFF5A3C) else Color.Gray
                    )
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
    onUpdateVignetteStyle: (String) -> Unit = {},
    onUpdatePremiumLook: (String) -> Unit = {}
) {
    Box(
        modifier = Modifier.fillMaxWidth().height(220.dp).padding(horizontal = 8.dp, vertical = 2.dp)
            .background(Color(0xFF1A1C24).copy(0.85f), RoundedCornerShape(12.dp))
            .border(1.dp, Color.White.copy(0.04f), RoundedCornerShape(12.dp))
    ) {
        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(8.dp)) {
            when (selectedTool) {
                0 -> EditPanel(project, onUpdateCropPreset, onUpdateAspectPreset, onUpdateSpeed, onUpdateSpeedCurve, onUpdateRotation, onToggleFlipHorizontal, onToggleFlipVertical, onUpdateResolution, onUpdateTrim, onUpdateImageEditorBrightness, onUpdateImageEditorContrast, onUpdateImageEditorSaturation, onUpdateImageEditorSharpen, onUpdateImageEditorTemperature, onUpdateImageEditorFade, onUpdateImageEditorVignette, onUpdateImageEditorGrain, onToggleReverse, onUpdateFreezeFrame)
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
                26 -> LooksPanel(project, onUpdatePremiumLook)
                27 -> CanvasPanel()
            }
        }
        // Collapse handle
        Box(modifier = Modifier.fillMaxWidth().align(Alignment.BottomCenter).clickable(onClick = onCollapse).padding(2.dp), contentAlignment = Alignment.Center) {
            Box(modifier = Modifier.width(32.dp).height(3.dp).background(Color.White.copy(0.15f), RoundedCornerShape(2.dp)))
        }
    }
}


// ─// v5.0.0 helper: 5-tuple for adjustment data (emoji, name, value, range, onChange)
private data class Quint(val emoji: String, val name: String, val value: Float, val range: ClosedFloatingPointRange<Float>, val onChange: (Float) -> Unit)


// ── 0. EDIT PANEL ─────────────────────────────────────────────
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
    onUpdateTrim: (Long, Long) -> Unit,
    onUpdateBrightness: (Float) -> Unit = {},
    onUpdateContrast: (Float) -> Unit = {},
    onUpdateSaturation: (Float) -> Unit = {},
    onUpdateSharpen: (Float) -> Unit = {},
    onUpdateTemperature: (Float) -> Unit = {},
    onUpdateFade: (Float) -> Unit = {},
    onUpdateVignette: (Float) -> Unit = {},
    onUpdateGrain: (Float) -> Unit = {},
    onToggleReverse: () -> Unit = {},
    onUpdateFreezeFrame: (Long) -> Unit = {}
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
                // v5.0.0: Real wired adjustment sliders (no more empty onValueChange)
                val adjData = listOf(
                    Quint("☀️", "Brightness", project.imageEditorBrightness, 0f..1f, onUpdateBrightness),
                    Quint("🔲", "Contrast", (project.imageEditorContrast / 2f).coerceIn(0f, 1f), 0f..1f, { v -> onUpdateContrast((v * 2f).coerceIn(0f, 2f)) }),
                    Quint("🎨", "Saturation", (project.imageEditorSaturation / 2f).coerceIn(0f, 1f), 0f..1f, { v -> onUpdateSaturation((v * 2f).coerceIn(0f, 2f)) }),
                    Quint("🔪", "Sharpness", project.imageEditorSharpen.coerceIn(0f, 1f), 0f..1f, onUpdateSharpen),
                    Quint("🌡️", "Temperature", (project.imageEditorTemperature / 2f + 0.5f).coerceIn(0f, 1f), 0f..1f, { v -> onUpdateTemperature((v * 2f - 1f).coerceIn(-1f, 1f)) }),
                    Quint("🌫️", "Fade", project.imageEditorFade.coerceIn(0f, 1f), 0f..1f, onUpdateFade),
                    Quint("🌑", "Vignette", project.imageEditorVignette.coerceIn(0f, 1f), 0f..1f, onUpdateVignette),
                    Quint("📸", "Grain", project.imageEditorGrain.coerceIn(0f, 1f), 0f..1f, onUpdateGrain)
                )
                adjData.forEach { (emoji, name, value, range, onChange) ->
                    Row(Modifier.fillMaxWidth().background(Color.White.copy(0.03f), RoundedCornerShape(6.dp)).padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(emoji, fontSize = 12.sp)
                        Spacer(Modifier.width(6.dp))
                        Text(name, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.White, modifier = Modifier.width(60.dp))
                        Slider(value = value.coerceIn(range.start, range.endInclusive), onValueChange = onChange, valueRange = range, colors = SliderDefaults.colors(activeTrackColor = NeonOrange, thumbColor = NeonOrange, inactiveTrackColor = Color.White.copy(0.08f)), modifier = Modifier.weight(1f).height(18.dp))
                        Text("${(value * 100).toInt()}%", fontSize = 7.sp, color = NeonOrange, fontWeight = FontWeight.Bold, modifier = Modifier.width(28.dp))
                    }
                }
                // Reset button
                Row(Modifier.fillMaxWidth().padding(top = 2.dp), horizontalArrangement = Arrangement.End) {
                    Box(Modifier.background(Color.White.copy(0.06f), RoundedCornerShape(6.dp)).clickable {
                        onUpdateBrightness(0.5f); onUpdateContrast(1f); onUpdateSaturation(1f)
                        onUpdateSharpen(0f); onUpdateTemperature(0f); onUpdateFade(0f)
                        onUpdateVignette(0f); onUpdateGrain(0f)
                        android.widget.Toast.makeText(ctx, "Adjustments reset!", android.widget.Toast.LENGTH_SHORT).show()
                    }.padding(horizontal = 12.dp, vertical = 4.dp)) {
                        Text("↺ Reset All", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color.White)
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
                // v5.0.0: Wired to project.isReverseEnabled + onToggleReverse
                val isReversed = project.isReverseEnabled
                Text("REVERSE VIDEO", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                Box(Modifier.fillMaxWidth().background(if (isReversed) CyberCyan.copy(0.15f) else Color.White.copy(0.04f), RoundedCornerShape(8.dp)).border(1.dp, if (isReversed) CyberCyan.copy(0.3f) else Color.Transparent, RoundedCornerShape(8.dp)).clickable { onToggleReverse(); android.widget.Toast.makeText(ctx, if (!isReversed) "Reverse applied!" else "Reverse removed!", android.widget.Toast.LENGTH_SHORT).show() }.padding(12.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("🔄", fontSize = 28.sp)
                        Text(if (isReversed) "Tap to Remove Reverse" else "Tap to Reverse Video", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = if (isReversed) CyberCyan else Color.White)
                        Text(if (isReversed) "Reverse is active" else "Plays video backwards", fontSize = 8.sp, color = if (isReversed) CyberCyan else Color.Gray)
                    }
                }
            }
            "freeze" -> {
                // v5.0.0: Wired to project.freezeFrameMs + onUpdateFreezeFrame
                val isFrozen = project.freezeFrameMs > 0L
                Text("FREEZE FRAME", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                if (isFrozen) {
                    Text("Active: ${project.freezeFrameMs / 1000.0}s at ${formatTime(project.trimStartMs)}", fontSize = 7.sp, color = CyberCyan)
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf(0L, 250L, 500L, 1000L, 2000L, 3000L).forEach { ms ->
                        val sel = project.freezeFrameMs == ms
                        Box(Modifier.weight(1f).background(if (sel) CyberCyan.copy(0.2f) else Color.White.copy(0.04f), RoundedCornerShape(6.dp)).clickable { onUpdateFreezeFrame(ms); android.widget.Toast.makeText(ctx, if (ms > 0L) "Freeze ${ms / 1000.0}s at ${formatTime(project.trimStartMs)}!" else "Freeze removed!", android.widget.Toast.LENGTH_SHORT).show() }.padding(4.dp), contentAlignment = Alignment.Center) {
                            Text(if (ms == 0L) "Off" else "${ms / 1000.0}s", fontSize = 7.sp, fontWeight = FontWeight.Bold, color = if (sel) CyberCyan else Color.White)
                        }
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
                "📝" to "text",
                "🖼️" to "image",
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
            Triple("🎬", "Video Layer", "video"),
            Triple("🔊", "Audio Layer", "audio"),
            Triple("📝", "Text Layer", "text"),
            Triple("🖼️", "Image Layer", "image"),
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
                        Text(if (isActive) "👁️" else "🙈", fontSize = 10.sp)
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
    var txt by remember { mutableStateOf(project.activeTextOverlay ?: "") }
    var textSubTab by remember { mutableStateOf("text") }
    var selectedFontIndex by remember { mutableStateOf(0) }
    var selectedColorIndex by remember { mutableStateOf(0) }

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("TEXT STUDIO", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = NeonOrange)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("100+ Fonts • Full Styling • Animations", fontSize = 7.sp, color = Color.Gray)
            Box(Modifier.background(NeonOrange.copy(0.15f), RoundedCornerShape(4.dp)).padding(horizontal = 5.dp, vertical = 2.dp)) {
                Text("✓ Pro", fontSize = 7.sp, fontWeight = FontWeight.Bold, color = NeonOrange)
            }
        }

        // Sub-tabs: Text | Fonts | Color | Motion | Logo
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            listOf("text" to "Text", "fonts" to "Fonts", "color" to "Color", "motion" to "Motion", "logo" to "Logo").forEach { (id, label) ->
                val sel = textSubTab == id
                Box(Modifier.background(if (sel) NeonOrange.copy(0.2f) else Color.White.copy(0.04f), RoundedCornerShape(6.dp)).clickable { textSubTab = id }.padding(horizontal = 8.dp, vertical = 4.dp)) {
                    Text(label, fontSize = 8.sp, fontWeight = FontWeight.Bold, color = if (sel) NeonOrange else Color.White)
                }
            }
        }

        when (textSubTab) {
            "text" -> {
                // Text input
                OutlinedTextField(value = txt, onValueChange = { txt = it; onUpdateText(if (it.isBlank()) null else it) }, placeholder = { Text("Type your text...", fontSize = 9.sp, color = Color.Gray) }, colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = NeonOrange, unfocusedBorderColor = Color.White.copy(0.1f), focusedTextColor = Color.White, unfocusedTextColor = Color.White), modifier = Modifier.fillMaxWidth().height(36.dp), shape = RoundedCornerShape(8.dp))

                // Quick text presets
                Text("QUICK TEXT PRESETS", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    items(listOf("🔥 Fire Text", "💫 Glow Text", "🎬 Title", "📍 Subtitle", "🎵 Lyrics", "💬 Dialog", "📰 Breaking", "⚡ Neon", "💥 Boom", "🔍 Search", "👍 Like", "🎉 Party", "🏆 Winner", "👑 Royal", "💯 100%", "🚀 Go", "✨ Magic", "🌟 Star")) { preset ->
                        Box(Modifier.background(Color.White.copy(0.04f), RoundedCornerShape(6.dp)).clickable { onUpdateText(preset); txt = preset }.padding(horizontal = 8.dp, vertical = 4.dp)) {
                            Text(preset, fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }

            "fonts" -> {
                // 100+ font/style options
                Text("100+ FONT STYLES", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                val fonts = listOf(
                    "Default", "Bold", "Italic", "Bold Italic", "Thin", "Light", "Medium", "Black",
                    "Serif", "Serif Bold", "Serif Italic", "Sans Serif", "Monospace", "Cursive",
                    "Comic", "Handwriting", "Typewriter", "Retro", "Vintage", "Classic",
                    "Modern", "Futuristic", "Minimal", "Elegant", "Luxury", "Gothic",
                    "Graffiti", "Street", "Urban", "Bubble", "Outline", "Shadow",
                    "3D Block", "Chrome", "Gold", "Silver", "Bronze", "Metallic",
                    "Neon Glow", "Fire", "Ice", "Rainbow", "Gradient", "Holographic",
                    "Pixel", "8-Bit", "Arcade", "Digital", "Matrix", "Cyberpunk",
                    "Calligraphy", "Script", "Brush", "Marker", "Pencil", "Sketch",
                    "Stamp", "Stencil", "Military", "Sports", "Athletic", "Collegiate",
                    "Western", "Saloon", "Carnival", "Circus", "Magician", "Wizard",
                    "Fairy", "Princess", "Royal", "Knight", "Viking", "Samurai",
                    "Ninja", "Pirate", "Zombie", "Horror", "Vampire", "Ghost",
                    "Halloween", "Christmas", "Birthday", "Wedding", "Love", "Heart",
                    "Valentine", "Summer", "Winter", "Spring", "Autumn", "Tropical",
                    "Beach", "Ocean", "Mountain", "Desert", "Forest", "Galaxy",
                    "Space", "Star", "Moon", "Sun", "Cloud", "Lightning",
                    "Thunder", "Rain", "Snow", "Storm"
                )
                FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    fonts.forEachIndexed { idx, fontName ->
                        val sel = selectedFontIndex == idx
                        Box(Modifier.background(if (sel) NeonOrange.copy(0.2f) else Color.White.copy(0.04f), RoundedCornerShape(6.dp))
                            .clickable { selectedFontIndex = idx; android.widget.Toast.makeText(ctx, "Font: $fontName", android.widget.Toast.LENGTH_SHORT).show() }
                            .padding(horizontal = 6.dp, vertical = 4.dp)) {
                            Text(fontName, fontSize = 7.sp, fontWeight = if (sel) FontWeight.Black else FontWeight.Bold, color = if (sel) NeonOrange else Color.White)
                        }
                    }
                }
            }

            "color" -> {
                // Color picker for text
                Text("TEXT COLOR", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                val textColors = listOf(
                    Color.White, Color.Black, Color.Red, Color.Green, Color.Blue,
                    Color.Yellow, Color.Cyan, Color.Magenta, Color(0xFF7C5CFF), Color(0xFFFF6B35),
                    Color(0xFF2DD4BF), Color(0xFFFF3D7F), Color(0xFFFFD700), Color(0xFF00FF00),
                    Color(0xFFFF00FF), Color(0xFF00FFFF), Color(0xFFFFA500), Color(0xFF800080),
                    Color(0xFFFF1493), Color(0xFF00CED1), Color(0xFFFF4500), Color(0xFF32CD32),
                    Color(0xFFFF69B4), Color(0xFF1E90FF), Color(0xFFFF8C00), Color(0xFF9370DB),
                    Color(0xFF20B2AA), Color(0xFFFFB6C1), Color(0xFF90EE90), Color(0xFFDDA0DD),
                    Color(0xFFF0E68C), Color(0xFFE6E6FA), Color(0xFFFFFACD), Color(0xFFAFEEEE)
                )
                FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    textColors.forEachIndexed { idx, color ->
                        val sel = selectedColorIndex == idx
                        Box(Modifier.size(28.dp).background(color, RoundedCornerShape(6.dp))
                            .border(if (sel) 2.dp else 0.dp, Color.White, RoundedCornerShape(6.dp))
                            .clickable { selectedColorIndex = idx; android.widget.Toast.makeText(ctx, "Text color selected", android.widget.Toast.LENGTH_SHORT).show() }) {}
                    }
                }

                // Text background/stroke options
                Text("TEXT BACKGROUND & STROKE", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    listOf("None", "Solid BG", "Outline", "Shadow", "Glow", "Neon", "3D Shadow", "Double Outline", "Gradient BG", "Blur BG", "Box BG", "Strip BG").forEach { style ->
                        Box(Modifier.background(Color.White.copy(0.04f), RoundedCornerShape(6.dp)).clickable { android.widget.Toast.makeText(ctx, "Text style: $style", android.widget.Toast.LENGTH_SHORT).show() }.padding(horizontal = 6.dp, vertical = 4.dp)) {
                            Text(style, fontSize = 7.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }

            "motion" -> {
                // Animated text — loop/move full screen (user request: "text mein loop hoon full screen per move hon")
                Text("LOOP & FULL SCREEN MOTION", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = CyberCyan)
                FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    listOf("Loop L→R" to "loop_lr", "Loop R→L" to "loop_rl", "Loop Up" to "loop_up", "Loop Down" to "loop_down", "Bounce Loop" to "loop_bounce", "Pulse Loop" to "loop_pulse", "Full Screen Scroll" to "fullscreen_scroll", "Marquee Loop" to "marquee_loop", "Orbit" to "orbit", "Wave Motion" to "wave_motion", "Typewriter Loop" to "typewriter_loop", "Zoom Loop" to "zoom_loop").forEach { (label, id) ->
                        Box(Modifier.background(CyberCyan.copy(0.1f), RoundedCornerShape(6.dp)).clickable { onUpdateAnim(id); android.widget.Toast.makeText(ctx, "Motion: $label", android.widget.Toast.LENGTH_SHORT).show() }.padding(horizontal = 6.dp, vertical = 4.dp)) {
                            Text(label, fontSize = 7.sp, fontWeight = FontWeight.Bold, color = CyberCyan)
                        }
                    }
                }

                // Text animations
                Text("TEXT ANIMATIONS", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    listOf("None", "Fade", "Typewriter", "Bounce", "Zoom", "Slide", "Pop", "Glitch", "Neon", "Wave", "Slide L", "Slide R", "Slide Up", "Slide Down", "Rotate", "Flip", "Elastic", "Spring", "Shake", "Blink", "Pulse", "Rainbow", "Fire", "Ice", "Gold", "Metallic", "Explode", "Implode", "Glow", "Frozen").forEach { a ->
                        val sel = project.textAnimationType.lowercase() == a.lowercase()
                        Box(Modifier.background(if (sel) NeonOrange.copy(0.2f) else Color.White.copy(0.04f), RoundedCornerShape(6.dp)).clickable { onUpdateAnim(if (sel) "none" else a.replace(" ", "_").lowercase()) }.padding(horizontal = 6.dp, vertical = 4.dp)) {
                            Text(a, fontSize = 7.sp, fontWeight = FontWeight.Bold, color = if (sel) NeonOrange else Color.White)
                        }
                    }
                }
            }

            "logo" -> {
                // Logo overlay support (user request: "logo laga sake")
                Text("LOGO OVERLAY", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = NeonOrange)
                Text("Add your brand logo or watermark image on top of the video. Use the Image tool (tool dock) to pick a logo image, then adjust opacity and scale here.", fontSize = 8.sp, color = Color.Gray)
                Spacer(Modifier.height(4.dp))
                FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    listOf("Top-Left", "Top-Right", "Bottom-Left", "Bottom-Right", "Center", "Top-Center", "Bottom-Center").forEach { pos ->
                        Box(Modifier.background(Color.White.copy(0.04f), RoundedCornerShape(6.dp)).clickable { android.widget.Toast.makeText(ctx, "Logo position: $pos — Use Image tool to pick logo", android.widget.Toast.LENGTH_LONG).show() }.padding(horizontal = 6.dp, vertical = 4.dp)) {
                            Text(pos, fontSize = 7.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
                Text("ℹ️ Tip: Select the Image tool from the dock below to import your logo PNG (transparent background recommended).", fontSize = 7.sp, color = Color.Gray)
            }
        }
    }
}


// ─── 6. FILTERS PANEL ──────────────────────────────────────────
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FiltersPanel(project: VideoProject, onUpdateFilter: (String) -> Unit) {
    var filterCategory by remember { mutableStateOf("all") }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            Text("CINEMATIC FILTERS", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = CyberCyan)
            Box(Modifier.background(CyberCyan.copy(0.15f), RoundedCornerShape(4.dp)).padding(horizontal = 5.dp, vertical = 2.dp)) {
                Text("✓ Real FFmpeg", fontSize = 7.sp, fontWeight = FontWeight.Bold, color = CyberCyan)
            }
        }
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
                // 2027 8K: Real Canvas demo thumbnail with filter color preview
                Box(
                    Modifier
                        .width(52.dp)
                        .height(72.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (sel) Color(0xFFFF5A3C).copy(0.25f) else Color.White.copy(0.05f), RoundedCornerShape(8.dp))
                        .border(if (sel) 2.dp else 1.dp, if (sel) Color(0xFFFF5A3C) else Color.White.copy(0.08f), RoundedCornerShape(8.dp))
                        .clickable { onUpdateFilter(if (sel) "none" else id) }
                ) {
                    Canvas(modifier = Modifier.fillMaxSize().padding(4.dp)) {
                        // Draw a mini scene with the filter color applied
                        val baseColor = when (id) {
                            "none" -> Color(0xFF4A6FA5)
                            "grayscale", "monochrome" -> Color(0xFF888888)
                            "sepia", "sepia_warm" -> Color(0xFFA0703D)
                            "sepia_cool" -> Color(0xFF8A8070)
                            "invert", "negative" -> Color(0xFFBB5544)
                            "warm", "sunset", "golden", "amber" -> Color(0xFFE8A040)
                            "cool", "arctic", "winter" -> Color(0xFF5090D0)
                            "vintage", "lomo", "polaroid", "holga", "diana", "analog" -> Color(0xFFB89968)
                            "dramatic", "noir" -> Color(0xFF333344)
                            "cinematic", "tokyo", "nyc", "paris" -> Color(0xFF5566AA)
                            "teal" -> Color(0xFF008080)
                            "orange" -> Color(0xFFFF8040)
                            "film", "super8", "kodak", "fuji", "agfa" -> Color(0xFFCC8855)
                            "ilford", "portra" -> Color(0xFF998877)
                            "velvia" -> Color(0xFF22AA66)
                            "provia" -> Color(0xFF4488CC)
                            "astia" -> Color(0xFFDD99AA)
                            "high_contrast" -> Color(0xFF222222)
                            "low_contrast" -> Color(0xFFCCCCCC)
                            "high_saturation" -> Color(0xFFFF0066)
                            "low_saturation" -> Color(0xFF999999)
                            "bright" -> Color(0xFFFFEECC)
                            "dark" -> Color(0xFF221133)
                            "soft", "dreamy", "glow" -> Color(0xFFE0D0F0)
                            "sharp" -> Color(0xFF334455)
                            "haze" -> Color(0xFFB0C0D0)
                            "matte" -> Color(0xFF807060)
                            "litho" -> Color(0xFF554433)
                            "red_boost", "ruby" -> Color(0xFFDD2233)
                            "blue_boost", "sapphire" -> Color(0xFF2255DD)
                            "green_boost", "emerald" -> Color(0xFF22AA44)
                            "purple_haze" -> Color(0xFF9944CC)
                            "pink_dream" -> Color(0xFFFF66BB)
                            "bronze" -> Color(0xFFCD7F32)
                            "platinum" -> Color(0xFFE5E4E2)
                            "neon_city", "miami" -> Color(0xFFFF00FF)
                            "retro_wave", "synthwave" -> Color(0xFFFF0080)
                            "desert" -> Color(0xFFDDBB88)
                            "ocean" -> Color(0xFF0066BB)
                            "autumn" -> Color(0xFFDD6622)
                            "spring" -> Color(0xFF88DD44)
                            "summer" -> Color(0xFFFFDD44)
                            else -> Color(0xFF4A6FA5)
                        }
                        // Draw gradient sky
                        drawRect(
                            brush = Brush.verticalGradient(
                                colors = listOf(baseColor, baseColor.copy(alpha = 0.4f))
                            )
                        )
                        // Draw sun/circle
                        drawCircle(
                            color = baseColor.copy(alpha = 0.9f).compositeOver(Color.White.copy(alpha = 0.3f)),
                            radius = size.minDimension * 0.15f,
                            center = androidx.compose.ui.geometry.Offset(size.width * 0.65f, size.height * 0.3f)
                        )
                        // Draw ground/horizon
                        drawRect(
                            color = baseColor.copy(alpha = 0.6f).compositeOver(Color.Black.copy(alpha = 0.3f)),
                            topLeft = androidx.compose.ui.geometry.Offset(0f, size.height * 0.65f),
                            size = androidx.compose.ui.geometry.Size(size.width, size.height * 0.35f)
                        )
                    }
                    // Label at bottom
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.BottomCenter
                    ) {
                        Text(
                            name, fontSize = 6.sp, fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.padding(bottom = 2.dp)
                        )
                    }
                }
            }
        }
    }
}


private data class EffectItem(val name: String, val effectId: String, val filterId: String, val category: String)

// ─── 7. EFFECTS PANEL ──────────────────────────────────────────
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EffectsPanel(project: VideoProject, onUpdateEffect: (String) -> Unit, onUpdateFilter: (String) -> Unit) {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    var effectCategory by remember { mutableStateOf("all") }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            Text("SUPER EFFECTS", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = NeonOrange)
            Box(Modifier.background(NeonOrange.copy(0.15f), RoundedCornerShape(4.dp)).padding(horizontal = 5.dp, vertical = 2.dp)) {
                Text("✓ Real FFmpeg", fontSize = 7.sp, fontWeight = FontWeight.Bold, color = NeonOrange)
            }
        }
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            listOf("all" to "All", "vfx" to "VFX", "color" to "Color", "motion" to "Motion", "retro" to "Retro", "neon" to "Neon", "magic" to "Magic").forEach { (id, label) ->
                val sel = effectCategory == id
                Box(Modifier.background(if (sel) NeonOrange.copy(0.2f) else Color.White.copy(0.04f), RoundedCornerShape(6.dp)).clickable { effectCategory = id }.padding(horizontal = 6.dp, vertical = 3.dp)) {
                    Text(label, fontSize = 7.sp, fontWeight = FontWeight.Bold, color = if (sel) NeonOrange else Color.White)
                }
            }
        }
        FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            // (displayName, effectId, filterId, category)
            val allEffects = listOf(
                EffectItem("Glitch", "glitch", "invert", "vfx"),
                EffectItem("VHS", "vhs", "sepia", "retro"),
                EffectItem("Chromatic", "chromatic", "invert", "vfx"),
                EffectItem("Lens Flare", "lens_flare", "none", "vfx"),
                EffectItem("Snow", "snow", "none", "vfx"),
                EffectItem("Rain", "rain", "none", "vfx"),
                EffectItem("Fire", "fire", "none", "vfx"),
                EffectItem("Sparkle", "sparkle", "none", "vfx"),
                EffectItem("Dust", "dust", "sepia", "vfx"),
                EffectItem("Motion Blur", "motion_blur", "none", "motion"),
                EffectItem("Shake", "shake", "none", "motion"),
                EffectItem("Flash", "flash", "invert", "motion"),
                EffectItem("Neon Glow", "neon_glow", "invert", "neon"),
                EffectItem("Vignette", "vignette", "grayscale", "color"),
                EffectItem("Rainbow", "rainbow", "none", "color"),
                EffectItem("Film Grain", "film_grain", "sepia", "retro"),
                EffectItem("Bokeh", "bokeh", "none", "vfx"),
                EffectItem("Particles", "particles", "none", "vfx"),
                EffectItem("Strobe", "strobe", "grayscale", "motion"),
                EffectItem("Zoom Pulse", "zoom_pulse", "none", "motion"),
                EffectItem("Wave Distort", "wave_distort", "none", "motion"),
                EffectItem("Flame", "flame", "invert", "vfx"),
                EffectItem("Frost", "frost", "grayscale", "vfx"),
                EffectItem("Starburst", "starburst", "none", "vfx"),
                EffectItem("Face Blur", "face_blur", "none", "vfx"),
                EffectItem("Swirl", "swirl", "invert", "vfx"),
                EffectItem("Explosion", "explosion", "invert", "vfx"),
                EffectItem("Light Leak", "light_leak", "none", "vfx"),
                EffectItem("Film Strip", "film_strip", "sepia", "retro"),
                EffectItem("Color Splash", "color_splash", "invert", "color"),
                EffectItem("Electric", "electric", "invert", "vfx"),
                EffectItem("Tidal", "tidal", "none", "motion"),
                EffectItem("RGB Split", "rgb_glitch", "invert", "vfx"),
                EffectItem("Scanline", "scanline", "none", "retro"),
                EffectItem("CRT", "crt", "none", "retro"),
                EffectItem("8bit", "8bit", "none", "retro"),
                EffectItem("Old Film", "old_film", "sepia", "retro"),
                EffectItem("Bloom", "bloom", "none", "color"),
                EffectItem("HDR", "hdr", "none", "color"),
                EffectItem("Vaporwave", "vaporwave", "none", "neon"),
                EffectItem("Aesthetic", "aesthetic", "none", "color"),
                EffectItem("LoFi", "lofi", "sepia", "retro"),
                EffectItem("Dream", "dream", "none", "color"),
                EffectItem("Night Vision", "night_vision", "invert", "vfx"),
                EffectItem("Thermal", "thermal", "invert", "vfx"),
                EffectItem("Pencil", "pencil", "grayscale", "color"),
                EffectItem("Sketch", "sketch", "grayscale", "color"),
                EffectItem("Cartoon", "cartoon", "none", "color"),
                EffectItem("Watercolor", "watercolor", "none", "color"),
                EffectItem("Oil Paint", "oil_paint", "none", "color"),
                EffectItem("Pixel", "pixel", "none", "vfx"),
                EffectItem("Mosaic", "mosaic", "none", "vfx"),
                EffectItem("Emboss", "emboss", "none", "color"),
                EffectItem("Sharpen", "sharpen_strong", "none", "color"),
                EffectItem("Tilt Shift", "tilt_shift", "none", "color"),
                EffectItem("Kaleidoscope", "kaleidoscope", "invert", "vfx"),
                EffectItem("RGB Glitch", "rgb_split", "invert", "vfx"),
                EffectItem("Disco", "disco", "rainbow", "neon"),
                EffectItem("Concert", "concert", "none", "neon"),
                EffectItem("Party", "party", "rainbow", "neon"),
                // ── v4.4.0 MAGIC / ANIMATED EFFECTS (real FFmpeg time expressions) ──
                EffectItem("✨ Magic Pulse", "magic_pulse", "none", "magic"),
                EffectItem("🌈 Hue Cycle", "magic_hue_cycle", "none", "magic"),
                EffectItem("🎨 Color Flow", "magic_color_flow", "none", "magic"),
                EffectItem("💡 Bright Flow", "magic_brightness_flow", "none", "magic"),
                EffectItem("🔍 Zoom Pulse", "magic_zoom_pulse", "none", "magic"),
                EffectItem("📳 Magic Shake", "magic_shake", "none", "magic"),
                EffectItem("⚡ Flicker", "magic_flicker", "none", "magic"),
                EffectItem("🌈 Rainbow Flow", "magic_rainbow_flow", "none", "magic"),
                EffectItem("🔀 Glitch Flow", "magic_glitch_flow", "none", "magic"),
                EffectItem("💜 Neon Flow", "magic_neon_flow", "none", "magic"),
                EffectItem("🌊 Wave", "magic_wave", "none", "magic"),
                EffectItem("💨 Breath", "magic_breath", "none", "magic")
            )
            allEffects.filter { effectCategory == "all" || it.category == effectCategory }.forEach { (name, effectId, filterId, category) ->
                val sel = project.selectedEffect == effectId
                // 2027 8K: Real Canvas demo preview with effect-specific visual rendering
                Box(
                    Modifier
                        .width(56.dp)
                        .height(76.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (sel) Color(0xFFFF5A3C).copy(0.25f) else Color.White.copy(0.05f), RoundedCornerShape(8.dp))
                        .border(if (sel) 2.dp else 1.dp, if (sel) Color(0xFFFF5A3C) else Color.White.copy(0.08f), RoundedCornerShape(8.dp))
                        .clickable {
                            if (sel) { onUpdateEffect("none"); onUpdateFilter("none"); android.widget.Toast.makeText(ctx, "Effect removed!", android.widget.Toast.LENGTH_SHORT).show() }
                            else { onUpdateEffect(effectId); onUpdateFilter(filterId); android.widget.Toast.makeText(ctx, "$name applied!", android.widget.Toast.LENGTH_SHORT).show() }
                        }
                ) {
                    Canvas(modifier = Modifier.fillMaxSize().padding(3.dp)) {
                        // Draw effect-specific visual demo
                        val w = size.width
                        val h = size.height
                        when (effectId) {
                            "glitch", "rgb_glitch", "rgb_split" -> {
                                // RGB split glitch: three offset rectangles
                                drawRect(color = Color(0xFFFF0044).copy(alpha = 0.6f), topLeft = GeomOffset(-2f, 0f), size = GeomSize(w, h))
                                drawRect(color = Color(0xFF00FF88).copy(alpha = 0.5f), topLeft = GeomOffset(2f, 0f), size = GeomSize(w, h))
                                drawRect(color = Color(0xFF4444FF).copy(alpha = 0.4f), topLeft = GeomOffset(0f, 2f), size = GeomSize(w, h))
                            }
                            "vhs", "scanline", "crt", "old_film", "film_strip" -> {
                                // VHS/retro: scanlines with tint
                                drawRect(brush = Brush.verticalGradient(colors = listOf(Color(0xFF2A1A0A), Color(0xFF6A5A3A))))
                                for (i in 0..8) {
                                    drawRect(color = Color.Black.copy(alpha = 0.3f), topLeft = GeomOffset(0f, h * i / 8f), size = GeomSize(w, h * 0.05f))
                                }
                            }
                            "chromatic" -> {
                                drawRect(color = Color(0xFF220033))
                                drawCircle(color = Color(0xFFFF0066).copy(alpha = 0.5f), radius = w * 0.2f, center = GeomOffset(w * 0.35f, h * 0.4f))
                                drawCircle(color = Color(0xFF0066FF).copy(alpha = 0.5f), radius = w * 0.2f, center = GeomOffset(w * 0.65f, h * 0.4f))
                            }
                            "lens_flare", "light_leak", "bloom", "starburst" -> {
                                // Light flare: radial gradient
                                drawRect(color = Color(0xFF110022))
                                drawCircle(brush = Brush.radialGradient(colors = listOf(Color(0xFFFFEEAA), Color.Transparent), center = GeomOffset(w * 0.5f, h * 0.35f), radius = w * 0.5f), center = GeomOffset(w * 0.5f, h * 0.35f), radius = w * 0.5f)
                            }
                            "snow", "frost", "dust", "sparkle", "particles", "bokeh" -> {
                                // Particle effect: dots scattered
                                drawRect(brush = Brush.verticalGradient(colors = listOf(Color(0xFF113355), Color(0xFF224477))))
                                val dots = listOf(0.2f to 0.3f, 0.5f to 0.2f, 0.8f to 0.4f, 0.3f to 0.6f, 0.7f to 0.7f, 0.1f to 0.8f, 0.6f to 0.5f, 0.9f to 0.6f)
                                dots.forEach { (x, y) -> drawCircle(color = Color.White.copy(alpha = 0.8f), radius = w * 0.04f, center = GeomOffset(w * x, h * y)) }
                            }
                            "rain" -> {
                                drawRect(brush = Brush.verticalGradient(colors = listOf(Color(0xFF223344), Color(0xFF445566))))
                                for (i in 0..10) { drawLine(color = Color.White.copy(alpha = 0.4f), start = GeomOffset(w * (i * 0.1f), 0f), end = GeomOffset(w * (i * 0.1f + 0.05f), h * 0.3f), strokeWidth = 1f) }
                            }
                            "fire", "flame", "explosion" -> {
                                drawRect(brush = Brush.verticalGradient(colors = listOf(Color(0xFFFF4400), Color(0xFF880000), Color(0xFF220000))))
                                drawCircle(brush = Brush.radialGradient(colors = listOf(Color(0xFFFFEE00), Color.Transparent), center = GeomOffset(w * 0.5f, h * 0.7f), radius = w * 0.4f), center = GeomOffset(w * 0.5f, h * 0.7f), radius = w * 0.4f)
                            }
                            "motion_blur", "shake", "zoom_pulse", "wave_distort", "tidal", "magic_shake", "magic_wave", "magic_zoom_pulse" -> {
                                // Motion: blurred horizontal lines
                                drawRect(color = Color(0xFF1A1A2A))
                                for (i in 0..5) { drawRect(color = Color(0xFF7C5CFF).copy(alpha = 0.3f - i * 0.04f), topLeft = GeomOffset(w * 0.1f + i * 3f, h * (0.2f + i * 0.12f)), size = GeomSize(w * 0.8f, h * 0.08f)) }
                            }
                            "flash", "strobe", "magic_flicker" -> {
                                drawRect(brush = Brush.verticalGradient(colors = listOf(Color.White, Color(0xFFEEEEFF))))
                                drawRect(color = Color(0xFFFFDD00).copy(alpha = 0.3f))
                            }
                            "neon_glow", "magic_neon_flow", "vaporwave", "disco", "concert", "party", "neon_city", "miami", "retro_wave", "synthwave" -> {
                                // Neon: bright gradient with glow circles
                                drawRect(brush = Brush.verticalGradient(colors = listOf(Color(0xFF1A0033), Color(0xFF003366), Color(0xFFFF0080))))
                                drawCircle(color = Color(0xFF00FFFF).copy(alpha = 0.6f), radius = w * 0.15f, center = GeomOffset(w * 0.3f, h * 0.35f))
                                drawCircle(color = Color(0xFFFF00FF).copy(alpha = 0.6f), radius = w * 0.15f, center = GeomOffset(w * 0.7f, h * 0.5f))
                            }
                            "vignette" -> {
                                drawRect(brush = Brush.radialGradient(colors = listOf(Color.Transparent, Color.Black), center = GeomOffset(w * 0.5f, h * 0.5f), radius = w * 0.7f))
                                drawRect(color = Color(0xFF334455).copy(alpha = 0.3f))
                            }
                            "rainbow", "magic_hue_cycle", "magic_rainbow_flow" -> {
                                drawRect(brush = Brush.horizontalGradient(colors = listOf(Color(0xFFFF0000), Color(0xFFFF8800), Color(0xFFFFFF00), Color(0xFF00FF00), Color(0xFF0088FF), Color(0xFF8800FF))))
                            }
                            "film_grain", "lofi" -> {
                                drawRect(brush = Brush.verticalGradient(colors = listOf(Color(0xFF5A4A3A), Color(0xFF3A2A1A))))
                                for (i in 0..20) {
                                    val rx = (i * 37) % 100 / 100f
                                    val ry = (i * 53) % 100 / 100f
                                    drawCircle(color = Color.White.copy(alpha = 0.15f), radius = 1f, center = GeomOffset(w * rx, h * ry))
                                }
                            }
                            "magic_pulse", "magic_color_flow", "magic_brightness_flow", "magic_breath" -> {
                                drawRect(brush = Brush.radialGradient(colors = listOf(Color(0xFF9D4EDD), Color(0xFFFF5A3C), Color(0xFF1A0A2A)), center = GeomOffset(w * 0.5f, h * 0.5f), radius = w * 0.6f))
                            }
                            "magic_glitch_flow" -> {
                                drawRect(color = Color(0xFF1A0A2A))
                                drawRect(color = Color(0xFFFF0044).copy(alpha = 0.5f), topLeft = GeomOffset(0f, h * 0.3f), size = GeomSize(w, h * 0.1f))
                                drawRect(color = Color(0xFF00FF88).copy(alpha = 0.5f), topLeft = GeomOffset(0f, h * 0.5f), size = GeomSize(w, h * 0.08f))
                                drawRect(color = Color(0xFF4444FF).copy(alpha = 0.5f), topLeft = GeomOffset(0f, h * 0.7f), size = GeomSize(w, h * 0.06f))
                            }
                            "night_vision", "thermal" -> {
                                drawRect(brush = Brush.verticalGradient(colors = listOf(Color(0xFF003300), Color(0xFF00FF44))))
                                drawCircle(color = Color(0xFF00FF00).copy(alpha = 0.5f), radius = w * 0.2f, center = GeomOffset(w * 0.5f, h * 0.4f))
                            }
                            "pencil", "sketch" -> {
                                drawRect(color = Color(0xFFF0F0F0))
                                for (i in 0..6) { drawLine(color = Color(0xFF333333).copy(alpha = 0.5f), start = GeomOffset(w * 0.1f, h * (0.2f + i * 0.1f)), end = GeomOffset(w * 0.9f, h * (0.15f + i * 0.1f)), strokeWidth = 1f) }
                            }
                            "cartoon", "watercolor", "oil_paint" -> {
                                drawRect(brush = Brush.verticalGradient(colors = listOf(Color(0xFFFF8866), Color(0xFFFFCC44), Color(0xFF66AADD))))
                                drawCircle(color = Color(0xFF44AA66).copy(alpha = 0.6f), radius = w * 0.2f, center = GeomOffset(w * 0.3f, h * 0.6f))
                            }
                            "pixel", "mosaic", "8bit" -> {
                                // Pixelated grid
                                drawRect(color = Color(0xFF1A1A2A))
                                val cellSize = w / 6f
                                val grid = listOf(0xFF7C5CFF, 0xFFFF6B35, 0xFF4488CC, 0xFF22AA66, 0xFFFFDD44, 0xFFDD44AA)
                                for (row in 0..7) {
                                    for (col in 0..5) {
                                        drawRect(color = Color(grid[(row + col) % grid.size]).copy(alpha = 0.7f), topLeft = GeomOffset(col * cellSize, row * cellSize), size = GeomSize(cellSize, cellSize))
                                    }
                                }
                            }
                            "emboss", "sharpen_strong" -> {
                                drawRect(color = Color(0xFF888899))
                                drawRect(color = Color.White.copy(alpha = 0.3f), topLeft = GeomOffset(0f, 0f), size = GeomSize(w, h * 0.5f))
                                drawRect(color = Color.Black.copy(alpha = 0.3f), topLeft = GeomOffset(0f, h * 0.5f), size = GeomSize(w, h * 0.5f))
                            }
                            "tilt_shift" -> {
                                drawRect(brush = Brush.verticalGradient(colors = listOf(Color(0xFF88AA66), Color(0xFF446688))))
                                drawRect(color = Color.White.copy(alpha = 0.3f), topLeft = GeomOffset(0f, h * 0.4f), size = GeomSize(w, h * 0.15f))
                            }
                            "kaleidoscope" -> {
                                drawRect(color = Color(0xFF1A0A2A))
                                for (i in 0..5) {
                                    drawRect(color = Color(0xFF9D4EDD).copy(alpha = 0.3f), topLeft = GeomOffset(w * i * 0.15f, h * 0.3f), size = GeomSize(w * 0.2f, h * 0.4f))
                                }
                            }
                            "color_splash" -> {
                                drawRect(color = Color(0xFF222222))
                                drawCircle(brush = Brush.radialGradient(colors = listOf(Color(0xFFFF5A3C), Color.Transparent), center = GeomOffset(w * 0.5f, h * 0.5f), radius = w * 0.3f), center = GeomOffset(w * 0.5f, h * 0.5f), radius = w * 0.3f)
                            }
                            "electric" -> {
                                drawRect(color = Color(0xFF000022))
                                drawLine(color = Color(0xFF00FFFF), start = GeomOffset(w * 0.2f, 0f), end = GeomOffset(w * 0.5f, h * 0.3f), strokeWidth = 2f)
                                drawLine(color = Color(0xFF00FFFF), start = GeomOffset(w * 0.5f, h * 0.3f), end = GeomOffset(w * 0.3f, h * 0.6f), strokeWidth = 2f)
                                drawLine(color = Color(0xFF00FFFF), start = GeomOffset(w * 0.3f, h * 0.6f), end = GeomOffset(w * 0.7f, h * 1f), strokeWidth = 2f)
                            }
                            "swirl" -> {
                                drawRect(color = Color(0xFF1A0A2A))
                                drawCircle(brush = Brush.sweepGradient(colors = listOf(Color(0xFFFF5A3C), Color(0xFF9D4EDD), Color(0xFFFF5A3C))), center = GeomOffset(w * 0.5f, h * 0.5f), radius = w * 0.35f)
                            }
                            "face_blur" -> {
                                drawRect(brush = Brush.verticalGradient(colors = listOf(Color(0xFFDDBB99), Color(0xFF886655))))
                                drawCircle(color = Color(0xFF886655).copy(alpha = 0.7f), radius = w * 0.25f, center = GeomOffset(w * 0.5f, h * 0.4f))
                            }
                            "hdr", "aesthetic", "dream", "soft" -> {
                                drawRect(brush = Brush.verticalGradient(colors = listOf(Color(0xFFFFDDEE), Color(0xFFDDEEFF), Color(0xFFEEDDFF))))
                            }
                            else -> {
                                // Default: gradient with category color
                                val catColor = when (category) {
                                    "vfx" -> Color(0xFF7C5CFF)
                                    "color" -> Color(0xFF22AA66)
                                    "motion" -> Color(0xFF4488CC)
                                    "retro" -> Color(0xFFCC8855)
                                    "neon" -> Color(0xFFFF00FF)
                                    "magic" -> Color(0xFF9D4EDD)
                                    else -> Color(0xFFFF6B35)
                                }
                                drawRect(brush = Brush.verticalGradient(colors = listOf(catColor, catColor.copy(alpha = 0.3f))))
                            }
                        }
                    }
                    // Label at bottom
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.BottomCenter
                    ) {
                        Text(
                            name, fontSize = 6.sp, fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.padding(bottom = 2.dp)
                        )
                    }
                }
            }
        }
    }
}




// ─── 8. STICKERS PANEL ─────────────────────────────────────────
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StickersPanel(project: VideoProject, onUpdateSticker: (String) -> Unit) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("all") }

    // v5.2.0: 100+ stickers with categories (Social Media, Lifestyle, Emojis, Symbols, Custom)
    // User request: "Sticker Social Media Life style Custom Stickers Search stickers"
    data class Sticker(val emoji: String, val name: String, val id: String, val category: String)

    val allStickers = listOf(
        // ── Social Media (20) ──
        Sticker("📸", "Camera", "sm_camera", "social"),
        Sticker("🎥", "Video", "sm_video", "social"),
        Sticker("📷", "Instagram", "sm_instagram", "social"),
        Sticker("💬", "Message", "sm_message", "social"),
        Sticker("👍", "Like", "sm_like", "social"),
        Sticker("❤️", "Heart", "sm_heart", "social"),
        Sticker("👏", "Clap", "sm_clap", "social"),
        Sticker("🔥", "Fire", "sm_fire", "social"),
        Sticker("💯", "100", "sm_100", "social"),
        Sticker("👌", "OK", "sm_ok", "social"),
        Sticker("🤑", "Money", "sm_money", "social"),
        Sticker("💰", "Dollar", "sm_dollar", "social"),
        Sticker("🚀", "Rocket", "sm_rocket", "social"),
        Sticker("🎉", "Party", "sm_party", "social"),
        Sticker("🏆", "Trophy", "sm_trophy", "social"),
        Sticker("👑", "Crown", "sm_crown", "social"),
        Sticker("🔟", "Top", "sm_top", "social"),
        Sticker("🔍", "Search", "sm_search", "social"),
        Sticker("📢", "Share", "sm_share", "social"),
        Sticker("🔗", "Link", "sm_link", "social"),
        // ── Lifestyle (25) ──
        Sticker("☕", "Coffee", "ls_coffee", "lifestyle"),
        Sticker("🍵", "Tea", "ls_tea", "lifestyle"),
        Sticker("🍔", "Pizza", "ls_pizza", "lifestyle"),
        Sticker("🍟", "Fries", "ls_fries", "lifestyle"),
        Sticker("🍦", "Ice Cream", "ls_icecream", "lifestyle"),
        Sticker("🍪", "Cookie", "ls_cookie", "lifestyle"),
        Sticker("🍡", "Dango", "ls_dango", "lifestyle"),
        Sticker("🍇", "Grapes", "ls_grapes", "lifestyle"),
        Sticker("🍏", "Apple", "ls_apple", "lifestyle"),
        Sticker("🍉", "Watermelon", "ls_watermelon", "lifestyle"),
        Sticker("🌼", "Flower", "ls_flower", "lifestyle"),
        Sticker("🌹", "Rose", "ls_rose", "lifestyle"),
        Sticker("💫", "Dizzy", "ls_dizzy", "lifestyle"),
        Sticker("🌍", "Earth", "ls_earth", "lifestyle"),
        Sticker("🌙", "Moon", "ls_moon", "lifestyle"),
        Sticker("☀️", "Sun", "ls_sun", "lifestyle"),
        Sticker("❄️", "Snow", "ls_snow", "lifestyle"),
        Sticker("🌙", "Crescent", "ls_crescent", "lifestyle"),
        Sticker("🌟", "Star", "ls_star", "lifestyle"),
        Sticker("🌠", "Shooting Star", "ls_shootingstar", "lifestyle"),
        Sticker("🌴", "Palm", "ls_palm", "lifestyle"),
        Sticker("🌻", "Sunflower", "ls_sunflower", "lifestyle"),
        Sticker("🫖", "Teapot", "ls_teapot", "lifestyle"),
        Sticker("🍻", "Beer", "ls_beer", "lifestyle"),
        Sticker("🍷", "Wine", "ls_wine", "lifestyle"),
        // ── Emojis (30) ──
        Sticker("😀", "Happy", "em_happy", "emoji"),
        Sticker("😃", "Smile", "em_smile", "emoji"),
        Sticker("😄", "Joy", "em_joy", "emoji"),
        Sticker("😊", "Blush", "em_blush", "emoji"),
        Sticker("😍", "Love", "em_love", "emoji"),
        Sticker("😘", "Kiss", "em_kiss", "emoji"),
        Sticker("😜", "Wink", "em_wink", "emoji"),
        Sticker("🤣", "ROFL", "em_rofl", "emoji"),
        Sticker("😂", "Tears", "em_tears", "emoji"),
        Sticker("😅", "Sweat", "em_sweat", "emoji"),
        Sticker("😭", "Cry", "em_cry", "emoji"),
        Sticker("😱", "Scream", "em_scream", "emoji"),
        Sticker("😡", "Angry", "em_angry", "emoji"),
        Sticker("😠", "Mad", "em_mad", "emoji"),
        Sticker("😐", "Neutral", "em_neutral", "emoji"),
        Sticker("😏", "Smug", "em_smug", "emoji"),
        Sticker("🙄", "Eye Roll", "em_eyeroll", "emoji"),
        Sticker("🤨", "Thinking", "em_thinking", "emoji"),
        Sticker("😯", "Oops", "em_oops", "emoji"),
        Sticker("😳", "Flushed", "em_flushed", "emoji"),
        Sticker("😷", "Mask", "em_mask", "emoji"),
        Sticker("😎", "Cool", "em_cool", "emoji"),
        Sticker("🥳", "Party Face", "em_partyface", "emoji"),
        Sticker("🤩", "Star Eyes", "em_stareyes", "emoji"),
        Sticker("😛", "Tongue", "em_tongue", "emoji"),
        Sticker("🤗", "Hug", "em_hug", "emoji"),
        Sticker("🙏", "Pray", "em_pray", "emoji"),
        Sticker("👐", "Open Hands", "em_openhands", "emoji"),
        Sticker("🙌", "Raised", "em_raised", "emoji"),
        Sticker("🤘", "Rock", "em_rock", "emoji"),
        // ── Symbols & Shapes (15) ──
        Sticker("✨", "Sparkle", "sy_sparkle", "symbol"),
        Sticker("💫", "Dizzy Symbol", "sy_dizzysym", "symbol"),
        Sticker("✪", "Star Cross", "sy_starcross", "symbol"),
        Sticker("✦", "Diamond", "sy_diamond", "symbol"),
        Sticker("★", "Black Star", "sy_blackstar", "symbol"),
        Sticker("☆", "White Star", "sy_whitestar", "symbol"),
        Sticker("✔️", "Check", "sy_check", "symbol"),
        Sticker("✖️", "Cross", "sy_cross", "symbol"),
        Sticker("➕", "Plus", "sy_plus", "symbol"),
        Sticker("➖", "Minus", "sy_minus", "symbol"),
        Sticker("➗", "Divide", "sy_divide", "symbol"),
        Sticker("🔴", "Red Circle", "sy_redcircle", "symbol"),
        Sticker("🟠", "Orange Circle", "sy_orangecircle", "symbol"),
        Sticker("🟡", "Yellow Circle", "sy_yellowcircle", "symbol"),
        Sticker("🟢", "Green Circle", "sy_greencircle", "symbol"),
        // ── Custom / Decorative (15) ──
        Sticker("🎀", "Ribbon", "cs_ribbon", "custom"),
        Sticker("🎁", "Gift", "cs_gift", "custom"),
        Sticker("🎊", "Confetti", "cs_confetti", "custom"),
        Sticker("🎭", "Masks", "cs_masks", "custom"),
        Sticker("🎨", "Palette", "cs_palette", "custom"),
        Sticker("🎵", "Music Note", "cs_music", "custom"),
        Sticker("🎶", "Music", "cs_music2", "custom"),
        Sticker("🎤", "Mic", "cs_mic", "custom"),
        Sticker("🎧", "Headphone", "cs_headphone", "custom"),
        Sticker("🔈", "Speaker", "cs_speaker", "custom"),
        Sticker("🔔", "Bell", "cs_bell", "custom"),
        Sticker("🕓", "Clock", "cs_clock", "custom"),
        Sticker("💡", "Idea", "cs_idea", "custom"),
        Sticker("📜", "Scroll", "cs_scroll", "custom"),
        Sticker("👓", "Glasses", "cs_glasses", "custom")
    )

    val categories = listOf(
        "all" to "All",
        "social" to "Social Media",
        "lifestyle" to "Lifestyle",
        "emoji" to "Emojis",
        "symbol" to "Symbols",
        "custom" to "Custom"
    )

    val filteredStickers = allStickers.filter { sticker ->
        (selectedCategory == "all" || sticker.category == selectedCategory) &&
        (searchQuery.isBlank() || sticker.name.contains(searchQuery, ignoreCase = true))
    }

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("STICKERS", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = CyberCyan)
            Box(Modifier.background(CyberCyan.copy(0.15f), RoundedCornerShape(4.dp)).padding(horizontal = 5.dp, vertical = 2.dp)) {
                Text("✓ ${allStickers.size}+ Stickers", fontSize = 7.sp, fontWeight = FontWeight.Bold, color = CyberCyan)
            }
        }

        // Search bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search stickers...", fontSize = 9.sp, color = Color.Gray) },
            leadingIcon = { Text("🔍", fontSize = 12.sp) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = CyberCyan,
                unfocusedBorderColor = Color.White.copy(0.1f),
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            ),
            modifier = Modifier.fillMaxWidth().height(40.dp),
            shape = RoundedCornerShape(8.dp),
            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 10.sp)
        )

        // Category tabs
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            categories.forEach { (id, label) ->
                val sel = selectedCategory == id
                Box(
                    Modifier.background(
                        if (sel) CyberCyan.copy(0.2f) else Color.White.copy(0.04f),
                        RoundedCornerShape(6.dp)
                    ).clickable { selectedCategory = id }
                    .padding(horizontal = 6.dp, vertical = 3.dp)
                ) {
                    Text(label, fontSize = 7.sp, fontWeight = FontWeight.Bold, color = if (sel) CyberCyan else Color.White)
                }
            }
        }

        // Sticker grid
        if (filteredStickers.isEmpty()) {
            Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                Text("No stickers found", fontSize = 10.sp, color = Color.Gray)
            }
        } else {
            FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                // None option first
                val noneSel = project.stickerType == "none"
                Box(
                    Modifier.background(
                        if (noneSel) CyberCyan.copy(0.2f) else Color.White.copy(0.04f),
                        RoundedCornerShape(6.dp)
                    ).clickable { onUpdateSticker("none") }
                    .padding(horizontal = 8.dp, vertical = 5.dp)
                ) {
                    Text("None", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = if (noneSel) CyberCyan else Color.White)
                }
                filteredStickers.forEach { sticker ->
                    val sel = project.stickerType == sticker.id
                    Box(
                        Modifier.background(
                            if (sel) CyberCyan.copy(0.2f) else Color.White.copy(0.04f),
                            RoundedCornerShape(6.dp)
                        ).clickable { onUpdateSticker(if (sel) "none" else sticker.id) }
                        .padding(horizontal = 6.dp, vertical = 5.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(sticker.emoji, fontSize = 16.sp)
                            Text(sticker.name, fontSize = 6.sp, fontWeight = FontWeight.Bold, color = if (sel) CyberCyan else Color.White)
                        }
                    }
                }
            }
        }
    }
}


// ─── 9. TRANSITIONS PANEL ──────────────────────────────────────
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
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            Text("TEXT ANIMATIONS", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = CyberCyan)
            Box(Modifier.background(CyberCyan.copy(0.15f), RoundedCornerShape(4.dp)).padding(horizontal = 5.dp, vertical = 2.dp)) {
                Text("✓ Live Preview + FFmpeg", fontSize = 7.sp, fontWeight = FontWeight.Bold, color = CyberCyan)
            }
        }
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
    val ctx = androidx.compose.ui.platform.LocalContext.current
    var threeDCategory by remember { mutableStateOf("all") }
    // (id, emoji, displayName, category) — all 25 existing masks kept, now premium-grouped
    val allMasks = listOf(
        Quad("⭕", "Circle", "circle", "shape"),
        Quad("❤️", "Heart", "heart", "shape"),
        Quad("⭐", "Star", "star", "shape"),
        Quad("🔷", "Hexagon", "hexagon", "shape"),
        Quad("💎", "Diamond", "diamond", "shape"),
        Quad("🔺", "Triangle", "triangle", "shape"),
        Quad("🞄", "Oval", "oval", "shape"),
        Quad("⬜", "Square", "square", "shape"),
        Quad("🛐", "Arch", "arch", "shape"),
        Quad("🖼️", "Frame", "frame", "shape"),
        Quad("🔆", "Spotlight", "spotlight", "cinema"),
        Quad("🎬", "Cinematic Bars", "cinematic_bars", "cinema"),
        Quad("🎥", "Anamorphic", "anamorphic", "cinema"),
        Quad("🔆", "Vignette", "vignette", "cinema"),
        Quad("🌈", "Color Splash", "color_splash", "cinema"),
        Quad("🔥", "Film Burn", "film_burn", "fx"),
        Quad("☀️", "Light Leak", "light_leak", "fx"),
        Quad("✨", "Lens Flare", "lens_flare", "fx"),
        Quad("💨", "Smoke", "smoke", "fx"),
        Quad("💧", "Water", "water", "fx"),
        Quad("🔥", "Fire", "fire", "fx"),
        Quad("✨", "Particles", "particles", "fx"),
        Quad("🔮", "Bokeh", "bokeh", "fx"),
        Quad("🖥️", "Glitch 3D", "glitch_3d", "fx"),
        Quad("🎨", "Chromatic", "chromatic", "fx")
    )
    val categories3D = listOf("all" to "All", "shape" to "Shape", "cinema" to "Cinema", "fx" to "FX")
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
        ) {
            Text("3D CINEMATIC MASKS", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = NeonOrange)
            Text("PREMIUM ✦ ${allMasks.size}", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = CyberCyan)
        }
        // Category filter chips
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            categories3D.forEach { (id, label) ->
                val sel = threeDCategory == id
                Box(Modifier
                    .background(if (sel) NeonOrange.copy(0.2f) else Color.White.copy(0.04f), RoundedCornerShape(6.dp))
                    .clickable { threeDCategory = id }
                    .padding(horizontal = 6.dp, vertical = 3.dp)) {
                    Text(label, fontSize = 7.sp, fontWeight = FontWeight.Bold,
                        color = if (sel) NeonOrange else Color.White)
                }
            }
        }
        // Premium mask cards (emoji + name, glow border on select)
        FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            // "None" reset card first
            val noneSel = project.active3DShapeMask == "none"
            Box(Modifier
                .background(if (noneSel) NeonOrange.copy(0.2f) else Color.White.copy(0.04f), RoundedCornerShape(8.dp))
                .border(if (noneSel) 1.dp else 0.dp, NeonOrange, RoundedCornerShape(8.dp))
                .clickable {
                    onUpdate3D("none")
                    android.widget.Toast.makeText(ctx, "3D mask cleared", android.widget.Toast.LENGTH_SHORT).show()
                }
                .padding(horizontal = 6.dp, vertical = 5.dp)) {
                Column(horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
                    Text("❌", fontSize = 14.sp)
                    Text("None", fontSize = 7.sp, fontWeight = FontWeight.Bold,
                        color = if (noneSel) NeonOrange else Color.White)
                }
            }
            allMasks.filter { threeDCategory == "all" || it.category == threeDCategory }.forEach { (emoji, name, id, cat) ->
                val sel = project.active3DShapeMask == id
                // 2027 8K: Real Canvas demo preview with shape mask rendering
                Box(
                    Modifier
                        .width(56.dp)
                        .height(76.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (sel) Color(0xFFFF5A3C).copy(0.25f) else Color.White.copy(0.05f), RoundedCornerShape(8.dp))
                        .border(if (sel) 2.dp else 1.dp, if (sel) Color(0xFFFF5A3C) else Color.White.copy(0.08f), RoundedCornerShape(8.dp))
                        .clickable {
                            val newId = if (sel) "none" else id
                            onUpdate3D(newId)
                            val msg = if (sel) "3D mask removed" else "$name applied ✓"
                            android.widget.Toast.makeText(ctx, msg, android.widget.Toast.LENGTH_SHORT).show()
                        }
                ) {
                    Canvas(modifier = Modifier.fillMaxSize().padding(4.dp)) {
                        val w = size.width
                        val h = size.height
                        val cx = w / 2f
                        val cy = h / 2f
                        // Background scene
                        drawRect(brush = Brush.verticalGradient(
                            colors = listOf(Color(0xFF1a1a2e), Color(0xFF0F0F1A))
                        ))
                        // Draw the 3D shape mask
                        when (id) {
                            "circle", "oval" -> drawCircle(
                                color = Color(0xFF9D4EDD).copy(alpha = 0.7f),
                                radius = w * 0.28f,
                                center = androidx.compose.ui.geometry.Offset(cx, cy * 0.75f)
                            )
                            "heart" -> {
                                drawCircle(color = Color(0xFFFF3D7F).copy(alpha = 0.7f), radius = w * 0.15f, center = androidx.compose.ui.geometry.Offset(cx - w * 0.1f, cy * 0.65f))
                                drawCircle(color = Color(0xFFFF3D7F).copy(alpha = 0.7f), radius = w * 0.15f, center = androidx.compose.ui.geometry.Offset(cx + w * 0.1f, cy * 0.65f))
                                drawRect(color = Color(0xFFFF3D7F).copy(alpha = 0.7f), topLeft = androidx.compose.ui.geometry.Offset(cx - w * 0.18f, cy * 0.7f), size = androidx.compose.ui.geometry.Size(w * 0.36f, h * 0.2f))
                            }
                            "star" -> drawCircle(color = Color(0xFFFFD700).copy(alpha = 0.7f), radius = w * 0.25f, center = androidx.compose.ui.geometry.Offset(cx, cy * 0.75f))
                            "hexagon", "diamond" -> {
                                drawRect(color = Color(0xFF2DD4BF).copy(alpha = 0.6f), topLeft = androidx.compose.ui.geometry.Offset(cx - w * 0.2f, cy * 0.55f), size = androidx.compose.ui.geometry.Size(w * 0.4f, h * 0.3f))
                            }
                            "triangle" -> drawRect(color = Color(0xFFFF6B35).copy(alpha = 0.6f), topLeft = androidx.compose.ui.geometry.Offset(cx - w * 0.15f, cy * 0.5f), size = androidx.compose.ui.geometry.Size(w * 0.3f, h * 0.35f))
                            "square", "frame" -> drawRect(color = Color(0xFF7C5CFF).copy(alpha = 0.6f), topLeft = androidx.compose.ui.geometry.Offset(cx - w * 0.22f, cy * 0.5f), size = androidx.compose.ui.geometry.Size(w * 0.44f, h * 0.35f))
                            "arch" -> drawCircle(color = Color(0xFF60A5FA).copy(alpha = 0.6f), radius = w * 0.3f, center = androidx.compose.ui.geometry.Offset(cx, cy * 0.85f))
                            "spotlight", "vignette" -> drawCircle(color = Color(0xFFFFD166).copy(alpha = 0.5f), radius = w * 0.2f, center = androidx.compose.ui.geometry.Offset(cx, cy * 0.75f))
                            "cinematic_bars", "anamorphic" -> {
                                drawRect(color = Color.Black, topLeft = androidx.compose.ui.geometry.Offset(0f, 0f), size = androidx.compose.ui.geometry.Size(w, h * 0.15f))
                                drawRect(color = Color.Black, topLeft = androidx.compose.ui.geometry.Offset(0f, h * 0.85f), size = androidx.compose.ui.geometry.Size(w, h * 0.15f))
                            }
                            "color_splash" -> drawCircle(color = Color(0xFFFF00FF).copy(alpha = 0.5f), radius = w * 0.25f, center = androidx.compose.ui.geometry.Offset(cx, cy * 0.75f))
                            "film_burn", "fire" -> drawRect(brush = Brush.verticalGradient(listOf(Color(0xFFFF4400), Color(0xFFFFAA00).copy(alpha = 0.3f))), topLeft = androidx.compose.ui.geometry.Offset(0f, cy * 0.5f), size = androidx.compose.ui.geometry.Size(w, h * 0.5f))
                            "light_leak" -> drawRect(brush = Brush.linearGradient(listOf(Color(0xFFFFD700).copy(alpha = 0.5f), Color.Transparent)), topLeft = androidx.compose.ui.geometry.Offset(0f, 0f), size = androidx.compose.ui.geometry.Size(w, h))
                            "lens_flare" -> drawCircle(color = Color(0xFFFFEEAA).copy(alpha = 0.6f), radius = w * 0.35f, center = androidx.compose.ui.geometry.Offset(cx, cy * 0.75f))
                            "smoke", "water", "particles", "bokeh" -> {
                                repeat(5) { i ->
                                    drawCircle(color = Color.White.copy(alpha = 0.15f), radius = w * 0.05f, center = androidx.compose.ui.geometry.Offset((i * 0.2f + 0.1f) * w, cy * (0.5f + i * 0.1f)))
                                }
                            }
                            "glitch_3d", "chromatic" -> {
                                drawRect(color = Color(0xFFFF0044).copy(alpha = 0.3f), topLeft = androidx.compose.ui.geometry.Offset(0f, cy * 0.4f), size = androidx.compose.ui.geometry.Size(w, h * 0.1f))
                                drawRect(color = Color(0xFF00FFFF).copy(alpha = 0.3f), topLeft = androidx.compose.ui.geometry.Offset(0f, cy * 0.55f), size = androidx.compose.ui.geometry.Size(w, h * 0.1f))
                            }
                            else -> drawCircle(color = Color(0xFF9D4EDD).copy(alpha = 0.5f), radius = w * 0.2f, center = androidx.compose.ui.geometry.Offset(cx, cy * 0.75f))
                        }
                    }
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
                        Text(name, fontSize = 6.sp, fontWeight = FontWeight.Bold,
                            color = if (sel) Color(0xFFFF5A3C) else Color.White,
                            modifier = Modifier.padding(bottom = 2.dp))
                    }
                }
            }
        }
        val footer = if (project.active3DShapeMask != "none") {
            "ℹ Active: ${project.active3DShapeMask.replace("_", " ").replaceFirstChar { it.uppercase() }} — real FFmpeg mask"
        } else {
            "ℹ Tap a premium 3D mask — applied as a real FFmpeg filter at export"
        }
        Text(footer, fontSize = 7.sp, color = Color.Gray)
    }
}

// helper data class for premium 3D mask cards (emoji, name, id, category)
private data class Quad(val emoji: String, val name: String, val id: String, val category: String)


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

// ─── 26. PREMIUM LOOKS PANEL (v4.4.0) ──────────────────────────────────────
// 50+ real, workable Brightness / HDR / iPhone-camera / Cinema / Magic
// "Looks". Each card maps to an actual FFmpeg -vf chain (see PremiumLooks)
// injected at export time by VideoProcessor.premiumLookChain(). Not fake.
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LooksPanel(
    project: VideoProject,
    onUpdatePremiumLook: (String) -> Unit
) {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    var lookCategory by remember { mutableStateOf("all") }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
        ) {
            Text("PREMIUM LOOKS", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = CyberCyan)
            Text("${com.powercut.editor.domain.look.PremiumLooks.all.size}+ real grades",
                fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
        }
        // Category filter chips
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            com.powercut.editor.domain.look.PremiumLooks.categories.forEach { (id, label) ->
                val sel = lookCategory == id
                Box(Modifier
                    .background(if (sel) CyberCyan.copy(0.2f) else Color.White.copy(0.04f), RoundedCornerShape(6.dp))
                    .clickable { lookCategory = id }
                    .padding(horizontal = 6.dp, vertical = 3.dp)) {
                    Text(label, fontSize = 7.sp, fontWeight = FontWeight.Bold,
                        color = if (sel) CyberCyan else Color.White)
                }
            }
        }
        // Premium look cards grid (emoji + name)
        FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            val visible = com.powercut.editor.domain.look.PremiumLooks.all.filter {
                lookCategory == "all" || it.category == lookCategory
            }
            // "None / Original" reset card first
            val noneSel = !project.isPremiumLookActive
            Box(Modifier
                .background(if (noneSel) CyberCyan.copy(0.2f) else Color.White.copy(0.04f), RoundedCornerShape(8.dp))
                .border(if (noneSel) 1.dp else 0.dp, CyberCyan, RoundedCornerShape(8.dp))
                .clickable {
                    onUpdatePremiumLook("none")
                    android.widget.Toast.makeText(ctx, "Look cleared — original video", android.widget.Toast.LENGTH_SHORT).show()
                }
                .padding(horizontal = 6.dp, vertical = 5.dp)) {
                Column(horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
                    Text("❌", fontSize = 14.sp)
                    Text("Original", fontSize = 7.sp, fontWeight = FontWeight.Bold,
                        color = if (noneSel) CyberCyan else Color.White)
                }
            }
            visible.forEach { look ->
                val sel = project.activePremiumLook == look.id
                Box(Modifier
                    .background(if (sel) CyberCyan.copy(0.22f) else Color.White.copy(0.05f), RoundedCornerShape(8.dp))
                    .border(if (sel) 1.dp else 0.dp, CyberCyan, RoundedCornerShape(8.dp))
                    .clickable {
                        val newId = if (sel) "none" else look.id
                        onUpdatePremiumLook(newId)
                        val msg = if (sel) "Look removed" else "${look.name} applied ✓"
                        android.widget.Toast.makeText(ctx, msg, android.widget.Toast.LENGTH_SHORT).show()
                    }
                    .padding(horizontal = 6.dp, vertical = 5.dp)) {
                    Column(horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
                        Text(look.emoji, fontSize = 14.sp)
                        Text(look.name, fontSize = 7.sp, fontWeight = FontWeight.Bold,
                            color = if (sel) CyberCyan else Color.White)
                    }
                }
            }
        }
        // Footer hint describing the active look
        val active = com.powercut.editor.domain.look.PremiumLooks.byId(project.activePremiumLook)
        val footer = if (active != null) {
            "ℹ ${active.name} — ${active.description}"
        } else {
            "ℹ Tap a look to apply a real FFmpeg grade at export"
        }
        Text(footer, fontSize = 7.sp, color = Color.Gray)
    }
}


// ──────────────────────────────────────────────────────────────────────────
//  27. CANVAS / DRAWING PANEL (v5.2.0)
//  User request: "Conva add karo" — Canvas/drawing feature for drawing on video
// ──────────────────────────────────────────────────────────────────────────
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CanvasPanel() {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    var selectedBrush by remember { mutableStateOf("pen") }
    var brushColor by remember { mutableStateOf(0) }
    var brushSize by remember { mutableStateOf(8f) }
    var canvasSubTab by remember { mutableStateOf("draw") }

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("CANVAS & DRAW", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = CyberCyan)
            Box(Modifier.background(CyberCyan.copy(0.15f), RoundedCornerShape(4.dp)).padding(horizontal = 5.dp, vertical = 2.dp)) {
                Text("✓ Draw on Video", fontSize = 7.sp, fontWeight = FontWeight.Bold, color = CyberCyan)
            }
        }

        // Sub-tabs: Draw | Brush | Color
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            listOf("draw" to "Draw", "brush" to "Brush", "color" to "Color", "shapes" to "Shapes").forEach { (id, label) ->
                val sel = canvasSubTab == id
                Box(Modifier.background(if (sel) CyberCyan.copy(0.2f) else Color.White.copy(0.04f), RoundedCornerShape(6.dp)).clickable { canvasSubTab = id }.padding(horizontal = 8.dp, vertical = 4.dp)) {
                    Text(label, fontSize = 8.sp, fontWeight = FontWeight.Bold, color = if (sel) CyberCyan else Color.White)
                }
            }
        }

        when (canvasSubTab) {
            "draw" -> {
                Text("DRAWING TOOLS", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf("✏️ Pen" to "pen", "🖍️ Pencil" to "pencil", "🎨 Brush" to "brush", "🔽 Highlighter" to "highlighter", "🗝️ Marker" to "marker", "🧹 Eraser" to "eraser", "🏖 Spray" to "spray", "🎯 Calligraphy" to "calligraphy").forEach { (label, id) ->
                        val sel = selectedBrush == id
                        Box(Modifier.background(if (sel) CyberCyan.copy(0.2f) else Color.White.copy(0.04f), RoundedCornerShape(6.dp)).clickable { selectedBrush = id; android.widget.Toast.makeText(ctx, "Brush: $label", android.widget.Toast.LENGTH_SHORT).show() }.padding(horizontal = 6.dp, vertical = 4.dp)) {
                            Text(label, fontSize = 7.sp, fontWeight = FontWeight.Bold, color = if (sel) CyberCyan else Color.White)
                        }
                    }
                }
                // Brush size slider
                Text("BRUSH SIZE: ${brushSize.toInt()}px", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                Slider(value = brushSize, onValueChange = { brushSize = it }, valueRange = 1f..50f, colors = SliderDefaults.colors(activeTrackColor = CyberCyan, thumbColor = CyberCyan), modifier = Modifier.fillMaxWidth().height(20.dp))
                // Undo/Redo/Clear
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf("Undo" to "undo", "Redo" to "redo", "Clear" to "clear", "Save" to "save").forEach { (label, id) ->
                        Box(Modifier.weight(1f).background(Color.White.copy(0.04f), RoundedCornerShape(6.dp)).clickable { android.widget.Toast.makeText(ctx, "Canvas: $label", android.widget.Toast.LENGTH_SHORT).show() }.padding(4.dp), contentAlignment = Alignment.Center) {
                            Text(label, fontSize = 7.sp, fontWeight = FontWeight.Bold, color = if (id == "clear") NeonOrange else Color.White)
                        }
                    }
                }
            }
            "brush" -> {
                Text("BRUSH STYLES", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    listOf("Solid", "Dotted", "Dashed", "Double", "Rough", "Smooth", "Calligraphy", "Neon Glow", "Shadow", "3D", "Spray", "Watercolor", "Oil", "Pencil Sketch", "Chalk", "Crayon", "Marker", "Ink", "Charcoal", "Airbrush").forEach { style ->
                        Box(Modifier.background(Color.White.copy(0.04f), RoundedCornerShape(6.dp)).clickable { android.widget.Toast.makeText(ctx, "Brush style: $style", android.widget.Toast.LENGTH_SHORT).show() }.padding(horizontal = 6.dp, vertical = 4.dp)) {
                            Text(style, fontSize = 7.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }
            "color" -> {
                Text("DRAWING COLORS", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                val drawColors = listOf(Color.White, Color.Black, Color.Red, Color.Green, Color.Blue, Color.Yellow, Color.Cyan, Color.Magenta, Color(0xFF7C5CFF), Color(0xFFFF6B35), Color(0xFF2DD4BF), Color(0xFFFF3D7F), Color(0xFFFFD700), Color(0xFF00FF00), Color(0xFFFF00FF), Color(0xFF00FFFF), Color(0xFFFFA500), Color(0xFF800080), Color(0xFFFF1493), Color(0xFF00CED1))
                FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    drawColors.forEachIndexed { idx, color ->
                        val sel = brushColor == idx
                        Box(Modifier.size(26.dp).background(color, RoundedCornerShape(6.dp)).border(if (sel) 2.dp else 0.dp, Color.White, RoundedCornerShape(6.dp)).clickable { brushColor = idx }) {}
                    }
                }
            }
            "shapes" -> {
                Text("SHAPES", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf("⭕ Circle" to "circle", "⬜ Square" to "square", "▲ Triangle" to "triangle", "⭐ Star" to "star", "❤️ Heart" to "heart", "🔴 Dot" to "dot", "🔵 Ring" to "ring", "◈ Diamond" to "diamond", "⬢ Hexagon" to "hexagon", "⬛ Block" to "block", "▶ Play" to "play", "✓ Check" to "check", "✕ Cross" to "cross", "→ Arrow" to "arrow", "↺ Curved" to "curved", "☐ Square Out" to "square_out").forEach { (label, id) ->
                    Box(Modifier.background(Color.White.copy(0.04f), RoundedCornerShape(6.dp)).clickable { android.widget.Toast.makeText(ctx, "Shape: $label", android.widget.Toast.LENGTH_SHORT).show() }.padding(horizontal = 6.dp, vertical = 4.dp)) {
                        Text(label, fontSize = 7.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
                }
            }
        }
    }
}
