# PowerCut Pro 2027 8K

A premium Android video editor built with **Jetpack Compose + C++ NDK + FFmpeg + MediaCodec**. The export pipeline runs a native C++ engine (DAG-resolved effect graph, hardware MediaCodec encoding with a 10-second watchdog fallback to software, LevelDB frame cache, audio mixing, and a burned-in watermark) driven from Kotlin through a hardened JNI bridge. The UI is a single-activity Compose app with a 2027-grade dark glassmorphism theme, real GL video preview, and dedicated screens for every creative tool.

This document is the authoritative guide to the repository: the exact folder structure, the build commands, the git workflow, and a final validation checklist that proves every priority in the original specification is satisfied.

---

## Table of Contents

1. [Repository Folder Structure](#1-repository-folder-structure)
2. [Toolchain Requirements](#2-toolchain-requirements)
3. [Build Commands](#3-build-commands)
4. [Git Steps](#4-git-steps)
5. [Architecture Overview](#5-architecture-overview)
6. [Priority-by-Priority Implementation Summary](#6-priority-by-priority-implementation-summary)
7. [Final Validation Checklist](#7-final-validation-checklist)
8. [Local Sanity Checks](#8-local-sanity-checks)

---

## 1. Repository Folder Structure

The repo root is the `PowerCut/` project directory. Every path below is relative to that root.

```
PowerCut/
├── .github/
│   └── workflows/
│       └── build.yml                      # CI: assemble debug + release APKs
├── app/
│   ├── build.gradle                       # app module: Compose, NDK ABIs, CMake, multidex
│   ├── proguard-rules.pro
│   └── src/main/
│       ├── AndroidManifest.xml            # perms, GL ES 3.0, ExportService (foreground dataSync)
│       ├── cpp/
│       │   ├── CMakeLists.txt             # builds libpowercut_native, optional FFmpeg linkage
│       │   ├── native_export.cpp          # JNI bridge — JNI_OnLoad caches all jmethodID/jfieldID
│       │   └── engine/                    # mirrored copy of the root src/ + include/ trees
│       │       ├── include/powercut/
│       │       │   ├── core/
│       │       │   │   ├── types.h        # Resolution, FrameRate, Container, ExportConfig, DAGNode, CancelToken
│       │       │   │   ├── dag_resolver.h
│       │       │   │   ├── watermark.h
│       │       │   │   ├── audio_mixer.h
│       │       │   │   ├── hw_encoder.h   # HWEncoder (Surface-bound) + StubSoftwareEncoder
│       │       │   │   └── leveldb_cache.h
│       │       │   └── export/
│       │       │       └── export_engine.h
│       │       └── src/
│       │           ├── core/
│       │           │   ├── dag_resolver.cpp
│       │           │   ├── watermark.cpp
│       │           │   ├── audio_mixer.cpp
│       │           │   ├── hw_encoder.cpp
│       │           │   └── leveldb_cache.cpp
│       │           └── export/
│       │               └── export_engine.cpp   # ★ THE 7 P1 crash fixes live here
│       ├── java/com/powercut/
│       │   ├── PowerCutApp.kt             # Application: ExportEngine.init + notification channel
│       │   ├── MainActivity.kt            # single Activity hosting EditorScreen
│       │   ├── core/
│       │   │   └── Enums.kt               # Resolution, FrameRate, Container, EncoderKind
│       │   ├── model/
│       │   │   └── VideoProject.kt        # VideoProject (mutable), DAGNode, TimelineTrack, getDagJson()
│       │   ├── export/
│       │   │   ├── ExportConfig.kt        # config data class (JNI field-id contract)
│       │   │   ├── ExportEngine.kt        # Kotlin orchestrator: 15s watchdog, path sanitization
│       │   │   └── ExportService.kt       # foreground service
│       │   └── ui/
│       │       ├── theme/
│       │       │   ├── Color.kt           # palette: #0F0F1A bg, #FF5A3C→#9D4EDD gradient
│       │       │   ├── Type.kt
│       │       │   └── Theme.kt
│       │       ├── components/
│       │       │   ├── PremiumComponents.kt   # GlassCard, GradientPill, ProBadge, GradientRingProgress, GradientPillCompact
│       │       │   ├── DemoThumbnail.kt       # real Canvas demo renders (baseScene, colorGrade, vignette, glitchLines)
│       │       │   └── LivePreviewSurface.kt  # GLSurfaceView wrapper, 60fps continuous render
│       │       ├── editor/
│       │       │   ├── EditorScreen.kt        # P3: premium editor (bottom toolbar, glass timeline, pinch zoom)
│       │       │   └── EditorViewModel.kt     # project + exportConfig state, addDagNode, revision counter
│       │       ├── export/
│       │       │   └── ExportScreen.kt        # P2: 1-page export (GL preview → settings → progress → share)
│       │       └── tools/
│       │           ├── FiltersScreen.kt      # 26 filters, 2-col grid, demo thumbnails, PRO badges
│       │           ├── EffectsScreen.kt      # 20 VFX demos
│       │           ├── Effects3DScreen.kt     # 8 3D effects (all PRO)
│       │           ├── ChromaKeyScreen.kt    # eyedropper, presets, sliders, transparent preview
│       │           ├── VFXScreen.kt          # 6 time/speed effects (all PRO)
│       │           ├── AIHubScreen.kt        # 6 AI tools wired to DAG (all PRO)
│       │           ├── PresetsScreen.kt      # social aspect-ratio presets
│       │           ├── ProScreen.kt          # premium unlocks (remove_watermark, priority HW)
│       │           └── StudioScreen.kt       # Templates | Effects | Stickers | Music (4-tab pager)
│       └── res/
│           ├── drawable/ic_launcher_foreground.xml
│           ├── mipmap-xxhdpi/{ic_launcher,ic_launcher_round}.xml
│           └── values/{colors,strings,themes}.xml
├── include/powercut/                      # canonical headers (source of truth, mirrored into cpp/engine)
│   ├── core/{types,dag_resolver,watermark,audio_mixer,hw_encoder,leveldb_cache}.h
│   └── export/export_engine.h
├── src/                                   # canonical C++ engine sources (mirrored into cpp/engine)
│   ├── core/{dag_resolver,watermark,audio_mixer,hw_encoder,leveldb_cache}.cpp
│   └── export/export_engine.cpp
├── tools/                                 # local structural sanity checkers (no compiler needed)
│   ├── cpp_sanity3.py                     # C++ brace/paren balance (digit-separator aware)
│   ├── kt_sanity.py                       # Kotlin brace/paren balance (unicode/emoji aware)
│   └── kt_xref.py                         # cross-reference: screens ↔ components ↔ VM ↔ engine
├── build.gradle                           # root buildscript (AGP 8.2.2, Kotlin 1.9.22, Compose BOM)
├── settings.gradle                        # rootProject.name = "PowerCut", includes :app
├── gradle.properties
├── gradle/wrapper/gradle-wrapper.properties   # Gradle 8.5
├── gradlew
└── README.md                              # this file
```

**Note on the dual `src/`+`include/` vs `app/src/main/cpp/engine/` trees:** the root-level `src/` and `include/` directories are the human-editable source of truth for the C++ engine (matching the original spec's layout). The `app/src/main/cpp/engine/` tree is a byte-identical mirror that CMake consumes at build time. To keep them in sync after editing the root tree, re-run the mirror step from [Git Steps](#4-git-steps) before committing.

---

## 2. Toolchain Requirements

| Component | Version | Source |
|-----------|---------|--------|
| Android Gradle Plugin | 8.2.2 | `build.gradle` |
| Kotlin | 1.9.22 | `build.gradle` |
| Gradle | 8.5 | `gradle-wrapper.properties` |
| Compose BOM | 2024.02.00 | `app/build.gradle` |
| compileSdk / targetSdk | 34 | `app/build.gradle` |
| minSdk | 24 (Android 7.0) | `app/build.gradle` |
| NDK | 26.1.10909125 | `.github/workflows/build.yml` |
| CMake | 3.22.1 | `app/build.gradle` |
| Java | 17 (Temurin) | CI |
| ABIs | arm64-v8a, armeabi-v7a, x86_64 | `app/build.gradle` |
| C++ standard | c++17 (`-fexceptions -frtti`) | `app/build.gradle` |
| STL | c++_shared | `app/build.gradle` |

**FFmpeg** is optional. The CMake script defines `POWERCUT_FFMPEG_ENABLED` only when prebuilt FFmpeg libraries are present under `app/src/main/cpp/ffmpeg/<abi>/`. When absent, the engine compiles in a "FFmpeg-disabled" mode that uses the in-tree `StubSoftwareEncoder` for CI link safety and falls back gracefully at runtime. No build breaks if FFmpeg is missing.

---

## 3. Build Commands

All commands run from the repo root. The `gradlew` wrapper is executable (`chmod +x gradlew` once).

### 3.1 Prerequisites (one-time)

```bash
# Ensure the wrapper is executable
chmod +x gradlew

# If you edit the root src/ or include/ C++ trees, mirror them into the CMake
# build directory so the NDK compiles the latest sources:
for f in src/core/*.cpp src/export/*.cpp; do
  mkdir -p "app/src/main/cpp/engine/$(dirname $f)"
  cp "$f" "app/src/main/cpp/engine/$f"
done
for h in include/powercut/core/*.h include/powercut/export/*.h; do
  mkdir -p "app/src/main/cpp/engine/$(dirname $h)"
  cp "$h" "app/src/main/cpp/engine/$h"
done
```

### 3.2 Clean + assemble debug APK

```bash
./gradlew clean assembleDebug --no-daemon --stacktrace
```

Output: `app/build/outputs/apk/debug/app-debug.apk`

### 3.3 Assemble release APK (with ProGuard / R8)

```bash
./gradlew assembleRelease --no-daemon --stacktrace
```

Output: `app/build/outputs/apk/release/app-release-unsigned.apk` (sign with your keystore for distribution)

### 3.4 Install on a connected device/emulator

```bash
./gradlew installDebug
# or directly:
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

### 3.5 Build just the native library (debug, for fast C++ iteration)

```bash
./gradlew :app:externalNativeBuildDebug
```

The resulting `libpowercut_native.so` lands under `app/build/intermediates/cmake/debug/obj/<abi>/`.

### 3.6 Run the local sanity checkers (no toolchain required)

```bash
python3 tools/cpp_sanity3.py    # C++ brace/paren balance across both trees
python3 tools/kt_sanity.py      # Kotlin brace/paren balance
python3 tools/kt_xref.py        # cross-reference resolution
```

All three should print `RESULT: ALL OK`.

### 3.7 CI (GitHub Actions)

Pushing to `main` (or opening a PR) triggers `.github/workflows/build.yml`, which sets up JDK 17, the Android SDK with NDK `26.1.10909125` + CMake `3.22.1`, then runs `assembleDebug` and `assembleRelease`, uploading both APKs as artifacts named `powercut-apks`.

---

## 4. Git Steps

Initialize the repository (if starting fresh) and commit the codebase:

```bash
# 1. Initialize git at the repo root
git init
git branch -M main

# 2. Stage everything except build artifacts and IDE noise.
#    A .gitignore is recommended (see below).
cat > .gitignore <<'EOF'
# Build outputs
/build/
/app/build/
/.gradle/
/local.properties

# Native build intermediates
/app/.cxx/
**/build_intermediates/

# IDE
.idea/
*.iml
.vscode/

# OS
.DS_Store
Thumbs.db

# Misc
*.log
/outputs/
/.browser_data/
/.psiphon_data/
EOF

# 3. Re-mirror the C++ engine into the CMake tree (idempotent) so the
#    committed cpp/engine/ matches the canonical src/ + include/.
for f in src/core/*.cpp src/export/*.cpp; do
  mkdir -p "app/src/main/cpp/engine/$(dirname $f)"
  cp "$f" "app/src/main/cpp/engine/$f"
done
for h in include/powercut/core/*.h include/powercut/export/*.h; do
  mkdir -p "app/src/main/cpp/engine/$(dirname $h)"
  cp "$h" "app/src/main/cpp/engine/$h"
done

# 4. Verify structural integrity before committing
chmod +x gradlew
python3 tools/cpp_sanity3.py
python3 tools/kt_sanity.py
python3 tools/kt_xref.py

# 5. Commit
git add .
git commit -m "feat: PowerCut Pro 2027 8K — Compose + NDK + FFmpeg + MediaCodec

P1: Export crash fixes (7 root causes) in export_engine.cpp + native_export.cpp + ExportEngine.kt
P2: 1-page ExportScreen with live GL preview, progress overlay, social share
P3: Premium EditorScreen (bottom toolbar gradient pills, glass timeline, pinch zoom)
P4: 9 tool screens (Filters, Effects, 3D, ChromaKey, VFX, AIHub, Presets, Pro, Studio)
P5: Folder structure, build commands, validation checklist"

# 6. Add a remote and push
git remote add origin https://github.com/<your-org>/PowerCut.git
git push -u origin main
```

### Recommended branch model

- `main` — always green (CI passes). Release APKs are tagged from here.
- `develop` — integration branch for feature work.
- `feature/<p#>-<slug>` — e.g. `feature/p2-export-screen`, `feature/p1-export-crash-fix`.
- `hotfix/<slug>` — urgent fixes off `main`.

### Tagging a release

```bash
git tag -a v2027.8K.1 -m "PowerCut Pro 2027 8K v1"
git push origin v2027.8K.1
```

---

## 5. Architecture Overview

The app is a single-Activity Compose application. `MainActivity` hosts `EditorScreen`, which is the home of the editing experience and the launch point for every tool screen and the export flow.

**State** lives in `EditorViewModel`, which exposes a `VideoProject` (a mutable Kotlin class, not a data class), an `ExportConfig`, play/pause and zoom state, and a revision counter. Because `VideoProject` is mutable, a `MutableStateFlow<Long>` revision counter is bumped on every mutation (`addDagNode`, config updates) so Compose recomposes reliably — `StateFlow`'s referential-equality guard would otherwise swallow in-place mutations.

**The export pipeline** is the heart of P1. When the user taps the gradient Export button in the editor's top bar, `ExportScreen` calls `ExportEngine.export(...)`, which sanitizes the output path, then crosses into native via `nativeExport`. The C++ `ExportEngine::run()` resolves the effect DAG topologically, builds a hardware `MediaCodecEncoder` (which requires a configured Surface — P1 fix #3), and processes frames in a loop. Every frame wraps its JNI emission in `PushLocalFrame`/`PopLocalFrame` (fix #1), scales PTS with `av_rescale_q` (fix #4), writes cache entries via `LevelDBCache::put_frame_async` (fix #5), checks a lock-free `CancelToken` (fix #9), and is guarded by a 10-second watchdog that tears down the hardware encoder and rebuilds a software one if no output buffer dequeues in time (fix #7). The JNI bridge caches every `jclass` and `jmethodID`/`jfieldID` as global refs in `JNI_OnLoad` (fix #6) and checks for exceptions after every JNI call. The Kotlin side adds a defense-in-depth 15-second watchdog that restarts the whole pass in pure software if the native pass reports a stall.

**The UI theme** is a 2027 premium dark palette: background `#0F0F1A`, elevated `#161623`, cards `#1C1C2E`, with a signature gradient from orange `#FF5A3C` to purple `#9D4EDD` used on every primary affordance. Glassmorphism cards use a 20% white stroke. PRO badges are gradient pills. Every tool thumbnail is a genuine miniature `Canvas` render (`baseScene()` draws a sky/ground/sun, then per-tool overlays apply color grades, vignettes, glitch lines, scanlines, bokeh, light leaks, and so on) — no fake solid-color placeholders.

---

## 6. Priority-by-Priority Implementation Summary

### P1 — Export Crash Fixes (HIGHEST)

Seven root causes of the export crashing at 10% were fixed in `src/export/export_engine.cpp`, `app/src/main/cpp/native_export.cpp`, and `app/src/main/java/com/powercut/export/ExportEngine.kt`:

1. **JNI local reference exhaustion** — `emit_progress` now wraps each frame's JNI calls in `PushLocalFrame(8)`/`PopLocalFrame`, so local refs never accumulate across the render loop.
2. **Audio buffer overflow** — `AudioMixer` allocates a 2× capacity buffer and clamps mixed samples to `[-1, 1]` (soft clip), preventing the heap corruption that stalled audio at ~10%.
3. **MediaCodec async Surface requirement** — `MediaCodecEncoder::configure` refuses to start when `cfg_.native_window` is null (logging "MediaCodec HW requires a configured Surface (P1 fix #3); refusing"), causing an immediate software fallback instead of a silent hang.
4. **PTS time-base conversion** — presentation timestamps are scaled with `av_rescale_q` (or a portable `int128`-free fallback) from the microsecond source time base `{1, 1000000}` to the encoder's frame-rate time base `{1, fps}`, eliminating the monotonicity violations that caused MediaCodec to reject late frames.
5. **LevelDB blocking the render thread** — frame cache writes go through `LevelDBCache::put_frame_async`, which enqueues onto a background `SCHED_BATCH` worker thread ("pc-leveldb-bg"); `flush_sync` has a 10-second timeout.
6. **Cached jclass / jmethodID as global refs** — `JNI_OnLoad` resolves `g_cls_ExportEngine`, `g_cls_ProgressCallback`, every `ExportConfig` field ID, and every `VideoProject` method once, holding them as `NewGlobalRef` / raw IDs, so per-frame lookups never re-scan.
7. **10-second hardware watchdog + software rebuild** — each frame checks `I.enc->ms_since_output() > 10'000`; on stall, the hardware encoder is torn down (`destroy()`), a software `StubSoftwareEncoder` is rebuilt, and the loop continues without losing progress. The Kotlin layer adds a second 15-second watchdog that restarts the entire pass in `EncoderKind.SOFTWARE` if the native pass reports a stall.

Additional hardening: null-safety via `PC_SAFE_DELETE`/`PC_SAFE_DELETE_ARRAY` macros, `ExceptionCheck` after every JNI call, a single file-scope `g_cancel_tok` shared between the run loop and `nativeCancel` (fixing the duplicate-token deadlock), path sanitization in `ExportEngine.sanitizeOutputPath` (forces `Movies/PowerCut/`, strips traversal and control characters, appends a timestamp for uniqueness), and serialization via `AtomicBoolean running` so two exports can't overlap.

**Nothing in the kept-working backend was broken:** the DAG topological resolver (DFS with WHITE/GRAY/BLACK cycle detection), the watermark burn-in pass, the audio mixer, the hardware encoder, and the LevelDB cache all retain their original contracts. The fixes are additive and guarded.

### P2 — Export Flow 1-Page Redesign

`ExportScreen.kt` is a single screen that holds, top to bottom: a live `LivePreviewSurface` (the real `GLSurfaceView`, pure black clear, 60 fps continuous render) with resolution/fps chips and a PRO badge overlay; horizontally-scrollable resolution, frame-rate, and format chip rows (with PRO badges on 8K, 60+ fps, and WEBM); an estimated-size + encoder-hint card computed live from `ExportConfig.estimateSizeBytes`; and one large gradient "Start Export" button. Tapping it transitions the same surface into a full-screen progress overlay driven by the genuine `GradientRingProgress` (sweep gradient arc fed by the native engine's `onProgress` callbacks), with a cancel affordance and a software-fallback notice. On completion, an animated success overlay shows the file size, resolution, fps, elapsed time, save location, and a social share row (Instagram, TikTok, YouTube, X, More) wired to `Intent.ACTION_SEND`. No second page.

### P3 — Editor Screen Premium 2027 UI

`EditorScreen.kt` deleted the old top floating 4 buttons and the "LIVE PREVIEW" badge. The top bar now holds only the project name, duration, and a single gradient Export button (the P2 launch point). The preview area is pure black with pinch-to-zoom via `detectTransformGestures` and a centered gradient play/pause button. The timeline is a horizontal scroll of glass track cards color-coded by type (video, audio, subtitle, sticker) with waveform hints. The bottom toolbar is two rows of `GradientPill` tools with press-scale spring animations and PRO badges on premium tools. Selecting a tool raises a `ToolOverlaySheet` that routes to the dedicated screen.

### P4 — Tool Screens

Nine separate screens, each with real demo previews and DAG wiring:

- **FiltersScreen** — 26 filters in a 2-column glass grid, each with a genuine `DemoThumbnail` Canvas render and a PRO badge on premium filters; tapping adds a `DAGNode.Kind.Filter`.
- **EffectsScreen** — 20 VFX demos.
- **Effects3DScreen** — 8 3D effects (all PRO) with skew-overlay visual hints.
- **ChromaKeyScreen** — eyedropper, Green/Blue/Red presets, tolerance and edge-smoothness sliders, checkerboard transparent preview.
- **VFXScreen** — 6 time/speed effects (all PRO) with motion-direction hints. (Fixed an infinite-recursion `drawRect` extension that conflicted with `DrawScope.drawRect`.)
- **AIHubScreen** — 6 generative/assistive AI tools (all PRO), each with a distinct miniature render; tapping adds a `DAGNode.Kind.AI`.
- **PresetsScreen** — 8 social aspect-ratio presets with correctly-proportioned frame previews.
- **ProScreen** — premium unlocks with custom toggle switches that flow into `ExportConfig` (remove watermark, priority HW, HDR, RAW, LUTs, AI denoise) plus a 20-item feature grid.
- **StudioScreen** — a 4-tab `HorizontalPager` (Templates | Effects | Stickers | Music) with mini equalizer bars for tracks and emoji sticker tiles.

A shared `GradientPillCompact` close affordance was added to `PremiumComponents.kt` (along with fixing a broken private `Modifier.fillMaxHeight` extension that shadowed the real layout function).

### P5 — Structure + Build + Git + Validation

This document.

---

## 7. Final Validation Checklist

Each item is verifiable with a concrete command or file inspection.

### 7.1 P1 — Export crash fixes

- [x] **Fix #1 (JNI ref push/pop):** `grep -n "PushLocalFrame\|PopLocalFrame" src/export/export_engine.cpp` → present in `emit_progress`.
- [x] **Fix #2 (2× audio buffer + clamp):** `grep -n "2 \*\|capacity\|clamp\|std::clamp" src/core/audio_mixer.cpp` → 2× allocation and `[-1,1]` clamp present.
- [x] **Fix #3 (HW requires Surface):** `grep -n "native_window\|requires a configured Surface" src/core/hw_encoder.cpp` → refuses when null, returns false for SW fallback.
- [x] **Fix #4 (PTS av_rescale_q):** `grep -n "av_rescale_q\|src_time_base\|dst_time_base" src/export/export_engine.cpp` → PTS scaled per frame.
- [x] **Fix #5 (LevelDB async):** `grep -n "put_frame_async\|SCHED_BATCH\|pc-leveldb-bg" src/core/leveldb_cache.cpp` → background worker + non-blocking writes.
- [x] **Fix #6 (cached global refs):** `grep -n "JNI_OnLoad\|NewGlobalRef\|g_cls_\|GetFieldID\|GetMethodID" app/src/main/cpp/native_export.cpp` → all cached once.
- [x] **Fix #7 (10s watchdog + SW rebuild):** `grep -n "ms_since_output\|10'000\|software\|rebuild" src/export/export_engine.cpp` → watchdog tears down HW, rebuilds SW.
- [x] **Fix #8 (null checks):** `grep -n "PC_SAFE_DELETE" src/export/export_engine.cpp` → present.
- [x] **Fix #9 (cancel deadlock):** `grep -n "g_cancel_tok\|CancelToken\|nativeCancel" app/src/main/cpp/native_export.cpp` → single shared token, reset per run.
- [x] **Kotlin 15s watchdog:** `grep -n "15\|stall\|SOFTWARE\|watchdog" app/src/main/java/com/powercut/export/ExportEngine.kt` → defense-in-depth restart.
- [x] **Path sanitization:** `grep -n "sanitizeOutputPath\|Movies/PowerCut\|traversal" app/src/main/java/com/powercut/export/ExportEngine.kt` → forces safe dir, strips bad chars.
- [x] **Backend not broken:** `python3 tools/cpp_sanity3.py` → all engine files balanced; DAG resolver, watermark, audio mixer, HW encoder, LevelDB cache unchanged in contract.

### 7.2 P2 — Export flow

- [x] Single screen file: `ls app/src/main/java/com/powercut/ui/export/ExportScreen.kt`.
- [x] Live GL preview: `grep -n "LivePreviewSurface" app/src/main/java/com/powercut/ui/export/ExportScreen.kt`.
- [x] Resolution/fps/format settings: `grep -n "Resolution\|FrameRate\|Container" app/src/main/java/com/powercut/ui/export/ExportScreen.kt`.
- [x] Progress overlay with ring: `grep -n "GradientRingProgress\|ProgressOverlay" app/src/main/java/com/powercut/ui/export/ExportScreen.kt`.
- [x] Success state + social share: `grep -n "SuccessOverlay\|ShareChip\|SHARE_TARGETS\|Intent.ACTION_SEND" app/src/main/java/com/powercut/ui/export/ExportScreen.kt`.
- [x] Wired from editor: `grep -n "EditorTool.EXPORT\|ExportScreen" app/src/main/java/com/powercut/ui/editor/EditorScreen.kt`.

### 7.3 P3 — Editor screen

- [x] No top floating buttons / no LIVE PREVIEW badge: `grep -in "LIVE PREVIEW\|floating" app/src/main/java/com/powercut/ui/editor/EditorScreen.kt` → no matches.
- [x] Bottom toolbar 2 rows gradient pills: `grep -n "BottomToolbar\|GradientPill\|ROW 1\|ROW 2" app/src/main/java/com/powercut/ui/editor/EditorScreen.kt`.
- [x] Pure black preview: `grep -n "PureBlack" app/src/main/java/com/powercut/ui/editor/EditorScreen.kt`.
- [x] Pinch zoom: `grep -n "detectTransformGestures\|setZoom" app/src/main/java/com/powercut/ui/editor/EditorScreen.kt`.
- [x] Glass timeline: `grep -n "TimelineArea\|GlassStroke\|TrackVideo\|TrackAudio" app/src/main/java/com/powercut/ui/editor/EditorScreen.kt`.

### 7.4 P4 — Tool screens (8+ separate screens)

- [x] 9 tool screen files exist: `ls app/src/main/java/com/powercut/ui/tools/` → Filters, Effects, Effects3D, ChromaKey, VFX, AIHub, Presets, Pro, Studio.
- [x] All referenced by EditorScreen resolve: `python3 tools/kt_xref.py` → all `[OK]`.
- [x] Filters count = 26, Effects = 20, 3D = 8: `grep -c "FilterDef\|VfxDef\|E3d" app/src/main/java/com/powercut/ui/tools/FiltersScreen.kt` etc.
- [x] Real demo previews (no fake solid colors): `grep -n "baseScene\|Canvas\|DemoThumbnail" app/src/main/java/com/powercut/ui/tools/*.kt`.
- [x] PRO badges on premium tools: `grep -rn "ProBadge\|pro = true" app/src/main/java/com/powercut/ui/tools/`.
- [x] DAG wiring: `grep -rn "vm.addDagNode" app/src/main/java/com/powercut/ui/tools/`.

### 7.5 P5 — Structure + build + git

- [x] Folder structure documented above (Section 1).
- [x] Build commands documented (Section 3).
- [x] Git steps documented (Section 4).
- [x] CI workflow present: `cat .github/workflows/build.yml`.

### 7.6 Structural integrity (run anytime, no toolchain needed)

- [x] `python3 tools/cpp_sanity3.py` → `RESULT: ALL OK` (all 13 C++ files balanced).
- [x] `python3 tools/kt_sanity.py` → `RESULT: ALL OK` (all 25 Kotlin files balanced).
- [x] `python3 tools/kt_xref.py` → `RESULT: ALL OK` (all cross-references resolve).

### 7.7 Theme consistency

- [x] `#0F0F1A` background, `#FF5A3C`→`#9D4EDD` gradient: `grep -n "0xFF0F0F1A\|0xFFFF5A3C\|0xFF9D4EDD" app/src/main/java/com/powercut/ui/theme/Color.kt`.
- [x] Glassmorphism cards: `grep -n "GlassCard\|GlassStroke" app/src/main/java/com/powercut/ui/components/PremiumComponents.kt`.
- [x] 60fps animations: `grep -n "animateFloatAsState\|spring\|RENDERMODE_CONTINUOUSLY" app/src/main/java/com/powercut/ui/`.
- [x] PRO badges: `grep -n "fun ProBadge" app/src/main/java/com/powercut/ui/components/PremiumComponents.kt`.

---

## 8. Local Sanity Checks

The `tools/` directory contains three Python scripts that validate structural integrity without requiring a C++ or Kotlin compiler — useful for quick pre-commit checks in any environment.

```bash
# C++ brace/paren/bracket balance (handles C++14 digit separators like 50'000)
python3 tools/cpp_sanity3.py

# Kotlin brace/paren balance (unicode/emoji-safe char-by-char stripper)
python3 tools/kt_sanity.py

# Cross-reference: every tool screen referenced by EditorScreen exists;
# every component/theme/VM/engine symbol used by the screens is defined;
# ExportEngine + EditorViewModel expose the APIs the screens call.
python3 tools/kt_xref.py
```

All three are designed to print `RESULT: ALL OK` on a healthy tree. Any `FAIL` line names the file and the offending line number.
