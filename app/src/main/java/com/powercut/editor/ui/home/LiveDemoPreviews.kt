package com.powercut.editor.ui.home

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate

/**
 * 2027 8K — Live Demo Preview Canvas composables for Quick Tool cards.
 * Each preview is an animated Canvas that visually demonstrates what the tool does,
 * matching CapCut/VN/KineMaster's real-time filter/effect thumbnail previews.
 *
 * The previews use the signature orange→purple gradient theme for brand consistency.
 */

private val SignatureOrange = Color(0xFFFF5A3C)
private val SignaturePurple = Color(0xFF9D4EDD)
private val AccentCyan = Color(0xFF2DD4BF)
private val AccentRose = Color(0xFFFF3D7F)
private val AccentGold = Color(0xFFFFD166)
private val DeepBg = Color(0xFF0F0F1A)

/** Shared infinite pulse animation driver for all demo previews */
@Composable
private fun rememberPulse(): Float {
    val transition = rememberInfiniteTransition(label = "pulse")
    val pulse by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseVal"
    )
    return pulse
}

/** Scissors / trim animation — two blade halves closing on a film strip */
@Composable
fun TrimDemoPreview(modifier: Modifier = Modifier) {
    val pulse = rememberPulse()
    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        // Film strip background
        drawRect(
            brush = Brush.horizontalGradient(
                colors = listOf(DeepBg, Color(0xFF1A1A2E))
            )
        )
        // Film perforations top & bottom
        val perfW = w * 0.06f
        for (i in 0..6) {
            val x = i * w / 6f
            drawRect(
                color = Color(0xFF2A2A3E),
                topLeft = Offset(x, 0f),
                size = Size(perfW, h * 0.12f)
            )
            drawRect(
                color = Color(0xFF2A2A3E),
                topLeft = Offset(x, h * 0.88f),
                size = Size(perfW, h * 0.12f)
            )
        }
        // Film frames (gradient colored)
        drawRect(
            brush = Brush.horizontalGradient(
                colors = listOf(SignatureOrange.copy(alpha = 0.7f), SignaturePurple.copy(alpha = 0.7f))
            ),
            topLeft = Offset(0f, h * 0.14f),
            size = Size(w, h * 0.72f)
        )
        // Scissor cut line (animated closing)
        val cutX = w * (0.4f + pulse * 0.2f)
        drawLine(
            color = Color.White.copy(alpha = 0.9f),
            start = Offset(cutX, h * 0.14f),
            end = Offset(cutX, h * 0.86f),
            strokeWidth = w * 0.02f
        )
        // Scissor blade indicators
        val bladeSize = w * 0.12f * (0.8f + pulse * 0.3f)
        drawCircle(
            color = AccentRose,
            radius = bladeSize * 0.4f,
            center = Offset(cutX, h * 0.1f)
        )
        drawCircle(
            color = AccentRose,
            radius = bladeSize * 0.4f,
            center = Offset(cutX, h * 0.9f)
        )
    }
}

/** MP3→MP4: Audio waveform morphing into a video frame */
@Composable
fun Mp3ToMp4DemoPreview(modifier: Modifier = Modifier) {
    val pulse = rememberPulse()
    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        drawRect(brush = Brush.verticalGradient(listOf(DeepBg, Color(0xFF12121F))))

        // Left half: audio waveform bars (decreasing as they morph to video)
        val waveAlpha = 1f - pulse * 0.5f
        val barCount = 7
        val barW = w * 0.04f
        val startX = w * 0.08f
        val spacing = w * 0.06f
        for (i in 0 until barCount) {
            val barH = h * (0.2f + 0.5f * kotlin.math.abs(kotlin.math.sin((i + pulse * 3f) * 0.8f)))
            drawRect(
                color = AccentCyan.copy(alpha = waveAlpha),
                topLeft = Offset(startX + i * spacing, (h - barH) / 2f),
                size = Size(barW, barH)
            )
        }
        // MP3 label area fading
        drawCircle(
            color = AccentCyan.copy(alpha = waveAlpha * 0.6f),
            radius = w * 0.08f,
            center = Offset(w * 0.12f, h * 0.2f)
        )

        // Right half: video frame emerging (grows with pulse)
        val frameW = w * (0.3f + pulse * 0.15f)
        val frameH = h * (0.5f + pulse * 0.2f)
        val frameX = w * 0.55f
        val frameY = (h - frameH) / 2f
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(SignatureOrange, SignaturePurple)
            ),
            topLeft = Offset(frameX, frameY),
            size = Size(frameW, frameH)
        )
        // Play triangle on the video frame
        val triCenter = Offset(frameX + frameW * 0.5f, frameY + frameH * 0.5f)
        val triSize = frameW * 0.15f
        val path = Path().apply {
            moveTo(triCenter.x - triSize * 0.4f, triCenter.y - triSize * 0.5f)
            lineTo(triCenter.x - triSize * 0.4f, triCenter.y + triSize * 0.5f)
            lineTo(triCenter.x + triSize * 0.6f, triCenter.y)
            close()
        }
        drawPath(path, color = Color.White.copy(alpha = 0.95f))
        // Arrow showing transformation
        drawLine(
            color = Color.White.copy(alpha = 0.5f),
            start = Offset(w * 0.42f, h * 0.5f),
            end = Offset(w * 0.52f, h * 0.5f),
            strokeWidth = w * 0.015f
        )
    }
}

/** Crop: Frame with animated crop handles tightening */
@Composable
fun CropDemoPreview(modifier: Modifier = Modifier) {
    val pulse = rememberPulse()
    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        drawRect(brush = Brush.verticalGradient(listOf(DeepBg, Color(0xFF12121F))))

        // Full image (dimmed)
        drawRect(
            brush = Brush.linearGradient(
                colors = listOf(SignatureOrange.copy(alpha = 0.25f), SignaturePurple.copy(alpha = 0.25f))
            )
        )
        // Crop region (shrinks slightly with pulse)
        val margin = w * (0.1f + pulse * 0.08f)
        val cropLeft = margin
        val cropTop = h * (0.1f + pulse * 0.06f)
        val cropRight = w - margin
        val cropBottom = h - h * (0.1f + pulse * 0.06f)
        // Bright cropped area
        drawRect(
            brush = Brush.linearGradient(
                colors = listOf(SignatureOrange, SignaturePurple)
            ),
            topLeft = Offset(cropLeft, cropTop),
            size = Size(cropRight - cropLeft, cropBottom - cropTop)
        )
        // Crop handles (L-shaped corners)
        val handleLen = w * 0.08f
        val handleThick = w * 0.025f
        val handleColor = AccentCyan
        // Top-left
        drawLine(handleColor, Offset(cropLeft, cropTop), Offset(cropLeft + handleLen, cropTop), handleThick)
        drawLine(handleColor, Offset(cropLeft, cropTop), Offset(cropLeft, cropTop + handleLen), handleThick)
        // Top-right
        drawLine(handleColor, Offset(cropRight, cropTop), Offset(cropRight - handleLen, cropTop), handleThick)
        drawLine(handleColor, Offset(cropRight, cropTop), Offset(cropRight, cropTop + handleLen), handleThick)
        // Bottom-left
        drawLine(handleColor, Offset(cropLeft, cropBottom), Offset(cropLeft + handleLen, cropBottom), handleThick)
        drawLine(handleColor, Offset(cropLeft, cropBottom), Offset(cropLeft, cropBottom - handleLen), handleThick)
        // Bottom-right
        drawLine(handleColor, Offset(cropRight, cropBottom), Offset(cropRight - handleLen, cropBottom), handleThick)
        drawLine(handleColor, Offset(cropRight, cropBottom), Offset(cropRight, cropBottom - handleLen), handleThick)
    }
}

