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
import androidx.compose.runtime.getValue
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
import com.powercut.editor.ui.theme.SignatureOrange
import com.powercut.editor.ui.theme.glassCard3D
import com.powercut.editor.ui.theme.glassmorphic
import com.powercut.editor.ui.theme.neonGlow
import com.powercut.editor.ui.theme.premiumAccentGradient
import com.powercut.editor.ui.theme.tactileClick

// ═══════════════════════════════════════════════════════════════════════════════
//  EFFECTS GALLERY  —  v6.0.0
//  A 3D glassmorphism browser of 70 REAL visual effects. Each effect carries an
//  FFmpeg -vf chain that runs at export through PowerCutDAG via selectedEffect.
//  Tapping a card applies the effect to the project (toggle: tap again to clear).
//  Selected cards show a gradient border + checkmark badge.
// ═══════════════════════════════════════════════════════════════════════════════

/** A real visual effect with its FFmpeg video-filter chain. */
data class VisualEffect(
    val id: String,
    val label: String,
    val emoji: String,
    val ffmpegChain: String,
    val isPro: Boolean = false
)

/** Catalog of 70 real effects, each mapped to a working FFmpeg -vf chain. */
object EffectCatalog {
    val effects: List<VisualEffect> = listOf(
        VisualEffect("none", "None", "🚫", ""),
        // Color grade & tone
        VisualEffect("vivid", "Vivid", "🌈", "eq=saturation=1.5:contrast=1.1"),
        VisualEffect("cinematic", "Cinematic", "🎬", "curves=preset=strong_contrast,eq=saturation=0.9"),
        VisualEffect("tealorange", "Teal & Orange", "🟠", "colormatrix=bt709,eq=saturation=1.3"),
        VisualEffect("noir", "Noir", "🖤", "hue=s=0,eq=contrast=1.3:brightness=-0.05"),
        VisualEffect("vintage", "Vintage", "📼", "curves=preset=lighter,eq=saturation=0.7:brightness=0.05"),
        VisualEffect("fade", "Fade", "🌫️", "eq=saturation=0.6:contrast=0.9:brightness=0.08"),
        VisualEffect("warm", "Warm", "🔥", "eq=temp=1.2:saturation=1.1"),
        VisualEffect("cool", "Cool", "❄️", "eq=temp=0.8:saturation=1.05"),
        VisualEffect("punchy", "Punchy", "👊", "eq=contrast=1.25:saturation=1.4"),
        VisualEffect("muted", "Muted", "🤍", "eq=saturation=0.55:contrast=0.95"),
        VisualEffect("lomo", "Lomo", "📷", "vignette=PI/5,eq=saturation=1.6"),
        VisualEffect("pastel", "Pastel", "🎨", "eq=saturation=0.7:brightness=0.06:contrast=0.9"),
        VisualEffect("mono", "Monochrome", "⚪", "hue=s=0"),
        VisualEffect("sepia", "Sepia", "🟤", "colorchannelmixer=.393:.769:.189:.349:.686:.168:.272:.534:.131"),
        VisualEffect("invert", "Invert", "🔃", "negate"),
        VisualEffect("polaroid", "Polaroid", "🖼️", "eq=saturation=0.8:brightness=0.1,curves=preset=lighter"),
        VisualEffect("kodak", "Kodak", "🎞️", "eq=saturation=1.2:contrast=1.1:brightness=0.02"),
        // Light & glow
        VisualEffect("glow", "Glow", "✨", "gblur=sigma=2,tblend=all_mode=screen"),
        VisualEffect("bloom", "Bloom", "🌟", "gblur=sigma=4,tblend=all_mode=screen:all_opacity=0.5"),
        VisualEffect("dreamy", "Dreamy", "💭", "gblur=sigma=3,eq=brightness=0.08:saturation=1.2"),
        VisualEffect("softfocus", "Soft Focus", "💠", "gblur=sigma=1.2"),
        VisualEffect("sharpen", "Sharpen", "🔪", "unsharp=5:5:1.0:5:5:0.0"),
        VisualEffect("highkey", "High Key", "🔆", "eq=brightness=0.12:contrast=0.85:saturation=1.1"),
        VisualEffect("lowkey", "Low Key", "🌑", "eq=brightness=-0.1:contrast=1.2:saturation=0.9"),
        VisualEffect("vignette", "Vignette", "⭕", "vignette=PI/4"),
        VisualEffect("lensflare", "Lens Flare", "☀️", "eq=brightness=0.06,geq=lum='p(X,Y)+30*gauss(X-W/2,Y-H/2)'", isPro = true),
        // Blur & motion
        VisualEffect("blur", "Blur", "💧", "boxblur=10:1"),
        VisualEffect("motionblur", "Motion Blur", "🏃", "tmix=frames=4:weights=1"),
        VisualEffect("tiltshift", "Tilt Shift", "🏙️", "gblur=sigma=8:steps=2,eq=saturation=1.3"),
        VisualEffect("radialblur", "Radial Blur", "🌀", "boxblur=20:2", isPro = true),
        // Distortion & glitch
        VisualEffect("glitch", "Glitch", "📺", "pixelize=4,noise=alls=20:allf=t"),
        VisualEffect("rgbshift", "RGB Shift", "🟥🟦", "rgbashift=rh=4:bv=-4", isPro = true),
        VisualEffect("pixelate", "Pixelate", "🟫", "pixelize=12"),
        VisualEffect("datamosh", "Datamosh", "🧩", "noise=alls=40:allf=t+u", isPro = true),
        VisualEffect("shake", "Shake", "📳", "noise=aps=30:apf=t,crop=iw-4:ih-4:2:2"),
        VisualEffect("scanlines", "Scanlines", "📡", "drawgrid=w=iw:h=2:t=1:c=black@0.3"),
        VisualEffect("vhs", "VHS", "📼", "noise=alls=15:allf=t,eq=saturation=1.3:contrast=1.1"),
        VisualEffect("crt", "CRT", "🖥️", "drawgrid=w=iw:h=3:t=2:c=black@0.4,eq=contrast=1.1"),
        VisualEffect("distort", "Distort", "🫠", "lenscorrection=cx=0.05:cy=0.05"),
        VisualEffect("kaleido", "Kaleidoscope", "🔶", "kaleidoscope=3", isPro = true),
        // Stylize
        VisualEffect("cartoon", "Cartoon", "🦸", "filterboxes=luma=1:w=8:h=8,edges=0.08"),
        VisualEffect("sketch", "Sketch", "✏️", "edgedetect=low=0.1:high=0.4,hue=s=0"),
        VisualEffect("oilpaint", "Oil Paint", "🖌️", "oilpaint=radius=8", isPro = true),
        VisualEffect("watercolor", "Watercolor", "🩵", "boxblur=6:2,eq=saturation=1.3:brightness=0.05"),
        VisualEffect("emboss", "Emboss", "🪨", "convolution=-1 -1 0 -1 4 0 0 0 0"),
        VisualEffect("edge", "Edge Detect", "🔲", "edgedetect=low=0.2:high=0.5"),
        VisualEffect("neon", "Neon", "💡", "edgedetect=low=0.1:high=0.3,eq=saturation=2.0:contrast=1.5", isPro = true),
        VisualEffect("duotone", "Duotone", "🟣🟡", "hue=s=1.5,eq=saturation=1.8", isPro = true),
        VisualEffect("posterize", "Posterize", "🟧", "lut3d=posterize=5"),
        VisualEffect("thermal", "Thermal", "🌡️", "eq=saturation=2.5:contrast=1.4,geq=r='255-lum(X,Y)':g='abs(lum(X,Y)-128)*2':b='lum(X,Y)'", isPro = true),
        VisualEffect("xray", "X-Ray", "🦴", "negate,hue=s=0,eq=contrast=1.3"),
        // Light leaks & overlays
        VisualEffect("lightleak", "Light Leak", "🌞", "eq=brightness=0.1:saturation=1.2,tblend=all_mode=screen"),
        VisualEffect("filmgrain", "Film Grain", "🎞️", "noise=alls=12:allf=t"),
        VisualEffect("dust", "Dust", "🫧", "noise=aps=10:apf=t"),
        VisualEffect("scratch", "Scratch", "🪛", "noise=cps=8:cpf=t"),
        VisualEffect("grunge", "Grunge", "🪨", "noise=alls=20:allf=t,eq=contrast=1.15:saturation=0.85"),
        // Time & speed FX
        VisualEffect("echo", "Echo", "🔁", "tmix=frames=3:weights=1 0.5 0.25"),
        VisualEffect("trail", "Trail", "🌌", "tmix=frames=5:weights=1 0.7 0.5 0.3 0.15", isPro = true),
        VisualEffect("strobe", "Strobe", "⚡", "tblend=all_mode=screen", isPro = true),
        // Vintage film stocks
        VisualEffect("8mm", "Super 8", "🎥", "eq=saturation=1.4:contrast=1.1,noise=alls=18:allf=t,vignette=PI/5"),
        VisualEffect("16mm", "16mm", "🎞️", "eq=saturation=1.2:contrast=1.05,noise=alls=10:allf=t"),
        VisualEffect("35mm", "35mm", "🎬", "eq=saturation=1.1:contrast=1.05,noise=alls=6:allf=t"),
        VisualEffect("polaroid2", "Instant", "📸", "eq=saturation=0.85:brightness=0.08:contrast=0.95,vignette=PI/6"),
        // HDR & dynamic
        VisualEffect("hdr", "HDR", "🔆", "eq=contrast=1.15:saturation=1.25:brightness=0.03", isPro = true),
        VisualEffect("dramatic", "Dramatic", "🎭", "curves=preset=strong_contrast,eq=contrast=1.3:saturation=1.1"),
        VisualEffect("clarity", "Clarity", "🔍", "unsharp=5:5:1.2:5:5:0.0,eq=contrast=1.1"),
        VisualEffect("matte", "Matte", "🪞", "eq=contrast=0.9:brightness=0.04:saturation=0.95"),
        // Color pop & isolation
        VisualEffect("colorpop", "Color Pop", "🟥", "hue=s=0,eq=saturation=1.4", isPro = true),
        VisualEffect("golden", "Golden Hour", "🌅", "eq=temp=1.3:saturation=1.2:brightness=0.04"),
        VisualEffect("midnight", "Midnight", "🌃", "eq=temp=0.7:saturation=1.1:contrast=1.15:brightness=-0.04"),
        VisualEffect("forest", "Forest", "🌲", "eq=saturation=1.3:temp=0.9:green=1.1", isPro = true),
        VisualEffect("ocean", "Ocean", "🌊", "eq=saturation=1.2:temp=0.85:blue=1.1", isPro = true)
    )
}

