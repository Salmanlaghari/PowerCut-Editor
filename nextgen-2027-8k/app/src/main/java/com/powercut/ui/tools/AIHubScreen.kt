package com.powercut.ui.tools

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Text
import com.powercut.model.DAGNode
import com.powercut.ui.components.DemoThumbnail
import com.powercut.ui.components.ProBadge
import com.powercut.ui.components.baseScene
import com.powercut.ui.components.powercutGradientBrush
import com.powercut.ui.editor.EditorViewModel
import com.powercut.ui.theme.*

/**
 * AI Hub — 6 generative / assistive AI tools, each wired into the effect DAG
 * as a [DAGNode.Kind.AI] node. Every card carries a genuine miniature Canvas
 * render (no fake solid colours) plus a PRO badge for premium models.
 */
@Composable
fun AIHubScreen(
    onClose: () -> Unit,
    vm: EditorViewModel
) {
    var selectedId by remember { mutableStateOf<String?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Bg)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp)
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    "AI Hub",
                    color = TextPrimary,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.weight(1f))
                GradientPillCompact(
                    text = "Close",
                    onClick = onClose
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(
                "Generative & assistive models · tap to apply",
                color = TextSecondary,
                fontSize = 13.sp
            )
            Spacer(Modifier.height(20.dp))

            // 2-column grid of AI tools
            val rows = AI_DEFS.chunked(2)
            rows.forEach { rowItems ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    rowItems.forEach { ai ->
                        AICard(
                            ai = ai,
                            selected = selectedId == ai.id,
                            modifier = Modifier.weight(1f),
                            onTap = {
                                selectedId = ai.id
                                vm.addDagNode(
                                    DAGNode.Kind.AI,
                                    """{"ai":"${ai.id}"}"""
                                )
                            }
                        )
                    }
                    // pad odd row so layout stays balanced
                    if (rowItems.size < 2) Spacer(Modifier.weight(1f))
                }
                Spacer(Modifier.height(12.dp))
            }
            Spacer(Modifier.height(80.dp))
        }
    }
}

@Composable
private fun AICard(
    ai: AIDef,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onTap: () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(BgCard)
            .then(
                if (selected) Modifier.border(2.dp, powercutGradientBrush(), RoundedCornerShape(18.dp))
                else Modifier.border(1.dp, GlassStroke, RoundedCornerShape(18.dp))
            )
            .pointerInput(ai.id) { detectTapGestures(onTap = { onTap() }) }
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            DemoThumbnail(
                renderDemo = { aiRender(ai.id) },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(10.dp))
            Text(ai.name, color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            Text(ai.desc, color = TextSecondary, fontSize = 12.sp)
        }
        // every AI tool is premium
        ProBadge(modifier = Modifier.align(Alignment.TopEnd).padding(6.dp))
    }
}

/** Genuine miniature renders — each AI tool gets a distinct visual signature. */
private fun DrawScope.aiRender(id: String) {
    baseScene()
    when (id) {
        "auto_caption" -> {
            // caption bar at bottom
            drawRect(
                color = Color(0xCC000000),
                topLeft = Offset(0f, size.height * 0.78f),
                size = Size(size.width, size.height * 0.22f)
            )
            // little text lines
            drawRect(Color.White, Offset(size.width * 0.08f, size.height * 0.84f),
                Size(size.width * 0.55f, size.height * 0.03f))
            drawRect(Color.White.copy(alpha = 0.7f), Offset(size.width * 0.08f, size.height * 0.90f),
                Size(size.width * 0.35f, size.height * 0.03f))
        }
        "bg_remove" -> {
            // checkerboard transparency strip on the subject area
            val cell = size.width / 12f
            var x = 0f
            var y = size.height * 0.35f
            var dark = true
            while (x < size.width) {
                drawRect(
                    if (dark) Color(0xFF3A3A4A) else Color(0xFF55556A),
                    Offset(x, y), Size(cell, size.height * 0.45f)
                )
                dark = !dark
                x += cell
            }
        }
        "super_res" -> {
            // pixelated upscale hint — 8x8 block grid overlay
            val bw = size.width / 8f
            val bh = size.height / 8f
            for (i in 0 until 8) for (j in 0 until 8) {
                drawRect(
                    Color(0x22FFFFFF),
                    Offset(i * bw, j * bh), Size(bw, bh)
                )
            }
        }
        "denoise" -> {
            // smooth gradient overlay representing noise removal
            drawRect(
                Brush.linearGradient(listOf(Color(0x33000000), Color(0x339D4EDD))),
                Offset.Zero, size
            )
        }
        "style_transfer" -> {
            // painterly strokes — diagonal colour bands
            drawRect(Brush.linearGradient(
                listOf(Color(0x66FF5A3C), Color(0x669D4EDD), Color(0x6600C2FF))),
                Offset.Zero, size)
        }
        "smart_crop" -> {
            // rule-of-thirds golden ratio frame
            drawRect(Color(0xFFFFC23C), Offset(size.width * 0.33f, 0f),
                Size(size.width * 0.008f, size.height))
            drawRect(Color(0xFFFFC23C), Offset(size.width * 0.66f, 0f),
                Size(size.width * 0.008f, size.height))
            drawRect(Color(0xFFFFC23C), Offset(0f, size.height * 0.33f),
                Size(size.width, size.height * 0.008f))
            drawRect(Color(0xFFFFC23C), Offset(0f, size.height * 0.66f),
                Size(size.width, size.height * 0.008f))
        }
    }
}

private data class AIDef(val id: String, val name: String, val desc: String)
private val AI_DEFS = listOf(
    AIDef("auto_caption", "Auto Captions", "Speech-to-text subtitle gen"),
    AIDef("bg_remove", "BG Remove", "Subject-aware matte extraction"),
    AIDef("super_res", "Super Resolution", "8K upscaling neural net"),
    AIDef("denoise", "AI Denoise", "Temporal noise reduction"),
    AIDef("style_transfer", "Style Transfer", "Painting / anime re-styling"),
    AIDef("smart_crop", "Smart Crop", "Auto reframing + tracking")
)
