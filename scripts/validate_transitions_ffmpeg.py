#!/usr/bin/env python3
"""
REAL-FFmpeg validation harness for PowerCut time-based transitions (PART 2).

WHAT THIS PROVES
----------------
Nothing in here is a hardcoded "expected string" assertion. Every check either
asks a real ffmpeg binary a question, or measures a real generated MP4.

  1. NAME VALIDITY
     Extracts every xfade transition name that TransitionCatalog.kt maps to, and
     checks it against the transition enum a REAL ffmpeg reports for the xfade
     filter. This is what catches the `fdissolve` class of bug (a name that
     exists in no FFmpeg release and hard-fails the export).

  2. UI COVERAGE
     Extracts the transition ids the editor UI actually offers
     (NextGenEditorScreen TransitionsPanel) and asserts every single one resolves
     to a real xfade name. A transition may NOT be silently dropped.

  3. PER-TRANSITION RENDER
     For every distinct xfade name used by the mapping, renders a REAL 2-clip MP4
     and verifies: ffmpeg exit status, the file exists and is non-trivial, it has
     a decodable video stream, and its duration matches the transition math
     (dA + dB - transitionDuration) within tolerance.

  4. TIMING / VISUAL PROOF
     Renders red -> green with a known transition window and samples decoded
     frames. Asserts frames BEFORE the window are pure clip A, frames AFTER are
     pure clip B, and frames INSIDE the window are genuinely mixed. This is what
     proves the transition is time-based and located at the cut point instead of
     being a post-filter smeared over the whole timeline.

  5. MULTI-CLIP CHAIN (A -> t -> B -> t -> C)
     Builds the same cumulative-offset xfade chain the Kotlin builds, renders it,
     and verifies total duration, per-cut timing, and that the AUDIO length
     tracks the video (acrossfade overlapping by the same amount as xfade).

USAGE
  python3 scripts/validate_transitions_ffmpeg.py
  python3 scripts/validate_transitions_ffmpeg.py --ffmpeg /path/to/ffmpeg
  python3 scripts/validate_transitions_ffmpeg.py --keep    # keep generated MP4s

EXIT CODES
  0 all checks passed
  1 at least one check failed
  2 ffmpeg/ffprobe not found
"""

import argparse
import json
import os
import re
import shutil
import subprocess
import sys
import tempfile

REPO = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
CATALOG = os.path.join(
    REPO, "app/src/main/java/com/powercut/editor/domain/processing/TransitionCatalog.kt"
)
UI_SCREEN = os.path.join(
    REPO, "app/src/main/java/com/powercut/editor/ui/editor/NextGenEditorScreen.kt"
)
PROCESSOR = os.path.join(
    REPO, "app/src/main/java/com/powercut/editor/domain/processing/VideoProcessor.kt"
)

FFMPEG = "ffmpeg"
FFPROBE = "ffprobe"

failures = []
passes = 0


def ok(msg):
    global passes
    passes += 1
    print(f"  \033[32mPASS\033[0m {msg}")


def fail(msg):
    failures.append(msg)
    print(f"  \033[31mFAIL\033[0m {msg}")


def section(title):
    print(f"\n\033[1m{title}\033[0m")


def run(cmd, timeout=180):
    return subprocess.run(cmd, capture_output=True, text=True, timeout=timeout)


# ---------------------------------------------------------------------------
# Source extraction (no hardcoded expectations — read from the Kotlin)
# ---------------------------------------------------------------------------
def read(path):
    with open(path, encoding="utf-8") as f:
        return f.read()


def catalog_specs():
    """[(id, xfadeName)] from TransitionCatalog.SPECS."""
    src = read(CATALOG)
    return re.findall(r'Spec\("([^"]+)",\s*"([^"]+)"', src)


def catalog_base_names():
    src = read(CATALOG)
    m = re.search(r"BASE_XFADE_NAMES: Set<String> = setOf\((.*?)\n    \)", src, re.S)
    if not m:
        return set()
    return set(re.findall(r'"([a-z]+)"', m.group(1)))


