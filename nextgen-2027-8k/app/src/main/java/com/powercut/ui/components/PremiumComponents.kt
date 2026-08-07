package com.powercut.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Text
import com.powercut.ui.theme.BgCard
import com.powercut.ui.theme.GlassStroke
import com.powercut.ui.theme.Orange
import com.powercut.ui.theme.Purple
import com.powercut.ui.theme.White

/** Orange (#FF5A3C) → Purple (#9D4EDD) diagonal gradient brush. */
@Composable
fun powercutGradientBrush(
    start: Color = Orange,
    end: Color = Purple
): Brush = Brush.linearGradient(
    colors = listOf(start, end),
    start = Offset(0f, 0f),
    end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
)

/** Glassmorphism card: semi-transparent surface + subtle white border. */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 20.dp,
    alpha: Float = 0.6f,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius))
            .background(BgCard.copy(alpha = alpha))
            .border(BorderStroke(1.dp, GlassStroke), RoundedCornerShape(cornerRadius)),
        contentAlignment = Alignment.Center
    ) { content() }
}

/** Small "PRO" badge (top-right corner of premium tools). */
@Composable
fun ProBadge(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(powercutGradientBrush())
            .padding(horizontal = 6.dp, vertical = 2.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "PRO",
            color = White,
            fontSize = 9.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 0.5.sp
        )
    }
}

/**
 * Premium gradient pill button used across the editor bottom toolbar + every
 * tool screen CTA. Orange→purple gradient, 28dp rounded, white icon+text,
 * optional PRO badge top-right, 60fps press-scale animation.
 */
@Composable
fun GradientPill(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: @Composable (() -> Unit)? = null,
    selected: Boolean = false,
    pro: Boolean = false,
    enabled: Boolean = true,
    cornerRadius: Dp = 28.dp,
    horizontalPadding: Dp = 16.dp,
    verticalPadding: Dp = 10.dp
) {
    var pressed by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.94f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessMedium),
        label = "pill-press"
    )
    Box(
        modifier = modifier
            .scale(scale)
            .pointerInput(enabled) {
                if (!enabled) return@pointerInput
                detectTapGestures(
                    onPress = {
                        pressed = true
                        tryAwaitRelease()
                        pressed = false
                    },
                    onTap = { onClick() }
                )
            }
            .clip(RoundedCornerShape(cornerRadius))
            .background(
                if (selected) Brush.linearGradient(listOf(Purple, Orange))
                else powercutGradientBrush()
            )
            .then(if (!enabled) Modifier.background(BgCard.copy(alpha = 0.5f)) else Modifier)
    ) {
        Row(
            modifier = Modifier.padding(
                PaddingValues(horizontal = horizontalPadding, vertical = verticalPadding)
            ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) {
                icon()
            }
            if (text.isNotEmpty()) {
                if (icon != null) androidx.compose.foundation.layout.Spacer(
                    Modifier.size(6.dp)
                )
                Text(
                    text = text,
                    color = White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
        if (pro) {
            ProBadge(modifier = Modifier.align(Alignment.TopEnd).padding(4.dp))
        }
    }
}

/** Large gradient ring progress (export overlay). 0..100. */
@Composable
fun GradientRingProgress(
    percent: Int,
    modifier: Modifier = Modifier,
    size: Dp = 180.dp,
    stroke: Dp = 12.dp
) {
    val angle by animateFloatAsState(
        targetValue = percent / 100f * 360f,
        animationSpec = tween(durationMillis = 400),
        label = "ring-angle"
    )
    Box(modifier = modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.size(size)) {
            val strokePx = stroke.toPx()
            val dia = size.toPx() - strokePx
            val topLeft = Offset((size.toPx() - dia) / 2f, (size.toPx() - dia) / 2f)
            // track
            drawArc(
                color = BgCard,
                startAngle = 0f, sweepAngle = 360f, useCenter = false,
                topLeft = topLeft, size = androidx.compose.ui.geometry.Size(dia, dia),
                style = Stroke(width = strokePx)
            )
            // gradient progress
            drawArc(
                brush = Brush.sweepGradient(listOf(Orange, Purple, Orange)),
                startAngle = -90f, sweepAngle = angle, useCenter = false,
                topLeft = topLeft, size = androidx.compose.ui.geometry.Size(dia, dia),
                style = Stroke(width = strokePx)
            )
        }
        Text(
            text = "$percent%",
            color = White,
            fontSize = 36.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

/** Thin gradient linear progress bar. */
@Composable
fun GradientLinearProgress(
    percent: Int,
    modifier: Modifier = Modifier,
    height: Dp = 6.dp
) {
    val animated by animateFloatAsState(
        targetValue = (percent.coerceIn(0, 100)) / 100f,
        animationSpec = tween(300), label = "lin-prog"
    )
    Box(
        modifier = modifier
            .height(height)
            .clip(RoundedCornerShape(height))
            .background(BgCard)
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(animated)
                .clip(RoundedCornerShape(height))
                .background(powercutGradientBrush())
        )
    }
}

/**
 * Compact gradient pill — a small, self-contained button used as the "Close"
 * affordance on tool screens. Wraps [GradientPill] with sensible defaults.
 */
@Composable
fun GradientPillCompact(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    GradientPill(
        text = text,
        onClick = onClick,
        modifier = modifier,
        cornerRadius = 20.dp,
        horizontalPadding = 18.dp,
        verticalPadding = 8.dp
    )
}
