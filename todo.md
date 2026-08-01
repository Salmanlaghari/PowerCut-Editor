# STEP 0 — Fix Overlay Export Crash (v4.3.0)

## Root Causes (10 identified)
- [x] FIX #1: drawtext has no fontfile → bundle powercut_sans.ttf + getFontFile()/fontFileClause() helpers
- [x] FIX #2: Double -filter_complex when BGM+overlay → unified single filter_complex builder
- [x] FIX #3: Overlay coords can be negative/out-of-bounds → clamped to [0, tw-w]/[0, th-h]
- [x] FIX #4: -map flags before audio filter_complex → moved after -filter_complex
- [x] FIX #6: content:// URIs as overlay paths → resolveOverlayPath() copies to temp file
- [x] FIX #7: [0:v]copy invalid → changed to [0:v]null
- [x] FIX #8: format=rgba on overlay → changed to format=auto
- [x] FIX #9: Recovery strips ALL overlays → overlay-aware recovery
- [x] FIX #10: No progress callback → executeFFmpegWithProgress() with StatisticsCallback

## Remaining Fixes (drawtext fontfile wiring)
- [x] FIX #1 wire: Add fontFileClause() to buildTextOverlay() base drawtext
- [x] FIX #5: Convert stickerOverlay() emoji drawtext → drawbox-based shapes (bundled font has no emoji glyphs)
- [x] Add fontFileClause() to audioToVideo() drawtext (2 drawtext calls)
- [x] Add fontFileClause() to auto-captions drawtext (line ~680)
- [x] Update ExportManager.kt: pass onProgress callback to processAndExport()
- [x] Update ExportManager.kt: call cleanupOverlayTempFiles() in finally block
- [x] CRF 24 (was 23), preset veryfast, 30fps CFR, 15GB space floor, 2sec sleep/5min thermal

## Finalize
- [x] Bump version to 4.3.0 (versionCode 8) in app/build.gradle.kts
- [x] Write crash validation test plan (1min+5 overlays, 30min+15, 60min+25 on 4GB RAM)
- [ ] Commit, push to feature/fix-overlay-export-crash
- [ ] Create PR, wait for CI pass

## Steps 1-5 (NOT STARTED — after STEP 0 passes CI)
- STEP 1: Canva-style image/overlay system with 21 categories
- STEP 2: Google Flow Music (flowmusic.app) full backend integration
- STEP 3: Canva two-way integration
- STEP 4: Ad-based watermark monetization
- STEP 5: Final stability + testing checklist

## Rules
- Working dir: /workspace/temp_clone on branch feature/fix-overlay-export-crash
- NEVER run Gradle build locally (3.8GB RAM → OOM). Rely on GitHub CI.
- Push with: git push https://x-access-token:$GITHUB_TOKEN@github.com/Salmanlaghari/PowerCut-Editor.git
