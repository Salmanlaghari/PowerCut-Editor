package com.powercut.editor.ui.theme

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
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
 * Applies a highly responsive tactile touch sensation with 60fps-ready spring interpolation.
 */
@Composable
fun Modifier.tactileClick(
    onClick: () -> Unit
): Modifier {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1.0f,
        animationSpec = spring(
            dampingRatio = 0.65f, // Smooth premium physical rebound
            stiffness = 400f
        ),
        label = "tactile_scale"
    )

    return this
        .scale(scale)
        .clickable(
            interactionSource = interactionSource,
            indication = null, // Custom physical touch replaces flat ripples
            onClick = onClick
        )
}

/**
 * Gives a highly detailed, luxurious 4D glassmorphic finish using multi-layered shadows and subtle gradients.
 */
fun Modifier.glassmorphic(
    shape: Shape = RoundedCornerShape(24.dp),
    borderColor: Color = Color(0xFFFFFFFF).copy(alpha = 0.08f),
    backColor: Color = Color(0xFF161B26).copy(alpha = 0.72f)
): Modifier {
    return this
        .shadow(
            elevation = 2.dp,
            shape = shape,
            clip = false,
            ambientColor = Color.Black.copy(alpha = 0.75f),
            spotColor = Color.Black.copy(alpha = 0.75f)
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
                    borderColor.copy(alpha = 0.02f)
                )
            ),
            shape = shape
        )
}

/**
 * Surrounds the element with a sophisticated studio-grade glowing border.
 */
fun Modifier.neonGlow(
    color: Color = Color(0xFFFF6B35), // Premium AccentSecondary
    shape: Shape = RoundedCornerShape(24.dp),
    glowWidth: Dp = 1.5.dp
): Modifier {
    return this
        .shadow(
            elevation = 14.dp,
            shape = shape,
            clip = false,
            ambientColor = color.copy(alpha = 0.35f),
            spotColor = color.copy(alpha = 0.35f)
        )
        .border(
            width = glowWidth,
            brush = Brush.linearGradient(
                colors = listOf(
                    color,
                    Color(0xFF7C5CFF) // electric violet (AccentPrimary)
                )
            ),
            shape = shape
        )
}
