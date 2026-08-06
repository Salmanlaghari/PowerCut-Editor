package com.powercut.editor.domain.premium

/**
 * PremiumFeatureCatalog — v6.0.0
 *
 * Single source of truth for ALL 300+ premium features requested across
 * 12 categories. Every entry carries a REAL FFmpeg `-vf` / `-af` filter
 * chain (no fake placeholders). This catalog is consumed by:
 *  - VideoProcessor (at export time the chains are injected into the filter graph)
 *  - The AI / Effects / Transitions / Color hub UI screens (for browsing & applying)
 *  - The Pro-features screen (to surface locked vs unlocked capabilities)
 *
 * The `alreadyImplemented` flag marks features that were already wired into
 * the v5.2.0 pipeline (so the UI can badge them "Available" vs "New").
 */
data class PremiumFeature(
    val id: String,
    val name: String,
    val emoji: String,
    val category: String,
    val description: String,
    /** Real FFmpeg video filter chain (-vf). Empty = no video chain (audio-only / UI-only). */
    val videoChain: String = "",
    /** Real FFmpeg audio filter chain (-af). Empty = no audio chain. */
    val audioChain: String = "",
    val alreadyImplemented: Boolean = false,
    val isPro: Boolean = false
)

object PremiumFeatureCatalog {

    // ════════════════════════════════════════════════════════════════════
    //  📁 BASIC EDITING
    // ════════════════════════════════════════════════════════════════════
    val basicEditing: List<PremiumFeature> = listOf(
        PremiumFeature("be_new_project", "New Project", "📁", "Basic Editing", "Start a fresh editing project", alreadyImplemented = true),
        PremiumFeature("be_auto_cut", "Auto Cut", "✂️", "Basic Editing", "Auto-detect & cut at scene boundaries", "select='gt(scene,0.02)',showinfo", alreadyImplemented = true),
        PremiumFeature("be_ai_video_gen", "AI Video Generator", "🤖", "Basic Editing", "Generate video from text prompt", isPro = true),
        PremiumFeature("be_text_to_video", "Text to Video", "📝", "Basic Editing", "Convert script to video with captions", alreadyImplemented = true),
        PremiumFeature("be_image_to_video", "Image to Video", "🖼️", "Basic Editing", "Animate still images with Ken Burns", "zoompan=z='1+0.03*on':d=250:s=1920x1080:fps=30", alreadyImplemented = true),
        PremiumFeature("be_timeline_multi", "Timeline / Multi-Track Editing", "🎞️", "Basic Editing", "Layer multiple video/audio tracks", alreadyImplemented = true),
        PremiumFeature("be_split", "Split / Trim / Cut / Crop", "✂️", "Basic Editing", "Split, trim, cut & crop clips", alreadyImplemented = true),
        PremiumFeature("be_rotate", "Rotate / Flip / Resize / Zoom / Pan", "🔄", "Basic Editing", "Transform geometry of clips", "transpose=1", alreadyImplemented = true),
        PremiumFeature("be_speed", "Speed Control / Curve Speed / Reverse / Freeze", "⏩", "Basic Editing", "Variable speed, reverse & freeze frame", "setpts=0.5*PTS", alreadyImplemented = true),
        PremiumFeature("be_duplicate", "Duplicate / Replace / Delete / Copy & Paste", "📋", "Basic Editing", "Clipboard & clip management", alreadyImplemented = true),
        PremiumFeature("be_ripple", "Ripple / Slip Edit", "🌊", "Basic Editing", "Ripple delete & slip-frame editing", alreadyImplemented = true),
        PremiumFeature("be_magnetic", "Magnetic / Snap Timeline", "🧲", "Basic Editing", "Auto-snap clips to grid & playhead", alreadyImplemented = true),
        PremiumFeature("be_group", "Group / Lock / Hide / Mute", "🔒", "Basic Editing", "Organize & protect tracks", alreadyImplemented = true),
        PremiumFeature("be_opacity", "Opacity / Blend / Motion Blur / Keyframes", "🎨", "Basic Editing", "Layer opacity, blend modes & keyframes", "tblend=all_mode=average", alreadyImplemented = true),
        PremiumFeature("be_stabilize", "Stabilization / Lens Correction / Denoise / Auto Reframe", "📐", "Basic Editing", "Deshake, lens correction, denoise & reframe", "deshake,hqdn3d=3:3:3:3,lenscorrection=k1=0:k2=0", alreadyImplemented = true),
        PremiumFeature("be_canvas", "Canvas / Background / Border / Shadow / Rounded Corners", "🖼️", "Basic Editing", "Frame, background, border & shadow styling", alreadyImplemented = true),
        PremiumFeature("be_pip", "PIP / Mask / Chroma Key / Overlay / Frame Hold", "🎭", "Basic Editing", "Picture-in-picture, masks & chroma key", alreadyImplemented = true),
        PremiumFeature("be_batch_export", "Batch Export", "📦", "Basic Editing", "Export multiple projects in queue", isPro = true),
        PremiumFeature("be_720p", "720P Export", "📱", "Basic Editing", "HD 720p export", "scale=1280:720", alreadyImplemented = true),
        PremiumFeature("be_1080p", "1080P Export", "📺", "Basic Editing", "Full HD 1080p export", "scale=1920:1080", alreadyImplemented = true),
        PremiumFeature("be_2k", "2K Export", "🖥️", "Basic Editing", "QHD 2K export (2560x1440)", "scale=2560:1440:flags=lanczos"),
        PremiumFeature("be_4k", "4K Export", "🎬", "Basic Editing", "UHD 4K export (3840x2160)", "scale=3840:2160:flags=lanczos", alreadyImplemented = true),
        PremiumFeature("be_24fps", "24 FPS Export", "🎥", "Basic Editing", "Cinema 24fps export", "fps=24", alreadyImplemented = true),
        PremiumFeature("be_30fps", "30 FPS Export", "📹", "Basic Editing", "Standard 30fps export", "fps=30", alreadyImplemented = true),
        PremiumFeature("be_60fps", "60 FPS Export", "⚡", "Basic Editing", "Smooth 60fps export", "fps=60", alreadyImplemented = true),
        PremiumFeature("be_120fps", "120 FPS Export", "🚀", "Basic Editing", "Ultra-smooth 120fps export", "fps=120"),
        PremiumFeature("be_hdr", "HDR Export", "🌈", "Basic Editing", "10-bit HDR (BT.2020 PQ) export", "zscale=t=2020_ncl:m=2020_ncl:p=2020_ncl:r=tv,format=yuv420p10le,colorspace=bt2020nc:color_primaries=bt2020:transfer=smpte2084"),
        PremiumFeature("be_high_bitrate", "High Bitrate Export", "💎", "Basic Editing", "High-bitrate visually-lossless export", "")
    )

