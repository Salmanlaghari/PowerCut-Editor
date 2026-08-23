package com.powercut.editor.ui.premium

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableIntStateOf

// ═══════════════════════════════════════════════════════════════
//  PREMIUM FEATURE DATA MODELS — 500+ Options
//  PowerCut Editor Pro
// ═══════════════════════════════════════════════════════════════

// ─── Core Feature Option ───────────────────────────────────────
data class PremiumOption(
    val id: String,
    val name: String,
    val description: String,
    val emoji: String,
    val category: String,
    val isEnabled: MutableState<Boolean> = mutableStateOf(false),
    val floatValue: MutableState<Float> = mutableFloatStateOf(0f),
    val intValue: MutableState<Int> = mutableIntStateOf(0),
    val stringValue: MutableState<String> = mutableStateOf("")
)

// ─── Slider-based control ──────────────────────────────────────
data class SliderControl(
    val id: String,
    val name: String,
    val emoji: String,
    val minValue: Float,
    val maxValue: Float,
    val defaultValue: Float,
    val stepSize: Float = 0.01f,
    val currentValue: MutableState<Float> = mutableFloatStateOf(defaultValue)
)

// ─── Dropdown / Enum selector ──────────────────────────────────
data class SelectorOption(
    val id: String,
    val name: String,
    val emoji: String,
    val options: List<String>,
    val selectedIndex: MutableState<Int> = mutableIntStateOf(0)
)

// ═══════════════════════════════════════════════════════════════
//  1. VIDEO EFFECTS (85+ options)
// ═══════════════════════════════════════════════════════════════

object VideoEffects {

    // ── Color Filters (25) ─────────────────────────────────────
    val colorFilters = listOf(
        PremiumOption("cf_vivid", "Vivid", "Boost saturation and contrast for vibrant colors", "🌈", "Color Filters"),
        PremiumOption("cf_cinematic", "Cinematic", "Hollywood-grade teal and orange color grade", "🎬", "Color Filters"),
        PremiumOption("cf_vintage", "Vintage", "Warm nostalgic tones with faded blacks", "📸", "Color Filters"),
        PremiumOption("cf_retro", "Retro", "80s-inspired neon color palette", "🕹️", "Color Filters"),
        PremiumOption("cf_noir", "Noir", "Classic black and white film noir style", "🖤", "Color Filters"),
        PremiumOption("cf_chrome", "Chrome", "High-contrast metallic silver tones", "🔘", "Color Filters"),
        PremiumOption("cf_fade", "Fade", "Soft washed-out pastel look", "🌫️", "Color Filters"),
        PremiumOption("cf_warm", "Warm", "Golden hour warmth with amber tones", "☀️", "Color Filters"),
        PremiumOption("cf_cool", "Cool", "Blue-tinted cold cinematic feel", "❄️", "Color Filters"),
        PremiumOption("cf_dreamy", "Dreamy", "Soft glow with lifted shadows", "💭", "Color Filters"),
        PremiumOption("cf_moody", "Moody", "Dark crushed shadows with desaturated tones", "🌑", "Color Filters"),
        PremiumOption("cf_filmgrain", "Film Grain", "Authentic analog film grain overlay texture", "🎞️", "Color Filters"),
        PremiumOption("cf_sepia_enhanced", "Sepia Enhanced", "Rich sepia with enhanced midtone detail", "🟤", "Color Filters"),
        PremiumOption("cf_crossprocess", "Cross Process", "Chemical cross-processing color shift", "🧪", "Color Filters"),
        PremiumOption("cf_bleachbypass", "Bleach Bypass", "High contrast desaturated cinema look", "🪣", "Color Filters"),
        PremiumOption("cf_lomo", "Lomo", "Lomography-style vignette and saturated colors", "📷", "Color Filters"),
        PremiumOption("cf_duotone_1", "Duotone Orange/Blue", "Two-tone orange highlights and blue shadows", "🔵", "Color Filters"),
        PremiumOption("cf_duotone_2", "Duotone Purple/Gold", "Two-tone purple highlights and gold shadows", "🟣", "Color Filters"),
        PremiumOption("cf_duotone_3", "Duotone Teal/Pink", "Two-tone teal highlights and pink shadows", "🩷", "Color Filters"),
        PremiumOption("cf_duotone_4", "Duotone Green/Magenta", "Two-tone green highlights and magenta shadows", "🟢", "Color Filters"),
        PremiumOption("cf_duotone_5", "Duotone Red/Cyan", "Two-tone red highlights and cyan shadows", "🔴", "Color Filters"),
        PremiumOption("cf_duotone_6", "Duotone Yellow/Indigo", "Two-tone yellow highlights and indigo shadows", "🟡", "Color Filters"),
        PremiumOption("cf_duotone_7", "Duotone Coral/Navy", "Two-tone coral highlights and navy shadows", "🪸", "Color Filters"),
        PremiumOption("cf_duotone_8", "Duotone Lime/Plum", "Two-tone lime highlights and plum shadows", "🍋", "Color Filters"),
        PremiumOption("cf_duotone_9", "Duotone Peach/Steel", "Two-tone peach highlights and steel shadows", "🍑", "Color Filters"),
        PremiumOption("cf_duotone_10", "Duotone Mint/Burgundy", "Two-tone mint highlights and burgundy shadows", "🍃", "Color Filters")
    )

    // ── Split Toning & Color Splash (5) ────────────────────────
    val splitToning = listOf(
        PremiumOption("st_shadow_hue", "Shadow Hue", "Apply color tint to shadow regions", "🎨", "Split Toning"),
        PremiumOption("st_highlight_hue", "Highlight Hue", "Apply color tint to highlight regions", "✨", "Split Toning"),
        PremiumOption("st_balance", "Toning Balance", "Balance between shadow and highlight toning", "⚖️", "Split Toning"),
        PremiumOption("cs_preserve_color", "Color Splash", "Preserve one selected color, rest becomes B&W", "💧", "Split Toning"),
        PremiumOption("cs_selected_hue", "Splash Hue Selector", "Hue angle for color preservation (0-360)", "🎯", "Split Toning")
    )

    // ── Light Leak (5) ─────────────────────────────────────────
    val lightLeaks = listOf(
        PremiumOption("ll_warm_burst", "Warm Burst", "Golden warm light leak from corner", "🌅", "Light Leak"),
        PremiumOption("ll_cool_streak", "Cool Streak", "Blue-tinted light streak overlay", "💎", "Light Leak"),
        PremiumOption("ll_rainbow_flare", "Rainbow Flare", "Multi-color prism light flare", "🌈", "Light Leak"),
        PremiumOption("ll_anamorphic", "Anamorphic Flare", "Horizontal blue anamorphic lens streak", "🔵", "Light Leak"),
        PremiumOption("ll_dust_particles", "Dust Particles", "Floating dust motes in light beams", "✨", "Light Leak")
    )

    // ── Bokeh Overlay (3) ──────────────────────────────────────
    val bokehOverlays = listOf(
        PremiumOption("bokeh_circle", "Circle Bokeh", "Soft circular bokeh light overlay", "⭕", "Bokeh"),
        PremiumOption("bokeh_hexagon", "Hexagon Bokeh", "Hexagonal bokeh lens blur pattern", "⬡", "Bokeh"),
        PremiumOption("bokeh_heart", "Heart Bokeh", "Heart-shaped bokeh light overlay", "💕", "Bokeh")
    )

    // ── Speed Ramping (8) ──────────────────────────────────────
    val speedRamping = listOf(
        PremiumOption("sr_epic_slowmo", "Epic Slow-Mo", "Dramatic slow motion with smooth interpolation", "🐌", "Speed Ramping"),
        PremiumOption("sr_hyperlapse", "Hyperlapse", "Ultra-fast timelapse with stabilization", "⏩", "Speed Ramping"),
        PremiumOption("sr_timefreeze", "Time Freeze", "Freeze frame with slow zoom effect", "🧊", "Speed Ramping"),
        PremiumOption("sr_reverse", "Reverse", "Full clip reverse playback", "⏪", "Speed Ramping"),
        PremiumOption("sr_pendulum", "Pendulum", "Forward-reverse-forward loop effect", "🔄", "Speed Ramping"),
        PremiumOption("sr_ramp_up", "Speed Ramp Up", "Gradual speed increase from slow to fast", "📈", "Speed Ramping"),
        PremiumOption("sr_ramp_down", "Speed Ramp Down", "Gradual speed decrease from fast to slow", "📉", "Speed Ramping"),
        PremiumOption("sr_jcut_speed", "J-Cut Speed", "Speed change aligned with audio transition", "✂️", "Speed Ramping")
    )