/** Compress: File size shrinking with downward arrows */
@Composable
fun CompressDemoPreview(modifier: Modifier = Modifier) {
    val pulse = rememberPulse()
    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        drawRect(brush = Brush.verticalGradient(listOf(DeepBg, Color(0xFF12121F))))

        // Large file box (top) shrinking into small file box (bottom)
        val bigSize = w * (0.6f - pulse * 0.1f)
        val smallSize = w * (0.25f + pulse * 0.05f)
        // Big file
        drawRoundRect(
            brush = Brush.linearGradient(listOf(SignatureOrange.copy(alpha = 0.7f), SignaturePurple.copy(alpha = 0.7f))),
            topLeft = Offset((w - bigSize) / 2f, h * 0.08f),
            size = Size(bigSize, bigSize * 0.6f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.03f)
        )
        // Compression arrows
        val arrowX = w * 0.5f
        for (i in 0..2) {
            val yStart = h * (0.5f + i * 0.05f)
            drawLine(
                color = AccentCyan.copy(alpha = 0.7f),
                start = Offset(arrowX - w * 0.08f, yStart),
                end = Offset(arrowX, yStart + h * 0.04f),
                strokeWidth = w * 0.015f
            )
            drawLine(
                color = AccentCyan.copy(alpha = 0.7f),
                start = Offset(arrowX + w * 0.08f, yStart),
                end = Offset(arrowX, yStart + h * 0.04f),
                strokeWidth = w * 0.015f
            )
        }
        // Small compressed file
        drawRoundRect(
            brush = Brush.linearGradient(listOf(AccentCyan, AccentCyan.copy(alpha = 0.5f))),
            topLeft = Offset((w - smallSize) / 2f, h * 0.7f),
            size = Size(smallSize, smallSize * 0.6f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.03f)
        )
    }
}

/** Reverse: Play arrow flipping direction */
@Composable
fun ReverseDemoPreview(modifier: Modifier = Modifier) {
    val pulse = rememberPulse()
    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        drawRect(brush = Brush.verticalGradient(listOf(DeepBg, Color(0xFF12121F))))

        // Film strip frames
        val frameCount = 4
        val frameW = w * 0.18f
        val frameH = h * 0.5f
        val stripY = h * 0.25f
        for (i in 0 until frameCount) {
            val x = w * 0.1f + i * w * 0.2f
            // Numbered frames in reverse order (4,3,2,1)
            val num = frameCount - i
            val alpha = if (num == (frameCount - (pulse * frameCount).toInt().coerceIn(0, frameCount - 1))) 1f else 0.4f
            drawRoundRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        SignatureOrange.copy(alpha = alpha),
                        SignaturePurple.copy(alpha = alpha)
                    )
                ),
                topLeft = Offset(x, stripY),
                size = Size(frameW, frameH),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.02f)
            )
        }
        // Reverse arrows (circular)
        val centerX = w * 0.5f
        val centerY = h * 0.15f
        val radius = w * 0.1f
        rotate(degrees = pulse * 360f, pivot = Offset(centerX, centerY)) {
            drawArc(
                color = AccentRose,
                startAngle = 0f,
                sweepAngle = 270f,
                useCenter = false,
                topLeft = Offset(centerX - radius, centerY - radius),
                size = Size(radius * 2, radius * 2),
                style = Stroke(width = w * 0.02f)
            )
        }
    }
}

/** Slow-Mo: Clock with slowing hand + speed lines */
@Composable
fun SlowMoDemoPreview(modifier: Modifier = Modifier) {
    val pulse = rememberPulse()
    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        drawRect(brush = Brush.verticalGradient(listOf(DeepBg, Color(0xFF12121F))))

        val centerX = w * 0.5f
        val centerY = h * 0.5f
        val radius = w * 0.3f

        // Clock circle
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(SignatureOrange.copy(alpha = 0.3f), SignaturePurple.copy(alpha = 0.15f)),
                center = Offset(centerX, centerY),
                radius = radius
            ),
            radius = radius,
            center = Offset(centerX, centerY)
        )
        drawCircle(
            color = Color.White.copy(alpha = 0.3f),
            radius = radius,
            center = Offset(centerX, centerY),
            style = Stroke(width = w * 0.02f)
        )
        // Clock hand (moving slowly — pulse drives it)
        val angle = pulse * 360f
        val handEnd = Offset(
            centerX + radius * 0.7f * kotlin.math.cos(Math.toRadians(angle - 90.0)).toFloat(),
            centerY + radius * 0.7f * kotlin.math.sin(Math.toRadians(angle - 90.0)).toFloat()
        )
        drawLine(
            color = AccentCyan,
            start = Offset(centerX, centerY),
            end = handEnd,
            strokeWidth = w * 0.025f
        )
        // Center dot
        drawCircle(color = Color.White, radius = w * 0.02f, center = Offset(centerX, centerY))

        // Speed lines trailing (showing slow-mo effect)
        for (i in 1..3) {
            val trailAngle = angle - i * 25f
            val trailStart = Offset(
                centerX + radius * 0.75f * kotlin.math.cos(Math.toRadians(trailAngle - 90.0)).toFloat(),
                centerY + radius * 0.75f * kotlin.math.sin(Math.toRadians(trailAngle - 90.0)).toFloat()
            )
            val trailEnd = Offset(
                centerX + radius * 0.95f * kotlin.math.cos(Math.toRadians(trailAngle - 90.0)).toFloat(),
                centerY + radius * 0.95f * kotlin.math.sin(Math.toRadians(trailAngle - 90.0)).toFloat()
            )
            drawLine(
                color = AccentCyan.copy(alpha = 0.3f / i),
                start = trailStart,
                end = trailEnd,
                strokeWidth = w * 0.01f
            )
        }
    }
}

/** Slideshow: Multiple photos cycling with transition */
@Composable
fun SlideshowDemoPreview(modifier: Modifier = Modifier) {
    val pulse = rememberPulse()
    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        drawRect(brush = Brush.verticalGradient(listOf(DeepBg, Color(0xFF12121F))))

        // Stack of photo cards offset, top one slides
        val colors = listOf(SignatureOrange, SignaturePurple, AccentCyan)
        for (i in 2 downTo 0) {
            val offsetX = w * 0.1f + i * w * 0.08f
            val offsetY = h * 0.15f + i * h * 0.06f
            val slideOffset = if (i == 0) pulse * w * 0.2f else 0f
            drawRoundRect(
                brush = Brush.verticalGradient(
                    colors = listOf(colors[i].copy(alpha = 0.8f), colors[i].copy(alpha = 0.4f))
                ),
                topLeft = Offset(offsetX + slideOffset, offsetY),
                size = Size(w * 0.5f, h * 0.6f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.03f)
            )
        }
        // Transition dots
        for (i in 0..2) {
            val dotX = w * 0.25f + i * w * 0.15f
            val dotAlpha = if (i == (pulse * 3f).toInt().coerceIn(0, 2)) 1f else 0.3f
            drawCircle(
                color = Color.White.copy(alpha = dotAlpha),
                radius = w * 0.015f,
                center = Offset(dotX, h * 0.85f)
            )
        }
    }
}

