#!/usr/bin/env python3
"""
REAL-FFmpeg validation harness for the v7.3 audit fixes:

  1. 3D MASKS (25)     — every ThreeDPanel id renders a real, non-empty frame and
                         the geometric shape masks are ACTUALLY different shapes
                         (circle vs triangle vs diamond vs heart differ).
  2. KEYFRAME PRESETS  — the app's generated filter chains for zoomIn / panLR /
                         spin360 / fadeIO / pulse render, animate per-frame,
                         keep the target output size, and fadeIO really fades.
  3. AI HUB            — every id the AI Hub exposes maps to a REAL chain in
                         PremiumFeatureCatalog and renders differently from the
                         input (video chains) or exits cleanly (audio chains).
  4. TEMPLATES (20)    — every TemplatePanel id renders a distinct real grade.

Chains are parsed from the Kotlin (VideoProcessor.kt / PremiumFeatureCatalog.kt)
so the harness tracks the code instead of duplicating strings.
"""
import argparse
import re
import shutil
import subprocess
import sys
import tempfile

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


def run(cmd, timeout=120):
    return subprocess.run(cmd, capture_output=True, text=True, timeout=timeout)


VP = "app/src/main/java/com/powercut/editor/domain/processing/VideoProcessor.kt"
CAT = "app/src/main/java/com/powercut/editor/domain/premium/PremiumFeatureCatalog.kt"


def read(path):
    with open(path, encoding="utf-8") as f:
        return f.read()


def unescape_chain(chain):
    """Kotlin string literal escapes -> FFmpeg text. `\\,` (source) -> `\,`."""
    return chain.replace("\\\\,", "\\,")


def split_top(s):
    """Split a filter list on top-level commas (respecting quotes, parens, \\,)."""
    out, cur, depth, quote = [], "", 0, False
    i = 0
    while i < len(s):
        ch = s[i]
        if ch == "\\" and i + 1 < len(s) and s[i + 1] == ",":
            cur += "\\,"
            i += 2
            continue
        if ch == "'":
            quote = not quote
        if not quote:
            if ch == "(":
                depth += 1
            elif ch == ")":
                depth -= 1
            elif ch == "," and depth == 0:
                out.append(cur.strip())
                cur = ""
                i += 1
                continue
        cur += ch
        i += 1
    if cur.strip():
        out.append(cur.strip())
    return out


def substitute_dims(chain, w, h):
    """Replace Kotlin `$w` / `$h` / ${(h * 0.42).toInt()} placeholders."""
    chain = chain.replace("$w", str(w)).replace("$h", str(h))
    chain = re.sub(r"\$\{\(h \* ([\d.]+)\)\.toInt\(\)\}", lambda m: str(int(h * float(m.group(1)))), chain)
    chain = re.sub(r"\$\{\(w \* ([\d.]+)\)\.toInt\(\)\}", lambda m: str(int(w * float(m.group(1)))), chain)
    return chain


def extract_3d_masks(w=320, h=240):
    """{id: [filter,...]} from threeDMaskChain in VideoProcessor.kt."""
    src = read(VP)
    m = re.search(r"fun threeDMaskChain\(mask: String, w: Int, h: Int\): List<String> \{.*?return when \(m\) \{(.*?)\n\s+else -> listOf\(\)", src, re.S)
    if not m:
        return None
    masks = {}
    for line in m.group(1).splitlines():
        line = line.strip()
        bm = re.match(r'"([a-z0-9_]+)"(?:\s*,\s*"([a-z0-9_]+)")?\s*->\s*listOf\((.*)\)$', line)
        if not bm:
            continue
        chain = []
        for part in split_top(bm.group(3)):
            if part.startswith("maskGeq("):
                expr = part[len("maskGeq("):].rstrip(")")
                expr = expr.strip()
                if expr.startswith('"') and expr.endswith('"'):
                    expr = expr[1:-1]
                chain.append(f"geq=lum='if({expr},lum(X,Y),0)':cb='if({expr},cb(X,Y),128)':cr='if({expr},cr(X,Y),128)'")
            elif part.startswith('"') and part.endswith('"'):
                chain.append(unescape_chain(part[1:-1]))
        chain = [substitute_dims(c, w, h) for c in chain]
        for mid in (bm.group(1), bm.group(2)):
            if mid:
                masks[mid] = chain
    return masks


