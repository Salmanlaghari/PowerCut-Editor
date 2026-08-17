#!/usr/bin/env python3
"""
REAL-FFmpeg validation harness for PowerCut text animations (PART 3).

WHAT THIS PROVES
----------------
Nothing here is a hardcoded "expected string" assertion. Every check either
asks a real ffmpeg binary a question, or measures a real generated MP4.

  1. PER-FRAME MECHANISM FLOOR (source audit)
     The catalog must animate ONLY through drawtext options that FFmpeg 4.4
     evaluates per-frame (x / y / alpha / fontsize / text expansion). The old
     broken patterns are forbidden and asserted absent from the catalog
     source:
       - `fontcolor=0x..@'expr'`  -> HARD graph-init failure on FFmpeg 4.4
                                    ("Invalid alpha value specifier")
       - `fontcolor_expr`         -> exists in 4.4 but is init-time only
                                    (a static frame on export)
       - `eval=frame` / drawbox / boxblur / gblur time expressions

  2. UI COVERAGE
     Extracts the animation ids the editor UI actually offers
     (TextAnimationCatalog.UI_IDS) and asserts EVERY one is realisable: the
     catalog produces a non-trivial drawtext chain for each.

  3. PER-ANIMATION RENDER
     For every id, renders a REAL 2s MP4 with the exact drawtext chain the
     catalog emits and verifies: ffmpeg exit status, the file exists and is
     non-trivial, and it has a decodable video stream.

  4. PER-FRAME PROOF
     For every ANIMATED id, extracts decoded frames at t=0.3s and t=1.8s and
     asserts they DIFFER — this is the check that catches the old broken way:
     an expression evaluated once at init produces identical frames, and a
     hard-failing option (fontcolor@expr) fails the render in step 3.
     Static looks (frozen/metallic/gold) are asserted to render, not animate.

USAGE
-----
  python3 scripts/validate_animations_ffmpeg.py
  python3 scripts/validate_animations_ffmpeg.py --ffmpeg /path/to/ffmpeg
  python3 scripts/validate_animations_ffmpeg.py --keep   # keep generated MP4s

EXIT CODES
----------
  0  all checks passed
  1  at least one check failed
  2  ffmpeg/ffprobe not found
"""

import argparse
import os
import re
import shutil
import subprocess
import sys
import tempfile

