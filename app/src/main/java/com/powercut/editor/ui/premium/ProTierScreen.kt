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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
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
import com.powercut.editor.ui.theme.PremiumGold
import com.powercut.editor.ui.theme.Surface
import com.powercut.editor.ui.theme.SurfaceVariant
import com.powercut.editor.ui.theme.SignatureOrange
import com.powercut.editor.ui.theme.glassCard3D
import com.powercut.editor.ui.theme.glassmorphic
import com.powercut.editor.ui.theme.neonGlow
import com.powercut.editor.ui.theme.premiumAccentGradient
import com.powercut.editor.ui.theme.tactileClick

// ═══════════════════════════════════════════════════════════════════════════════
//  PRO TIER SCREEN  —  v6.0.0
//  Unlocks the Pro tier in the editor (viewModel.unlockProTier) and lists the
//  real Pro capabilities from PremiumFeatureCatalog.proFeatures. Every Pro
//  feature that has a video/audio chain is workable through the export pipeline.
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
fun ProTierScreen(
    viewModel: EditorViewModel,
    onBack: () -> Unit
) {
    val proFeatures = PremiumFeatureCatalog.proFeatures
    val isPro = viewModel.isProTier.value

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
                Text("PowerCut Pro", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = PremiumGold)
                Text(
                    "${proFeatures.size} premium capabilities",
                    fontSize = 11.sp,
                    color = OnSurfaceSecondary
                )
            }
        }

        // ── Unlock banner ──
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp)
                .neonGlow(color = PremiumGold, shape = RoundedCornerShape(16.dp), glowWidth = 2.dp)
                .background(
                    if (isPro) Brush.horizontalGradient(listOf(AccentSecondary, PremiumGold, AccentPrimary))
                    else premiumAccentGradient,
                    RoundedCornerShape(16.dp)
                )
                .tactileClick {
                    if (!isPro) PremiumFeatureBridge.unlockPro(viewModel)
                }
                .padding(horizontal = 20.dp, vertical = 18.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    if (isPro) "👑 PRO UNLOCKED" else "Unlock PowerCut Pro",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    if (isPro) "All premium features are now active"
                    else "Tap to unlock all 300+ premium features",
                    fontSize = 11.sp,
                    color = Color.White.copy(alpha = 0.85f)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // ── Pro feature list ──
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(proFeatures, key = { it.id }) { feature ->
                ProFeatureRow(feature = feature, isProActive = isPro)
            }
            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun ProFeatureRow(feature: PremiumFeature, isProActive: Boolean) {
    val hasChain = feature.videoChain.isNotBlank() || feature.audioChain.isNotBlank()

    // 2027 8K — Premium 3D Glass Card
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .glassCard3D(
                shape = RoundedCornerShape(14.dp),
                glowColor = if (isProActive) PremiumGold else SignatureOrange.copy(alpha = 0.2f),
                backColor = Surface
            )
            .border(
                width = if (isProActive) 2.dp else 1.dp,
                color = if (isProActive) PremiumGold.copy(alpha = 0.5f) else Color.White.copy(alpha = 0.06f),
                shape = RoundedCornerShape(14.dp)
            )
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        Brush.linearGradient(listOf(PremiumGold.copy(alpha = 0.2f), AccentSecondary.copy(alpha = 0.15f)))
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(feature.emoji, fontSize = 20.sp)
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        feature.name,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isProActive) OnPrimary else OnSurfaceSecondary
                    )
                    if (hasChain && isProActive) {
                        Text("●", fontSize = 10.sp, color = CyberCyan)
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    feature.description,
                    fontSize = 10.sp,
                    color = OnSurfaceSecondary,
                    maxLines = 2
                )
            }

            // Lock / unlocked indicator
            Text(
                if (isProActive) "✓" else "🔒",
                fontSize = 16.sp,
                color = if (isProActive) CyberCyan else OnSurfaceSecondary.copy(alpha = 0.5f)
            )
        }
    }
}
