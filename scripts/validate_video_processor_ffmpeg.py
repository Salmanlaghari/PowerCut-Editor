#!/usr/bin/env python3
"""
Real-FFmpeg validation harness for the PowerCut editor.

PURPOSE
-------
Validate that the FFmpeg filter graphs actually *built* by the app's export
code are real, parseable FFmpeg — not hardcoded assertion strings.

Everything the harness checks is *derived from the source code itself*:

  1. It statically parses VideoProcessor.kt (and PremiumFeatureCatalog.kt) and
     extracts every FFmpeg filter name that the code references.
  2. It validates each referenced filter name against a REAL ffmpeg binary via
     `ffmpeg -filters` / `ffmpeg -h filter=<name>`. If `copy` is used as a
     filter name, that is flagged (it is NOT a filter — it is stream copy).
  3. It extracts every fully-static filter-graph string literal (no `${...}`
     placeholders, no multi-input `[label]` references) and validates it by
     actually *parsing + running* it through the real ffmpeg (`-vf`/`-af` into
     `-f null`). No expected strings are hardcoded; ffmpeg is the judge.

     `-vf`/`-af` only accept a *simple* graph (exactly 1 input, 1 output), so a
     graph is re-driven the way it is really used when ffmpeg reports an arity
     mismatch: source filters (`color=`, `anullsrc=`) are validated as an
     `-i lavfi` input, and mixers (`sidechaincompress`) via `-filter_complex`
     with as many synthetic inputs as they ask for. Option *fragments* that the
     Kotlin concatenates onto another filter (leading `:` or `,`) are not
     graphs and are skipped rather than reported as broken filters.

USAGE
-----
  python3 validate_video_processor_ffmpeg.py
  python3 validate_video_processor_ffmpeg.py --ffmpeg /path/to/ffmpeg
  python3 validate_video_processor_ffmpeg.py --extract-only   # no ffmpeg needed
  python3 validate_video_processor_ffmpeg.py --sources a.kt b.kt

EXIT CODES
----------
  0  all referenced filters exist and all static graphs parse/run under ffmpeg
  1  at least one referenced filter is missing, or a static graph failed
  2  ffmpeg binary could not be found (live validation skipped)

This script does NOT modify any Kotlin source. It only reads and validates.
"""

import argparse
import os
import re
import shutil
import subprocess
import sys

# ---------------------------------------------------------------------------
# Source discovery
# ---------------------------------------------------------------------------
DEFAULT_SOURCES = [
    "app/src/main/java/com/powercut/editor/domain/processing/VideoProcessor.kt",
    "app/src/main/java/com/powercut/editor/domain/premium/PremiumFeatureCatalog.kt",
]

# Filters that operate on audio only. Used to decide whether a static graph
# should be validated with `-af` (anullsrc) instead of `-vf` (color source).
AUDIO_FILTERS = {
    "volume", "atempo", "asetrate", "aresample", "amix", "silenceremove",
    "anullsrc", "aecho", "afftdn", "highpass", "lowpass", "dynaudnorm", "pan",
    "tremolo", "vibrato", "chorus", "flanger", "acompressor", "alimiter",
    "adeclip", "adeclick", "arnndn", "speechnorm", "loudnorm", "bass",
    "treble", "equalizer", "firequalizer", "stereotools", "stereowiden",
    "aformat", "apad", "atrim", "asetpts", "aselect", "anullsink", "asettb",
    "acrossfade", "amerge", "apulsator", "asubboost", "asupercut",
}

# Markers that mean a graph needs inputs other than a single video/audio
# stream, so it cannot be deep-validated with a synthetic lavfi source.
MULTI_INPUT_MARKERS = (
    "overlay", "chromakey", "chromahold", "amix", "concat", "drawtext",
    "movie", "sendcmd", "readvitc", "selectivecolor", "remap", "xfade",
    "blend", "multiply", "overlay_qsv",
)


