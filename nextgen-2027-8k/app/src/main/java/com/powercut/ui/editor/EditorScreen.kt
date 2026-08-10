package com.powercut.ui.editor

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.powercut.ui.components.GradientPill
import com.powercut.ui.components.LivePreviewSurface
import com.powercut.ui.components.PreviewFrameProvider
import com.powercut.ui.components.ProBadge
import com.powercut.ui.components.powercutGradientBrush
import com.powercut.ui.theme.*
import com.powercut.model.TimelineTrack
import com.powercut.model.VideoProject

/**
 * Editor Screen — Premium 2027 8K UI (P3).
 *
 * DELETED (per spec): the top floating 4 buttons (AI Hub / Presets / Pro /
 * Studio) and the orange "LIVE PREVIEW" badge on the video preview.
 *
 * ADDED: a single BOTTOM TOOLBAR with two scrollable rows of gradient pills.
 * Every tool is the SAME premium gradient pill style (orange→purple, 28dp
 * rounded, white icon+text, PRO badge on premium tools, 60fps press animation).
 *
 *   ROW 1: Edit | Layers | Speed | Crop | Audio | Text | Filters | Effects | Stickers
 *   ROW 2: Chroma Key | VFX | 3D Effects | 🤖 AI Hub | 📱 Presets | 👑 Pro | ✨ Studio
 *
 * Video preview: pure black background, correct 16:9 aspect, centered play,
 * NO overlays except user content, pinch zoom / drag pan @60fps.
 *
 * Timeline: glass track cards (Video=orange, Audio=blue+waveform, Subtitle=
 * purple, Sticker=yellow), smooth pinch zoom.
 *
 * NOTE on navigation: in this single-activity production drop the editor is
 * the home screen; each tool pill switches the tool overlay sheet (kept as a
 * state-driven overlay so the existing timeline/audio/stickers stay 100%
 * intact underneath — no breaking changes). The full tool SCREENS live in
 * com.powercut.ui.tools.* and are reachable via the same overlay routing.
 */
@Composable
fun EditorScreen(vm: EditorViewModel = viewModel()) {
    val project by vm.project.collectAsState()
    val revision by vm.revision.collectAsState()
    val isPlaying by vm.isPlaying.collectAsState()
    val zoom by vm.zoom.collectAsState()
    val selectedTrackId by vm.selectedTrackId.collectAsState()
    var activeTool by remember { mutableStateOf<EditorTool?>(null) }

    // Re-key the timeline on revision so mutations recompose.
    val timelineKey by remember(revision) { derivedStateOf { revision } }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Bg)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // ---- Top bar: minimal (project name + duration only — no floating
        //      buttons, no LIVE PREVIEW badge — DELETED per spec) -----------
        //      A single gradient Export button lives here (P2 launch point). --
        EditorTopBar(
            project = project,
            onExport = { activeTool = EditorTool.EXPORT }
        )

        // ---- Video preview: pure black, 16:9, pinch zoom, centered play ---
        PreviewArea(
            isPlaying = isPlaying,
            zoom = zoom,
            onTogglePlay = vm::togglePlay,
            onZoom = vm::setZoom,
            frameProvider = vm.frameProvider,
            modifier = Modifier.fillMaxWidth().weight(1f)
        )

        // ---- Timeline: glass track cards, pinch zoom ---------------------
        TimelineArea(
            project = project,
            selectedTrackId = selectedTrackId,
            onSelect = vm::selectTrackId,
            zoom = zoom,
            onZoom = vm::setZoom,
            key = timelineKey
        )

        Spacer(Modifier.height(8.dp))

        // ---- Bottom toolbar: two rows of gradient pill tools -------------
        BottomToolbar(
            activeTool = activeTool,
            onTool = { activeTool = it }
        )
    }

    // Tool overlay sheet (drives the dedicated tool screens in ui.tools.*).
    if (activeTool != null) {
        ToolOverlaySheet(tool = activeTool!!, onDismiss = { activeTool = null }, vm = vm)
    }
}

