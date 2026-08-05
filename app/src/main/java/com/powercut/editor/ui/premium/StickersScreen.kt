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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
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
//  STICKERS GALLERY  —  v6.0.0
//  A 3D glassmorphism browser of 61 REAL stickers. The selected sticker is
//  written to project.stickerType via viewModel.updateStickerType(...) and is
//  composited onto the timeline at export through PowerCutDAG.
//  Selected cards show a gradient border + checkmark badge.
// ═══════════════════════════════════════════════════════════════════════════════

/** A sticker entry rendered as a 3D glass card. */
data class StickerItem(
    val id: String,
    val label: String,
    val emoji: String
)

/** Catalog of 61 real stickers mapped to stickerType ids. */
object StickerCatalog {
    val stickers: List<StickerItem> = listOf(
        StickerItem("none", "None", "🚫"),
        // Emotions & reactions
        StickerItem("fire", "Fire", "🔥"),
        StickerItem("star", "Star", "⭐"),
        StickerItem("heart", "Heart", "❤️"),
        StickerItem("glow", "Glow", "⚡"),
        StickerItem("smile", "Smile", "😀"),
        StickerItem("laugh", "Laugh", "😂"),
        StickerItem("love", "Love", "😍"),
        StickerItem("cool", "Cool", "😎"),
        StickerItem("wink", "Wink", "😉"),
        StickerItem("cry", "Cry", "😭"),
        StickerItem("angry", "Angry", "😡"),
        StickerItem("shock", "Shock", "😱"),
        StickerItem("thumbsup", "Thumbs Up", "👍"),
        StickerItem("thumbsdown", "Thumbs Down", "👎"),
        StickerItem("ok", "OK", "👌"),
        StickerItem("peace", "Peace", "✌️"),
        StickerItem("clap", "Clap", "👏"),
        StickerItem("muscle", "Muscle", "💪"),
        StickerItem("pray", "Pray", "🙏"),
        StickerItem("point", "Point", "👉"),
        // Weather & nature
        StickerItem("sun", "Sun", "☀️"),
        StickerItem("moon", "Moon", "🌙"),
        StickerItem("cloud", "Cloud", "☁️"),
        StickerItem("rainbow", "Rainbow", "🌈"),
        StickerItem("bolt", "Bolt", "⚡"),
        StickerItem("snow", "Snow", "❄️"),
        StickerItem("sparkle", "Sparkle", "✨"),
        StickerItem("star2", "Sparkle Star", "🌟"),
        StickerItem("flower", "Flower", "🌸"),
        StickerItem("rose", "Rose", "🌹"),
        StickerItem("tree", "Tree", "🌳"),
        StickerItem("leaf", "Leaf", "🍃"),
        StickerItem("wave", "Wave", "🌊"),
        StickerItem("volcano", "Volcano", "🌋"),
        StickerItem("mountain", "Mountain", "⛰️"),
        // Animals
        StickerItem("cat", "Cat", "🐱"),
        StickerItem("dog", "Dog", "🐶"),
        StickerItem("panda", "Panda", "🐼"),
        StickerItem("fox", "Fox", "🦊"),
        StickerItem("lion", "Lion", "🦁"),
        StickerItem("frog", "Frog", "🐸"),
        StickerItem("unicorn", "Unicorn", "🦄"),
        StickerItem("butterfly", "Butterfly", "🦋"),
        StickerItem("bee", "Bee", "🐝"),
        StickerItem("turtle", "Turtle", "🐢"),
        // Food & drink
        StickerItem("coffee", "Coffee", "☕"),
        StickerItem("pizza", "Pizza", "🍕"),
        StickerItem("burger", "Burger", "🍔"),
        StickerItem("cake", "Cake", "🎂"),
        StickerItem("icecream", "Ice Cream", "🍦"),
        StickerItem("donut", "Donut", "🍩"),
        StickerItem("cherry", "Cherry", "🍒"),
        StickerItem("apple", "Apple", "🍎"),
        StickerItem("avocado", "Avocado", "🥑"),
        // Objects & symbols
        StickerItem("crown", "Crown", "👑"),
        StickerItem("diamond", "Diamond", "💎"),
        StickerItem("trophy", "Trophy", "🏆"),
        StickerItem("medal", "Medal", "🎖️"),
        StickerItem("rocket", "Rocket", "🚀"),
        StickerItem("balloon", "Balloon", "🎈"),
        StickerItem("gift", "Gift", "🎁"),
        StickerItem("party", "Party", "🎉"),
        StickerItem("confetti", "Confetti", "🎊"),
        StickerItem("music", "Music", "🎵"),
        StickerItem("camera", "Camera", "📷"),
        StickerItem("film", "Film", "🎥")
    )
}