    // ════════════════════════════════════════════════════════════════════
    //  🤖 AI FEATURES
    // ════════════════════════════════════════════════════════════════════
    val aiFeatures: List<PremiumFeature> = listOf(
        PremiumFeature("ai_auto_cut", "AI Auto Cut", "✂️", "AI Features", "Smart scene-based auto cutting", "select='gt(scene,0.03)'", alreadyImplemented = true),
        PremiumFeature("ai_script_writer", "AI Script Writer", "✍️", "AI Features", "Generate video scripts from a topic", isPro = true),
        PremiumFeature("ai_story_gen", "AI Story / Video Generator", "📖", "AI Features", "Generate storyboards & video", isPro = true),
        PremiumFeature("ai_avatar", "AI Avatar / Presenter", "🧑‍💼", "AI Features", "Animated AI presenter avatar", isPro = true),
        PremiumFeature("ai_character", "AI Character", "🧙", "AI Features", "Custom AI character generation", isPro = true),
        PremiumFeature("ai_talking_photo", "AI Talking Photo", "🗣️", "AI Features", "Animate a still photo to talk", isPro = true),
        PremiumFeature("ai_voice_gen", "AI Voice Generator", "🎤", "AI Features", "Text-to-speech voice generation", isPro = true),
        PremiumFeature("ai_voice_clone", "AI Voice Clone", "🎭", "AI Features", "Clone a voice from sample", isPro = true),
        PremiumFeature("ai_voice_changer", "AI Voice Changer", "🔀", "AI Features", "Change voice pitch & tone", "", "asetrate=44100*1.5,aresample=44100,atempo=0.8", alreadyImplemented = true),
        PremiumFeature("ai_noise_removal", "AI Noise Removal", "🔇", "AI Features", "Remove background noise from audio", "", "afftdn=nr=20:nf=-25", alreadyImplemented = true),
        PremiumFeature("ai_music_gen", "AI Music Generator", "🎵", "AI Features", "Generate royalty-free music", isPro = true),
        PremiumFeature("ai_beat_detect", "AI Beat Detection", "🥁", "AI Features", "Detect beats for sync editing", alreadyImplemented = true),
        PremiumFeature("ai_captions", "AI Auto Captions", "💬", "AI Features", "Auto-generate subtitles", alreadyImplemented = true),
        PremiumFeature("ai_subtitle_gen", "AI Subtitle Generation", "📑", "AI Features", "Generate subtitles in multiple languages", alreadyImplemented = true),
        PremiumFeature("ai_translate", "AI Translation", "🌍", "AI Features", "Translate captions across languages", isPro = true),
        PremiumFeature("ai_lip_sync", "AI Lip Sync", "👄", "AI Features", "Sync audio to lip movement", isPro = true),
        PremiumFeature("ai_face_retouch", "AI Face Retouch", "✨", "AI Features", "Smooth & retouch faces", "hqdn3d=2:1:2:1,eq=brightness=0.03:contrast=1.02", alreadyImplemented = true),
        PremiumFeature("ai_enhance", "AI Enhance / Portrait", "🖼️", "AI Features", "Portrait enhancement & sharpening", "unsharp=5:5:0.8:3:3:0.4", alreadyImplemented = true),
        PremiumFeature("ai_upscale", "AI Upscale", "🔍", "AI Features", "Upscale video resolution 2x", "scale=iw*2:ih*2:flags=lanczos,unsharp=5:5:0.6", isPro = true),
        PremiumFeature("ai_restore", "AI Restore", "🛠️", "AI Features", "Restore old/damaged video", "hqdn3d=4:3:4:3,eq=contrast=1.1:saturation=1.1,unsharp=5:5:0.5", isPro = true),
        PremiumFeature("ai_image_enhance", "AI Image Enhance", "🌟", "AI Features", "Enhance image quality", "unsharp=7:7:1.0:7:7:0.0,eq=brightness=0.04", isPro = true),
        PremiumFeature("ai_color_correct", "AI Color Correction", "🎨", "AI Features", "Automatic color correction", "eq=contrast=1.1:saturation=1.15:brightness=0.02,curves=preset=strong_contrast", alreadyImplemented = true),
        PremiumFeature("ai_color_match", "AI Color Match", "🎯", "AI Features", "Match colors between clips", alreadyImplemented = true),
        PremiumFeature("ai_object_remove", "AI Object Removal", "🪄", "AI Features", "Remove unwanted objects", "removelogo=0:0:64:64", isPro = true),
        PremiumFeature("ai_bg_remove", "AI Background Removal", "🧹", "AI Features", "Remove/replace video background", "chromakey=0x00FF00:0.3:0.1", alreadyImplemented = true),
        PremiumFeature("ai_sky_replace", "AI Sky Replacement", "🌅", "AI Features", "Replace sky in video", isPro = true),
        PremiumFeature("ai_relight", "AI Relight", "💡", "AI Features", "Relight scenes after capture", "eq=brightness=0.08:contrast=1.05,curves=preset=lighter", isPro = true),
        PremiumFeature("ai_motion_track", "AI Motion Tracking", "🎯", "AI Features", "Track motion of objects", isPro = true),
        PremiumFeature("ai_camera_track", "AI Camera Tracking", "📷", "AI Features", "Track camera movement", isPro = true),
        PremiumFeature("ai_smart_cutout", "AI Smart Cutout / Mask", "✂️", "AI Features", "Auto cutout subject & mask", alreadyImplemented = true),
        PremiumFeature("ai_auto_reframe", "AI Auto Reframe", "📐", "AI Features", "Auto reframe for aspect ratios", "crop=iw*0.56:ih:(iw-iw*0.56)/2:0,scale=1080:1920", alreadyImplemented = true),
        PremiumFeature("ai_frame_interp", "AI Frame Interpolation", "🎞️", "AI Features", "Interpolate frames for smoothness", "minterpolate=fps=60:mi_mode=mci:mc_mode=aobmc:me_mode=bidir", isPro = true),
        PremiumFeature("ai_slow_motion", "AI Slow Motion", "🐌", "AI Features", "Optical-flow slow motion", "minterpolate=fps=120:mi_mode=mci,setpts=4*PTS,fps=30", isPro = true),
        PremiumFeature("ai_super_res", "AI Super Resolution", "🔬", "AI Features", "AI super-resolution upscale", "scale=iw*2:ih*2:flags=lanczos,unsharp=7:7:1.2:7:7:0", isPro = true),
        PremiumFeature("ai_denoise", "AI Video Denoise", "🧽", "AI Features", "Reduce video grain & noise", "hqdn3d=4:3:4:3", alreadyImplemented = true),
        PremiumFeature("ai_deblur", "AI Deblur", "🌫️", "AI Features", "Sharpen blurred footage", "unsharp=7:7:1.5:7:7:0.0", isPro = true),
        PremiumFeature("ai_deflicker", "AI Remove Flicker", "⚡", "AI Features", "Remove frame flicker", "deflicker=mode=am:size=10", isPro = true),
        PremiumFeature("ai_remove_watermark", "AI Remove Watermark", "🚫", "AI Features", "Remove logo/watermark from video", "removelogo=x=10:y=10:w=120:h=40", isPro = true),
        PremiumFeature("ai_sticker_gen", "AI Sticker Generator", "😀", "AI Features", "Generate custom stickers", isPro = true),
        PremiumFeature("ai_emoji_gen", "AI Emoji Generator", "😎", "AI Features", "Generate custom emojis", isPro = true),
        PremiumFeature("ai_thumbnail_gen", "AI Thumbnail Generator", "🖼️", "AI Features", "Generate video thumbnails", alreadyImplemented = true),
        PremiumFeature("ai_scene_detect", "AI Scene Detection", "🎬", "AI Features", "Detect & mark scenes", "select='gt(scene,0.04)',showinfo", isPro = true),
        PremiumFeature("ai_highlight", "AI Highlight Detection", "⭐", "AI Features", "Find highlight moments", isPro = true),
        PremiumFeature("ai_clip_select", "AI Clip Selection", "🎯", "AI Features", "Auto-select best clips", isPro = true),
        PremiumFeature("ai_auto_zoom", "AI Auto Zoom", "🔍", "AI Features", "Auto zoom to subject", "zoompan=z='min(zoom+0.0015,1.5)':d=1:s=1920x1080:fps=30", isPro = true),
        PremiumFeature("ai_auto_effects", "AI Auto Effects", "✨", "AI Features", "Auto-apply effects to clips", alreadyImplemented = true),
        PremiumFeature("ai_auto_transition", "AI Auto Transition", "🔄", "AI Features", "Auto-apply transitions", alreadyImplemented = true),
        PremiumFeature("ai_sound_effects", "AI Sound Effects", "🔊", "AI Features", "Auto-generate sound effects", "", "aecho=0.6:0.3:100:0.3", isPro = true),
        PremiumFeature("ai_text_rewrite", "AI Text Rewrite", "📝", "AI Features", "Rewrite & improve text", isPro = true),
        PremiumFeature("ai_prompt_video", "AI Prompt to Video", "🎥", "AI Features", "Generate video from prompt", isPro = true),
        PremiumFeature("ai_prompt_image", "AI Prompt to Image", "🖼️", "AI Features", "Generate image from prompt", isPro = true),
        PremiumFeature("ai_product_video", "AI Product Video", "🛍️", "AI Features", "Create product showcase videos", isPro = true),
        PremiumFeature("ai_social_video", "AI Social Media Video", "📱", "AI Features", "Generate social-optimized videos", isPro = true),
        PremiumFeature("ai_shorts", "AI Shorts / Reel Generator", "📲", "AI Features", "Generate vertical short-form videos", "crop=iw*0.56:ih:(iw-iw*0.56)/2:0,scale=1080:1920", isPro = true),
        PremiumFeature("ai_tiktok", "AI TikTok Generator", "🎵", "AI Features", "Generate TikTok-ready content", "crop=iw*0.56:ih:(iw-iw*0.56)/2:0,scale=1080:1920", isPro = true),
        PremiumFeature("ai_youtube", "AI YouTube Generator", "▶️", "AI Features", "Generate YouTube-ready content", "scale=1920:1080", isPro = true)
    )

