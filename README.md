# PowerCut – Premium Ultra Smooth Pro 2027 NextGen
### *Sab se Tez • Sab se Taqatwar • World Premium Interface*

PowerCut is a professional-grade, blisteringly fast, and incredibly powerful native Android video editor designed for absolute performance and seamless mobile workflows. The **2027 NextGen** release transforms PowerCut into a world-class premium editor with a brand-new aurora-glassmorphic UI, 22 cinematic color grades, 30 real transitions, 32 super effects, 20 text animations, green-screen chroma key, a full image-editor suite, and a buttery-smooth 60fps playback and interaction engine.

Built 100% in pure Kotlin and Jetpack Compose, PowerCut delivers instant, watermark-free video editing, upscaling up to 8K, GPU-shader live-preview filters, multi-track audio mixing, precise speed ramping, image overlays, and smooth video transitions — fully localized in English and Urdu with automatic RTL support.

---

## ⚡ What's New in 3.0.0 — Premium Ultra Smooth Pro 2027 NextGen

### 🎨 World Premium Interface
*   **Aurora Glassmorphic Theme**: A completely redesigned dark obsidian palette with aurora gradient accents (violet, coral, teal, rose), multi-layered glassmorphic surfaces, neon glow, animated aurora background sweeps, and shimmer overlays for premium badges and loading states.
*   **Refined Typography**: Full display/headline/title/body/label hierarchy with tuned font weights and letter spacing for a crisp, modern feel.
*   **Tactile 60fps Interactions**: Every tap uses spring-animated tactile feedback (dampingRatio 0.6, stiffness 420) so the UI feels alive and responsive.
*   **Edge-to-Edge Immersive**: Transparent status and navigation bars with short-edge cutout mode for true fullscreen editing.

### 🎬 22 Cinematic Color Grades (Real GPUImage + FFmpeg)
Vivid, Warm, Cool, Sunset, Golden, Teal-Orange, Dramatic, Vintage, Fade, Pastel, Bloom, Mist, Cyberpunk, Noir, Mono, Grayscale, Sepia, Rose, Forest, Arctic, Invert — each rendered with real GPUImage filter groups for live preview and real FFmpeg `eq`/`colorbalance`/`curves`/`colorchannelmixer` chains on export.

### ✨ 32 Super Effects (Real FFmpeg)
Glitch, VHS, Chromatic, Lens Flare, Snow, Rain, Fire, Sparkle, Dust, Motion Blur, Shake, Flash, Neon Glow, Vignette, Rainbow, Film Grain, Bokeh, Particles, Strobe, Zoom Pulse, Wave Distort, Frost, Starburst, Swirl, Explosion, Light Leak, Film Strip, Color Splash, Electric, Tidal, and more — each mapped to real FFmpeg `noise`/`chromashift`/`boxblur`/`hue`/`eq` filters.

### 🔀 30 Transitions (Real FFmpeg)
Crossfade, Glitch, Zoom In/Out, Spin, Wipe, Dissolve, Blur, Pixelate, Mosaic, Split, Film Burn, Light Leak, Smoke, Circle, Diamond, Heart, Flash, L-Cut, J-Cut, Slide Left/Right/Up/Down, Rotate In/Out, Bounce, Elastic, Spring — all with timeline-expression-driven `fade`/`zoompan`/`crop`/`vignette`/`rotate` filters.

### 🎭 20 Text Animations
Fade In/Out, Typewriter, Bounce, Slide L/R/U/D, Zoom In/Out, Rotate, Wave, Glitch In, Neon Pulse, Pop, Flip, Elastic, Spring, Rubber, Swing — implemented with animated `drawtext` alpha/x/y/fontsize expressions.

### 🟢 Green Screen / Chroma Key
Real FFmpeg `chromakey` filter with adjustable threshold and color (green/blue/red), plus background image replacement.

### 🖼️ Image Overlay
Layer any image on top of your video with adjustable opacity, scale, and X/Y position via `filter_complex` overlay.

### 🎛️ Full Image Editor Suite
Brightness, Contrast, Saturation, Exposure, Highlights, Shadows, Temperature, Sharpen (unsharp), Blur (boxblur), Vignette, Grain (noise), Fade — all real FFmpeg `eq`/`colorbalance`/`unsharp`/`boxblur`/`vignette`/`noise` filters.

### 🧊 3D Cinematic Masks & Cinematic Bars
Circle, Heart, Star, Hexagon, Diamond, Triangle, Vignette, Film Burn, Light Leak, Lens Flare, Smoke, Water, Fire, Particles, Bokeh, Glitch 3D, Chromatic, Anamorphic, Cinematic Bars, Color Splash.

### 🚀 Smooth 60fps Engine
*   **ExoPlayer Tuned**: Custom `DefaultLoadControl` (800ms min buffer, 8s max), `DefaultTrackSelector` forcing highest bitrate/sample rate, and extension renderer mode for hardware-accelerated decoding.
*   **33ms Progress Polling**: Playback scrubber updates at ~30fps for a fluid timeline feel.
*   **Live ColorMatrix Preview**: All 22 filters render in real time via Compose `ColorMatrix` — no waiting for export to see the look.
*   **Spring Panel Animations**: Tool panels expand/collapse with spring physics + crossfade for premium motion.

