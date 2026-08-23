package com.powercut.editor.ui.premium

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.powercut.editor.domain.premium.PremiumFeature
import com.powercut.editor.domain.premium.PremiumFeatureCatalog
import com.powercut.editor.ui.editor.EditorViewModel
import com.powercut.editor.ui.theme.AccentPrimary
import com.powercut.editor.ui.theme.AccentSecondary
import com.powercut.editor.ui.theme.AccentTertiary
import com.powercut.editor.ui.theme.CyberCyan
import com.powercut.editor.ui.theme.DarkBgStart
import com.powercut.editor.ui.theme.NeonOrange
import com.powercut.editor.ui.theme.OnPrimary
import com.powercut.editor.ui.theme.OnSurfaceSecondary
import com.powercut.editor.ui.theme.Surface
import com.powercut.editor.ui.theme.SurfaceVariant
import com.powercut.editor.ui.theme.SignatureOrange
import com.powercut.editor.ui.theme.SignaturePurple
import com.powercut.editor.ui.theme.glassCard3D
import com.powercut.editor.ui.theme.glassmorphic
import com.powercut.editor.ui.home.AiFeatureDemoPreview
import androidx.compose.foundation.Canvas
import com.powercut.editor.ui.theme.neonGlow
import com.powercut.editor.ui.theme.premiumAccentGradient
import com.powercut.editor.ui.theme.tactileClick

// ═══════════════════════════════════════════════════════════════════════════════
//  AI FEATURE HUB  —  v6.0.0
//  A focused browser for the 50+ AI features in PremiumFeatureCatalog.
//  Each feature carries a REAL FFmpeg -vf / -af chain that runs at export.
//  Tapping a feature applies it to the project (activeAiFeature) via the bridge.
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
fun SmartToolsHubScreen(
    viewModel: EditorViewModel,
    onBack: () -> Unit
) {
    val aiFeatures = PremiumFeatureCatalog.aiFeatures
    val activeId = viewModel.activeAiFeature.value

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
                Text("Smart Tools Hub", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = OnPrimary)
                Text(
                    "${aiFeatures.size} smart tools · real FFmpeg chains",
                    fontSize = 11.sp,
                    color = OnSurfaceSecondary
                )
            }
        }

        // ── Active feature banner ──
        val activeChain = PremiumFeatureBridge.activeAiChainPreview(viewModel)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp)
                .neonGlow(
                    color = if (activeId == "none") AccentTertiary else AccentSecondary,
                    shape = RoundedCornerShape(14.dp),
                    glowWidth = 1.dp
                )
                .background(
                    if (activeId == "none") Brush.linearGradient(listOf(SurfaceVariant, SurfaceVariant)) else premiumAccentGradient,
                    RoundedCornerShape(14.dp)
                )
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Column {
                Text(
                    if (activeId == "none") "No smart feature active" else "Active: $activeId",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "FFmpeg: $activeChain",
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace,
                    color = Color.White.copy(alpha = 0.75f),
                    maxLines = 2
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // ── Feature list ──
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(aiFeatures, key = { it.id }) { feature ->
                AiFeatureRow(
                    feature = feature,
                    isActive = feature.id == activeId,
                    onClick = {
                        // Toggle the feature via the bridge — pushes real FFmpeg chain.
                        val pushed = PremiumFeatureBridge.applyAiFeature(viewModel, feature.id)
                        if (!pushed) {
                            // Feature has no video chain: try audio chain as the active feature.
                            val audioChain = PremiumFeatureCatalog.audioChainFor(feature.id)
                            if (audioChain.isNotBlank()) {
                                if (viewModel.activeAiFeature.value == feature.id) {
                                    viewModel.updateAiFeature("none")
                                } else {
                                    viewModel.updateAiFeature(feature.id)
                                }
                            }
                        }
                    }
                )
            }
            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun AiFeatureRow(
    feature: PremiumFeature,
    isActive: Boolean,
    onClick: () -> Unit
) {
    val hasChain = feature.videoChain.isNotBlank() || feature.audioChain.isNotBlank()
    val accent = if (feature.isPro) NeonOrange else if (hasChain) CyberCyan else OnSurfaceSecondary

    // 2027 8K — Premium 3D Glass Card
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .glassCard3D(
                shape = RoundedCornerShape(16.dp),
                glowColor = if (isActive) SignatureOrange else accent.copy(alpha = 0.3f),
                backColor = if (isActive) SurfaceVariant else Surface
            )
            .border(
                width = if (isActive) 2.dp else 1.dp,
                color = if (isActive) SignatureOrange else Color.White.copy(alpha = 0.08f),
                shape = RoundedCornerShape(16.dp)
            )
            .clickable(enabled = hasChain, onClick = onClick)
            .padding(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // 2027 8K LIVE DEMO PREVIEW — animated Canvas showing the AI feature
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(12.dp))
            ) {
                AiFeatureDemoPreview(
                    featureId = feature.id,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(2.dp)
                        .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(3.dp))
                        .padding(horizontal = 2.dp, vertical = 1.dp)
                ) {
                    Text("LIVE", fontSize = 5.sp, fontWeight = FontWeight.Black, color = AccentTertiary)
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        feature.name,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isActive) Color.White else OnPrimary
                    )
                    if (feature.isPro) {
                        Text("PRO", fontSize = 8.sp, fontWeight = FontWeight.ExtraBold, color = NeonOrange)
                    }
                    if (!hasChain) {
                        Text("soon", fontSize = 8.sp, color = OnSurfaceSecondary.copy(alpha = 0.5f))
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    feature.description,
                    fontSize = 10.sp,
                    color = if (isActive) Color.White.copy(alpha = 0.8f) else OnSurfaceSecondary,
                    maxLines = 2
                )
                // Show the real FFmpeg chain so users know it is workable
                val chainText = feature.videoChain.ifBlank { feature.audioChain }
                if (chainText.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        chainText.take(70) + if (chainText.length > 70) "…" else "",
                        fontSize = 8.sp,
                        fontFamily = FontFamily.Monospace,
                        color = accent.copy(alpha = 0.7f),
                        maxLines = 1
                    )
                }
            }

            // Active indicator
            if (isActive) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.White.copy(alpha = 0.25f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("✓", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
    }
}
