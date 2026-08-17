package com.powercut.editor.domain.processing

/**
 * ════════════════════════════════════════════════════════════════════════════
 *  TEXT ANIMATION CATALOG — single source of truth for the editor's Animations
 * ════════════════════════════════════════════════════════════════════════════
 *
 * PART 3 of the PowerCut effect work (Animations — instant tap-to-preview).
 *
 * WHY THIS FILE EXISTS
 * --------------------
 * The editor's Animations panel offers text animations (fade, bounce, zoom,
 * neon_pulse, …). Before this catalog existed, the animation expressions were
 * inlined in `VideoProcessor.buildTextOverlay` — which is a private Android
 * class, so the live preview (a Compose animation) and the export (FFmpeg
 * drawtext) were TWO disconnected implementations that could silently diverge,
 * exactly like the transition gap PART 2 closed.
 *
 * This catalog is the single place that maps every animation id the UI offers
 * to a REAL FFmpeg `drawtext` chain, and BOTH sides consume it:
 *
 *   • Export  — `VideoProcessor.buildTextOverlay` delegates here 1:1.
 *   • Preview — `TextAnimationPreviewRenderer.buildPreviewFilter` delegates
 *               here 1:1, then FFmpeg renders the exact same chain into a
 *               short MP4 the editor plays back the moment an animation is
 *               tapped (same pattern as `TransitionPreviewRenderer`).
 *
 * Because both callers pass through the same pure function with the same
 * arguments, the animation burned into the exported file can never diverge
 * from what the user saw on the live preview. `TextAnimationPreviewExportParityTest`
 * pins this.
 *
 * FFMPEG 4.4 PER-FRAME FLOOR (empirically verified against FFmpeg 4.4.2)
 * ---------------------------------------------------------------------
 * The task forbids the old broken pattern of putting per-frame time
 * expressions on filter options that are only evaluated ONCE at graph init —
 * that silently shows a static frame on export or hard-fails the graph.
 * Verified on the bundled floor version (FFmpeg 4.4, same as ffmpeg-kit-full
 * 8.1.2):
 *
 *   PER-FRAME (used here)              INIT-TIME ONLY (forbidden here)
 *   ---------------------------------  ----------------------------------
 *   drawtext x, y                      drawtext fontcolor_expr
 *   drawtext alpha                     drawtext fontcolor@'expr' (HARD FAIL:
 *   drawtext fontsize                  "Invalid alpha value specifier")
 *   drawtext text %{eif\:…} expansion  drawbox / boxblur / gblur exprs
 *
 * So every animation below is expressed ONLY through `x`, `y`, `alpha`,
 * `fontsize` and text-expansion. The colour-based animations (neon_pulse,
 * glow, fire, color_cycle, rainbow, …) use a STATIC `fontcolor` plus a
 * per-frame `alpha` pulse; `color_cycle` and `rainbow` additionally layer two
 * / three stacked drawtext instances whose per-frame alphas are phase-shifted
 * sines, which yields a real, continuous per-frame colour crossfade without
 * any init-time-only option.
 */
object TextAnimationCatalog {

    /** Animation id meaning "no animation" (static text overlay). */
    const val NONE = "none"

    /** Demo text used by the live preview when no text has been typed yet. */
    const val DEFAULT_DEMO_TEXT = "PowerCut"

    /**
     * Every animation id the editor's Animations panel offers, in UI order.
     * The panel iterates this exact list, so a UI animation can never exist
     * without a real drawtext implementation below (and vice versa).
     */
    val UI_IDS: List<String> = listOf(
        "none", "fade", "fade_out", "fade_in_out", "typewriter", "typewriter_fast",
        "bounce", "slide_left", "slide_right", "slide_up", "slide_down",
        "slide_in_3d", "zoom_in", "zoom_out", "rotate", "wave", "glitch_in",
        "neon_pulse", "neon_flicker", "pop", "flip", "elastic", "spring",
        "rubber", "swing", "shake", "blink", "pulse", "color_cycle",
        "explode_in", "implode", "marquee", "scroll_up", "scroll_down",
        "glow", "rainbow", "frozen", "fire", "metallic", "gold"
    )

    /**
     * Ids that are deliberate STATIC looks (a colour change, no motion).
     * Everything else must animate per-frame — the validation harness and the
     * unit tests assert exactly that, so a "static" animation can't sneak in.
     */
    val STATIC_IDS: Set<String> = setOf("frozen", "metallic", "gold")

    /** Normalises a UI/project animation id to a catalog key. */
    fun normalize(raw: String?): String =
        (raw ?: NONE).trim().lowercase().replace(" ", "_").replace("-", "_")

    /** True when [raw] means "no animation". */
    fun isNone(raw: String?): Boolean {
        val n = normalize(raw)
        return n == NONE || n.isEmpty() || n == "no_animation" || n == "static"
    }

