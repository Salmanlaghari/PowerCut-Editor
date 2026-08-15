# PowerCut Unified Architecture Migration Plan

**Audit date:** 2026-08-15  
**Scope:** Main app (`app/src/...`) vs nextgen prototype (`nextgen-2027-8k/`)  
**Constraint:** Audit only — no files modified.

---

## 1. Executive Summary

The main app and nextgen prototype implement **two completely different export architectures** that cannot be simply merged. The main app uses a **FFmpeg-Kit pipeline** (`VideoProcessor.kt`) with 300+ filter chains, while nextgen uses a **native C++ DAG-based render engine** with HW encoder, LevelDB cache, and async compositing. Both have their own `native_export.cpp`, `CMakeLists.txt`, `VideoProject.kt`, and `ExportEngine.kt` — all with incompatible APIs and namespaces.

**Recommended strategy:** Keep the main app's Kotlin/FFmpeg-Kit pipeline as the primary export path, and progressively integrate nextgen's superior C++ components as **optional acceleration backends** (DAG resolver, HW encoder, audio mixer, watermark, LevelDB cache) behind the existing `ExportManager` abstraction.

---

## 2. Nextgen C++ Components Superior to Main App's FFmpeg-Kit Pipeline

| Component | Nextgen File(s) | Why Superior | Main App Equivalent |
|-----------|-----------------|--------------|---------------------|
| **DAG Resolver** | `nextgen-2027-8k/src/core/dag_resolver.cpp` + `include/powercut/core/dag_resolver.h` | Topological sort with cycle detection for effect graphs. Deterministic render order. Main app has no native DAG — everything is linear FFmpeg filter chains. | None (main app uses `PremiumFeatureCatalog` flat chains) |
| **Export Engine** | `nextgen-2027-8k/src/export/export_engine.cpp` + `include/powercut/export/export_engine.h` | 9 critical bug fixes: JNI local ref overflow, audio buffer miscalc, MediaCodec lock, PTS scaling, LevelDB on render thread, FindClass per-frame, 10s watchdog, null-free guards, cancel deadlock. Has async HW encoder with SW fallback. | `app/src/main/cpp/powercut/export/export_engine.h` — header-only stub, no implementation. Main app's `ExportEngine.kt` is a **deprecated no-op stub**. |
| **Audio Mixer** | `nextgen-2027-8k/src/core/audio_mixer.cpp` + `include/powercut/core/audio_mixer.h` | Native PCM float32 mixing with per-track gain, start offset, soft-clip, and channel up/down-mix. Main app relies on FFmpeg `-af` chains which are less precise for multi-track mixing. | None — main app uses `VideoProcessor` FFmpeg `-af` filter chains |
| **HW Encoder** | `nextgen-2027-8k/src/core/hw_encoder.cpp` + `include/powercut/core/hw_encoder.h` | MediaCodec (HW) / libx264 (SW) wrapper with async Surface, watchdog stall detection, and transparent fallback. Main app has no native encoder — FFmpeg-Kit uses software encoding by default on most devices. | None — main app relies entirely on FFmpeg-Kit's built-in encoders |
| **LevelDB Cache** | `nextgen-2027-8k/src/core/leveldb_cache.cpp` + `include/powercut/core/leveldb_cache.h` | Background MPSC queue render cache with `SCHED_BATCH` worker thread. Prevents ANR by offloading disk writes from render thread. Main app has no render cache. | None |
| **Watermark** | `nextgen-2027-8k/src/core/watermark.cpp` + `include/powercut/core/watermark.h` | Native RGBA alpha-blended watermark (gradient orange→purple "PRO" badge). Applied per-frame in the render loop. Main app uses FFmpeg `overlay` filter with a PNG asset — slower and less precise. | `app/src/main/cpp/powercut/core/` — no watermark implementation exists at native level |

---

## 3. Main App Features Missing from Nextgen