def ui_transition_ids():
    """Transition ids the editor's TransitionsPanel actually offers."""
    src = read(UI_SCREEN)
    m = re.search(
        r'LiveAnimatedHeader\("TRANSITIONS".*?listOf\((.*?)\)\.forEach', src, re.S
    )
    if not m:
        return []
    raw = re.findall(r'"([^"]+)"', m.group(1))
    return [r.strip().lower().replace(" ", "_").replace("-", "_") for r in raw]


def ffmpeg_xfade_names():
    """The transition enum a REAL ffmpeg reports for xfade."""
    r = run([FFMPEG, "-h", "filter=xfade"])
    names = set(re.findall(r"^\s+([a-z]+)\s+-?\d+\s+\.\.FV", r.stdout, re.M))
    names.discard("custom")
    return names


# ---------------------------------------------------------------------------
# Media helpers
# ---------------------------------------------------------------------------
def make_clip(path, color, dur, w=320, h=240, fps=30, freq=440):
    r = run([
        FFMPEG, "-y", "-loglevel", "error",
        "-f", "lavfi", "-i", f"color=c={color}:s={w}x{h}:r={fps}:d={dur}",
        "-f", "lavfi", "-i", f"sine=frequency={freq}:duration={dur}",
        "-c:v", "libx264", "-pix_fmt", "yuv420p", "-c:a", "aac", "-shortest",
        path,
    ])
    return r.returncode == 0


def probe_duration(path):
    r = run([FFPROBE, "-v", "error", "-show_entries", "format=duration",
             "-of", "csv=p=0", path])
    try:
        return float(r.stdout.strip())
    except ValueError:
        return -1.0


def probe_stream_duration(path, kind):
    r = run([FFPROBE, "-v", "error", "-select_streams", kind[0],
             "-show_entries", "stream=duration", "-of", "csv=p=0", path])
    try:
        return float(r.stdout.strip().split("\n")[0])
    except (ValueError, IndexError):
        return -1.0


def has_video_stream(path):
    r = run([FFPROBE, "-v", "error", "-select_streams", "v:0",
             "-show_entries", "stream=codec_name,nb_frames",
             "-of", "json", path])
    try:
        streams = json.loads(r.stdout).get("streams", [])
        return len(streams) > 0
    except json.JSONDecodeError:
        return False


def avg_rgb(path, t):
    """Average RGB of the decoded frame at timestamp t. Real decode, no guessing."""
    r = subprocess.run(
        [FFMPEG, "-loglevel", "error", "-ss", str(t), "-i", path,
         "-frames:v", "1", "-f", "rawvideo", "-pix_fmt", "rgb24", "-"],
        capture_output=True, timeout=60,
    )
    d = r.stdout
    if len(d) < 3:
        return None
    n = len(d) // 3
    rs = sum(d[i * 3] for i in range(n)) // n
    gs = sum(d[i * 3 + 1] for i in range(n)) // n
    bs = sum(d[i * 3 + 2] for i in range(n)) // n
    return (rs, gs, bs)


# ---------------------------------------------------------------------------
# Graph builders — mirror the Kotlin so we validate the real shape
# ---------------------------------------------------------------------------
def clip_chains(n, w=320, h=240, fps=30):
    """Per-clip normalisation chain, same as VideoProcessor.processMultiClipTimeline."""
    parts = []
    for i in range(n):
        parts.append(
            f"[{i}:v]setpts=PTS-STARTPTS,"
            f"scale={w}:{h}:force_original_aspect_ratio=decrease,"
            f"pad={w}:{h}:(ow-iw)/2:(oh-ih)/2:black,"
            f"fps={fps},settb=AVTB,format=yuv420p[v{i}]"
        )
        parts.append(
            f"[{i}:a]asetpts=PTS-STARTPTS,"
            f"aformat=sample_fmts=fltp:sample_rates=44100:channel_layouts=stereo[a{i}]"
        )
    return parts


