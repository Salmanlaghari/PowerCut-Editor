#!/usr/bin/env python3
"""Cross-reference check:
 1. Every tool screen referenced in EditorScreen.kt exists as a file + fn.
 2. Every `com.powercut.ui.X.Y(` call in tool screens resolves to a real fn.
 3. Common symbol exports present in components/theme."""
import os, re, sys

def find_defs(path, pat):
    try:
        txt = open(path, encoding='utf-8').read()
    except FileNotFoundError:
        return []
    return set(re.findall(pat, txt))

# 1. tool screens referenced by EditorScreen
ed = open('app/src/main/java/com/powercut/ui/editor/EditorScreen.kt').read()
refs = re.findall(r'com\.powercut\.ui\.(tools|export)\.(\w+)\(onClose', ed)
print("=== Tool screens referenced by EditorScreen ===")
ok = True
for pkg, name in refs:
    # find file
    cand = f"app/src/main/java/com/powercut/ui/{pkg}/{name}.kt"
    exists = os.path.exists(cand)
    # find fn def
    fn = find_defs(cand, r'\nfun\s+' + re.escape(name) + r'\s*\(')
    fndef = bool(fn) or find_defs(cand, r'\n@Composable\s+fun\s+' + re.escape(name) + r'\s*\(')
    fndef = bool(find_defs(cand, r'fun\s+' + re.escape(name) + r'\s*\('))
    status = "OK" if exists and fndef else "MISSING"
    if status != "OK": ok = False
    print(f"  [{status}] {pkg}.{name}  file={exists} fn={fndef}")

# 2. component symbols used across tool screens
print("\n=== Component symbol exports ===")
comp = open('app/src/main/java/com/powercut/ui/components/PremiumComponents.kt').read()
comp_defs = set(re.findall(r'fun\s+(\w+)\s*\(', comp))
demo = open('app/src/main/java/com/powercut/ui/components/DemoThumbnail.kt').read()
demo_defs = set(re.findall(r'fun\s+(?:DrawScope\.)?(\w+)\s*\(', demo))
needed = {'GradientPill','GradientPillCompact','ProBadge','GlassCard',
          'GradientRingProgress','GradientLinearProgress','powercutGradientBrush',
          'DemoThumbnail','baseScene','colorGrade','vignette','glitchLines'}
all_defs = comp_defs | demo_defs
for s in sorted(needed):
    status = "OK" if s in all_defs else "MISSING"
    if status != "OK": ok = False
    print(f"  [{status}] {s}")

# 3. theme colors used
print("\n=== Theme color exports ===")
col = open('app/src/main/java/com/powercut/ui/theme/Color.kt').read()
col_defs = set(re.findall(r'^val\s+(\w+)', col, re.M))
needed_colors = {'Bg','BgElev','BgCard','Orange','Purple','TextPrimary',
                 'TextSecondary','GlassStroke','TrackVideo','TrackAudio',
                 'TrackSubtitle','TrackSticker','Success','Danger','White','PureBlack'}
for s in sorted(needed_colors):
    status = "OK" if s in col_defs else "MISSING"
    if status != "OK": ok = False
    print(f"  [{status}] {s}")

# 4. ExportEngine API used by ExportScreen
print("\n=== ExportEngine API used by ExportScreen ===")
ee = open('app/src/main/java/com/powercut/export/ExportEngine.kt').read()
for sym in ['fun export', 'fun cancel', 'interface ProgressCallback',
            'fun onProgress', 'fun onComplete']:
    status = "OK" if sym in ee else "MISSING"
    if status != "OK": ok = False
    print(f"  [{status}] {sym}")

# 5. VM methods used by screens
print("\n=== EditorViewModel API used by screens ===")
vm = open('app/src/main/java/com/powercut/ui/editor/EditorViewModel.kt').read()
for sym in ['fun addDagNode', 'fun updateResolution', 'fun updateFps',
            'fun updateRemoveWatermark', 'val project', 'val exportConfig',
            'val revision', 'val isPlaying', 'val zoom']:
    status = "OK" if sym in vm else "MISSING"
    if status != "OK": ok = False
    print(f"  [{status}] {sym}")

print("\nRESULT:", "ALL OK" if ok else "FAILURES ABOVE")
sys.exit(0 if ok else 1)
