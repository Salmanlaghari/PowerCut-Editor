# Crash Validation Test Plan — Overlay Export Fix (v4.3.0)

## Overview

This document defines the test matrix for validating that the overlay export crash
is permanently fixed. The fix addresses 10 root causes that previously caused 100%
crash rate when ANY edit (text overlay, image overlay, sticker, shape, filter, audio
clip) was added and then exported.

## Root Causes Fixed

| # | Root Cause | Fix |
|---|-----------|-----|
| 1 | drawtext has no fontfile → FFmpeg-Kit has no bundled font | Bundle `powercut_sans.ttf` + `getFontFile()`/`fontFileClause()` helpers |
| 2 | Double `-filter_complex` when BGM + image overlay | Unified single filter_complex builder |
| 3 | Overlay coordinates negative/out-of-bounds | Clamped to `[0, tw-w]` / `[0, th-h]` |
| 4 | `-map` flags before audio filter_complex | Moved after `-filter_complex` |
| 5 | Emoji stickers use Unicode in drawtext (no emoji font) | Converted to `drawbox`-based geometric shapes |
| 6 | `content://` URIs as overlay paths | `resolveOverlayPath()` stream-copies to temp file |
| 7 | `[0:v]copy` invalid FFmpeg syntax | Changed to `[0:v]null` |
| 8 | `format=rgba` on overlay then yuv420p base | Changed to `format=auto` |
| 9 | Recovery strips ALL overlays silently | Overlay-aware recovery (keeps overlays in Recovery 1) |
| 10 | No progress callback → stuck at 10% | `executeFFmpegWithProgress()` with `StatisticsCallback` |

## Test Environment

- **Device class**: Mid-range Android phone with 4 GB RAM
- **Android version**: 10+ (API 26+)
- **Storage**: At least 20 GB free (15 GB hard floor for long videos)
- **Battery**: At least 50% charged, temperature below 40°C at start
- **App build**: v4.3.0 (versionCode 8) debug or release APK
- **Network**: Not required for export tests

---

## Test Matrix

### TEST 1: Short Video + 5 Overlays (Smoke Test)

**Goal**: Verify that the basic overlay pipeline works end-to-end without crashing.

| Parameter | Value |
|-----------|-------|
| Video duration | 1 minute (60 sec) |
| Resolution | 1080p (1920×1080) |
| Overlays | 5 simultaneous: |
| | 1. Text overlay "Hello World" with fade_in animation |
| | 2. Image overlay (PNG, 200×200, opacity 0.8, scale 0.3) |
| | 3. Sticker: "star" (drawbox shape) |
| | 4. Color filter: "vintage" |
| | 5. Border style: "rounded" |
| Audio | Original (no BGM) |
| Expected time | 30-60 seconds |
| Expected result | Export succeeds, output file plays correctly with all overlays visible |
| Progress bar | Moves smoothly from 10% to 90% during encoding, then 100% on save |
| Pass criteria | ✅ No crash, ✅ output file > 0 bytes, ✅ all 5 overlays visible in output |

### TEST 2: Medium Video + 15 Overlays (Stress Test)

**Goal**: Verify the pipeline handles moderate duration with many simultaneous overlays.

| Parameter | Value |
|-----------|-------|
| Video duration | 30 minutes (1800 sec) |
| Resolution | 1080p (1920×1080) |
| Overlays | 15 simultaneous: |
| | 1. Text overlay "Scene 1" with typewriter animation |
| | 2. Text overlay "Scene 2" with slide_left animation |
| | 3. Image overlay (JPEG, 300×300, opacity 0.9, scale 0.25) |
| | 4. Image overlay (PNG, 150×150, opacity 0.5, scale 0.15) |
| | 5. Sticker: "heart" (drawbox shape) |
| | 6. Sticker: "fire" (drawbox shape) |
| | 7. Sticker: "crown" (drawbox shape) |
| | 8. Color filter: "cinematic" |
| | 9. Effect: "vhs" |
| | 10. 3D mask: "cinematic_bars" |
| | 11. Border style: "neon" |
| | 12. Vignette style: "soft" |
| | 13. Blend mode: "overlay" |
| | 14. BGM track (MP3, 3 min, volume 0.3) |
| | 15. Watermark image (PNG, 100×100) |
| Audio | Original + BGM mix |
| Expected time | 15-25 minutes |
| Expected result | Export succeeds, output file plays with all overlays + BGM |
| Progress bar | Moves smoothly 10% → 90% over ~20 min, then 100% |
| Thermal | Thermal throttle may trigger at ~5min intervals (2 sec sleep if ≥45°C) |
| Pass criteria | ✅ No crash, ✅ output file > 0 bytes, ✅ all 15 overlays visible, ✅ BGM audible |

### TEST 3: Long Video + 25 Overlays (Extreme Stress Test)

**Goal**: Verify the pipeline handles 60-minute duration with maximum overlay count on
a low-memory (4 GB RAM) device without OOM or thermal shutdown.

