package com.powercut.editor.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
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

@Composable
fun PowerCutTheme(
    content: @Composable () -> Unit
) {
    val colorScheme = PremiumColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            // Edge-to-edge transparent bars for a modern immersive editor
            window.statusBarColor = Color.Transparent.toArgb()
            window.navigationBarColor = Color(0xFF07080D).toArgb()
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = false
            controller.isAppearanceLightNavigationBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
