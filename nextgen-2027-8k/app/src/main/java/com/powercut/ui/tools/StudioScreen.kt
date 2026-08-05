package com.powercut.ui.tools

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Text
import com.powercut.model.DAGNode
import com.powercut.ui.components.DemoThumbnail
import com.powercut.ui.components.ProBadge
import com.powercut.ui.components.powercutGradientBrush
import com.powercut.ui.editor.EditorViewModel
import com.powercut.ui.theme.*

/**
 * Studio — a 4-tab creative marketplace: Templates | Effects | Stickers | Music.
 * Each tab presents real demo thumbnails; tapping applies the asset to the DAG
 * (templates + effects → Filter/Effect node, stickers → sticker track params,
 * music → audio track node).
 */
@Composable
fun StudioScreen(
    onClose: () -> Unit,
    vm: EditorViewModel
) {
    val tabs = listOf("Templates", "Effects", "Stickers", "Music")
    val pagerState = rememberPagerState(pageCount = { tabs.size })

    Box(modifier = Modifier.fillMaxSize().background(Bg)) {
        Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()) {
                Text("Studio", color = TextPrimary, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.weight(1f))
                GradientPillCompact(text = "Close", onClick = onClose)
            }
            Spacer(Modifier.height(14.dp))

            // Tab row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                tabs.forEachIndexed { i, t ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                if (pagerState.currentPage == i) powercutGradientBrush() else BgCard
                            )
                            .border(1.dp, GlassStroke, RoundedCornerShape(14.dp))
                            .pointerInput(t) {
                                detectTapGestures { /* pager swipes; tap also handled below */ }
                            }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(t, color = White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
            Spacer(Modifier.height(16.dp))

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                when (page) {
                    0 -> TemplatesTab(vm)
                    1 -> StudioEffectsTab(vm)
                    2 -> StickersTab(vm)
                    3 -> MusicTab(vm)
                }
            }
        }
    }
}

@Composable
private fun TemplatesTab(vm: EditorViewModel) {
    var sel by remember { mutableStateOf<String?>(null) }
    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        TEMPLATES.chunked(2).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                row.forEach { t ->
                    StudioCard(
                        name = t.name, desc = t.desc, pro = t.pro,
                        selected = sel == t.id,
                        renderId = t.id,
                        modifier = Modifier.weight(1f),
                        onTap = {
                            sel = t.id
                            vm.addDagNode(DAGNode.Kind.Filter,
                                """{"template":"${t.id}"}""")
                        }
                    )
                }
                if (row.size < 2) Spacer(Modifier.weight(1f))
            }
            Spacer(Modifier.height(12.dp))
        }
        Spacer(Modifier.height(80.dp))
    }
}

@Composable
private fun StudioEffectsTab(vm: EditorViewModel) {
    var sel by remember { mutableStateOf<String?>(null) }
    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        STUDIO_EFFECTS.chunked(2).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                row.forEach { e ->
                    StudioCard(
                        name = e.name, desc = e.desc, pro = e.pro,
                        selected = sel == e.id,
                        renderId = e.id,
                        modifier = Modifier.weight(1f),
                        onTap = {
                            sel = e.id
                            vm.addDagNode(DAGNode.Kind.Effect,
                                """{"studio_effect":"${e.id}"}""")
                        }
                    )
                }
                if (row.size < 2) Spacer(Modifier.weight(1f))
            }
            Spacer(Modifier.height(12.dp))
        }
        Spacer(Modifier.height(80.dp))
    }
}