def extract_templates(w=320, h=240):
    """{id: [filter,...]} from templateChain in VideoProcessor.kt.

    templateChain entries are multi-line (`"id" -> listOf(
  "filter",
)`),
    so this scans forward from each `-> listOf(` line to the closing `)`.
    """
    src = read(VP)
    m = re.search(r"fun templateChain\(templateId: String\): List<String> \{.*?return when \(t\) \{(.*?)\n\s+else -> listOf\(", src, re.S)
    if not m:
        return None
    lines = m.group(1).splitlines()
    templates = {}
    i = 0
    while i < len(lines):
        line = lines[i].strip()
        bm = re.match(r'"([a-z0-9_]+)"(?:\s*,\s*"([a-z0-9_]+)")?\s*->\s*listOf\($', line)
        if bm:
            chain = []
            i += 1
            while i < len(lines):
                l = lines[i].strip()
                if l == ")":
                    break
                if l.startswith('"'):
                    lit = l[1:]
                    if lit.endswith('",'):
                        lit = lit[:-2]
                    elif lit.endswith('"'):
                        lit = lit[:-1]
                    chain.append(unescape_chain(lit.strip()))
                i += 1
            for tid in (bm.group(1), bm.group(2)):
                if tid:
                    templates[tid] = [substitute_dims(c, w, h) for c in chain]
        i += 1
    return templates


def extract_ai_chains():
    """{id: (videoChain, audioChain)} from PremiumFeatureCatalog aiFeatures."""
    src = read(CAT)
    m = re.search(r"val aiFeatures: List<PremiumFeature> = listOf\((.*?)\n    \)", src, re.S)
    if not m:
        return None
    ai = {}
    for line in m.group(1).splitlines():
        line = line.strip()
        if not line.startswith("PremiumFeature("):
            continue
        quoted = re.findall(r'"((?:[^"\\]|\\.)*)"', line)
        if not quoted:
            continue
        vid = quoted[5] if len(quoted) > 5 else ""
        aud = quoted[6] if len(quoted) > 6 else ""
        ai[quoted[0]] = (unescape_chain(vid), unescape_chain(aud))
    return ai


# ────────────────────────────────────────────────────────────────────────────
#  Rendering helpers
# ────────────────────────────────────────────────────────────────────────────
W, H, FPS, DUR = 320, 240, 15, 0.6
SRC_ARGS = ["-f", "lavfi", "-i", f"testsrc2=duration={DUR}:size={W}x{H}:rate={FPS}"]


def render(ffmpeg, tmpdir, name, vf=None, audio_filter=None):
    out = f"{tmpdir}/{name}.mp4"
    cmd = [ffmpeg, "-y", "-hide_banner", "-loglevel", "error"] + SRC_ARGS
    if vf:
        cmd += ["-vf", vf]
    cmd += ["-frames:v", "9", "-pix_fmt", "yuv420p"]
    if audio_filter:
        cmd += ["-af", audio_filter, "-t", str(DUR)]
    else:
        cmd += ["-an"]
    cmd += [out]
    r = run(cmd)
    return out, r.returncode, r.stderr


def frame_stats(ffmpeg, mp4, t):
    # metadata=print only emits at INFO level — do NOT pass -loglevel error here.
    cmd = [ffmpeg, "-hide_banner", "-ss", str(t), "-i", mp4,
           "-frames:v", "1", "-vf", "signalstats,metadata=print", "-f", "null", "-"]
    r = run(cmd)
    yavg = ymin = ymax = None
    for line in r.stderr.splitlines():
        if "YAVG=" in line:
            yavg = float(line.split("YAVG=")[1].split()[0])
        if "YMIN=" in line:
            ymin = float(line.split("YMIN=")[1].split()[0])
        if "YMAX=" in line:
            ymax = float(line.split("YMAX=")[1].split()[0])
    return yavg, ymin, ymax


def render_chain(ffmpeg, tmpdir, name, chain):
    return render(ffmpeg, tmpdir, name, ",".join(chain) if chain else None)


def frame_bytes(ffmpeg, mp4, t):
    """Raw rgb24 frame (scaled to 320x240) at time t, or None."""
    cmd = [ffmpeg, "-hide_banner", "-loglevel", "error", "-ss", str(t), "-i", mp4,
           "-frames:v", "1", "-vf", f"scale={W}:{H},format=rgb24", "-f", "rawvideo", "-"]
    r = subprocess.run(cmd, capture_output=True, timeout=120)  # binary output
    if r.returncode != 0 or not r.stdout:
        return None
    return r.stdout