    // ── Visual FX (20) ─────────────────────────────────────────
    val visualFx = listOf(
        PremiumOption("vfx_glitch_digital", "Digital Glitch", "Digital artifact corruption effect", "📺", "Visual FX"),
        PremiumOption("vfx_glitch_analog", "Analog Glitch", "VHS-style analog signal distortion", "📼", "Visual FX"),
        PremiumOption("vfx_glitch_rgb", "RGB Split Glitch", "Red-green-blue channel separation", "🔴", "Visual FX"),
        PremiumOption("vfx_glitch_scan", "Scan Line Glitch", "CRT scanline distortion effect", "📡", "Visual FX"),
        PremiumOption("vfx_glitch_datamosh", "Datamosh", "Compression artifact glitch art", "💾", "Visual FX"),
        PremiumOption("vfx_vhs", "VHS Effect", "Complete VHS tape aesthetic with tracking", "📼", "Visual FX"),
        PremiumOption("vfx_chromatic", "Chromatic Aberration", "Color fringing lens distortion", "🔮", "Visual FX"),
        PremiumOption("vfx_lensflare_warm", "Warm Lens Flare", "Golden warm lens flare overlay", "☀️", "Visual FX"),
        PremiumOption("vfx_lensflare_cool", "Cool Lens Flare", "Blue cool lens flare overlay", "💎", "Visual FX"),
        PremiumOption("vfx_lensflare_anamorphic", "Anamorphic Lens Flare", "Horizontal blue streak flare", "🔵", "Visual FX"),
        PremiumOption("vfx_lensflare_rainbow", "Rainbow Lens Flare", "Multi-color prism flare overlay", "🌈", "Visual FX"),
        PremiumOption("vfx_particles_snow", "Snow Particles", "Falling snow particle simulation", "❄️", "Visual FX"),
        PremiumOption("vfx_particles_rain", "Rain Particles", "Rain drop particle overlay", "🌧️", "Visual FX"),
        PremiumOption("vfx_particles_fire", "Fire Particles", "Rising fire ember particles", "🔥", "Visual FX"),
        PremiumOption("vfx_particles_sparkle", "Sparkle Particles", "Twinkling sparkle star particles", "✨", "Visual FX"),
        PremiumOption("vfx_particles_dust", "Dust Particles", "Floating dust motes in light", "🌫️", "Visual FX"),
        PremiumOption("vfx_motion_blur", "Motion Blur", "Directional motion blur effect", "💨", "Visual FX"),
        PremiumOption("vfx_zoom_pulse", "Zoom Pulse", "Rhythmic zoom in/out pulse effect", "🔍", "Visual FX"),
        PremiumOption("vfx_shake", "Camera Shake", "Simulated camera shake intensity", "📳", "Visual FX"),
        PremiumOption("vfx_flash", "Flash", "Bright flash transition overlay", "⚡", "Visual FX")
    )

    // ── Strobe & Additional FX (3) ─────────────────────────────
    val additionalFx = listOf(
        PremiumOption("vfx_strobe", "Strobe Light", "Rhythmic strobe light flash effect", "💡", "Visual FX"),
        PremiumOption("vfx_neon_glow", "Neon Glow", "Neon edge detection glow overlay", "💜", "Visual FX"),
        PremiumOption("vfx_vignette", "Cinematic Vignette", "Dark edge vignette focus effect", "🔲", "Visual FX")
    )

    val all get() = colorFilters + splitToning + lightLeaks + bokehOverlays + speedRamping + visualFx + additionalFx
}

// ═══════════════════════════════════════════════════════════════
//  2. AUDIO TOOLS (65+ options)
// ═══════════════════════════════════════════════════════════════

object AudioTools {

    // ── Equalizer Presets (9) ──────────────────────────────────
    val equalizerPresets = listOf(
        PremiumOption("eq_flat", "Flat", "No EQ modification, original audio", "📊", "Equalizer"),
        PremiumOption("eq_bass_boost", "Bass Boost", "Enhanced low frequencies for punch", "🔈", "Equalizer"),
        PremiumOption("eq_treble_boost", "Treble Boost", "Enhanced high frequencies for clarity", "🔊", "Equalizer"),
        PremiumOption("eq_vocal", "Vocal", "Optimized for voice and speech clarity", "🗣️", "Equalizer"),
        PremiumOption("eq_rock", "Rock", "Punchy mids and boosted bass for rock", "🎸", "Equalizer"),
        PremiumOption("eq_pop", "Pop", "Balanced with slight bass and treble lift", "🎵", "Equalizer"),
        PremiumOption("eq_jazz", "Jazz", "Warm mids with smooth high rolloff", "🎷", "Equalizer"),
        PremiumOption("eq_classical", "Classical", "Wide dynamic range with natural balance", "🎻", "Equalizer"),
        PremiumOption("eq_custom_5band", "Custom 5-Band", "Manual 5-band equalizer control", "🎛️", "Equalizer")
    )

    // ── Custom EQ Bands (5) ────────────────────────────────────
    val eqBands = listOf(
        SliderControl("eq_band_60", "60 Hz", "🔘", -12f, 12f, 0f),
        SliderControl("eq_band_230", "230 Hz", "🔘", -12f, 12f, 0f),
        SliderControl("eq_band_910", "910 Hz", "🔘", -12f, 12f, 0f),
        SliderControl("eq_band_3600", "3.6 kHz", "🔘", -12f, 12f, 0f),
        SliderControl("eq_band_14000", "14 kHz", "🔘", -12f, 12f, 0f)
    )

    // ── Audio Effects (14) ─────────────────────────────────────
    val audioEffects = listOf(
        PremiumOption("ae_reverb_hall", "Hall Reverb", "Large hall reverb simulation", "🏛️", "Audio Effects"),
        PremiumOption("ae_reverb_room", "Room Reverb", "Small room reverb simulation", "🏠", "Audio Effects"),
        PremiumOption("ae_reverb_plate", "Plate Reverb", "Classic plate reverb metallic tail", "💿", "Audio Effects"),
        PremiumOption("ae_reverb_spring", "Spring Reverb", "Vintage spring reverb surf tone", "🌊", "Audio Effects"),
        PremiumOption("ae_reverb_chamber", "Chamber Reverb", "Echoic chamber reverb simulation", "🏰", "Audio Effects"),
        PremiumOption("ae_echo", "Echo", "Single echo delay effect", "🔁", "Audio Effects"),
        PremiumOption("ae_pitch_shift", "Pitch Shift", "Shift pitch from -12 to +12 semitones", "🎹", "Audio Effects"),
        PremiumOption("ae_tempo_change", "Tempo Change", "Change playback tempo without pitch shift", "⏱️", "Audio Effects"),
        PremiumOption("ae_fade_in", "Fade In", "Gradual volume increase at start", "📈", "Audio Effects"),
        PremiumOption("ae_fade_out", "Fade Out", "Gradual volume decrease at end", "📉", "Audio Effects"),
        PremiumOption("ae_normalize", "Normalize", "Auto-adjust peak volume to 0dB", "📊", "Audio Effects"),
        PremiumOption("ae_compressor", "Compressor", "Dynamic range compression for even levels", "🗜️", "Audio Effects"),
        PremiumOption("ae_noise_gate", "Noise Gate", "Remove audio below threshold level", "🚪", "Audio Effects"),
        PremiumOption("ae_deesser", "De-Esser", "Reduce sibilant frequencies in speech", "🐍", "Audio Effects")
    )

    // ── Audio Sliders (4) ──────────────────────────────────────
    val audioSliders = listOf(
        SliderControl("as_pitch_semitones", "Pitch (Semitones)", "🎹", -12f, 12f, 0f, 1f),
        SliderControl("as_tempo_factor", "Tempo Factor", "⏱️", 0.5f, 2.0f, 1.0f, 0.05f),
        SliderControl("as_reverb_mix", "Reverb Mix", "🏛️", 0f, 100f, 30f, 1f),
        SliderControl("as_compressor_ratio", "Compressor Ratio", "🗜️", 1f, 20f, 4f, 0.5f)
    )

    // ── Stereo Effects (2) ─────────────────────────────────────
    val stereoEffects = listOf(
        PremiumOption("se_stereo_widener", "Stereo Widener", "Expand stereo image width", "🎧", "Stereo Effects"),
        PremiumOption("se_mono_convert", "Mono Convert", "Merge stereo channels to mono", "📻", "Stereo Effects")
    )

    // ── Voice Changer (9) ──────────────────────────────────────
    val voiceChangers = listOf(
        PremiumOption("vc_chipmunk", "Chipmunk", "High-pitched chipmunk voice", "🐿️", "Voice Changer"),
        PremiumOption("vc_deep", "Deep Voice", "Deep bass-boosted voice", "🧔", "Voice Changer"),
        PremiumOption("vc_robot", "Robot", "Robotic vocoder voice effect", "🤖", "Voice Changer"),
        PremiumOption("vc_alien", "Alien", "Alien otherworldly voice modulation", "👽", "Voice Changer"),
        PremiumOption("vc_megaphone", "Megaphone", "Megaphone/PA system voice", "📣", "Voice Changer"),
        PremiumOption("vc_radio", "Radio", "AM radio crackly voice", "📻", "Voice Changer"),
        PremiumOption("vc_underwater", "Underwater", "Submerged underwater voice", "🫧", "Voice Changer"),
        PremiumOption("vc_whisper", "Whisper", "Soft whisper ASMR voice", "🤫", "Voice Changer"),
        PremiumOption("vc_darthvader", "Darth Vader", "Deep breathing dark lord voice", "😈", "Voice Changer")
    )

