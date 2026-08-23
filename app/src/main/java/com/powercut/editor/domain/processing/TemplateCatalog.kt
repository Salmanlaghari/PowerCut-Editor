package com.powercut.editor.domain.processing

import com.powercut.editor.data.TemplatePreset

object TemplateCatalog {

    val TEMPLATES: List<TemplatePreset> = listOf(
        // 1. TikTok Trending
        TemplatePreset(
            id = "tk_trending",
            name = "TikTok Trending",
            category = "Social Media",
            aspectRatio = "9:16",
            fps = 30,
            durationMs = 30000L,
            effects = listOf("fast_zoom_in", "neon_glow", "vibrance_plus20"),
            transitions = listOf("cross_dissolve"),
            textAnimation = "beat_synced_bold",
            defaultMusicId = "upbeat_trending_30s",
            description = "Fast zoom, neon glow, beat-synced text for viral content"
        ),

        // 2. Reels Intro
        TemplatePreset(
            id = "reels_intro",
            name = "Reels Intro",
            category = "Social Media",
            aspectRatio = "9:16",
            fps = 30,
            durationMs = 8000L,
            effects = listOf("fade_in_1s", "soft_vignette"),
            transitions = listOf("wipe_down"),
            textAnimation = "large_centered_title",
            defaultMusicId = "",
            description = "3s intro with fade-in, vignette, centered title"
        ),

        // 3. YouTube Shorts
        TemplatePreset(
            id = "youtube_shorts",
            name = "YouTube Shorts",
            category = "Social Media",
            aspectRatio = "9:16",
            fps = 30,
            durationMs = 60000L,
            effects = listOf("contrast_plus10", "sharpness_plus5"),
            transitions = listOf("cross_dissolve"),
            textAnimation = "top_bold_bottom_cta",
            defaultMusicId = "",
            description = "Vertical optimized with contrast boost and safe area guides"
        ),

        // 4. Cinematic Opening
        TemplatePreset(
            id = "cinematic_opening",
            name = "Cinematic Opening",
            category = "Cinematic",
            aspectRatio = "2.39:1",
            fps = 24,
            durationMs = 15000L,
            effects = listOf("film_grain", "vignette", "teal_orange_grade"),
            transitions = listOf("slow_cross_dissolve"),
            textAnimation = "elegant_serif_fade",
            defaultMusicId = "cinematic_ambient",
            description = "Letterbox 2.39:1, film look with elegant text"
        ),

        // 5. Instagram Story
        TemplatePreset(
            id = "ig_story",
            name = "Instagram Story",
            category = "Social Media",
            aspectRatio = "9:16",
            fps = 30,
            durationMs = 15000L,
            effects = listOf("gradient_bg", "rounded_corners_24dp"),
            transitions = listOf("cross_dissolve"),
            textAnimation = "swipe_up_placeholder",
            defaultMusicId = "",
            description = "Story format with gradient background and swipe-up CTA"
        ),

        // 6. Birthday Greeting
        TemplatePreset(
            id = "birthday_greeting",
            name = "Birthday Greeting",
            category = "Celebration",
            aspectRatio = "9:16",
            fps = 30,
            durationMs = 20000L,
            effects = listOf("confetti_particles", "colorful_border", "glow_text"),
            transitions = listOf("cross_dissolve"),
            textAnimation = "animated_birthday_text",
            defaultMusicId = "birthday_song_preview",
            description = "Confetti overlay, customizable name, birthday music"
        ),

        // 7. Wedding Highlight
        TemplatePreset(
            id = "wedding_highlight",
            name = "Wedding Highlight",
            category = "Celebration",
            aspectRatio = "16:9",
            fps = 30,
            durationMs = 60000L,
            effects = listOf("pastel_gradient", "vignette", "warm_tones"),
            transitions = listOf("slow_cross_dissolve_15s"),
            textAnimation = "elegant_white_serif",
            defaultMusicId = "romantic_piano",
            description = "Soft pastel look with elegant typography"
        ),

        // 8. Vlog Intro
        TemplatePreset(
            id = "vlog_intro",
            name = "Vlog Intro",
            category = "Vlog",
            aspectRatio = "9:16",
            fps = 30,
            durationMs = 5000L,
            effects = listOf("fast_montage_0_5s", "zoom_in_each"),
            transitions = listOf("slide_left"),
            textAnimation = "logo_reveal",
            defaultMusicId = "upbeat_5s_intro",
            description = "5s fast montage with logo animation"
        ),

        // 9. Music Visualizer
        TemplatePreset(
            id = "music_visualizer",
            name = "Music Visualizer",
            category = "Audio Visual",
            aspectRatio = "9:16",
            fps = 30,
            durationMs = 0L, // Match audio duration
            effects = listOf("fft_bars", "fft_waves", "fft_radial"),
            transitions = listOf("cross_dissolve"),
            textAnimation = "none",
            defaultMusicId = "",
            description = "Real-time FFT audio reactive visualizer with 50+ options"
        ),

        // 10. Lyric Video
        TemplatePreset(
            id = "lyric_video",
            name = "Lyric Video",
            category = "Audio Visual",
            aspectRatio = "9:16",
            fps = 30,
            durationMs = 0L, // Match audio duration
            effects = listOf("karaoke_color_change", "bold_text"),
            transitions = listOf("cross_dissolve"),
            textAnimation = "word_by_word_sync",
            defaultMusicId = "",
            description = "Import SRT/LRC for auto-timed karaoke lyrics"
        )
    )

    fun getTemplate(id: String): TemplatePreset? = TEMPLATES.firstOrNull { it.id == id }

    fun getTemplatesByCategory(category: String): List<TemplatePreset> = TEMPLATES.filter { it.category == category }

    val CATEGORIES: List<String> = TEMPLATES.map { it.category }.distinct()
}