### 📥 Long Video Import — Fixed
The previous long-video import failure (OOM / blocking / rejected content URIs) is now resolved:
*   **Fast MediaStore path**: Duration and size read from MediaStore on Android Q+ without touching `MediaMetadataRetriever`.
*   **Bounded MMR**: When MediaStore is unavailable, `MediaMetadataRetriever` is used with proper cleanup and never blocks the main thread.
*   **IO dispatcher**: All path resolution and metadata extraction runs on `Dispatchers.IO`.
*   **Persistable URI permission**: Content URIs are granted persistable read access so they survive across sessions.
*   **No full-file copy**: Large videos are never loaded into memory; FFmpeg streams directly with an 8MB buffer, and disk-space is verified (3× input size, min 500MB) before export begins.
*   **Progress reporting**: The import dialog shows a determinate percentage so users know it's working.

---

## ⚡ Core Features (Carried Forward)

*   **Instant Trim (Sab se Tez)**: Slice videos in milliseconds without re-encoding using smart stream copy.
*   **4K/8K Extreme HD Exports (Sab se Taqatwar)**: Hardware-accelerated upscaling up to 8K.
*   **Speed Control (0.1x to 16.0x)**: Smooth speed ramping with speed curves (constant, ease-in, ease-out, ease-in-out, smooth, ramp) and synchronized audio pitching.
*   **Multi-Track Audio Mixer**: Layer background music, adjust independent audio track volumes, or mute audio streams.
*   **Auto-Captions**: Dynamic captions in English and Urdu.
*   **Silence Remover**: Smart silence-gate volume detection and automatic silent segment removal.
*   **Preset Aspect Ratios**: 9:16, 16:9, 1:1, 4:5 with letterbox and orientation tools.
*   **Bilingual Dynamic Interface**: Seamless English/Urdu toggle with full LTR/RTL layout switching.
*   **Strict No Watermark Promise**: Export premium media without intrusive branding.
*   **Pure Native Architecture**: No WebViews, no hybrid wrappers, no Python. Fully offline.

---

## 🛠️ Technology Stack

*   **Language**: Kotlin 1.9.22
*   **UI Framework**: Jetpack Compose (Declarative, Reactive Design)
*   **Player & Engine**: Media3 & ExoPlayer (tuned LoadControl + TrackSelector for 60fps)
*   **Transcoding**: FFmpeg-Kit `io.github.maitrungduc1410:ffmpeg-kit-full:8.1.2` (Multi-Core & ARM NEON optimized)
*   **Filters**: GPUImage `jp.co.cyberagent.android:gpuimage:2.1.0` (GPU-accelerated live shader preview)
*   **Dependency Injection**: Hilt & Dagger
*   **Asynchronous Flow**: Kotlin Coroutines & Flow
*   **Min SDK**: 26 (Android 8.0 Oreo)
*   **Target SDK**: 34 (Android 14)
*   **AGP**: 8.4.0

---

## 📁 Project Structure

The project strictly follows the clean architecture MVVM pattern:

```text
app/src/main/java/com/powercut/editor/
├── core/
│  ├── base/       # Base Resource wrappers and states
│  ├── di/         # Hilt Modules (Dispatchers, App Context providers)
│  └── utils/      # LanguageHelper (RTL), UriHelper (fast metadata, long-video import fix)
├── data/
│  ├── VideoProject.kt      # Unified Project model (all 2027 NextGen fields + serialization)
│  └── ProjectRepository.kt  # Shared State repository for screen sync
├── domain/
│  ├── ai/         # 22 AIFilter GPUImage shader mappings
│  ├── export/     # ExportManager — 8MB buffer, disk checks, instant-trim detection
│  ├── processing/ # VideoProcessor — real FFmpeg filtergraph for all effects/grades/transitions
│  └── timeline/   # TimelineHelper for formatted playback rulers
└── ui/
   ├── theme/      # Aurora glassmorphic palette, gradients, tactile modifiers, typography
   ├── home/       # Home dashboard with 40+ templates across 7 categories
   ├── editor/     # NextGen editor: ExoPlayer, live ColorMatrix preview, 18 tool panels
   └── export/     # Export progress and success readouts
```

---

## 🚀 Build & Compile Instructions

### Prerequisites
*   Android SDK (target API 34, min API 26)
*   Java Development Kit (JDK) 17 or 21
*   Gradle 8.8+

### Locally Run Unit Tests
To run compile-time checks and verify ViewModel, model, and timeline helper business logic:
```bash
./gradlew test
```

### Build Debug APK
```bash
./gradlew assembleDebug
```
The resulting APK can be found under `app/build/outputs/apk/debug/`.

### Build Release APK & App Bundle (AAB)
```bash
./gradlew assembleRelease bundleRelease
```
The resulting release builds can be found under `app/build/outputs/apk/release/` and `app/build/outputs/bundle/release/`.

---

## 🤖 GitHub Actions CI/CD Pipeline

The project includes an automated workflow in `.github/workflows/build.yml`. On every push or pull request, the workflow will:
1. Initialize JDK 21 and cache Gradle packages.
2. Compile and assemble the APKs and AAB bundles.
3. Automatically sign release builds if secure secrets are defined (`SIGNING_KEY`, `ALIAS`, `KEY_STORE_PASSWORD`, `KEY_PASSWORD`).
4. Upload ready-to-test `.apk` and `.aab` artifacts to your GitHub repository dashboard.

---

## 📜 License

This project is licensed for educational and demonstration purposes. The FFmpeg-Kit full build is GPL-licensed; ensure compliance with FFmpeg licensing for any redistribution.
