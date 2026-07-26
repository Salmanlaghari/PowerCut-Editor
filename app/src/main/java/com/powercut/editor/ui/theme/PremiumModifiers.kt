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
 * Applies a tactile touch sensation.
 * Depresses slightly on press and springs back when released, feeling physical!
 */
@Composable
fun Modifier.tactileClick(
    onClick: () -> Unit
): Modifier {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.94f else 1.0f,
        animationSpec = spring(
            dampingRatio = 0.55f, // Sweet physical springiness
            stiffness = 300f
        ),
        label = "tactile_scale"
    )

    return this
        .scale(scale)
        .clickable(
            interactionSource = interactionSource,
            indication = null, // Disable default flat ripple to preserve custom physical touch feel
            onClick = onClick
        )
}

/**
 * Gives a futuristic glassmorphic backing with fine frosted edges.
 */
fun Modifier.glassmorphic(
    shape: Shape = RoundedCornerShape(16.dp),
    borderColor: Color = Color.White.copy(alpha = 0.08f),
    backColor: Color = Color(0xFF1A1A22).copy(alpha = 0.8f)
): Modifier {
    return this
        .shadow(
            elevation = 12.dp,
            shape = shape,
            clip = false,
            ambientColor = Color.Black.copy(alpha = 0.6f),
            spotColor = Color.Black.copy(alpha = 0.6f)
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
                    borderColor.copy(alpha = 0.1f)
                )
            ),
            shape = shape
        )
}

/**
 * Surrounds the element with a glowing neon border to indicate premium focus or active state.
 */
fun Modifier.neonGlow(
    color: Color = Color(0xFFFF5722), // Default Neon Orange
    shape: Shape = RoundedCornerShape(16.dp),
    glowWidth: Dp = 2.dp
): Modifier {
    return this
        .shadow(
            elevation = 10.dp,
            shape = shape,
            clip = false,
            ambientColor = color.copy(alpha = 0.45f),
            spotColor = color.copy(alpha = 0.45f)
        )
        .border(
            width = glowWidth,
            brush = Brush.linearGradient(
                colors = listOf(
                    color,
                    Color(0xFF00BCD4) // Gradient from Orange to Cyan
                )
            ),
            shape = shape
        )
}
