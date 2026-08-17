package com.powercut.editor.domain.processing

/**
 * ════════════════════════════════════════════════════════════════════════════
 *  TRANSITION CATALOG — single source of truth for REAL time-based transitions
 * ════════════════════════════════════════════════════════════════════════════
 *
 * PART 2 of the PowerCut transition work.
 *
 * WHY THIS FILE EXISTS
 * --------------------
 * Before this catalog existed, the editor had two disconnected transition
 * implementations:
 *
 *   1. `VideoProcessor.transitionChain()` — applied a *post-filter* to the
 *      ALREADY-CONCATENATED video. That means a "slide_left" transition faded
 *      the whole timeline at t=0 instead of sliding clip B over clip A at the
 *      cut point. It was a single-clip cosmetic effect masquerading as a
 *      transition. Many entries were also pure no-ops (`elastic`, `spring`,
 *      `hexagon`, `star` all collapsed to the same vignette/fade).
 *
 *   2. `VideoProcessor.buildXfadeTransition()` — a real xfade builder that was
 *      *never called from anywhere*, and which emitted the transition name
 *      `fdissolve`, which DOES NOT EXIST in any FFmpeg release. Any export that
 *      had reached it would have hard-failed with
 *      "Error applying options to the filter".
 *
 * This catalog replaces the guesswork with a validated mapping from every
 * transition id the UI can produce to a REAL FFmpeg `xfade` transition name,
 * so the transition happens BETWEEN clips at the cut point.
 *
 * FFMPEG VERSION FLOOR
 * --------------------
 * Every name in [BASE_XFADE_NAMES] is present in the `xfade` filter since
 * FFmpeg 4.3/4.4 (43 transitions, enum 0..42). The app bundles
 * ffmpeg-kit-full 8.1.2, which is a superset. We deliberately map ONLY onto
 * this 4.4-era floor set so the mapping is verifiable against any modern
 * FFmpeg and cannot break on an older/older-bundled build.
 *
 * Newer FFmpeg (7.1+) adds `zoomin`, `fadefast`, `fadeslow`, `hlwind`,
 * `hrwind`, `vuwind`, `vdwind`, `cover*` and `reveal*`. Those are listed in
 * [EXTENDED_XFADE_NAMES] and are NOT used by the mapping, because they would
 * make the graph fail on a 4.4 FFmpeg. See [XFADE_ONLY_IN_FFMPEG_71_PLUS].
 */
object TransitionCatalog {

    /** Transition id meaning "no transition / hard cut". */
    const val NONE = "none"

    /** Default transition duration in seconds when the caller has no preference. */
    const val DEFAULT_DURATION_SEC = 0.7

    /**
     * Minimum usable transition duration. Below this, xfade produces a
     * transition shorter than a single frame at low fps, which FFmpeg accepts
     * but which is visually a hard cut.
     */
    const val MIN_DURATION_SEC = 0.1

    /**
     * The 43 REAL `xfade` transition names available in FFmpeg 4.3/4.4 and
     * every later release (xfade enum 0..42, excluding `custom` which requires
     * an `expr`). Verified against a real ffmpeg binary by
     * `scripts/validate_transitions_ffmpeg.py`.
     */
    val BASE_XFADE_NAMES: Set<String> = setOf(
        "fade", "wipeleft", "wiperight", "wipeup", "wipedown",
        "slideleft", "slideright", "slideup", "slidedown",
        "circlecrop", "rectcrop", "distance", "fadeblack", "fadewhite",
        "radial", "smoothleft", "smoothright", "smoothup", "smoothdown",
        "circleopen", "circleclose", "vertopen", "vertclose",
        "horzopen", "horzclose", "dissolve", "pixelize",
        "diagtl", "diagtr", "diagbl", "diagbr",
        "hlslice", "hrslice", "vuslice", "vdslice",
        "hblur", "fadegrays", "wipetl", "wipetr", "wipebl", "wipebr",
        "squeezeh", "squeezev"
    )

    /**
     * `xfade` names that only exist in FFmpeg 7.1+ . The bundled
     * ffmpeg-kit-full 8.1.2 has them, but we do NOT emit them: the mapping must
     * stay valid on the 4.4-era floor so it can be validated in CI and cannot
     * regress on an older bundled build. Documented so a future change can opt
     * in deliberately rather than by accident.
     */
    val XFADE_ONLY_IN_FFMPEG_71_PLUS: Set<String> = setOf(
        "zoomin", "fadefast", "fadeslow",
        "hlwind", "hrwind", "vuwind", "vdwind",
        "coverleft", "coverright", "coverup", "coverdown",
        "revealleft", "revealright", "revealup", "revealdown"
    )