    /** True when the id is offered by the UI list (or "none"). */
    fun isKnown(raw: String?): Boolean = normalize(raw) in UI_IDS

    /** True when the id is a real per-frame animation (not static/none). */
    fun isAnimated(raw: String?): Boolean {
        val n = normalize(raw)
        return n != NONE && n !in STATIC_IDS
    }

    /**
     * Builds the FULL FFmpeg `drawtext` filter chain for [text] with [animation].
     *
     * This is the single function shared by the export pipeline and the live
     * preview renderer. Returns a comma-separated chain of one or more
     * drawtext filters (colour crossfades stack 2–3 layers). Only per-frame
     * mechanisms are used (see class doc).
     *
     * @param fontFileClause the `:fontfile=<path>` clause to append, or "".
     *                       Kept as a parameter so this function stays a pure
     *                       JVM testable unit (no Android context).
     */
    fun buildDrawtextFilters(
        text: String,
        animation: String,
        duration: Double,
        posX: Float = 0.5f,
        posY: Float = 0.85f,
        colorHex: String = "#FFFFFF",
        fontSize: Float = 42f,
        textBold: Boolean = false,
        textItalic: Boolean = false,
        textShadow: Boolean = false,
        textOutline: Boolean = false,
        textGlow: Boolean = false,
        textNeon: Boolean = false,
        textBgColor: String = "#00000000",
        textBgOpacity: Float = 0.5f,
        fontFileClause: String = ""
    ): String {
        val safeText = text.replace("'", "\\'").replace(":", "\\:")
        val anim = normalize(animation)
        // Convert hex color (#RRGGBB) to FFmpeg format (0xRRGGBB).
        val fcHex = colorHex.removePrefix("#").let { h ->
            when (h.length) {
                6 -> h
                3 -> "${h[0]}${h[0]}${h[1]}${h[1]}${h[2]}${h[2]}"
                else -> "FFFFFF"
            }
        }
        val fontColor = "0x$fcHex"
        val xExpr = "w*${String.format(java.util.Locale.US, "%.3f", posX)}-text_w/2"
        val yExpr = "h*${String.format(java.util.Locale.US, "%.3f", posY)}-text_h/2"
        val fs = fontSize.toInt().coerceIn(8, 200)
        val d = fmt(duration)
        // The fade-out window ends 1s before the clip ends.
        val fadeEnd = fmt((duration - 1.0).coerceAtLeast(0.0))

        val shadowFlag = if (textShadow) ":shadowx=3:shadowy=3:shadowcolor=black@0.8" else ""
        val outlineFlag = if (textOutline) ":borderw=3:bordercolor=black" else ""
        val boldFlag = if (textBold) ":bold=1" else ""
        val italicFlag = if (textItalic) ":italic=1" else ""
        // Glow / neon are pulsing-brightness looks. FFmpeg 4.4 drawtext has NO
        // per-frame colour mechanism (fontcolor_expr is init-time only;
        // fontcolor=@'expr' HARD-FAILS graph init), so both are expressed with
        // the verified per-frame `alpha` option.
        val glowFlag = if (textGlow) ":alpha='0.7+0.3*sin(t*4)'" else ""
        val neonFlag = if (textNeon) ":alpha='0.5+0.5*sin(t*6)'" else ""
        // Background box.
        val bgBox = if (textBgColor != "#00000000") {
            val bgHex = textBgColor.removePrefix("#")
            val bgArgb = if (bgHex.length == 8) bgHex else "FF$bgHex"
            val bgR = bgArgb.substring(2, 4); val bgG = bgArgb.substring(4, 6); val bgB = bgArgb.substring(6, 8)
            ":box=1:boxcolor=0x${bgR}${bgG}${bgB}@${fmt(textBgOpacity.toDouble())}"
        } else ":box=1:boxcolor=black@0.5"

        val base =
            "drawtext=text='$safeText':fontsize=$fs:fontcolor=$fontColor$bgBox" +
                ":x=($xExpr):y=($yExpr)$fontFileClause" +
                "$shadowFlag$outlineFlag$boldFlag$italicFlag$glowFlag$neonFlag"

        // A boxless extra layer used by the colour-crossfade animations: the
        // text is drawn again in another colour; its per-frame alpha is
        // phase-shifted so the two/three layers continuously cross-fade.
        fun extraLayer(color: String, alphaExpr: String): String =
            "drawtext=text='$safeText':fontsize=$fs:fontcolor=$color" +
                ":x=($xExpr):y=($yExpr)$fontFileClause:alpha='$alphaExpr'"

        return when (anim) {
            "none", "fade_in", "fade" -> "$base:alpha='if(lt(t,1)\\,t\\,1)'"
            "fade_out" -> "$base:alpha='if(gt(t,$fadeEnd)\\,$fadeEnd-t\\,1)'"
            "fade_in_out" -> "$base:alpha='if(lt(t,1)\\,t\\,if(gt(t,$fadeEnd)\\,$fadeEnd-t\\,1))'"
            "typewriter" -> "$base:alpha='1':text='$safeText%{eif\\:trunc(t*8)\\:d}'"
            "typewriter_fast" -> "$base:alpha='1':text='$safeText%{eif\\:trunc(t*16)\\:d}'"
            "bounce" -> "$base:y='($yExpr)+20*abs(sin(t*4))'"
            "slide_left" -> "$base:x='w-text_w-(w-text_w)*min(1\\,t/0.5)':y=h-100"
            "slide_right" -> "$base:x='(w-text_w)*min(1\\,t/0.5)':y=h-100"
            "slide_up" -> "$base:x=(w-text_w)/2:y='h-(h-100)*min(1\\,t/0.5)'"
            "slide_down" -> "$base:x=(w-text_w)/2:y='(h-100)*min(1\\,t/0.5)'"
            "slide_in_3d" -> "$base:x='(w-text_w)/2+100*exp(-t*3)':y=h-100:alpha='min(1\\,t*3)'"
            "zoom_in" -> "$base:fontsize='$fs*min(1\\,t/0.5)'"
            "zoom_out" -> "$base:fontsize='$fs*max(0.1\\,1-t/$d)'"
            "rotate" -> "$base:x='(w-text_w)/2+10*sin(t*2)':y=h-100"
            "wave" -> "$base:x='(w-text_w)/2+20*sin(t*3)':y='h-100+10*cos(t*3)'"
            "glitch_in" -> "$base:x='(w-text_w)/2+5*sin(t*30)':y='h-100+3*cos(t*30)':alpha='min(1\\,t*2)'"
            "neon_pulse" -> "$base:fontcolor=0x7C5CFF:alpha='0.7+0.3*sin(t*6)'"
            "neon_flicker" -> "$base:fontcolor=0x00ffff:alpha='0.5+0.5*sin(t*15)'"
            "pop" -> "$base:fontsize='$fs*(1+0.3*exp(-t*4))'"
            "flip" -> "$base:x=(w-text_w)/2:y=h-100:alpha='min(1\\,t*2)'"
            "elastic" -> "$base:y='($yExpr)+30*exp(-t*2)*sin(t*10)'"
            "spring" -> "$base:y='($yExpr)+20*exp(-t*3)*cos(t*8)'"
            "rubber" -> "$base:y='($yExpr)+15*exp(-t*2)*sin(t*6)'"
            "swing" -> "$base:x='(w-text_w)/2+30*sin(t*2)':y=h-100"
            "shake" -> "$base:x='(w-text_w)/2+5*sin(t*20)':y='h-100+3*cos(t*20)'"
            "blink" -> "$base:alpha='0.5+0.5*sin(t*8)'"
            "pulse" -> "$base:fontsize='$fs*(1+0.1*sin(t*5))'"
            // Colour crossfade: red ⇄ blue, per-frame alphas sum to 1.
            "color_cycle" -> "$base:fontcolor=0xFF3B30:alpha='0.5+0.5*sin(t*2)'," +
                extraLayer("0x007AFF", "0.5+0.5*sin(t*2+3.14159)")
            // Rainbow: red ⇄ green ⇄ blue, phase-shifted thirds, total alpha 1.
            "rainbow" -> "$base:fontcolor=0xFF3B30:alpha='0.333+0.333*sin(t*2)'," +
                extraLayer("0x00C853", "0.333+0.333*sin(t*2+2.09440)") + "," +
                extraLayer("0x2979FF", "0.333+0.333*sin(t*2+4.18879)")
            "explode_in" -> "$base:fontsize='$fs*2*exp(-t*3)+$fs'"
            "implode" -> "$base:fontsize='$fs+100*exp(-t*4)'"
            "marquee" -> "$base:x='w-w*t':y=h-100"
            "scroll_up" -> "$base:x=(w-text_w)/2:y='h-t*200':alpha='1'"
            "scroll_down" -> "$base:x=(w-text_w)/2:y='-text_h+t*200':alpha='1'"
            "glow" -> "$base:fontcolor=0xffff00:alpha='0.7+0.3*sin(t*4)'"
            "frozen" -> "$base:fontcolor=0x88ccff"
            "fire" -> "$base:fontcolor=0xff6600:alpha='0.7+0.3*sin(t*6)'"
            "metallic" -> "$base:fontcolor=0xc0c0c0"
            "gold" -> "$base:fontcolor=0xffd700"
            else -> base
        }
    }

    /** Locale-safe seconds formatter (FFmpeg rejects locale commas). */
    fun fmt(v: Double): String = String.format(java.util.Locale.US, "%.3f", v)
}