| Feature | Main App File(s) | Description | Nextgen Status |
|---------|-------------------|-------------|----------------|
| **PremiumFeatureCatalog** | `app/src/main/java/com/powercut/editor/domain/premium/PremiumFeatureCatalog.kt` | 300+ FFmpeg filter chains across 12 categories (basic editing, AI, text, audio, color, effects, transitions, animation, stickers, templates, social, pro). This is the **single source of truth** for all effects. | **Absent.** Nextgen has no equivalent catalog. |
| **VideoProcessor (FFmpeg pipeline)** | `app/src/main/java/com/powercut/editor/domain/processing/VideoProcessor.kt` (3644 lines) | The entire FFmpeg-Kit export engine: builds `-vf`/`-af` chains, handles SAF URIs, multi-clip timeline with xfade transitions, thermal checks, auto-recovery, 300+ effect parameters. | **Absent.** Nextgen has no FFmpeg filter chain builder. |
| **ExportForegroundService** | `app/src/main/java/com/powercut/editor/domain/export/ExportForegroundService.kt` | Foreground service with `PARTIAL_WAKE_LOCK` (4h), `WIFI_LOCK`, persistent notification, survives Activity death. Critical for long exports (15-40 min). | **Absent.** Nextgen's export runs in a coroutine scope but not in a foreground service. |
| **ExportManager** | `app/src/main/java/com/powercut/editor/domain/export/ExportManager.kt` | High-level export orchestrator: storage checks, temp file management, gallery save, quick tools (MP3→MP4, compress, slideshow, AI edit), multi-clip fallback, 1080p retry. | **Partially present.** Nextgen has `ExportEngine.kt` (Kotlin) but lacks storage checks, quick tools, gallery integration. |
| **Canvas / Stickers / Fonts** | `NextGenEditorScreen.kt` (5160 lines), `StickersScreen.kt`, `PremiumComponents.kt` | Extensive Compose Canvas drawing for stickers, emoji, text overlays, image overlays, green screen, eraser tools. Font resolution with bundled TTF. | **Partially present.** Nextgen has basic UI screens but not the full canvas/sticker/font pipeline. |
| **Preview ColorMatrix** | `NextGenEditorScreen.kt` (uses `ColorMatrix` for live preview) | Live preview applies color grades via Compose `ColorMatrix` before export. | **Absent.** Nextgen has no live preview color grading. |
| **VideoProject data model** | `app/src/main/java/com/powercut/editor/data/VideoProject.kt` (221 lines) | Rich data class with 60+ parameters: trim, speed, transitions, text overlays, stickers, 3D masks, keyframes, green screen, image editor, orientation, color curves, audio effects, HDR, social presets. | **Incompatible.** Nextgen's `VideoProject` is a mutable class with DAG nodes + timeline tracks — completely different structure. |
| **ProjectRepository / Persistence** | `app/src/main/java/com/powercut/editor/data/ProjectRepository.kt` | Save/load projects to JSON, drafts management. | **Absent.** Nextgen has no project persistence. |
| **Timeline models** | `app/src/main/java/com/powercut/editor/data/TimelineModels.kt` | Multi-track timeline with clips, trim points, track assignments. | **Partially present.** Nextgen has `TimelineTrack` but no clip-level trim/speed model. |
| **Keyframe animation** | `app/src/main/java/com/powercut/editor/data/VideoProject.kt` (keyframeTracks) + `VideoProcessor.kt` | Keyframe interpolation for position, scale, rotation, opacity, effect parameters. | **Absent.** Nextgen's EditorViewModel has basic keyframe support but no interpolation engine. |
| **TransitionCatalog** | `app/src/main/java/com/powercut/editor/domain/processing/TransitionCatalog.kt` | Transition definitions with clamp duration logic for multi-clip xfade. | **Absent.** |
| **Image editor tools** | `ImageEditorTools.kt`, `ImageStudio.kt` | 12 adjustments: brightness, contrast, saturation, blur, sharpen, temperature, vignette, grain, fade, highlights, shadows, exposure. | **Absent.** |
| **Orientation tools** | `OrientationTools.kt` | Auto-reframe, vertical safe zone, horizontal letterbox. | **Absent.** |
| **Green screen / chroma key** | `GreenScreenTool.kt` | Chroma key with threshold, background replacement. | **Absent.** |
| **Eraser tools** | `EraserTools.kt` | Brush-based eraser with tolerance, soft edge. | **Absent.** |
| **Royalty-free music** | `RoyaltyFreeMusicGenerator.kt` | Music generation/selection for background audio. | **Absent.** |
| **AI features hub** | `AIFilter.kt`, `AiFeatureHubScreen.kt` | AI auto-enhance, scene detection, auto-cut, beat sync. | **Absent.** |
| **Undo/redo** | `EditorViewModel.kt` | Snapshot-based undo/redo (max 30 states). | **Absent.** |
| **Drafts** | `DraftsScreen.kt`, `ProjectRepository.kt` | JSON draft save/load with project serialization. | **Absent.** |
| **Premium UI screens** | `EffectsScreen.kt`, `ProTierScreen.kt`, `SocialPresetScreen.kt`, etc. | Full premium feature browsing, pro tier, social presets, stickers. | **Partially present.** Nextgen has basic screens but not the full premium catalog integration. |