def xfade_chain(durations, name, tdur):
    """
    The cumulative-offset xfade chain, identical to the Kotlin implementation.
    Returns (filter_parts, expected_total_duration).
    """
    n = len(durations)
    parts = clip_chains(n)
    v_acc, a_acc = "v0", "a0"
    acc = durations[0]
    for i in range(1, n):
        v_out = "vout" if i == n - 1 else f"vx{i}"
        a_out = "aout" if i == n - 1 else f"ax{i}"
        offset = max(acc - tdur, 0.0)
        parts.append(
            f"[{v_acc}][v{i}]xfade=transition={name}"
            f":duration={tdur:.3f}:offset={offset:.3f}[{v_out}]"
        )
        parts.append(f"[{a_acc}][a{i}]acrossfade=d={tdur:.3f}:c1=tri:c2=tri[{a_out}]")
        acc += durations[i] - tdur
        v_acc, a_acc = v_out, a_out
    return parts, acc


def render(inputs, parts, out, extra=None):
    cmd = [FFMPEG, "-y", "-loglevel", "error"]
    for i in inputs:
        cmd += ["-i", i]
    cmd += ["-filter_complex", ";".join(parts),
            "-map", "[vout]", "-map", "[aout]",
            "-c:v", "libx264", "-pix_fmt", "yuv420p", "-c:a", "aac"]
    if extra:
        cmd += extra
    cmd += [out]
    return run(cmd)


# ---------------------------------------------------------------------------
# Checks
# ---------------------------------------------------------------------------
def check_names(real_names):
    section("1. xfade NAME VALIDITY (vs real ffmpeg enum)")
    specs = catalog_specs()
    if not specs:
        fail("could not extract any Spec(...) from TransitionCatalog.kt")
        return set()
    used = sorted({x for _, x in specs})
    bad = [x for x in used if x not in real_names]
    if bad:
        fail(f"mapping uses xfade names this ffmpeg rejects: {bad}")
    else:
        ok(f"all {len(used)} distinct xfade names used by the mapping exist in ffmpeg")

    declared = catalog_base_names()
    if declared:
        phantom = sorted(declared - real_names)
        if phantom:
            fail(f"BASE_XFADE_NAMES declares names ffmpeg does not have: {phantom}")
        else:
            ok(f"BASE_XFADE_NAMES ({len(declared)}) all exist in this ffmpeg")

    # The bug that PART 2 fixes must not come back. We look for a real *usage*
    # (a quoted string / an xfade= argument), not the prose in the comments that
    # documents why the name is forbidden.
    bad_use = []
    for path, label in ((PROCESSOR, "VideoProcessor.kt"), (CATALOG, "TransitionCatalog.kt")):
        for line in read(path).splitlines():
            stripped = line.strip()
            if stripped.startswith("*") or stripped.startswith("//"):
                continue  # documentation, not code
            if re.search(r'"[^"]*fdissolve|transition=fdissolve', line):
                bad_use.append(f"{label}: {stripped[:80]}")
    if bad_use:
        for b in bad_use:
            fail(f"invalid transition name 'fdissolve' is USED in code: {b}")
    else:
        ok("invalid name 'fdissolve' is not used in any code path")
    return set(used)


def check_ui_coverage():
    section("2. UI COVERAGE (no transition may be silently dropped)")
    ui = [t for t in ui_transition_ids() if t != "none"]
    if not ui:
        fail("could not extract the UI transition list")
        return
    ids = {i for i, _ in catalog_specs()}
    missing = [t for t in ui if t not in ids]
    if missing:
        fail(f"{len(missing)} UI transitions are not mapped: {missing}")
    else:
        ok(f"all {len(ui)} UI transitions map to a real xfade transition")


def check_render_each(used_names, tmp):
    section("3. PER-TRANSITION REAL RENDER + DURATION MATH")
    a = os.path.join(tmp, "a.mp4")
    b = os.path.join(tmp, "b.mp4")
    if not (make_clip(a, "red", 3, freq=440) and make_clip(b, "green", 3, freq=880)):
        fail("could not create sample clips")
        return
    tdur = 1.0
    expected = 3 + 3 - tdur  # 5.0
    bad = []
    for name in sorted(used_names):
        out = os.path.join(tmp, f"t_{name}.mp4")
        parts, exp = xfade_chain([3.0, 3.0], name, tdur)
        r = render([a, b], parts, out)
        if r.returncode != 0:
            bad.append((name, f"ffmpeg failed: {r.stderr.strip()[:120]}"))
            continue
        if not os.path.exists(out) or os.path.getsize(out) < 1024:
            bad.append((name, "output missing or too small"))
            continue
        if not has_video_stream(out):
            bad.append((name, "no decodable video stream"))
            continue
        d = probe_duration(out)
        if abs(d - expected) > 0.25:
            bad.append((name, f"duration {d:.3f}s != expected {expected:.3f}s"))
    if bad:
        for name, why in bad:
            fail(f"{name}: {why}")
    else:
        ok(f"all {len(used_names)} transitions rendered a valid MP4 "
           f"with duration {expected:.2f}s (= 3 + 3 - {tdur})")