    /** All names a modern (7.1+) FFmpeg accepts. */
    val EXTENDED_XFADE_NAMES: Set<String> = BASE_XFADE_NAMES + XFADE_ONLY_IN_FFMPEG_71_PLUS

    /**
     * Describes how one editor transition id is realised with real FFmpeg.
     *
     * @param id           the transition id the UI/project stores.
     * @param xfade        the REAL `xfade` transition name used at the cut point.
     * @param approximated true when [xfade] is a deliberate visual approximation
     *                     of a effect FFmpeg has no native equivalent for. The
     *                     transition still really happens between the clips —
     *                     this flag only records that the look is an equivalent,
     *                     not an exact match.
     * @param note         human-readable rationale, surfaced in reports/tests.
     */
    data class Spec(
        val id: String,
        val xfade: String,
        val approximated: Boolean = false,
        val note: String = ""
    )

    /**
     * Every transition id the editor can produce, mapped to a REAL xfade name.
     *
     * Keys are normalised ids (lowercase, `_` separated). [specFor] normalises
     * incoming ids (spaces/dashes → underscore) before lookup, so "Slide Left",
     * "slide-left" and "slide_left" all resolve here.
     *
     * The rationale for each *approximated* mapping is recorded in `note` so
     * nothing silently pretends to be an exact effect.
     */
    private val SPECS: List<Spec> = listOf(
        // ── Direct, exact equivalents ─────────────────────────────────────
        Spec("fade", "fade"),
        Spec("crossfade", "fade"),
        Spec("fade_in_out", "fade"),
        Spec("dissolve", "dissolve"),
        Spec("fade_out", "fadeblack", note = "fade out through black into next clip"),
        Spec("black_fade", "fadeblack"),
        Spec("fade_black", "fadeblack"),
        Spec("white_fade", "fadewhite"),
        Spec("fade_white", "fadewhite"),
        Spec("pixelate", "pixelize"),
        Spec("pixel_in", "pixelize"),
        Spec("mosaic", "pixelize", note = "pixelize is FFmpeg's block/mosaic transition"),
        Spec("blur", "hblur"),
        Spec("blur_in", "hblur"),
        Spec("blur_out", "hblur"),
        Spec("wipe", "wiperight"),
        Spec("wipe_left", "wipeleft"),
        Spec("wipe_right", "wiperight"),
        Spec("wipe_up", "wipeup"),
        Spec("wipe_down", "wipedown"),
        Spec("slide_left", "slideleft"),
        Spec("slide_right", "slideright"),
        Spec("slide_up", "slideup"),
        Spec("slide_down", "slidedown"),
        Spec("push_left", "slideleft", note = "xfade slide* pushes the outgoing clip out of frame"),
        Spec("push_right", "slideright", note = "xfade slide* pushes the outgoing clip out of frame"),
        Spec("push_up", "slideup", note = "xfade slide* pushes the outgoing clip out of frame"),
        Spec("push_down", "slidedown", note = "xfade slide* pushes the outgoing clip out of frame"),
        Spec("pull", "slideright", note = "reverse-direction push"),
        Spec("circle", "circleopen"),
        Spec("iris_in", "circleopen"),
        Spec("iris_out", "circleclose"),
        Spec("circle_open", "circleopen"),
        Spec("circle_close", "circleclose"),
        Spec("split", "vertopen", note = "vertical split opening from the centre"),
        Spec("curtain", "horzopen", note = "horizontal curtain opening from the centre"),
        Spec("blinds", "hlslice", note = "sliced/venetian-blind style reveal"),
        Spec("checkerboard", "hrslice", note = "sliced reveal; FFmpeg has no checker transition"),
        Spec("diagonal", "diagtl"),
        Spec("triangle", "diagtr", approximated = true, note = "diagonal corner wipe stands in for a triangle wipe"),
        Spec("cross", "vertopen", approximated = true, note = "centre-out open approximates a cross reveal"),
        Spec("star_wipe", "circleopen", approximated = true, note = "FFmpeg has no star matte; radial open is the closest native reveal"),
        Spec("star", "circleopen", approximated = true, note = "FFmpeg has no star matte; radial open is the closest native reveal"),
        Spec("hexagon", "circlecrop", approximated = true, note = "FFmpeg has no polygon matte; circlecrop is the closest native shape reveal"),
        Spec("diamond", "rectcrop", approximated = true, note = "FFmpeg has no diamond matte; rectcrop is the closest native shape reveal"),
        Spec("heart", "circlecrop", approximated = true, note = "FFmpeg has no heart matte; circlecrop is the closest native shape reveal"),
        Spec("clock_wipe", "radial", note = "radial is FFmpeg's clock/sweep wipe"),
        Spec("spiral", "radial", approximated = true, note = "radial sweep approximates a spiral"),
        Spec("zoom_in", "smoothup", approximated = true, note = "zoomin xfade needs FFmpeg 7.1+; smoothup keeps a real inter-clip move on the 4.4 floor"),
        Spec("zoom_out", "smoothdown", approximated = true, note = "no native zoom-out xfade on the 4.4 floor; smoothdown keeps a real inter-clip move"),
        Spec("zoom_burst", "smoothup", approximated = true, note = "no native zoom xfade on the 4.4 floor"),
        Spec("warp", "squeezeh", approximated = true, note = "horizontal squeeze approximates a warp"),
        Spec("stretch", "squeezeh", approximated = true, note = "horizontal squeeze is FFmpeg's native stretch-style transition"),
        Spec("squeeze_h", "squeezeh"),
        Spec("squeeze_v", "squeezev"),
        Spec("spin", "radial", approximated = true, note = "FFmpeg xfade has no rotation transition; radial sweep is the closest native rotational motion"),
        Spec("rotate_in", "radial", approximated = true, note = "FFmpeg xfade has no rotation transition; radial sweep is the closest native rotational motion"),
        Spec("rotate_out", "radial", approximated = true, note = "FFmpeg xfade has no rotation transition; radial sweep is the closest native rotational motion"),
        Spec("rotate_3d", "squeezeh", approximated = true, note = "no 3D rotation in xfade; horizontal squeeze mimics a plane turning"),
        Spec("cube", "squeezeh", approximated = true, note = "no 3D cube in xfade; horizontal squeeze mimics a cube face rotating"),
        Spec("page_turn", "squeezeh", approximated = true, note = "no page-curl in xfade; horizontal squeeze mimics the page turning"),
        Spec("flip_h", "squeezeh", approximated = true, note = "horizontal squeeze mimics a horizontal flip"),
        Spec("flip_v", "squeezev", approximated = true, note = "vertical squeeze mimics a vertical flip"),
        Spec("flip", "squeezeh", approximated = true, note = "horizontal squeeze mimics a flip"),
        Spec("swing", "smoothright", approximated = true, note = "eased directional move approximates a swing"),
        Spec("whip_pan", "slideleft", approximated = true, note = "fast directional slide; speed comes from a short duration"),
        Spec("camera_move", "smoothleft", approximated = true, note = "eased directional move approximates a camera push"),
        Spec("smooth_cut", "fade"),
        Spec("l_cut", "fade", note = "audio-led cut; video side is a short crossfade"),
        Spec("j_cut", "fade", note = "audio-led cut; video side is a short crossfade"),
        Spec("bounce", "smoothup", approximated = true, note = "eased move; xfade has no elastic easing"),
        Spec("elastic", "smoothup", approximated = true, note = "eased move; xfade has no elastic easing"),
        Spec("spring", "smoothdown", approximated = true, note = "eased move; xfade has no spring easing"),
        Spec("ripple", "distance", approximated = true, note = "distance is FFmpeg's displacement-style transition"),
        Spec("wave", "distance", approximated = true, note = "distance is FFmpeg's displacement-style transition"),
        Spec("shatter", "pixelize", approximated = true, note = "no fragment/shatter matte in xfade; pixelize is the closest native break-up"),
        Spec("smoke", "hblur", approximated = true, note = "blur-through approximates a smoke dissolve"),
        Spec("light_leak", "fadewhite", approximated = true, note = "fade through white approximates a light leak"),
        Spec("film_burn", "fadewhite", approximated = true, note = "fade through white approximates a film burn"),
        Spec("flash", "fadewhite", note = "flash through white"),
        Spec("white_flash", "fadewhite"),
        Spec("color_flash", "fadewhite", approximated = true, note = "xfade cannot hue-cycle mid-transition; flashes through white"),
        Spec("glitch", "pixelize", approximated = true, note = "xfade has no glitch transition; pixelize break-up is the closest native effect"),
        Spec("glitch_in", "pixelize", approximated = true, note = "xfade has no glitch transition; pixelize break-up is the closest native effect"),
        Spec("rgb_glitch", "pixelize", approximated = true, note = "xfade cannot channel-shift mid-transition; pixelize break-up is the closest native effect"),
        Spec("tv_static", "fadegrays", approximated = true, note = "xfade cannot inject noise; fadegrays desaturates through the cut like a signal drop"),
        Spec("channel_change", "fadegrays", approximated = true, note = "xfade cannot inject noise; fadegrays desaturates through the cut like a signal drop"),
        Spec("vhs_transition", "fadegrays", approximated = true, note = "xfade cannot inject noise; fadegrays desaturates through the cut like a tape glitch"),
        Spec("shake", "hlslice", approximated = true, note = "xfade cannot displace the frame per-frame; sliced reveal is the closest native disruption"),
        Spec("shake_in", "hlslice", approximated = true, note = "xfade cannot displace the frame per-frame; sliced reveal is the closest native disruption"),
        Spec("shake_burst", "hrslice", approximated = true, note = "xfade cannot displace the frame per-frame; sliced reveal is the closest native disruption"),
        Spec("shake_transition", "hlslice", approximated = true, note = "xfade cannot displace the frame per-frame; sliced reveal is the closest native disruption"),
        Spec("typewriter", "fade", approximated = true, note = "typewriter is a TEXT animation, not a clip transition; falls back to a crossfade at the cut")
    )