# ---------------------------------------------------------------------------
# String literal extraction (double-quoted + triple-quoted Kotlin strings)
# ---------------------------------------------------------------------------
# Unambiguous CODEC / MUXER / ENCODER parameter keywords that appear as
# `key=value` strings in the export code but are NOT FFmpeg filters. They are
# passed as values to flags such as -x265-params / -maxrate and must not be
# reported as missing filters. This is a denylist of known *non-filters*, not an
# assertion of expected filters.
NON_FILTER_TOKENS = {
    "profile", "level", "maxrate", "bufsize", "crf", "tune", "gop",
    "keyint_min", "sc_threshold", "colorprim", "transfer", "colormatrix",
    "repeat", "max-cll", "hdr10-opt", "repeat-headers", "tag", "movflags",
    "faststart", "map_metadata", "preset", "logs", "analyzeduration",
    "probesize", "err_detect", "fflags", "ignore_unknown", "safe", "shortest",
    "x265-params", "colorrange", "color_trc", "colorspace", "field_order",
    "threads", "y", "ss", "t", "c", "i", "an", "vf", "af", "filter_complex",
    "map", "encoders", "h", "filters", "b", "b:a", "b:v", "r", "g", "pixel_format",
}


def extract_string_literals(text):
    """Return list of string-literal *contents* found in Kotlin source.

    Adjacent string literals joined by `+` (optionally across a newline or
    comment) are stitched into one logical literal, so that a filter split
    across Kotlin string concatenation is reconstructed faithfully.
    """
    spans = []  # (content, start, end)
    for m in re.finditer(r'"""(.*?)"""', text, re.DOTALL):
        spans.append((m.group(1), m.start(), m.end()))
    for m in re.finditer(r'"((?:[^"\\]|\\.)*)"', text):
        content = (m.group(1)
                   .replace('\\"', '"').replace("\\\\", "\\")
                   .replace("\\n", "\n"))
        spans.append((content, m.start(), m.end()))
    if not spans:
        return []
    spans.sort(key=lambda x: x[1])
    merged = []
    i = 0
    while i < len(spans):
        content, start, end = spans[i]
        j = i + 1
        while j < len(spans):
            between = text[end:spans[j][1]]
            cleaned = re.sub(r"/\*.*?\*/", "", between, flags=re.DOTALL)
            cleaned = re.sub(r"//.*", "", cleaned).strip()
            if cleaned == "+":
                content = content + spans[j][0]
                end = spans[j][2]
                j += 1
            else:
                break
        merged.append(content)
        i = j
    return merged


def split_top_level_commas(s):
    """Split a filtergraph segment on commas that are NOT inside '...' quotes."""
    out, buf, in_q = [], "", False
    i = 0
    while i < len(s):
        c = s[i]
        if c == "'" and (i == 0 or s[i - 1] != "\\"):
            in_q = not in_q
            buf += c
        elif c == "," and not in_q:
            out.append(buf)
            buf = ""
        else:
            buf += c
        i += 1
    if buf.strip():
        out.append(buf)
    return out


def is_graph_literal(literal):
    """Heuristic: does this string literal look like FFmpeg filtergraph
    syntax (rather than a log message, file path, or option fragment)?

    We accept a literal as graph-like when it either:
      * contains `word=` AND filtergraph punctuation (`:`, `,`, `'`, `"`, `[`); or
      * contains at least two `=` signs (option=value style).
    Kotlin string templates (`$var`, `${...}`) are excluded: their interpolated
    expressions contain `=`/`,`/`:` and would be misread as filtergraphs, and they
    cannot be validated deterministically without executing the Kotlin anyway.

    This keeps prose ("Compress succeeded (CRF ...)"), option fragments
    ("brightness=0.04"), bare preset/codec/format words ("veryfast",
    "libx264", "yuv420p") and sentinels ("none") out of the filter-name set.

    NOTE: no-argument bare filters (e.g. `negate`, `hflip`, `null`) used as
    standalone string literals are intentionally NOT treated as graphs here;
    their names are still validated whenever they appear inside an actual
    filtergraph string, and they are standard FFmpeg filters.
    """
    if not literal:
        return False
    if "$" in literal:  # Kotlin string template — not statically knowable
        return False
    # An *option fragment* rather than a graph: these literals are concatenated
    # onto an existing filter by the Kotlin code (e.g. `":box=1:boxcolor=..."`
    # appended to a drawtext filter, or `",silenceremove=..."` appended to an
    # audio chain). A real filtergraph always starts with a filter name or a
    # `[label]`, never with a separator.
    if re.match(r"^\s*[,:;|=]", literal):
        return False
    if not re.search(r"[A-Za-z_]\w*=", literal):
        return False
    if re.search(r'[:,\'"\[\]]', literal):
        return True
    if literal.count("=") >= 2:
        return True
    return False