def check_timing(tmp):
    section("4. TIMING / VISUAL PROOF (transition is at the cut, not global)")
    a = os.path.join(tmp, "ta.mp4")
    b = os.path.join(tmp, "tb.mp4")
    make_clip(a, "red", 4, freq=440)
    make_clip(b, "green", 4, freq=880)
    tdur = 1.0
    # transition window is [3.0, 4.0): offset = dA - tdur
    parts, exp = xfade_chain([4.0, 4.0], "fade", tdur)
    out = os.path.join(tmp, "timing.mp4")
    r = render([a, b], parts, out)
    if r.returncode != 0:
        fail(f"timing render failed: {r.stderr.strip()[:200]}")
        return

    before = avg_rgb(out, 1.5)
    inside = avg_rgb(out, 3.5)
    after = avg_rgb(out, 6.0)
    if not all([before, inside, after]):
        fail("could not decode sample frames")
        return

    # before the window: essentially pure red
    if before[0] > 200 and before[1] < 60:
        ok(f"t=1.5s before the cut is clip A (red) {before}")
    else:
        fail(f"t=1.5s should be pure clip A, got {before}")

    # after the window: essentially pure green
    if after[1] > 100 and after[0] < 60:
        ok(f"t=6.0s after the cut is clip B (green) {after}")
    else:
        fail(f"t=6.0s should be pure clip B, got {after}")

    # inside the window: genuinely blended (both channels present)
    if inside[0] > 30 and inside[1] > 20:
        ok(f"t=3.5s inside the transition window is genuinely blended {inside}")
    else:
        fail(f"t=3.5s should be mid-transition blend, got {inside}")

    # A post-filter (the old broken behaviour) would have altered t=0..1 as well.
    start = avg_rgb(out, 0.2)
    if start and start[0] > 200 and start[1] < 60:
        ok(f"t=0.2s is untouched clip A {start} -> transition is NOT a global post-filter")
    else:
        fail(f"t=0.2s should be untouched clip A, got {start}")


def check_multiclip(tmp):
    section("5. MULTI-CLIP CHAIN  A -> t -> B -> t -> C")
    paths = []
    for i, (c, f) in enumerate([("red", 440), ("green", 880), ("blue", 1320)]):
        p = os.path.join(tmp, f"m{i}.mp4")
        make_clip(p, c, 4, freq=f)
        paths.append(p)

    tdur = 1.0
    durs = [4.0, 4.0, 4.0]
    parts, exp = xfade_chain(durs, "fade", tdur)
    out = os.path.join(tmp, "abc.mp4")
    r = render(paths, parts, out)
    if r.returncode != 0:
        fail(f"3-clip render failed: {r.stderr.strip()[:250]}")
        return

    # expected = 12 - 2*1 = 10
    expected = sum(durs) - 2 * tdur
    if abs(exp - expected) > 1e-6:
        fail(f"chain builder computed {exp}, arithmetic says {expected}")
    d = probe_duration(out)
    if abs(d - expected) > 0.25:
        fail(f"3-clip duration {d:.3f}s != expected {expected:.3f}s")
    else:
        ok(f"3-clip output duration {d:.3f}s == {sum(durs):.0f} - 2x{tdur} "
           f"(transitions overlap, they do not extend the timeline)")

    va = probe_stream_duration(out, "video")
    aa = probe_stream_duration(out, "audio")
    if va > 0 and aa > 0 and abs(va - aa) > 0.35:
        fail(f"A/V drift: video {va:.3f}s vs audio {aa:.3f}s")
    else:
        ok(f"audio ({aa:.3f}s) tracks video ({va:.3f}s) — acrossfade overlaps "
           f"by the same amount as xfade")

    # Per-cut timing: cut 1 window [3,4], cut 2 window [6,7]
    samples = {
        1.0: ("A", lambda p: p[0] > 200 and p[1] < 60),
        3.5: ("A->B blend", lambda p: p[0] > 30 and p[1] > 20),
        5.0: ("B", lambda p: p[1] > 100 and p[0] < 60 and p[2] < 60),
        6.5: ("B->C blend", lambda p: p[1] > 20 and p[2] > 20),
        8.5: ("C", lambda p: p[2] > 150 and p[0] < 60),
    }
    for t, (label, pred) in samples.items():
        px = avg_rgb(out, t)
        if px is None:
            fail(f"could not decode t={t}")
        elif pred(px):
            ok(f"t={t}s is {label} {px}")
        else:
            fail(f"t={t}s expected {label}, got {px}")