    private val byId: Map<String, Spec> = SPECS.associateBy { it.id }

    /** Every transition id this catalog can realise (excluding [NONE]). */
    val supportedIds: List<String> = SPECS.map { it.id }.sorted()

    /** Normalises a UI/project transition id to a catalog key. */
    fun normalize(raw: String?): String =
        (raw ?: NONE).trim().lowercase().replace(" ", "_").replace("-", "_")

    /** True when [raw] means "no transition". */
    fun isNone(raw: String?): Boolean {
        val n = normalize(raw)
        return n == NONE || n.isEmpty() || n == "none_transition" || n == "hard_cut"
    }

    /**
     * Resolves a transition id to its [Spec], or null when the id is unknown.
     * Unknown ids are NOT silently dropped by callers — they fall back to a real
     * crossfade via [xfadeNameFor] so the user still gets a transition.
     */
    fun specFor(raw: String?): Spec? {
        val n = normalize(raw)
        if (isNone(n)) return null
        byId[n]?.let { return it }
        // "tr_slide_left" style ids coming from the premium catalog.
        if (n.startsWith("tr_")) byId[n.removePrefix("tr_")]?.let { return it }
        return null
    }

    /**
     * The REAL xfade transition name to use at a cut point for [raw].
     *
     * Returns null only for [NONE]/hard-cut. Any other unrecognised id degrades
     * to `fade` — a real, visible crossfade — rather than being dropped.
     */
    fun xfadeNameFor(raw: String?): String? {
        if (isNone(raw)) return null
        return specFor(raw)?.xfade ?: "fade"
    }

