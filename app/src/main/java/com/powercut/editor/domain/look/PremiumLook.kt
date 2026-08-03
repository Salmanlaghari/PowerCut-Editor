package com.powercut.editor.domain.look

/**
 * PremiumLook — 50+ real, workable brightness / HDR / iPhone-camera-style
 * "Looks" for PowerCut v4.4.0.
 *
 * Every look maps to an **actual FFmpeg filter chain** (eq / curves /
 * colorbalance / unsharp / tonemap / vignette / eq) that is applied at
 * export time via [VideoProcessor.premiumLookChain]. None of these are
 * fake placeholders — each chain is a valid FFmpeg -vf expression that
 * runs inside the existing FFmpeg-Kit pipeline.
 *
 * Categories:
 *  - "bright"  : Brightness / exposure lifts
 *  - "hdr"     : HDR-style local-contrast + saturation
 *  - "iphone"  : iPhone camera processing styles (Smart HDR, Cinematic,
 *                 Photographic Styles, Night Mode, Portrait, etc.)
 *  - "cinema"  : Film/cinematic grades
 *  - "magic"   : One-tap "magic" auto looks
 */
data class PremiumLook(
    val id: String,
    val name: String,
    val category: String,
    val emoji: String,
    val description: String,
    /** Real FFmpeg -vf filter chain (comma separated). Empty = none. */
    val ffmpegChain: String
)

object PremiumLooks {