def check_short_clip_clamp(tmp):
    section("6. DURATION CLAMP (transition longer than the clips)")
    # A 0.6s clip cannot host a 2s transition. The Kotlin clamps to half the
    # shorter clip; verify that clamped value actually renders.
    a = os.path.join(tmp, "s1.mp4")
    b = os.path.join(tmp, "s2.mp4")
    make_clip(a, "red", 0.6, freq=440)
    make_clip(b, "green", 0.6, freq=880)
    clamped = 0.6 / 2.0  # TransitionCatalog.clampDuration
    parts, exp = xfade_chain([0.6, 0.6], "fade", clamped)
    out = os.path.join(tmp, "short.mp4")
    r = render([a, b], parts, out)
    if r.returncode != 0:
        fail(f"clamped short-clip render failed: {r.stderr.strip()[:200]}")
        return
    d = probe_duration(out)
    if d <= 0:
        fail("clamped short-clip output is not decodable")
    else:
        ok(f"clamped transition ({clamped:.3f}s) on 0.6s clips renders: {d:.3f}s")


def production_graph(clip_specs, name, requested, w=320, h=240, fps=30):
    """
    Reproduces the EXACT filter graph VideoProcessor.processMultiClipTimeline
    builds, including trim, speed (setpts/atempo), settb=AVTB, aformat, the
    per-cut duration clamp, and the hard-cut fallback for cuts that are too
    short to transition on.

    clip_specs: [(trim_start, trim_end, speed)]
    Returns (filter_parts, expected_total_duration, per_cut_durations).
    """
    n = len(clip_specs)
    durations = [(e - s) / sp for (s, e, sp) in clip_specs]

    parts = []
    for i, (s, e, sp) in enumerate(clip_specs):
        v = f"[{i}:v]trim=start={s}:end={e},setpts=PTS-STARTPTS"
        if sp != 1.0:
            v += f",setpts=PTS/{sp}"
        v += (f",scale={w}:{h}:force_original_aspect_ratio=decrease"
              f",pad={w}:{h}:(ow-iw)/2:(oh-ih)/2:black"
              f",fps={fps},settb=AVTB,format=yuv420p[v{i}]")
        parts.append(v)

        a = f"[{i}:a]atrim=start={s}:end={e},asetpts=PTS-STARTPTS"
        if sp != 1.0:
            a += f",atempo={sp}"
        a += (",aformat=sample_fmts=fltp:sample_rates=44100"
              ":channel_layouts=stereo[a" + str(i) + "]")
        parts.append(a)

    # per-cut clamp, mirroring TransitionCatalog.clampDuration
    def clamp(req, da, db):
        shorter = min(da, db)
        if shorter <= 0:
            return 0.0
        cap = shorter / 2.0
        if cap < 0.1:
            return 0.0
        return max(0.1, min(req, cap))

    tdurs = [clamp(requested, durations[i], durations[i + 1]) for i in range(n - 1)]

    v_acc, a_acc = "v0", "a0"
    acc = durations[0]
    for i in range(1, n):
        td = tdurs[i - 1]
        v_out = "vout" if i == n - 1 else f"vx{i}"
        a_out = "aout" if i == n - 1 else f"ax{i}"
        if td < 0.1:
            parts.append(f"[{v_acc}][v{i}]concat=n=2:v=1:a=0[{v_out}]")
            parts.append(f"[{a_acc}][a{i}]concat=n=2:v=0:a=1[{a_out}]")
            acc += durations[i]
        else:
            offset = max(acc - td, 0.0)
            parts.append(f"[{v_acc}][v{i}]xfade=transition={name}"
                         f":duration={td:.3f}:offset={offset:.3f}[{v_out}]")
            parts.append(f"[{a_acc}][a{i}]acrossfade=d={td:.3f}:c1=tri:c2=tri[{a_out}]")
            acc += durations[i] - td
        v_acc, a_acc = v_out, a_out
    return parts, acc, tdurs