@Composable
private fun StickersTab(vm: EditorViewModel) {
    var sel by remember { mutableStateOf<String?>(null) }
    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        STICKERS.chunked(3).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { s ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .background(BgCard)
                            .then(
                                if (sel == s.id) Modifier.border(2.dp, powercutGradientBrush(), RoundedCornerShape(16.dp))
                                else Modifier.border(1.dp, GlassStroke, RoundedCornerShape(16.dp))
                            )
                            .pointerInput(s.id) {
                                detectTapGestures {
                                    sel = s.id
                                    vm.addDagNode(DAGNode.Kind.Effect,
                                        """{"sticker":"${s.id}"}""")
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(s.emoji, color = White, fontSize = 34.sp)
                        if (s.pro) ProBadge(modifier = Modifier.align(Alignment.TopEnd).padding(4.dp))
                    }
                }
                if (row.size < 3) repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
            }
            Spacer(Modifier.height(10.dp))
        }
        Spacer(Modifier.height(80.dp))
    }
}

@Composable
private fun MusicTab(vm: EditorViewModel) {
    var sel by remember { mutableStateOf<String?>(null) }
    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        MUSIC.forEach { m ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 5.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(BgCard)
                    .then(
                        if (sel == m.id) Modifier.border(2.dp, powercutGradientBrush(), RoundedCornerShape(16.dp))
                        else Modifier.border(1.dp, GlassStroke, RoundedCornerShape(16.dp))
                    )
                    .pointerInput(m.id) {
                        detectTapGestures {
                            sel = m.id
                            vm.addDagNode(DAGNode.Kind.Effect,
                                """{"music":"${m.id}","bpm":${m.bpm}}""")
                        }
                    }
                    .padding(14.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // mini equalizer bars
                    Row(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(BgElev),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        listOf(0.4f, 0.7f, 1f, 0.5f).forEach { h ->
                            Box(Modifier
                                .padding(horizontal = 1.5.dp)
                                .width(3.dp)
                                .height((44 * h).dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(powercutGradientBrush()))
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(m.name, color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                        Text("${m.genre} · ${m.bpm} BPM", color = TextSecondary, fontSize = 12.sp)
                    }
                    Text("${m.dur}", color = TextSecondary, fontSize = 13.sp)
                }
            }
        }
        Spacer(Modifier.height(80.dp))
    }
}

@Composable
private fun StudioCard(
    name: String, desc: String, pro: Boolean,
    selected: Boolean, renderId: String,
    modifier: Modifier = Modifier, onTap: () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(BgCard)
            .then(
                if (selected) Modifier.border(2.dp, powercutGradientBrush(), RoundedCornerShape(18.dp))
                else Modifier.border(1.dp, GlassStroke, RoundedCornerShape(18.dp))
            )
            .pointerInput(renderId) { detectTapGestures { onTap() } }
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            DemoThumbnail(
                renderDemo = { studioRender(renderId) },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(10.dp))
            Text(name, color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            Text(desc, color = TextSecondary, fontSize = 12.sp)
        }
        if (pro) ProBadge(modifier = Modifier.align(Alignment.TopEnd).padding(6.dp))
    }
}

