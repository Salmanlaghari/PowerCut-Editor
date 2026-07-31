package com.powercut.editor.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush

// ════════════════════════════════════════════════════════════════════════════
// POWERCUT — PREMIUM 2027 NEXTGEN PRO DESIGN SYSTEM
// World-Class "Ultra Smooth Pro 2027" interface palette
// ════════════════════════════════════════════════════════════════════════════

// Legacy (kept for compatibility — mapped to new tokens below)
val Purple80 = Color(0xFFD0BCFF)
val PurpleGrey80 = Color(0xFFCCC2DC)
val Pink80 = Color(0xFFEFB8C8)
val Purple40 = Color(0xFF6650a4)
val PurpleGrey40 = Color(0xFF625b71)
val Pink40 = Color(0xFF7D5260)

// ── Core Obsidian Surfaces ──────────────────────────────────────────────────
// Deep, true-black adjacent backgrounds for an OLED-friendly premium canvas
val BackgroundPrimary = Color(0xFF07080D)   // near-black obsidian base
val Surface = Color(0xFF0F1117)            // elevated panel
val SurfaceVariant = Color(0xFF161A24)     // secondary panel / cards
val SurfaceTertiary = Color(0xFF1E2330)    // hover / active states
val OnPrimary = Color(0xFFF6F8FC)          // primary text — near white
val OnSurfaceSecondary = Color(0xFF8B93A7) // secondary text — cool grey
val OutlineColor = Color(0xFFFFFFFF).copy(alpha = 0.07f)

// ── Aurora Accent System ────────────────────────────────────────────────────
// A premium duo: electric violet + solar coral, used across gradients & glows
val AccentPrimary = Color(0xFF7C5CFF)      // electric violet
val AccentSecondary = Color(0xFFFF6B35)    // solar coral
val AccentTertiary = Color(0xFF2DD4BF)     // aurora teal (new)
val AccentRose = Color(0xFFFF3D7F)         // rose (new)

// ── Premium Gradients ───────────────────────────────────────────────────────
val premiumAccentGradient = Brush.horizontalGradient(
    colors = listOf(Color(0xFFFF6B35), Color(0xFFFF3D7F), Color(0xFF7C5CFF))
)

val auroraGradient = Brush.linearGradient(
    colors = listOf(
        Color(0xFF7C5CFF),
        Color(0xFF2DD4BF),
        Color(0xFFFF6B35)
    )
)

val obsidianGradient = Brush.verticalGradient(
    colors = listOf(Color(0xFF0B0E16), Color(0xFF07080D))
)

val surfaceGradient = Brush.verticalGradient(
    colors = listOf(Color(0xFF161A24), Color(0xFF0F1117))
)

val coralVioletGradient = Brush.horizontalGradient(
    colors = listOf(Color(0xFFFF6B35), Color(0xFF7C5CFF))
)

val tealVioletGradient = Brush.horizontalGradient(
    colors = listOf(Color(0xFF2DD4BF), Color(0xFF7C5CFF))
)

// Mapping previous design tokens to the new premium palette
val NeonOrange = AccentSecondary
val CyberCyan = AccentPrimary
val DarkBgStart = BackgroundPrimary
val DarkBgEnd = BackgroundPrimary

// 4D Glassmorphic Colors
val GlassBackground = Surface.copy(alpha = 0.72f)
val GlassBorderTop = Color(0xFFFFFFFF).copy(alpha = 0.12f)
val GlassBorderBottom = Color(0xFFFFFFFF).copy(alpha = 0.03f)

val TextPrimary = OnPrimary
val TextSecondary = OnSurfaceSecondary

// ── Premium 2027 Status Colors ──────────────────────────────────────────────
val PremiumGold = Color(0xFFFFD166)
val PremiumSuccess = Color(0xFF34D399)
val PremiumError = Color(0xFFFF5470)
val PremiumInfo = Color(0xFF60A5FA)