def extract_filter_names(literal):
    """Extract the set of filter names from a filtergraph literal.

    A filter name is the leading identifier of each comma-separated segment
    (optionally preceded by `[label]` inputs and followed by `[label]` outputs).
    A bare no-argument filter (e.g. `negate`, `hflip`, `null`) is also captured.
    """
    if not is_graph_literal(literal):
        return set()
    names = set()
    for stmt in literal.split(";"):
        for seg in split_top_level_commas(stmt):
            seg0 = seg.strip()
            if not seg0:
                continue
            seg = re.sub(r"^\[[^\]]*\]", "", seg0)
            while re.match(r"^\[[^\]]*\]", seg):
                seg = re.sub(r"^\[[^\]]*\]", "", seg, count=1)
            seg = re.sub(r"\[[^\]]*\]\s*$", "", seg).strip()
            if not seg:
                continue
            m = re.match(r"[A-Za-z_]\w*(?==)", seg)
            if m:
                cand = m.group(0)
                if cand not in NON_FILTER_TOKENS:
                    names.add(cand)
            else:
                m2 = re.fullmatch(r"[a-z][a-z0-9_]*", seg)
                if m2 and m2.group(0) not in NON_FILTER_TOKENS:
                    names.add(m2.group(0))
    return names


def is_static_graph(literal):
    """True if the literal is a self-contained single-stream filter graph
    that can be fed straight to `ffmpeg -vf`/`-af` for validation."""
    if not literal:
        return False
    if "${" in literal:  # Kotlin interpolation -> not statically known
        return False
    if "[" in literal:   # label references -> multi-input graph
        return False
    if any(marker in literal for marker in MULTI_INPUT_MARKERS):
        return False
    if not is_graph_literal(literal):
        return False
    # Skip codec/muxer parameter blocks (e.g. the value of `-x265-params`)
    # whose leading token is a known non-filter keyword.
    first = re.match(r"\s*(?:\[[^\]]*\])*\s*([A-Za-z_]\w*)", literal)
    if first and first.group(1) in NON_FILTER_TOKENS:
        return False
    return True


# ---------------------------------------------------------------------------
# ffmpeg queries
# ---------------------------------------------------------------------------
def get_available_filters(ffmpeg):
    """Return a set of filter names reported by `ffmpeg -filters`."""
    try:
        out = subprocess.run(
            [ffmpeg, "-hide_banner", "-filters"],
            capture_output=True, text=True, timeout=60,
        ).stdout
    except Exception:
        return set()
    available = set()
    media = re.compile(r"^[VASN]\->[VASN]$")
    for line in out.splitlines():
        parts = line.split()
        if len(parts) < 2:
            continue
        for idx, tok in enumerate(parts):
            if media.match(tok):
                if idx >= 1:
                    available.add(parts[idx - 1])
                break
    return available