    // ── Background Music Genres (20) ───────────────────────────
    val backgroundMusicGenres = listOf(
        PremiumOption("bgm_cinematic", "Cinematic", "Epic orchestral cinematic score", "🎬", "Background Music"),
        PremiumOption("bgm_happy", "Happy", "Upbeat cheerful positive vibes", "😊", "Background Music"),
        PremiumOption("bgm_sad", "Sad", "Emotional melancholic piano", "😢", "Background Music"),
        PremiumOption("bgm_epic", "Epic", "Grand epic orchestral build", "⚔️", "Background Music"),
        PremiumOption("bgm_lofi", "Lo-fi", "Chill lo-fi hip hop beats", "🎧", "Background Music"),
        PremiumOption("bgm_hiphop", "Hip Hop", "Urban hip hop beat instrumental", "🎤", "Background Music"),
        PremiumOption("bgm_edm", "EDM", "Electronic dance music drop", "🎛️", "Background Music"),
        PremiumOption("bgm_acoustic", "Acoustic", "Gentle acoustic guitar melody", "🎸", "Background Music"),
        PremiumOption("bgm_jazz", "Jazz", "Smooth jazz trio instrumental", "🎷", "Background Music"),
        PremiumOption("bgm_classical", "Classical", "Classical orchestra arrangement", "🎻", "Background Music"),
        PremiumOption("bgm_ambient", "Ambient", "Atmospheric ambient soundscape", "🌌", "Background Music"),
        PremiumOption("bgm_rock", "Rock", "Electric guitar rock anthem", "🤘", "Background Music"),
        PremiumOption("bgm_pop", "Pop", "Modern pop instrumental beat", "🎵", "Background Music"),
        PremiumOption("bgm_rnb", "R&B", "Smooth R&B groove instrumental", "💜", "Background Music"),
        PremiumOption("bgm_reggae", "Reggae", "Island reggae rhythm instrumental", "🏝️", "Background Music"),
        PremiumOption("bgm_latin", "Latin", "Latin salsa/bachata rhythm", "💃", "Background Music"),
        PremiumOption("bgm_kpop", "K-Pop", "Korean pop dance instrumental", "🇰🇷", "Background Music"),
        PremiumOption("bgm_country", "Country", "Country folk guitar and fiddle", "🤠", "Background Music"),
        PremiumOption("bgm_electronic", "Electronic", "Synth electronic pulse beat", "⚡", "Background Music"),
        PremiumOption("bgm_meditation", "Meditation", "Calm meditation bell and drone", "🧘", "Background Music")
    )

    val all get() = equalizerPresets + eqBands.map { PremiumOption(it.id, it.name, "EQ Band ${it.name}", it.emoji, "Equalizer") } + audioEffects + audioSliders.map { PremiumOption(it.id, it.name, "${it.name} control", it.emoji, "Audio Effects") } + stereoEffects + voiceChangers + backgroundMusicGenres
}

// ═══════════════════════════════════════════════════════════════
//  3. TEXT & TYPOGRAPHY (75+ options)
// ═══════════════════════════════════════════════════════════════

object TextTypography {

    // ── Premium Fonts (15) ─────────────────────────────────────
    val premiumFonts = listOf(
        PremiumOption("font_bold", "Bold", "Heavy weight bold typeface", "🅱️", "Fonts"),
        PremiumOption("font_italic", "Italic", "Elegant italic slanted typeface", "✏️", "Fonts"),
        PremiumOption("font_handwritten", "Handwritten", "Natural handwritten script font", "✍️", "Fonts"),
        PremiumOption("font_monospace", "Monospace", "Fixed-width code-style font", "💻", "Fonts"),
        PremiumOption("font_serif", "Serif", "Classic serif editorial font", "📰", "Fonts"),
        PremiumOption("font_sansserif", "Sans-Serif", "Clean modern sans-serif font", "🔤", "Fonts"),
        PremiumOption("font_display", "Display", "Bold display headline font", "🔠", "Fonts"),
        PremiumOption("font_gothic", "Gothic", "Dark gothic medieval typeface", "🏰", "Fonts"),
        PremiumOption("font_retro", "Retro", "Vintage retro sign lettering", "🪧", "Fonts"),
        PremiumOption("font_neon", "Neon", "Glowing neon sign lettering", "💜", "Fonts"),
        PremiumOption("font_glitch", "Glitch Text", "Corrupted glitch-style lettering", "📺", "Fonts"),
        PremiumOption("font_3d_shadow", "3D Shadow", "Three-dimensional drop shadow text", "📐", "Fonts"),
        PremiumOption("font_outline", "Outline", "Hollow outline stroke text", "⬜", "Fonts"),
        PremiumOption("font_gradient", "Gradient", "Color gradient fill text", "🌈", "Fonts"),
        PremiumOption("font_metallic", "Metallic", "Shiny metallic chrome text", "⚙️", "Fonts")
    )

    // ── Text Animations (12) ───────────────────────────────────
    val textAnimations = listOf(
        PremiumOption("ta_fade_in", "Fade In", "Text fades in from transparent", "🌅", "Text Animations"),
        PremiumOption("ta_fade_out", "Fade Out", "Text fades out to transparent", "🌇", "Text Animations"),
        PremiumOption("ta_typewriter", "Typewriter", "Characters appear one by one", "⌨️", "Text Animations"),
        PremiumOption("ta_bounce", "Bounce", "Text bounces in from above", "🏀", "Text Animations"),
        PremiumOption("ta_slide_left", "Slide Left", "Text slides in from the right", "⬅️", "Text Animations"),
        PremiumOption("ta_slide_right", "Slide Right", "Text slides in from the left", "➡️", "Text Animations"),
        PremiumOption("ta_slide_up", "Slide Up", "Text slides up from below", "⬆️", "Text Animations"),
        PremiumOption("ta_slide_down", "Slide Down", "Text slides down from above", "⬇️", "Text Animations"),
        PremiumOption("ta_zoom_in", "Zoom In", "Text zooms in from small to full size", "🔍", "Text Animations"),
        PremiumOption("ta_zoom_out", "Zoom Out", "Text zooms out from large to normal", "🔎", "Text Animations"),
        PremiumOption("ta_rotate", "Rotate", "Text spins and rotates into position", "🔄", "Text Animations"),
        PremiumOption("ta_wave", "Wave", "Characters animate in wave pattern", "🌊", "Text Animations")
    )

    // ── Text Positions (9) ─────────────────────────────────────
    val textPositions = listOf(
        PremiumOption("tp_top_left", "Top Left", "Position text at top-left corner", "↖️", "Text Position"),
        PremiumOption("tp_top_center", "Top Center", "Position text at top-center", "⬆️", "Text Position"),
        PremiumOption("tp_top_right", "Top Right", "Position text at top-right corner", "↗️", "Text Position"),
        PremiumOption("tp_mid_left", "Middle Left", "Position text at middle-left", "⬅️", "Text Position"),
        PremiumOption("tp_mid_center", "Middle Center", "Position text at center of frame", "🎯", "Text Position"),
        PremiumOption("tp_mid_right", "Middle Right", "Position text at middle-right", "➡️", "Text Position"),
        PremiumOption("tp_bot_left", "Bottom Left", "Position text at bottom-left corner", "↙️", "Text Position"),
        PremiumOption("tp_bot_center", "Bottom Center", "Position text at bottom-center", "⬇️", "Text Position"),
        PremiumOption("tp_bot_right", "Bottom Right", "Position text at bottom-right corner", "↘️", "Text Position")
    )

    // ── Text Shadow Styles (5) ─────────────────────────────────
    val textShadows = listOf(
        PremiumOption("ts_drop", "Drop Shadow", "Standard drop shadow behind text", "🔲", "Text Shadow"),
        PremiumOption("ts_long", "Long Shadow", "Extended diagonal long shadow", "📏", "Text Shadow"),
        PremiumOption("ts_soft", "Soft Shadow", "Diffused soft glow shadow", "☁️", "Text Shadow"),
        PremiumOption("ts_hard", "Hard Shadow", "Sharp-edged hard shadow", "⬛", "Text Shadow"),
        PremiumOption("ts_neon_shadow", "Neon Shadow", "Colored neon glow shadow effect", "💜", "Text Shadow")
    )

    // ── Text Glow Colors (6) ──────────────────────────────────
    val textGlows = listOf(
        PremiumOption("tg_orange", "Orange Glow", "Warm orange neon glow", "🟠", "Text Glow"),
        PremiumOption("tg_cyan", "Cyan Glow", "Cool cyan neon glow", "🔵", "Text Glow"),
        PremiumOption("tg_pink", "Pink Glow", "Vibrant pink neon glow", "🩷", "Text Glow"),
        PremiumOption("tg_green", "Green Glow", "Electric green neon glow", "🟢", "Text Glow"),
        PremiumOption("tg_purple", "Purple Glow", "Royal purple neon glow", "🟣", "Text Glow"),
        PremiumOption("tg_gold", "Gold Glow", "Luxurious gold glow", "🟡", "Text Glow")
    )

    // ── Text Outline & Background (6) ─────────────────────────
    val textOutlineBg = listOf(
        SliderControl("to_outline_width", "Outline Width", "⬜", 0f, 10f, 0f, 0.5f),
        PremiumOption("tb_solid_bg", "Solid Background", "Solid color background box behind text", "⬛", "Text Background"),
        PremiumOption("tb_gradient_bg", "Gradient Background", "Gradient color background behind text", "🎨", "Text Background"),
        PremiumOption("tb_blur_bg", "Blur Background", "Blurred video background behind text", "🌫️", "Text Background"),
        PremiumOption("te_underline", "Underline", "Underline decoration beneath text", "📏", "Text Decoration"),
        PremiumOption("te_strikethrough", "Strikethrough", "Strikethrough line through text", "❌", "Text Decoration")
    )

    // ── Subtitle Styles (8) ────────────────────────────────────
    val subtitleStyles = listOf(
        PremiumOption("ss_standard", "Standard", "Clean standard subtitle format", "📝", "Subtitle Style"),
        PremiumOption("ss_movie", "Movie", "Hollywood movie theater subtitles", "🎬", "Subtitle Style"),
        PremiumOption("ss_news", "News", "News broadcast lower-third style", "📰", "Subtitle Style"),
        PremiumOption("ss_social", "Social Media", "Bold social media caption style", "📱", "Subtitle Style"),
        PremiumOption("ss_karaoke", "Karaoke", "Highlighted karaoke sing-along style", "🎤", "Subtitle Style"),
        PremiumOption("ss_neon", "Neon Subtitle", "Glowing neon-style subtitles", "💜", "Subtitle Style"),
        PremiumOption("ss_minimal", "Minimal", "Ultra-clean minimal subtitle", "✨", "Subtitle Style"),
        PremiumOption("ss_bold_impact", "Bold Impact", "Heavy bold impact statement text", "💥", "Subtitle Style")
    )

