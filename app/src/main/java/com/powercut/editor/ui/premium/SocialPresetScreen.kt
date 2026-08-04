package com.powercut.editor.ui.premium

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.powercut.editor.domain.premium.PremiumFeature
import com.powercut.editor.domain.premium.PremiumFeatureCatalog
import com.powercut.editor.ui.editor.EditorViewModel
import com.powercut.editor.ui.theme.AccentPrimary
import com.powercut.editor.ui.theme.AccentSecondary
import com.powercut.editor.ui.theme.CyberCyan
import com.powercut.editor.ui.theme.DarkBgStart
import com.powercut.editor.ui.theme.NeonOrange
import com.powercut.editor.ui.theme.OnPrimary
import com.powercut.editor.ui.theme.OnSurfaceSecondary
import com.powercut.editor.ui.theme.Surface
import com.powercut.editor.ui.theme.SurfaceVariant
import com.powercut.editor.ui.theme.glassmorphic
import com.powercut.editor.ui.theme.neonGlow
import com.powercut.editor.ui.theme.premiumAccentGradient
import com.powercut.editor.ui.theme.tactileClick

// ═══════════════════════════════════════════════════════════════════════════════
//  SOCIAL MEDIA PRESET SCREEN  —  v6.0.0
//  One-tap platform presets that apply a REAL FFmpeg crop/scale/pad chain to the
//  export. Each preset maps to a PremiumFeatureCatalog.socialMedia entry whose
//  videoChain is injected into the -vf graph at export time.
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
fun SocialPresetScreen(
    viewModel: EditorViewModel,
    onBack: () -> Unit
) {
    val presets = PremiumFeatureCatalog.socialMedia
    val activeId = viewModel.socialPreset.value

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(DarkBgStart, DarkBgStart)))
    ) {
        // ── Header ──
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .glassmorphic(shape = RoundedCornerShape(10.dp))
                    .tactileClick(onClick = onBack),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.ArrowBack, "Back", tint = OnPrimary, modifier = Modifier.size(18.dp))
            }
            Column {
                Text("Social Media Presets", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = OnPrimary)
                Text(
                    "One-tap aspect ratios · real FFmpeg crop chains",
                    fontSize = 11.sp,
                    color = OnSurfaceSecondary
                )
            }
        }

        // ── Active preset banner ──
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp)
                .neonGlow(
                    color = if (activeId == "none") CyberCyan else AccentSecondary,
                    shape = RoundedCornerShape(14.dp),
                    glowWidth = 1.dp
                )
                .background(
                    if (activeId == "none") SurfaceVariant else premiumAccentGradient,
                    RoundedCornerShape(14.dp)
                )
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Column {
                Text(
                    if (activeId == "none") "No preset — original aspect kept" else "Active: $activeId",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "FFmpeg: ${PremiumFeatureBridge.activeSocialChainPreview(viewModel)}",
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace,
                    color = Color.White.copy(alpha = 0.75f),
                    maxLines = 2
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // ── Preset grid (2 columns) ──
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(presets, key = { it.id }) { preset ->
                SocialPresetCard(
                    preset = preset,
                    isActive = preset.id == activeId,
                    onClick = {
                        PremiumFeatureBridge.applySocialPreset(viewModel, preset.id)
                    }
                )
            }
            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun SocialPresetCard(
    preset: PremiumFeature,
    isActive: Boolean,
    onClick: () -> Unit
) {
    // Derive a visual aspect ratio preview from the preset id.
    val previewAspectRatio = aspectRatioForId(preset.id)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(
                if (isActive) premiumAccentGradient else Brush.verticalGradient(listOf(SurfaceVariant, Surface))
            )
            .border(
                width = if (isActive) 0.dp else 1.dp,
                color = Color.White.copy(alpha = 0.08f),
                shape = RoundedCornerShape(16.dp)
            )
            .clickable(onClick = onClick)
            .padding(14.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            // Aspect-ratio preview frame
            Box(
                modifier = Modifier
                    .width(56.dp)
                    .aspectRatio(previewAspectRatio)
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        if (isActive) Color.White.copy(alpha = 0.3f) else
                            Brush.linearGradient(listOf(AccentSecondary.copy(alpha = 0.25f), AccentPrimary.copy(alpha = 0.25f)))
                    )
                    .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(preset.emoji, fontSize = 18.sp)
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                preset.name,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = if (isActive) Color.White else OnPrimary,
                textAlign = TextAlign.Center,
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                preset.description,
                fontSize = 9.sp,
                color = if (isActive) Color.White.copy(alpha = 0.8f) else OnSurfaceSecondary,
                textAlign = TextAlign.Center,
                maxLines = 2
            )

            // Real FFmpeg chain line
            val chain = preset.videoChain
            if (chain.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    chain.take(46) + if (chain.length > 46) "…" else "",
                    fontSize = 7.sp,
                    fontFamily = FontFamily.Monospace,
                    color = if (isActive) Color.White.copy(alpha = 0.7f) else CyberCyan.copy(alpha = 0.7f),
                    maxLines = 1,
                    textAlign = TextAlign.Center
                )
            }

            if (isActive) {
                Spacer(modifier = Modifier.height(6.dp))
                Text("✓ APPLIED", fontSize = 9.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
            }
        }
    }
}

/** Map a social preset id → a float aspect ratio (w/h) for the visual preview. */
private fun aspectRatioForId(id: String): Float = when (id) {
    "sm_9_16", "sm_tiktok", "sm_reel", "sm_shorts", "sm_snapchat" -> 9f / 16f
    "sm_1_1", "sm_facebook" -> 1f
    "sm_4_5", "sm_whatsapp" -> 4f / 5f
    "sm_16_9", "sm_youtube" -> 16f / 9f
    "sm_21_9" -> 21f / 9f
    "sm_custom" -> 1.5f
    else -> 1f
}