/** Add Music: Musical note with animated sound waves */
@Composable
fun AddMusicDemoPreview(modifier: Modifier = Modifier) {
    val pulse = rememberPulse()
    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        drawRect(brush = Brush.verticalGradient(listOf(DeepBg, Color(0xFF12121F))))

        // Animated sound wave circles emanating
        val centerX = w * 0.5f
        val centerY = h * 0.5f
        for (i in 1..4) {
            val waveRadius = w * (0.1f + (pulse + i * 0.25f) % 1f * 0.3f)
            drawCircle(
                color = SignatureOrange.copy(alpha = (1f - (pulse + i * 0.25f) % 1f) * 0.4f),
                radius = waveRadius,
                center = Offset(centerX, centerY),
                style = Stroke(width = w * 0.015f)
            )
        }

        // Musical note
        val noteX = centerX
        val noteY = centerY
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(SignatureOrange, SignaturePurple),
                center = Offset(noteX, noteY),
                radius = w * 0.08f
            ),
            radius = w * 0.08f,
            center = Offset(noteX, noteY)
        )
        // Note stem
        drawLine(
            color = Color.White,
            start = Offset(noteX + w * 0.07f, noteY),
            end = Offset(noteX + w * 0.07f, noteY - h * 0.25f),
            strokeWidth = w * 0.02f
        )
        // Note flag
        val flagPath = Path().apply {
            moveTo(noteX + w * 0.07f, noteY - h * 0.25f)
            cubicTo(
                noteX + w * 0.18f, noteY - h * 0.22f,
                noteX + w * 0.18f, noteY - h * 0.12f,
                noteX + w * 0.07f, noteY - h * 0.1f
            )
        }
        drawPath(flagPath, color = Color.White, style = Stroke(width = w * 0.02f))
    }
}

/** Dispatch: returns the right demo preview composable for a tool id */
@Composable
fun QuickToolDemoPreview(toolId: String, modifier: Modifier = Modifier) {
    when (toolId) {
        "trim" -> TrimDemoPreview(modifier)
        "convert_mp3" -> Mp3ToMp4DemoPreview(modifier)
        "crop" -> CropDemoPreview(modifier)
        "compress" -> CompressDemoPreview(modifier)
        "reverse" -> ReverseDemoPreview(modifier)
        "slowmo" -> SlowMoDemoPreview(modifier)
        "slideshow" -> SlideshowDemoPreview(modifier)
        "addmusic" -> AddMusicDemoPreview(modifier)
        else -> {
            // Fallback: generic gradient preview
            Canvas(modifier = modifier.fillMaxSize()) {
                drawRect(
                    brush = Brush.linearGradient(
                        colors = listOf(SignatureOrange, SignaturePurple)
                    )
                )
            }
        }
    }
}

// ============================================================
// Premium Feature Demo Previews (for the 4 top premium buttons)
// ============================================================

/** AI Hub: Neural network nodes pulsing with connections */
@Composable
fun AiHubDemoPreview(modifier: Modifier = Modifier) {
    val pulse = rememberPulse()
    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        drawRect(brush = Brush.radialGradient(
            colors = listOf(Color(0xFF1A0A2E), DeepBg),
            center = Offset(w * 0.5f, h * 0.5f),
            radius = w * 0.7f
        ))
        // Neural network: 3 layers of nodes
        val layers = listOf(2, 3, 2)
        val nodeRadius = w * 0.04f
        val nodes = mutableListOf<Offset>()
        layers.forEachIndexed { layerIdx, count ->
            val x = w * (0.2f + layerIdx * 0.3f)
            for (i in 0 until count) {
                val y = h * (0.25f + i * (0.5f / (count - 1).coerceAtLeast(1)))
                nodes.add(Offset(x, y))
            }
        }
        // Draw connections
        var idx = 0
        layers.forEachIndexed { layerIdx, count ->
            if (layerIdx < layers.size - 1) {
                val nextCount = layers[layerIdx + 1]
                val nextStart = idx + count
                for (i in 0 until count) {
                    for (j in 0 until nextCount) {
                        drawLine(
                            color = SignaturePurple.copy(alpha = 0.3f + pulse * 0.3f),
                            start = nodes[idx + i],
                            end = nodes[nextStart + j],
                            strokeWidth = w * 0.008f
                        )
                    }
                }
            }
            idx += count
        }
        // Draw nodes (pulsing)
        nodes.forEachIndexed { i, node ->
            val nodePulse = (kotlin.math.abs(kotlin.math.sin((i + pulse * 3f) * 1.5f)) + 1f) / 2f
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(SignatureOrange, SignaturePurple),
                    center = node,
                    radius = nodeRadius * (1f + nodePulse * 0.3f)
                ),
                radius = nodeRadius * (1f + nodePulse * 0.3f),
                center = node
            )
        }
    }
}

/** Presets/Effects: Gallery of effect swatches */
@Composable
fun PresetsDemoPreview(modifier: Modifier = Modifier) {
    val pulse = rememberPulse()
    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        drawRect(brush = Brush.verticalGradient(listOf(DeepBg, Color(0xFF12121F))))
        // Grid of effect swatches
        val swatchColors = listOf(
            SignatureOrange, SignaturePurple, AccentCyan,
            AccentRose, AccentGold, Color(0xFF7C5CFF)
        )
        val cols = 3
        val rows = 2
        val swatchW = w * 0.25f
        val swatchH = h * 0.35f
        val gapX = w * 0.04f
        val gapY = h * 0.05f
        val startX = (w - cols * swatchW - (cols - 1) * gapX) / 2f
        val startY = (h - rows * swatchH - (rows - 1) * gapY) / 2f
        for (i in swatchColors.indices) {
            val col = i % cols
            val row = i / cols
            val x = startX + col * (swatchW + gapX)
            val y = startY + row * (swatchH + gapY)
            val glowAlpha = if (i == (pulse * swatchColors.size).toInt() % swatchColors.size) 1f else 0.5f
            drawRoundRect(
                brush = Brush.linearGradient(
                    colors = listOf(swatchColors[i].copy(alpha = glowAlpha), swatchColors[i].copy(alpha = glowAlpha * 0.4f))
                ),
                topLeft = Offset(x, y),
                size = Size(swatchW, swatchH),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.02f)
            )
        }
    }
}

