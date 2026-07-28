package com.powercut.editor.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush

val Purple80 = Color(0xFFD0BCFF)
val PurpleGrey80 = Color(0xFFCCC2DC)
val Pink80 = Color(0xFFEFB8C8)

val Purple40 = Color(0xFF6650a4)
val PurpleGrey40 = Color(0xFF625b71)
val Pink40 = Color(0xFF7D5260)

// PREMIUM STUDIO PRO DESIGN SYSTEM
val BackgroundPrimary = Color(0xFF0B0F1A)
val Surface = Color(0xFF161B26)
val SurfaceVariant = Color(0xFF1C2230)
val AccentPrimary = Color(0xFF7C5CFF)
val AccentSecondary = Color(0xFFFF6B35)
val OnPrimary = Color(0xFFF5F7FA)
val OnSurfaceSecondary = Color(0xFF9CA3AF)
val OutlineColor = Color(0xFFFFFFFF).copy(alpha = 0.08f)

val premiumAccentGradient = Brush.horizontalGradient(
    colors = listOf(Color(0xFFFF6B35), Color(0xFFFF3D7F), Color(0xFF7C5CFF))
)

// Mapping previous design tokens to the new premium palette
val NeonOrange = AccentSecondary
val CyberCyan = AccentPrimary
val DarkBgStart = BackgroundPrimary
val DarkBgEnd = BackgroundPrimary

// 4D Glassmorphic Colors
val GlassBackground = Surface.copy(alpha = 0.72f)
val GlassBorderTop = OutlineColor
val GlassBorderBottom = OutlineColor

val TextPrimary = OnPrimary
val TextSecondary = OnSurfaceSecondary
