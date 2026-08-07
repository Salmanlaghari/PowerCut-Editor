package com.powercut.editor.ui.premium

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

// ═══════════════════════════════════════════════════════════════
//  PREMIUM NAVIGATION — Entry Point
//  Call this composable from anywhere to launch the full
//  premium features experience.
// ═══════════════════════════════════════════════════════════════

/**
 * Full premium features navigation host.
 * Manages screen transitions between the hub and individual category detail screens.
 *
 * Usage:
 * ```
 * var showPremium by remember { mutableStateOf(false) }
 * if (showPremium) {
 *     PremiumEntryPoint(onExit = { showPremium = false })
 * }
 * ```
 */
@Composable
fun PremiumEntryPoint(
    onExit: () -> Unit
) {
    var currentScreen by remember { mutableStateOf<PremiumScreen>(PremiumScreen.Hub) }

    AnimatedContent(
        targetState = currentScreen,
        transitionSpec = {
            if (targetState is PremiumScreen.CategoryDetail) {
                (slideInHorizontally { it } + fadeIn()) togetherWith
                    (slideOutHorizontally { -it / 3 } + fadeOut())
            } else {
                (slideInHorizontally { -it / 3 } + fadeIn()) togetherWith
                    (slideOutHorizontally { it } + fadeOut())
            }
        },
        label = "premium_nav"
    ) { screen ->
        when (screen) {
            is PremiumScreen.Hub -> {
                PremiumHubScreen(
                    onBack = onExit,
                    onCategoryClick = { categoryId ->
                        currentScreen = PremiumScreen.CategoryDetail(categoryId)
                    }
                )
            }
            is PremiumScreen.CategoryDetail -> {
                CategoryDetailScreen(
                    categoryId = screen.categoryId,
                    onBack = { currentScreen = PremiumScreen.Hub }
                )
            }
        }
    }
}

// ─── Navigation State ──────────────────────────────────────────
sealed class PremiumScreen {
    data object Hub : PremiumScreen()
    data class CategoryDetail(val categoryId: String) : PremiumScreen()
}

// ═══════════════════════════════════════════════════════════════
//  STATS SUMMARY — Quick access to feature counts
// ═══════════════════════════════════════════════════════════════

object PremiumStats {
    val videoEffectsCount = VideoEffects.all.size
    val audioToolsCount = AudioTools.all.size
    val textTypographyCount = TextTypography.all.size
    val transitionsCount = Transitions.all.size
    val colorGradingCount = ColorGrading.all.size
    val exportSettingsCount = ExportSettings.all.size
    val aiFeaturesCount = AIFeatures.all.size
    val stickersOverlaysCount = StickersOverlays.all.size
    val projectSettingsCount = ProjectSettings.all.size

    val totalFeatures = videoEffectsCount + audioToolsCount + textTypographyCount +
            transitionsCount + colorGradingCount + exportSettingsCount +
            aiFeaturesCount + stickersOverlaysCount + projectSettingsCount

    fun summary(): String = buildString {
        appendLine("╔══════════════════════════════════════╗")
        appendLine("║   PowerCut Editor Premium Features   ║")
        appendLine("╠══════════════════════════════════════╣")
        appendLine("║ Video Effects:      ${videoEffectsCount.toString().padStart(3)} options      ║")
        appendLine("║ Audio Tools:        ${audioToolsCount.toString().padStart(3)} options      ║")
        appendLine("║ Text & Typography:  ${textTypographyCount.toString().padStart(3)} options      ║")
        appendLine("║ Transitions:        ${transitionsCount.toString().padStart(3)} options      ║")
        appendLine("║ Color Grading:      ${colorGradingCount.toString().padStart(3)} options      ║")
        appendLine("║ Export Settings:    ${exportSettingsCount.toString().padStart(3)} options      ║")
        appendLine("║ AI Features:        ${aiFeaturesCount.toString().padStart(3)} options      ║")
        appendLine("║ Stickers & Overlays:${stickersOverlaysCount.toString().padStart(3)} options      ║")
        appendLine("║ Project Settings:   ${projectSettingsCount.toString().padStart(3)} options      ║")
        appendLine("╠══════════════════════════════════════╣")
        appendLine("║ TOTAL:              ${totalFeatures.toString().padStart(3)}+ options     ║")
        appendLine("╚══════════════════════════════════════╝")
    }
}