---

## 4. Exact Files That Need to Merge

### 4.1 C++ Engine Layer

| Action | Source File | Target Location | Notes |
|--------|-------------|-----------------|-------|
| **COPY** | `nextgen-2027-8k/src/core/dag_resolver.cpp` | `app/src/main/cpp/engine/core/dag_resolver.cpp` | New file. Topological DAG resolver. |
| **COPY** | `nextgen-2027-8k/include/powercut/core/dag_resolver.h` | `app/src/main/cpp/engine/include/powercut/core/dag_resolver.h` | New header. |
| **COPY** | `nextgen-2027-8k/src/export/export_engine.cpp` | `app/src/main/cpp/engine/export/export_engine.cpp` | New file. Full export engine with 9 P1 fixes. |
| **COPY** | `nextgen-2027-8k/include/powercut/export/export_engine.h` | `app/src/main/cpp/engine/include/powercut/export/export_engine.h` | New header. |
| **COPY** | `nextgen-2027-8k/src/core/audio_mixer.cpp` | `app/src/main/cpp/engine/core/audio_mixer.cpp` | New file. |
| **COPY** | `nextgen-2027-8k/include/powercut/core/audio_mixer.h` | `app/src/main/cpp/engine/include/powercut/core/audio_mixer.h` | New header. |
| **COPY** | `nextgen-2027-8k/src/core/hw_encoder.cpp` | `app/src/main/cpp/engine/core/hw_encoder.cpp` | New file. |
| **COPY** | `nextgen-2027-8k/include/powercut/core/hw_encoder.h` | `app/src/main/cpp/engine/include/powercut/core/hw_encoder.h` | New header. |
| **COPY** | `nextgen-2027-8k/src/core/leveldb_cache.cpp` | `app/src/main/cpp/engine/core/leveldb_cache.cpp` | New file. |
| **COPY** | `nextgen-2027-8k/include/powercut/core/leveldb_cache.h` | `app/src/main/cpp/engine/include/powercut/core/leveldb_cache.h` | New header. |
| **COPY** | `nextgen-2027-8k/src/core/watermark.cpp` | `app/src/main/cpp/engine/core/watermark.cpp` | New file. |
| **COPY** | `nextgen-2027-8k/include/powercut/core/watermark.h` | `app/src/main/cpp/engine/include/powercut/core/watermark.h` | New header. |
| **COPY** | `nextgen-2027-8k/include/powercut/core/types.h` | `app/src/main/cpp/engine/include/powercut/core/types.h` | New header. |
| **MERGE** | `nextgen-2027-8k/app/src/main/cpp/native_export.cpp` | `app/src/main/cpp/native_export.cpp` | **CONFLICT** — different JNI bridges, different Kotlin class signatures. Must reconcile. |
| **KEEP** | `app/src/main/cpp/powercut/core/dag.h` | `app/src/main/cpp/powercut/core/dag.h` | Main app's DAG types (PowerCut:: namespace). Keep for backward compat. |
| **KEEP** | `app/src/main/cpp/powercut/core/compositor.h` | `app/src/main/cpp/powercut/core/compositor.h` | Keep. |
| **KEEP** | `app/src/main/cpp/powercut/core/decoder_farm.h` | `app/src/main/cpp/powercut/core/decoder_farm.h` | Keep. |
| **KEEP** | `app/src/main/cpp/core_globals.cpp` | `app/src/main/cpp/core_globals.cpp` | Keep. |

### 4.2 Kotlin Layer

