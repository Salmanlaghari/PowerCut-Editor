# PowerCut – Fast Video Editor
### *Sab se Tez • Sab se Taqatwar*

PowerCut is a professional-grade, blisteringly fast, and incredibly powerful native Android video editor designed for absolute performance and seamless mobile workflows.

Built 100% in pure Kotlin and Jetpack Compose, PowerCut delivers instant, watermark-free video editing, upscaling up to 8K, custom AI filters, multi-track audio mixing, precise speed ramping, and smooth video transitions—fully localized in English and Urdu with automatic RTL support.

---

## ⚡ Main Features

*   **Instant Trim (Sab se Tez)**: Slice videos in milliseconds without re-encoding using smart stream copy technology.
*   **4K/8K Extreme HD Exports (Sab se Taqatwar)**: Hardware-accelerated upscaling and exporting up to 8K resolutions.
*   **Speed Control (0.1x to 16.0x)**: Smooth speed ramping and time-lapse creation with synchronized audio pitching.
*   **Transitions (Fade, Slide, Dissolve)**: Beautiful and cinematic clip-to-clip transition filters.
*   **Multi-Track Audio Mixer**: Layer background music, adjust independent audio track volumes, or mute audio streams.
*   **Auto-Captions**: Instant dynamic captions generated in English and Urdu.
*   **Silence Remover**: Smart silence-gate volume detection and automatic silent segment removal.
*   **Preset Aspect Ratios**: One-click crop/padding presets for modern formats:
    *   **9:16** (TikTok / Instagram Reels / YouTube Shorts)
    *   **16:9** (Widescreen Landscape)
    *   **1:1** (Square post)
    *   **4:5** (Portrait grid)
*   **Bilingual Dynamic Interface**: Seamless toggle between English and Urdu with full dynamic LTR/RTL layout direction switching.
*   **Strict No Watermark Promise**: Export premium, high-fidelity media without intrusive branding or watermarks.
*   **Pure Native Architecture**: Built using pure Android APIs. No WebViews, no hybrid wrappers, no Python. Runs fully offline!

---

## 🛠️ Technology Stack

*   **Language**: Kotlin
*   **UI Framework**: Jetpack Compose (Declarative, Reactive Design)
*   **Player & Engine**: Media3 & ExoPlayer (High-performance playback)
*   **Transcoding**: FFmpeg-Kit (GPL/LGPL Multi-Core & ARM NEON optimized)
*   **Filters**: GPUImage (GPU-accelerated live shader preview rendering)
*   **Dependency Injection**: Hilt & Dagger
*   **Asynchronous Flow**: Kotlin Coroutines & Flow
*   **Min SDK**: 26 (Android 8.0 Oreo)
*   **Target SDK**: 34 (Android 14)

---

## 📁 Project Structure

The project strictly follows the clean architecture MVVM pattern:

```text
app/src/main/java/com/powercut/editor/
├─ core/
│  ├─ base/       # Base Resource wrappers and states
│  ├─ di/         # Hilt Modules (Dispatchers, App Context providers)
│  └─ utils/      # Dynamic Locale/RTL LanguageHelper, local UriHelper, File utilities
├─ data/
│  ├─ VideoProject.kt      # Unified Project metadata model (Speed, Trim, Audio, Captions)
│  └─ ProjectRepository.kt  # Shared State repository for screen sync
├─ domain/
│  ├─ ai/         # AI Filters and GPUImage shader mappings
│  ├─ export/     # ExportManager orchestrating high-quality transcoding pipelines
│  ├─ processing/ # VideoProcessor running multi-core NEON-optimized FFmpeg operations
│  └─ timeline/   # TimelineHelper for formatted playback millisecond rulers
└─ ui/
   ├─ theme/      # Dark Cyberpunk-Neon brand typography, color palette, and styles
   ├─ home/       # Home screen with video selection launcher
   ├─ editor/     # Main Editor workspace with ExoPlayer, real-time Compose filter canvases, speed sliders, and mixers
   └─ export/     # Export progress animations and success readouts
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
To build the debug package:
```bash
./gradlew assembleDebug
```
The resulting APK can be found under `app/build/outputs/apk/debug/`.

### Build Release APK & App Bundle (AAB)
To package the app for store submission:
```bash
./gradlew assembleRelease bundleRelease
```
The resulting release builds can be found under `app/build/outputs/apk/release/` and `app/build/outputs/bundle/release/`.

---

## 🤖 GitHub Actions CI/CD Pipeline

The project includes an automated workflow located in `.github/workflows/build.yml`. On every push or pull request to any branch, the workflow will automatically:
1. Initialize JDK 21 and cache Gradle packages.
2. Compile and assemble the APKs and AAB bundles.
3. Automatically sign release builds if secure secrets are defined (`SIGNING_KEY`, `ALIAS`, `KEY_STORE_PASSWORD`, `KEY_PASSWORD`).
4. Upload ready-to-test `.apk` and `.aab` artifacts to your GitHub repository dashboard.