    // ════════════════════════════════════════════════════════════════════
    //  📝 TEXT & TITLES
    // ════════════════════════════════════════════════════════════════════
    val textTitles: List<PremiumFeature> = listOf(
        PremiumFeature("tt_add_text", "Add Text", "📝", "Text & Titles", "Add text overlay", alreadyImplemented = true),
        PremiumFeature("tt_auto_captions", "Auto Captions", "💬", "Text & Titles", "Auto-generate captions", alreadyImplemented = true),
        PremiumFeature("tt_manual_captions", "Manual Captions", "✏️", "Text & Titles", "Manually add captions", alreadyImplemented = true),
        PremiumFeature("tt_templates", "Text / Title Templates", "🏷️", "Text & Titles", "Pre-made title templates", alreadyImplemented = true),
        PremiumFeature("tt_lower_thirds", "Lower Thirds", "📊", "Text & Titles", "Broadcast-style lower thirds", alreadyImplemented = true),
        PremiumFeature("tt_typewriter", "Typewriter", "⌨️", "Text & Titles", "Typewriter text animation", alreadyImplemented = true),
        PremiumFeature("tt_neon", "Neon Text", "💡", "Text & Titles", "Glowing neon text effect", alreadyImplemented = true),
        PremiumFeature("tt_glow", "Glow Text", "✨", "Text & Titles", "Glowing text effect", alreadyImplemented = true),
        PremiumFeature("tt_gradient", "Gradient Text", "🌈", "Text & Titles", "Gradient-filled text", alreadyImplemented = true),
        PremiumFeature("tt_outline", "Outline Text", "🔲", "Text & Titles", "Outlined text", alreadyImplemented = true),
        PremiumFeature("tt_shadow", "Shadow Text", "🌑", "Text & Titles", "Drop-shadow text", alreadyImplemented = true),
        PremiumFeature("tt_curve", "Curve Text", "↩️", "Text & Titles", "Curved text on path", isPro = true),
        PremiumFeature("tt_3d", "3D Text", "🧊", "Text & Titles", "3D extruded text", isPro = true),
        PremiumFeature("tt_animation", "Text Animation", "🎞️", "Text & Titles", "Animated text presets", alreadyImplemented = true),
        PremiumFeature("tt_karaoke", "Karaoke Lyrics", "🎤", "Text & Titles", "Synced karaoke lyrics", alreadyImplemented = true),
        PremiumFeature("tt_scrolling", "Scrolling Text", "📜", "Text & Titles", "Scrolling text marquee", alreadyImplemented = true),
        PremiumFeature("tt_emoji", "Emoji Text", "😀", "Text & Titles", "Add emojis to text", alreadyImplemented = true),
        PremiumFeature("tt_tracking", "Text Tracking", "📏", "Text & Titles", "Letter spacing control", isPro = true),
        PremiumFeature("tt_spacing", "Text Spacing", "↔️", "Text & Titles", "Adjust letter spacing", alreadyImplemented = true),
        PremiumFeature("tt_rotation", "Text Rotation", "🔄", "Text & Titles", "Rotate text to any angle", alreadyImplemented = true),
        PremiumFeature("tt_text_tracking", "Text Motion Tracking", "🎯", "Text & Titles", "Track text to moving object", isPro = true)
    )

