# PowerCut 5-Priority Fix Plan

## PRIORITY 1 — Fix Export Crash (APP CLOSES DURING EXPORT)
- [x] 1.1 Read current export_engine.cpp, native_export.cpp, ExportEngine.kt, ExportScreen.kt
- [x] 1.2 Fix export_engine.cpp: null checks, ref counting, mutex on cancel, safe encoder flush, avio_open check, no double-free
- [x] 1.3 Fix native_export.cpp: JNI exception checks + progress callback JNI bridge
- [x] 1.4 Fix ExportEngine.kt: thread-safe progress (Handler.post), path sanitize, onProgressCallback JNI method
- [x] 1.5 Fix EditorScreen.kt: cancel guard, isFinishing, DisposableEffect cleanup, cancel button

## PRIORITY 2 — Fix Export Screen: Remove Blue Placeholder
- [x] 2.1 Replace blue Box with dark gradient (#1A1A2E → #12121F) + centered play icon + project title white
- [x] 2.2 Keep Duration: MM:SS label, rounded corners 24dp, same card size

## PRIORITY 3 — Simplify Export Progress Screen
- [x] 3.1 Keep only: circular progress (120dp orange→purple gradient), big % text (white 36sp), thin linear bar (gradient), "Exporting video..." (gray 14sp), Cancel button
- [x] 3.2 DELETE all stage text, step dots, "Processing high-speed output pipeline...", "Decoding video frames..."

## PRIORITY 4 — Move Top 4 Buttons to Editor + Make Real
- [x] 4.0 Read HomeScreen.kt + EditorScreen.kt + MainActivity.kt + existing premium screens
- [x] 4.1 REMOVE 4 floating buttons from HomeScreen.kt (call block + params + import)
- [x] 4.2 ADD fixed top 4-button action row in EditorScreen.kt (AI Hub, Presets, Pro, Studio) + 4 callback params
- [x] 4.3-4.6 Wire callbacks in MainActivity EditorScreen call (state flags + overlays already exist)
- [x] 4.7 Remove HomeScreen premium wiring from MainActivity (onAiHub etc. in HomeScreen call)

## PRIORITY 5 — Stickers / Effects Cards: Real 3D Glass UI
- [x] 5.1 Create EffectsScreen.kt — 70 real effects, 3D glass cards, gradient border + checkmark on select, applies to timeline via viewModel.updateSelectedEffect
- [x] 5.2 Create StickersScreen.kt — 61 real stickers, 3D glass cards with selected state, applies via viewModel.updateStickerType
- [x] 5.3 Wire EffectsScreen + StickersScreen into MainActivity (state flags + overlays) + EditorScreen bottom-toolbar entry buttons

## SHIP
- [x] 6.1 Stage, commit, push to branch fix/export-crash-ui-ai-presets-pro-studio
- [ ] 6.2 Create PR, watch CI build, report results