/** Pro Tier: Crown / diamond premium badge */
@Composable
fun ProTierDemoPreview(modifier: Modifier = Modifier) {
    val pulse = rememberPulse()
    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        drawRect(brush = Brush.radialGradient(
            colors = listOf(Color(0xFF2A1A0A), DeepBg),
            center = Offset(w * 0.5f, h * 0.4f),
            radius = w * 0.6f
        ))
        // Diamond/crown shape
        val centerX = w * 0.5f
        val centerY = h * 0.45f
        val size_ = w * (0.25f + pulse * 0.05f)
        // Crown body
        val crownPath = Path().apply {
            moveTo(centerX - size_, centerY + size_ * 0.5f)
            lineTo(centerX - size_, centerY - size_ * 0.2f)
            lineTo(centerX - size_ * 0.5f, centerY)
            lineTo(centerX, centerY - size_ * 0.6f)
            lineTo(centerX + size_ * 0.5f, centerY)
            lineTo(centerX + size_, centerY - size_ * 0.2f)
            lineTo(centerX + size_, centerY + size_ * 0.5f)
            close()
        }
        drawPath(
            crownPath,
            brush = Brush.verticalGradient(listOf(AccentGold, SignatureOrange))
        )
        // Sparkles
        for (i in 0..4) {
            val sx = centerX + (kotlin.math.cos(pulse * 3f + i) * size_ * 1.2f)
            val sy = centerY + (kotlin.math.sin(pulse * 3f + i) * size_ * 1.2f)
            drawCircle(
                color = Color.White.copy(alpha = (1f - pulse) * 0.6f),
                radius = w * 0.015f,
                center = Offset(sx, sy)
            )
        }
    }
}

/** Studio: Film studio with camera + lights */
@Composable
fun StudioDemoPreview(modifier: Modifier = Modifier) {
    val pulse = rememberPulse()
    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        drawRect(brush = Brush.verticalGradient(listOf(DeepBg, Color(0xFF12121F))))
        // Camera body
        val camX = w * 0.25f
        val camY = h * 0.3f
        val camW = w * 0.4f
        val camH = h * 0.35f
        drawRoundRect(
            brush = Brush.linearGradient(listOf(SignatureOrange.copy(alpha = 0.6f), SignaturePurple.copy(alpha = 0.6f))),
            topLeft = Offset(camX, camY),
            size = Size(camW, camH),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.03f)
        )
        // Camera lens
        val lensCenter = Offset(camX + camW * 0.5f, camY + camH * 0.5f)
        drawCircle(
            color = Color(0xFF1A1A2E),
            radius = camW * 0.18f,
            center = lensCenter
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(AccentCyan.copy(alpha = 0.8f + pulse * 0.2f), Color.Transparent),
                center = lensCenter,
                radius = camW * 0.15f
            ),
            radius = camW * 0.15f,
            center = lensCenter
        )
        // Recording light (pulsing red)
        drawCircle(
            color = AccentRose.copy(alpha = 0.5f + pulse * 0.5f),
            radius = w * 0.02f,
            center = Offset(camX + camW * 0.85f, camY + camH * 0.15f)
        )
        // Light beams
        for (i in 0..2) {
            val beamAlpha = (0.3f + pulse * 0.4f) * (1f - i * 0.2f)
            drawLine(
                color = AccentGold.copy(alpha = beamAlpha),
                start = Offset(w * (0.1f + i * 0.3f), 0f),
                end = Offset(w * (0.2f + i * 0.3f), h * 0.25f),
                strokeWidth = w * 0.02f
            )
        }
    }
}

/** Dispatch for premium feature previews */
@Composable
fun PremiumFeatureDemoPreview(featureId: String, modifier: Modifier = Modifier) {
    when (featureId) {
        "ai_hub" -> AiHubDemoPreview(modifier)
        "presets" -> PresetsDemoPreview(modifier)
        "pro" -> ProTierDemoPreview(modifier)
        "studio" -> StudioDemoPreview(modifier)
        else -> {
            Canvas(modifier = modifier.fillMaxSize()) {
                drawRect(brush = Brush.linearGradient(listOf(SignatureOrange, SignaturePurple)))
            }
        }
    }
}

// ============================================================
// AI Feature Demo Previews (for AiFeatureHubScreen cards)
// ============================================================

/** Generic AI feature preview based on feature id — animated visual */
@Composable
fun AiFeatureDemoPreview(featureId: String, modifier: Modifier = Modifier) {
    val pulse = rememberPulse()
    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        drawRect(brush = Brush.verticalGradient(listOf(DeepBg, Color(0xFF12121F))))
        when (featureId) {
            "ai_auto_caption", "auto_caption" -> {
                // Caption text bars
                drawRect(
                    brush = Brush.horizontalGradient(listOf(SignatureOrange.copy(0.6f), SignaturePurple.copy(0.6f))),
                    topLeft = Offset(0f, h * 0.15f),
                    size = Size(w, h * 0.5f)
                )
                // Caption bars at bottom
                val captionAlpha = 0.6f + pulse * 0.4f
                drawRoundRect(Color.White.copy(alpha = captionAlpha), Offset(w * 0.1f, h * 0.72f), Size(w * 0.5f, h * 0.06f), cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.02f))
                drawRoundRect(Color.White.copy(alpha = captionAlpha * 0.7f), Offset(w * 0.1f, h * 0.82f), Size(w * 0.35f, h * 0.06f), cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.02f))
            }
            "ai_bg_remove", "bg_remove" -> {
                // Person silhouette with background being removed
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(Color.Transparent, Color(0xFF1A1A2E).copy(alpha = pulse)),
                        center = Offset(w * 0.5f, h * 0.5f),
                        radius = w * 0.5f
                    )
                )
                // Person silhouette
                drawCircle(SignatureOrange.copy(alpha = 0.8f), w * 0.08f, Offset(w * 0.5f, h * 0.3f))
                drawRoundRect(SignatureOrange.copy(alpha = 0.8f), Offset(w * 0.4f, h * 0.4f), Size(w * 0.2f, h * 0.4f), cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.03f))
            }
            "ai_enhance", "ai_upscale", "upscale" -> {
                // Resolution upgrade arrows
                drawRoundRect(Brush.verticalGradient(listOf(SignatureOrange.copy(0.3f), SignaturePurple.copy(0.3f))), Offset(w * 0.1f, h * 0.1f), Size(w * 0.3f, h * 0.25f), cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.02f))
                drawRoundRect(Brush.verticalGradient(listOf(SignatureOrange, SignaturePurple)), Offset(w * 0.5f, h * 0.15f), Size(w * (0.35f + pulse * 0.1f), h * (0.3f + pulse * 0.1f)), cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.02f))
                // Arrow
                drawLine(AccentCyan, Offset(w * 0.42f, h * 0.5f), Offset(w * 0.48f, h * 0.5f), w * 0.02f)
                // Quality stars
                for (i in 0..2) {
                    drawCircle(AccentGold.copy(alpha = 0.8f), w * 0.02f, Offset(w * (0.55f + i * 0.12f), h * 0.75f))
                }
            }
            "ai_stabilize", "stabilize" -> {
                // Stabilization grid
                for (i in 0..4) {
                    drawLine(Color.White.copy(alpha = 0.1f), Offset(i * w / 4f, 0f), Offset(i * w / 4f, h), w * 0.005f)
                    drawLine(Color.White.copy(alpha = 0.1f), Offset(0f, i * h / 4f), Offset(w, i * h / 4f), w * 0.005f)
                }
                // Centering crosshair
                drawCircle(AccentCyan.copy(alpha = 0.6f + pulse * 0.4f), w * 0.06f, Offset(w * 0.5f, h * 0.5f), style = Stroke(w * 0.01f))
                drawLine(AccentCyan, Offset(w * 0.4f, h * 0.5f), Offset(w * 0.6f, h * 0.5f), w * 0.01f)
                drawLine(AccentCyan, Offset(w * 0.5f, h * 0.4f), Offset(w * 0.5f, h * 0.6f), w * 0.01f)
            }
            "ai_denoise", "denoise" -> {
                // Noise particles being removed
                for (i in 0..15) {
                    val x = (kotlin.math.sin(i * 1.7f + pulse * 5f) * 0.5f + 0.5f) * w
                    val y = (kotlin.math.cos(i * 2.3f + pulse * 5f) * 0.5f + 0.5f) * h
                    drawCircle(Color.White.copy(alpha = (1f - pulse) * 0.4f), w * 0.015f, Offset(x, y))
                }
                // Clean result
                drawRoundRect(Brush.verticalGradient(listOf(SignatureOrange.copy(0.5f), SignaturePurple.copy(0.5f))), Offset(w * 0.2f, h * 0.2f), Size(w * 0.6f, h * 0.6f), cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.03f))
            }
            else -> {
                // Default: AI brain nodes
                drawCircle(SignatureOrange.copy(alpha = 0.6f + pulse * 0.3f), w * 0.1f, Offset(w * 0.3f, h * 0.35f))
                drawCircle(SignaturePurple.copy(alpha = 0.6f + pulse * 0.3f), w * 0.1f, Offset(w * 0.7f, h * 0.35f))
                drawCircle(AccentCyan.copy(alpha = 0.6f + pulse * 0.3f), w * 0.08f, Offset(w * 0.5f, h * 0.65f))
                drawLine(SignaturePurple.copy(alpha = 0.4f), Offset(w * 0.3f, h * 0.35f), Offset(w * 0.5f, h * 0.65f), w * 0.01f)
                drawLine(SignaturePurple.copy(alpha = 0.4f), Offset(w * 0.7f, h * 0.35f), Offset(w * 0.5f, h * 0.65f), w * 0.01f)
            }
        }
    }
}