def filter_exists(name, ffmpeg, available):
    if name in available:
        return True
    # Authoritative fallback check. NOTE: `ffmpeg -h filter=<name>` exits 0 even
    # for a name it does not know (it just prints "Unknown filter 'x'."), so the
    # return code alone would make this check always succeed.
    try:
        r = subprocess.run(
            [ffmpeg, "-hide_banner", "-h", "filter=" + name],
            capture_output=True, text=True, timeout=30,
        )
        out = (r.stdout or "") + (r.stderr or "")
        if "Unknown filter" in out:
            return False
        return r.returncode == 0 and ("AVOptions" in out or "Inputs:" in out
                                      or "Outputs:" in out)
    except Exception:
        return False


def _run(cmd):
    try:
        r = subprocess.run(cmd, capture_output=True, text=True, timeout=60)
    except subprocess.TimeoutExpired:
        return None, "timeout"
    return r, (r.stderr or "")


# Messages emitted by the *muxer*, not by the filtergraph. A speed-changing
# `setpts` compresses timestamps, so on a short synthetic clip two frames can
# land on the same output timestamp and the null muxer reports non-monotonic
# DTS. ffmpeg still exits 0: the graph parsed and ran. This harness validates
# filtergraphs, so output-timing complaints must not be counted as graph errors.
MUXER_NOISE_RE = re.compile(
    r"(?i)non[- ]monotonic(ally)?( increasing)? (dts|pts)"
    r"|Application provided invalid, non monotonic"
    r"|data is not aligned")


def _strip_muxer_noise(stderr):
    return "\n".join(l for l in stderr.splitlines()
                     if not MUXER_NOISE_RE.search(l))


def _verdict(r, stderr):
    """Turn an ffmpeg run into (ok, detail), judging the filtergraph only."""
    stderr = _strip_muxer_noise(stderr)
    detail = stderr.strip().splitlines()
    if r.returncode == 0 and not re.search(r"(?i)error|invalid|not.*found", stderr):
        return True, ""
    msg = next((l for l in detail if "Error" in l or "Invalid" in l
                or "No such" in l or "failed" in l), "")
    return False, msg or (detail[0] if detail else "exit %d" % r.returncode)



# `-vf` / `-af` accept only a *simple* filtergraph: exactly one input and one
# output. Source filters (`color=`, `anullsrc=`, ...) have zero inputs and
# mixers (`sidechaincompress`, ...) have several, so feeding them through
# `-vf`/`-af` fails on the graph's arity, not on the graph being wrong.
# ffmpeg reports that arity mismatch explicitly, and we re-drive the graph the
# way it is actually meant to be used.
ARITY_RE = re.compile(
    r"expected to have exactly 1 input and 1 output\. However, it had "
    r"(\S+) input\(s\) and (\S+) output\(s\)")

AUDIO_SRC = "anullsrc=channel_layout=stereo:sample_rate=44100:d=0.2"
# A 320x240 4:3 source is too small/too square for aspect-ratio crops and
# letterbox pads, which legitimately need a real 16:9 HD frame.
VIDEO_SRC = "color=c=black:s=1920x1080:d=0.2:r=25"

_ARITY_CACHE = {}


def filter_input_count(name, ffmpeg):
    """How many inputs does this filter take? 0 for a source filter, 2+ for a
    mixer, None when ffmpeg reports it as dynamic/unknown.

    Read from the filter's own metadata rather than from an error string, so the
    behaviour does not depend on ffmpeg's wording, which changes across releases.
    """
    if name in _ARITY_CACHE:
        return _ARITY_CACHE[name]
    n = None
    try:
        r = subprocess.run([ffmpeg, "-hide_banner", "-h", "filter=" + name],
                           capture_output=True, text=True, timeout=30)
        out = (r.stdout or "") + (r.stderr or "")
        block = re.search(r"Inputs:\s*\n(.*?)(?:Outputs:|AVOptions)", out, re.DOTALL)
        if block:
            body = block.group(1)
            if "none (source filter)" in body:
                n = 0
            elif "dynamic" in body:
                n = None
            else:
                n = len(re.findall(r"^\s*#\d+:", body, re.MULTILINE)) or None
    except Exception:
        n = None
    _ARITY_CACHE[name] = n
    return n