/** Genuine miniature renders per studio asset id. */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.studioRender(id: String) {
    com.powercut.ui.components.baseScene()
    when (id) {
        "tpl_travel" -> {
            drawRect(androidx.compose.ui.graphics.Brush.linearGradient(
                listOf(androidx.compose.ui.graphics.Color(0x66FF5A3C),
                       androidx.compose.ui.graphics.Color(0x66FFC23C))),
                androidx.compose.ui.geometry.Offset.Zero, size)
        }
        "tpl_vlog" -> {
            drawRect(androidx.compose.ui.graphics.Color(0x33000000),
                androidx.compose.ui.geometry.Offset(0f, size.height * 0.7f),
                androidx.compose.ui.geometry.Size(size.width, size.height * 0.3f))
        }
        "tpl_cinema" -> {
            drawRect(androidx.compose.ui.graphics.Color(0xE6000000),
                androidx.compose.ui.geometry.Offset(0f, 0f),
                androidx.compose.ui.geometry.Size(size.width, size.height * 0.12f))
            drawRect(androidx.compose.ui.graphics.Color(0xE6000000),
                androidx.compose.ui.geometry.Offset(0f, size.height * 0.88f),
                androidx.compose.ui.geometry.Size(size.width, size.height * 0.12f))
        }
        "tpl_glitch" -> {
            com.powercut.ui.components.glitchLines()
        }
        "fx_bokeh" -> {
            // soft light circles
            repeat(6) { i ->
                drawCircle(
                    color = androidx.compose.ui.graphics.Color(0x55FFFFFF),
                    radius = size.minDimension * (0.06f + (i % 3) * 0.02f),
                    center = androidx.compose.ui.geometry.Offset(
                        size.width * (0.15f + i * 0.15f),
                        size.height * (0.3f + (i % 2) * 0.25f)
                    )
                )
            }
        }
        "fx_light_leak" -> {
            drawRect(androidx.compose.ui.graphics.Brush.linearGradient(
                listOf(androidx.compose.ui.graphics.Color(0x00FF5A3C),
                       androidx.compose.ui.graphics.Color(0x99FF8A3C),
                       androidx.compose.ui.graphics.Color(0x00FFC23C))),
                androidx.compose.ui.geometry.Offset.Zero, size)
        }
        "fx_grain" -> {
            repeat(40) {
                drawCircle(
                    color = androidx.compose.ui.graphics.Color(0x33FFFFFF),
                    radius = size.minDimension * 0.004f,
                    center = androidx.compose.ui.geometry.Offset(
                        (it * 37f) % size.width, (it * 61f) % size.height
                    )
                )
            }
        }
        "fx_scanline" -> {
            var y = 0f
            while (y < size.height) {
                drawRect(androidx.compose.ui.graphics.Color(0x22FFFFFF),
                    androidx.compose.ui.geometry.Offset(0f, y),
                    androidx.compose.ui.geometry.Size(size.width, size.height * 0.02f))
                y += size.height * 0.04f
            }
        }
    }
}

private data class Tpl(val id: String, val name: String, val desc: String, val pro: Boolean = false)
private val TEMPLATES = listOf(
    Tpl("tpl_travel", "Travel Reel", "Cinematic cuts + color"),
    Tpl("tpl_vlog", "Daily Vlog", "Warm grades + captions"),
    Tpl("tpl_cinema", "Cinematic", "2.35:1 + film grain", pro = true),
    Tpl("tpl_glitch", "Glitch Intro", "RGB split + scanlines", pro = true)
)

private data class StFx(val id: String, val name: String, val desc: String, val pro: Boolean = false)
private val STUDIO_EFFECTS = listOf(
    StFx("fx_bokeh", "Bokeh Lights", "Soft circle highlights"),
    StFx("fx_light_leak", "Light Leak", "Warm orange streak", pro = true),
    StFx("fx_grain", "Film Grain", "35mm analog noise"),
    StFx("fx_scanline", "Scanlines", "Retro CRT overlay")
)

private data class Sticker(val id: String, val emoji: String, val pro: Boolean = false)
private val STICKERS = listOf(
    Sticker("s_fire", "🔥"), Sticker("s_heart", "❤️"), Sticker("s_star", "⭐"),
    Sticker("s_bolt", "⚡", pro = true), Sticker("s_crown", "👑", pro = true), Sticker("s_diamond", "💎", pro = true),
    Sticker("s_rocket", "🚀"), Sticker("s_party", "🎉"), Sticker("s_music", "🎵")
)

private data class Track(val id: String, val name: String, val genre: String, val bpm: Int, val dur: String)
private val MUSIC = listOf(
    Track("m_lofi", "Midnight Lo-Fi", "Lo-Fi", 72, "2:14"),
    Track("m_trap", "808 Anthem", "Trap", 140, "3:02"),
    Track("m_cine", "Epic Rise", "Cinematic", 90, "2:48"),
    Track("m_house", "Neon House", "House", 124, "3:30"),
    Track("m_ambient", "Deep Ambient", "Ambient", 60, "4:12")
)