    // ── Additional Text Options (7) ────────────────────────────
    val additionalText = listOf(
        SliderControl("tsk_size", "Text Size", "🔤", 8f, 120f, 24f, 1f),
        SliderControl("tsk_opacity", "Text Opacity", "👁️", 0f, 1f, 1f, 0.05f),
        SliderControl("tsk_letter_spacing", "Letter Spacing", "↔️", 0f, 20f, 0f, 0.5f),
        SliderControl("tsk_line_height", "Line Height", "↕️", 0.5f, 3f, 1.2f, 0.1f),
        PremiumOption("tsk_all_caps", "ALL CAPS", "Force all uppercase letters", "🔠", "Text Style"),
        PremiumOption("tsk_small_caps", "Small Caps", "Small capital letters style", "🔡", "Text Style"),
        PremiumOption("tsk_auto_subtitle", "Auto Subtitle Timing", "Auto-sync subtitle timing to speech", "⏱️", "Subtitle Style")
    )

    val all get() = premiumFonts + textAnimations + textPositions + textShadows + textGlows + textOutlineBg.map { if (it is PremiumOption) it else PremiumOption((it as SliderControl).id, it.name, "Adjust ${it.name}", it.emoji, "Text Control") } + subtitleStyles + additionalText.map { if (it is PremiumOption) it else PremiumOption((it as SliderControl).id, it.name, "Adjust ${it.name}", it.emoji, "Text Control") }
}

// ═══════════════════════════════════════════════════════════════
//  4. TRANSITIONS (50+ options)
// ═══════════════════════════════════════════════════════════════

object Transitions {

    // ── Basic Transitions (9) ──────────────────────────────────
    val basic = listOf(
        PremiumOption("tr_crossfade", "Crossfade", "Smooth opacity crossfade blend", "🔀", "Basic Transition"),
        PremiumOption("tr_wipe_left", "Wipe Left", "Reveal from right to left wipe", "⬅️", "Basic Transition"),
        PremiumOption("tr_wipe_right", "Wipe Right", "Reveal from left to right wipe", "➡️", "Basic Transition"),
        PremiumOption("tr_wipe_up", "Wipe Up", "Reveal from bottom to top wipe", "⬆️", "Basic Transition"),
        PremiumOption("tr_wipe_down", "Wipe Down", "Reveal from top to bottom wipe", "⬇️", "Basic Transition"),
        PremiumOption("tr_dissolve", "Dissolve", "Pixel dissolve scatter transition", "💫", "Basic Transition"),
        PremiumOption("tr_fade_black", "Fade to Black", "Fade through black between clips", "⬛", "Basic Transition"),
        PremiumOption("tr_fade_white", "Fade to White", "Fade through white between clips", "⬜", "Basic Transition"),
        PremiumOption("tr_none_transition", "Hard Cut", "Instant hard cut no transition", "✂️", "Basic Transition")
    )

    // ── Creative Transitions (10) ──────────────────────────────
    val creative = listOf(
        PremiumOption("tr_zoom_in", "Zoom In", "Zoom into clip center transition", "🔍", "Creative Transition"),
        PremiumOption("tr_zoom_out", "Zoom Out", "Zoom out from clip center", "🔎", "Creative Transition"),
        PremiumOption("tr_spin", "Spin", "Rotating spin transition between clips", "🔄", "Creative Transition"),
        PremiumOption("tr_flip", "Flip", "3D flip card transition", "🔃", "Creative Transition"),
        PremiumOption("tr_cube", "Cube", "3D cube rotation transition", "🧊", "Creative Transition"),
        PremiumOption("tr_page_turn", "Page Turn", "Page turn book flip transition", "📄", "Creative Transition"),
        PremiumOption("tr_blur", "Blur", "Blur in/out transition", "🌫️", "Creative Transition"),
        PremiumOption("tr_pixelate", "Pixelate", "Pixelation mosaic transition", "🟩", "Creative Transition"),
        PremiumOption("tr_split", "Split", "Split screen vertical/horizontal divide", "↕️", "Creative Transition"),
        PremiumOption("tr_stretch", "Stretch", "Rubber band stretch transition", "↔️", "Creative Transition")
    )

    // ── Cinematic Transitions (6) ──────────────────────────────
    val cinematic = listOf(
        PremiumOption("tr_film_burn", "Film Burn", "Analog film burn light transition", "🔥", "Cinematic Transition"),
        PremiumOption("tr_light_leak", "Light Leak", "Light leak flash transition", "✨", "Cinematic Transition"),
        PremiumOption("tr_lens_flare", "Lens Flare", "Lens flare sweep transition", "☀️", "Cinematic Transition"),
        PremiumOption("tr_smoke", "Smoke", "Smoke cloud reveal transition", "💨", "Cinematic Transition"),
        PremiumOption("tr_water", "Water", "Water ripple dissolve transition", "🌊", "Cinematic Transition"),
        PremiumOption("tr_fire", "Fire", "Fire flame burst transition", "🔥", "Cinematic Transition")
    )

    // ── Glitch Transitions (5) ─────────────────────────────────
    val glitch = listOf(
        PremiumOption("tr_glitch_digital", "Digital Glitch", "Digital corruption transition", "📺", "Glitch Transition"),
        PremiumOption("tr_glitch_analog", "Analog Glitch", "VHS tracking distortion transition", "📼", "Glitch Transition"),
        PremiumOption("tr_rgb_split", "RGB Split", "Red-green-blue channel split", "🔴", "Glitch Transition"),
        PremiumOption("tr_scanlines", "Scan Lines", "CRT scanline sweep transition", "📡", "Glitch Transition"),
        PremiumOption("tr_static", "Static", "TV static noise transition", "📻", "Glitch Transition")
    )

    // ── Shape Reveals (6) ──────────────────────────────────────
    val shapeReveals = listOf(
        PremiumOption("tr_circle", "Circle Reveal", "Expanding circle mask reveal", "⭕", "Shape Transition"),
        PremiumOption("tr_diamond", "Diamond Reveal", "Diamond shape expanding reveal", "💎", "Shape Transition"),
        PremiumOption("tr_heart", "Heart Reveal", "Heart shape expanding reveal", "❤️", "Shape Transition"),
        PremiumOption("tr_star", "Star Reveal", "Star shape expanding reveal", "⭐", "Shape Transition"),
        PremiumOption("tr_hexagon", "Hexagon Reveal", "Hexagonal pattern reveal", "⬡", "Shape Transition"),
        PremiumOption("tr_triangle", "Triangle Reveal", "Triangular wipe reveal", "🔺", "Shape Transition")
    )

    // ── Professional Cuts (5) ──────────────────────────────────
    val professional = listOf(
        PremiumOption("tr_lcut", "L-Cut", "Audio leads video transition", "🎙️", "Pro Transition"),
        PremiumOption("tr_jcut", "J-Cut", "Video leads audio transition", "🎬", "Pro Transition"),
        PremiumOption("tr_match_cut", "Match Cut", "Matched action between clips", "🎯", "Pro Transition"),
        PremiumOption("tr_jump_cut", "Jump Cut", "Intentional jump cut discontinuity", "⚡", "Pro Transition"),
        PremiumOption("tr_smash_cut", "Smash Cut", "Abrupt dramatic scene change", "💥", "Pro Transition")
    )

    // ── Additional Transitions (10) ────────────────────────────
    val additional = listOf(
        PremiumOption("tr_whip_pan", "Whip Pan", "Fast motion blur pan transition", "💨", "Creative Transition"),
        PremiumOption("tr_swirl", "Swirl", "Swirling vortex transition", "🌀", "Creative Transition"),
        PremiumOption("tr_glitch_inout", "Glitch In/Out", "Glitch appear and disappear", "📺", "Glitch Transition"),
        PremiumOption("tr_color_flash", "Color Flash", "Colored flash bang transition", "⚡", "Cinematic Transition"),
        PremiumOption("tr_morph", "Morph", "Shape morphing between scenes", "🔮", "Creative Transition"),
        PremiumOption("tr_push_left", "Push Left", "New clip pushes old clip left", "⬅️", "Basic Transition"),
        PremiumOption("tr_push_right", "Push Right", "New clip pushes old clip right", "➡️", "Basic Transition"),
        PremiumOption("tr_iris", "Iris", "Iris open/close eye transition", "👁️", "Shape Transition"),
        PremiumOption("tr_clock_wipe", "Clock Wipe", "Clock hand sweep wipe transition", "🕐", "Basic Transition"),
        PremiumOption("tr_barn_door", "Barn Door", "Center split barn door open/close", "🚪", "Basic Transition")
    )

    val all get() = basic + creative + cinematic + glitch + shapeReveals + professional + additional
}

// ═══════════════════════════════════════════════════════════════
//  5. COLOR GRADING (65+ options)
// ═══════════════════════════════════════════════════════════════

object ColorGrading {