// ─── TRANSITION DEMO PREVIEW ──────────────────────────────────────────────
@Composable
fun TransitionDemoPreview(transitionId: String, modifier: Modifier = Modifier) {
    val pulse = rememberPulse()
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val p = (transitionId.lowercase())
        // Two clip panels representing transition between scenes
        val leftCol = Brush.verticalGradient(listOf(SignatureOrange.copy(0.55f), DeepBg))
        val rightCol = Brush.verticalGradient(listOf(SignaturePurple.copy(0.55f), DeepBg))
        drawRect(leftCol, Offset.Zero, size)
        when {
            p.contains("fade") || p == "none" -> {
                drawRect(rightCol, Offset.Zero, size, alpha = pulse * 0.85f)
            }
            p.contains("zoom") -> {
                val sz = (0.2f + pulse * 0.8f)
                drawRect(rightCol, Offset(w * (0.5f - sz / 2f), h * (0.5f - sz / 2f)), Size(w * sz, h * sz))
            }
            p.contains("slide") || p.contains("push") -> {
                val dir = if (p.contains("right") || p.contains("down")) 1f else -1f
                val off = pulse * w * dir
                drawRect(rightCol, Offset(w - off, 0f), Size(w, h))
            }
            p.contains("spin") || p.contains("rotate") -> {
                rotate(pulse * 180f, pivot = Offset(w * 0.5f, h * 0.5f)) {
                    drawRect(rightCol, Offset(w * 0.1f, h * 0.1f), Size(w * 0.8f, h * 0.8f))
                }
            }
            p.contains("wipe") || p.contains("circle") || p.contains("iris") -> {
                drawCircle(rightCol, (0.1f + pulse * 0.9f) * w, Offset(w * 0.5f, h * 0.5f))
            }
            p.contains("blur") -> {
                drawRect(rightCol, Offset.Zero, size, alpha = pulse)
                for (i in 0..8) drawCircle(Color.White.copy(alpha = (1f - pulse) * 0.3f), w * 0.04f, Offset(w * (i / 8f), h * 0.5f))
            }
            p.contains("glitch") || p.contains("rgb") || p.contains("tv") || p.contains("vhs") -> {
                drawRect(rightCol, Offset.Zero, size, alpha = pulse)
                drawRect(SignatureOrange, Offset(w * 0.2f * (1 - pulse), h * 0.3f), Size(w * 0.4f, h * 0.08f))
                drawRect(AccentCyan, Offset(w * 0.3f * pulse, h * 0.6f), Size(w * 0.5f, h * 0.08f))
            }
            p.contains("flash") || p.contains("burn") || p.contains("leak") || p.contains("light") -> {
                drawRect(Color.White.copy(alpha = pulse * 0.7f), Offset.Zero, size)
                drawCircle(AccentGold.copy(alpha = 0.8f), w * 0.15f, Offset(w * (0.3f + pulse * 0.4f), h * 0.5f))
            }
            p.contains("pixel") || p.contains("mosaic") -> {
                val s = (1f - pulse) * 6 + 2
                for (xx in 0..(w / (s * 8)).toInt()) for (yy in 0..(h / (s * 8)).toInt()) {
                    drawRect(if ((xx + yy) % 2 == 0) SignatureOrange.copy(0.5f) else SignaturePurple.copy(0.5f),
                        Offset(xx * s * 8f, yy * s * 8f), Size(s * 8, s * 8))
                }
            }
            p.contains("split") || p.contains("checker") || p.contains("blind") || p.contains("curtain") -> {
                for (i in 0..4) {
                    val hh = h / 5f
                    drawRect(if (i % 2 == 0) rightCol else leftCol, Offset(0f, i * hh * pulse), Size(w, hh * (1f - pulse * 0.5f)))
                }
            }
            p.contains("shake") || p.contains("bounce") || p.contains("elastic") || p.contains("spring") || p.contains("swing") -> {
                val ox = kotlin.math.sin(pulse * 6.28f) * w * 0.1f
                drawRect(rightCol, Offset(ox, 0f), Size(w, h))
            }
            p.contains("wave") || p.contains("ripple") || p.contains("shatter") -> {
                val path = Path()
                path.moveTo(0f, h * 0.5f)
                for (xx in 0..10) path.lineTo(xx * w / 10f, h * (0.5f + kotlin.math.sin(xx + pulse * 6f) * 0.15f))
                path.lineTo(w, h); path.lineTo(0f, h); path.close()
                drawPath(path, rightCol)
            }
            else -> {
                drawRect(rightCol, Offset(w * pulse, 0f), Size(w, h), alpha = pulse)
            }
        }
    }
}