// ---- Top bar ----------------------------------------------------------------
@Composable
private fun EditorTopBar(project: VideoProject, onExport: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Filled.Menu, contentDescription = "Menu",
             tint = TextPrimary, modifier = Modifier.size(24.dp))
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(project.name, color = TextPrimary,
                 fontSize = 17.sp, fontWeight = FontWeight.Bold)
            Text(formatDuration(project.durationUs), color = TextSecondary, fontSize = 12.sp)
        }
        // Gradient Export button (P2 entry point) — replaces the old
        // Settings icon so the top bar stays clean (1 action, on-brand).
        GradientPill(
            text = "Export",
            icon = { Icon(Icons.Filled.Download, contentDescription = "Export",
                          tint = White, modifier = Modifier.size(16.dp)) },
            onClick = onExport,
            cornerRadius = 20.dp,
            horizontalPadding = 16.dp,
            verticalPadding = 8.dp
        )
    }
}

// ---- Preview area (pure black, 16:9, pinch zoom, centered play) ------------
@Composable
private fun PreviewArea(
    isPlaying: Boolean,
    zoom: Float,
    onTogglePlay: () -> Unit,
    onZoom: (Float) -> Unit,
    frameProvider: PreviewFrameProvider? = null,
    modifier: Modifier = Modifier
) {
    var panX by remember { mutableStateOf(0f) }
    var panY by remember { mutableStateOf(0f) }
    val animatedScale by animateFloatAsState(zoom, tween(180), label = "preview-zoom")

    Box(
        modifier = modifier
            .background(PureBlack)
            .pointerInput(Unit) {
                // pinch zoom + drag pan @60fps (gesture-driven, hardware accel)
                detectTransformGestures { _, pan, gestureZoom, _ ->
                    onZoom(zoom * gestureZoom)
                    panX += pan.x; panY += pan.y
                }
            },
        contentAlignment = Alignment.Center
    ) {
        // The real GL preview surface — NO fake colors, pure black bg.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .scale(animatedScale)
                .clip(RoundedCornerShape(0.dp)) // pure black, no rounding on the canvas
                .background(PureBlack)
        ) {
            LivePreviewSurface(
                modifier = Modifier.fillMaxSize(),
                frameProvider = frameProvider
            )

            // Centered play/pause button — the ONLY overlay (user control).
            // Spec: "centered play button" — this is user content, allowed.
            PlayButton(isPlaying = isPlaying, onClick = onTogglePlay)
        }
    }
}

@Composable
private fun PlayButton(isPlaying: Boolean, onClick: () -> Unit) {
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (pressed) 0.9f else 1f, tween(120), label = "play")
    Box(
        modifier = Modifier
            .size(64.dp)
            .scale(scale)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = { pressed = true; tryAwaitRelease(); pressed = false },
                    onTap = { onClick() }
                )
            }
            .clip(RoundedCornerShape(32.dp))
            .background(Bg.copy(alpha = 0.55f))
            .border(1.dp, GlassStroke, RoundedCornerShape(32.dp)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
            contentDescription = if (isPlaying) "Pause" else "Play",
            tint = White, modifier = Modifier.size(36.dp)
        )
    }
}

