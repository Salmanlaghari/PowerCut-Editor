# PowerCut Pro 2027 8K — Full Execution Plan
Repo root = /workspace (== the `PowerCut/` folder). Paths below are relative to /workspace.

## P0: Repo Skeleton + Build Config (Gradle/CMake)
- [x] Create root build.gradle, settings.gradle, app/build.gradle, gradle.properties, proguard
- [x] Create CMakeLists.txt + FFmpeg/MediaCodec integration config
- [x] Create AndroidManifest.xml + res base (colors, themes, strings, icons)
- [x] GitHub Actions CI + gradlew wrapper
- [x] PowerCutApp.kt + MainActivity.kt

## P1: CRITICAL — Export Crash Fixes
- [x] include/powercut/core/ (DAG, watermark, audio mix, encoder config — kept working)
- [x] include/powercut/export/ (export_engine.h with safe interfaces)
- [x] src/core/ (kept working DAG resolve, watermark, audio mix, HW encoders, LevelDB cache)
- [x] src/export/export_engine.cpp (7 crash fixes: JNI ref push/pop, 2x audio buffer, MediaCodec async surface, PTS av_rescale_q, LevelDB bg thread, cached jclass global refs, 10s watchdog SW fallback, null checks, cancel deadlock fix)
- [x] app/src/main/cpp/native_export.cpp (JNI_OnLoad cache all jmethodID/jfieldID globals, NewGlobalRef VideoProject, ExceptionCheck every JNI call)
- [x] app/src/main/java/com/powercut/export/ExportEngine.kt (try/catch all native, 15s timeout SW restart, sanitize output paths)
- [x] ExportConfig.kt + Enums.kt + VideoProject.kt model + ExportService.kt

## P2: Export Flow 1-Page Redesign
- [x] app/src/main/java/com/powercut/ui/export/ExportScreen.kt (1 page: live GL preview, resolution/fps/format chips, est. size, big Export button → progress overlay with GradientRingProgress + cancel → success overlay with social share row)
- [x] Wired into EditorScreen via EXPORT tool (gradient Export button in top bar)

## P3: Editor Screen Premium 2027 UI
- [x] app/src/main/java/com/powercut/ui/editor/EditorScreen.kt (no top floating buttons, no LIVE PREVIEW badge, bottom toolbar 2 rows gradient pills, pure black preview, pinch zoom, glass timeline)
- [x] EditorViewModel.kt (revision counter, addDagNode, exportConfig flow)

## P4: All Tools → Separate Screens
- [x] ui/tools/FiltersScreen.kt (26 filters, 2-col glass grid, demo thumbnails, PRO badges)
- [x] ui/tools/EffectsScreen.kt (20 VFX demos)
- [x] ui/tools/Effects3DScreen.kt (8 3D effects)
- [x] ui/tools/ChromaKeyScreen.kt (eyedropper, presets, sliders, live transparent preview)
- [x] ui/tools/VFXScreen.kt (time/speed effects) + fixed drawRect recursion bug
- [x] ui/tools/AIHubScreen.kt (6 AI tools wired to DAG)
- [x] ui/tools/PresetsScreen.kt (social aspect ratios)
- [x] ui/tools/ProScreen.kt (premium features → remove_watermark + priority HW encode)
- [x] ui/tools/StudioScreen.kt (Templates|Effects|Stickers|Music tabs)
- [x] Shared theme/components: ui/theme/Theme.kt, Color.kt, Type.kt, components/*.kt
- [x] Added GradientPillCompact + fixed broken fillMaxHeight extension in PremiumComponents.kt

## P5: Structure + Build + Git Steps
- [x] Output exact folder structure
- [x] Output build commands + git steps
- [x] Final validation checklist