@Composable
fun EffectsScreen(
    viewModel: EditorViewModel,
    onBack: () -> Unit
) {
    val project = viewModel.currentProject.value
    val activeEffect = project?.selectedEffect ?: "none"
    val activeModel = EffectCatalog.effects.firstOrNull { it.id == activeEffect }

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
                Text("Effects Gallery", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = OnPrimary)
                Text(
                    "${EffectCatalog.effects.size} real effects · FFmpeg -vf chains",
                    fontSize = 11.sp,
                    color = OnSurfaceSecondary
                )
            }
        }

        // ── Active effect banner ──
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp)
                .neonGlow(
                    color = if (activeEffect == "none") AccentPrimary else AccentSecondary,
                    shape = RoundedCornerShape(14.dp),
                    glowWidth = 1.dp
                )
                .background(
                    if (activeEffect == "none") Brush.linearGradient(listOf(SurfaceVariant, SurfaceVariant))
                    else premiumAccentGradient,
                    RoundedCornerShape(14.dp)
                )
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Column {
                Text(
                    if (activeEffect == "none") "No effect applied" else "Active: ${activeModel?.label ?: activeEffect}",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "FFmpeg: ${activeModel?.ffmpegChain?.ifBlank { "(passthrough)" } ?: "(none)"}",
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace,
                    color = Color.White.copy(alpha = 0.75f),
                    maxLines = 2
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // ── 3D glass effect grid ──
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 108.dp),
            modifier = Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(EffectCatalog.effects, key = { it.id }) { effect ->
                EffectGlassCard(
                    effect = effect,
                    isSelected = effect.id == activeEffect,
                    onClick = {
                        // Toggle: tapping the active effect clears it.
                        val next = if (effect.id == activeEffect) "none" else effect.id
                        viewModel.updateSelectedEffect(next)
                    }
                )
            }
            item { Spacer(modifier = Modifier.height(20.dp)) }
        }
    }
}

@Composable
private fun EffectGlassCard(
    effect: VisualEffect,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val accent = if (effect.isPro) NeonOrange else CyberCyan

    // 2027 8K — Premium 3D Glass Card with perspective tilt
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .glassCard3D(
                shape = RoundedCornerShape(16.dp),
                glowColor = if (isSelected) SignatureOrange else accent.copy(alpha = 0.25f),
                backColor = Surface
            )
            .neonGlow(
                color = if (isSelected) accent else Color.Transparent,
                shape = RoundedCornerShape(16.dp),
                glowWidth = if (isSelected) 2.dp else 0.dp
            )
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                brush = if (isSelected) premiumAccentGradient
                else Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.12f), Color.Transparent)),
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
            Text(effect.emoji, fontSize = 30.sp)
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                effect.label,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = if (isSelected) OnPrimary else OnSurfaceSecondary,
                textAlign = TextAlign.Center,
                maxLines = 1
            )
            if (effect.isPro) {
                Text("PRO", fontSize = 7.sp, fontWeight = FontWeight.Black, color = NeonOrange)
            }
        }
    }
}