    /** True when [raw] resolves to an xfade name valid on the FFmpeg 4.4 floor. */
    fun isRealizable(raw: String?): Boolean {
        val name = xfadeNameFor(raw) ?: return false
        return name in BASE_XFADE_NAMES
    }

    /**
     * Clamps a requested transition duration so the xfade is always valid.
     *
     * `xfade` consumes [durationSec] from the END of clip A and the START of
     * clip B. If the duration is longer than either clip, FFmpeg runs out of
     * frames and either truncates output or errors. It must also leave the
     * shorter clip with some non-transition footage, so we cap at half the
     * shorter clip.
     */
    fun clampDuration(
        requestedSec: Double,
        clipADurationSec: Double,
        clipBDurationSec: Double
    ): Double {
        val shorter = minOf(clipADurationSec, clipBDurationSec)
        if (shorter <= 0.0) return 0.0
        val cap = shorter / 2.0
        if (cap < MIN_DURATION_SEC) return 0.0
        return requestedSec.coerceIn(MIN_DURATION_SEC, cap)
    }

    /**
     * Builds the real `xfade` filter body (no stream labels) for a cut point.
     *
     * @param offsetSec when the transition starts, on the timeline of the
     *                  accumulated left-hand stream.
     */
    fun xfadeFilter(transitionId: String?, durationSec: Double, offsetSec: Double): String? {
        val name = xfadeNameFor(transitionId) ?: return null
        if (durationSec < MIN_DURATION_SEC) return null
        return "xfade=transition=$name:duration=${fmt(durationSec)}:offset=${fmt(offsetSec)}"
    }

