package com.powercut.editor.domain.processing

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Phase 3 — Animations catalog coverage and per-frame mechanism safety.
 *
 * The editor's Animations panel iterates [TextAnimationCatalog.UI_IDS], so
 * every animation the UI offers MUST produce a real drawtext chain here — a
 * UI animation can never exist without a backend implementation, and vice
 * versa.
 *
 * The second half of this test pins the FFmpeg 4.4 floor rule the task
 * requires: per-frame animation may ONLY be expressed through drawtext
 * options that FFmpeg 4.4 actually evaluates per-frame (`x`, `y`, `alpha`,
 * `fontsize`, `text` expansion — empirically verified). The old broken
 * patterns (`fontcolor=0x…@'expr'` hard-fails graph init; `fontcolor_expr`
 * is init-time-only; `eval=frame` / drawbox / boxblur / gblur expressions
 * are init-time or hard errors) are forbidden and asserted absent.
 */
class TextAnimationCatalogTest {

    private fun chain(id: String, duration: Double = 2.0): String =
        TextAnimationCatalog.buildDrawtextFilters(
            text = "PowerCut", animation = id, duration = duration, fontSize = 42f
        )

    @Test
    fun everyUiAnimationProducesARealDrawtextChain() {
        // "none" is the explicit no-animation entry: a static drawtext.
        for (id in TextAnimationCatalog.UI_IDS) {
            val c = chain(id)
            assertTrue("'$id' must produce a drawtext filter", c.startsWith("drawtext="))
            assertTrue("'$id' chain must contain the text", c.contains("PowerCut"))
            assertTrue("'$id' chain must carry a fontsize", c.contains("fontsize=42"))
        }
    }

    @Test
    fun everyUiAnimationIdIsKnownToTheCatalog() {
        for (id in TextAnimationCatalog.UI_IDS) {
            assertTrue("'$id' must be known", TextAnimationCatalog.isKnown(id))
        }
    }

    @Test
    fun perFrameMechanismsOnly_noInitTimeOnlyTimeExpressions() {
        for (id in TextAnimationCatalog.UI_IDS) {
            val c = chain(id)
            // fontcolor=0xRRGGBB@'expr' hard-fails FFmpeg 4.4 graph init
            // ("Invalid alpha value specifier"). This was the old broken way.
            assertFalse(
                "'$id' must not use a fontcolor alpha expression: $c",
                Regex("fontcolor=0x[0-9A-Fa-f]+@'").containsMatchIn(c)
            )
            // fontcolor_expr exists in FFmpeg 4.4's option list but is only
            // evaluated at init — a static frame on export. Forbidden.
            assertFalse("'$id' must not use fontcolor_expr", c.contains("fontcolor_expr"))
            // eval=frame / eval=init flags on non-drawtext filters are the old
            // broken way of forcing per-frame evaluation where it is not
            // supported.
            assertFalse("'$id' must not use eval=frame", c.contains("eval=frame"))
            assertFalse("'$id' must not use drawbox/boxblur/gblur time exprs",
                Regex("(drawbox|boxblur|gblur|scale=.*'[^']*t[^']*')").containsMatchIn(c))
        }
    }

    @Test
    fun animatedIdsActuallyUseTime() {
        for (id in TextAnimationCatalog.UI_IDS) {
            if (TextAnimationCatalog.isNone(id) || id in TextAnimationCatalog.STATIC_IDS) continue
            val c = chain(id)
            assertTrue(
                "'$id' is animated but its chain has no time expression: $c",
                Regex("\\bt\\b|sin\\(t|cos\\(t|exp\\(-t|%\\{eif").containsMatchIn(c)
            )
        }
    }

    @Test
    fun staticLooksAreStatic() {
        for (id in TextAnimationCatalog.STATIC_IDS) {
            val c = chain(id)
            assertFalse("'$id' is a static look but animates: $c", c.contains("sin(t"))
            assertFalse("'$id' is a static look but animates: $c", c.contains("exp(-t"))
        }
    }

    @Test
    fun typewriterUsesTextExpansion() {
        val c = chain("typewriter")
        assertTrue("typewriter must reveal text via %{eif}", c.contains("%{eif"))
        val fast = chain("typewriter_fast")
        assertTrue("typewriter_fast must reveal text faster", fast.contains("trunc(t*16)"))
    }

    @Test
    fun colorCrossfadesLayerMultipleDrawtexts() {
        val cycle = chain("color_cycle")
        assertEquals("color_cycle must stack red + blue layers", 2, cycle.split(",").size)
        val rainbow = chain("rainbow")
        assertEquals("rainbow must stack red + green + blue layers", 3, rainbow.split(",").size)
        // The extra layers are boxless so the black background box is not
        // stacked; their per-frame alphas phase-shift to cross-fade colours.
        assertTrue("color_cycle layer 2 must be a drawtext", cycle.contains(",drawtext="))
        assertTrue("rainbow must contain two extra drawtext layers", rainbow.split("drawtext=").size == 4)
    }

    @Test
    fun normalizeAndNoneHandling() {
        assertEquals("fade", TextAnimationCatalog.normalize("Fade"))
        assertEquals("fade", TextAnimationCatalog.normalize("fade"))
        assertEquals("fade", TextAnimationCatalog.normalize(" fade "))
        assertEquals("slide_left", TextAnimationCatalog.normalize("slide left"))
        assertTrue(TextAnimationCatalog.isNone(null))
        assertTrue(TextAnimationCatalog.isNone("none"))
        assertTrue(TextAnimationCatalog.isNone(""))
        assertFalse(TextAnimationCatalog.isNone("bounce"))
        assertTrue(TextAnimationCatalog.isAnimated("bounce"))
        assertFalse(TextAnimationCatalog.isAnimated("frozen"))
        assertFalse(TextAnimationCatalog.isAnimated("none"))
        assertNotNull(TextAnimationCatalog.normalize("zoom-in"))
    }

    @Test
    fun unknownIdsFallBackToStaticText() {
        val c = chain("no_such_animation_xyz")
        assertTrue("unknown ids must degrade to a static drawtext", c.startsWith("drawtext="))
        assertFalse("unknown ids must not animate", c.contains("sin(t"))
    }
}