def graph_first_filter(graph):
    m = re.match(r"\s*([a-z][a-z0-9_]*)", graph)
    return m.group(1) if m else ""


def validate_as_source(ffmpeg, graph):
    """Validate a *source* filter graph (zero inputs) by using it as an input."""
    cmd = [ffmpeg, "-hide_banner", "-loglevel", "error",
           "-f", "lavfi", "-i", graph, "-t", "0.2", "-f", "null", "-"]
    r, stderr = _run(cmd)
    if r is None:
        return False, stderr
    return _verdict(r, stderr)


def validate_as_multi_input(ffmpeg, graph, is_audio):
    """Validate a multi-input graph (a mixer such as sidechaincompress) by
    feeding it the number of synthetic inputs it actually asks for."""
    src = AUDIO_SRC if is_audio else VIDEO_SRC
    pad = "a" if is_audio else "v"
    last = "no input count matched"
    for n in (2, 3, 4):
        inputs = []
        for _ in range(n):
            inputs += ["-f", "lavfi", "-i", src]
        labels = "".join("[%d:%s]" % (i, pad) for i in range(n))
        cmd = [ffmpeg, "-hide_banner", "-loglevel", "error", *inputs,
               "-filter_complex", labels + graph, "-f", "null", "-"]
        r, stderr = _run(cmd)
        if r is None:
            return False, stderr
        ok, detail = _verdict(r, stderr)
        if ok:
            return True, ""
        # Wrong guess at the input count -> try one more input.
        if not ARITY_RE.search(stderr) and "Invalid file index" not in stderr \
                and "matches no streams" not in stderr:
            return False, detail
        last = detail
    return False, last


def validate_graph(ffmpeg, graph, is_audio):
    """Run the real ffmpeg on the graph with a synthetic source.
    Returns (ok, detail)."""
    # Pick the right driving mode up-front from the graph's own arity, so a
    # source filter or a mixer is never mis-reported as a broken graph.
    n_in = filter_input_count(graph_first_filter(graph), ffmpeg)
    if n_in == 0:
        return validate_as_source(ffmpeg, graph)
    if n_in is not None and n_in > 1:
        return validate_as_multi_input(ffmpeg, graph, is_audio)

    if is_audio:
        src = ["-f", "lavfi", "-i", AUDIO_SRC]
        port = "-af"
    else:
        src = ["-f", "lavfi", "-i", VIDEO_SRC]
        port = "-vf"
    cmd = [ffmpeg, "-hide_banner", "-loglevel", "error",
           *src, port, graph, "-f", "null", "-"]
    r, stderr = _run(cmd)
    if r is None:
        return False, stderr
    ok, detail = _verdict(r, stderr)
    if ok:
        return True, ""

    # Safety net: if ffmpeg still complains purely about arity (e.g. a mixer
    # sitting later in the chain), re-drive the graph accordingly.
    m = ARITY_RE.search(stderr)
    if m:
        if m.group(1) == "0":
            return validate_as_source(ffmpeg, graph)
        return validate_as_multi_input(ffmpeg, graph, is_audio)
    return False, detail