// ─── ANIMATION (TEXT) DEMO PREVIEW ────────────────────────────────────────
@Composable
fun AnimationDemoPreview(animId: String, modifier: Modifier = Modifier) {
    val pulse = rememberPulse()
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val p = animId.lowercase()
        val textCol = Brush.horizontalGradient(listOf(SignatureOrange, SignaturePurple))
        val baseAlpha = 0.9f
        when {
            p.contains("fade") || p == "none" -> {
                drawRoundRect(textCol, Offset(w * 0.2f, h * 0.4f), Size(w * 0.6f, h * 0.2f), cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.02f), alpha = baseAlpha * pulse)
            }
            p.contains("typewriter") || p.contains("marquee") || p.contains("scroll") -> {
                val cw = w * 0.6f * pulse
                drawRoundRect(textCol, Offset(w * 0.2f, h * 0.4f), Size(cw, h * 0.2f), cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.02f))
                if (pulse > 0.9f) drawRect(Color.White, Offset(w * 0.2f + cw, h * 0.4f), Size(w * 0.02f, h * 0.2f))
            }
            p.contains("zoom") || p.contains("pop") || p.contains("explode") || p.contains("implode") -> {
                val s = if (p.contains("implode")) (1f - pulse) else pulse
                val ww = w * 0.6f * s; val hh = h * 0.2f * s
                drawRoundRect(textCol, Offset(w * 0.5f - ww / 2f, h * 0.5f - hh / 2f), Size(ww, hh), cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.02f))
            }
            p.contains("slide") -> {
                val dir = if (p.contains("right") || p.contains("down")) -1f else 1f
                val off = (1f - pulse) * w * dir
                drawRoundRect(textCol, Offset(w * 0.2f + off, h * 0.4f), Size(w * 0.6f, h * 0.2f), cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.02f))
            }
            p.contains("rotate") || p.contains("flip") || p.contains("spin") -> {
                rotate(pulse * 360f, pivot = Offset(w * 0.5f, h * 0.5f)) {
                    drawRoundRect(textCol, Offset(w * 0.25f, h * 0.4f), Size(w * 0.5f, h * 0.2f), cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.02f))
                }
            }
            p.contains("bounce") || p.contains("elastic") || p.contains("spring") || p.contains("rubber") || p.contains("swing") -> {
                val oy = kotlin.math.sin(pulse * 6.28f) * h * 0.2f
                drawRoundRect(textCol, Offset(w * 0.2f, h * 0.4f + oy), Size(w * 0.6f, h * 0.2f), cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.02f))
            }
            p.contains("shake") || p.contains("glitch") || p.contains("flicker") -> {
                val ox = kotlin.math.sin(pulse * 20f) * w * 0.05f
                drawRect(SignatureOrange, Offset(w * 0.2f + ox, h * 0.38f), Size(w * 0.6f, h * 0.08f))
                drawRect(AccentCyan, Offset(w * 0.2f - ox, h * 0.5f), Size(w * 0.6f, h * 0.08f))
            }
            p.contains("pulse") || p.contains("blink") || p.contains("neon") || p.contains("glow") -> {
                drawRoundRect(textCol, Offset(w * 0.2f, h * 0.4f), Size(w * 0.6f, h * 0.2f), cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.02f), alpha = 0.3f + pulse * 0.7f)
                drawRect(textCol, Offset(w * 0.2f, h * 0.4f), Size(w * 0.6f, h * 0.2f), style = Stroke(w * 0.01f * (1f + pulse)))
            }
            p.contains("wave") -> {
                val path = Path()
                path.moveTo(w * 0.2f, h * 0.5f)
                for (xx in 0..10) path.lineTo(w * 0.2f + xx * w * 0.06f, h * (0.5f + kotlin.math.sin(xx + pulse * 6f) * 0.1f))
                drawPath(path, textCol, style = Stroke(w * 0.04f))
            }
            p.contains("rainbow") || p.contains("color") || p.contains("fire") -> {
                val cols = listOf(SignatureOrange, AccentGold, AccentCyan, SignaturePurple)
                for (i in cols.indices) drawRect(cols[i].copy(alpha = baseAlpha), Offset(w * (0.15f + i * 0.15f + pulse * 0.05f), h * 0.4f), Size(w * 0.12f, h * 0.2f))
            }
            p.contains("frozen") || p.contains("metallic") || p.contains("gold") -> {
                val c = if (p.contains("gold")) AccentGold else AccentCyan
                drawRoundRect(Brush.horizontalGradient(listOf(c.copy(0.5f), Color.White, c.copy(0.5f))), Offset(w * 0.2f, h * 0.4f), Size(w * 0.6f, h * 0.2f), cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.02f))
            }
            else -> {
                drawRoundRect(textCol, Offset(w * 0.2f, h * 0.4f), Size(w * 0.6f, h * 0.2f), cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.02f), alpha = baseAlpha * pulse)
            }
        }
    }
}

// ════════════════════════════════════════════════════════════════════════════
// ULTRA REDesign v2 — NEW dramatic feature showcase previews
// These are LARGER, more visually striking animated Canvas demos that
// represent the synced CapCut / VN / YouCut / KineMaster feature set.
// ════════════════════════════════════════════════════════════════════════════

/**
 * CapCut-style Templates Browser demo — animated template cards flipping through
 * a carousel with gradient covers and play buttons.
 */
@Composable
fun TemplatesBrowserDemoPreview(modifier: Modifier = Modifier) {
    val pulse = rememberPulse()
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        drawRect(DeepBg, Offset.Zero, size)
        // background aurora
        drawRect(
            Brush.linearGradient(
                listOf(SignaturePurple.copy(alpha = 0.15f), SignatureOrange.copy(alpha = 0.1f)),
                Offset.Zero, Offset(w, h)
            ), Offset.Zero, size
        )
        // 3 template cards in a row, animated slide
        val cols = listOf(
            listOf(SignatureOrange, AccentRose),
            listOf(SignaturePurple, AccentCyan),
            listOf(AccentGold, SignatureOrange)
        )
        for (i in 0..2) {
            val cardW = w * 0.26f
            val cardH = h * 0.62f
            val xOff = w * 0.06f + i * (cardW + w * 0.04f) + kotlin.math.sin(pulse * 2f + i) * w * 0.015f
            val yOff = h * 0.18f + kotlin.math.cos(pulse * 1.5f + i * 0.7f) * h * 0.03f
            drawRoundRect(
                Brush.verticalGradient(cols[i]),
                Offset(xOff, yOff),
                Size(cardW, cardH),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(cardW * 0.12f)
            )
            // play button circle
            drawCircle(
                Color.White.copy(alpha = 0.9f),
                cardW * 0.14f,
                center = Offset(xOff + cardW / 2f, yOff + cardH / 2f)
            )
            drawCircle(
                Color.Black,
                cardW * 0.1f,
                center = Offset(xOff + cardW / 2f, yOff + cardH / 2f)
            )
        }
        // bottom "Trending" label bar
        drawRoundRect(
            Color.White.copy(alpha = 0.08f),
            Offset(w * 0.06f, h * 0.86f),
            Size(w * 0.4f, h * 0.08f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(h * 0.04f)
        )
    }
}