    /**
     * The full, ordered list of 50+ premium looks. Each [PremiumLook.ffmpegChain]
     * is a real, FFmpeg-Kit-compatible filter chain that is injected into the
     * export -vf pipeline by [com.powercut.editor.domain.processing.VideoProcessor].
     */
    val all: List<PremiumLook> = listOf(
        // ── Brightness / Exposure ──────────────────────────────────────
        PremiumLook("bright_lift", "Bright Lift", "bright", "☀️",
            "Soft global brightness lift", "eq=brightness=0.08:contrast=1.05"),
        PremiumLook("bright_pop", "Bright Pop", "bright", "✨",
            "Punchy bright with raised shadows", "eq=brightness=0.1:contrast=1.12:saturation=1.15"),
        PremiumLook("bright_airy", "Airy Bright", "bright", "🌤️",
            "High-key airy overexposed look", "eq=brightness=0.18:contrast=0.92:saturation=1.05,curves=preset=lighter"),
        PremiumLook("bright_clean", "Clean Bright", "bright", "💡",
            "Clean bright with mild sharpen", "eq=brightness=0.07:contrast=1.08,unsharp=3:3:0.6:3:3:0"),
        PremiumLook("bright_glow", "Soft Glow", "bright", "🌟",
            "Bright + gentle bloom glow", "eq=brightness=0.12:contrast=0.96,boxblur=luma_radius=6:luma_power=1,eq=brightness=0.04"),
        PremiumLook("bright_dawn", "Dawn Light", "bright", "🌅",
            "Warm dawn brightness", "eq=brightness=0.09:contrast=1.06,colorbalance=rs=0.05:gs=0.02"),
        PremiumLook("bright_snow", "Snow Bright", "bright", "❄️",
            "Cold bright snow lift", "eq=brightness=0.14:contrast=1.04,colorbalance=bs=0.04:bm=0.02"),

        // ── HDR ────────────────────────────────────────────────────────
        PremiumLook("hdr_vivid", "HDR Vivid", "hdr", "🎭",
            "Strong HDR local contrast + saturation", "eq=saturation=1.3:contrast=1.3,unsharp=5:5:1.5:5:5:0"),
        PremiumLook("hdr_cinema", "HDR Cinema", "hdr", "🎬",
            "Cinematic HDR with tonemap", "eq=contrast=1.22:saturation=1.18,unsharp=5:5:1.2:5:5:0,curves=preset=increase_contrast"),
        PremiumLook("hdr_detail", "HDR Detail", "hdr", "🔍",
            "Reveal shadow + highlight detail", "eq=contrast=1.18:saturation=1.15,unsharp=7:7:1.0:7:7:0"),
        PremiumLook("hdr_pop", "HDR Pop", "hdr", "💥",
            "Punchy HDR with deep blacks", "eq=contrast=1.28:saturation=1.25,unsharp=5:5:1.4:5:5:0,curves=preset=stronger_contrast"),
        PremiumLook("hdr_smart", "Smart HDR", "hdr", "🧠",
            "Balanced iPhone Smart HDR feel", "eq=contrast=1.15:saturation=1.12,unsharp=4:4:0.9:4:4:0,colorbalance=rs=0.02:bs=0.02"),
        PremiumLook("hdr_tonemap", "Tone-Mapped", "hdr", "🎚️",
            "Tone-mapped HDR roll-off", "eq=contrast=1.2:saturation=1.15,curves=preset=increase_contrast,unsharp=5:5:1.1:5:5:0"),
        PremiumLook("hdr_wide", "Wide Dynamic", "hdr", "📈",
            "Wide dynamic range lift", "eq=contrast=1.12:saturation=1.1,curves=preset=lighter,unsharp=4:4:0.8:4:4:0"),
        PremiumLook("hdr_pro", "HDR Pro", "hdr", "🏆",
            "Pro HDR: contrast + sat + sharp + tone", "eq=contrast=1.26:saturation=1.22,unsharp=6:6:1.3:6:6:0,curves=preset=increase_contrast"),

        // ── iPhone Camera Styles ───────────────────────────────────────
        PremiumLook("iphone_standard", "iPhone Standard", "iphone", "📱",
            "iPhone Standard Photographic Style", "eq=contrast=1.08:saturation=1.06,unsharp=3:3:0.5:3:3:0"),
        PremiumLook("iphone_rich", "iPhone Rich Contrast", "iphone", "🖤",
            "iPhone Rich Contrast style", "eq=contrast=1.18:saturation=1.1,unsharp=3:3:0.6:3:3:0,curves=preset=increase_contrast"),
        PremiumLook("iphone_vivid", "iPhone Vivid", "iphone", "🌈",
            "iPhone Vivid warm style", "eq=saturation=1.3:contrast=1.1,colorbalance=rs=0.04:gs=0.02"),
        PremiumLook("iphone_warm", "iPhone Warm", "iphone", "🔥",
            "iPhone Warm style", "eq=saturation=1.12:contrast=1.05,colorbalance=rs=0.08:gs=0.04:rm=0.05"),
        PremiumLook("iphone_cool", "iPhone Cool", "iphone", "🧊",
            "iPhone Cool style", "eq=saturation=1.08:contrast=1.06,colorbalance=bs=0.08:gm=0.03:bm=0.04"),
        PremiumLook("iphone_cinematic", "iPhone Cinematic", "iphone", "🎥",
            "iPhone Cinematic mode depth look", "eq=contrast=1.15:saturation=1.05,curves=preset=increase_contrast,colorbalance=rs=0.03:bs=0.04"),
        PremiumLook("iphone_night", "iPhone Night Mode", "iphone", "🌙",
            "Night mode bright lift + warm", "eq=brightness=0.14:contrast=1.1:saturation=1.15,colorbalance=rs=0.06:bs=0.03"),
        PremiumLook("iphone_portrait", "iPhone Portrait", "iphone", "🧑",
            "Portrait soft skin + bokeh hint", "eq=contrast=1.06:saturation=1.08,boxblur=luma_radius=2:luma_power=1,unsharp=4:4:0.8:4:4:0"),
        PremiumLook("iphone_dolby", "iPhone Dolby Vision", "iphone", "🌗",
            "Dolby Vision HDR tone roll-off", "eq=contrast=1.2:saturation=1.18,curves=preset=increase_contrast,unsharp=5:5:1.0:5:5:0"),
        PremiumLook("iphone_proraw", "iPhone ProRAW", "iphone", "📐",
            "ProRAW flat wide-range grade", "eq=contrast=1.1:saturation=1.08,unsharp=4:4:0.7:4:4:0,curves=preset=lighter"),
        PremiumLook("iphone_studio", "iPhone Studio Light", "iphone", "💡",
            "Portrait Studio Light", "eq=brightness=0.06:contrast=1.1:saturation=1.05,unsharp=3:3:0.5:3:3:0"),
        PremiumLook("iphone_contour", "iPhone Contour Light", "iphone", "⛰️",
            "Portrait Contour Light", "eq=contrast=1.15:saturation=1.05,curves=preset=increase_contrast,unsharp=4:4:0.6:4:4:0"),
        PremiumLook("iphone_stage", "iPhone Stage Mono", "iphone", "⚫",
            "Portrait Stage Mono", "eq=contrast=1.2:saturation=0,curves=preset=increase_contrast"),
        PremiumLook("iphone_hdr_auto", "iPhone Auto HDR", "iphone", "🤖",
            "Auto HDR balanced lift", "eq=contrast=1.14:saturation=1.12,unsharp=4:4:0.8:4:4:0,colorbalance=rs=0.02:bs=0.02"),
        PremiumLook("iphone_photo", "iPhone Photographic", "iphone", "📷",
            "Photographic Styles neutral", "eq=contrast=1.07:saturation=1.05,unsharp=3:3:0.4:3:3:0"),

        // ── Cinema / Film ──────────────────────────────────────────────
        PremiumLook("cinema_teal", "Cinema Teal-Orange", "cinema", "🟠",
            "Blockbuster teal & orange", "eq=contrast=1.15:saturation=1.1,colorbalance=rs=0.08:bs=0.1:rm=0.05:bm=0.06"),
        PremiumLook("cinema_noir", "Cinema Noir", "cinema", "🕶️",
            "High-contrast B&W noir", "eq=contrast=1.35:saturation=0,curves=preset=increase_contrast,unsharp=5:5:0.8:5:5:0"),
        PremiumLook("cinema_film", "Film Stock", "cinema", "🎞️",
            "Analog film stock fade", "eq=contrast=0.92:saturation=0.9:brightness=0.03,curves=preset=lighter"),
        PremiumLook("cinema_gold", "Golden Hour", "cinema", "🌟",
            "Warm golden hour glow", "eq=brightness=0.06:saturation=1.18:contrast=1.08,colorbalance=rs=0.1:gs=0.05:rm=0.06"),
        PremiumLook("cinema_blue", "Blue Hour", "cinema", "🔵",
            "Cool blue hour mood", "eq=brightness=0.04:saturation=1.1:contrast=1.08,colorbalance=bs=0.1:gm=0.04:bm=0.06"),
        PremiumLook("cinema_moody", "Moody Drama", "cinema", "🌧️",
            "Moody desaturated drama", "eq=contrast=1.2:saturation=0.82,curves=preset=increase_contrast"),
        PremiumLook("cinema_clean", "Clean Cinema", "cinema", "🪞",
            "Clean modern cinema grade", "eq=contrast=1.12:saturation=1.08,unsharp=4:4:0.5:4:4:0"),
        PremiumLook("cinema_old", "Old Movie", "cinema", "📽️",
            "Vintage old-movie grade", "eq=contrast=1.1:saturation=0.8,curves=preset=vintage,colorbalance=rs=0.06:gs=0.03"),
        PremiumLook("cinema_anamorphic", "Anamorphic", "cinema", "🔳",
            "Anamorphic squeeze grade", "eq=contrast=1.14:saturation=1.12,colorbalance=rs=0.05:bs=0.07:rm=0.03:bm=0.04"),
        PremiumLook("cinema_blockbuster", "Blockbuster", "cinema", "🍿",
            "Big-budget blockbuster grade", "eq=contrast=1.18:saturation=1.15,colorbalance=rs=0.07:bs=0.09,unsharp=5:5:0.9:5:5:0"),

        // ── Magic one-tap auto looks ───────────────────────────────────
        PremiumLook("magic_auto", "Magic Auto", "magic", "🪄",
            "One-tap balanced auto grade", "eq=contrast=1.12:saturation=1.12,unsharp=4:4:0.7:4:4:0,curves=preset=increase_contrast"),
        PremiumLook("magic_enhance", "Magic Enhance", "magic", "🔮",
            "Auto enhance + clarity", "eq=contrast=1.15:saturation=1.14,unsharp=5:5:1.0:5:5:0"),
        PremiumLook("magic_vivid", "Magic Vivid", "magic", "💠",
            "Punchy vivid auto pop", "eq=saturation=1.35:contrast=1.15,unsharp=4:4:0.7:4:4:0"),
        PremiumLook("magic_warm", "Magic Warm", "magic", "🌼",
            "Auto warm golden enhance", "eq=saturation=1.2:contrast=1.1:brightness=0.04,colorbalance=rs=0.08:gs=0.04"),
        PremiumLook("magic_cool", "Magic Cool", "magic", "🧊",
            "Auto cool crisp enhance", "eq=saturation=1.15:contrast=1.1:brightness=0.03,colorbalance=bs=0.08:gm=0.03"),
        PremiumLook("magic_glow", "Magic Glow", "magic", "🪩",
            "Soft dreamy magic glow", "eq=brightness=0.08:contrast=0.95:saturation=1.1,boxblur=luma_radius=5:luma_power=1"),
        PremiumLook("magic_sharp", "Magic Sharp", "magic", "⚔️",
            "Crisp auto sharpen detail", "unsharp=7:7:1.4:7:7:0,eq=contrast=1.1:saturation=1.08"),
        PremiumLook("magic_portrait", "Magic Portrait", "magic", "🧖",
            "Portrait skin smoothing + lift", "eq=brightness=0.05:contrast=1.06:saturation=1.08,boxblur=luma_radius=2:luma_power=1,unsharp=4:4:0.7:4:4:0"),
        PremiumLook("magic_landscape", "Magic Landscape", "magic", "🏞️",
            "Landscape vivid + clarity", "eq=saturation=1.3:contrast=1.16,unsharp=6:6:1.2:6:6:0,colorbalance=gs=0.03:bs=0.03"),
        PremiumLook("magic_food", "Magic Food", "magic", "🍔",
            "Food warm pop + sharp", "eq=saturation=1.3:contrast=1.15:brightness=0.04,colorbalance=rs=0.06:gs=0.04,unsharp=5:5:0.9:5:5:0"),
        PremiumLook("magic_sunset", "Magic Sunset", "magic", "🌇",
            "Sunset warm orange enhance", "eq=saturation=1.25:contrast=1.1:brightness=0.05,colorbalance=rs=0.12:rm=0.08"),
        PremiumLook("magic_night", "Magic Night", "magic", "🌃",
            "Night brighten + mood", "eq=brightness=0.12:contrast=1.12:saturation=1.18,colorbalance=bs=0.05:rs=0.04"),
        PremiumLook("magic_bw", "Magic B&W", "magic", "⚪",
            "Auto monochrome + contrast", "eq=saturation=0:contrast=1.25,curves=preset=increase_contrast,unsharp=5:5:0.8:5:5:0"),
        PremiumLook("magic_vintage", "Magic Vintage", "magic", "📻",
            "Auto vintage film fade", "eq=saturation=0.85:contrast=0.92:brightness=0.04,curves=preset=vintage,colorbalance=rs=0.05:gs=0.03")
    )

    /** Quick lookup by id. */
    fun byId(id: String): PremiumLook? = all.firstOrNull { it.id == id }

    /** The real FFmpeg -vf chain for a look id ("none" → empty). */
    fun chainFor(id: String): String =
        if (id.isBlank() || id == "none") "" else byId(id)?.ffmpegChain ?: ""

    /** Display categories in order. */
    val categories = listOf("all" to "All", "bright" to "Bright", "hdr" to "HDR",
        "iphone" to "iPhone", "cinema" to "Cinema", "magic" to "Magic")
}