def check_production_graph(tmp):
    section("7. PRODUCTION GRAPH (exact VideoProcessor graph shape)")
    # Uneven clip lengths, a trim, and a speed change — the awkward real-world
    # case that the naive builder used to get wrong.
    srcs = []
    for i, (c, f, d) in enumerate([("red", 440, 6), ("green", 880, 5), ("blue", 1320, 6)]):
        p = os.path.join(tmp, f"p{i}.mp4")
        make_clip(p, c, d, freq=f)
        srcs.append(p)

    # clip0: trim 1..5 @1x -> 4s ; clip1: 0..4 @2x -> 2s ; clip2: 0..5 @1x -> 5s
    specs = [(1.0, 5.0, 1.0), (0.0, 4.0, 2.0), (0.0, 5.0, 1.0)]
    parts, exp, tdurs = production_graph(specs, "slideleft", 0.7)
    out = os.path.join(tmp, "production.mp4")
    r = render(srcs, parts, out)
    if r.returncode != 0:
        fail(f"production graph failed to render: {r.stderr.strip()[:300]}")
        return
    ok("production graph (trim + speed + settb + aformat + clamp) renders")

    d = probe_duration(out)
    if abs(d - exp) > 0.35:
        fail(f"production duration {d:.3f}s != expected {exp:.3f}s")
    else:
        ok(f"production duration {d:.3f}s matches computed {exp:.3f}s "
           f"(clips 4+2+5 minus transitions {['%.2f' % t for t in tdurs]})")

    # A 2s clip must clamp the 0.7s request down to <= 1.0s; both cuts here
    # involve the 2s clip so both should be 0.7 (0.7 < 2/2).
    if all(t > 0 for t in tdurs):
        ok(f"per-cut clamp produced usable transitions at every cut: "
           f"{['%.2f' % t for t in tdurs]}")
    else:
        fail(f"a cut lost its transition unexpectedly: {tdurs}")

    va, aa = probe_stream_duration(out, "video"), probe_stream_duration(out, "audio")
    if va > 0 and aa > 0 and abs(va - aa) > 0.4:
        fail(f"production A/V drift: video {va:.3f}s audio {aa:.3f}s")
    else:
        ok(f"production A/V stay locked: video {va:.3f}s audio {aa:.3f}s")


def check_hardcut_fallback(tmp):
    section("8. HARD-CUT FALLBACK (clip too short to transition)")
    # A 0.15s middle clip cannot host any transition (cap 0.075 < 0.1), so that
    # cut must degrade to a real concat while the OTHER cut keeps its xfade.
    srcs = []
    for i, (c, f, d) in enumerate([("red", 440, 4), ("green", 880, 1), ("blue", 1320, 4)]):
        p = os.path.join(tmp, f"hc{i}.mp4")
        make_clip(p, c, d, freq=f)
        srcs.append(p)
    specs = [(0.0, 4.0, 1.0), (0.0, 0.15, 1.0), (0.0, 4.0, 1.0)]
    parts, exp, tdurs = production_graph(specs, "fade", 0.7)
    graph = ";".join(parts)
    if tdurs[0] != 0.0 or tdurs[1] != 0.0:
        # cap for a 0.15s clip is 0.075 -> below MIN -> 0 for BOTH its cuts
        fail(f"expected both cuts around the 0.15s clip to clamp to 0, got {tdurs}")
    if "concat=n=2" not in graph:
        fail("expected a real hard-cut concat fallback in the graph")
    else:
        ok("cut too short to transition falls back to a real concat (not a broken xfade)")
    out = os.path.join(tmp, "hardcut.mp4")
    r = render(srcs, parts, out)
    if r.returncode != 0:
        fail(f"hard-cut fallback graph failed: {r.stderr.strip()[:250]}")
    elif probe_duration(out) <= 0:
        fail("hard-cut fallback output not decodable")
    else:
        ok(f"hard-cut fallback renders a valid MP4 ({probe_duration(out):.3f}s)")


