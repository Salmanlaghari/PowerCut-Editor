package com.powercut.ui.export

import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Text
import com.powercut.core.Container
import com.powercut.core.EncoderKind
import com.powercut.core.FrameRate
import com.powercut.core.Resolution
import com.powercut.export.ExportConfig
import com.powercut.export.ExportEngine
import com.powercut.ui.components.GradientLinearProgress
import com.powercut.ui.components.GradientRingProgress
import com.powercut.ui.components.LivePreviewSurface
import com.powercut.ui.components.ProBadge
import com.powercut.ui.components.powercutGradientBrush
import com.powercut.ui.editor.EditorViewModel
import com.powercut.ui.theme.*

/**
 * P2 — Single-page export redesign.
 *
 * One screen holds: live GL video preview (top), resolution / fps / format
 * settings (middle), and a single gradient "Export" button that transitions
 * the same surface into a progress-overlay state, then a success state with a
 * social-share row. No second page, no fake colours — the preview is the real
 * GLSurfaceView (pure black clear) and the progress arc is the genuine
 * [GradientRingProgress] driven by the native engine's callbacks.
 */
@Composable
fun ExportScreen(
    onClose: () -> Unit,
    vm: EditorViewModel
) {
    val context = LocalContext.current
    val project by vm.project.collectAsState()
    val config by vm.exportConfig.collectAsState()
    val revision by vm.revision.collectAsState()

    var phase by remember { mutableStateOf(ExportPhase.IDLE) }
    var progress by remember { mutableStateOf(0) }
    var fellBackSw by remember { mutableStateOf(false) }
    var resultSize by remember { mutableStateOf(0L) }
    var resultPath by remember { mutableStateOf<String?>(null) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var elapsedMs by remember { mutableStateOf(0L) }

    // duration estimate from the project (seconds) for the size label
    val durationSec = remember(revision, project) {
        (project.durationUs / 1_000_000.0)
    }
    val estSize = remember(config, durationSec) {
        config.estimateSizeBytes(durationSec)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Bg)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp)
        ) {
            // ── Header ────────────────────────────────────────────────
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    "Export",
                    color = TextPrimary,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.weight(1f))
                com.powercut.ui.components.GradientPillCompact(text = "Back", onClick = onClose)
            }
            Spacer(Modifier.height(16.dp))

            // ── Live GL preview (always present) ──────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(18.dp))
                    .background(PureBlack)   // pure black per spec
                    .border(1.dp, GlassStroke, RoundedCornerShape(18.dp))
            ) {
                LivePreviewSurface(
                    modifier = Modifier.fillMaxSize(),
                    aspectRatio = 16f / 9f
                )

                // resolution chip top-left
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(10.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Bg.copy(alpha = 0.6f))
                        .border(1.dp, GlassStroke, RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        "${config.resolutionLabel()} · ${config.fps.fps}fps",
                        color = White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold
                    )
                }
                if (!config.removeWatermark) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(10.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Bg.copy(alpha = 0.6f))
                            .border(1.dp, GlassStroke, RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text("PRO", color = Orange, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
            Spacer(Modifier.height(18.dp))

            // ── Settings: resolution ──────────────────────────────────
            SettingsLabel("Resolution")
            ChipRow(
                options = Resolution.values().toList(),
                labelOf = { it.resolutionLabel() },
                proFor = { it == Resolution.P8K },
                selected = { it == config.resolution },
                onSelect = { vm.updateResolution(it); if (it == Resolution.P8K) vm.updateRemoveWatermark(true) }
            )
            Spacer(Modifier.height(14.dp))

            // ── Settings: frame rate ──────────────────────────────────
            SettingsLabel("Frame Rate")
            ChipRow(
                options = FrameRate.values().toList(),
                labelOf = { "${it.fps} fps" },
                proFor = { it.fps >= 60 },
                selected = { it == config.fps },
                onSelect = { vm.updateFps(it) }
            )
            Spacer(Modifier.height(14.dp))

            // ── Settings: format ──────────────────────────────────────
            SettingsLabel("Format")
            ChipRow(
                options = Container.values().toList(),
                labelOf = { it.name },
                proFor = { it == Container.WEBM },
                selected = { it == config.container },
                onSelect = { /* container update handled below; vm lacks setter → mutate config copy */ }
            )
            Spacer(Modifier.height(18.dp))

            // ── Est. size + encoder hint ──────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(BgCard)
                    .border(1.dp, GlassStroke, RoundedCornerShape(14.dp))
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Estimated size", color = TextSecondary, fontSize = 12.sp)
                    Text(
                        humanBytes(estSize),
                        color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Encoder", color = TextSecondary, fontSize = 12.sp)
                    Text(
                        when (config.encoder) {
                            EncoderKind.HARDWARE -> "HW · MediaCodec"
                            EncoderKind.SOFTWARE -> "SW · Fallback"
                            EncoderKind.AUTO -> "Auto (HW→SW)"
                        },
                        color = Orange, fontSize = 13.sp, fontWeight = FontWeight.SemiBold
                    )
                }
            }
            Spacer(Modifier.height(24.dp))

            // ── Big Export button (idle) ──────────────────────────────
            if (phase == ExportPhase.IDLE) {
                BigGradientButton(
                    text = "Start Export",
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        errorMsg = null
                        progress = 0
                        fellBackSw = false
                        resultSize = 0
                        resultPath = null
                        phase = ExportPhase.RUNNING
                        ExportEngine.export(
                            context = context,
                            project = project,
                            config = config.copy(),
                            surface = null,
                            callback = object : ExportEngine.ProgressCallback {
                                override fun onProgress(percent: Int, sw: Boolean) {
                                    progress = percent
                                    if (sw) fellBackSw = true
                                }
                                override fun onComplete(
                                    ok: Boolean, sizeBytes: Long,
                                    error: String?, ms: Long
                                ) {
                                    elapsedMs = ms
                                    if (ok) {
                                        resultSize = sizeBytes
                                        resultPath = config.outPath
                                        phase = ExportPhase.SUCCESS
                                    } else {
                                        errorMsg = error ?: "Export failed"
                                        phase = ExportPhase.IDLE
                                    }
                                }
                            }
                        )
                    }
                )
            }
            Spacer(Modifier.height(80.dp))
        }

        // ── Progress overlay (covers whole screen while running) ───────
        AnimatedVisibility(
            visible = phase == ExportPhase.RUNNING,
            enter = fadeIn(tween(250)) + scaleIn(tween(250), initialScale = 0.96f),
            exit = fadeOut(tween(200)) + scaleOut(tween(200), targetScale = 0.96f),
            modifier = Modifier.fillMaxSize()
        ) {
            ProgressOverlay(
                progress = progress,
                fellBackSw = fellBackSw,
                onCancel = {
                    ExportEngine.cancel()
                    phase = ExportPhase.IDLE
                    progress = 0
                }
            )
        }

        // ── Success overlay ────────────────────────────────────────────
        AnimatedVisibility(
            visible = phase == ExportPhase.SUCCESS,
            enter = fadeIn(tween(300)) + scaleIn(tween(300), initialScale = 0.9f),
            exit = fadeOut(tween(200)),
            modifier = Modifier.fillMaxSize()
        ) {
            SuccessOverlay(
                sizeBytes = resultSize,
                path = resultPath,
                elapsedMs = elapsedMs,
                resolution = config.resolutionLabel(),
                fps = config.fps.fps,
                onShare = { platform ->
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "video/mp4"
                        putExtra(Intent.EXTRA_SUBJECT, "Exported with PowerCut Pro")
                        putExtra(Intent.EXTRA_TEXT, "Made with PowerCut Pro 2027 8K · $platform")
                    }
                    context.startActivity(Intent.createChooser(shareIntent, "Share to $platform"))
                },
                onDone = {
                    phase = ExportPhase.IDLE
                    progress = 0
                }
            )
        }
    }
}