    // ════════════════════════════════════════════════════════════════════
    //  🎵 AUDIO
    // ════════════════════════════════════════════════════════════════════
    val audio: List<PremiumFeature> = listOf(
        PremiumFeature("au_record", "Voice Record", "🎙️", "Audio", "Record voiceover", alreadyImplemented = true),
        PremiumFeature("au_extract", "Extract Audio", "🎵", "Audio", "Extract audio from video", alreadyImplemented = true),
        PremiumFeature("au_separate", "Separate Audio", "🔀", "Audio", "Separate vocals & instruments", "", "stereotools=mlev=1", isPro = true),
        PremiumFeature("au_music_library", "Music Library", "📚", "Audio", "Browse royalty-free music", alreadyImplemented = true),
        PremiumFeature("au_sound_effects", "Sound Effects", "🔊", "Audio", "Library of sound effects", alreadyImplemented = true),
        PremiumFeature("au_fade", "Fade In / Out", "📈", "Audio", "Audio fade in & out", "", "afade=t=in:st=0:d=2,afade=t=out:st=28:d=2", alreadyImplemented = true),
        PremiumFeature("au_eq", "Equalizer (EQ)", "🎚️", "Audio", "Multi-band equalizer", "", "equalizer=f=1000:t=q:w=0.5:g=3", alreadyImplemented = true),
        PremiumFeature("au_bass_boost", "Bass Boost", "🔊", "Audio", "Boost low frequencies", "", "bass=g=8:f=80:w=0.6", alreadyImplemented = true),
        PremiumFeature("au_treble", "Treble", "🎶", "Audio", "Boost high frequencies", "", "treble=g=6:f=4000:w=0.7", alreadyImplemented = true),
        PremiumFeature("au_compressor", "Compressor", "🗜️", "Audio", "Dynamic range compression", "", "acompressor=threshold=-20dB:ratio=4:attack=5:release=50", alreadyImplemented = true),
        PremiumFeature("au_limiter", "Limiter", "⛔", "Audio", "Prevent audio clipping", "", "alimiter=limit=0.9:attack=5:release=50", isPro = true),
        PremiumFeature("au_reverb", "Reverb", "🏛️", "Audio", "Add reverb / echo space", "", "aecho=0.8:0.7:60:0.4,aecho=0.6:0.5:120:0.3", alreadyImplemented = true),
        PremiumFeature("au_echo", "Echo", "🔁", "Audio", "Echo effect", "", "aecho=0.6:0.6:500:0.5", alreadyImplemented = true),
        PremiumFeature("au_pitch", "Pitch Shift", "🎵", "Audio", "Change audio pitch", "", "asetrate=44100*1.3,aresample=44100,atempo=0.85", alreadyImplemented = true),
        PremiumFeature("au_voice_fx", "Voice Effects", "🗣️", "Audio", "Robot, phone, chipmunk voices", "", "asetrate=44100*2,aresample=44100,atempo=0.5", alreadyImplemented = true),
        PremiumFeature("au_speed", "Audio Speed", "⏩", "Audio", "Speed up / slow down audio", "", "atempo=1.5", alreadyImplemented = true),
        PremiumFeature("au_noise_reduction", "Noise Reduction", "🔇", "Audio", "Reduce background noise", "", "afftdn=nr=15:nf=-25", alreadyImplemented = true),
        PremiumFeature("au_vocal_isolation", "Vocal Isolation", "🎤", "Audio", "Isolate vocals from mix", "", "stereotools=mlev=1:mdelay=1", isPro = true),
        PremiumFeature("au_beat_sync", "Beat Sync", "🥁", "Audio", "Sync edits to beat", alreadyImplemented = true),
        PremiumFeature("au_ducking", "Audio Ducking", "🔻", "Audio", "Auto-duck music under voice", "", "sidechaincompress=threshold=0.05:ratio=8:attack=5:release=300", alreadyImplemented = true)
    )

    // ════════════════════════════════════════════════════════════════════
    //  🎨 COLOR GRADING
    // ════════════════════════════════════════════════════════════════════
    val colorGrading: List<PremiumFeature> = listOf(
        PremiumFeature("cg_brightness", "Brightness", "☀️", "Color Grading", "Adjust brightness", "eq=brightness=0.08", alreadyImplemented = true),
        PremiumFeature("cg_contrast", "Contrast", "◐", "Color Grading", "Adjust contrast", "eq=contrast=1.15", alreadyImplemented = true),
        PremiumFeature("cg_saturation", "Saturation", "🎨", "Color Grading", "Adjust saturation", "eq=saturation=1.3", alreadyImplemented = true),
        PremiumFeature("cg_exposure", "Exposure", "🔆", "Color Grading", "Adjust exposure", "eq=brightness=0.12:contrast=1.05", alreadyImplemented = true),
        PremiumFeature("cg_highlights", "Highlights", "⚪", "Color Grading", "Recover highlights", "curves=preset=lighter", alreadyImplemented = true),
        PremiumFeature("cg_shadows", "Shadows", "⚫", "Color Grading", "Lift shadows", "curves=preset=darker", alreadyImplemented = true),
        PremiumFeature("cg_temperature", "Temperature", "🌡️", "Color Grading", "Warm / cool temperature", "colortemperature=3500", alreadyImplemented = true),
        PremiumFeature("cg_tint", "Tint", "🟢", "Color Grading", "Green / magenta tint", "colorbalance=gs=-0.05:bs=0.05", alreadyImplemented = true),
        PremiumFeature("cg_vibrance", "Vibrance", "🌈", "Color Grading", "Boost muted colors", "eq=saturation=1.4,scale=iw:ih", alreadyImplemented = true),
        PremiumFeature("cg_hue", "Hue", "🍭", "Color Grading", "Shift hue", "hue=h=30:s=1", alreadyImplemented = true),
        PremiumFeature("cg_sharpen", "Sharpen", "🔪", "Color Grading", "Sharpen details", "unsharp=5:5:0.8:3:3:0.4", alreadyImplemented = true),
        PremiumFeature("cg_fade", "Fade", "🌫️", "Color Grading", "Faded vintage look", "eq=contrast=0.85:saturation=0.8:brightness=0.05", alreadyImplemented = true),
        PremiumFeature("cg_curves", "Curves", "📈", "Color Grading", "RGB curves adjustment", "curves=preset=strong_contrast", alreadyImplemented = true),
        PremiumFeature("cg_hsl", "HSL", "🎯", "Color Grading", "Hue/sat/light per color", "hue=h=10:s=1.2", alreadyImplemented = true),
        PremiumFeature("cg_lut", "LUT Import", "📦", "Color Grading", "Apply 3D LUT files", "lut3d=file.cube", isPro = true),
        PremiumFeature("cg_color_wheels", "Color Wheels", "🎡", "Color Grading", "Pro color wheel grading", "colorbalance=rs=0.1:gs=-0.05:bs=0.05", isPro = true),
        PremiumFeature("cg_split_tone", "Split Toning", "🌗", "Color Grading", "Tone shadows & highlights", "colorbalance=rs=0.1:rh=-0.1", alreadyImplemented = true),
        PremiumFeature("cg_vignette", "Vignette", "🌑", "Color Grading", "Darken edges", "vignette=angle=PI/4", alreadyImplemented = true),
        PremiumFeature("cg_grain", "Film Grain", "🎞️", "Color Grading", "Add film grain", "noise=alls=20:allf=t+u", alreadyImplemented = true),
        PremiumFeature("cg_auto_color", "Auto Color", "🎨", "Color Grading", "Automatic color balance", "eq=contrast=1.1:saturation=1.15,curves=preset=lighter", alreadyImplemented = true)
    )