    // ── LUT Presets (20) ───────────────────────────────────────
    val lutPresets = listOf(
        PremiumOption("lut_teal_orange", "Teal & Orange", "Classic Hollywood teal and orange", "🎬", "LUT Preset"),
        PremiumOption("lut_day_for_night", "Day for Night", "Convert daytime to moonlit night", "🌙", "LUT Preset"),
        PremiumOption("lut_kodachrome", "Kodachrome", "Classic Kodachrome film stock look", "📸", "LUT Preset"),
        PremiumOption("lut_fuji_velvia", "Fuji Velvia", "Fuji Velvia vivid landscape film", "🏔️", "LUT Preset"),
        PremiumOption("lut_portra_400", "Portra 400", "Kodak Portra 400 portrait film", "👤", "LUT Preset"),
        PremiumOption("lut_tri_x", "Tri-X", "Kodak Tri-X high contrast B&W", "🖤", "LUT Preset"),
        PremiumOption("lut_cyberpunk", "Cyberpunk", "Neon-soaked cyberpunk aesthetic", "💜", "LUT Preset"),
        PremiumOption("lut_pastel_dream", "Pastel Dream", "Soft pastel color palette", "🌸", "LUT Preset"),
        PremiumOption("lut_muted_earth", "Muted Earth", "Earthy muted natural tones", "🌍", "LUT Preset"),
        PremiumOption("lut_arctic_blue", "Arctic Blue", "Cold arctic blue tone grade", "🧊", "LUT Preset"),
        PremiumOption("lut_golden_hour", "Golden Hour", "Warm golden hour sunlight", "🌅", "LUT Preset"),
        PremiumOption("lut_vintage_film", "Vintage Film", "Old film stock with green tint", "🎞️", "LUT Preset"),
        PremiumOption("lut_horror", "Horror", "Dark desaturated horror movie grade", "👻", "LUT Preset"),
        PremiumOption("lut_summer_glow", "Summer Glow", "Bright warm summer atmosphere", "☀️", "LUT Preset"),
        PremiumOption("lut_noir_bw", "Noir B&W", "High contrast black and white noir", "🖤", "LUT Preset"),
        PremiumOption("lut_cinematic_blue", "Cinematic Blue", "Cool blue cinematic blockbuster", "🔵", "LUT Preset"),
        PremiumOption("lut_autumn", "Autumn", "Warm autumn leaf color palette", "🍂", "LUT Preset"),
        PremiumOption("lut_retro_80s", "Retro 80s", "Neon-soaked 1980s retro look", "🕹️", "LUT Preset"),
        PremiumOption("lut_matte", "Matte", "Flat matte film emulation", "🪵", "LUT Preset"),
        PremiumOption("lut_dramatic", "Dramatic", "High contrast dramatic cinematic", "🎭", "LUT Preset")
    )

    // ── Manual Controls (15 sliders) ───────────────────────────
    val manualControls = listOf(
        SliderControl("cg_brightness", "Brightness", "☀️", -100f, 100f, 0f, 1f),
        SliderControl("cg_contrast", "Contrast", "🔲", -100f, 100f, 0f, 1f),
        SliderControl("cg_saturation", "Saturation", "🎨", -100f, 100f, 0f, 1f),
        SliderControl("cg_hue", "Hue Rotation", "🌈", -180f, 180f, 0f, 1f),
        SliderControl("cg_temperature", "Temperature", "🌡️", -100f, 100f, 0f, 1f),
        SliderControl("cg_tint", "Tint", "💜", -100f, 100f, 0f, 1f),
        SliderControl("cg_shadows", "Shadows", "🌑", -100f, 100f, 0f, 1f),
        SliderControl("cg_highlights", "Highlights", "☀️", -100f, 100f, 0f, 1f),
        SliderControl("cg_whites", "Whites", "⬜", -100f, 100f, 0f, 1f),
        SliderControl("cg_blacks", "Blacks", "⬛", -100f, 100f, 0f, 1f),
        SliderControl("cg_clarity", "Clarity", "🔍", -100f, 100f, 0f, 1f),
        SliderControl("cg_vibrance", "Vibrance", "💎", -100f, 100f, 0f, 1f),
        SliderControl("cg_dehaze", "Dehaze", "🌫️", -100f, 100f, 0f, 1f),
        SliderControl("cg_vignette_amount", "Vignette", "🔲", 0f, 100f, 0f, 1f),
        SliderControl("cg_grain_amount", "Film Grain", "🎞️", 0f, 100f, 0f, 1f)
    )

    // ── Color Wheels — Lift/Gamma/Gain (12 sliders) ────────────
    val colorWheels = listOf(
        SliderControl("cw_lift_r", "Lift Red", "🔴", -1f, 1f, 0f, 0.01f),
        SliderControl("cw_lift_g", "Lift Green", "🟢", -1f, 1f, 0f, 0.01f),
        SliderControl("cw_lift_b", "Lift Blue", "🔵", -1f, 1f, 0f, 0.01f),
        SliderControl("cw_lift_lum", "Lift Luminance", "⚪", -1f, 1f, 0f, 0.01f),
        SliderControl("cw_gamma_r", "Gamma Red", "🔴", 0.1f, 3f, 1f, 0.01f),
        SliderControl("cw_gamma_g", "Gamma Green", "🟢", 0.1f, 3f, 1f, 0.01f),
        SliderControl("cw_gamma_b", "Gamma Blue", "🔵", 0.1f, 3f, 1f, 0.01f),
        SliderControl("cw_gamma_lum", "Gamma Luminance", "⚪", 0.1f, 3f, 1f, 0.01f),
        SliderControl("cw_gain_r", "Gain Red", "🔴", 0f, 3f, 1f, 0.01f),
        SliderControl("cw_gain_g", "Gain Green", "🟢", 0f, 3f, 1f, 0.01f),
        SliderControl("cw_gain_b", "Gain Blue", "🔵", 0f, 3f, 1f, 0.01f),
        SliderControl("cw_gain_lum", "Gain Luminance", "⚪", 0f, 3f, 1f, 0.01f)
    )

    // ── Curves (4) ─────────────────────────────────────────────
    val curves = listOf(
        PremiumOption("cv_rgb", "RGB Curves", "Master RGB tone curve adjustment", "📊", "Curves"),
        PremiumOption("cv_red", "Red Channel Curve", "Red channel tone curve", "🔴", "Curves"),
        PremiumOption("cv_green", "Green Channel Curve", "Green channel tone curve", "🟢", "Curves"),
        PremiumOption("cv_blue", "Blue Channel Curve", "Blue channel tone curve", "🔵", "Curves")
    )

    // ── Additional Grading (10) ────────────────────────────────
    val additional = listOf(
        SliderControl("cg_fade_amount", "Fade Amount", "🌫️", 0f, 100f, 0f, 1f),
        SliderControl("cg_crushed_blacks", "Crushed Blacks", "⬛", 0f, 100f, 0f, 1f),
        SliderControl("cg_blown_whites", "Blown Whites", "⬜", 0f, 100f, 0f, 1f),
        SliderControl("cg_split_tone_balance", "Split Tone Balance", "⚖️", -100f, 100f, 0f, 1f),
        PremiumOption("cg_auto_color", "Auto Color Correct", "Smart automatic color correction", "🤖", "Color Grading"),
        PremiumOption("cg_auto_exposure", "Auto Exposure", "Smart exposure correction", "📸", "Color Grading"),
        PremiumOption("cg_match_reference", "Match Reference", "Match color to reference frame", "🎯", "Color Grading"),
        PremiumOption("cg_lut_blend", "LUT Blend Mode", "Adjust LUT intensity blending", "🎛️", "Color Grading"),
        SliderControl("cg_lut_intensity", "LUT Intensity", "🎨", 0f, 100f, 100f, 1f),
        PremiumOption("cg_scopes", "Show Scopes", "Display waveform/vectorscope overlay", "📊", "Color Grading")
    )

    val all get() = lutPresets + manualControls.map { PremiumOption(it.id, it.name, "Adjust ${it.name}", it.emoji, "Manual Control") } + colorWheels.map { PremiumOption(it.id, it.name, "Adjust ${it.name}", it.emoji, "Color Wheel") } + curves + additional.map { if (it is PremiumOption) it else PremiumOption((it as SliderControl).id, it.name, "Adjust ${it.name}", it.emoji, "Color Grading") }
}

// ═══════════════════════════════════════════════════════════════
//  6. EXPORT SETTINGS (40+ options)
// ═══════════════════════════════════════════════════════════════

object ExportSettings {

    // ── Resolution (6) ─────────────────────────────────────────
    val resolutions = listOf(
        PremiumOption("ex_360p", "360p", "SD 360p lightweight export", "📱", "Resolution"),
        PremiumOption("ex_480p", "480p", "SD 480p standard definition", "📱", "Resolution"),
        PremiumOption("ex_720p", "720p", "HD 720p high definition", "📺", "Resolution"),
        PremiumOption("ex_1080p", "1080p", "Full HD 1080p", "🖥️", "Resolution"),
        PremiumOption("ex_2k", "2K QHD", "2K Quad HD resolution", "🖥️", "Resolution"),
        PremiumOption("ex_4k", "4K UHD", "4K Ultra HD resolution", "🖥️", "Resolution")
    )

    // ── Frame Rate (5) ─────────────────────────────────────────
    val frameRates = listOf(
        PremiumOption("ex_24fps", "24 fps", "Cinematic film standard", "🎬", "Frame Rate"),
        PremiumOption("ex_25fps", "25 fps", "PAL broadcast standard", "📺", "Frame Rate"),
        PremiumOption("ex_30fps", "30 fps", "Standard web video", "🌐", "Frame Rate"),
        PremiumOption("ex_60fps", "60 fps", "Smooth high frame rate", "🎮", "Frame Rate"),
        PremiumOption("ex_120fps", "120 fps", "Ultra-smooth slow motion", "🐌", "Frame Rate")
    )

    // ── Video Codec (4) ────────────────────────────────────────
    val codecs = listOf(
        PremiumOption("ex_h264", "H.264 / AVC", "Universal compatibility codec", "📦", "Codec"),
        PremiumOption("ex_h265", "H.265 / HEVC", "High efficiency modern codec", "📦", "Codec"),
        PremiumOption("ex_vp9", "VP9", "Google open-source codec", "📦", "Codec"),
        PremiumOption("ex_av1", "AV1", "Next-gen open codec best compression", "📦", "Codec")
    )