| Parameter | Value |
|-----------|-------|
| Video duration | 60 minutes (3600 sec) |
| Resolution | 1080p (1920×1080) |
| Overlays | 25 simultaneous: |
| | 1-5. Five text overlays with different animations (fade_in, bounce, zoom_in, neon_pulse, glitch_in) |
| | 6-8. Three image overlays (different sizes, opacities, positions) |
| | 9-13. Five stickers (star, heart, fire, diamond, rocket — all drawbox shapes) |
| | 14. Color filter: "teal_orange" |
| | 15. Effect: "film_grain" |
| | 16. Transition: "fade" (applied at midpoint) |
| | 17. 3D mask: "vignette" |
| | 18. Border style: "film" |
| | 19. Vignette style: "dramatic" |
| | 20. Blend mode: "screen" |
| | 21. BGM track (MP3, 5 min, volume 0.25, ducking enabled) |
| | 22. Watermark image (PNG, 80×80) |
| | 23. Audio effect: "bass_boost" |
| | 24. Speed curve: "ease_in" (1.0x → 1.2x) |
| | 25. Auto-captions: "en" (placeholder) |
| Audio | Original + BGM + bass_boost + ducking |
| Expected time | 30-50 minutes |
| Expected result | Export succeeds, output file plays with all overlays + BGM + audio effects |
| Progress bar | Moves smoothly 10% → 90% over ~40 min, then 100% |
| Thermal | Thermal throttle triggers every 5 min (2 sec sleep if ≥45°C) |
| Storage | Pre-check: requires ≥ 15 GB free (hard floor enforced) |
| Pass criteria | ✅ No crash, ✅ No OOM, ✅ No thermal shutdown, ✅ output file > 0 bytes, ✅ all overlays visible, ✅ BGM + audio effects audible |

---

## Test Execution Steps

For each test (TEST 1, TEST 2, TEST 3):

### Step 1: Prepare
1. Install v4.3.0 APK on the test device
2. Record or transfer the test video to the device
3. Ensure sufficient storage (check Settings → Storage)
4. Note the battery temperature (dial `*#*#4636#*#*` → Battery info, or use ADB:
   `adb shell dumpsys battery | grep temperature` — value is in tenths of °C, so 410 = 41°C)

### Step 2: Add Overlays
1. Open the app, import the test video
2. Add each overlay one by one using the editor UI:
   - Text: tap Text tool → enter text → select animation
   - Image: tap Image Overlay → select image from gallery → adjust opacity/scale/position
   - Sticker: tap Stickers → select sticker type
   - Filter: tap Filters → select color grade
   - Effect: tap Effects → select effect
   - etc.
3. Verify each overlay appears in the preview

### Step 3: Export
1. Tap Export button
2. Select 1080p resolution
3. Confirm export starts
4. Monitor:
   - Progress bar moves (not stuck at 10%)
   - No crash dialog ("Export failed for this video...")
   - Foreground service notification persists
   - Device does not overheat (check temperature periodically)

### Step 4: Verify Output
1. After export completes, open the output video
2. Verify:
   - Video plays without corruption
   - All overlays are visible at correct positions
   - Text overlays render with correct font (not boxes/tofu)
   - Stickers render as colored shapes (not blank)
   - BGM is audible (if applicable)
   - Audio effects are applied (if applicable)
   - Video duration matches expected (accounting for speed changes)
3. Check the output file size is reasonable (not 0 bytes, not excessively large)

### Step 5: Collect Logs
```bash
# Capture logcat during export for crash analysis
adb logcat -s "VideoProcessor" "ExportManager" "ExportForegroundService" "*:E"
```
Look for:
- `CRITICAL: Could not extract bundled font` → font extraction failed
- `ProcessAndExport failed — attempting recovery` → primary encode failed
- `Recovery 1/2/3` → recovery path triggered
- `Thermal throttle: battery at X°C` → thermal throttling active
- `Could not load font` → FFmpeg drawtext font issue
- `Cannot find a valid font` → fontfile not set correctly
- `No such filter: 'copy'` → FIX #7 regression
- `Filtering thread-related error` → filter_complex syntax error

---

## Regression Tests

After the overlay tests pass, also verify these don't break:

| Test | Description | Expected |
|------|-------------|----------|
| R1 | Raw video import → export (no overlays) | ✅ Success (instant trim or fast re-encode) |
| R2 | Trim only (no overlays) | ✅ Success (instant trim) |
| R3 | Speed change only (no overlays) | ✅ Success |
| R4 | Filter only (no overlays) | ✅ Success |
| R5 | Audio mute only | ✅ Success |
| R6 | Text overlay only (no other edits) | ✅ Success |
| R7 | Image overlay only | ✅ Success |
| R8 | Sticker only | ✅ Success |
| R9 | BGM only (no video overlays) | ✅ Success |
| R10 | Green screen + background image | ✅ Success |

---

## Pass/Fail Criteria

### Overall PASS requires:
- ✅ TEST 1 passes (smoke test)
- ✅ TEST 2 passes (stress test)
- ✅ TEST 3 passes (extreme stress test)
- ✅ All regression tests R1-R10 pass
- ✅ No "Export failed" crash dialog in any test
- ✅ No OOM (OutOfMemoryError) in logcat
- ✅ No thermal shutdown
- ✅ Progress bar moves during all exports

### Any single FAIL means:
- The fix is incomplete
- Collect logcat output and identify the failing component
- Do NOT proceed to STEP 1 (Canva-style overlay system) until all tests pass