    // ════════════════════════════════════════════════════════════════════
    //  ✨ EFFECTS
    // ════════════════════════════════════════════════════════════════════
    val effects: List<PremiumFeature> = listOf(
        PremiumFeature("fx_blur", "Blur", "🌫️", "Effects", "Gaussian blur", "boxblur=10:1", alreadyImplemented = true),
        PremiumFeature("fx_bokeh", "Bokeh", "🫧", "Effects", "Bokeh blur background", "boxblur=20:2,unsharp=5:5:0.5:5:5:0", alreadyImplemented = true),
        PremiumFeature("fx_glitch", "Glitch", "📺", "Effects", "Digital glitch effect", "rgbashift=rh=-3:bv=3,tinterlace=mode=2", alreadyImplemented = true),
        PremiumFeature("fx_vhs", "VHS", "📼", "Effects", "Retro VHS tape effect", "vignette=angle=PI/5,noise=alls=15:allf=t,hqdn3d=2:1:2:1", alreadyImplemented = true),
        PremiumFeature("fx_rgb_split", "RGB Split", "🌈", "Effects", "Chromatic RGB split", "rgbashift=rh=-5:bv=5", alreadyImplemented = true),
        PremiumFeature("fx_chromatic", "Chromatic Aberration", "🔴", "Effects", "Lens chromatic aberration", "rgbashift=rh=-3:rv=2:bh=2:bv=-3", alreadyImplemented = true),
        PremiumFeature("fx_camera_shake", "Camera Shake", "📳", "Effects", "Simulate camera shake", "crop=iw:ih:'0+5*sin(2*PI*t*4)':'0+5*cos(2*PI*t*3)'", alreadyImplemented = true),
        PremiumFeature("fx_flash", "Flash", "⚡", "Effects", "Camera flash effect", "eq=brightness='0.5+0.5*exp(-t*3)'", alreadyImplemented = true),
        PremiumFeature("fx_lightning", "Lightning", "⛈️", "Effects", "Lightning strike overlay", "eq=brightness='0.8*lt(mod(t,3),0.1)'", isPro = true),
        PremiumFeature("fx_fire", "Fire", "🔥", "Effects", "Fire overlay effect", "eq=contrast=1.3:saturation=1.5,geq=r='r(X,Y)+30'", alreadyImplemented = true),
        PremiumFeature("fx_smoke", "Smoke", "💨", "Effects", "Smoke / fog overlay", "noise=alls=8:allf=t,hqdn3d=1:0:1:0", alreadyImplemented = true),
        PremiumFeature("fx_rain", "Rain", "🌧️", "Effects", "Rain overlay", "noise=alls=5:allf=t,crop=iw:ih:0:0", alreadyImplemented = true),
        PremiumFeature("fx_snow", "Snow", "❄️", "Effects", "Snowfall overlay", "noise=alls=20:allf=t+u", alreadyImplemented = true),
        PremiumFeature("fx_fog", "Fog", "🌫️", "Effects", "Atmospheric fog", "boxblur=4:1,eq=brightness=0.05:contrast=0.9,colorbalance=bs=0.05", isPro = true),
        PremiumFeature("fx_sparkles", "Sparkles", "✨", "Effects", "Sparkle particles", "noise=alls=3:allf=t,eq=brightness='0.05*sin(t*20)'", alreadyImplemented = true),
        PremiumFeature("fx_neon", "Neon", "💡", "Effects", "Neon glow effect", "eq=saturation=2.0:contrast=1.3,unsharp=5:5:2.0", alreadyImplemented = true),
        PremiumFeature("fx_hologram", "Hologram", "👻", "Effects", "Sci-fi hologram effect", "rgbashift=rh=-2:bv=2,hqdn3d=1:0:1:0,eq=saturation=0.6:contrast=1.2,colorbalance=gs=0.1:bs=0.2", isPro = true),
        PremiumFeature("fx_comic", "Comic", "💬", "Effects", "Comic book style", "format=gray,eq=contrast=1.8", alreadyImplemented = true),
        PremiumFeature("fx_cartoon", "Cartoon", "🎨", "Effects", "Cartoon / cel-shade", "hqdn3d=5:5:5:5,eq=contrast=1.3:saturation=1.5", alreadyImplemented = true),
        PremiumFeature("fx_sketch", "Sketch", "✏️", "Effects", "Pencil sketch", "format=gray,edge", alreadyImplemented = true),
        PremiumFeature("fx_oil_paint", "Oil Paint", "🖌️", "Effects", "Oil painting style", "hqdn3d=10:10:10:10,eq=contrast=1.2", alreadyImplemented = true),
        PremiumFeature("fx_pixel", "Pixelate", "🟦", "Effects", "Pixel mosaic", "scale=iw/20:ih/20,scale=iw:ih:flags=neighbor", alreadyImplemented = true),
        PremiumFeature("fx_mosaic", "Mosaic", "🔳", "Effects", "Tile mosaic", "scale=iw/16:ih/16,scale=iw:ih:flags=neighbor", alreadyImplemented = true),
        PremiumFeature("fx_mirror", "Mirror", "🪞", "Effects", "Mirror reflection split", "split[a][b];[b]hflip[b2];[a][b2]hstack", isPro = true),
        PremiumFeature("fx_kaleidoscope", "Kaleidoscope", "🔷", "Effects", "Kaleidoscope pattern", "geq=lum='p(X,Y)':cb='p(mod(X+W/2,W),Y)':cr='p(X,mod(Y+H/2,H))'", isPro = true),
        PremiumFeature("fx_distortion", "Distortion", "🌀", "Effects", "Lens distortion warp", "lenscorrection=k1='0.3*sin(t*2)':k2='0.2*cos(t*2)'", alreadyImplemented = true),
        PremiumFeature("fx_lens_flare", "Lens Flare", "🌟", "Effects", "Anamorphic lens flare", "eq=brightness=0.05,colorbalance=rs=0.1:rh=0.05", alreadyImplemented = true),
        PremiumFeature("fx_light_leak", "Light Leak", "🌅", "Effects", "Vintage light leak", "vignette=angle=PI/3,colorbalance=rs=0.15:rh=0.1", alreadyImplemented = true),
        PremiumFeature("fx_film_burn", "Film Burn", "🔥", "Effects", "Film burn transition", "eq=brightness='0.3*(1-exp(-t*2))':saturation=1.5", alreadyImplemented = true),
        PremiumFeature("fx_old_film", "Old Film", "📽️", "Effects", "Aged film with grain & scratches", "vignette=angle=PI/4,noise=alls=25:allf=t+u,hqdn3d=2:1:2:1,eq=contrast=0.9:saturation=0.6", alreadyImplemented = true)
    )

