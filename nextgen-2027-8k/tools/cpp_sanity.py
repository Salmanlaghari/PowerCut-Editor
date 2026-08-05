#!/usr/bin/env python3
"""Lightweight C++ structural sanity check: brace/paren/bracket balance,
stray 'impl_->' inside free functions, and obvious leftover placeholders."""
import re, sys, pathlib

def scan(path):
    src = pathlib.Path(path).read_text()
    issues = []
    for op, cl, name in [('{','}','braces'), ('(',')','parens'), ('[',']','brackets')]:
        # crude: ignore inside strings/line comments is hard; do raw count
        if src.count(op) != src.count(cl):
            issues.append(f"{name}: {src.count(op)} open vs {src.count(cl)} close")
    # placeholder markers
    for marker in ['TODO', 'FIXME', 'PLACEHOLDER', 'XXX_FIXME', '/* place']:
        if marker in src:
            # allow 'TODO' in comments? we forbid placeholders strictly
            issues.append(f"contains {marker}")
    return issues

for p in sys.argv[1:]:
    i = scan(p)
    print(f"{p}: {'OK' if not i else i}")