    // ── Encoding Profile (4) ───────────────────────────────────
    val profiles = listOf(
        PremiumOption("ex_profile_baseline", "Baseline", "Low complexity baseline profile", "⚙️", "Profile"),
        PremiumOption("ex_profile_main", "Main", "Balanced main profile", "⚙️", "Profile"),
        PremiumOption("ex_profile_high", "High", "High quality profile", "⚙️", "Profile"),
        PremiumOption("ex_profile_high10", "High 10-bit", "10-bit color depth high profile", "⚙️", "Profile")
    )

    // ── Bitrate (5) ────────────────────────────────────────────
    val bitrates = listOf(
        PremiumOption("ex_br_low", "Low (2 Mbps)", "Small file size, basic quality", "📉", "Bitrate"),
        PremiumOption("ex_br_medium", "Medium (8 Mbps)", "Balanced size and quality", "📊", "Bitrate"),
        PremiumOption("ex_br_high", "High (20 Mbps", "High quality larger file", "📈", "Bitrate"),
        PremiumOption("ex_br_ultra", "Ultra (50 Mbps)", "Maximum quality large file", "🚀", "Bitrate"),
        PremiumOption("ex_br_custom", "Custom Bitrate", "Set custom bitrate value", "🎛️", "Bitrate")
    )

    // ── Container Format (4) ───────────────────────────────────
    val containers = listOf(
        PremiumOption("ex_mp4", "MP4", "Universal MP4 container", "📦", "Container"),
        PremiumOption("ex_mov", "MOV", "Apple QuickTime MOV", "📦", "Container"),
        PremiumOption("ex_webm", "WebM", "Web-optimized WebM container", "📦", "Container"),
        PremiumOption("ex_mkv", "MKV", "Matroska MKV container", "📦", "Container")
    )

    // ── Audio Export Settings (8) ──────────────────────────────
    val audioExport = listOf(
        PremiumOption("ex_aac", "AAC", "Advanced Audio Coding", "🔊", "Audio Codec"),
        PremiumOption("ex_mp3_audio", "MP3", "MPEG Layer 3 audio", "🔊", "Audio Codec"),
        PremiumOption("ex_opus", "Opus", "Opus open-source audio codec", "🔊", "Audio Codec"),
        PremiumOption("ex_pcm", "PCM / WAV", "Uncompressed PCM audio", "🔊", "Audio Codec"),
        SliderControl("ex_audio_bitrate", "Audio Bitrate", "🎵", 64f, 320f, 128f, 32f),
        SliderControl("ex_audio_sample_rate", "Sample Rate", "📊", 22050f, 96000f, 44100f, 11025f),
        SelectorOption("ex_audio_channels", "Audio Channels", "🔈", listOf("Mono", "Stereo", "5.1 Surround")),
        PremiumOption("ex_audio_normalize", "Normalize Audio", "Auto-normalize audio on export", "📊", "Audio Export")
    )

    // ── HDR & Color Space (5) ──────────────────────────────────
    val hdrColorSpace = listOf(
        PremiumOption("ex_hdr_hlg", "HLG HDR", "Hybrid Log-Gamma HDR output", "🌈", "HDR"),
        PremiumOption("ex_hdr_pq", "PQ HDR", "Perceptual Quantizer HDR10 output", "🌈", "HDR"),
        PremiumOption("ex_sdr", "SDR", "Standard Dynamic Range output", "📺", "HDR"),
        PremiumOption("ex_cs_rec709", "Rec. 709", "Standard HD color space", "🎨", "Color Space"),
        PremiumOption("ex_cs_rec2020", "Rec. 2020", "Wide color gamut UHD space", "🎨", "Color Space")
    )

    // ── Additional Export (5) ──────────────────────────────────
    val additional = listOf(
        PremiumOption("ex_hardware_accel", "Hardware Acceleration", "GPU-accelerated encoding", "⚡", "Export"),
        PremiumOption("ex_two_pass", "Two-Pass Encoding", "Two-pass encoding for better quality", "🔄", "Export"),
        PremiumOption("ex_faststart", "Fast Start (moov)", "Move moov atom for web streaming", "🌐", "Export"),
        PremiumOption("ex_gif_export", "GIF Export", "Export as animated GIF", "🖼️", "Export"),
        PremiumOption("ex_thumbnail_strip", "Thumbnail Strip", "Generate video thumbnail sprite sheet", "🖼️", "Export")
    )

    val all get() = resolutions + frameRates + codecs + profiles + bitrates + containers + audioExport.map { when (it) { is PremiumOption -> it; is SliderControl -> PremiumOption(it.id, it.name, "Adjust ${it.name}", it.emoji, "Audio Export"); else -> PremiumOption((it as SelectorOption).id, it.name, "Select ${it.name}", it.emoji, "Audio Export") } } + hdrColorSpace + additional
}

// ═══════════════════════════════════════════════════════════════
//  7. AI FEATURES (40+ options)
// ═══════════════════════════════════════════════════════════════

object AIFeatures {

    // ── Smart Auto Edit (8) ───────────────────────────────────────
    val autoEdit = listOf(
        PremiumOption("ai_auto_highlight", "Auto Highlight Reel", "selects best moments for highlight reel", "🌟", "Smart Auto Edit"),
        PremiumOption("ai_auto_trim_silence", "Auto Trim Silence", "detects and removes silent sections", "🔇", "Smart Auto Edit"),
        PremiumOption("ai_auto_pacing", "Auto Pacing", "adjusts clip timing for optimal pacing", "⏱️", "Smart Auto Edit"),
        PremiumOption("ai_scene_detect", "Scene Detection", "auto-detects scene changes", "🎬", "Smart Auto Edit"),
        PremiumOption("ai_smart_crop", "Smart Crop", "auto-crops to focus on subjects", "📐", "Smart Auto Edit"),
        PremiumOption("ai_auto_stabilize", "Auto Stabilize", "Smart video stabilization", "📹", "Smart Auto Edit"),
        PremiumOption("ai_auto_color_match", "Auto Color Match", "matches color across clips", "🎨", "Smart Auto Edit"),
        PremiumOption("ai_smart_transition", "Smart Transition", "picks best transitions between clips", "🔀", "Smart Auto Edit")
    )

    // ── Smart Captions (8) ───────────────────────────────────────
    val captions = listOf(
        PremiumOption("ai_auto_caption", "Auto Caption", "generates captions from speech", "💬", "Smart Captions"),
        PremiumOption("ai_translate_caption", "Translate Captions", "translates captions to 50+ languages", "🌍", "Smart Captions"),
        PremiumOption("ai_caption_style", "Caption Styling", "styles captions for platform", "✨", "Smart Captions"),
        PremiumOption("ai_word_highlight", "Word Highlight", "highlights spoken words in real-time", "🔦", "Smart Captions"),
        PremiumOption("ai_karaoke_mode", "Karaoke Mode", "AI-synced karaoke-style lyrics", "🎤", "Smart Captions"),
        PremiumOption("ai_subtitle_timing", "Subtitle Timing AI", "AI auto-adjusts subtitle duration", "⏱️", "Smart Captions"),
        PremiumOption("ai_speaker_id", "Speaker Identification", "identifies different speakers", "👥", "Smart Captions"),
        PremiumOption("ai_emotion_caption", "Emotion Captions", "adds emotion-aware caption styling", "😊", "Smart Captions")
    )

    // ── Smart Background (6) ──────────────────────────────────────
    val background = listOf(
        PremiumOption("ai_bg_remove", "Background Remove", "removes video background", "✂️", "Smart Background"),
        PremiumOption("ai_bg_blur", "Background Blur", "blurs background keeping subject sharp", "🌫️", "Smart Background"),
        PremiumOption("ai_bg_replace", "Background Replace", "replaces background with image/video", "🖼️", "Smart Background"),
        PremiumOption("ai_bg_green_screen", "Virtual Green Screen", "simulates green screen effect", "🟩", "Smart Background"),
        PremiumOption("ai_bg_bokeh", "Smart Bokeh", "creates depth-based bokeh blur", "🔵", "Smart Background"),
        PremiumOption("ai_bg_night_mode", "Smart Night Mode", "brightens low-light footage", "🌙", "Smart Background")
    )

    // ── Smart Voice (6) ───────────────────────────────────────────
    val voice = listOf(
        PremiumOption("ai_tts_narration", "TTS Narration", "AI text-to-speech narration voice", "🗣️", "Smart Voice"),
        PremiumOption("ai_voice_clone", "Voice Clone", "clones a voice from sample audio", "🎭", "Smart Voice"),
        PremiumOption("ai_voice_isolate", "Voice Isolate", "separates voice from background noise", "🎙️", "Smart Voice"),
        PremiumOption("ai_auto_duck", "Auto Duck", "lowers music when speech detected", "🔉", "Smart Voice"),
        PremiumOption("ai_voice_enhance", "Voice Enhance", "enhances voice clarity and warmth", "✨", "Smart Voice"),
        PremiumOption("ai_transcribe", "Transcribe", "AI full speech-to-text transcription", "📝", "Smart Voice")
    )

    // ── Smart Enhancement (7) ─────────────────────────────────────
    val enhancement = listOf(
        PremiumOption("ai_super_res", "Super Resolution", "upscales video resolution", "🔍", "Smart Enhancement"),
        PremiumOption("ai_denoise", "Smart Denoise", "removes video noise and grain", "🧹", "Smart Enhancement"),
        PremiumOption("ai_sharpen", "Smart Sharpen", "intelligent sharpening", "🔪", "Smart Enhancement"),
        PremiumOption("ai_hdr_enhance", "HDR Enhance", "creates HDR effect from SDR", "🌈", "Smart Enhancement"),
        PremiumOption("ai_slow_motion", "Smart Slow Motion", "generates intermediate frames for slow-mo", "🐌", "Smart Enhancement"),
        PremiumOption("ai_deinterlace", "Deinterlace", "deinterlaces interlaced footage", "📺", "Smart Enhancement"),
        PremiumOption("ai_frame_interp", "Frame Interpolation", "generates smooth intermediate frames", "🎞️", "Smart Enhancement")
    )