/**
 * AI Tools Hub demo — shows AI auto-caption, bg-remove, beat-sync icons
 * with animated scanning lines and pulsing neural network nodes.
 */
@Composable
fun AiToolsHubDemoPreview(modifier: Modifier = Modifier) {
    val pulse = rememberPulse()
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        drawRect(DeepBg, Offset.Zero, size)
        // gradient bg
        drawRect(
            Brush.radialGradient(
                listOf(AccentCyan.copy(alpha = 0.2f), DeepBg),
                center = Offset(w / 2f, h / 2f),
                radius = w * 0.6f
            ), Offset.Zero, size
        )
        // animated scanning line
        val scanY = h * (0.2f + (pulse % 1f) * 0.6f)
        drawRect(
            AccentCyan.copy(alpha = 0.5f),
            Offset(w * 0.1f, scanY),
            Size(w * 0.8f, 2f)
        )
        // 3 AI feature icons — circles with pulse
        val icons = listOf(SignatureOrange, SignaturePurple, AccentCyan)
        val labels = listOf("AI", "BG", "SYNC")
        for (i in icons.indices) {
            val cx = w * (0.22f + i * 0.28f)
            val cy = h * 0.45f
            val r = w * 0.08f + kotlin.math.sin(pulse * 3f + i) * w * 0.01f
            drawCircle(icons[i].copy(alpha = 0.3f), r * 1.4f, center = Offset(cx, cy))
            drawCircle(icons[i], r, center = Offset(cx, cy))
            drawCircle(Color.White, r * 0.3f, center = Offset(cx, cy))
        }
        // neural network connection lines
        for (i in 0..2) {
            val x1 = w * (0.22f + i * 0.28f)
            val y1 = h * 0.45f
            if (i < 2) {
                val x2 = w * (0.22f + (i + 1) * 0.28f)
                drawLine(
                    Color.White.copy(alpha = 0.2f),
                    Offset(x1, y1),
                    Offset(x2, y1),
                    strokeWidth = 1.5f
                )
            }
        }
    }
}

/**
 * KineMaster-style Multi-Track Timeline demo — shows multiple layers
 * (video, audio, text, sticker) with colored clips and a moving playhead.
 */
@Composable
fun MultiTrackTimelineDemoPreview(modifier: Modifier = Modifier) {
    val pulse = rememberPulse()
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        drawRect(DeepBg, Offset.Zero, size)
        // 4 tracks: video (orange), audio (cyan), text (purple), sticker (rose)
        val tracks = listOf(
            Triple(SignatureOrange, 0.12f, 0.7f),
            Triple(AccentCyan, 0.30f, 0.55f),
            Triple(SignaturePurple, 0.48f, 0.4f),
            Triple(AccentRose, 0.66f, 0.85f)
        )
        for ((idx, t) in tracks.withIndex()) {
            val (col, yStart, clipFrac) = t
            val yOff = h * yStart
            val trackH = h * 0.14f
            // track background
            drawRoundRect(
                Color.White.copy(alpha = 0.05f),
                Offset(w * 0.02f, yOff),
                Size(w * 0.96f, trackH),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(trackH * 0.3f)
            )
            // clips on track
            val numClips = 2 + idx
            val clipW = w * clipFrac / numClips * 0.9f
            for (c in 0 until numClips) {
                val cx = w * 0.05f + c * (clipW + w * 0.02f)
                drawRoundRect(
                    Brush.horizontalGradient(listOf(col, col.copy(alpha = 0.6f))),
                    Offset(cx, yOff + trackH * 0.1f),
                    Size(clipW, trackH * 0.8f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(trackH * 0.15f)
                )
            }
        }
        // moving playhead
        val playX = w * (0.1f + (pulse * 0.5f % 1f) * 0.8f)
        drawRect(SignatureOrange, Offset(playX, h * 0.1f), Size(2f, h * 0.8f))
        drawCircle(SignatureOrange, h * 0.03f, center = Offset(playX, h * 0.08f))
    }
}

/**
 * YouCut-style Speed Dial demo — circular speed control with pointer
 * sweeping from 0.25x to 4x, showing speed multiplier.
 */
@Composable
fun SpeedDialDemoPreview(modifier: Modifier = Modifier) {
    val pulse = rememberPulse()
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        drawRect(DeepBg, Offset.Zero, size)
        val cx = w / 2f
        val cy = h * 0.55f
        val r = w * 0.32f
        // outer ring
        drawCircle(Color.White.copy(alpha = 0.1f), r, center = Offset(cx, cy))
        drawCircle(SignatureOrange.copy(alpha = 0.3f), r * 0.92f, center = Offset(cx, cy))
        // speed segments
        val speeds = listOf("0.25", "0.5", "1x", "2x", "4x")
        for (i in speeds.indices) {
            val angle = -90f + i * (180f / (speeds.size - 1)) - 90f
            val rad = kotlin.math.PI.toFloat() * angle / 180f
            val sx = cx + kotlin.math.cos(rad) * r * 0.7f
            val sy = cy + kotlin.math.sin(rad) * r * 0.7f
            drawCircle(SignaturePurple.copy(alpha = 0.4f), w * 0.025f, center = Offset(sx, sy))
        }
        // animated pointer
        val ptrAngle = -180f + (pulse * 0.5f % 1f) * 180f
        val ptrRad = kotlin.math.PI.toFloat() * ptrAngle / 180f
        drawLine(
            SignatureOrange,
            Offset(cx, cy),
            Offset(cx + kotlin.math.cos(ptrRad) * r * 0.75f, cy + kotlin.math.sin(ptrRad) * r * 0.75f),
            strokeWidth = 3f
        )
        drawCircle(SignatureOrange, w * 0.04f, center = Offset(cx, cy))
        drawCircle(Color.White, w * 0.015f, center = Offset(cx, cy))
    }
}

/**
 * Keyframe Animation demo (KineMaster) — diamond keyframe markers on a
 * timeline with animated position/scale/rotation interpolation.
 */
@Composable
fun KeyframeAnimationDemoPreview(modifier: Modifier = Modifier) {
    val pulse = rememberPulse()
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        drawRect(DeepBg, Offset.Zero, size)
        // animated curve path
        val path = Path()
        path.moveTo(w * 0.05f, h * 0.75f)
        path.cubicTo(w * 0.3f, h * 0.2f, w * 0.7f, h * 0.8f, w * 0.95f, h * 0.25f)
        drawPath(
            path,
            Brush.horizontalGradient(listOf(SignatureOrange, SignaturePurple)),
            style = Stroke(width = 3f)
        )
        // keyframe diamonds at fixed positions
        val keyframes = listOf(0.05f, 0.3f, 0.55f, 0.8f, 0.95f)
        for (kf in keyframes) {
            val kx = w * kf
            val ky = h * (0.75f - kotlin.math.sin(kf * kotlin.math.PI.toFloat()) * 0.5f)
            // diamond shape
            val ds = w * 0.03f
            drawLine(SignatureOrange, Offset(kx, ky - ds), Offset(kx + ds, ky), strokeWidth = 2f)
            drawLine(SignatureOrange, Offset(kx + ds, ky), Offset(kx, ky + ds), strokeWidth = 2f)
            drawLine(SignatureOrange, Offset(kx, ky + ds), Offset(kx - ds, ky), strokeWidth = 2f)
            drawLine(SignatureOrange, Offset(kx - ds, ky), Offset(kx, ky - ds), strokeWidth = 2f)
        }
        // moving interpolation point
        val t = pulse * 0.5f % 1f
        val px = w * (0.05f + t * 0.9f)
        val py = h * (0.75f - kotlin.math.sin(t * kotlin.math.PI.toFloat()) * 0.5f)
        drawCircle(AccentCyan, w * 0.035f, center = Offset(px, py))
        drawCircle(Color.White, w * 0.015f, center = Offset(px, py))
    }
}

