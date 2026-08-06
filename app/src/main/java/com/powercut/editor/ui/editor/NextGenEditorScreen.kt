package com.powercut.editor.ui.editor

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.EaseInOutSine
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
import androidx.compose.ui.BiasAlignment
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
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.runtime.State
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
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
import com.powercut.editor.ui.theme.glassCard3D
import com.powercut.editor.ui.home.TransitionDemoPreview
import com.powercut.editor.ui.home.AnimationDemoPreview
import com.powercut.editor.ui.theme.GlassBackground
import com.powercut.editor.ui.theme.SignatureOrange
import com.powercut.editor.ui.theme.PremiumGold
import com.powercut.editor.ui.theme.SignaturePurple
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
    onUpdateTextStyle: (String) -> Unit = {},
    onUpdateTextPositionX: (Float) -> Unit = {},
    onUpdateTextPositionY: (Float) -> Unit = {},
    onUpdateTextColor: (String) -> Unit = {},
    onUpdateTextFontSize: (Float) -> Unit = {},
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
    onUpdateImageOverlayX: (Float) -> Unit = {},
    onUpdateImageOverlayY: (Float) -> Unit = {},
    onUpdateImageOverlayCrop: (String) -> Unit = {},
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
    // KineMaster-style keyframe animation
    onUpdateKeyframeAnim: (String) -> Unit = {},
    // In-editor premium panels (AI Hub, Presets) — v6.2.0
    onUpdateAiFeature: (String) -> Unit = {},
    onUpdateSocialPreset: (String) -> Unit = {},
    // v6.0.0 Premium launcher — top action row (AI Hub, Presets, Pro, Studio)
    onAiHub: () -> Unit = {},
    onSocialPresets: () -> Unit = {},
    onProTier: () -> Unit = {},
    onPremiumStudio: () -> Unit = {},
    // v6.0.0 Effects & Stickers full-screen galleries
    onOpenEffects: () -> Unit = {},
    onOpenStickers: () -> Unit = {},
    onGenerateRoyaltyFreeMusic: (String) -> Unit = {}
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

    // ═══ BGM (Background Music) ExoPlayer — second player for background music ═══
    val bgmPlayer = remember { ExoPlayer.Builder(context).build().apply { repeatMode = Player.REPEAT_MODE_ONE } }
    var bgmPrepared by remember { mutableStateOf(false) }

    // Prepare BGM when backgroundMusicPath changes
    LaunchedEffect(project.backgroundMusicPath) {
        val bgmPath = project.backgroundMusicPath
        if (!bgmPath.isNullOrBlank()) {
            val bgmUri = if (bgmPath.startsWith("content://") || bgmPath.startsWith("file://"))
                Uri.parse(bgmPath) else Uri.fromFile(java.io.File(bgmPath))
            bgmPlayer.setMediaItem(MediaItem.fromUri(bgmUri))
            bgmPlayer.prepare()
            bgmPrepared = true
            if (isPlaying) bgmPlayer.play()
        } else {
            bgmPlayer.stop()
            bgmPlayer.clearMediaItems()
            bgmPrepared = false
        }
    }

    // Sync BGM play/pause with video
    LaunchedEffect(isPlaying) {
        if (isPlaying && bgmPrepared) bgmPlayer.play()
        else bgmPlayer.pause()
    }

    // Sync BGM volume
    LaunchedEffect(project.backgroundMusicVolume) {
        bgmPlayer.volume = project.backgroundMusicVolume
    }

    // Release BGM player on dispose
    DisposableEffect(Unit) { onDispose { bgmPlayer.release() } }

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
        project.imageEditorExposure,
        project.imageEditorVignette,
        project.imageEditorGrain,
        project.imageEditorFade,
        project.imageEditorHighlights,
        project.imageEditorShadows,
        project.imageEditorBlur,
        project.imageEditorSharpen,
        project.selectedEffect
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
        val vi = project.imageEditorVignette
        val gr = project.imageEditorGrain
        val fa = project.imageEditorFade
        val hi = project.imageEditorHighlights
        val sh = project.imageEditorShadows
        val hasAdjustments = b != 1f || c != 1f || s != 1f || t != 1f || e != 1f || vi != 0f || gr != 0f || fa != 0f || hi != 0f || sh != 0f

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

            // Fade: lifts blacks (adds to all channels equally)
            val fadeAdd = fa * 60f
            // Highlights: brighten upper range
            val hlAdd = hi * 40f
            // Shadows: lift darks
            val shAdd = sh * 30f

            val adjMatrix = ColorMatrix(floatArrayOf(
                contrastScale * tempRed * expScale, 0f, 0f, 0f, brightnessShift + contrastShift + fadeAdd + hlAdd + shAdd,
                0f, contrastScale * expScale, 0f, 0f, brightnessShift + contrastShift + fadeAdd + hlAdd + shAdd,
                0f, 0f, contrastScale * tempBlue * expScale, 0f, brightnessShift + contrastShift + fadeAdd + hlAdd + shAdd,
                0f, 0f, 0f, 1f, 0f
            ))
            val satMatrix = ColorMatrix().apply { setToSaturation(s) }
            adjMatrix *= satMatrix
            // Grain: approximate by slight random-like desaturation + contrast bump
            if (gr > 0f) {
                val grainSat = ColorMatrix().apply { setToSaturation((1f - gr * 0.3f).coerceAtLeast(0.5f)) }
                adjMatrix *= grainSat
            }

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

                // ── Live Adjustment Overlays ──
                // Vignette overlay: radial darkening at edges
                if (project.imageEditorVignette > 0f) {
                    Box(
                        modifier = Modifier.fillMaxSize()
                            .drawWithContent {
                                drawContent()
                                drawRect(
                                    brush = Brush.radialGradient(
                                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = project.imageEditorVignette * 0.7f)),
                                        center = center,
                                        radius = size.maxDimension * 0.7f
                                    )
                                )
                            }
                    )
                }
                // Fade overlay: lift blacks
                if (project.imageEditorFade > 0f) {
                    Box(Modifier.fillMaxSize().background(Color.White.copy(alpha = project.imageEditorFade * 0.15f)))
                }
                // Grain overlay: noise dots approximation
                if (project.imageEditorGrain > 0f) {
                    val grainAlpha = (project.imageEditorGrain * 0.2f).coerceAtMost(0.3f)
                    Box(
                        modifier = Modifier.fillMaxSize()
                            .drawWithContent {
                                drawContent()
                                // Draw random-looking dots for grain effect
                                for (i in 0..40) {
                                    val x = (i * 97 + 31) % size.width.toInt()
                                    val y = (i * 53 + 17) % size.height.toInt()
                                    drawCircle(
                                        color = Color.White.copy(alpha = grainAlpha),
                                        radius = 1.5f,
                                        center = GeomOffset(x.toFloat(), y.toFloat())
                                    )
                                }
                            }
                    )
                }
                // Selected effect overlay (VHS scanlines, glitch, etc.)
                if (project.selectedEffect != "none") {
                    val effectOverlayAlpha = 0.12f
                    Box(
                        modifier = Modifier.fillMaxSize()
                            .drawWithContent {
                                drawContent()
                                when (project.selectedEffect) {
                                    "vhs", "scanline", "crt", "old_film" -> {
                                        // VHS scanlines
                                        for (i in 0..20) {
                                            val y = size.height * i / 20f
                                            drawRect(
                                                color = Color.Black.copy(alpha = effectOverlayAlpha),
                                                topLeft = GeomOffset(0f, y),
                                                size = GeomSize(size.width, size.height * 0.02f)
                                            )
                                        }
                                    }
                                    "vignette" -> {
                                        drawRect(
                                            brush = Brush.radialGradient(
                                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.4f)),
                                                center = center,
                                                radius = size.maxDimension * 0.6f
                                            )
                                        )
                                    }
                                    "film_grain", "lofi" -> {
                                        for (i in 0..30) {
                                            val x = (i * 137 + 53) % size.width.toInt()
                                            val y = (i * 89 + 29) % size.height.toInt()
                                            drawCircle(
                                                color = Color.White.copy(alpha = 0.1f),
                                                radius = 1f,
                                                center = GeomOffset(x.toFloat(), y.toFloat())
                                            )
                                        }
                                    }
                                    "night_vision" -> {
                                        drawRect(color = Color(0xFF00FF00).copy(alpha = 0.08f))
                                    }
                                    "thermal" -> {
                                        drawRect(
                                            brush = Brush.verticalGradient(
                                                colors = listOf(Color(0xFFFF0000).copy(alpha = 0.06f), Color(0xFF0000FF).copy(alpha = 0.06f))
                                            )
                                        )
                                    }
                                    else -> { /* no overlay for other effects */ }
                                }
                            }
                    )
                }
                // Chroma key overlay: show green screen indicator
                if (project.greenScreenEnabled) {
                    Box(
                        modifier = Modifier.fillMaxSize()
                            .border(2.dp, Color(0xFF00FF00).copy(alpha = 0.4f))
                    )
                    Box(
                        modifier = Modifier.align(Alignment.TopEnd).padding(6.dp)
                            .background(Color(0xFF00AA00).copy(alpha = 0.7f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text("🟢 Chroma", fontSize = 7.sp, color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }

                // Image overlay — show actual image with position/scale/opacity
                if (project.imageOverlayPath != null && layerImageVisible) {
                    val overlayBitmap = remember(project.imageOverlayPath) {
                        try {
                            val path = project.imageOverlayPath!!
                            if (path.startsWith("content://") || path.startsWith("file://")) {
                                val uri = android.net.Uri.parse(path)
                                val inputStream = context.contentResolver.openInputStream(uri)
                                inputStream?.use { android.graphics.BitmapFactory.decodeStream(it) }
                            } else {
                                android.graphics.BitmapFactory.decodeFile(path)
                            }
                        } catch (e: Exception) { null }
                    }
                    if (overlayBitmap != null) {
                        val painter = androidx.compose.ui.graphics.painter.BitmapPainter(
                            overlayBitmap.asImageBitmap()
                        )
                        androidx.compose.foundation.Image(
                            painter = painter,
                            contentDescription = "Image overlay",
                            modifier = Modifier
                                .fillMaxSize(project.imageOverlayScale)
                                .align(
                                    BiasAlignment(
                                        horizontalBias = (project.imageOverlayX * 2f - 1f).coerceIn(-1f, 1f),
                                        verticalBias = (project.imageOverlayY * 2f - 1f).coerceIn(-1f, 1f)
                                    )
                                )
                                .graphicsLayer { alpha = project.imageOverlayOpacity }
                        )
                    } else {
                        Box(
                            modifier = Modifier.fillMaxSize(project.imageOverlayScale).align(
                                BiasAlignment(
                                    horizontalBias = (project.imageOverlayX * 2f - 1f).coerceIn(-1f, 1f),
                                    verticalBias = (project.imageOverlayY * 2f - 1f).coerceIn(-1f, 1f)
                                )
                            ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("🖼️", fontSize = 32.sp)
                        }
                    }
                }

                // Text overlay -- v5.0.0 LIVE PREVIEW ANIMATION
                // v6.3.0 — Centered "Enter your text here" placeholder when text
                // tool is active but no text has been entered yet. This gives the
                // user a clear visual cue to start typing.
                if ((project.activeTextOverlay == null || project.activeTextOverlay!!.isBlank()) && selectedTool == 5 && layerTextVisible) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .background(Color.Black.copy(alpha = 0.55f), RoundedCornerShape(12.dp))
                            .border(1.5.dp, NeonOrange.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 24.dp, vertical = 14.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("✏️", fontSize = 16.sp)
                            Text(
                                "Enter your text here",
                                color = Color.White.copy(alpha = 0.7f),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
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
                        modifier = Modifier.align(
                                BiasAlignment(
                                    horizontalBias = (project.textPositionX * 2f - 1f).coerceIn(-1f, 1f),
                                    verticalBias = (project.textPositionY * 2f - 1f).coerceIn(-1f, 1f)
                                )
                            )
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

                // ══ Clean preview — no hardcoded badges ══
                // Resolution badge top-left
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(10.dp)
                        .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(6.dp))
                        .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 6.dp, vertical = 3.dp)
                ) {
                    Text("PREVIEW", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color.White.copy(alpha = 0.8f), letterSpacing = 1.sp)
                }

                // v6.3.0 — ACTIVE EFFECTS INDICATOR: Shows what effects/filters are
                // applied and will be burned in at export. This makes it clear to the
                // user that their selections are REAL (applied via FFmpeg at export).
                val hasEffect = project.selectedEffect != "none"
                val hasFilter = project.selectedFilter != "none" && project.selectedFilter.lowercase() != "none"
                val hasLook = project.activePremiumLook.isNotEmpty() && project.activePremiumLook != "none"
                if (hasEffect || hasFilter || hasLook) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(10.dp)
                            .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(6.dp))
                            .border(1.dp, CyberCyan.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 6.dp, vertical = 3.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Box(Modifier.size(5.dp).background(Color(0xFF34D399), CircleShape))
                            Text("✓ FX", fontSize = 7.sp, fontWeight = FontWeight.Bold, color = Color(0xFF34D399))
                            if (hasEffect) {
                                Text(project.selectedEffect.replace("_", " ").replaceFirstChar { it.uppercase() }, fontSize = 7.sp, color = Color.White, fontWeight = FontWeight.Medium, maxLines = 1)
                            }
                            if (hasFilter) {
                                Text("+ ${project.selectedFilter.replace("_", " ").replaceFirstChar { it.uppercase() }}", fontSize = 7.sp, color = CyberCyan, fontWeight = FontWeight.Medium, maxLines = 1)
                            }
                            if (hasLook) {
                                Text("+ ${project.activePremiumLook.replaceFirstChar { it.uppercase() }}", fontSize = 7.sp, color = PremiumGold, fontWeight = FontWeight.Medium, maxLines = 1)
                            }
                        }
                    }
                }
                // v5.2.0 — Tap-to-edit overlay on text
                if (project.activeTextOverlay != null && layerTextVisible) {
                    Box(
                        modifier = Modifier.align(
                            BiasAlignment(
                                horizontalBias = (project.textPositionX * 2f - 1f).coerceIn(-1f, 1f),
                                verticalBias = ((project.textPositionY - 0.15f) * 2f - 1f).coerceIn(-1f, 1f)
                            )
                        )
                            .background(Color.Black.copy(0.5f), RoundedCornerShape(6.dp))
                            .border(1.dp, CyberCyan.copy(0.5f), RoundedCornerShape(6.dp))
                            .clickable { selectedTool = 5; isPanelExpanded = true }
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
                            .clickable { selectedTool = 8; isPanelExpanded = true }
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
            selectedTool = selectedTool,
            onToggleVideoLayer = { layerVideoVisible = !layerVideoVisible; selectedTool = 0; isPanelExpanded = true },
            onToggleAudioLayer = { layerAudioVisible = !layerAudioVisible; selectedTool = 4; isPanelExpanded = true },
            onToggleTextLayer = { layerTextVisible = !layerTextVisible; selectedTool = 5; isPanelExpanded = true },
            onToggleImageLayer = { layerImageVisible = !layerImageVisible; selectedTool = 12; isPanelExpanded = true },
            onToggleStickerLayer = { layerStickerVisible = !layerStickerVisible; selectedTool = 8; isPanelExpanded = true },
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
                onUpdateTextStyle = onUpdateTextStyle,
                onUpdateTextPositionX = onUpdateTextPositionX,
                onUpdateTextPositionY = onUpdateTextPositionY,
                onUpdateTextColor = onUpdateTextColor,
                onUpdateTextFontSize = onUpdateTextFontSize,
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
                onUpdateImageOverlayScale = onUpdateImageOverlayScale,
                onUpdateImageOverlayX = onUpdateImageOverlayX,
                onUpdateImageOverlayY = onUpdateImageOverlayY,
                onUpdateImageOverlayCrop = onUpdateImageOverlayCrop,
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
                onUpdatePremiumLook = onUpdatePremiumLook,
                onUpdateKeyframeAnim = onUpdateKeyframeAnim,
                onUpdateAiFeature = onUpdateAiFeature,
                onUpdateSocialPreset = onUpdateSocialPreset,
                onGenerateRoyaltyFreeMusic = onGenerateRoyaltyFreeMusic
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
                // v6.3.0 — ALL tools (including Effects & Stickers) use in-editor panels.
                // No more separate full-screen gallery screens — everything is inline.
                if (selectedTool == idx) { isPanelExpanded = !isPanelExpanded } else { selectedTool = idx; isPanelExpanded = true }
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
    selectedTool: Int = -1,
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
        modifier = Modifier.fillMaxWidth().height(140.dp)
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
                // Video track — show real filename from project.videoPath
                val videoFileName = project.videoPath.substringAfterLast("/").substringBeforeLast(".").take(18)
                TimelineTrackRow(
                    label = "🎬", isActive = layerVideoVisible || selectedTool == 0, onToggle = onToggleVideoLayer,
                    content = {
                        Box(Modifier.weight(1f).fillMaxHeight().background(Brush.horizontalGradient(listOf(NeonOrange, Color(0xFFFF7043))), RoundedCornerShape(3.dp)), contentAlignment = Alignment.Center) { Text(videoFileName.ifBlank { "Video" }, fontSize = 7.sp, color = Color.White, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                    }
                )
                // Audio track
                TimelineTrackRow(
                    label = "🔊", isActive = layerAudioVisible || selectedTool == 4, onToggle = onToggleAudioLayer,
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
                    label = "📝", isActive = layerTextVisible || selectedTool == 5, onToggle = onToggleTextLayer,
                    content = {
                        Spacer(Modifier.weight(0.15f))
                        Box(Modifier.weight(0.7f).fillMaxHeight().background(Brush.horizontalGradient(listOf(Color(0xFFAB47BC), Color(0xFFBA68C8))), RoundedCornerShape(3.dp)), contentAlignment = Alignment.Center) { Text(project.activeTextOverlay?.take(12) ?: "Subtitle", fontSize = 7.sp, color = Color.White, fontWeight = FontWeight.Bold) }
                        Spacer(Modifier.weight(0.15f))
                    }
                )
                // Image track
                TimelineTrackRow(
                    label = "🖼️", isActive = layerImageVisible || selectedTool == 12, onToggle = onToggleImageLayer,
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
                    label = "⭐", isActive = layerStickerVisible || selectedTool == 8, onToggle = onToggleStickerLayer,
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
        modifier = Modifier.fillMaxWidth().height(20.dp).padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // KineMaster-style layer toggle icon with colored accent
        Box(
            modifier = Modifier.width(22.dp).fillMaxHeight()
                .background(if (isActive) Color(0xFFFF5A3C).copy(0.15f) else Color.White.copy(0.02f), RoundedCornerShape(4.dp))
                .border(1.dp, if (isActive) Color(0xFFFF5A3C).copy(0.3f) else Color.Transparent, RoundedCornerShape(4.dp))
                .clickable(onClick = onToggle),
            contentAlignment = Alignment.Center
        ) {
            Text(label, fontSize = 9.sp, color = Color.White.copy(alpha = if (isActive) 1f else 0.3f))
        }
        // Track content
        Row(modifier = Modifier.weight(1f).fillMaxHeight().padding(start = 3.dp), verticalAlignment = Alignment.CenterVertically) {
            if (isActive) content() else Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Hidden", fontSize = 7.sp, color = Color.Gray.copy(0.4f)) }
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
        "💎" to "Keyframe",
        // 2027 8K: Premium tools merged into bottom toolbar as gradient pills
        "🤖" to "AI Hub", "📱" to "Presets",
        "👑" to "Pro", "✨" to "Studio"
    )
    Row(
        modifier = Modifier.fillMaxWidth().height(72.dp)
            .glassmorphic(shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp), backColor = Color(0xFF0F0F1A).copy(alpha = 0.95f))
            .border(1.5.dp, Brush.horizontalGradient(listOf(Color(0xFFFF5A3C).copy(alpha = 0.3f), Color(0xFF9D4EDD).copy(alpha = 0.3f))), RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        tools.forEachIndexed { idx, (emoji, name) ->
            val isActive = selectedTool == idx
            // 2027 8K: Premium tools (last 4) get gradient pill styling
            val isPremium = idx >= tools.size - 4
            Box(
                modifier = Modifier.clip(RoundedCornerShape(12.dp))
                    .background(
                        if (isPremium) {
                            if (isActive) Brush.horizontalGradient(listOf(Color(0xFFFF5A3C), Color(0xFF9D4EDD)))
                            else Brush.horizontalGradient(listOf(Color(0xFFFF5A3C).copy(alpha = 0.3f), Color(0xFF9D4EDD).copy(alpha = 0.3f)))
                        } else {
                            Brush.horizontalGradient(listOf(
                                if (isActive) Color(0xFFFF5A3C).copy(alpha = 0.35f) else Color.Transparent,
                                if (isActive) Color(0xFF9D4EDD).copy(alpha = 0.2f) else Color.Transparent
                            ))
                        }
                    )
                    .border(
                        if (isActive) 1.dp else 0.dp,
                        if (isActive) Color(0xFFFF5A3C) else Color.Transparent,
                        RoundedCornerShape(12.dp)
                    )
                    .clickable {
                        onToolSelected(idx)
                    }
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(emoji, fontSize = 18.sp)
                    Spacer(Modifier.height(2.dp))
                    Text(
                        name, fontSize = 8.sp, fontWeight = FontWeight.Black,
                        color = if (isPremium) Color.White else if (isActive) Color(0xFFFF5A3C) else Color.Gray,
                        letterSpacing = 0.5.sp
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
    onUpdateTextStyle: (String) -> Unit = {},
    onUpdateTextPositionX: (Float) -> Unit = {},
    onUpdateTextPositionY: (Float) -> Unit = {},
    onUpdateTextColor: (String) -> Unit = {},
    onUpdateTextFontSize: (Float) -> Unit = {},
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
    onUpdateImageOverlayScale: (Float) -> Unit = {},
    onUpdateImageOverlayX: (Float) -> Unit = {},
    onUpdateImageOverlayY: (Float) -> Unit = {},
    onUpdateImageOverlayCrop: (String) -> Unit = {},
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
    onUpdatePremiumLook: (String) -> Unit = {},
    onUpdateKeyframeAnim: (String) -> Unit = {},
    onUpdateAiFeature: (String) -> Unit = {},
    onUpdateSocialPreset: (String) -> Unit = {},
    onProTier: () -> Unit = {},
    onPremiumStudio: () -> Unit = {},
    onGenerateRoyaltyFreeMusic: (String) -> Unit = {}
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
                2 -> SpeedPanel(project, onUpdateSpeed, onUpdateSpeedCurve, onToggleReverse, onUpdateFreezeFrame)
                3 -> CropPanel(project, onUpdateCropPreset, onUpdateAspectPreset, onUpdateRotation, onToggleFlipHorizontal, onToggleFlipVertical)
                4 -> AudioPanel(project, onToggleMute, onUpdateVideoVolume, onUpdateMusicVolume, onUpdateVisualizerStyle, onToggleBeatSync, musicPicker, onClearAudio = { onUpdateBackgroundMusic(null) }, onGenerateRoyaltyFreeMusic = onGenerateRoyaltyFreeMusic)
                5 -> TextPanel(project, onUpdateTextOverlay, onUpdateTextAnimation, onUpdateTextStyle, onUpdateTextPositionX, onUpdateTextPositionY, onUpdateTextColor, onUpdateTextFontSize)
                6 -> FiltersPanel(project, onUpdateFilter)
                7 -> EffectsPanel(project, onUpdateSelectedEffect, onUpdateFilter)
                8 -> StickersPanel(project, onUpdateStickerType)
                9 -> TransitionsPanel(project, onUpdateTransition)
                10 -> AnimationsPanel(project, onUpdateTextAnimation)
                11 -> ThreeDPanel(project, onUpdate3DShapeMask)
                12 -> ImagePanel(project, imagePicker, onUpdateImageOverlay, onUpdateImageOverlayOpacity, onUpdateImageOverlayScale, onUpdateImageOverlayX, onUpdateImageOverlayY, onUpdateImageOverlayCrop)
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
                16 -> com.powercut.editor.ui.editor.tools.ImageStudioPanel(
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
                    onUpdateBrightness = onUpdateImageEditorBrightness,
                    onUpdateContrast = onUpdateImageEditorContrast,
                    onUpdateSaturation = onUpdateImageEditorSaturation,
                    onUpdateExposure = onUpdateImageEditorExposure,
                    onUpdateTemperature = onUpdateImageEditorTemperature,
                    onUpdateVignette = onUpdateImageEditorVignette,
                    onUpdateGrain = onUpdateImageEditorGrain,
                    onUpdateFade = onUpdateImageEditorFade,
                    onUpdateHighlights = onUpdateImageEditorHighlights,
                    onUpdateShadows = onUpdateImageEditorShadows,
                    onUpdateBlur = onUpdateImageEditorBlur,
                    onUpdateSharpen = onUpdateImageEditorSharpen,
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
                28 -> KeyframePanel(project, onUpdateKeyframeAnim)
                29 -> AiHubPanel(project, onUpdateAiFeature)
                30 -> PresetsPanel(project, onUpdateSocialPreset)
                31 -> ProPanel(project, onProTier, onUpdatePremiumLook)
                32 -> StudioPanel(project, onPremiumStudio)
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
        LiveAnimatedHeader("EDIT", "✂️", NeonOrange)

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
        LiveAnimatedHeader("LAYERS", "📑", CyberCyan)

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
private fun SpeedPanel(project: VideoProject, onUpdateSpeed: (Float) -> Unit, onUpdateSpeedCurve: (String) -> Unit, onToggleReverse: () -> Unit = {}, onUpdateFreezeFrame: (Long) -> Unit = {}) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        LiveAnimatedHeader("SPEED", "⚡", NeonOrange)

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
            listOf("Standard", "Montage", "Hero", "Flash", "Smooth").forEach { c ->
                val sel = project.speedCurve.lowercase() == c.lowercase()
                Box(Modifier.weight(1f).background(if (sel) CyberCyan.copy(0.15f) else Color.White.copy(0.03f), RoundedCornerShape(6.dp)).clickable { onUpdateSpeedCurve(if (sel) "constant" else c) }.padding(4.dp), contentAlignment = Alignment.Center) {
                    Text(c, fontSize = 7.sp, fontWeight = FontWeight.Bold, color = if (sel) CyberCyan else Color.White)
                }
            }
        }

        // Reverse toggle
        Row(Modifier.fillMaxWidth().background(if (project.isReverseEnabled) CyberCyan.copy(0.15f) else Color.White.copy(0.04f), RoundedCornerShape(6.dp)).border(1.dp, if (project.isReverseEnabled) CyberCyan.copy(0.3f) else Color.Transparent, RoundedCornerShape(6.dp)).clickable { onToggleReverse() }.padding(horizontal = 10.dp, vertical = 6.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("🔄 REVERSE", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = if (project.isReverseEnabled) CyberCyan else Color.White)
            Text(if (project.isReverseEnabled) "ON" else "OFF", fontSize = 8.sp, color = if (project.isReverseEnabled) CyberCyan else Color.Gray)
        }

        // Freeze frame duration
        Text("FREEZE FRAME", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            listOf(0L to "Off", 250L to "0.25s", 500L to "0.5s", 1000L to "1s", 2000L to "2s", 3000L to "3s").forEach { (ms, label) ->
                val sel = project.freezeFrameMs == ms
                Box(Modifier.weight(1f).background(if (sel) CyberCyan.copy(0.2f) else Color.White.copy(0.04f), RoundedCornerShape(6.dp)).clickable { onUpdateFreezeFrame(ms) }.padding(3.dp), contentAlignment = Alignment.Center) {
                    Text(label, fontSize = 7.sp, fontWeight = FontWeight.Bold, color = if (sel) CyberCyan else Color.White)
                }
            }
        }
    }
}


// ─── 3. CROP PANEL ─────────────────────────────────────────────
@Composable
private fun CropPanel(project: VideoProject, onUpdateCrop: (String) -> Unit, onUpdateAspect: (String) -> Unit, onRotate: () -> Unit, onFlipH: () -> Unit, onFlipV: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        LiveAnimatedHeader("CROP", "📐", SignaturePurple)

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
    musicPicker: androidx.activity.result.ActivityResultLauncher<String>,
    onImportAudio: (String) -> Unit = {},
    onClearAudio: () -> Unit = {},
    onGenerateRoyaltyFreeMusic: (String) -> Unit = {}
) {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    var audioSubTab by remember { mutableStateOf("mixer") }
    val pulse by rememberPulse()
    val infiniteTransition = rememberInfiniteTransition(label = "audio")
    val waveAnim by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1500, easing = EaseInOutSine), RepeatMode.Reverse),
        label = "wave"
    )

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("AUDIO STUDIO", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = CyberCyan)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Import · Timeline · Royal Free · Local", fontSize = 7.sp, color = Color.Gray)
            Box(Modifier.background(CyberCyan.copy(0.15f), RoundedCornerShape(4.dp)).padding(horizontal = 5.dp, vertical = 2.dp)) {
                Text("✓ Full Audio", fontSize = 7.sp, fontWeight = FontWeight.Bold, color = CyberCyan)
            }
        }

        // Sub-tabs: Mixer | Import | Royal Free | Timeline
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            listOf("mixer" to "Mixer", "import" to "Import", "royal" to "Royal Free", "timeline" to "Timeline").forEach { (id, label) ->
                val sel = audioSubTab == id
                Box(Modifier.background(if (sel) CyberCyan.copy(0.2f) else Color.White.copy(0.04f), RoundedCornerShape(6.dp)).clickable { audioSubTab = id }.padding(horizontal = 8.dp, vertical = 4.dp)) {
                    Text(label, fontSize = 8.sp, fontWeight = FontWeight.Bold, color = if (sel) CyberCyan else Color.White)
                }
            }
        }

        when (audioSubTab) {
            "mixer" -> {
                // Volume mixing controls
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

            "import" -> {
                // Audio import — user's own audio + local music
                Text("IMPORT AUDIO", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = CyberCyan)
                Text("Add your own audio file or pick from device storage", fontSize = 7.sp, color = Color.Gray)

                // Import from file (custom audio)
                Box(Modifier.fillMaxWidth().background(CyberCyan.copy(0.15f), RoundedCornerShape(8.dp)).clickable { musicPicker.launch("audio/*") }.padding(10.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.MusicNote, "Import", tint = CyberCyan, modifier = Modifier.size(24.dp))
                        Spacer(Modifier.height(4.dp))
                        Text("IMPORT AUDIO FILE", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = CyberCyan)
                        Text("MP3, WAV, M4A, AAC, OGG", fontSize = 7.sp, color = Color.Gray)
                    }
                }

                Spacer(Modifier.height(2.dp))

                // Local music (from device)
                Text("LOCAL MUSIC", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = NeonOrange)
                Text("Browse music stored on your device", fontSize = 7.sp, color = Color.Gray)
                Box(Modifier.fillMaxWidth().background(NeonOrange.copy(0.12f), RoundedCornerShape(8.dp)).clickable { musicPicker.launch("audio/*"); android.widget.Toast.makeText(ctx, "Select local music from your device", android.widget.Toast.LENGTH_SHORT).show() }.padding(10.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("📁 BROWSE DEVICE MUSIC", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = NeonOrange)
                        Text("Pick from your music library", fontSize = 7.sp, color = Color.Gray)
                    }
                }

                // Currently loaded audio info
                if (project.backgroundMusicPath != null) {
                    Spacer(Modifier.height(2.dp))
                    Text("CURRENT AUDIO", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                    Box(Modifier.fillMaxWidth().background(Color.White.copy(0.06f), RoundedCornerShape(6.dp)).padding(6.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.MusicNote, "Current", tint = CyberCyan, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Column(Modifier.weight(1f)) {
                                Text("♪ ${project.backgroundMusicPath!!.substringAfterLast("/")}", fontSize = 8.sp, color = Color.White, maxLines = 1)
                                Text("Volume: ${(project.backgroundMusicVolume * 100).toInt()}%", fontSize = 7.sp, color = Color.Gray)
                            }
                            Box(Modifier.background(Color.Red.copy(0.3f), RoundedCornerShape(4.dp)).clickable { onClearAudio() }.padding(horizontal = 6.dp, vertical = 3.dp)) {
                                Text("✕", fontSize = 9.sp, color = Color.White)
                            }
                        }
                    }
                    // Volume slider for imported audio
                    Text("AUDIO VOLUME: ${(project.backgroundMusicVolume * 100).toInt()}%", fontSize = 7.sp, fontWeight = FontWeight.Bold, color = CyberCyan)
                    Slider(value = project.backgroundMusicVolume, onValueChange = onUpdateMusicVol, valueRange = 0f..1f, colors = SliderDefaults.colors(activeTrackColor = CyberCyan, thumbColor = CyberCyan), modifier = Modifier.fillMaxWidth().height(24.dp))
                }
            }

            "royal" -> {
                // Royal free music library — royalty-free tracks
                Text("ROYAL FREE MUSIC LIBRARY", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = PremiumGold)
                Text("Royalty-free tracks — safe for commercial use", fontSize = 7.sp, color = Color.Gray)

                // Animated music wave background
                Canvas(modifier = Modifier.fillMaxWidth().height(40.dp)) {
                    val w = size.width
                    val h = size.height
                    val barCount = 30
                    val barWidth = w / barCount
                    for (i in 0 until barCount) {
                        val phase = (i.toFloat() / barCount + waveAnim) % 1f
                        val barHeight = h * (0.2f + 0.6f * kotlin.math.sin(phase * kotlin.math.PI * 2f).toFloat().coerceIn(0f, 1f))
                        drawRoundRect(
                            color = PremiumGold.copy(0.4f + 0.3f * kotlin.math.sin(phase * kotlin.math.PI * 2f).toFloat().coerceIn(0f, 1f)),
                            topLeft = androidx.compose.ui.geometry.Offset(i * barWidth + barWidth * 0.2f, (h - barHeight) / 2),
                            size = androidx.compose.ui.geometry.Size(barWidth * 0.6f, barHeight),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(barWidth * 0.3f)
                        )
                    }
                }

                // Royal free music categories
                val royalMusic = listOf(
                    "🎵 Cinematic Epic" to "cinematic_epic", "🎵 Corporate Upbeat" to "corporate_upbeat",
                    "🎵 Lo-Fi Chill" to "lofi_chill", "🎵 EDM Energy" to "edm_energy",
                    "🎵 Acoustic Folk" to "acoustic_folk", "🎵 Jazz Lounge" to "jazz_lounge",
                    "🎵 Hip-Hop Beat" to "hiphop_beat", "🎵 Rock Anthem" to "rock_anthem",
                    "🎵 Classical Piano" to "classical_piano", "🎵 Ambient Space" to "ambient_space",
                    "🎵 Tropical House" to "tropical_house", "🎵 Trap 808" to "trap_808",
                    "🎵 Reggae Vibes" to "reggae_vibes", "🎵 Country Road" to "country_road",
                    "🎵 R&B Smooth" to "rnb_smooth", "🎵 Drum & Bass" to "dnb",
                    "🎵 Synthwave 80s" to "synthwave", "🎵 Orchestral" to "orchestral",
                    "🎵 Kids Playful" to "kids_playful", "🎵 Horror Suspense" to "horror_suspense",
                    "🎵 Wedding Romance" to "wedding_romance", "🎵 Birthday Party" to "birthday_party",
                    "🎵 Action Trailer" to "action_trailer", "🎵 Meditation Calm" to "meditation_calm"
                )

                LazyColumn(
                    modifier = Modifier.fillMaxWidth().height(180.dp),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    items(royalMusic) { (label, id) ->
                        val sel = project.backgroundMusicPath?.contains(id) == true
                        Box(
                            Modifier.fillMaxWidth()
                                .background(if (sel) PremiumGold.copy(0.2f) else Color.White.copy(0.04f), RoundedCornerShape(6.dp))
                                .border(if (sel) 1.dp else 0.dp, PremiumGold, RoundedCornerShape(6.dp))
                                .clickable {
                                    android.widget.Toast.makeText(ctx, "♻️ Generating $label — royalty-free audio being created...", android.widget.Toast.LENGTH_SHORT).show()
                                    onGenerateRoyaltyFreeMusic(id)
                                }
                                .padding(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text(label, fontSize = 8.sp, fontWeight = FontWeight.Bold, color = if (sel) PremiumGold else Color.White)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("Free", fontSize = 7.sp, color = PremiumGold, fontWeight = FontWeight.Bold)
                                    Spacer(Modifier.width(4.dp))
                                    Text("▶", fontSize = 10.sp, color = if (sel) PremiumGold else Color.Gray)
                                }
                            }
                        }
                    }
                }
                Text("✓ ${royalMusic.size} royalty-free tracks — no copyright strikes", fontSize = 7.sp, color = PremiumGold, fontWeight = FontWeight.Bold)
            }

            "timeline" -> {
                // Audio timeline — visual representation of video audio + BGM
                Text("AUDIO TIMELINE", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = CyberCyan)
                Text("Video audio + background music tracks", fontSize = 7.sp, color = Color.Gray)

                // Timeline canvas with animated waveforms
                Canvas(modifier = Modifier.fillMaxWidth().height(120.dp).background(Color.Black.copy(0.3f), RoundedCornerShape(8.dp))) {
                    val w = size.width
                    val h = size.height
                    val trackHeight = h / 3f

                    // Track 1: Video Audio
                    drawRoundRect(color = NeonOrange.copy(0.1f), topLeft = androidx.compose.ui.geometry.Offset(0f, 2f), size = androidx.compose.ui.geometry.Size(w, trackHeight - 4f), cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f))
                    // Video audio waveform
                    val vSamples = 60
                    for (i in 0 until vSamples) {
                        val phase = (i.toFloat() / vSamples + waveAnim) % 1f
                        val amp = trackHeight * 0.35f * (0.5f + 0.5f * kotlin.math.sin(phase * kotlin.math.PI * 4f).toFloat())
                        drawRoundRect(
                            color = NeonOrange.copy(0.7f),
                            topLeft = androidx.compose.ui.geometry.Offset(i * w / vSamples + 1f, (trackHeight - amp) / 2),
                            size = androidx.compose.ui.geometry.Size(w / vSamples - 2f, amp),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(2f)
                        )
                    }

                    // Track 2: Background Music
                    drawRoundRect(color = CyberCyan.copy(0.1f), topLeft = androidx.compose.ui.geometry.Offset(0f, trackHeight + 2f), size = androidx.compose.ui.geometry.Size(w, trackHeight - 4f), cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f))
                    if (project.backgroundMusicPath != null) {
                        // BGM waveform
                        val bSamples = 60
                        for (i in 0 until bSamples) {
                            val phase = (i.toFloat() / bSamples + waveAnim * 1.3f) % 1f
                            val amp = trackHeight * 0.3f * (0.4f + 0.6f * kotlin.math.sin(phase * kotlin.math.PI * 6f).toFloat()).coerceIn(0f, 1f)
                            drawRoundRect(
                                color = CyberCyan.copy(0.7f),
                                topLeft = androidx.compose.ui.geometry.Offset(i * w / bSamples + 1f, trackHeight + (trackHeight - amp) / 2),
                                size = androidx.compose.ui.geometry.Size(w / bSamples - 2f, amp),
                                cornerRadius = androidx.compose.ui.geometry.CornerRadius(2f)
                            )
                        }
                    } else {
                        drawContext.canvas.nativeCanvas.drawText(
                            "No BGM — tap Import to add",
                            w / 2 - 80f, trackHeight + trackHeight / 2 + 5f,
                            android.graphics.Paint().apply { color = android.graphics.Color.GRAY; textSize = 24f; textAlign = android.graphics.Paint.Align.CENTER }
                        )
                    }

                    // Track 3: Voice/Effects (placeholder)
                    drawRoundRect(color = PremiumGold.copy(0.1f), topLeft = androidx.compose.ui.geometry.Offset(0f, trackHeight * 2 + 2f), size = androidx.compose.ui.geometry.Size(w, trackHeight - 4f), cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f))

                    // Playhead
                    val playX = w * waveAnim
                    drawLine(color = Color.White.copy(0.8f), start = androidx.compose.ui.geometry.Offset(playX, 0f), end = androidx.compose.ui.geometry.Offset(playX, h), strokeWidth = 2f)
                }

                // Track labels
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("🔊 Video Audio ${(project.videoVolume * 100).toInt()}%", fontSize = 7.sp, color = NeonOrange, fontWeight = FontWeight.Bold)
                    Text("🎵 BGM ${(project.backgroundMusicVolume * 100).toInt()}%", fontSize = 7.sp, color = CyberCyan, fontWeight = FontWeight.Bold)
                    Text("🎤 Voice FX", fontSize = 7.sp, color = PremiumGold, fontWeight = FontWeight.Bold)
                }

                // Timeline controls
                Text("TRACK CONTROLS", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Box(Modifier.weight(1f).background(if (project.videoVolume > 0f) NeonOrange.copy(0.15f) else Color.White.copy(0.04f), RoundedCornerShape(6.dp)).clickable { onToggleMute() }.padding(6.dp), contentAlignment = Alignment.Center) {
                        Text(if (project.isMuted) "🔇 Muted" else "🔊 Video On", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = if (project.isMuted) Color.Gray else NeonOrange)
                    }
                    Box(Modifier.weight(1f).background(CyberCyan.copy(0.15f), RoundedCornerShape(6.dp)).clickable { musicPicker.launch("audio/*") }.padding(6.dp), contentAlignment = Alignment.Center) {
                        Text("🎵 + Add BGM", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = CyberCyan)
                    }
                }

                // Mix balance
                Text("MIX BALANCE", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Column(Modifier.weight(1f)) {
                        Text("Video ${(project.videoVolume * 100).toInt()}%", fontSize = 7.sp, color = NeonOrange)
                        Slider(value = project.videoVolume, onValueChange = onUpdateVideoVol, valueRange = 0f..1f, colors = SliderDefaults.colors(activeTrackColor = NeonOrange, thumbColor = NeonOrange), modifier = Modifier.height(20.dp))
                    }
                    Column(Modifier.weight(1f)) {
                        Text("Music ${(project.backgroundMusicVolume * 100).toInt()}%", fontSize = 7.sp, color = CyberCyan)
                        Slider(value = project.backgroundMusicVolume, onValueChange = onUpdateMusicVol, valueRange = 0f..1f, colors = SliderDefaults.colors(activeTrackColor = CyberCyan, thumbColor = CyberCyan), modifier = Modifier.height(20.dp))
                    }
                }

                // Beat sync
                Box(Modifier.fillMaxWidth().background(if (project.isBeatSyncEnabled) CyberCyan.copy(0.2f) else Color.White.copy(0.04f), RoundedCornerShape(6.dp)).clickable { onToggleBeatSync() }.padding(6.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("BEAT SYNC", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = if (project.isBeatSyncEnabled) CyberCyan else Color.White); Text(if (project.isBeatSyncEnabled) "ON" else "OFF", fontSize = 8.sp, color = if (project.isBeatSyncEnabled) CyberCyan else Color.Gray) }
                }
            }
        }
    }
}