    // ── Smart Art & Creative (6) ──────────────────────────────────
    val artCreative = listOf(
        PremiumOption("ai_style_transfer", "Style Transfer", "Apply art style from reference image", "🎨", "Smart Art"),
        PremiumOption("ai_anime_style", "Anime Style", "Convert footage to anime art style", "🌸", "Smart Art"),
        PremiumOption("ai_painting_style", "Painting Style", "Convert footage to oil painting", "🖼️", "Smart Art"),
        PremiumOption("ai_sketch_style", "Sketch Style", "Convert footage to pencil sketch", "✏️", "Smart Art"),
        PremiumOption("ai_cartoon_style", "Cartoon Style", "Convert footage to cartoon style", "🎭", "Smart Art"),
        PremiumOption("ai_depth_map", "Depth Map", "generates depth map from footage", "🗺️", "Smart Art")
    )

    val all get() = autoEdit + captions + background + voice + enhancement + artCreative
}

// ═══════════════════════════════════════════════════════════════
//  8. STICKERS & OVERLAYS (50+ options)
// ═══════════════════════════════════════════════════════════════

object StickersOverlays {

    // ── Emoji Stickers (20) ────────────────────────────────────
    val emojiStickers = listOf(
        PremiumOption("stk_fire", "Fire", "Fire emoji sticker", "🔥", "Emoji Sticker"),
        PremiumOption("stk_heart", "Heart", "Red heart emoji sticker", "❤️", "Emoji Sticker"),
        PremiumOption("stk_star", "Star", "Gold star emoji sticker", "⭐", "Emoji Sticker"),
        PremiumOption("stk_thumbsup", "Thumbs Up", "Thumbs up approval sticker", "👍", "Emoji Sticker"),
        PremiumOption("stk_laugh", "Laugh", "Laughing crying emoji sticker", "😂", "Emoji Sticker"),
        PremiumOption("skt_clap", "Clap", "Clapping hands sticker", "👏", "Emoji Sticker"),
        PremiumOption("stk_rocket", "Rocket", "Rocket launch sticker", "🚀", "Emoji Sticker"),
        PremiumOption("stk_crown", "Crown", "Royal crown sticker", "👑", "Emoji Sticker"),
        PremiumOption("skt_lightning", "Lightning", "Lightning bolt sticker", "⚡", "Emoji Sticker"),
        PremiumOption("stk_party", "Party", "Party popper celebration sticker", "🎉", "Emoji Sticker"),
        PremiumOption("stk_100", "100%", "100 percent score sticker", "💯", "Emoji Sticker"),
        PremiumOption("stk_eyes", "Eyes", "Shifty eyes sticker", "👀", "Emoji Sticker"),
        PremiumOption("stk_mind_blown", "Mind Blown", "Mind blown exploding head sticker", "🤯", "Emoji Sticker"),
        PremiumOption("stk_cool", "Cool", "Sunglasses cool face sticker", "😎", "Emoji Sticker"),
        PremiumOption("stk_gem", "Gem", "Gem stone sticker", "💎", "Emoji Sticker"),
        PremiumOption("stk_trophy", "Trophy", "Winner trophy sticker", "🏆", "Emoji Sticker"),
        PremiumOption("stk_flame_heart", "Flame Heart", "Heart on fire sticker", "❤️‍🔥", "Emoji Sticker"),
        PremiumOption("skt_rainbow", "Rainbow", "Rainbow arc sticker", "🌈", "Emoji Sticker"),
        PremiumOption("stk_sparkles", "Sparkles", "Sparkle glitter sticker", "✨", "Emoji Sticker"),
        PremiumOption("stk_music_note", "Music Note", "Musical note sticker", "🎵", "Emoji Sticker")
    )

    // ── Shape Overlays (10) ────────────────────────────────────
    val shapeOverlays = listOf(
        PremiumOption("sh_circle", "Circle Frame", "Circular shape overlay frame", "⭕", "Shape Overlay"),
        PremiumOption("sh_square", "Square Frame", "Square shape overlay frame", "⬜", "Shape Overlay"),
        PremiumOption("sh_triangle", "Triangle", "Triangle shape overlay", "🔺", "Shape Overlay"),
        PremiumOption("sh_diamond", "Diamond", "Diamond shape overlay", "💎", "Shape Overlay"),
        PremiumOption("sh_hexagon", "Hexagon", "Hexagonal shape overlay", "⬡", "Shape Overlay"),
        PremiumOption("sh_heart_shape", "Heart Shape", "Heart shape overlay", "❤️", "Shape Overlay"),
        PremiumOption("sh_star_shape", "Star Shape", "Star shape overlay", "⭐", "Shape Overlay"),
        PremiumOption("sh_arrow", "Arrow Pointer", "Arrow shape indicator overlay", "➡️", "Shape Overlay"),
        PremiumOption("sh_speech_bubble", "Speech Bubble", "Speech bubble overlay", "💬", "Shape Overlay"),
        PremiumOption("sh_badge", "Badge", "Badge/ribbon shape overlay", "🏷️", "Shape Overlay")
    )

    // ── Watermark (8) ──────────────────────────────────────────
    val watermarks = listOf(
        PremiumOption("wm_text_watermark", "Text Watermark", "Custom text watermark overlay", "💧", "Watermark"),
        PremiumOption("wm_logo_watermark", "Logo Watermark", "Image logo watermark overlay", "🏷️", "Watermark"),
        PremiumOption("wm_opacity", "Watermark Opacity", "Adjust watermark transparency", "👁️", "Watermark"),
        SliderControl("wm_position_x", "Position X", "↔️", 0f, 1f, 0.9f, 0.01f),
        SliderControl("wm_position_y", "Position Y", "↕️", 0f, 1f, 0.1f, 0.01f),
        SliderControl("wm_scale", "Watermark Scale", "📐", 0.1f, 2f, 0.5f, 0.05f),
        PremiumOption("wm_shadow", "Watermark Shadow", "Add drop shadow to watermark", "🔲", "Watermark"),
        PremiumOption("wm_auto_brand", "Auto Brand", "Auto-apply brand watermark to all exports", "✨", "Watermark")
    )

    // ── Picture-in-Picture (7) ─────────────────────────────────
    val pip = listOf(
        PremiumOption("pip_top_left", "PiP Top Left", "Picture-in-picture top-left position", "↖️", "PiP"),
        PremiumOption("pip_top_right", "PiP Top Right", "Picture-in-picture top-right position", "↗️", "PiP"),
        PremiumOption("pip_bottom_left", "PiP Bottom Left", "Picture-in-picture bottom-left position", "↙️", "PiP"),
        PremiumOption("pip_bottom_right", "PiP Bottom Right", "Picture-in-picture bottom-right position", "↘️", "PiP"),
        SliderControl("pip_size", "PiP Size", "📐", 0.1f, 0.5f, 0.25f, 0.01f),
        SliderControl("pip_border_radius", "PiP Border Radius", "🔘", 0f, 50f, 12f, 1f),
        PremiumOption("pip_pip_shadow", "PiP Shadow", "Drop shadow on PiP window", "🔲", "PiP")
    )

    // ── Split Screen (6) ───────────────────────────────────────
    val splitScreen = listOf(
        PremiumOption("ss_vertical_2", "Vertical 2-Way", "Vertical split 2 clips side by side", "↕️", "Split Screen"),
        PremiumOption("ss_horizontal_2", "Horizontal 2-Way", "Horizontal split 2 clips top/bottom", "↔️", "Split Screen"),
        PremiumOption("ss_3way_vertical", "3-Way Vertical", "Three clips in vertical columns", "📊", "Split Screen"),
        PremiumOption("ss_4way_grid", "4-Way Grid", "Four clips in 2x2 grid", "🔲", "Split Screen"),
        PremiumOption("ss_pip_overlay", "PiP Overlay Mode", "One clip overlaid on another", "🖼️", "Split Screen"),
        PremiumOption("ss_freeform", "Freeform Split", "Custom split screen layout", "🎨", "Split Screen")
    )

    // ── Text Overlay Presets (10) ────────────────────────────────
    val textOverlayPresets = listOf(
        PremiumOption("to_lower_third", "Lower Third", "Professional lower-third name bar", "📺", "Text Overlay"),
        PremiumOption("to_title_card", "Title Card", "Full-screen title card overlay", "🎬", "Text Overlay"),
        PremiumOption("to_end_credits", "End Credits", "Scrolling end credits overlay", "📜", "Text Overlay"),
        PremiumOption("to_chyron", "Chyron", "News-style chyron text bar", "📰", "Text Overlay"),
        PremiumOption("to_callout", "Callout Bubble", "Animated callout annotation bubble", "💬", "Text Overlay"),
        PremiumOption("to_countdown", "Countdown Timer", "Animated countdown timer overlay", "⏱️", "Text Overlay"),
        PremiumOption("to_progress_bar", "Progress Bar", "Animated progress bar overlay", "📊", "Text Overlay"),
        PremiumOption("to_social_handle", "Social Handle", "Social media username tag overlay", "📱", "Text Overlay"),
        PremiumOption("to_like_subscribe", "Like & Subscribe", "YouTube-style like/subscribe CTA", "👍", "Text Overlay"),
        PremiumOption("to_watermark_text", "Animated Watermark", "Animated pulsing watermark text", "💧", "Text Overlay")
    )

