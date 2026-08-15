# PowerCut Transitions — PART 2 Implementation Notes

Status: **COMPLETE & VALIDATED** against a real FFmpeg binary (4.4.2).

## What was wrong (the PART 1 gap)

1. `VideoProcessor.processMultiClipTimeline()` joined clips with `concat` and
   then applied `transitionChain()` as a *post-filter* on the already-concatenated
   video. That is NOT a transition — e.g. a "slide_left" slid the whole finished
   timeline at t=0 instead of sliding clip B over clip A at the cut point.
2. `VideoProcessor.buildXfadeTransition()` was the real xfade builder but was
   **never called**, and emitted `xfade=transition=fdissolve` — a name that exists
   in **no** FFmpeg release. Any path reaching it hard-failed the export with
   `Error applying options to the filter`.
3. Many single-clip `transitionChain()` entries were **permanently applied to the
   whole clip** rather than animated (see FFmpeg capability findings below).

## What PART 2 does

* **`TransitionCatalog.kt`** — single source of truth mapping all 71 UI
  transitions (+ 22 premium/alias ids) to a REAL xfade name. Every name is in
  the FFmpeg 4.4-era floor set (43 names, enum 0..42), so it is valid on the
  bundled ffmpeg-kit-full 8.1.2 AND on any modern FFmpeg. Approximated mappings
  are explicitly flagged with a rationale in `Spec.note`.
* **Real inter-clip xfade chain** in `processMultiClipTimeline()`: builds
  `[v0][v1]xfade...[vx1]` → `[vx1][v2]xfade...[vout]` with **cumulative
  offsets** and a matching `acrossfade` audio chain so A/V stay locked.
* **`transitionDurationSec`** added to `VideoProject` (persisted, VM setter) so
  the overlap duration is user/state controllable and clamped per cut point.
* **`buildXfadeTransition()`** now delegates to `TransitionCatalog` (fdissolve
  bug fixed).
* `transitionChain()` is no longer invoked as a multi-clip post-filter. On the
  single-clip path its time-based bugs (`scale`-neighbor pixelate, static
  `drawbox` wipes) were fixed.

## Genuinely unsupported / approximated (NOT silently removed)

Per the task rules, nothing was removed or faked. Equivalent FFmpeg
implementations were used, and approximations are flagged:

* **No native zoom in/out xfade on FFmpeg 4.4** (`zoomin` only exists in
  7.1+). `zoom_in`/`zoom_out`/`zoom_burst` map to `smoothup`/`smoothdown`
  (a real inter-clip move, just not an actual zoom). `XFADE_ONLY_IN_FFMPEG_71_PLUS`
  lists `zoomin`, `fadefast`, `fadeslow`, `hlwind`, `hrwind`, `vuwind`, `vdwind`,
  `cover*`, `reveal*` — intentionally NOT emitted so the mapping stays valid on
  the 4.4 floor.
* **No rotation/cube/page-curl/spin xfade.** `spin`, `rotate_in/out`, `rotate_3d`,
  `cube`, `page_turn` map to `radial`/`squeeze*` — real transitions, documented
  as approximations.
* **No polygon/star/heart matte.** `hexagon`/`diamond`/`heart`/`star_wipe`/
  `star` map to `circlecrop`/`rectcrop`/`circleopen` (real shape reveals).
* **No glitch/static/channel-change xfade.** `glitch*`, `rgb_glitch`, `tv_static`,
  `channel_change`, `vhs_transition` map to `pixelize`/`fadegrays` (real
  break-up/desaturate). `shake*` map to sliced reveals (`hlslice`/`hrslice`).
* **Single-clip `drawbox` wipes** (`wipe*`, `curtain`, `cross`, `diagonal`,
  `triangle`, `split`) — `drawbox` geometry does NOT animate per-frame (see
  below), so they became genuinely time-based `fade`s on the single-clip path;
  the real directional/shape wipes are delivered on the multi-clip path via xfade.

## Verified FFmpeg per-frame expression support (FFmpeg 4.4)

| Construct | Per-frame animation? |
|---|---|
| `crop` x/y | YES |
| `crop` w/h | NO (output size, init only) |
| `drawbox` x/y/w/h | **NO** (measured: sin(t) bar never moved) |
| `boxblur` luma_radius expr | **NO** (Error reinitializing filters) |
| `gblur` sigma expr | **NO** (Invalid argument) |
| `eq` (eval=frame) | YES |
| `fade` | YES (native) |
| `xfade` | YES (native inter-clip) |
| `drawbox` w=0 | **fills whole frame black** (a latent bug) |

## Validation

* `scripts/validate_transitions_ffmpeg.py` — 27 real-FFmpeg checks: name
  validity, UI coverage, per-transition render, decoded-frame timing proof,
  3-clip A→B→C offsets/duration/A-V lock, duration clamp, exact production
  graph, hard-cut fallback, and a single-clip render + time-based check with
  regression guards (catches non-time-based scale/drawbox tricks via injected
  probes). **All 27 pass.**
* `scripts/validate_video_processor_ffmpeg.py` (PART 1) — still 69 filters,
  697 graphs, 0 failures.
* `app/src/test/.../TransitionCatalogTest.kt` — JVM unit tests (run in CI via
  Gradle; the sandbox Kotlin compiler is broken and the Gradle dist is not
  downloadable here, so logic was independently executed on a real JVM in Java).

## Remaining work for PART 3

* Build a proper UI control for `transitionDurationSec` (Slider) on the
  Transitions panel.
* Add a `TransitionDemoPreview` that renders the actual xfade for each id
  (currently it is a separate animated composable).
* Optionally opt into the FFmpeg 7.1+ names (`zoomin`, `reveal*`, `cover*`) via
  a runtime FFmpeg-version probe, replacing the current approximations.
* ARM64 Android runtime export verification (labels/simulator) was not possible
  in this sandbox — only real-FFmpeg desktop validation was run.
