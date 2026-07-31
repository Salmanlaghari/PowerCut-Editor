package com.powercut.editor.ui.theme

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Applies a highly responsive tactile touch sensation with 60fps-ready spring
 * interpolation. Tuned for a buttery-smooth premium feel.
 */
@Composable
fun Modifier.tactileClick(
    onClick: () -> Unit
): Modifier {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1.0f,
        animationSpec = spring(
            dampingRatio = 0.6f,   // smooth premium physical rebound
            stiffness = 420f
        ),
        label = "tactile_scale"
    )

    return this
        .scale(scale)
        .clickable(
            interactionSource = interactionSource,
            indication = null, // custom physical touch replaces flat ripples
            onClick = onClick
        )
}

/**
 * Premium 2027 glassmorphism v2 — multi-layered shadow + subtle gradient border
 * for a luxurious frosted-glass finish.
 */
fun Modifier.glassmorphic(
    shape: Shape = RoundedCornerShape(24.dp),
    borderColor: Color = GlassBorderTop,
    backColor: Color = GlassBackground
): Modifier {
    return this
        .shadow(
            elevation = 3.dp,
            shape = shape,
            clip = false,
            ambientColor = Color.Black.copy(alpha = 0.8f),
            spotColor = Color.Black.copy(alpha = 0.7f)
        )
        .background(
            color = backColor,
            shape = shape
        )
        .border(
            width = 1.dp,
            brush = Brush.verticalGradient(
                colors = listOf(
                    borderColor,
                    GlassBorderBottom
                )
            ),
            shape = shape
        )
}

/**
 * Studio-grade glowing border — premium aurora glow using a linear gradient.
 */
fun Modifier.neonGlow(
    color: Color = AccentSecondary,
    shape: Shape = RoundedCornerShape(24.dp),
    glowWidth: Dp = 1.5.dp
): Modifier {
    return this
        .shadow(
            elevation = 16.dp,
            shape = shape,
            clip = false,
            ambientColor = color.copy(alpha = 0.4f),
            spotColor = color.copy(alpha = 0.4f)
        )
        .border(
            width = glowWidth,
            brush = Brush.linearGradient(
                colors = listOf(color, AccentPrimary)
            ),
            shape = shape
        )
}

/**
 * Premium aurora gradient background — animated multi-color sweep for hero
 * surfaces and splash screens.
 */
@Composable
fun Modifier.auroraBackground(
    shape: Shape = RoundedCornerShape(24.dp)
): Modifier {
    val transition = rememberInfiniteTransition(label = "aurora")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(6000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "aurora_progress"
    )
    // Shift the gradient stops for a living aurora effect
    val shift = 0.3f + progress * 0.4f
    return this.background(
        brush = Brush.linearGradient(
            colors = listOf(
                AccentPrimary.copy(alpha = 0.25f),
                AccentTertiary.copy(alpha = 0.2f),
                AccentSecondary.copy(alpha = 0.25f)
            ),
            start = androidx.compose.ui.geometry.Offset(0f, 0f),
            end = androidx.compose.ui.geometry.Offset(1000f * shift, 1000f)
        ),
        shape = shape
    )
}

/**
 * Subtle shimmer sweep — used for premium "PRO" badges and loading skeletons.
 */
@Composable
fun Modifier.shimmerOverlay(
    shape: Shape = RoundedCornerShape(8.dp)
): Modifier {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val progress by transition.animateFloat(
        initialValue = -2f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_progress"
    )
    return this.background(
        brush = Brush.linearGradient(
            colors = listOf(
                Color.White.copy(alpha = 0f),
                Color.White.copy(alpha = 0.18f),
                Color.White.copy(alpha = 0f)
            ),
            start = androidx.compose.ui.geometry.Offset(progress * 500f, 0f),
            end = androidx.compose.ui.geometry.Offset(progress * 500f + 200f, 200f)
        ),
        shape = shape
    )
}