# ---------------------------------------------------------------------------
# Main
# ---------------------------------------------------------------------------
def main():
    ap = argparse.ArgumentParser(description=__doc__,
                                 formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("--ffmpeg", default=None,
                    help="Path to the ffmpeg binary (default: $PATH lookup).")
    ap.add_argument("--sources", nargs="*", default=DEFAULT_SOURCES,
                    help="Kotlin source files to scan.")
    ap.add_argument("--extract-only", action="store_true",
                    help="Only print extracted filters/graphs; skip ffmpeg.")
    ap.add_argument("--allow-missing-ffmpeg", action="store_true",
                    help="Exit 0 even if ffmpeg is missing (extraction only).")
    args = ap.parse_args()

    # Locate sources that actually exist.
    sources = [s for s in args.sources if os.path.isfile(s)]
    if not sources:
        print("ERROR: none of the source files were found:", file=sys.stderr)
        for s in args.sources:
            print("  -", s, file=sys.stderr)
        return 2

    # Collect literals + the filter names / graphs they contain.
    filter_origin = {}      # name -> first source file
    graph_origin = []       # (graph, source)
    for path in sources:
        text = open(path, "r", encoding="utf-8", errors="replace").read()
        for lit in extract_string_literals(text):
            for name in extract_filter_names(lit):
                filter_origin.setdefault(name, path)
            if is_static_graph(lit):
                graph_origin.append((lit, path))

    referenced = sorted(filter_origin)
    print("=" * 72)
    print("REAL-FFMPEG VALIDATION HARNESS — PowerCut VideoProcessor")
    print("=" * 72)
    print(f"Sources scanned : {len(sources)}")
    for s in sources:
        print(f"  - {s}")
    print(f"Distinct filter names referenced : {len(referenced)}")
    print(f"Static filter graphs to validate : {len(graph_origin)}")

    # Decide ffmpeg availability.
    ffmpeg = args.ffmpeg or shutil.which("ffmpeg")
    if args.extract_only:
        ffmpeg = None

    if not ffmpeg:
        print("\n[!] ffmpeg binary NOT found — live validation skipped.")
        print("    Extraction results (what WOULD be validated):")
        print("    Referenced filters:")
        for n in referenced:
            print(f"      {n:24s} <- {os.path.basename(filter_origin[n])}")
        print("    Static graphs:")
        for g, _ in graph_origin:
            print(f"      {g}")
        if args.extract_only or args.allow_missing_ffmpeg:
            return 0
        return 2

    available = get_available_filters(ffmpeg)
    print(f"ffmpeg filters available        : {len(available)} "
          f"({os.path.basename(ffmpeg)})")

    # ---- 1) filter-name existence -----------------------------------------
    print("\n--- 1) Referenced filter names vs real ffmpeg ---")
    missing = []
    for name in referenced:
        # `copy` is a stream-copy keyword, never a filter.
        if name == "copy":
            print(f"  [FAIL] {name:24s} (NOT a filter — it is stream copy)")
            missing.append(name)
            continue
        ok = filter_exists(name, ffmpeg, available)
        status = "ok  " if ok else "FAIL"
        if not ok:
            missing.append(name)
        print(f"  [{status}] {name:24s} <- {os.path.basename(filter_origin[name])}")

    # ---- 2) static graph parse/run ----------------------------------------
    print("\n--- 2) Static filter graphs parsed by real ffmpeg ---")
    graph_fail = []
    for graph, src in graph_origin:
        names = extract_filter_names(graph)
        is_audio = bool(names & AUDIO_FILTERS)
        # Try the most likely media type, fall back to the other.
        ok, detail = validate_graph(ffmpeg, graph, is_audio)
        if not ok and not is_audio:
            ok, detail = validate_graph(ffmpeg, graph, True)
        if not ok and is_audio:
            ok, detail = validate_graph(ffmpeg, graph, False)
        status = "ok  " if ok else "FAIL"
        if not ok:
            graph_fail.append((graph, detail))
        print(f"  [{status}] {graph}" + ("" if ok else f"  -> {detail}"))

    # ---- summary -----------------------------------------------------------
    print("\n" + "=" * 72)
    print("SUMMARY")
    print("=" * 72)
    print(f"  Referenced filters : {len(referenced)} "
          f"(missing: {len(missing)})")
    print(f"  Static graphs      : {len(graph_origin)} "
          f"(failed: {len(graph_fail)})")
    if missing:
        print("  Missing filters:")
        for m in missing:
            print(f"    - {m}")
    if graph_fail:
        print("  Failed graphs:")
        for g, d in graph_fail:
            print(f"    - {g}   [{d}]")

    if missing or graph_fail:
        print("\nRESULT: FAIL — see entries above.")
        return 1
    print("\nRESULT: PASS — all referenced filters exist and all static "
          "graphs parse/run under real ffmpeg.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