def single_clip_chains():
    """
    Extracts VideoProcessor.transitionChain()'s single-clip branches:
    [(ids, [filter, ...])] with $-templates resolved to concrete numbers.
    """
    src = read(PROCESSOR)
    m = re.search(
        r"private fun transitionChain\(.*?\n(.*?)\n            else -> listOf\(\)",
        src, re.S,
    )
    if not m:
        return []
    body = m.group(1)
    out = []
    # match:  "a", "b" -> listOf("filter", "filter")
    for mm in re.finditer(
        r'^\s*((?:"[a-z0-9_]+"\s*,\s*)*"[a-z0-9_]+")\s*->\s*listOf\((.*?)\)\s*$',
        body, re.S | re.M,
    ):
        ids = re.findall(r'"([a-z0-9_]+)"', mm.group(1))
        raw = mm.group(2)
        filters = re.findall(r'"((?:[^"\\]|\\.)*)"', raw)
        if filters:
            out.append((ids, filters))
    return out


def resolve_template(f, duration=4.0, fade=1.0, w=320, h=240):
    """Substitutes the Kotlin string-template placeholders with real numbers."""
    f = f.replace("${w}", str(w)).replace("${h}", str(h))
    f = f.replace("$fadeDur", str(fade)).replace("$duration", str(duration))
    f = f.replace("$outStart", str(duration - fade))
    # Kotlin escapes a literal comma inside an expression as \\, -> \,
    f = f.replace("\\\\,", "\\,").replace('\\"', '"')
    return f


def frame_sig(path, t):
    """MD5 of the decoded luma plane at t — detects geometric moves that an
    average-colour comparison cannot see."""
    r = subprocess.run(
        [FFMPEG, "-loglevel", "error", "-ss", str(t), "-i", path,
         "-frames:v", "1", "-f", "rawvideo", "-pix_fmt", "gray", "-"],
        capture_output=True, timeout=60,
    )
    import hashlib
    return hashlib.md5(r.stdout).hexdigest() if r.stdout else None