    /**
     * Formats a seconds value for an FFmpeg filter argument.
     *
     * Always uses `.` as the decimal separator: FFmpeg does not accept a
     * locale-specific comma, and Kotlin's default `toString()` on some
     * Android locales (and `String.format` without an explicit Locale) can
     * emit `0,7`, which makes the whole filter graph fail to parse.
     */
    fun fmt(v: Double): String = String.format(java.util.Locale.US, "%.3f", v)

    /**
     * The total duration of a timeline whose clips are joined with transitions.
     *
     * Each transition OVERLAPS the two clips it joins, so it removes its own
     * duration from the summed length. This is the value the export progress
     * calculation and any duration assertion must use.
     */
    fun totalDurationWithTransitions(
        clipDurationsSec: List<Double>,
        transitionDurationsSec: List<Double>
    ): Double {
        val sum = clipDurationsSec.sum()
        val overlap = transitionDurationsSec.sum()
        return (sum - overlap).coerceAtLeast(0.0)
    }

    /**
     * One resolved cut point in a multi-clip timeline.
     *
     * This is the SINGLE piece of transition state shared by the export
     * pipeline ([com.powercut.editor.domain.processing.VideoProcessor.processMultiClipTimeline])
     * and the live transition preview
     * ([com.powercut.editor.domain.processing.TransitionPreviewRenderer]):
     * both consume the same [cutSpecs] output, so the xfade name, clamped
     * duration and cut offset can never diverge between what the user sees on
     * the preview and what is burned into the exported file.
     *
     * @param xfadeName the REAL `xfade` transition name, or null when this cut
     *                  must be a hard cut (no transition selected, or both
     *                  joined clips are too short to host one).
     */
    data class CutSpec(
        val transitionId: String,
        val xfadeName: String?,
        val durationSec: Double,
        val offsetSec: Double
    )

    /**
     * Resolves every cut point of a multi-clip timeline into a [CutSpec].
     *
     * This is the exact math the export pipeline used inline (name lookup,
     * per-cut clamping to half the shorter clip, and the cumulative xfade
     * offset on the accumulated left-hand stream). Extracting it here lets the
     * live transition preview build the SAME graph, and lets tests pin the
     * parity — same rule as Phase 1's shared `buildEffects`/`buildEffectsFromProject`.
     *
     * Returns an empty list when no real transition is selected or fewer than
     * two clips are provided (the caller then joins with plain hard cuts).
     */
    fun cutSpecs(
        transitionId: String?,
        clipDurationsSec: List<Double>,
        requestedDurationSec: Double
    ): List<CutSpec> {
        val name = xfadeNameFor(transitionId) ?: return emptyList()
        val n = clipDurationsSec.size
        if (n < 2) return emptyList()

        val requested = requestedDurationSec.takeIf { it > 0.0 } ?: DEFAULT_DURATION_SEC
        val clamped = (0 until n - 1).map { i ->
            clampDuration(requested, clipDurationsSec[i], clipDurationsSec[i + 1])
        }

        val specs = ArrayList<CutSpec>(n - 1)
        var accDuration = clipDurationsSec[0]
        for (i in 1 until n) {
            val t = clamped[i - 1]
            if (t < MIN_DURATION_SEC) {
                // Too short to transition on: hard cut, no overlap.
                specs.add(CutSpec(normalize(transitionId), null, 0.0, 0.0))
                accDuration += clipDurationsSec[i]
            } else {
                // xfade consumes [t] from the end of the accumulated left
                // stream, so the transition starts at accDuration - t.
                val offset = (accDuration - t).coerceAtLeast(0.0)
                specs.add(CutSpec(normalize(transitionId), name, t, offset))
                accDuration += clipDurationsSec[i] - t
            }
        }
        return specs
    }
}