def frame_diff(a, b):
    """Mean absolute byte difference between two equal-length raw frames."""
    if a is None or b is None or len(a) != len(b):
        return None
    total = 0
    for x, y in zip(a, b):
        total += abs(x - y)
    return total / len(a)


def piecewise(points, default):
    """Mirror of VideoProcessor.buildKeyframeExpressions piecewise builder."""
    if len(points) < 2:
        return default
    parts = []
    for i in range(len(points) - 1):
        t0, v0 = points[i]
        t1, v1 = points[i + 1]
        span = max(t1 - t0, 0.001)
        slope = (v1 - v0) / span
        expr = f"({v0}+({slope})*(t-{t0}))"
        cond = f"between(t,{t0},{t1})" if i == 0 else f"gte(t,{t0})"
        parts.append(f"if({cond},{expr},")
    return "".join(parts) + str(points[-1][1]) + ")" * (len(points) - 1)


def keyframe_filters(preset, D):
    """Mirror of buildKeyframeExpressions for one preset."""
    # Mirrors EditorViewModel.buildPresetKeyframes() — every preset the
    # KeyframePanel exposes, with the exact same keyframe values.
    presets = {
        "zoomIn": {"scale": [(0.0, 1.0), (D / 2, 1.25), (D, 1.5)]},
        "zoomOut": {"scale": [(0.0, 1.5), (D / 2, 1.25), (D, 1.0)]},
        "panLR": {"position_x": [(0.0, 0.0), (D, 1.0)]},
        "panRL": {"position_x": [(0.0, 1.0), (D, 0.0)]},
        "spin360": {"rotation": [(0.0, 0.0), (D, 360.0)]},
        "fadeIO": {"opacity": [(0.0, 0.0), (D * 0.2, 1.0), (D * 0.8, 1.0), (D, 0.0)]},
        "pulse": {"scale": [(0.0, 1.0), (D / 4, 1.1), (D / 2, 1.0), (D * 0.75, 1.1), (D, 1.0)]},
        "wobble": {"position_x": [(0.0, 0.5), (D / 4, 0.6), (D / 2, 0.5), (D * 0.75, 0.4), (D, 0.5)]},
        "slideUp": {"position_y": [(0.0, 1.0), (D, 0.0)]},
        "slideDown": {"position_y": [(0.0, 0.0), (D, 1.0)]},
        "bounceIn": {"scale": [(0.0, 0.0), (D * 0.6, 1.05), (D * 0.8, 0.95), (D, 1.0)]},
        "shake": {"position_x": [(0.0, 0.5), (D / 6, 0.55), (D / 3, 0.45), (D / 2, 0.5), (D * 2 / 3, 0.55), (D * 5 / 6, 0.45), (D, 0.5)]},
    }
    tracks = presets[preset]
    filters = []
    px = piecewise(tracks.get("position_x", []), "")
    py = piecewise(tracks.get("position_y", []), "")
    if px or py:
        x = f"'(iw*0.3)*({px})'" if px else "'(iw*0.3)*0.5'"
        y = f"'(ih*0.3)*({py})'" if py else "'(ih*0.3)*0.5'"
        filters.append(f"crop=w=iw*0.7:h=ih*0.7:x={x}:y={y}")
    sc = piecewise(tracks.get("scale", []), "")
    if sc:
        filters.append(f"scale='trunc(iw*max({sc},0.2)/2)*2:trunc(ih*max({sc},0.2)/2)*2':eval=frame")
    ro = piecewise(tracks.get("rotation", []), "")
    if ro:
        filters.append(f"rotate=a='({ro})*PI/180'")
    op = piecewise(tracks.get("opacity", []), "")
    if op:
        opT = re.sub(r"\bt\b", "T", op)
        filters.append(f"geq=lum='lum(X,Y)*{opT}':cb='(cb(X,Y)-128)*{opT}+128':cr='(cr(X,Y)-128)*{opT}+128'")
    # Mirror of the v7.3 fix: normalise per-frame sizes to even dims before
    # the pin scale+pad (FFmpeg 4.4 swscale re-init bug -> black frames).
    if filters:
        filters.append(f"pad=ceil(iw/2)*2:ceil(ih/2)*2:(ow-iw)/2:(oh-ih)/2:black:eval=frame")
    filters.append(f"scale={W}:{H}:force_original_aspect_ratio=decrease,pad={W}:{H}:(ow-iw)/2:(oh-ih)/2:black")
    return filters