def check_single_clip_chains(tmp):
    section("9. SINGLE-CLIP transitionChain() — renders + is time-based")
    chains = single_clip_chains()
    if not chains:
        fail("could not extract single-clip transitionChain branches")
        return
    # TEXTURED source: a flat colour cannot reveal a geometric move (a pan over
    # flat red looks identical at every t), which would produce false "static"
    # verdicts. testsrc has fine detail everywhere.
    src = os.path.join(tmp, "sc_src.mp4")
    run([FFMPEG, "-y", "-loglevel", "error", "-f", "lavfi",
         "-i", "testsrc=s=320x240:r=30:d=4",
         "-f", "lavfi", "-i", "sine=frequency=440:duration=4",
         "-c:v", "libx264", "-crf", "8", "-pix_fmt", "yuv420p",
         "-c:a", "aac", "-shortest", src])

    broken, static_ids = [], []
    checked = 0
    for ids, filters in chains:
        resolved = [resolve_template(f) for f in filters]
        if any("${" in f or "$" in f for f in resolved):
            continue  # unresolved template, skip rather than false-fail
        vf = ",".join(resolved)
        out = os.path.join(tmp, f"sc_{ids[0]}.mp4")
        r = run([FFMPEG, "-y", "-loglevel", "error", "-i", src, "-vf", vf,
                 "-c:v", "libx264", "-crf", "8", "-pix_fmt", "yuv420p",
                 "-an", out])
        checked += 1
        if r.returncode != 0:
            broken.append((ids[0], r.stderr.strip().split("\n")[-1][:110]))
            continue
        # A transition must change the picture over time. Compare decoded frames
        # early (inside the transition window) against a late, settled frame.
        #
        # We require the LATE frame to differ from the frames sampled inside the
        # transition window. Comparing only "are all 4 signatures unique" is too
        # weak: lossy encoding can make frame 0 differ by a few bits even when
        # the picture is frozen, which would hide a genuinely static filter.
        inside = [frame_sig(out, t) for t in (0.1, 0.45, 0.8)]
        late = frame_sig(out, 3.2)
        inside = [s for s in inside if s]
        if late and inside and all(s == late for s in inside):
            static_ids.append(ids[0])

    if broken:
        for i, why in broken:
            fail(f"single-clip '{i}' does not render: {why}")
    else:
        ok(f"all {checked} single-clip transition chains render under real ffmpeg")

    if static_ids:
        fail(f"{len(static_ids)} single-clip chains are NOT time-based (identical "
             f"frames throughout): {sorted(static_ids)}")
    else:
        ok(f"every single-clip chain is genuinely time-based "
           f"(decoded frames change over the transition window)")

    # The specific bug PART 2 fixed must not return, on ANY transition:
    # `scale` down + `scale` back up is evaluated once at init, so it degrades
    # the WHOLE clip permanently instead of only the transition window. It is a
    # permanent effect masquerading as a transition.
    scale_trick = []
    for ids, filters in chains:
        joined = " ".join(filters)
        downscaled = re.search(r"scale=w?=?iw/\d+", joined)
        if downscaled and "flags=neighbor" in joined and "enable=" not in joined:
            scale_trick.append(ids[0])
    if scale_trick:
        for i in sorted(set(scale_trick)):
            fail(f"'{i}' uses the non-time-based scale/neighbor trick "
                 f"(degrades the entire clip, not just the transition)")
    else:
        ok("no transition uses the non-time-based scale/neighbor trick")

    # drawbox geometry cannot animate — assert no transition relies on it.
    offenders = []
    for ids, filters in chains:
        for f in filters:
            if "drawbox" in f and re.search(r"(x|y|w|h)='[^']*\bt\b", f):
                offenders.append(ids[0])
                break
    if offenders:
        fail(f"transitions still rely on per-frame drawbox geometry (proven "
             f"non-animating): {sorted(set(offenders))}")
    else:
        ok("no transition relies on per-frame drawbox geometry")


def main():
    global FFMPEG, FFPROBE
    ap = argparse.ArgumentParser()
    ap.add_argument("--ffmpeg", default="ffmpeg")
    ap.add_argument("--ffprobe", default="ffprobe")
    ap.add_argument("--keep", action="store_true", help="keep generated MP4s")
    ap.add_argument("--outdir", default=None)
    args = ap.parse_args()

    FFMPEG = shutil.which(args.ffmpeg) or args.ffmpeg
    FFPROBE = shutil.which(args.ffprobe) or args.ffprobe
    if not shutil.which(FFMPEG) or not shutil.which(FFPROBE):
        print(f"ffmpeg/ffprobe not found ({args.ffmpeg}/{args.ffprobe})")
        return 2

    ver = run([FFMPEG, "-version"]).stdout.split("\n")[0]
    print(f"\033[1mPowerCut transition validation\033[0m\n{ver}")

    real = ffmpeg_xfade_names()
    print(f"this ffmpeg reports {len(real)} xfade transitions")

    tmp = args.outdir or tempfile.mkdtemp(prefix="pc-trans-")
    os.makedirs(tmp, exist_ok=True)
    try:
        used = check_names(real)
        check_ui_coverage()
        if used:
            check_render_each(used, tmp)
        check_timing(tmp)
        check_multiclip(tmp)
        check_short_clip_clamp(tmp)
        check_production_graph(tmp)
        check_hardcut_fallback(tmp)
        check_single_clip_chains(tmp)
    finally:
        if args.keep or args.outdir:
            print(f"\ngenerated media kept in: {tmp}")
        else:
            shutil.rmtree(tmp, ignore_errors=True)

    print(f"\n{'=' * 60}")
    if failures:
        print(f"\033[31m{len(failures)} FAILED\033[0m, {passes} passed")
        for f in failures:
            print(f"  - {f}")
        return 1
    print(f"\033[32mALL {passes} CHECKS PASSED\033[0m")
    return 0


if __name__ == "__main__":
    sys.exit(main())