| Action | Source File | Target Location | Notes |
|--------|-------------|-----------------|-------|
| **KEEP** | `app/src/main/java/com/powercut/editor/domain/premium/PremiumFeatureCatalog.kt` | — | Keep as-is. 300+ FFmpeg chains. |
| **KEEP** | `app/src/main/java/com/powercut/editor/domain/processing/VideoProcessor.kt` | — | Keep as-is. 3644-line FFmpeg pipeline. |
| **KEEP** | `app/src/main/java/com/powercut/editor/domain/export/ExportManager.kt` | — | Keep as-is. High-level orchestrator. |
| **KEEP** | `app/src/main/java/com/powercut/editor/domain/export/ExportForegroundService.kt` | — | Keep as-is. Foreground service. |
| **KEEP** | `app/src/main/java/com/powercut/editor/data/VideoProject.kt` | — | Keep as-is. Rich data model. |
| **KEEP** | `app/src/main/java/com/powercut/editor/data/TimelineModels.kt` | — | Keep as-is. |
| **KEEP** | `app/src/main/java/com/powercut/editor/data/ProjectRepository.kt` | — | Keep as-is. |
| **KEEP** | `app/src/main/java/com/powercut/editor/ui/editor/NextGenEditorScreen.kt` | — | Keep as-is. 5160-line canvas/sticker/font pipeline. |
| **KEEP** | `app/src/main/java/com/powercut/editor/domain/processing/TransitionCatalog.kt` | — | Keep as-is. |
| **KEEP** | All premium UI screens | — | Keep as-is. |
| **REFERENCE** | `nextgen-2027-8k/app/src/main/java/com/powercut/export/ExportEngine.kt` | New: `app/src/main/java/com/powercut/editor/export/NativeExportEngine.kt` | Refactor nextgen's Kotlin export orchestrator into main app's package. Must reconcile with main app's `ExportManager`. |
| **REFERENCE** | `nextgen-2027-8k/app/src/main/java/com/powercut/model/VideoProject.kt` | Reference only | Nextgen's DAG-based model is incompatible with main app's flat model. Use as reference for future DAG migration. |
| **REFERENCE** | `nextgen-2027-8k/app/src/main/java/com/powercut/core/Enums.kt` | New: `app/src/main/java/com/powercut/editor/domain/export/EncoderEnums.kt` | Port `Resolution`, `FrameRate`, `Container`, `EncoderKind` enums to main app package. |

### 4.3 Build System

| Action | Source File | Target Location | Notes |
|--------|-------------|-----------------|-------|
| **MERGE** | `nextgen-2027-8k/app/src/main/cpp/CMakeLists.txt` | `app/src/main/cpp/CMakeLists.txt` | **CONFLICT** — nextgen builds `powercut_native` with all engine sources; main app builds `powercut` with stub-only. Must merge include paths, FFmpeg detection, and source lists. |
| **KEEP** | `app/src/main/cpp/CMakeLists.txt` | — | Keep as base, integrate nextgen sources conditionally. |
| **KEEP** | Root `CMakeLists.txt` | — | Keep as-is (desktop build). |
| **KEEP** | `app/build.gradle.kts` | — | Keep as-is. |

---

## 5. Build Conflicts

### 5.1 Namespace Conflict (HIGH RISK)

- **Main app C++** uses `namespace PowerCut { ... }` (e.g., `PowerCut::ExportEngine`, `PowerCut::DAGSegment`, `PowerCut::RGBAFrame`).
- **Nextgen C++** uses `namespace powercut::core { ... }` and `namespace powercut::export_ { ... }`.
- Both define `ExportEngine`, `DAGSegment`, `RGBAFrame`, etc. with different APIs.
- **Resolution:** Keep both namespaces. The main app's `native_export.cpp` includes `powercut/export/export_engine.h` via `#include` paths but uses `PowerCut::` types. Nextgen's `native_export.cpp` uses `powercut::` types. When merging, the JNI bridge must pick one namespace or wrap both.

### 5.2 JNI Signature Conflict (HIGH RISK)

- **Main app JNI bridge** (`app/src/main/cpp/native_export.cpp`) bridges to `com.powercut.editor.export.ExportEngine` with signatures:
  - `nativeExport(ExportConfig, VideoProject, ProgressCallback, Surface):Boolean`
  - Uses `PowerCut::ExportEngine`, `PowerCut::ExportConfig`, `PowerCut::PowerCutDAG`
