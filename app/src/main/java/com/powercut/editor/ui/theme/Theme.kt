package com.powercut.editor.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * Premium 2027 NextGen Pro color scheme.
 * A refined dark-only palette tuned for a world-class editor interface with
 * deep obsidian surfaces, electric-violet/coral accents, and OLED-true blacks.
 */
private val PremiumColorScheme = darkColorScheme(
    primary = AccentSecondary,
    onPrimary = Color.White,
    secondary = AccentPrimary,
    onSecondary = Color.White,
    tertiary = AccentTertiary,
    onTertiary = Color.Black,
    background = BackgroundPrimary,
    onBackground = OnPrimary,
    surface = Surface,
    onSurface = OnPrimary,
    surfaceVariant = SurfaceVariant,
    onSurfaceVariant = OnSurfaceSecondary,
    outline = OutlineColor,
    error = PremiumError,
    onError = Color.White
)

/**
 * Light mode counterpart of the Premium 2027 NextGen Pro color scheme.
 * Uses lighter backgrounds, darker text, and preserves the same accent colors
 * (electric-violet, solar coral, aurora teal) for brand consistency.
 */
private val LightColorScheme = lightColorScheme(
    primary = AccentSecondary,
    onPrimary = Color.White,
    secondary = AccentPrimary,
    onSecondary = Color.White,
    tertiary = AccentTertiary,
    onTertiary = Color.Black,
    background = Color(0xFFF6F8FC),
    onBackground = Color(0xFF0F1117),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF0F1117),
    surfaceVariant = Color(0xFFE8ECF4),
    onSurfaceVariant = Color(0xFF4A5568),
    outline = Color(0xFF0F1117).copy(alpha = 0.12f),
    error = PremiumError,
    onError = Color.White
)

@Composable
fun PowerCutTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) PremiumColorScheme else LightColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            // Edge-to-edge transparent bars for a modern immersive editor
            window.statusBarColor = Color.Transparent.toArgb()
            if (darkTheme) {
                window.navigationBarColor = Color(0xFF07080D).toArgb()
            } else {
                window.navigationBarColor = Color(0xFFF6F8FC).toArgb()
            }
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = !darkTheme
            controller.isAppearanceLightNavigationBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