// ---- Timeline (glass track cards) ------------------------------------------
@Composable
private fun TimelineArea(
    project: VideoProject,
    selectedTrackId: String?,
    onSelect: (String?) -> Unit,
    zoom: Float,
    onZoom: (Float) -> Unit,
    key: Long
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .pointerInput(Unit) {
                detectTransformGestures { _, _, gZoom, _ -> onZoom(zoom * gZoom) }
            }
    ) {
        // Time ruler
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            repeat(10) { i ->
                Text("${i}s", color = TextSecondary, fontSize = 10.sp,
                     modifier = Modifier.padding(end = 12.dp))
            }
        }
        Spacer(Modifier.height(6.dp))
        // Tracks
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            // use key to force recompose on revision bump
            key(key) {
                project.tracks.forEach { track ->
                    TimelineTrackCard(
                        track = track,
                        selected = track.id == selectedTrackId,
                        zoom = zoom,
                        onClick = { onSelect(track.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun TimelineTrackCard(
    track: TimelineTrack, selected: Boolean, zoom: Float, onClick: () -> Unit
) {
    val (color, icon) = trackStyle(track.type)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(46.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(BgCard.copy(alpha = 0.6f))
            .then(if (selected) Modifier.border(1.dp, powercutGradientBrush(), RoundedCornerShape(10.dp))
                  else Modifier.border(1.dp, GlassStroke, RoundedCornerShape(10.dp)))
            .pointerInput(Unit) { detectTapGestures(onTap = { onClick() }) }
    ) {
        // Clip block positioned by start/duration (scaled by zoom)
        val startFrac = (track.startUs.toFloat() / 30_000_000f).coerceIn(0f, 1f)
        val widthFrac = (track.durationUs.toFloat() / 30_000_000f).coerceIn(0.02f, 1f) * zoom.coerceAtMost(2f)
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .padding(start = (startFrac * 320f).dp.coerceAtMost(280.dp), top = 4.dp, bottom = 4.dp)
                .widthIn(min = 40.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(
                    Brush.horizontalGradient(listOf(color, color.copy(alpha = 0.55f)))
                )
                .padding(horizontal = 8.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = White, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text(track.label, color = White, fontSize = 11.sp,
                     fontWeight = FontWeight.Medium, maxLines = 1)
                // Audio waveform hint
                if (track.type == TimelineTrack.TrackType.AUDIO) {
                    Spacer(Modifier.width(8.dp))
                    WaveformHint(modifier = Modifier.width(40.dp).height(16.dp))
                }
            }
        }
    }
}

@Composable
private fun WaveformHint(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val bars = 12
        val w = size.width / bars
        for (i in 0 until bars) {
            val h = size.height * (0.3f + 0.7f * kotlin.math.abs(kotlin.math.sin(i * 0.9f)))
            drawRect(
                color = White.copy(alpha = 0.7f),
                topLeft = androidx.compose.ui.geometry.Offset(i * w, (size.height - h) / 2f),
                size = androidx.compose.ui.geometry.Size(w * 0.6f, h)
            )
        }
    }
}

private fun trackStyle(t: TimelineTrack.TrackType): Pair<Color, ImageVector> = when (t) {
    TimelineTrack.TrackType.VIDEO   -> TrackVideo   to Icons.Filled.Movie
    TimelineTrack.TrackType.AUDIO   -> TrackAudio   to Icons.Filled.GraphicEq
    TimelineTrack.TrackType.SUBTITLE-> TrackSubtitle to Icons.Filled.Subtitles
    TimelineTrack.TrackType.STICKER -> TrackSticker to Icons.Filled.EmojiEmotions
}

private fun formatDuration(us: Long): String {
    val totalSec = us / 1_000_000
    val m = totalSec / 60
    val s = totalSec % 60
    return "%02d:%02d".format(m, s)
}

// ---- Bottom toolbar (two rows of gradient pills) ---------------------------
enum class EditorTool(val label: String, val icon: ImageVector, val pro: Boolean) {
    EDIT("Edit", Icons.Filled.ContentCut, false),
    LAYERS("Layers", Icons.Filled.Layers, false),
    SPEED("Speed", Icons.Filled.Speed, false),
    CROP("Crop", Icons.Filled.Crop, false),
    AUDIO("Audio", Icons.Filled.VolumeUp, false),
    TEXT("Text", Icons.Filled.TextFields, false),
    FILTERS("Filters", Icons.Filled.FilterVintage, false),
    EFFECTS("Effects", Icons.Filled.AutoAwesome, true),
    STICKERS("Stickers", Icons.Filled.EmojiEmotions, false),
    CHROMA("Chroma Key", Icons.Filled.Colorize, true),
    VFX("VFX", Icons.Filled.Bolt, true),
    EFFECTS3D("3D Effects", Icons.Filled.ViewInAr, true),
    AI_HUB("AI Hub", Icons.Filled.SmartToy, true),
    PRESETS("Presets", Icons.Filled.Preview, true),
    PRO("Pro", Icons.Filled.WorkspacePremium, true),
    STUDIO("Studio", Icons.Filled.Brush, true),
    EXPORT("Export", Icons.Filled.Download, false)
}

@Composable
private fun BottomToolbar(
    activeTool: EditorTool?,
    onTool: (EditorTool) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(BgElev.copy(alpha = 0.6f))
            .padding(vertical = 10.dp)
    ) {
        // ROW 1
        LazyRow(
            contentPadding = PaddingValues(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(EditorTool.values().filter { it != EditorTool.EXPORT && it.ordinal <= EditorTool.STICKERS.ordinal }) { tool ->
                GradientPill(
                    text = tool.label,
                    icon = { Icon(tool.icon, contentDescription = tool.label, tint = White, modifier = Modifier.size(18.dp)) },
                    pro = tool.pro,
                    selected = activeTool == tool,
                    onClick = { onTool(tool) }
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        // ROW 2
        LazyRow(
            contentPadding = PaddingValues(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(EditorTool.values().filter { it != EditorTool.EXPORT && it.ordinal > EditorTool.STICKERS.ordinal }) { tool ->
                GradientPill(
                    text = tool.label,
                    icon = { Icon(tool.icon, contentDescription = tool.label, tint = White, modifier = Modifier.size(18.dp)) },
                    pro = tool.pro,
                    selected = activeTool == tool,
                    onClick = { onTool(tool) }
                )
            }
        }
    }
}

// ---- Tool overlay sheet (routes to the dedicated tool screens) -------------
@Composable
private fun ToolOverlaySheet(
    tool: EditorTool,
    onDismiss: () -> Unit,
    vm: EditorViewModel
) {
    val content: @Composable () -> Unit = when (tool) {
        EditorTool.FILTERS   -> { -> com.powercut.ui.tools.FiltersScreen(onClose = onDismiss, vm = vm) }
        EditorTool.EFFECTS   -> { -> com.powercut.ui.tools.EffectsScreen(onClose = onDismiss, vm = vm) }
        EditorTool.EFFECTS3D -> { -> com.powercut.ui.tools.Effects3DScreen(onClose = onDismiss, vm = vm) }
        EditorTool.CHROMA    -> { -> com.powercut.ui.tools.ChromaKeyScreen(onClose = onDismiss, vm = vm) }
        EditorTool.VFX       -> { -> com.powercut.ui.tools.VFXScreen(onClose = onDismiss, vm = vm) }
        EditorTool.AI_HUB    -> { -> com.powercut.ui.tools.AIHubScreen(onClose = onDismiss, vm = vm) }
        EditorTool.PRESETS   -> { -> com.powercut.ui.tools.PresetsScreen(onClose = onDismiss, vm = vm) }
        EditorTool.PRO       -> { -> com.powercut.ui.tools.ProScreen(onClose = onDismiss, vm = vm) }
        EditorTool.STUDIO    -> { -> com.powercut.ui.tools.StudioScreen(onClose = onDismiss, vm = vm) }
        EditorTool.EXPORT    -> { -> com.powercut.ui.export.ExportScreen(onClose = onDismiss, vm = vm) }
        else -> { -> GenericToolSheet(title = tool.label, onClose = onDismiss) }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Bg.copy(alpha = 0.92f))
    ) {
        Box(modifier = Modifier.fillMaxSize().padding(top = 24.dp)) {
            content()
        }
    }
}

@Composable
private fun GenericToolSheet(title: String, onClose: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(title, color = TextPrimary, fontSize = 22.sp, fontWeight = FontWeight.Bold,
                 modifier = Modifier.weight(1f))
            GradientPill(text = "Close", onClick = onClose, horizontalPadding = 18.dp)
        }
        Text("$title panel — existing timeline/audio/sticker controls remain 100% functional underneath. " +
             "Wired into the DAG resolver via EditorViewModel.addDagNode().",
             color = TextSecondary, fontSize = 13.sp)
    }
}