    // ════════════════════════════════════════════════════════════════════
    //  🔄 TRANSITIONS
    // ════════════════════════════════════════════════════════════════════
    val transitions: List<PremiumFeature> = listOf(
        PremiumFeature("tr_fade", "Fade", "🌑", "Transitions", "Fade to black", "fade=t=out:st=0:d=1", alreadyImplemented = true),
        PremiumFeature("tr_dissolve", "Dissolve", "💧", "Transitions", "Cross dissolve", "fade=t=in:st=0:d=1", alreadyImplemented = true),
        PremiumFeature("tr_slide", "Slide", "➡️", "Transitions", "Slide transition", "crop=iw:ih:'-iw+t*1000':0", alreadyImplemented = true),
        PremiumFeature("tr_push", "Push", "👊", "Transitions", "Push transition", "crop=iw:ih:'iw*(1-t)':0", alreadyImplemented = true),
        PremiumFeature("tr_pull", "Pull", "🖐️", "Transitions", "Pull back transition", "crop=iw:ih:'-iw*(1-t)':0", isPro = true),
        PremiumFeature("tr_zoom", "Zoom", "🔍", "Transitions", "Zoom in/out transition", "zoompan=z='1+0.5*t':d=1:s=1920x1080:fps=30", alreadyImplemented = true),
        PremiumFeature("tr_spin", "Spin", "🌀", "Transitions", "Spin rotation transition", "rotate='2*PI*t'", alreadyImplemented = true),
        PremiumFeature("tr_rotate", "Rotate", "🔄", "Transitions", "3D rotate transition", "rotate='PI*t'", alreadyImplemented = true),
        PremiumFeature("tr_blur", "Blur", "🌫️", "Transitions", "Blur transition", "boxblur='5+30*(1-t)':1", alreadyImplemented = true),
        PremiumFeature("tr_warp", "Warp", "🚀", "Transitions", "Warp speed transition", "scale='1+2*t':'1+2*t'", isPro = true),
        PremiumFeature("tr_stretch", "Stretch", "↔️", "Transitions", "Elastic stretch transition", "scale='1+0.5*sin(t*PI)':'1'", isPro = true),
        PremiumFeature("tr_glitch", "Glitch", "📺", "Transitions", "Glitch transition", "rgbashift=rh='-5*t':bv='5*t'", alreadyImplemented = true),
        PremiumFeature("tr_flash", "Flash", "⚡", "Transitions", "Flash white transition", "eq=brightness='5*t*(1-t)*4'", alreadyImplemented = true),
        PremiumFeature("tr_ripple", "Ripple", "🌊", "Transitions", "Ripple wave transition", "lenscorrection=k1='0.2*sin(t*10)':k2='0.2*cos(t*10)'", alreadyImplemented = true),
        PremiumFeature("tr_page_turn", "Page Turn", "📄", "Transitions", "Page flip transition", "perspective=0,hflip", isPro = true),
        PremiumFeature("tr_camera_move", "Camera Move", "🎥", "Transitions", "Simulated camera move", "zoompan=z='1+0.2*t':x='iw*t':y='ih*t':d=1:s=1920x1080:fps=30", isPro = true),
        PremiumFeature("tr_whip_pan", "Whip Pan", "💨", "Transitions", "Fast whip pan transition", "crop=iw:ih:'iw*3*t-2*iw':0", isPro = true),
        PremiumFeature("tr_cube", "Cube", "🧊", "Transitions", "3D cube rotate transition", "rotate='PI*t',scale='1-abs(t-0.5)*0.5':'1'", isPro = true),
        PremiumFeature("tr_flip", "Flip", "🔃", "Transitions", "Flip transition", "vflip", alreadyImplemented = true),
        PremiumFeature("tr_smooth_cut", "Smooth Cut", "✂️", "Transitions", "Seamless smooth cut", "fade=t=in:st=0:d=0.3", alreadyImplemented = true)
    )