/**
 * Blend Modes demo (KineMaster) — two overlapping circles with
 * animated blend mode transitions.
 */
@Composable
fun BlendModesDemoPreview(modifier: Modifier = Modifier) {
    val pulse = rememberPulse()
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        drawRect(DeepBg, Offset.Zero, size)
        val t = kotlin.math.sin(pulse * 2f) * 0.5f + 0.5f
        // circle 1 — orange
        drawCircle(
            SignatureOrange.copy(alpha = 0.7f),
            w * 0.18f,
            center = Offset(w * (0.38f - t * 0.08f), h * 0.45f)
        )
        // circle 2 — purple (overlapping)
        drawCircle(
            SignaturePurple.copy(alpha = 0.7f),
            w * 0.18f,
            center = Offset(w * (0.62f + t * 0.08f), h * 0.45f)
        )
        // overlap glow
        drawCircle(
            AccentCyan.copy(alpha = 0.5f * t),
            w * 0.08f,
            center = Offset(w * 0.5f, h * 0.45f)
        )
    }
}

/**
 * Aspect Ratio Switch demo (YouCut) — animated frame that morphs between
 * 16:9, 9:16, and 1:1 aspect ratios.
 */
@Composable
fun AspectRatioDemoPreview(modifier: Modifier = Modifier) {
    val pulse = rememberPulse()
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        drawRect(DeepBg, Offset.Zero, size)
        val t = pulse * 0.33f % 1f
        // 3 aspect ratios cycling
        val aspect = when {
            t < 0.33f -> 1.78f // 16:9
            t < 0.66f -> 0.56f // 9:16
            else -> 1.0f       // 1:1
        }
        val frameH = h * 0.7f
        val frameW = frameH * aspect
        val fx = (w - frameW) / 2f
        val fy = h * 0.15f
        drawRoundRect(
            Brush.verticalGradient(listOf(SignatureOrange, SignaturePurple)),
            Offset(fx, fy),
            Size(frameW, frameH),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(frameW * 0.05f)
        )
        // grid lines
        drawLine(Color.White.copy(alpha = 0.2f), Offset(fx + frameW / 3f, fy), Offset(fx + frameW / 3f, fy + frameH), strokeWidth = 1f)
        drawLine(Color.White.copy(alpha = 0.2f), Offset(fx + frameW * 2f / 3f, fy), Offset(fx + frameW * 2f / 3f, fy + frameH), strokeWidth = 1f)
        drawLine(Color.White.copy(alpha = 0.2f), Offset(fx, fy + frameH / 3f), Offset(fx + frameW, fy + frameH / 3f), strokeWidth = 1f)
        // ratio label
        val ratioText = when {
            t < 0.33f -> "16:9"
            t < 0.66f -> "9:16"
            else -> "1:1"
        }
        // small badge
        drawRoundRect(
            Color.Black.copy(alpha = 0.6f),
            Offset(w * 0.35f, h * 0.88f),
            Size(w * 0.3f, h * 0.08f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(h * 0.04f)
        )
    }
}

/**
 * Social Media Share demo (CapCut) — platform icons with animated
 * share pulse radiating outward.
 */
@Composable
fun SocialShareDemoPreview(modifier: Modifier = Modifier) {
    val pulse = rememberPulse()
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        drawRect(DeepBg, Offset.Zero, size)
        val cx = w / 2f
        val cy = h / 2f
        // radiating share pulse
        for (i in 0..3) {
            val t = (pulse + i * 0.25f) % 1f
            drawCircle(
                SignatureOrange.copy(alpha = (1f - t) * 0.3f),
                w * (0.1f + t * 0.35f),
                center = Offset(cx, cy)
            )
        }
        // 5 platform circles
        val platforms = listOf(SignatureOrange, SignaturePurple, AccentCyan, AccentRose, AccentGold)
        for (i in platforms.indices) {
            val angle = -90f + i * (360f / platforms.size)
            val rad = kotlin.math.PI.toFloat() * angle / 180f
            val px = cx + kotlin.math.cos(rad) * w * 0.28f
            val py = cy + kotlin.math.sin(rad) * w * 0.28f
            drawCircle(platforms[i].copy(alpha = 0.3f), w * 0.06f, center = Offset(px, py))
            drawCircle(platforms[i], w * 0.04f, center = Offset(px, py))
        }
        // center share icon
        drawCircle(SignatureOrange, w * 0.05f, center = Offset(cx, cy))
        drawCircle(Color.White, w * 0.02f, center = Offset(cx, cy))
    }
}

/**
 * Export Pipeline demo — animated progress showing the export process
 * with FFmpeg pipeline visualization.
 */
@Composable
fun ExportPipelineDemoPreview(modifier: Modifier = Modifier) {
    val pulse = rememberPulse()
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        drawRect(DeepBg, Offset.Zero, size)
        // progress bar
        val progress = pulse * 0.5f % 1f
        drawRoundRect(
            Color.White.copy(alpha = 0.08f),
            Offset(w * 0.08f, h * 0.35f),
            Size(w * 0.84f, h * 0.1f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(h * 0.05f)
        )
        drawRoundRect(
            Brush.horizontalGradient(listOf(SignatureOrange, SignaturePurple)),
            Offset(w * 0.08f, h * 0.35f),
            Size(w * 0.84f * progress, h * 0.1f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(h * 0.05f)
        )
        // pipeline nodes
        val nodes = listOf("FX", "TXT", "MIX", "MP4")
        for (i in nodes.indices) {
            val nx = w * (0.15f + i * 0.235f)
            val ny = h * 0.65f
            val active = progress * 4f > i
            val col = if (active) SignatureOrange else Color.White.copy(alpha = 0.2f)
            drawCircle(col, w * 0.035f, center = Offset(nx, ny))
            if (i < 3) {
                drawLine(
                    if (active) SignatureOrange.copy(alpha = 0.6f) else Color.White.copy(alpha = 0.1f),
                    Offset(nx + w * 0.035f, ny),
                    Offset(nx + w * 0.2f, ny),
                    strokeWidth = 2f
                )
            }
        }
        // 8K badge
        drawRoundRect(
            Brush.horizontalGradient(listOf(SignatureOrange, SignaturePurple)),
            Offset(w * 0.35f, h * 0.82f),
            Size(w * 0.3f, h * 0.1f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(h * 0.05f)
        )
    }
}