    val all get() = emojiStickers + shapeOverlays + watermarks.map { if (it is PremiumOption) it else PremiumOption((it as SliderControl).id, it.name, "Adjust ${it.name}", it.emoji, "Watermark") } + pip.map { if (it is PremiumOption) it else PremiumOption((it as SliderControl).id, it.name, "Adjust ${it.name}", it.emoji, "PiP") } + splitScreen + textOverlayPresets
}

// ═══════════════════════════════════════════════════════════════
//  9. PROJECT SETTINGS (35+ options)
// ═══════════════════════════════════════════════════════════════

object ProjectSettings {

    // ── General Settings (10) ──────────────────────────────────
    val general = listOf(
        PremiumOption("ps_project_name", "Project Name", "Set custom project name", "📝", "General"),
        PremiumOption("ps_auto_save", "Auto Save", "Enable periodic auto-save", "💾", "General"),
        SelectorOption("ps_autosave_interval", "Auto-Save Interval", "⏱️", listOf("30s", "1min", "2min", "5min", "10min")),
        PremiumOption("ps_undo_levels", "Undo Levels", "Number of undo steps available", "↩️", "General"),
        SelectorOption("ps_undo_count", "Undo Count", "↩️", listOf("10", "25", "50", "100", "Unlimited")),
        PremiumOption("ps_snap_to_grid", "Snap to Grid", "Snap timeline elements to grid", "🧲", "General"),
        PremiumOption("ps_magnetic_timeline", "Magnetic Timeline", "Clips snap together magnetically", "🧲", "General"),
        PremiumOption("ps_show_waveform", "Show Audio Waveform", "Display audio waveform on timeline", "📊", "General"),
        PremiumOption("ps_show_keyframes", "Show Keyframes", "Display animation keyframes on timeline", "🔑", "General"),
        PremiumOption("ps_timeline_zoom", "Timeline Zoom", "Adjust timeline zoom level", "🔍", "General")
    )

    // ── Timeline Settings (8) ──────────────────────────────────
    val timeline = listOf(
        SelectorOption("pt_default_duration", "Default Clip Duration", "⏱️", listOf("3s", "5s", "10s", "15s", "30s")),
        PremiumOption("pt_ripple_edit", "Ripple Edit", "Ripple timeline on trim/delete", "🌊", "Timeline"),
        PremiumOption("pt_roll_edit", "Roll Edit", "Roll edit between adjacent clips", "🔄", "Timeline"),
        PremiumOption("pt_slip_edit", "Slip Edit", "Slip clip content within bounds", "↔️", "Timeline"),
        PremiumOption("pt_slide_edit", "Slide Edit", "Slide clip position preserving neighbors", "↕️", "Timeline"),
        SelectorOption("pt_track_height", "Track Height", "📏", listOf("Compact", "Normal", "Tall", "Extra Tall")),
        PremiumOption("pt_auto_add_transition", "Auto Add Transition", "Auto-add default transition on join", "🔀", "Timeline"),
        PremiumOption("pt_transition_default", "Default Transition", "Set default transition type", "✨", "Timeline")
    )

    // ── Preview Settings (9) ───────────────────────────────────
    val preview = listOf(
        SelectorOption("pv_preview_quality", "Preview Quality", "🖥️", listOf("Quarter", "Half", "Full", "Auto")),
        PremiumOption("pv_proxy_editing", "Proxy Editing", "Use low-res proxy for smooth editing", "📹", "Preview"),
        PremiumOption("pv_safe_areas", "Show Safe Areas", "Display title/action safe area guides", "🔲", "Preview"),
        PremiumOption("pv_grid_overlay", "Grid Overlay", "Show rule-of-thirds grid", "📐", "Preview"),
        PremiumOption("pv_histogram", "Show Histogram", "Display luminance histogram", "📊", "Preview"),
        PremiumOption("pv_vectorscope", "Show Vectorscope", "Display color vectorscope", "🎯", "Preview"),
        PremiumOption("pv_waveform_monitor", "Waveform Monitor", "Display video waveform monitor", "📈", "Preview"),
        PremiumOption("pv_focus_peaking", "Focus Peaking", "Highlight in-focus areas", "🔍", "Preview"),
        PremiumOption("pv_zebra_stripes", "Zebra Stripes", "Show overexposure zebra warnings", "🦓", "Preview")
    )

    // ── Misc Settings (8) ──────────────────────────────────────
    val misc = listOf(
        PremiumOption("ps_gpu_accel", "GPU Acceleration", "Use GPU for real-time effects", "⚡", "Performance"),
        PremiumOption("ps_render_cache", "Render Cache", "Pre-render effects for smooth playback", "💨", "Performance"),
        SelectorOption("ps_cache_size", "Cache Size", "💾", listOf("1 GB", "2 GB", "5 GB", "10 GB", "Unlimited")),
        PremiumOption("ps_background_render", "Background Render", "Render while editing", "🔄", "Performance"),
        PremiumOption("ps_export_preset_save", "Save Export Preset", "Save current export settings as preset", "💾", "Export"),
        PremiumOption("ps_export_preset_load", "Load Export Preset", "Load saved export preset", "📂", "Export"),
        PremiumOption("ps_cloud_backup", "Cloud Backup", "Auto-backup project to cloud", "☁️", "Backup"),
        PremiumOption("ps_collaboration", "Collaboration", "Enable real-time collaboration mode", "👥", "Collaboration")
    )

    // ── Keyboard Shortcuts (5) ─────────────────────────────────
    val shortcuts = listOf(
        PremiumOption("ks_custom_shortcuts", "Custom Shortcuts", "Remap keyboard shortcuts", "⌨️", "Shortcuts"),
        PremiumOption("ks_gesture_controls", "Gesture Controls", "Touch gesture customization", "👆", "Shortcuts"),
        PremiumOption("ks_quick_export", "Quick Export", "One-tap export with last settings", "⚡", "Shortcuts"),
        PremiumOption("ks_quick_save", "Quick Save", "Instant project save shortcut", "💾", "Shortcuts"),
        PremiumOption("ks_double_tap_split", "Double-Tap Split", "Double-tap timeline to split clip", "✂️", "Shortcuts")
    )

    val all get() = general.map { if (it is PremiumOption) it else PremiumOption((it as SelectorOption).id, it.name, "Select ${it.name}", it.emoji, "General") } + timeline.map { if (it is PremiumOption) it else PremiumOption((it as SelectorOption).id, it.name, "Select ${it.name}", it.emoji, "Timeline") } + preview.map { if (it is PremiumOption) it else PremiumOption((it as SelectorOption).id, it.name, "Select ${it.name}", it.emoji, "Preview") } + misc.map { if (it is PremiumOption) it else PremiumOption((it as SelectorOption).id, it.name, "Select ${it.name}", it.emoji, "Performance") } + shortcuts
}

// ═══════════════════════════════════════════════════════════════
//  MASTER REGISTRY
// ═══════════════════════════════════════════════════════════════

object PremiumRegistry {

    data class Category(
        val id: String,
        val name: String,
        val emoji: String,
        val description: String,
        val optionCount: Int
    )

    val categories = listOf(
        Category("video_effects", "Video Effects", "🎬", "Color filters, speed ramps, visual FX, particles", VideoEffects.all.size),
        Category("audio_tools", "Audio Tools", "🎵", "EQ, reverb, voice changer, background music", AudioTools.all.size),
        Category("text_typography", "Text & Typography", "🔤", "Fonts, animations, positions, subtitles", TextTypography.all.size),
        Category("transitions", "Transitions", "🔀", "Basic, creative, cinematic, glitch, shape transitions", Transitions.all.size),
        Category("color_grading", "Color Grading", "🎨", "LUTs, manual controls, color wheels, curves", ColorGrading.all.size),
        Category("export_settings", "Export Settings", "📤", "Resolution, codec, bitrate, HDR, audio export", ExportSettings.all.size),
        Category("ai_features", "Smart Features", "🤖", "Auto-edit, captions, background, voice, art", AIFeatures.all.size),
        Category("stickers_overlays", "Stickers & Overlays", "😄", "Emoji, shapes, watermark, PiP, split screen", StickersOverlays.all.size),
        Category("project_settings", "Project Settings", "⚙️", "Timeline, preview, performance, backup", ProjectSettings.all.size)
    )

    val totalOptions: Int
        get() = categories.sumOf { it.optionCount }

    fun getAllOptions(): List<PremiumOption> = VideoEffects.all + AudioTools.all + TextTypography.all + Transitions.all + ColorGrading.all + ExportSettings.all + AIFeatures.all + StickersOverlays.all + ProjectSettings.all

    fun getOptionsForCategory(categoryId: String): List<PremiumOption> = when (categoryId) {
        "video_effects" -> VideoEffects.all
        "audio_tools" -> AudioTools.all
        "text_typography" -> TextTypography.all
        "transitions" -> Transitions.all
        "color_grading" -> ColorGrading.all
        "export_settings" -> ExportSettings.all
        "ai_features" -> AIFeatures.all
        "stickers_overlays" -> StickersOverlays.all
        "project_settings" -> ProjectSettings.all
        else -> emptyList()
    }

    fun getSliderControlsForCategory(categoryId: String): List<SliderControl> = when (categoryId) {
        "audio_tools" -> AudioTools.eqBands + AudioTools.audioSliders
        "text_typography" -> TextTypography.textOutlineBg.filterIsInstance<SliderControl>() + TextTypography.additionalText.filterIsInstance<SliderControl>()
        "color_grading" -> ColorGrading.manualControls + ColorGrading.colorWheels + ColorGrading.additional.filterIsInstance<SliderControl>()
        "stickers_overlays" -> StickersOverlays.watermarks.filterIsInstance<SliderControl>() + StickersOverlays.pip.filterIsInstance<SliderControl>()
        "project_settings" -> emptyList()
        else -> emptyList()
    }
}