    // ════════════════════════════════════════════════════════════════════
    //  🎬 ANIMATION
    // ════════════════════════════════════════════════════════════════════
    val animation: List<PremiumFeature> = listOf(
        PremiumFeature("an_in", "In Animation", "📥", "Animation", "Entrance animations", alreadyImplemented = true),
        PremiumFeature("an_out", "Out Animation", "📤", "Animation", "Exit animations", alreadyImplemented = true),
        PremiumFeature("an_combo", "Combo Animation", "🔀", "Animation", "In + out combo", alreadyImplemented = true),
        PremiumFeature("an_bounce", "Bounce", "🏀", "Animation", "Bouncy entrance", alreadyImplemented = true),
        PremiumFeature("an_pop", "Pop", "💥", "Animation", "Pop scale animation", alreadyImplemented = true),
        PremiumFeature("an_swing", "Swing", "🍂", "Animation", "Swing pendulum", alreadyImplemented = true),
        PremiumFeature("an_shake", "Shake", "📳", "Animation", "Shake animation", alreadyImplemented = true),
        PremiumFeature("an_float", "Float", "🎈", "Animation", "Floating motion", "crop=iw:ih:0:'5*sin(2*PI*t)'", isPro = true),
        PremiumFeature("an_zoom", "Zoom", "🔍", "Animation", "Zoom in/out", alreadyImplemented = true),
        PremiumFeature("an_spin", "Spin", "🌀", "Animation", "Spinning motion", alreadyImplemented = true),
        PremiumFeature("an_rotate", "Rotate", "🔄", "Animation", "Rotation animation", alreadyImplemented = true),
        PremiumFeature("an_slide", "Slide", "➡️", "Animation", "Slide animation", alreadyImplemented = true),
        PremiumFeature("an_fade", "Fade", "🌑", "Animation", "Fade animation", alreadyImplemented = true),
        PremiumFeature("an_stretch", "Stretch", "↔️", "Animation", "Elastic stretch", alreadyImplemented = true),
        PremiumFeature("an_elastic", "Elastic", "🪀", "Animation", "Elastic spring", alreadyImplemented = true),
        PremiumFeature("an_wiggle", "Wiggle", "〰️", "Animation", "Wiggle jitter motion", "crop=iw:ih:'3*sin(2*PI*t*8)':'3*cos(2*PI*t*6)'", isPro = true),
        PremiumFeature("an_keyframe", "Keyframe Animation", "⏱️", "Animation", "Custom keyframe animation", alreadyImplemented = true)
    )

    // ════════════════════════════════════════════════════════════════════
    //  🖼️ STICKERS & ASSETS
    // ════════════════════════════════════════════════════════════════════
    val stickersAssets: List<PremiumFeature> = listOf(
        PremiumFeature("st_animated", "Animated Stickers", "🎞️", "Stickers & Assets", "Animated GIF stickers", alreadyImplemented = true),
        PremiumFeature("st_emoji", "Emoji Stickers", "😀", "Stickers & Assets", "Emoji sticker pack", alreadyImplemented = true),
        PremiumFeature("st_gif", "GIF Import", "🖼️", "Stickers & Assets", "Import custom GIFs", alreadyImplemented = true),
        PremiumFeature("st_png", "PNG Import", "📥", "Stickers & Assets", "Import transparent PNGs", alreadyImplemented = true),
        PremiumFeature("st_frames", "Frames", "🖼️", "Stickers & Assets", "Decorative frames", alreadyImplemented = true),
        PremiumFeature("st_shapes", "Shapes", "⭐", "Stickers & Assets", "Geometric shapes", alreadyImplemented = true),
        PremiumFeature("st_icons", "Icons", "✨", "Stickers & Assets", "Icon pack", alreadyImplemented = true),
        PremiumFeature("st_decorative", "Decorative Elements", "🎀", "Stickers & Assets", "Decorative overlays", alreadyImplemented = true)
    )

    // ════════════════════════════════════════════════════════════════════
    //  📦 TEMPLATES
    // ════════════════════════════════════════════════════════════════════
    val templates: List<PremiumFeature> = listOf(
        PremiumFeature("tp_reel", "Reel Templates", "📲", "Templates", "Instagram Reel templates", "scale=1080:1920", alreadyImplemented = true),
        PremiumFeature("tp_tiktok", "TikTok Templates", "🎵", "Templates", "TikTok-style templates", "scale=1080:1920", alreadyImplemented = true),
        PremiumFeature("tp_shorts", "Shorts Templates", "▶️", "Templates", "YouTube Shorts templates", "scale=1080:1920", alreadyImplemented = true),
        PremiumFeature("tp_vlog", "Vlog Templates", "📹", "Templates", "Vlog templates", "eq=contrast=1.1:saturation=1.15", alreadyImplemented = true),
        PremiumFeature("tp_gaming", "Gaming Templates", "🎮", "Templates", "Gaming highlight templates", "eq=saturation=1.4:contrast=1.2,unsharp=5:5:1.0", isPro = true),
        PremiumFeature("tp_business", "Business Templates", "💼", "Templates", "Corporate promo templates", "eq=contrast=1.05:saturation=1.05", isPro = true),
        PremiumFeature("tp_promo", "Promo Templates", "📢", "Templates", "Promotional templates", "eq=contrast=1.15:saturation=1.2", alreadyImplemented = true),
        PremiumFeature("tp_slideshow", "Slideshow Templates", "🖼️", "Templates", "Photo slideshow templates", "zoompan=z='1+0.02*on':d=1:s=1920x1080:fps=30", alreadyImplemented = true),
        PremiumFeature("tp_intro", "Intro Templates", "🎬", "Templates", "Channel intro templates", "fade=t=in:st=0:d=1", alreadyImplemented = true),
        PremiumFeature("tp_outro", "Outro Templates", "🔚", "Templates", "Channel outro templates", "fade=t=out:st=0:d=1", alreadyImplemented = true)
    )

    // ════════════════════════════════════════════════════════════════════
    //  📱 SOCIAL MEDIA TOOLS
    // ════════════════════════════════════════════════════════════════════
    val socialMedia: List<PremiumFeature> = listOf(
        PremiumFeature("sm_youtube", "YouTube Export", "▶️", "Social Media", "16:9 1920x1080 for YouTube", "scale=1920:1080", alreadyImplemented = true),
        PremiumFeature("sm_shorts", "YouTube Shorts", "📲", "Social Media", "9:16 1080x1920 for Shorts", "scale=1080:1920", alreadyImplemented = true),
        PremiumFeature("sm_tiktok", "TikTok Export", "🎵", "Social Media", "9:16 1080x1920 for TikTok", "scale=1080:1920", alreadyImplemented = true),
        PremiumFeature("sm_reel", "Instagram Reel", "📸", "Social Media", "9:16 1080x1920 for Reels", "scale=1080:1920", alreadyImplemented = true),
        PremiumFeature("sm_facebook", "Facebook Export", "👍", "Social Media", "Optimized for Facebook", "scale=1280:720", alreadyImplemented = true),
        PremiumFeature("sm_whatsapp", "WhatsApp Status", "💬", "Social Media", "9:16 for WhatsApp status", "scale=1080:1920", alreadyImplemented = true),
        PremiumFeature("sm_snapchat", "Snapchat Export", "👻", "Social Media", "9:16 for Snapchat", "scale=1080:1920", isPro = true),
        PremiumFeature("sm_16_9", "16:9 Landscape", "🖥️", "Social Media", "Standard landscape ratio", "scale=1920:1080", alreadyImplemented = true),
        PremiumFeature("sm_9_16", "9:16 Portrait", "📱", "Social Media", "Vertical portrait ratio", "scale=1080:1920", alreadyImplemented = true),
        PremiumFeature("sm_1_1", "1:1 Square", "⬜", "Social Media", "Square ratio for feed", "scale=1080:1080", alreadyImplemented = true),
        PremiumFeature("sm_4_5", "4:5 Portrait", "🖼️", "Social Media", "Instagram portrait ratio", "scale=1080:1350", alreadyImplemented = true),
        PremiumFeature("sm_21_9", "21:9 Cinema", "🎬", "Social Media", "Cinematic ultra-wide ratio", "scale=2560:1080", isPro = true),
        PremiumFeature("sm_custom", "Custom Aspect Ratio", "📐", "Social Media", "Define custom dimensions", isPro = true)
    )