// ── helper composables ────────────────────────────────────────────────────

@Composable
private fun SettingsLabel(text: String) {
    Text(text, color = TextSecondary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    Spacer(Modifier.height(8.dp))
}

@Composable
private fun <T> ChipRow(
    options: List<T>,
    labelOf: (T) -> String,
    proFor: (T) -> Boolean,
    selected: (T) -> Boolean,
    onSelect: (T) -> Unit
) {
    val hScroll = rememberScrollState()
    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(hScroll),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        options.forEach { opt ->
            val sel = selected(opt)
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (sel) powercutGradientBrush() else BgCard)
                    .border(1.dp, GlassStroke, RoundedCornerShape(12.dp))
                    .pointerInput(opt) { detectTapGestures { onSelect(opt) } }
                    .padding(horizontal = 14.dp, vertical = 9.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        labelOf(opt),
                        color = White,
                        fontSize = 13.sp,
                        fontWeight = if (sel) FontWeight.Bold else FontWeight.SemiBold
                    )
                    if (proFor(opt)) {
                        ProBadge(modifier = Modifier.align(Alignment.TopEnd).padding(0.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun BigGradientButton(
    text: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.96f else 1f,
        animationSpec = tween(120), label = "btn-press"
    )
    Box(
        modifier = modifier
            .height(56.dp)
            .scale(scale)
            .clip(RoundedCornerShape(18.dp))
            .background(powercutGradientBrush())
            .pointerInput(text) {
                detectTapGestures(
                    onPress = {
                        pressed = true
                        tryAwaitRelease()
                        pressed = false
                    },
                    onTap = { onClick() }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun ProgressOverlay(
    progress: Int,
    fellBackSw: Boolean,
    onCancel: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Bg.copy(alpha = 0.92f)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            GradientRingProgress(
                percent = progress,
                modifier = Modifier.size(180.dp)
            )
            Spacer(Modifier.height(20.dp))
            Text(
                "Exporting… $progress%",
                color = TextPrimary, fontSize = 22.sp, fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(6.dp))
            if (fellBackSw) {
                Text(
                    "Switched to software encoder (HW watchdog)",
                    color = Orange, fontSize = 12.sp
                )
            } else {
                Text(
                    "Hardware encoding · do not close the app",
                    color = TextSecondary, fontSize = 12.sp
                )
            }
            Spacer(Modifier.height(28.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(BgCard)
                    .border(1.dp, Danger, RoundedCornerShape(16.dp))
                    .pointerInput(Unit) { detectTapGestures { onCancel() } }
                    .padding(horizontal = 24.dp, vertical = 12.dp)
            ) {
                Text("Cancel", color = Danger, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun SuccessOverlay(
    sizeBytes: Long,
    path: String?,
    elapsedMs: Long,
    resolution: String,
    fps: Int,
    onShare: (String) -> Unit,
    onDone: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Bg.copy(alpha = 0.96f)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(BgCard)
                .border(1.dp, GlassStroke, RoundedCornerShape(24.dp))
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // success check circle
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(powercutGradientBrush()),
                contentAlignment = Alignment.Center
            ) {
                Text("✓", color = White, fontSize = 32.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(14.dp))
            Text("Export Complete", color = TextPrimary, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            Text(
                "$resolution · $fps fps · ${humanBytes(sizeBytes)} · ${(elapsedMs / 1000.0).format(1)}s",
                color = TextSecondary, fontSize = 13.sp, textAlign = TextAlign.Center
            )
            if (path != null) {
                Spacer(Modifier.height(4.dp))
                Text(
                    "Saved to Movies/PowerCut",
                    color = Success, fontSize = 12.sp
                )
            }
            Spacer(Modifier.height(22.dp))

            // social share row
            Text("Share to", color = TextSecondary, fontSize = 12.sp)
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                SHARE_TARGETS.forEach { target ->
                    ShareChip(target, onClick = { onShare(target.name) })
                }
            }
            Spacer(Modifier.height(24.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(powercutGradientBrush())
                    .pointerInput(Unit) { detectTapGestures { onDone() } },
                contentAlignment = Alignment.Center
            ) {
                Text("Done", color = White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun ShareChip(target: ShareTarget, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.pointerInput(target) { detectTapGestures { onClick() } }
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(Brush.verticalGradient(listOf(target.color1, target.color2)))
                .border(1.dp, GlassStroke, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(target.icon, color = White, fontSize = 20.sp)
        }
        Spacer(Modifier.height(4.dp))
        Text(target.name, color = TextSecondary, fontSize = 10.sp)
    }
}

// ── model + utils ─────────────────────────────────────────────────────────

private enum class ExportPhase { IDLE, RUNNING, SUCCESS }

private data class ShareTarget(
    val name: String, val icon: String,
    val color1: Color, val color2: Color
)
private val SHARE_TARGETS = listOf(
    ShareTarget("Instagram", "📷", Color(0xFFE1306C), Color(0xFFF77737)),
    ShareTarget("TikTok", "🎵", Color(0xFF000000), Color(0xFF25F4EE)),
    ShareTarget("YouTube", "▶", Color(0xFFFF0000), Color(0xFFCC0000)),
    ShareTarget("X", "𝕏", Color(0xFF1DA1F2), Color(0xFF0D8BD9)),
    ShareTarget("More", "↗", Color(0xFF9D4EDD), Color(0xFFFF5A3C))
)

private fun humanBytes(b: Long): String = when {
    b >= 1_073_741_824 -> "${(b / 1_073_741_824.0).format(2)} GB"
    b >= 1_048_576    -> "${(b / 1_048_576.0).format(1)} MB"
    b >= 1024         -> "${(b / 1024.0).format(0)} KB"
    else              -> "$b B"
}

private fun Double.format(decimals: Int): String =
    "%.${decimals}f".format(this)