- **Nextgen JNI bridge** (`nextgen-2027-8k/app/src/main/cpp/native_export.cpp`) bridges to `com.powercut.export.ExportEngine` with signatures:
  - `nativeExport(ExportConfig, VideoProject, ProgressCallback, Surface):Boolean`
  - Uses `powercut::export_::ExportEngine`, `powercut::core::ExportConfig`, `powercut::model::VideoProject`
- **Resolution:** The Kotlin `ExportEngine` classes have different package names (`com.powercut.editor.export` vs `com.powercut.export`) and different field layouts. A single unified JNI bridge must be written that bridges to the main app's `ExportManager`/`VideoProcessor` architecture. **Do not try to unify the Kotlin classes — keep them separate and add a new `NativeExportBridge.kt`.**

### 5.3 CMakeLists.txt Conflict (MEDIUM RISK)

- **Main app** builds `powercut` shared library with local copies of headers in `cpp/powercut/`. Conditionally adds `src/export/export_engine.cpp` only when `third_party/ffmpeg` and `third_party/leveldb` exist (which they don't in this repo).
- **Nextgen** builds `powercut_native` shared library with all engine sources in `cpp/engine/`. Conditionally links FFmpeg from `cpp/ffmpeg/<abi>/`.
- **Resolution:** Merge into a single `powercut_native` target that always compiles the nextgen engine sources. Remove the conditional FFmpeg/LevelDB detection from the main app's CMakeLists.txt and use nextgen's approach.

### 5.4 VideoProject Model Conflict (HIGH RISK)

- **Main app** `VideoProject` is an immutable `data class` with 60+ parameters (trim, speed, filters, text, stickers, keyframes, etc.).
- **Nextgen** `VideoProject` is a mutable class with `tracks: MutableList<TimelineTrack>` and `dag: MutableList<DAGNode>`.
- **Resolution:** Keep main app's `VideoProject` as the source of truth. The native engine should receive a serialized representation (e.g., the existing `getDagJson()` from nextgen, or a new protobuf/JSON format) rather than trying to share the Kotlin object directly.

### 5.5 include/ Directory Overlap (LOW RISK)

- Both `include/powercut/core/dag.h` (main app) and `include/powercut/core/dag_resolver.h` (nextgen) exist at the repo root.
- Both `include/powercut/export/export_engine.h` exist but with different contents.
- **Resolution:** Rename nextgen's headers to avoid collision, or place them in a subdirectory like `include/powercut/engine/`. The main app's headers in `app/src/main/cpp/powercut/` are local copies and don't conflict.

---

## 6. Concrete Migration Plan (Merge Order)

### Phase 0: Preparation (No code changes)
- [ ] Create `app/src/main/cpp/engine/` directory tree
- [ ] Create `app/src/main/java/com/powercut/editor/export/NativeExportBridge.kt` (new file)
- [ ] Create `app/src/main/java/com/powercut/editor/domain/export/EncoderEnums.kt` (new file, from nextgen)

### Phase 1: C++ Engine Integration (Low Risk)
1. **Copy nextgen C++ sources** into `app/src/main/cpp/engine/`:
   - `core/dag_resolver.cpp`, `core/audio_mixer.cpp`, `core/hw_encoder.cpp`, `core/leveldb_cache.cpp`, `core/watermark.cpp`
   - `export/export_engine.cpp`
   - All corresponding headers in `engine/include/powercut/`
2. **Update `app/src/main/cpp/CMakeLists.txt`**:
   - Change target name from `powercut` to `powercut_native`
   - Add nextgen engine sources to `POWERCUT_SOURCES`
   - Add nextgen include directories
   - Add FFmpeg detection logic from nextgen's CMakeLists.txt
   - Keep `-fexceptions`, `-frtti`, `-mcpu=cortex-a76` flags
3. **Verify compilation**: The new `powercut_native.so` must compile without linking FFmpeg/LevelDB (CI-safe fallback).

### Phase 2: JNI Bridge Unification (High Risk)
1. **Write `NativeExportBridge.kt`** in main app's package:
   - Loads `powercut_native` library
   - Provides `nativeExport(config, project, callback, surface):Boolean`
   - Bridges to nextgen's `powercut::export_::ExportEngine::run()`
   - Handles DAG serialization from main app's `VideoProject` to nextgen's format
2. **Update `ExportManager.kt`**:
   - Add a `useNativeEngine: Boolean` flag
   - When `true`, dispatch to `NativeExportBridge.nativeExport()`
   - When `false`, use existing `VideoProcessor.processAndExport()`
   - Keep FFmpeg-Kit as the default/fallback
3. **Do NOT modify** `ExportEngine.kt` (deprecated stub) or `VideoProcessor.kt`.

### Phase 3: Native Feature Extraction (Medium Risk)
1. **Integrate native watermark**: Add `Watermark::apply()` call in `VideoProcessor`'s overlay pipeline as an optional accelerated path.
2. **Integrate native audio mixer**: Add `AudioMixer::mix()` as an optional path in `VideoProcessor`'s audio pipeline.
3. **Integrate DAG resolver**: Use `DAGResolver::resolve()` for effect ordering in a future DAG-based export mode.

### Phase 4: Nextgen UI/Features Migration (Low Risk, Long Term)
1. Port nextgen's `Enums.kt` (`Resolution`, `FrameRate`, `Container`, `EncoderKind`) to main app.
2. Port nextgen's `ExportConfig.kt` data class.
3. Evaluate nextgen's UI screens for features not in main app (e.g., AIHub, ChromaKey, Effects3D).

---

## 7. Risk Assessment

| Risk | Severity | Likelihood | Mitigation |
|------|----------|------------|------------|
| Namespace collision between `PowerCut::` and `powercut::` | HIGH | Certain | Keep both namespaces; wrap nextgen types in adapter headers if needed. |
| JNI bridge incompatibility | HIGH | Certain | Write a new `NativeExportBridge.kt` instead of modifying existing JNI code. |
| VideoProject model mismatch | HIGH | Certain | Serialize to JSON/protobuf for native consumption; don't share Kotlin objects across JNI. |
| FFmpeg linking failure on CI | MEDIUM | Likely | Use nextgen's `POWERCUT_FFMPEG_ENABLED=0` fallback; engine must compile without FFmpeg. |
| LevelDB not available | MEDIUM | Likely | Nextgen already has in-memory fallback (`mem` map). Keep it. |
| Build system merge conflicts | MEDIUM | Certain | Merge CMakeLists.txt incrementally; test after each change. |
| Main app regression (FFmpeg pipeline breakage) | HIGH | Low | Keep all changes additive; never modify `VideoProcessor.kt` or `ExportManager.kt` export paths. |
| Performance regression from dual pipeline | LOW | Medium | Use feature flag `useNativeEngine` to A/B test. |

---

## 8. Files to Leave Unchanged

These files must **not** be modified during migration:

- `app/src/main/java/com/powercut/editor/domain/processing/VideoProcessor.kt`
- `app/src/main/java/com/powercut/editor/domain/export/ExportManager.kt`
- `app/src/main/java/com/powercut/editor/domain/export/ExportForegroundService.kt`
- `app/src/main/java/com/powercut/editor/domain/premium/PremiumFeatureCatalog.kt`
- `app/src/main/java/com/powercut/editor/data/VideoProject.kt`
- `app/src/main/java/com/powercut/editor/data/TimelineModels.kt`
- `app/src/main/java/com/powercut/editor/data/ProjectRepository.kt`
- `app/src/main/java/com/powercut/editor/ui/editor/NextGenEditorScreen.kt`
- `app/src/main/java/com/powercut/editor/export/ExportEngine.kt` (deprecated stub)
- All premium UI screens (`EffectsScreen.kt`, `ProTierScreen.kt`, etc.)
- All image/audio/orientation/green-screen/eraser tool files
- `app/src/main/AndroidManifest.xml`

---

## 9. Summary Statistics

| Metric | Count |
|--------|-------|
| Nextgen C++ source files to copy | 12 |
| Nextgen C++ header files to copy | 7 |
| Main app files to keep unchanged | 30+ |
| Main app files needing new wrappers | 2 |
| Build files to merge | 2 |
| High-risk conflicts | 3 (namespace, JNI, VideoProject model) |
| Medium-risk conflicts | 2 (CMake, FFmpeg linking) |
| Low-risk conflicts | 1 (include/ overlap) |

---

## 10. Recommended First Step

**Copy nextgen C++ sources into `app/src/main/cpp/engine/` and merge the CMakeLists.txt.** This is the lowest-risk step and unblocks all subsequent work. The native library can compile in stub mode (without FFmpeg/LevelDB) and be loaded by the app without changing any Kotlin code.