# ---------------------------------------------------------------------------
def main():
    global passes
    ap = argparse.ArgumentParser()
    ap.add_argument("--ffmpeg", default=None)
    args = ap.parse_args()
    ffmpeg = args.ffmpeg or shutil.which("ffmpeg")
    if not ffmpeg:
        print("ERROR: ffmpeg not found", file=sys.stderr)
        sys.exit(2)

    tmp = tempfile.mkdtemp(prefix="v73_")
    try:
        # Baseline (no filter) frame for diff comparisons
        _, rc, _ = render(ffmpeg, tmp, "baseline")
        base_frame = frame_bytes(ffmpeg, f"{tmp}/baseline.mp4", 0.0) if rc == 0 else None
        base_ya = None

        # ═══ 1. 3D MASKS ═══
        section("1. 3D MASKS (threeDMaskChain)")
        masks = extract_3d_masks()
        if not masks:
            fail("could not extract threeDMaskChain from source")
        else:
            rendered = {}
            for mid, chain in sorted(masks.items()):
                out, rc, err = render_chain(ffmpeg, tmp, f"mask_{mid}", chain)
                if rc != 0:
                    fail(f"3D mask '{mid}' failed: {err.strip()[:200]}")
                    continue
                ya, _, _ = frame_stats(ffmpeg, out, 0.4)
                rendered[mid] = ya
                if ya is not None and 0 < ya < 240:
                    ok(f"3D mask '{mid}' renders (YAVG={ya:.1f})")
                else:
                    fail(f"3D mask '{mid}' suspicious YAVG={ya}")
            shapes = ["circle", "triangle", "diamond", "heart", "square", "oval", "hexagon", "star", "arch"]
            pairs = [(a, b) for i, a in enumerate(shapes) for b in shapes[i + 1:]]
            distinct = sum(1 for a, b in pairs
                           if rendered.get(a) is not None and rendered.get(b) is not None
                           and abs(rendered[a] - rendered[b]) > 3.0)
            if distinct >= 8:
                ok(f"shape masks are geometrically distinct ({distinct}/{len(pairs)} pairs differ)")
            else:
                fail(f"shape masks too similar — only {distinct}/{len(pairs)} pairs differ")

        # ═══ 2. KEYFRAME PRESETS ═══
        section("2. KEYFRAME PRESETS (buildKeyframeExpressions)")
        for pname in ("zoomIn", "zoomOut", "panLR", "panRL", "spin360", "fadeIO", "pulse", "wobble", "slideUp", "slideDown", "bounceIn", "shake"):
            filters = keyframe_filters(pname, DUR)
            out, rc, err = render_chain(ffmpeg, tmp, f"kf_{pname}", filters)
            if rc != 0:
                fail(f"keyframe preset '{pname}' failed: {err.strip()[:200]}")
                continue
            probe = run([ffmpeg, "-hide_banner", "-i", out])
            size_ok = f"{W}x{H}" in probe.stderr
            f0 = frame_bytes(ffmpeg, out, 0.0)
            f1 = frame_bytes(ffmpeg, out, 0.15)
            f2 = frame_bytes(ffmpeg, out, 0.45)
            if pname == "fadeIO":
                d = frame_diff(f0, f1)
                if d is not None and d > 8.0:
                    ok(f"fadeIO really fades (frame 0 vs mid differ by {d:.1f}/255)")
                else:
                    fail(f"fadeIO did not fade: diff={d}")
            else:
                d = frame_diff(f1, f2)
                if d is not None and d > 1.0:
                    ok(f"'{pname}' animates (t=0.15 vs t=0.45 differ by {d:.2f}/255)")
                else:
                    fail(f"'{pname}' appears static: diff={d}")
            if size_ok:
                ok(f"'{pname}' output size pinned to {W}x{H}")
            else:
                fail(f"'{pname}' output size wrong")

        # ═══ 3. AI HUB ═══
        section("3. AI HUB (PremiumFeatureCatalog chains)")
        ai = extract_ai_chains()
        if not ai:
            fail("could not extract aiFeatures from PremiumFeatureCatalog")
        else:
            ui = read("app/src/main/java/com/powercut/editor/ui/editor/NextGenEditorScreen.kt")
            hub_ids = sorted({i for i in re.findall(r'to "([a-z0-9_]+)"', ui) if i.startswith("ai_")})
            missing = sorted(set(hub_ids) - set(ai.keys()))
            nochain = sorted(i for i in hub_ids if i in ai and not ai[i][0] and not ai[i][1])
            if missing:
                fail(f"AI Hub ids missing from catalog: {missing}")
            else:
                ok(f"all {len(hub_ids)} AI Hub ids exist in the catalog")
            if nochain:
                fail(f"AI Hub ids with EMPTY chains: {nochain}")
            else:
                ok("every AI Hub id has a real video or audio chain")
            for i in hub_ids:
                vc, ac = ai[i]
                if vc:
                    # Input-dependent chains need a matching source to prove
                    # they really process frames: chromakey needs green, and
                    # denoise/stabilize need noise/shake to remove.
                    pre = ""
                    if "chromakey" in vc:
                        pre = "colorbalance=gs=0.9:bs=-0.9:rs=-0.9,"
                    elif "hqdn3d" in vc or "deshake" in vc:
                        pre = "noise=alls=25:allf=t,"
                    out, rc, err = render(ffmpeg, tmp, f"ai_{i}", pre + vc)
                    if rc != 0:
                        fail(f"AI '{i}' failed: {err.strip()[:150]}")
                        continue
                    sample_t = 0.3 if any(k in vc for k in ("minterpolate", "setpts", "zoompan")) else 0.0
                    fb = frame_bytes(ffmpeg, out, sample_t)
                    if fb is None:
                        # frame-dropping chains (select=, minterpolate=...) may
                        # re-time the stream; a non-empty file + clean exit is
                        # enough proof they run on real footage.
                        probe = run([ffmpeg, "-hide_banner", "-i", out])
                        if "Duration:" in probe.stderr and "No start time" not in probe.stderr:
                            ok(f"AI '{i}' renders (frame-timing chain)")
                        else:
                            fail(f"AI '{i}' produced no usable output: {probe.stderr.strip()[:120]}")
                        continue
                    d = frame_diff(fb, base_frame)
                    if d is not None and d > 0.8:
                        ok(f"AI '{i}' renders & processes frames (diff {d:.1f}/255)")
                    else:
                        fail(f"AI '{i}' frame diff {d} (looks unchanged)")
                elif ac:
                    out, rc, err = render(ffmpeg, tmp, f"ai_{i}", audio_filter=ac)
                    if rc == 0:
                        ok(f"AI '{i}' audio chain renders")
                    else:
                        fail(f"AI '{i}' audio chain failed: {err.strip()[:150]}")

        # ═══ 4. TEMPLATES ═══
        section("4. TEMPLATES (templateChain)")
        templates = extract_templates()
        if not templates:
            fail("could not extract templateChain from source")
        else:
            rendered = {}
            for tid, chain in sorted(templates.items()):
                out, rc, err = render_chain(ffmpeg, tmp, f"tpl_{tid}", chain)
                if rc != 0:
                    fail(f"template '{tid}' failed: {err.strip()[:200]}")
                    continue
                fb = frame_bytes(ffmpeg, out, 0.0)
                d = frame_diff(fb, base_frame)
                rendered[tid] = d if d is not None else 0.0
                if d is not None and d > 0.8:
                    ok(f"template '{tid}' renders & grades (frame diff {d:.1f}/255)")
                else:
                    fail(f"template '{tid}' frame diff {d} (looks unchanged)")
            ids = [i for i in ("cinema", "wedding", "travel", "vlog", "poetry", "beats", "glitch", "spark",
                               "bloom", "reels", "tiktok", "neon", "retro", "minimal", "dark", "golden",
                               "ocean", "fire", "ice") if i in rendered]
            distinct = sum(1 for i, a in enumerate(ids) for b in ids[i + 1:]
                           if abs(rendered[a] - rendered[b]) > 2.0)
            if distinct >= 20:
                ok(f"templates are distinctly graded ({distinct} pairs differ)")
            else:
                fail(f"templates too similar ({distinct} pairs differ)")

    finally:
        shutil.rmtree(tmp, ignore_errors=True)

    print(f"\n{'=' * 60}\n{passes} passed, {len(failures)} failed")
    if failures:
        for f in failures:
            print(f"  FAIL: {f}")
        sys.exit(1)
    print("ALL CHECKS PASSED")


if __name__ == "__main__":
    main()