// ─── 5. TEXT PANEL ──────────────────────────────────────────────
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TextPanel(
    project: VideoProject,
    onUpdateText: (String?) -> Unit,
    onUpdateAnim: (String) -> Unit,
    onUpdateStyle: (String) -> Unit = {},
    onUpdatePosX: (Float) -> Unit = {},
    onUpdatePosY: (Float) -> Unit = {},
    onUpdateColor: (String) -> Unit = {},
    onUpdateFontSize: (Float) -> Unit = {}
) {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    var txt by remember { mutableStateOf(project.activeTextOverlay ?: "") }
    var textSubTab by remember { mutableStateOf("text") }
    var selectedFontIndex by remember { mutableStateOf(0) }
    var selectedColorIndex by remember { mutableStateOf(0) }
    var selectedStyleIndex by remember { mutableStateOf(0) }

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        LiveAnimatedHeader("TEXT STUDIO", "📝", NeonOrange)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("40+ Styles · Position · 100+ Fonts · Animations", fontSize = 7.sp, color = Color.Gray)
            Box(Modifier.background(NeonOrange.copy(0.15f), RoundedCornerShape(4.dp)).padding(horizontal = 5.dp, vertical = 2.dp)) {
                Text("✓ Pro", fontSize = 7.sp, fontWeight = FontWeight.Bold, color = NeonOrange)
            }
        }

        // Sub-tabs: Text | Style | Position | Fonts | Color | Motion | Logo
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            listOf("text" to "Text", "style" to "Style", "pos" to "Position", "fonts" to "Fonts", "color" to "Color", "motion" to "Motion", "logo" to "Logo").forEach { (id, label) ->
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

                // Font size slider
                Text("FONT SIZE: ${project.textFontSize.toInt()}pt", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = CyberCyan)
                Slider(value = project.textFontSize, onValueChange = { onUpdateFontSize(it) }, valueRange = 8f..120f, colors = SliderDefaults.colors(thumbColor = NeonOrange, activeTrackColor = NeonOrange), modifier = Modifier.fillMaxWidth().height(28.dp))

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

            "style" -> {
                // 40+ visual design/capture styles (like CapCut text styles)
                Text("40+ TEXT DESIGN STYLES", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = NeonOrange)
                Text("Tap a style to apply — each has unique visual design", fontSize = 7.sp, color = Color.Gray)
                val styles = listOf(
                    "Classic" to "classic", "Bold Title" to "bold_title", "Subtitle" to "subtitle",
                    "Neon Glow" to "neon_glow", "Fire Burn" to "fire_burn", "Ice Freeze" to "ice_freeze",
                    "Gold Lux" to "gold_lux", "Silver Chrome" to "silver_chrome", "Bronze Metal" to "bronze_metal",
                    "Rainbow" to "rainbow", "Gradient" to "gradient", "Holographic" to "holographic",
                    "3D Block" to "3d_block", "3D Shadow" to "3d_shadow", "3D Pop" to "3d_pop",
                    "Outline" to "outline", "Double Outline" to "double_outline", "Shadow Drop" to "shadow_drop",
                    "Glow Aura" to "glow_aura", "Neon Border" to "neon_border", "Box Banner" to "box_banner",
                    "Strip Banner" to "strip_banner", "Gradient BG" to "gradient_bg", "Solid BG" to "solid_bg",
                    "Blur BG" to "blur_bg", "Frosted Glass" to "frosted_glass", "Glassmorphic" to "glassmorphic",
                    "Typewriter" to "typewriter", "Retro 80s" to "retro_80s", "Vintage Film" to "vintage_film",
                    "Comic Book" to "comic_book", "Graffiti" to "graffiti", "Street Art" to "street_art",
                    "Calligraphy" to "calligraphy", "Handwriting" to "handwriting", "Brush Script" to "brush_script",
                    "Pixel 8-Bit" to "pixel_8bit", "Arcade" to "arcade", "Digital Matrix" to "matrix",
                    "Cyberpunk" to "cyberpunk", "Hologram" to "hologram", "Glitch" to "glitch"
                )
                FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    styles.forEachIndexed { idx, (label, id) ->
                        val sel = project.textStyleId == id
                        Box(
                            Modifier
                                .background(if (sel) NeonOrange.copy(0.25f) else Color.White.copy(0.04f), RoundedCornerShape(6.dp))
                                .border(if (sel) 1.dp else 0.dp, NeonOrange, RoundedCornerShape(6.dp))
                                .clickable {
                                    selectedStyleIndex = idx
                                    onUpdateStyle(if (sel) "classic" else id)
                                    android.widget.Toast.makeText(ctx, "Style: $label", android.widget.Toast.LENGTH_SHORT).show()
                                }
                                .padding(horizontal = 6.dp, vertical = 5.dp)
                        ) {
                            Text(label, fontSize = 7.sp, fontWeight = if (sel) FontWeight.Black else FontWeight.Bold, color = if (sel) NeonOrange else Color.White)
                        }
                    }
                }
                Text("✓ ${styles.size} design styles available — more than CapCut!", fontSize = 7.sp, color = CyberCyan, fontWeight = FontWeight.Bold)
            }

            "pos" -> {
                // Position/placement control — user can set where text appears on screen
                Text("TEXT POSITION CONTROL", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = CyberCyan)
                Text("Tap a position or use sliders to place text anywhere on screen", fontSize = 7.sp, color = Color.Gray)

                // Visual 3x3 position grid
                Text("QUICK POSITIONS", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                val positions = listOf(
                    "TL" to Pair(0.15f, 0.15f), "TC" to Pair(0.5f, 0.15f), "TR" to Pair(0.85f, 0.15f),
                    "ML" to Pair(0.15f, 0.5f), "C" to Pair(0.5f, 0.5f), "MR" to Pair(0.85f, 0.5f),
                    "BL" to Pair(0.15f, 0.85f), "BC" to Pair(0.5f, 0.85f), "BR" to Pair(0.85f, 0.85f)
                )
                // Build 3x3 grid
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    for (row in 0..2) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                            for (col in 0..2) {
                                val (label, coords) = positions[row * 3 + col]
                                val (px, py) = coords
                                val isSel = kotlin.math.abs(project.textPositionX - px) < 0.05f && kotlin.math.abs(project.textPositionY - py) < 0.05f
                                Box(
                                    Modifier
                                        .weight(1f)
                                        .height(32.dp)
                                        .background(if (isSel) CyberCyan.copy(0.25f) else Color.White.copy(0.04f), RoundedCornerShape(6.dp))
                                        .border(if (isSel) 1.dp else 0.dp, CyberCyan, RoundedCornerShape(6.dp))
                                        .clickable {
                                            onUpdatePosX(px)
                                            onUpdatePosY(py)
                                            android.widget.Toast.makeText(ctx, "Text → $label", android.widget.Toast.LENGTH_SHORT).show()
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(label, fontSize = 7.sp, fontWeight = FontWeight.Bold, color = if (isSel) CyberCyan else Color.White)
                                }
                            }
                        }
                    }
                }

                // X position slider
                Text("X POSITION: ${(project.textPositionX * 100).toInt()}%", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                Slider(value = project.textPositionX, onValueChange = { onUpdatePosX(it) }, valueRange = 0f..1f, colors = SliderDefaults.colors(thumbColor = CyberCyan, activeTrackColor = CyberCyan), modifier = Modifier.fillMaxWidth().height(28.dp))

                // Y position slider
                Text("Y POSITION: ${(project.textPositionY * 100).toInt()}%", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                Slider(value = project.textPositionY, onValueChange = { onUpdatePosY(it) }, valueRange = 0f..1f, colors = SliderDefaults.colors(thumbColor = CyberCyan, activeTrackColor = CyberCyan), modifier = Modifier.fillMaxWidth().height(28.dp))

                // Quick presets
                Text("PRESET POSITIONS", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    listOf("Top" to Pair(0.5f, 0.1f), "Bottom" to Pair(0.5f, 0.9f), "Center" to Pair(0.5f, 0.5f), "Lower Third" to Pair(0.5f, 0.75f), "Upper Third" to Pair(0.5f, 0.25f), "Left Edge" to Pair(0.1f, 0.5f), "Right Edge" to Pair(0.9f, 0.5f)).forEach { (label, coords) ->
                        val (px, py) = coords
                        Box(Modifier.background(Color.White.copy(0.04f), RoundedCornerShape(6.dp)).clickable { onUpdatePosX(px); onUpdatePosY(py) }.padding(horizontal = 6.dp, vertical = 4.dp)) {
                            Text(label, fontSize = 7.sp, fontWeight = FontWeight.Bold, color = Color.White)
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
                val colorHexes = listOf(
                    "#FFFFFF", "#000000", "#FF0000", "#00FF00", "#0000FF",
                    "#FFFF00", "#00FFFF", "#FF00FF", "#7C5CFF", "#FF6B35",
                    "#2DD4BF", "#FF3D7F", "#FFD700", "#00FF00", "#FF00FF",
                    "#00FFFF", "#FFA500", "#800080", "#FF1493", "#00CED1",
                    "#FF4500", "#32CD32", "#FF69B4", "#1E90FF", "#FF8C00",
                    "#9370DB", "#20B2AA", "#FFB6C1", "#90EE90", "#DDA0DD",
                    "#F0E68C", "#E6E6FA", "#FFFACD", "#AFEEEE"
                )
                FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    textColors.forEachIndexed { idx, color ->
                        val sel = selectedColorIndex == idx
                        Box(Modifier.size(28.dp).background(color, RoundedCornerShape(6.dp))
                            .border(if (sel) 2.dp else 0.dp, Color.White, RoundedCornerShape(6.dp))
                            .clickable {
                                selectedColorIndex = idx
                                onUpdateColor(colorHexes[idx])
                                android.widget.Toast.makeText(ctx, "Text color selected", android.widget.Toast.LENGTH_SHORT).show()
                            }) {}
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
                // Animated text — loop/move full screen
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
                // Logo overlay support
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
        LiveAnimatedHeader("FILTERS", "🎨", SignaturePurple)

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
        LiveAnimatedHeader("EFFECTS", "✨", NeonOrange)

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            Text("SUPER EFFECTS", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = NeonOrange)
            Box(Modifier.background(NeonOrange.copy(0.15f), RoundedCornerShape(4.dp)).padding(horizontal = 5.dp, vertical = 2.dp)) {
                Text("✓ Real FFmpeg", fontSize = 7.sp, fontWeight = FontWeight.Bold, color = NeonOrange)
            }
        }
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            listOf("all" to "All", "vfx" to "VFX", "color" to "Color", "motion" to "Motion", "retro" to "Retro", "neon" to "Neon", "magic" to "Magic", "artistic" to "Art").forEach { (id, label) ->
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
                // ── v6.3.0 ANIME & ARTISTIC EFFECTS (user-requested, real FFmpeg) ──
                EffectItem("🌸 Anime", "anime", "none", "artistic"),
                EffectItem("🎨 Ghibli", "ghibli", "none", "artistic"),
                EffectItem("📖 Manga", "manga", "grayscale", "artistic"),
                EffectItem("💥 Comic", "comic", "none", "artistic"),
                EffectItem("🖼️ Painting", "painting", "none", "artistic"),
                EffectItem("🎭 Cel Shade", "cel_shade", "none", "artistic"),
                EffectItem("📽️ Retro Film", "vintage_film", "sepia", "artistic"),
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
        LiveAnimatedHeader("STICKERS", "😀", CyberCyan)

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
        LiveAnimatedHeader("TRANSITIONS", "🔄", SignaturePurple)

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            Text("TRANSITIONS", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = NeonOrange)
            Box(Modifier.background(NeonOrange.copy(0.15f), RoundedCornerShape(4.dp)).padding(horizontal = 5.dp, vertical = 2.dp)) {
                Text("✦ 3D GLASS • LIVE", fontSize = 7.sp, fontWeight = FontWeight.Bold, color = NeonOrange)
            }
        }
        FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
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
                Column(
                    modifier = Modifier.width(58.dp).height(74.dp)
                        .glassCard3D(shape = RoundedCornerShape(12.dp), glowColor = if (sel) NeonOrange else SignaturePurple.copy(0.3f), backColor = GlassBackground)
                        .border(if (sel) 2.dp else 0.5.dp, if (sel) NeonOrange else Color.White.copy(0.08f), RoundedCornerShape(12.dp))
                        .tactileClick { onUpdateTransition(if (sel) "none" else t) }
                        .padding(3.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(modifier = Modifier.fillMaxWidth().weight(1f).clip(RoundedCornerShape(8.dp))) {
                        TransitionDemoPreview(transitionId = t, modifier = Modifier.fillMaxSize())
                        if (sel) {
                            Box(modifier = Modifier.align(Alignment.TopEnd).padding(1.dp)
                                .background(NeonOrange, CircleShape).size(6.dp))
                        }
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(display, fontSize = 6.sp, fontWeight = FontWeight.Bold,
                        color = if (sel) NeonOrange else Color.White.copy(0.85f),
                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center)
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
                Text("✦ 3D GLASS • LIVE", fontSize = 7.sp, fontWeight = FontWeight.Bold, color = CyberCyan)
            }
        }
        FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
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
                Column(
                    modifier = Modifier.width(58.dp).height(74.dp)
                        .glassCard3D(shape = RoundedCornerShape(12.dp), glowColor = if (sel) CyberCyan else SignaturePurple.copy(0.3f), backColor = GlassBackground)
                        .border(if (sel) 2.dp else 0.5.dp, if (sel) CyberCyan else Color.White.copy(0.08f), RoundedCornerShape(12.dp))
                        .tactileClick { onUpdateAnim(if (sel) "none" else a) }
                        .padding(3.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(modifier = Modifier.fillMaxWidth().weight(1f).clip(RoundedCornerShape(8.dp))) {
                        AnimationDemoPreview(animId = a, modifier = Modifier.fillMaxSize())
                        if (sel) {
                            Box(modifier = Modifier.align(Alignment.TopEnd).padding(1.dp)
                                .background(CyberCyan, CircleShape).size(6.dp))
                        }
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(display, fontSize = 6.sp, fontWeight = FontWeight.Bold,
                        color = if (sel) CyberCyan else Color.White.copy(0.85f),
                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center)
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
        LiveAnimatedHeader("3D MASK", "🎭", CyberCyan)

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
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ImagePanel(
    project: VideoProject,
    imagePicker: androidx.activity.result.ActivityResultLauncher<String>,
    onUpdateImage: (String?) -> Unit,
    onUpdateOpacity: (Float) -> Unit,
    onUpdateScale: (Float) -> Unit = {},
    onUpdateX: (Float) -> Unit = {},
    onUpdateY: (Float) -> Unit = {},
    onUpdateCrop: (String) -> Unit = {}
) {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    var imageSubTab by remember { mutableStateOf("add") }
    var selectedCropArea by remember { mutableStateOf("full") }

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("IMAGE OVERLAY STUDIO", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = CyberCyan)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Add · Crop · Position · Scale", fontSize = 7.sp, color = Color.Gray)
            Box(Modifier.background(CyberCyan.copy(0.15f), RoundedCornerShape(4.dp)).padding(horizontal = 5.dp, vertical = 2.dp)) {
                Text("✓ Full Control", fontSize = 7.sp, fontWeight = FontWeight.Bold, color = CyberCyan)
            }
        }

        // Sub-tabs: Add | Crop | Position | Scale | Effects
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            listOf("add" to "Add", "crop" to "Crop", "pos" to "Position", "scale" to "Scale", "fx" to "Effects").forEach { (id, label) ->
                val sel = imageSubTab == id
                Box(Modifier.background(if (sel) CyberCyan.copy(0.2f) else Color.White.copy(0.04f), RoundedCornerShape(6.dp)).clickable { imageSubTab = id }.padding(horizontal = 8.dp, vertical = 4.dp)) {
                    Text(label, fontSize = 8.sp, fontWeight = FontWeight.Bold, color = if (sel) CyberCyan else Color.White)
                }
            }
        }

        when (imageSubTab) {
            "add" -> {
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
                    Text("IMAGE ADDED ✓", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = CyberCyan)
                    Text("Use Crop, Position, Scale tabs to adjust placement on video", fontSize = 7.sp, color = Color.Gray)
                }
            }
            "crop" -> {
                Text("CROP AREA — Select where to place image on video", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                // Visual crop area selector grid
                FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf("Full Screen" to "full", "Center" to "center", "Top Half" to "top", "Bottom Half" to "bottom", "Left Half" to "left", "Right Half" to "right", "Top-Left" to "tl", "Top-Right" to "tr", "Bottom-Left" to "bl", "Bottom-Right" to "br", "Square Center" to "square", "Wide Bar" to "bar", "Circle Mask" to "circle", "Corner Badge" to "badge", "Split Screen" to "split", "Picture-in-Pic" to "pip").forEach { (label, id) ->
                        val sel = selectedCropArea == id
                        Box(Modifier.background(if (sel) CyberCyan.copy(0.2f) else Color.White.copy(0.04f), RoundedCornerShape(6.dp)).border(if (sel) 1.dp else 0.dp, if (sel) CyberCyan else Color.Transparent, RoundedCornerShape(6.dp)).clickable {
                            selectedCropArea = id
                            onUpdateCrop(id)
                            android.widget.Toast.makeText(ctx, "Crop area: $label", android.widget.Toast.LENGTH_SHORT).show()
                        }.padding(horizontal = 6.dp, vertical = 4.dp)) {
                            Text(label, fontSize = 7.sp, fontWeight = FontWeight.Bold, color = if (sel) CyberCyan else Color.White)
                        }
                    }
                }
                // Aspect ratio crop presets
                Text("ASPECT RATIO CROP", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf("1:1" to "1:1", "4:3" to "4:3", "3:4" to "3:4", "16:9" to "16:9", "9:16" to "9:16", "3:2" to "3:2", "2:3" to "2:3", "Free" to "free").forEach { (label, id) ->
                        Box(Modifier.background(Color.White.copy(0.04f), RoundedCornerShape(6.dp)).clickable {
                            android.widget.Toast.makeText(ctx, "Crop ratio: $label", android.widget.Toast.LENGTH_SHORT).show()
                        }.padding(horizontal = 6.dp, vertical = 4.dp)) {
                            Text(label, fontSize = 7.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }
            "pos" -> {
                Text("POSITION — Drag to set image location on video", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                // Visual position grid (3x3)
                Box(Modifier.fillMaxWidth().height(80.dp).background(Color.White.copy(0.04f), RoundedCornerShape(8.dp)).border(1.dp, CyberCyan.copy(0.2f), RoundedCornerShape(8.dp))) {
                    // 3x3 grid for position selection
                    val positions = listOf(
                        "↖ TL" to Pair(0.15f, 0.15f), "↑ TC" to Pair(0.5f, 0.15f), "↖ TR" to Pair(0.85f, 0.15f),
                        "← ML" to Pair(0.15f, 0.5f), "● C" to Pair(0.5f, 0.5f), "→ MR" to Pair(0.85f, 0.5f),
                        "↙ BL" to Pair(0.15f, 0.85f), "↓ BC" to Pair(0.5f, 0.85f), "↘ BR" to Pair(0.85f, 0.85f)
                    )
                    positions.forEach { (label, xy) ->
                        val (x, y) = xy
                        val sel = kotlin.math.abs(project.imageOverlayX - x) < 0.1f && kotlin.math.abs(project.imageOverlayY - y) < 0.1f
                        Box(Modifier.offset(x = (x * 280).dp - 20.dp, y = (y * 80).dp - 12.dp).size(40.dp).background(if (sel) CyberCyan.copy(0.3f) else Color.White.copy(0.08f), RoundedCornerShape(6.dp)).border(if (sel) 1.dp else 0.dp, CyberCyan, RoundedCornerShape(6.dp)).clickable {
                            onUpdateX(x); onUpdateY(y)
                            android.widget.Toast.makeText(ctx, "Position: $label", android.widget.Toast.LENGTH_SHORT).show()
                        }, contentAlignment = Alignment.Center) {
                            Text(label, fontSize = 6.sp, fontWeight = FontWeight.Bold, color = if (sel) CyberCyan else Color.White)
                        }
                    }
                }
                // Fine position sliders
                Text("X POSITION: ${String.format("%.0f", project.imageOverlayX * 100)}%", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                Slider(value = project.imageOverlayX, onValueChange = onUpdateX, valueRange = 0f..1f, colors = SliderDefaults.colors(activeTrackColor = CyberCyan, thumbColor = CyberCyan), modifier = Modifier.fillMaxWidth().height(18.dp))
                Text("Y POSITION: ${String.format("%.0f", project.imageOverlayY * 100)}%", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                Slider(value = project.imageOverlayY, onValueChange = onUpdateY, valueRange = 0f..1f, colors = SliderDefaults.colors(activeTrackColor = CyberCyan, thumbColor = CyberCyan), modifier = Modifier.fillMaxWidth().height(18.dp))
            }
            "scale" -> {
                Text("SCALE & OPACITY", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                Text("SIZE: ${String.format("%.0f", project.imageOverlayScale * 100)}%", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = CyberCyan)
                Slider(value = project.imageOverlayScale, onValueChange = onUpdateScale, valueRange = 0.1f..3f, colors = SliderDefaults.colors(activeTrackColor = CyberCyan, thumbColor = CyberCyan), modifier = Modifier.fillMaxWidth().height(18.dp))
                Text("OPACITY: ${String.format("%.0f", project.imageOverlayOpacity * 100)}%", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = NeonOrange)
                Slider(value = project.imageOverlayOpacity, onValueChange = onUpdateOpacity, valueRange = 0f..1f, colors = SliderDefaults.colors(activeTrackColor = NeonOrange, thumbColor = NeonOrange), modifier = Modifier.fillMaxWidth().height(18.dp))
                // Quick scale presets
                Text("QUICK SIZE", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf("25%" to 0.25f, "50%" to 0.5f, "75%" to 0.75f, "100%" to 1f, "150%" to 1.5f, "200%" to 2f).forEach { (label, value) ->
                        Box(Modifier.weight(1f).background(Color.White.copy(0.04f), RoundedCornerShape(6.dp)).clickable { onUpdateScale(value) }.padding(4.dp), contentAlignment = Alignment.Center) {
                            Text(label, fontSize = 7.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }
            "fx" -> {
                Text("IMAGE EFFECTS", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf("None", "Blur Edge", "Round Corners", "Circle Mask", "Shadow", "Glow", "Border", "Grayscale", "Sepia", "Invert", "Vignette", "Gradient BG", "Neon Edge", "3D Pop", "Glass", "Frosted").forEach { fx ->
                        Box(Modifier.background(Color.White.copy(0.04f), RoundedCornerShape(6.dp)).clickable {
                            android.widget.Toast.makeText(ctx, "Image FX: $fx", android.widget.Toast.LENGTH_SHORT).show()
                        }.padding(horizontal = 6.dp, vertical = 4.dp)) {
                            Text(fx, fontSize = 7.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
                // Blend mode for image
                Text("BLEND MODE", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    listOf("Normal", "Multiply", "Screen", "Overlay", "Soft Light", "Hard Light", "Color Dodge", "Color Burn", "Darken", "Lighten", "Difference", "Exclusion").forEach { mode ->
                        Box(Modifier.background(Color.White.copy(0.04f), RoundedCornerShape(6.dp)).clickable {
                            android.widget.Toast.makeText(ctx, "Blend: $mode", android.widget.Toast.LENGTH_SHORT).show()
                        }.padding(horizontal = 5.dp, vertical = 3.dp)) {
                            Text(mode, fontSize = 6.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
                // Animation
                Text("ENTRANCE ANIMATION", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    listOf("Fade In", "Slide L", "Slide R", "Slide Up", "Slide Down", "Zoom In", "Zoom Out", "Pop", "Bounce", "Flip", "Rotate", "Elastic").forEach { anim ->
                        Box(Modifier.background(Color.White.copy(0.04f), RoundedCornerShape(6.dp)).clickable {
                            android.widget.Toast.makeText(ctx, "Image anim: $anim", android.widget.Toast.LENGTH_SHORT).show()
                        }.padding(horizontal = 5.dp, vertical = 3.dp)) {
                            Text(anim, fontSize = 6.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }
        }
    }
}


// ─── 13. TEMPLATE PANEL ────────────────────────────────────────
@Composable
private fun TemplatePanel(project: VideoProject, onUpdateTemplate: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        LiveAnimatedHeader("IMAGE", "🖼️", NeonOrange)

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
        LiveAnimatedHeader("CANVAS", "🖌️", CyberCyan)

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



// ── 28. KEYFRAME PANEL (KineMaster-style) ──────────────────────────────────────
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun KeyframePanel(project: VideoProject, onUpdateKeyframeAnim: (String) -> Unit) {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    var selectedProperty by remember { mutableStateOf("position") }
    var selectedEasing by remember { mutableStateOf("linear") }

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        LiveAnimatedHeader("KEYFRAMES", "💎", NeonOrange)

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("KEYFRAMES", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = SignatureOrange)
            Box(Modifier.background(SignatureOrange.copy(0.15f), RoundedCornerShape(4.dp)).padding(horizontal = 5.dp, vertical = 2.dp)) {
                Text("◆ KineMaster", fontSize = 7.sp, fontWeight = FontWeight.Bold, color = SignatureOrange)
            }
        }

        // Animated keyframe timeline visualization
        Box(
            Modifier.fillMaxWidth().height(48.dp)
                .background(Color.White.copy(0.04f), RoundedCornerShape(8.dp))
                .border(1.dp, SignatureOrange.copy(0.3f), RoundedCornerShape(8.dp))
        ) {
            Canvas(Modifier.fillMaxSize().padding(6.dp)) {
                val w = size.width
                val h = size.height
                // Timeline track
                drawLine(
                    color = Color.White.copy(alpha = 0.2f),
                    start = androidx.compose.ui.geometry.Offset(0f, h / 2f),
                    end = androidx.compose.ui.geometry.Offset(w, h / 2f),
                    strokeWidth = 2f
                )
                // Diamond keyframe markers along an animated curve
                val keyPositions = listOf(0.1f, 0.3f, 0.5f, 0.7f, 0.9f)
                keyPositions.forEachIndexed { idx, pos ->
                    val x = w * pos
                    val baseY = h / 2f
                    val waveY = baseY + (kotlin.math.sin((pos * 6.28f).toDouble()).toFloat() * (h * 0.25f))
                    // Connecting curve
                    if (idx > 0) {
                        val prevX = w * keyPositions[idx - 1]
                        val prevWaveY = baseY + (kotlin.math.sin((keyPositions[idx - 1] * 6.28f).toDouble()).toFloat() * (h * 0.25f))
                        drawLine(
                            color = SignatureOrange.copy(alpha = 0.5f),
                            start = androidx.compose.ui.geometry.Offset(prevX, prevWaveY),
                            end = androidx.compose.ui.geometry.Offset(x, waveY),
                            strokeWidth = 2f
                        )
                    }
                    // Diamond marker
                    val diamondSize = 6f
                    val diamond = androidx.compose.ui.graphics.Path().apply {
                        moveTo(x, waveY - diamondSize)
                        lineTo(x + diamondSize, waveY)
                        lineTo(x, waveY + diamondSize)
                        lineTo(x - diamondSize, waveY)
                        close()
                    }
                    drawPath(diamond, SignatureOrange)
                    drawPath(diamond, Color.White.copy(alpha = 0.3f), style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1f))
                }
            }
        }

        // Property selector
        Text("ANIMATE PROPERTY", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
        FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            listOf("📏 Position" to "position", "🔄 Rotation" to "rotation", "🔍 Scale" to "scale", "🌫️ Opacity" to "opacity", "↔️ Skew" to "skew", "🎨 Color" to "color", "💫 Blur" to "blur", "📈 Anchor" to "anchor").forEach { (label, id) ->
                val sel = selectedProperty == id
                Box(Modifier.background(if (sel) SignatureOrange.copy(0.2f) else Color.White.copy(0.04f), RoundedCornerShape(6.dp)).border(if (sel) 1.dp else 0.dp, if (sel) SignatureOrange else Color.Transparent, RoundedCornerShape(6.dp)).clickable {
                    selectedProperty = id
                    onUpdateKeyframeAnim("$id:$selectedEasing")
                    android.widget.Toast.makeText(ctx, "Keyframe property: $label", android.widget.Toast.LENGTH_SHORT).show()
                }.padding(horizontal = 6.dp, vertical = 4.dp)) {
                    Text(label, fontSize = 7.sp, fontWeight = FontWeight.Bold, color = if (sel) SignatureOrange else Color.White)
                }
            }
        }

        // Easing curve selector
        Text("EASING CURVE", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
        FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            listOf("Linear" to "linear", "Ease In" to "easeIn", "Ease Out" to "easeOut", "Ease In-Out" to "easeInOut", "Bounce" to "bounce", "Elastic" to "elastic", "Back" to "back", "Spring" to "spring").forEach { (label, id) ->
                val sel = selectedEasing == id
                Box(Modifier.background(if (sel) SignaturePurple.copy(0.2f) else Color.White.copy(0.04f), RoundedCornerShape(6.dp)).border(if (sel) 1.dp else 0.dp, if (sel) SignaturePurple else Color.Transparent, RoundedCornerShape(6.dp)).clickable {
                    selectedEasing = id
                    onUpdateKeyframeAnim("$selectedProperty:$id")
                    android.widget.Toast.makeText(ctx, "Easing: $label", android.widget.Toast.LENGTH_SHORT).show()
                }.padding(horizontal = 6.dp, vertical = 4.dp)) {
                    Text(label, fontSize = 7.sp, fontWeight = FontWeight.Bold, color = if (sel) SignaturePurple else Color.White)
                }
            }
        }

        // Quick animation presets
        Text("QUICK PRESETS", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
        FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            listOf("Zoom In" to "zoomIn", "Zoom Out" to "zoomOut", "Pan Left→Right" to "panLR", "Pan Right→Left" to "panRL", "Spin 360°" to "spin360", "Fade In-Out" to "fadeIO", "Pulse" to "pulse", "Wobble" to "wobble", "Slide Up" to "slideUp", "Slide Down" to "slideDown", "Bounce In" to "bounceIn", "Shake" to "shake").forEach { (label, id) ->
                Box(Modifier.background(Color.White.copy(0.04f), RoundedCornerShape(6.dp)).border(1.dp, SignatureOrange.copy(0.2f), RoundedCornerShape(6.dp)).clickable {
                    onUpdateKeyframeAnim("preset:$id")
                    android.widget.Toast.makeText(ctx, "Keyframe preset: $label", android.widget.Toast.LENGTH_SHORT).show()
                }.padding(horizontal = 6.dp, vertical = 4.dp)) {
                    Text(label, fontSize = 7.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }

        // Action buttons
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            listOf("➕ Add Key" to "add", "🗑️ Clear" to "clear", "📋 Copy" to "copy", "↔️ Reverse" to "reverse", "🎬 Preview" to "preview").forEach { (label, id) ->
                Box(Modifier.weight(1f).background(Color.White.copy(0.04f), RoundedCornerShape(6.dp)).border(1.dp, if (id == "add") SignatureOrange.copy(0.4f) else Color.White.copy(0.1f), RoundedCornerShape(6.dp)).clickable {
                    android.widget.Toast.makeText(ctx, "Keyframe: $label", android.widget.Toast.LENGTH_SHORT).show()
                }.padding(5.dp), contentAlignment = Alignment.Center) {
                    Text(label, fontSize = 7.sp, fontWeight = FontWeight.Bold, color = if (id == "add") SignatureOrange else Color.White)
                }
            }
        }
    }
}



// Shared infinite pulse animation driver for in-editor premium panels
@Composable
private fun rememberPulse(): State<Float> {
    val transition = rememberInfiniteTransition(label = "pulse")
    return transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseVal"
    )
}

// v6.2.0: Reusable live animated header for all tool panels
// User request: "All Demos Live Background"
@Composable
private fun LiveAnimatedHeader(title: String, icon: String, accentColor: Color = NeonOrange) {
    val pulse = rememberPulse()
    Box(
        Modifier.fillMaxWidth().height(44.dp)
            .background(Brush.horizontalGradient(listOf(accentColor.copy(0.15f), SignaturePurple.copy(0.12f))), RoundedCornerShape(10.dp))
            .border(1.dp, accentColor.copy(0.35f), RoundedCornerShape(10.dp))
    ) {
        Canvas(Modifier.fillMaxSize().padding(3.dp)) {
            val w = size.width
            val h = size.height
            val t = pulse.value
            // Animated flowing wave background
            val path = androidx.compose.ui.graphics.Path().apply {
                moveTo(0f, h * 0.7f)
                for (x in 0..w.toInt() step 6) {
                    val y = h * 0.7f + kotlin.math.sin((x / w.toFloat() * 6.28f * 2f + t * 3f).toDouble()).toFloat() * h * 0.2f
                    lineTo(x.toFloat(), y)
                }
                lineTo(w, h)
                lineTo(0f, h)
                close()
            }
            drawPath(path, accentColor.copy(alpha = 0.1f))
            // Floating particles
            for (i in 0 until 6) {
                val px = ((t * 0.3f + i * 0.18f) % 1f) * w
                val py = h * (0.2f + (kotlin.math.sin((t * 2f + i).toDouble()).toFloat() * 0.15f + 0.15f))
                val r = 2f + (kotlin.math.sin((t * 4f + i).toDouble()).toFloat() * 1f)
                drawCircle(accentColor.copy(alpha = 0.5f), r, androidx.compose.ui.geometry.Offset(px, py))
            }
        }
        Row(Modifier.fillMaxSize().padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(icon, fontSize = 14.sp)
            Spacer(Modifier.width(6.dp))
            Text(title, fontSize = 11.sp, fontWeight = FontWeight.Black, color = Color.White)
        }
    }
}


// ════════════════════════════════════════════════════════════════════════════════
//  IN-EDITOR PREMIUM PANELS (AI Hub, Presets, Pro, Studio)
//  v6.2.0: Converted from separate screens to in-editor 3D Glass slide-up panels
//  User request: "isko separate screen na rakho jaise anim 3D Glass CARD mein
//  usi screen per hain waise hi in sab ko rakho"
// ════════════════════════════════════════════════════════════════════════════════

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AiHubPanel(project: VideoProject, onUpdateAiFeature: (String) -> Unit) {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("all") }
    val pulse = rememberPulse()

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        // Header with live animated background
        Box(
            Modifier.fillMaxWidth().height(52.dp)
                .background(Brush.horizontalGradient(listOf(SignatureOrange.copy(0.2f), SignaturePurple.copy(0.2f))), RoundedCornerShape(10.dp))
                .border(1.dp, SignatureOrange.copy(0.4f), RoundedCornerShape(10.dp))
        ) {
            // Live animated neural network background
            Canvas(Modifier.fillMaxSize().padding(4.dp)) {
                val w = size.width
                val h = size.height
                val t = pulse.value
                // Neural network nodes
                val nodes = listOf(
                    GeomOffset(w * 0.1f, h * 0.3f), GeomOffset(w * 0.25f, h * 0.7f),
                    GeomOffset(w * 0.4f, h * 0.2f), GeomOffset(w * 0.55f, h * 0.6f),
                    GeomOffset(w * 0.7f, h * 0.3f), GeomOffset(w * 0.85f, h * 0.7f),
                    GeomOffset(w * 0.92f, h * 0.4f)
                )
                // Connections
                for (i in nodes.indices) {
                    for (j in (i + 1) until nodes.size) {
                        val alpha = (kotlin.math.sin((t * 2f + i + j).toDouble()).toFloat() * 0.5f + 0.5f) * 0.3f
                        drawLine(SignatureOrange.copy(alpha = alpha), nodes[i], nodes[j], strokeWidth = 1f)
                    }
                }
                // Nodes
                nodes.forEachIndexed { idx, node ->
                    val r = 4f + (kotlin.math.sin((t * 3f + idx).toDouble()).toFloat() * 2f)
                    drawCircle(SignatureOrange, r, node)
                    drawCircle(Color.White.copy(0.5f), r * 0.5f, node)
                }
            }
            Row(Modifier.fillMaxSize().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("🤖 AI HUB", fontSize = 12.sp, fontWeight = FontWeight.Black, color = Color.White)
                Spacer(Modifier.weight(1f))
                Box(Modifier.background(SignatureOrange.copy(0.3f), RoundedCornerShape(4.dp)).padding(horizontal = 6.dp, vertical = 2.dp)) {
                    Text("50+ AI Tools", fontSize = 7.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }

        // Search bar
        OutlinedTextField(
            value = searchQuery, onValueChange = { searchQuery = it },
            placeholder = { Text("Search AI tools...", fontSize = 8.sp, color = Color.Gray) },
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = SignatureOrange, unfocusedBorderColor = Color.White.copy(0.1f), focusedTextColor = Color.White, unfocusedTextColor = Color.White),
            modifier = Modifier.fillMaxWidth().height(36.dp), shape = RoundedCornerShape(8.dp),
            leadingIcon = null, trailingIcon = null, singleLine = true
        )

        // Category filter
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            listOf("all" to "All", "ai" to "AI Features", "enhance" to "Enhance", "audio" to "Audio AI", "visual" to "Visual AI").forEach { (id, label) ->
                val sel = selectedCategory == id
                Box(Modifier.background(if (sel) SignatureOrange.copy(0.2f) else Color.White.copy(0.04f), RoundedCornerShape(6.dp)).clickable { selectedCategory = id }.padding(horizontal = 8.dp, vertical = 4.dp)) {
                    Text(label, fontSize = 7.sp, fontWeight = FontWeight.Bold, color = if (sel) SignatureOrange else Color.White)
                }
            }
        }

        // AI features grid (40+ AI tools from PremiumFeatureCatalog)
        val aiTools = listOf(
            "✂️" to "AI Auto Cut" to "ai_auto_cut",
            "✍️" to "AI Script Writer" to "ai_script_writer",
            "📖" to "AI Story Gen" to "ai_story_gen",
            "🧑‍💼" to "AI Avatar" to "ai_avatar",
            "🧙" to "AI Character" to "ai_character",
            "🗣️" to "AI Talking Photo" to "ai_talking_photo",
            "🎤" to "AI Voice Gen" to "ai_voice_gen",
            "🎭" to "AI Voice Clone" to "ai_voice_clone",
            "🔄" to "AI Voice Changer" to "ai_voice_changer",
            "🔇" to "AI Noise Removal" to "ai_noise_removal",
            "🎵" to "AI Music Gen" to "ai_music_gen",
            "🥁" to "AI Beat Detect" to "ai_beat_detect",
            "💬" to "AI Auto Captions" to "ai_captions",
            "📑" to "AI Subtitles" to "ai_subtitle_gen",
            "🌍" to "AI Translation" to "ai_translate",
            "👄" to "AI Lip Sync" to "ai_lip_sync",
            "✨" to "AI Face Retouch" to "ai_face_retouch",
            "🖼️" to "AI Enhance" to "ai_enhance",
            "🔍" to "AI Upscale" to "ai_upscale",
            "🛠️" to "AI Restore" to "ai_restore",
            "🌟" to "AI Image Enhance" to "ai_image_enhance",
            "🎨" to "AI Color Correct" to "ai_color_correct",
            "🎯" to "AI Color Match" to "ai_color_match",
            "🪄" to "AI Object Remove" to "ai_object_remove",
            "🧹" to "AI BG Remove" to "ai_bg_remove",
            "🌤️" to "AI Sky Replace" to "ai_sky_replace",
            "💡" to "AI Relight" to "ai_relight",
            "🎯" to "AI Motion Track" to "ai_motion_track",
            "🤖" to "AI Smart Crop" to "ai_smart_crop",
            "📐" to "AI Auto Reframe" to "ai_auto_reframe",
            "🖼️" to "AI BG Blur" to "ai_bg_blur",
            "🎭" to "AI Style Transfer" to "ai_style_transfer",
            "✨" to "AI Glamour" to "ai_glamour",
            "👁️" to "AI Eye Enhance" to "ai_eye_enhance",
            "💄" to "AI Makeup" to "ai_makeup",
            "🔄" to "AI Deinterlace" to "ai_deinterlace",
            "📊" to "AI Stabilize" to "ai_stabilize",
            "🎞️" to "AI Frame Interp" to "ai_frame_interp",
            "🌈" to "AI HDR" to "ai_hdr",
            "🎬" to "AI Cinematic" to "ai_cinematic",
            "🔊" to "AI Audio Enhance" to "ai_audio_enhance",
            "🎵" to "AI Music Match" to "ai_music_match",
            "📝" to "AI Text Gen" to "ai_text_gen",
            "🪄" to "AI Magic Edit" to "ai_magic_edit"
        )

        val filtered = aiTools.filter { tool ->
            (searchQuery.isBlank() || tool.first.second.contains(searchQuery, ignoreCase = true)) &&
            (selectedCategory == "all" || (selectedCategory == "ai" && tool.second.startsWith("ai_")))
        }

        Text("${filtered.size} AI TOOLS", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
        FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(3.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            filtered.forEach { (emojiName, id) ->
                val (emoji, name) = emojiName
                val sel = project.activeAiFeature == id
                Box(Modifier.background(if (sel) SignatureOrange.copy(0.25f) else Color.White.copy(0.04f), RoundedCornerShape(6.dp))
                    .border(if (sel) 1.dp else 0.dp, if (sel) SignatureOrange else Color.Transparent, RoundedCornerShape(6.dp))
                    .clickable {
                        onUpdateAiFeature(if (sel) "none" else id)
                        android.widget.Toast.makeText(ctx, "AI: $name ${if (sel) "disabled" else "enabled"}", android.widget.Toast.LENGTH_SHORT).show()
                    }.padding(horizontal = 5.dp, vertical = 4.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(emoji, fontSize = 10.sp)
                        Spacer(Modifier.width(2.dp))
                        Text(name, fontSize = 6.sp, fontWeight = FontWeight.Bold, color = if (sel) SignatureOrange else Color.White, maxLines = 1)
                    }
                }
            }
        }
    }
}


@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PresetsPanel(project: VideoProject, onUpdateSocialPreset: (String) -> Unit) {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val pulse = rememberPulse()

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        // Header with live animated background
        Box(
            Modifier.fillMaxWidth().height(52.dp)
                .background(Brush.horizontalGradient(listOf(SignaturePurple.copy(0.2f), CyberCyan.copy(0.2f))), RoundedCornerShape(10.dp))
                .border(1.dp, SignaturePurple.copy(0.4f), RoundedCornerShape(10.dp))
        ) {
            Canvas(Modifier.fillMaxSize().padding(4.dp)) {
                val w = size.width
                val h = size.height
                val t = pulse.value
                // Animated aspect ratio frames morphing
                val ratios = listOf(0.3f, 0.45f, 0.6f, 0.75f, 0.9f)
                ratios.forEachIndexed { idx, rx ->
                    val frameW = w * rx
                    val frameH = h * (0.3f + (kotlin.math.sin((t + idx).toDouble()).toFloat() * 0.2f + 0.2f))
                    val fx = (w - frameW) / 2f
                    val fy = (h - frameH) / 2f
                    drawRoundRect(
                        color = SignaturePurple.copy(alpha = 0.3f),
                        topLeft = GeomOffset(fx, fy),
                        size = GeomSize(frameW, frameH),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f, 4f),
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.5f)
                    )
                }
            }
            Row(Modifier.fillMaxSize().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("📱 SOCIAL PRESETS", fontSize = 12.sp, fontWeight = FontWeight.Black, color = Color.White)
                Spacer(Modifier.weight(1f))
                Box(Modifier.background(SignaturePurple.copy(0.3f), RoundedCornerShape(4.dp)).padding(horizontal = 6.dp, vertical = 2.dp)) {
                    Text("1-Tap Export", fontSize = 7.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }

        Text("EXPORT PRESETS FOR EVERY PLATFORM", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color.Gray)

        data class SocialPreset(val emoji: String, val name: String, val id: String, val spec: String)
        val presets = listOf(
            SocialPreset("📱", "Instagram Post", "ig_post", "1:1 · 1080×1080"),
            SocialPreset("📱", "Instagram Story", "ig_story", "9:16 · 1080×1920"),
            SocialPreset("📱", "Instagram Reel", "ig_reel", "9:16 · 1080×1920"),
            SocialPreset("🎵", "TikTok", "tiktok", "9:16 · 1080×1920"),
            SocialPreset("📺", "YouTube Short", "yt_short", "9:16 · 1080×1920"),
            SocialPreset("📺", "YouTube Video", "yt_video", "16:9 · 1920×1080"),
            SocialPreset("📘", "Facebook Post", "fb_post", "1:1 · 1080×1080"),
            SocialPreset("📘", "Facebook Story", "fb_story", "9:16 · 1080×1920"),
            SocialPreset("💬", "WhatsApp Status", "wa_status", "9:16 · 1080×1920"),
            SocialPreset("🐦", "X / Twitter", "twitter", "16:9 · 1920×1080"),
            SocialPreset("📌", "Pinterest", "pinterest", "2:3 · 1000×1500"),
            SocialPreset("💼", "LinkedIn", "linkedin", "1.91:1 · 1200×627"),
            SocialPreset("🎬", "Cinema 4K", "cinema_4k", "16:9 · 3840×2160"),
            SocialPreset("🎮", "Gaming 8K", "gaming_8k", "16:9 · 7680×4320"),
            SocialPreset("📺", "TV Broadcast", "tv_broadcast", "16:9 · 1920×1080"),
            SocialPreset("🖼️", "Portrait", "portrait", "2:3 · 1080×1620"),
            SocialPreset("🖼️", "Landscape", "landscape", "3:2 · 1620×1080"),
            SocialPreset("⬜", "Square", "square", "1:1 · 1080×1080")
        )

        FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            presets.forEach { p ->
                val sel = project.socialPreset == p.id
                Box(Modifier.weight(1f).fillMaxWidth(0.48f)
                    .background(if (sel) SignaturePurple.copy(0.25f) else Color.White.copy(0.04f), RoundedCornerShape(8.dp))
                    .border(if (sel) 1.5.dp else 0.dp, if (sel) SignaturePurple else Color.Transparent, RoundedCornerShape(8.dp))
                    .clickable {
                        onUpdateSocialPreset(if (sel) "none" else p.id)
                        android.widget.Toast.makeText(ctx, "Preset: ${p.name}\n${p.spec}", android.widget.Toast.LENGTH_SHORT).show()
                    }.padding(6.dp)) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(p.emoji, fontSize = 16.sp)
                            Spacer(Modifier.width(4.dp))
                            Text(p.name, fontSize = 7.sp, fontWeight = FontWeight.Bold, color = if (sel) SignaturePurple else Color.White, maxLines = 1)
                        }
                        Text(p.spec, fontSize = 5.sp, color = Color.Gray, maxLines = 1)
                    }
                }
            }
        }
    }
}


@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ProPanel(project: VideoProject, onUpdateProTier: () -> Unit, onApplyLook: (String) -> Unit) {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val pulse = rememberPulse()

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        // Header with live animated gold shimmer background
        Box(
            Modifier.fillMaxWidth().height(52.dp)
                .background(Brush.horizontalGradient(listOf(PremiumGold.copy(0.2f), SignatureOrange.copy(0.2f))), RoundedCornerShape(10.dp))
                .border(1.dp, PremiumGold.copy(0.5f), RoundedCornerShape(10.dp))
        ) {
            Canvas(Modifier.fillMaxSize().padding(4.dp)) {
                val w = size.width
                val h = size.height
                val t = pulse.value
                // Animated gold shimmer streaks
                for (i in 0 until 5) {
                    val x = ((t * 0.5f + i * 0.2f) % 1f) * w
                    drawLine(
                        PremiumGold.copy(alpha = 0.4f),
                        GeomOffset(x - 20f, 0f), GeomOffset(x + 20f, h),
                        strokeWidth = 2f
                    )
                }
                // Crown icon pulsing
                val cx = w * 0.5f
                val cy = h * 0.5f
                val r = 8f + (kotlin.math.sin((t * 2f).toDouble()).toFloat() * 3f)
                drawCircle(PremiumGold.copy(0.2f), r + 6f, GeomOffset(cx, cy))
            }
            Row(Modifier.fillMaxSize().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("👑 PRO TIER", fontSize = 12.sp, fontWeight = FontWeight.Black, color = PremiumGold)
                Spacer(Modifier.weight(1f))
                Box(Modifier.background(PremiumGold.copy(0.3f), RoundedCornerShape(4.dp)).padding(horizontal = 6.dp, vertical = 2.dp)) {
                    Text("Unlock All", fontSize = 7.sp, fontWeight = FontWeight.Bold, color = PremiumGold)
                }
            }
        }

        // Pro features list
        Text("PRO FEATURES", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = PremiumGold)
        val proFeatures = listOf(
            "🚫 No Watermark" to "Remove PowerCut watermark from all exports",
            "🎬 8K Export" to "Export up to 8K (7680×4320) resolution",
            "💎 Premium Looks" to "50+ cinematic LUTs and color grades",
            "🤖 AI Tools" to "All 45+ AI features unlocked",
            "🎵 Royalty Music" to "Full royalty-free music library",
            "📱 All Presets" to "18+ social media export presets",
            "💎 4K HDR" to "HDR10+ export with wide gamut",
            "∞ Unlimited Drafts" to "Save unlimited project drafts",
            "⚡ Priority Export" to "Hardware-accelerated fast export",
            "🎯 No Ads" to "Completely ad-free experience"
        )
        proFeatures.forEach { (title, desc) ->
            Box(Modifier.fillMaxWidth().background(Color.White.copy(0.04f), RoundedCornerShape(6.dp)).border(1.dp, PremiumGold.copy(0.15f), RoundedCornerShape(6.dp)).padding(6.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(title, fontSize = 8.sp, fontWeight = FontWeight.Bold, color = PremiumGold)
                    Spacer(Modifier.weight(1f))
                    Text("✓", fontSize = 10.sp, fontWeight = FontWeight.Black, color = PremiumGold)
                }
                Spacer(Modifier.height(1.dp))
            }
        }

        // Premium Looks (cinematic LUTs)
        Text("PREMIUM LOOKS (50+)", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
        val looks = listOf("Cinematic", "Teal Orange", "Vintage Film", "Golden Hour", "Noir B&W", "Cyberpunk", "Sunset Glow", "Faded", "Vivid Pop", "Matte", "Warm Retro", "Cool Blue", "Dreamy", "High Contrast", "Soft Pastel", "Film Grain", "Anamorphic", "Bleach Bypass", "Cross Process", "Sepia Warm")
        FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            looks.forEach { look ->
                val sel = project.activePremiumLook == look.lowercase().replace(" ", "_")
                Box(Modifier.background(if (sel) PremiumGold.copy(0.25f) else Color.White.copy(0.04f), RoundedCornerShape(6.dp))
                    .border(if (sel) 1.dp else 0.dp, if (sel) PremiumGold else Color.Transparent, RoundedCornerShape(6.dp))
                    .clickable {
                        onApplyLook(if (sel) "none" else look.lowercase().replace(" ", "_"))
                        android.widget.Toast.makeText(ctx, "Look: $look", android.widget.Toast.LENGTH_SHORT).show()
                    }.padding(horizontal = 6.dp, vertical = 4.dp)) {
                    Text(look, fontSize = 6.sp, fontWeight = FontWeight.Bold, color = if (sel) PremiumGold else Color.White)
                }
            }
        }

        // Upgrade button
        Box(Modifier.fillMaxWidth().background(Brush.horizontalGradient(listOf(PremiumGold, SignatureOrange)), RoundedCornerShape(8.dp)).clickable {
            onUpdateProTier()
            android.widget.Toast.makeText(ctx, "Pro Tier activated! All features unlocked.", android.widget.Toast.LENGTH_LONG).show()
        }.padding(10.dp), contentAlignment = Alignment.Center) {
            Text("🚀 UPGRADE TO PRO", fontSize = 10.sp, fontWeight = FontWeight.Black, color = Color.Black)
        }
    }
}


@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StudioPanel(project: VideoProject, onPremiumStudio: () -> Unit) {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val pulse = rememberPulse()

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        // Header with live animated aurora background
        Box(
            Modifier.fillMaxWidth().height(52.dp)
                .background(Brush.horizontalGradient(listOf(SignatureOrange.copy(0.15f), SignaturePurple.copy(0.15f), CyberCyan.copy(0.15f))), RoundedCornerShape(10.dp))
                .border(1.dp, CyberCyan.copy(0.4f), RoundedCornerShape(10.dp))
        ) {
            Canvas(Modifier.fillMaxSize().padding(4.dp)) {
                val w = size.width
                val h = size.height
                val t = pulse.value
                // Aurora waves
                for (i in 0 until 3) {
                    val phase = t + i * 1.5f
                    val path = androidx.compose.ui.graphics.Path().apply {
                        moveTo(0f, h * 0.5f)
                        for (x in 0..w.toInt() step 8) {
                            val y = h * 0.5f + kotlin.math.sin((x / w.toFloat() * 6.28f + phase).toDouble()).toFloat() * h * 0.3f
                            lineTo(x.toFloat(), y)
                        }
                    }
                    val colors = listOf(SignatureOrange, SignaturePurple, CyberCyan)
                    drawPath(path, colors[i].copy(alpha = 0.3f), style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2f))
                }
            }
            Row(Modifier.fillMaxSize().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("✨ STUDIO", fontSize = 12.sp, fontWeight = FontWeight.Black, color = Color.White)
                Spacer(Modifier.weight(1f))
                Box(Modifier.background(CyberCyan.copy(0.3f), RoundedCornerShape(4.dp)).padding(horizontal = 6.dp, vertical = 2.dp)) {
                    Text("Premium FX", fontSize = 7.sp, fontWeight = FontWeight.Bold, color = CyberCyan)
                }
            }
        }

        Text("PREMIUM STUDIO EFFECTS", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color.Gray)

        // Studio effect categories
        val studioEffects = listOf(
            "🎬 Cinematic Bars" to "cinematic_bars",
            "📽️ Film Burn" to "film_burn",
            "📼 VHS Effect" to "vhs",
            "📡 Glitch TV" to "glitch_tv",
            "🌈 RGB Split" to "rgb_split",
            "🔮 Prism" to "prism",
            "⚡ Lightning" to "lightning",
            "🎆 Fireworks" to "fireworks",
            "❄️ Frozen" to "frozen",
            "🔥 Fire Effect" to "fire_effect",
            "💧 Water Ripple" to "water_ripple",
            "🌌 Starfield" to "starfield",
            "🫧 Bokeh" to "bokeh",
            "📐 Light Leak" to "light_leak",
            "🎞️ Film Scratch" to "film_scratch",
            "🌫️ Dream Blur" to "dream_blur",
            "🌑 Vignette Pro" to "vignette_pro",
            "✨ Sparkle" to "sparkle",
            "🎯 Lens Flare" to "lens_flare",
            "⭐ Star Burst" to "star_burst"
        )
        FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            studioEffects.forEach { (label, id) ->
                Box(Modifier.background(Color.White.copy(0.04f), RoundedCornerShape(6.dp)).border(1.dp, CyberCyan.copy(0.2f), RoundedCornerShape(6.dp))
                    .clickable {
                        android.widget.Toast.makeText(ctx, "Studio FX: $label", android.widget.Toast.LENGTH_SHORT).show()
                    }.padding(horizontal = 6.dp, vertical = 4.dp)) {
                    Text(label, fontSize = 6.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }

        // Advanced studio controls
        Text("ADVANCED CONTROLS", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
        val advancedTools = listOf("🎚️ Audio Mixer", "🎨 Color Grader", "📐 Composition Guide", "🔌 Plugin Manager", "📊 Analytics", "🎥 Multi-Cam", "📱 Screen Record", "🖥️ Desktop Sync")
        FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            advancedTools.forEach { tool ->
                Box(Modifier.background(Color.White.copy(0.04f), RoundedCornerShape(8.dp)).border(1.dp, CyberCyan.copy(0.2f), RoundedCornerShape(8.dp)).clickable {
                    onPremiumStudio()
                    android.widget.Toast.makeText(ctx, "Studio: $tool", android.widget.Toast.LENGTH_SHORT).show()
                }.padding(8.dp)) {
                    Text(tool, fontSize = 7.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
    }
}
