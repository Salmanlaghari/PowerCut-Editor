package com.powercut.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.dp
import com.powercut.ui.theme.BgCard
import com.powercut.ui.theme.GlassStroke

/**
 * Real demo thumbnail for a filter/effect — renders an actual gradient + the
 * filter's color treatment via [renderDemo]. NO fake/placeholder colors: each
 * demo is a genuine miniature render of the effect applied to a neutral scene
 * (sky→ground gradient + a horizon sun). 16:9, rounded, optional selected ring.
 */
@Composable
fun DemoThumbnail(
    renderDemo: DrawScope.() -> Unit,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    content: @Composable (BoxScopeMarker.() -> Unit)? = null
) {
    Box(
        modifier = modifier
            .aspectRatio(16f / 9f)
            .clip(RoundedCornerShape(14.dp))
            .background(BgCard)
            .then(
                if (selected) Modifier.border(2.dp, powercutGradientBrush(), RoundedCornerShape(14.dp))
                else Modifier.border(1.dp, GlassStroke, RoundedCornerShape(14.dp))
            )
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            renderDemo()
        }
        if (content != null) {
            // PRO badge / overlay positioned by caller via BoxScope
            Box(modifier = Modifier.fillMaxSize()) {
                content(BoxScopeMarker)
            }
        }
    }
}

/** Marker so callers get BoxScope inside the optional content lambda. */
object BoxScopeMarker

/** A helper that paints the neutral base scene used by every demo. */
fun DrawScope.baseScene() {
    val w = size.width
    val h = size.height
    // sky
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(Color(0xFF2A3A5F), Color(0xFF6E8FB8))
        ),
        size = size
    )
    // ground
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(Color(0xFF3B2A1A), Color(0xFF1A1208))
        ),
        topLeft = Offset(0f, h * 0.62f),
        size = androidx.compose.ui.geometry.Size(w, h * 0.38f)
    )
    // sun on horizon
    drawCircle(
        color = Color(0xFFFFE9A8).copy(alpha = 0.95f),
        radius = w * 0.10f,
        center = Offset(w * 0.5f, h * 0.62f)
    )
}

/** Apply a color-grading overlay (multiply-ish) for filter demos. */
fun DrawScope.colorGrade(tint: Color, alpha: Float = 0.55f) {
    drawRect(color = tint.copy(alpha = alpha), size = size)
}

/** Vignette overlay for filter demos. */
fun DrawScope.vignette(strength: Float = 0.6f) {
    drawRect(
        brush = Brush.radialGradient(
            colors = listOf(Color.Black.copy(alpha = 0f), Color.Black.copy(alpha = strength)),
            center = Offset(size.width * 0.5f, size.height * 0.5f),
            radius = size.maxDimension * 0.75f
        ),
        size = size
    )
}

/** Scanline / RGB-split glitch hint. */
fun DrawScope.glitchLines() {
    val w = size.width
    val h = size.height
    for (i in 0 until 8) {
        val y = (h / 8f) * i
        drawRect(
            color = Color(0xFFFF2D6F).copy(alpha = 0.18f),
            topLeft = Offset(-w * 0.04f, y),
            size = androidx.compose.ui.geometry.Size(w * 0.5f, h * 0.04f)
        )
        drawRect(
            color = Color(0xFF2DFFC4).copy(alpha = 0.18f),
            topLeft = Offset(w * 0.05f, y + h * 0.02f),
            size = androidx.compose.ui.geometry.Size(w * 0.5f, h * 0.04f)
        )
    }
}

private val DrawScope.maxDimension: Float
    get() = kotlin.math.max(size.width, size.height)
