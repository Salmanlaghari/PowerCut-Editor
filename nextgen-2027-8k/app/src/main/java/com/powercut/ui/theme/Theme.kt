package com.powercut.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val PowerCutColors = darkColorScheme(
    primary = Orange,
    secondary = Purple,
    tertiary = Purple,
    background = Bg,
    surface = BgElev,
    surfaceVariant = BgCard,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    error = Danger
)

@Composable
fun PowerCutTheme(content: @Composable () -> Unit) {
    // Always dark — premium 2027 8K. isSystemInDarkTheme() ignored by design.
    MaterialTheme(
        colorScheme = PowerCutColors,
        typography = PowerCutTypography,
        content = content
    )
}