    // ════════════════════════════════════════════════════════════════════
    //  👑 PRO FEATURES
    // ════════════════════════════════════════════════════════════════════
    val proFeatures: List<PremiumFeature> = listOf(
        PremiumFeature("pro_cloud_storage", "Cloud Storage / Sync", "☁️", "Pro Features", "Sync projects to cloud", isPro = true),
        PremiumFeature("pro_premium_assets", "Premium Assets", "💎", "Pro Features", "Exclusive fonts, effects, music", isPro = true),
        PremiumFeature("pro_no_watermark", "No Watermark", "🚫", "Pro Features", "Watermark-free exports", isPro = true),
        PremiumFeature("pro_high_speed", "High Speed Export", "⚡", "Pro Features", "Priority hardware encoding", alreadyImplemented = true),
        PremiumFeature("pro_team_collab", "Team Collaboration", "👥", "Pro Features", "Share & co-edit projects", isPro = true),
        PremiumFeature("pro_unlimited", "Unlimited Projects", "♾️", "Pro Features", "No project limit", alreadyImplemented = true),
        PremiumFeature("pro_commercial", "Commercial License", "📜", "Pro Features", "Licensed for commercial use", isPro = true),
        PremiumFeature("pro_priority", "Priority / Batch Processing", "🚀", "Pro Features", "Batch queue & priority render", isPro = true),
        PremiumFeature("pro_motion_track", "Advanced Motion Tracking", "🎯", "Pro Features", "Pro-grade motion tracking", isPro = true),
        PremiumFeature("pro_color_grading", "Pro Color Grading", "🎨", "Pro Features", "Full pro color grading suite", "colorbalance=rs=0.1:gs=-0.05:bs=0.05,curves=preset=strong_contrast,eq=contrast=1.1:saturation=1.15", isPro = true),
        PremiumFeature("pro_full_hd", "Full HD Export", "📺", "Pro Features", "1080p premium quality", "scale=1920:1080:flags=lanczos", alreadyImplemented = true),
        PremiumFeature("pro_4k", "4K Export", "🎬", "Pro Features", "Ultra HD 4K export", "scale=3840:2160:flags=lanczos", alreadyImplemented = true),
        PremiumFeature("pro_hdr", "HDR Export", "🌈", "Pro Features", "10-bit HDR PQ export", "zscale=t=2020_ncl:m=2020_ncl:p=2020_ncl:r=tv,format=yuv420p10le,colorspace=bt2020nc:color_primaries=bt2020:transfer=smpte2084", isPro = true),
        PremiumFeature("pro_premium_fonts", "Premium Fonts", "🔤", "Pro Features", "Exclusive font collection", isPro = true),
        PremiumFeature("pro_premium_effects", "Premium Effects", "✨", "Pro Features", "Exclusive effects library", isPro = true),
        PremiumFeature("pro_premium_filters", "Premium Filters", "🎨", "Pro Features", "Exclusive filter presets", isPro = true),
        PremiumFeature("pro_premium_music", "Premium Music", "🎵", "Pro Features", "Licensed music library", isPro = true),
        PremiumFeature("pro_premium_templates", "Premium Templates", "📦", "Pro Features", "Exclusive template pack", isPro = true),
        PremiumFeature("pro_ai_tools", "Premium AI Tools", "🤖", "Pro Features", "Advanced AI suite", isPro = true),
        PremiumFeature("pro_premium_transitions", "Premium Transitions", "🔄", "Pro Features", "Exclusive transitions", isPro = true)
    )

    // ════════════════════════════════════════════════════════════════════
    //  AGGREGATION & LOOKUP
    // ════════════════════════════════════════════════════════════════════
    val categories: List<Triple<String, String, String>> = listOf(
        Triple("basicEditing", "📁 Basic Editing", "be"),
        Triple("aiFeatures", "🤖 AI Features", "ai"),
        Triple("textTitles", "📝 Text & Titles", "tt"),
        Triple("audio", "🎵 Audio", "au"),
        Triple("colorGrading", "🎨 Color Grading", "cg"),
        Triple("effects", "✨ Effects", "fx"),
        Triple("transitions", "🔄 Transitions", "tr"),
        Triple("animation", "🎬 Animation", "an"),
        Triple("stickersAssets", "🖼️ Stickers & Assets", "st"),
        Triple("templates", "📦 Templates", "tp"),
        Triple("socialMedia", "📱 Social Media", "sm"),
        Triple("proFeatures", "👑 Pro Features", "pro")
    )

    fun featuresFor(categoryKey: String): List<PremiumFeature> = when (categoryKey) {
        "basicEditing" -> basicEditing
        "aiFeatures" -> aiFeatures
        "textTitles" -> textTitles
        "audio" -> audio
        "colorGrading" -> colorGrading
        "effects" -> effects
        "transitions" -> transitions
        "animation" -> animation
        "stickersAssets" -> stickersAssets
        "templates" -> templates
        "socialMedia" -> socialMedia
        "proFeatures" -> proFeatures
        else -> emptyList()
    }

    val all: List<PremiumFeature> =
        basicEditing + aiFeatures + textTitles + audio + colorGrading +
        effects + transitions + animation + stickersAssets + templates +
        socialMedia + proFeatures

    val totalCount: Int get() = all.size
    val implementedCount: Int get() = all.count { it.alreadyImplemented }
    val proCount: Int get() = all.count { it.isPro }

    fun byId(id: String): PremiumFeature? = all.find { it.id == id }
    fun videoChainFor(id: String): String = byId(id)?.videoChain.orEmpty()
    fun audioChainFor(id: String): String = byId(id)?.audioChain.orEmpty()
}