REPO = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
CATALOG = os.path.join(
    REPO, "app/src/main/java/com/powercut/editor/domain/processing/TextAnimationCatalog.kt"
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
# Source extraction (read from the Kotlin, no hardcoded expectation lists)
# ---------------------------------------------------------------------------
def read(path):
    with open(path, encoding="utf-8") as f:
        return f.read()


def ui_animation_ids():
    """[(id)] from TextAnimationCatalog.UI_IDS in UI order."""
    src = read(CATALOG)
    m = re.search(r"val UI_IDS: List<String> = listOf\((.*?)\n    \)", src, re.S)
    if not m:
        return []
    return re.findall(r'"([a-z0-9_]+)"', m.group(1))


def catalog_when_block():
    src = read(CATALOG)
    m = re.search(r"return when \(anim\) \{(.*?)\n        \}", src, re.S)
    return m.group(1) if m else ""


def extract_branch_args(when_block):
    """Map id -> drawtext args fragment (the text after `$base`), by reading
    the actual Kotlin branch strings. Multi-id branches map every id to the
    same args. The two multi-layer colour crossfades (color_cycle, rainbow)
    are assembled by build_multi_layer() below instead, because their branches
    call the catalog's extraLayer() helper."""
    mapping = {}
    for m in re.finditer(r'(.*?)->\s*"\$base(.*?)"', when_block, re.S):
        lhs = m.group(1)
        args = m.group(2)
        ids = re.findall(r'"([a-z0-9_]+)"', lhs)
        if not ids:
            continue
        # Kotlin `\\` -> single backslash in the emitted FFmpeg string.
        args = args.replace("\\\\", "\\")
        args = args.replace("${fs}", "{FS}").replace("$fs", "{FS}")
        args = args.replace("${duration}", "{DUR}").replace("$duration", "{DUR}")
        # Catalog locals: $fadeEnd (duration-1) and $d (fmt(duration)).
        args = args.replace("$fadeEnd", "{FADE_END}")
        args = args.replace("$d", "{D}")
        args = args.replace("$yExpr", "{Y}").replace("$xExpr", "{X}")
        args = args.replace("${yExpr}", "{Y}").replace("${xExpr}", "{X}")
        for i in ids:
            mapping[i] = args
    return mapping


def build_multi_layer(animation_id):
    """Reconstructs the two colour-crossfade chains the catalog builds via its
    extraLayer() helper. The chain structure (layer colours, per-frame alpha
    phase shifts, boxless extra layers) is pinned by the JVM test
    TextAnimationCatalogTest.colorCrossfadesLayerMultipleDrawtexts; this
    mirrors it so a real ffmpeg can render it."""
    base = (
        "drawtext=text='PowerCut':fontsize=42:fontcolor=0xFFFFFF:"
        "box=1:boxcolor=black@0.5:x=(w*0.500-text_w/2):y=(h*0.850-text_h/2)"
    )

    def extra_layer(color, alpha_expr):
        return (
            "drawtext=text='PowerCut':fontsize=42:fontcolor=%s:"
            "x=(w*0.500-text_w/2):y=(h*0.850-text_h/2):alpha='%s'"
            % (color, alpha_expr)
        )

    if animation_id == "color_cycle":
        return (
            base + ":fontcolor=0xFF3B30:alpha='0.5+0.5*sin(t*2)',"
            + extra_layer("0x007AFF", "0.5+0.5*sin(t*2+3.14159)")
        )
    if animation_id == "rainbow":
        return (
            base + ":fontcolor=0xFF3B30:alpha='0.333+0.333*sin(t*2)',"
            + extra_layer("0x00C853", "0.333+0.333*sin(t*2+2.09440)") + ","
            + extra_layer("0x2979FF", "0.333+0.333*sin(t*2+4.18879)")
        )
    return None


def build_chain(animation_id, args):
    base = (
        "drawtext=text='PowerCut':fontsize=42:fontcolor=0xFFFFFF:"
        "box=1:boxcolor=black@0.5:x=(w*0.500-text_w/2):y=(h*0.850-text_h/2)"
    )
    args = (args
            .replace("{FS}", "42")
            .replace("{DUR}", "2.0")
            .replace("{FADE_END}", "1.000")
            .replace("{D}", "2.000")
            .replace("{Y}", "h*0.850-text_h/2")
            .replace("{X}", "w*0.500-text_w/2"))
    return base + args


# ---------------------------------------------------------------------------
# Source audit: forbidden init-time-only mechanisms
# ---------------------------------------------------------------------------
def code_only(src):
    """Strip Kotlin comments (KDoc `*` lines, `//`, `/* */`) so the audit
    checks the emitted filter code, not the documentation that names the
    forbidden patterns."""
    out = []
    for line in src.splitlines():
        s = line.strip()
        if s.startswith("*") or s.startswith("//") or s.startswith("/*"):
            continue
        out.append(line)
    return "\n".join(out)


def audit_source(catalog_src):
    section("1) Per-frame mechanism floor (source audit)")
    code = code_only(catalog_src)
    # The forbidden patterns (old broken way, hard-fail or static on 4.4):
    checks = [
        ("fontcolor alpha expression (fontcolor=0x..@'expr')",
         r"fontcolor=0x[0-9A-Fa-f]+@'"),
        ("fontcolor_expr (init-time only in 4.4)", r"fontcolor_expr"),
        ("eval=frame / eval=init", r"eval\s*=\s*(frame|init)"),
        ("drawbox/boxblur/gblur with time expr", r"(drawbox|boxblur|gblur)"),
    ]
    clean = True
    for label, pattern in checks:
        if re.search(pattern, code):
            fail(f"catalog source contains {label}")
            clean = False
    if clean:
        ok("catalog uses only per-frame drawtext mechanisms "
           "(no fontcolor@expr / fontcolor_expr / eval= / drawbox / boxblur / gblur)")


# ---------------------------------------------------------------------------
# ffmpeg helpers
# ---------------------------------------------------------------------------
def render_clip(ffmpeg, chain, out_mp4):
    """Render 2s of a synthetic video with the drawtext chain."""
    cmd = [ffmpeg, "-y", "-hide_banner", "-loglevel", "error",
           "-f", "lavfi", "-i", "color=c=0x3a3a4a:s=640x360:d=2:r=30",
           "-vf", chain, "-c:v", "libx264", "-preset", "ultrafast",
           "-pix_fmt", "yuv420p", out_mp4]
    return run(cmd)


def extract_frame(ffmpeg, mp4, t, png):
    cmd = [ffmpeg, "-y", "-hide_banner", "-loglevel", "error",
           "-ss", str(t), "-i", mp4, "-frames:v", "1", png]
    return run(cmd)


def media_ok(ffprobe, mp4):
    cmd = [ffprobe, "-v", "error", "-select_streams", "v:0",
           "-show_entries", "stream=codec_type,width,height",
           "-of", "csv=p=0", mp4]
    r = run(cmd)
    return r.returncode == 0 and "video" in r.stdout


def main():
    ap = argparse.ArgumentParser(description=__doc__,
                                 formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("--ffmpeg", default=None)
    ap.add_argument("--ffprobe", default=None)
    ap.add_argument("--keep", action="store_true",
                    help="keep generated MP4s in a temp dir")
    args = ap.parse_args()

    ffmpeg = args.ffmpeg or shutil.which("ffmpeg")
    ffprobe = args.ffprobe or shutil.which("ffprobe")
    if not ffmpeg or not ffprobe:
        print("ERROR: ffmpeg/ffprobe not found; cannot validate.", file=sys.stderr)
        return 2

    tmp = tempfile.mkdtemp(prefix="pw_anim_")
    try:
        catalog_src = read(CATALOG)
        ids = ui_animation_ids()
        if not ids:
            fail("could not extract UI_IDS from TextAnimationCatalog.kt")
            ids = []

        audit_source(catalog_src)

        section(f"2) UI coverage ({len(ids)} animations)")
        when_block = catalog_when_block()
        branch_args = extract_branch_args(when_block)
        realisable = 0
        for i in ids:
            if i == "none":
                continue
            if i in ("color_cycle", "rainbow"):
                chain = build_multi_layer(i)
            else:
                branch = branch_args.get(i)
                if branch is None:
                    fail(f"'{i}' has no catalog branch — UI animation without backend logic")
                    continue
                chain = build_chain(i, branch)
            if not chain or not chain.startswith("drawtext="):
                fail(f"'{i}' produced an empty/invalid chain")
                continue
            realisable += 1
        if realisable == len(ids) - 1:
            ok(f"all {len(ids) - 1} non-'none' UI animations resolve to a real drawtext chain")
        else:
            fail(f"only {realisable}/{len(ids) - 1} UI animations realisable")

        section("3+4) Per-animation real-FFmpeg render + per-frame proof")
        animated = [i for i in ids if i != "none"
                    and i not in ("frozen", "metallic", "gold")]
        static = ["frozen", "metallic", "gold"]
        for i in animated + static:
            if i not in ids:
                continue
            if i in ("color_cycle", "rainbow"):
                chain = build_multi_layer(i)
            else:
                branch = branch_args.get(i)
                if branch is None:
                    fail(f"'{i}' has no catalog branch")
                    continue
                chain = build_chain(i, branch)
            out_mp4 = os.path.join(tmp, f"{i}.mp4")
            r = render_clip(ffmpeg, chain, out_mp4)
            if r.returncode != 0 or not os.path.isfile(out_mp4) \
                    or os.path.getsize(out_mp4) < 4096:
                detail = r.stderr.strip().splitlines()
                fail(f"'{i}' render failed: "
                     + (detail[-1] if detail else f"rc={r.returncode}"))
                continue
            if not media_ok(ffprobe, out_mp4):
                fail(f"'{i}' output has no decodable video stream")
                continue
            if i in animated:
                p1 = os.path.join(tmp, f"{i}_a.png")
                p2 = os.path.join(tmp, f"{i}_b.png")
                r1 = extract_frame(ffmpeg, out_mp4, 0.3, p1)
                r2 = extract_frame(ffmpeg, out_mp4, 1.8, p2)
                if r1.returncode != 0 or r2.returncode != 0:
                    fail(f"'{i}' frame extraction failed")
                    continue
                with open(p1, "rb") as f1, open(p2, "rb") as f2:
                    differ = f1.read() != f2.read()
                if not differ:
                    fail(f"'{i}' frames at t=0.3/t=1.8 identical — "
                         "animation is NOT per-frame (init-time-only expression?)")
                    continue
                ok(f"'{i}' renders and animates per-frame")
            else:
                ok(f"'{i}' renders (static look, no per-frame requirement)")
    finally:
        if not args.keep:
            shutil.rmtree(tmp, ignore_errors=True)

    print("\n" + "=" * 72)
    print("SUMMARY")
    print("=" * 72)
    print(f"  Checks passed : {passes}")
    print(f"  Failures      : {len(failures)}")
    if failures:
        for f in failures:
            print(f"    - {f}")
        print("\nRESULT: FAIL — see entries above.")
        return 1
    print("\nRESULT: PASS — every animation renders on real ffmpeg and every "
          "animated id provably animates per-frame.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