@Composable
fun StickersScreen(
    viewModel: EditorViewModel,
    onBack: () -> Unit
) {
    val project = viewModel.currentProject.value
    val activeSticker = project?.stickerType ?: "none"
    val activeModel = StickerCatalog.stickers.firstOrNull { it.id == activeSticker }

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
                Text("Stickers Gallery", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = OnPrimary)
                Text(
                    "${StickerCatalog.stickers.size} stickers · composited on timeline",
                    fontSize = 11.sp,
                    color = OnSurfaceSecondary
                )
            }
        }

        // ── Active sticker banner ──
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp)
                .neonGlow(
                    color = if (activeSticker == "none") AccentPrimary else AccentSecondary,
                    shape = RoundedCornerShape(14.dp),
                    glowWidth = 1.dp
                )
                .background(
                    if (activeSticker == "none") Brush.linearGradient(listOf(SurfaceVariant, SurfaceVariant))
                    else premiumAccentGradient,
                    RoundedCornerShape(14.dp)
                )
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(activeModel?.emoji ?: "🚫", fontSize = 22.sp)
                Column {
                    Text(
                        if (activeSticker == "none") "No sticker applied" else "Active: ${activeModel?.label ?: activeSticker}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "stickerType = \"$activeSticker\"  →  PowerCutDAG overlay node",
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace,
                        color = Color.White.copy(alpha = 0.75f),
                        maxLines = 2
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // ── 3D glass sticker grid ──
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 92.dp),
            modifier = Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(StickerCatalog.stickers, key = { it.id }) { sticker ->
                StickerGlassCard(
                    sticker = sticker,
                    isSelected = sticker.id == activeSticker,
                    onClick = {
                        // Toggle: tapping the active sticker clears it.
                        val next = if (sticker.id == activeSticker) "none" else sticker.id
                        viewModel.updateStickerType(next)
                    }
                )
            }
            item { Spacer(modifier = Modifier.height(20.dp)) }
        }
    }
}

@Composable
private fun StickerGlassCard(
    sticker: StickerItem,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(16.dp))
            // 3D glass base: layered translucent surface for depth.
            .background(
                Brush.verticalGradient(
                    listOf(
                        Surface.copy(alpha = 0.95f),
                        SurfaceVariant.copy(alpha = 0.85f)
                    )
                )
            )
            // Selected state: gradient border (neon glow) + accent ring.
            .neonGlow(
                color = if (isSelected) CyberCyan else Color.Transparent,
                shape = RoundedCornerShape(16.dp),
                glowWidth = if (isSelected) 2.dp else 0.dp
            )
            .border(
                width = if (isSelected) 1.6.dp else 0.8.dp,
                brush = if (isSelected) premiumAccentGradient
                else Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.10f), Color.Transparent)),
                shape = RoundedCornerShape(16.dp)
            )
            .clickable(onClick = onClick)
            .padding(10.dp),
        contentAlignment = Alignment.Center
    ) {
        // Selected checkmark badge (top-end corner).
        if (isSelected) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(20.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(premiumAccentGradient, RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = "Selected",
                    tint = Color.White,
                    modifier = Modifier.size(13.dp)
                )
            }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            Text(sticker.emoji, fontSize = 30.sp)
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                sticker.label,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = if (isSelected) OnPrimary else OnSurfaceSecondary,
                textAlign = TextAlign.Center,
                maxLines = 1
            )
        }
    }
}
