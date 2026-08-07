#!/usr/bin/env python3
"""C++ brace/paren/bracket balance, stripping line/block comments and string
literals. Char literals are NOT stripped (apostrophes are rare in C++ and the
risk of mis-stripping identifiers is higher). For files known to use char
literals, results may over-count — rely on this as a coarse lint only."""
import sys, pathlib

def strip(src):
    out = []
    i, n = 0, len(src)
    while i < n:
        c = src[i]
        nx = src[i+1] if i+1 < n else ''
        if c == '/' and nx == '/':
            while i < n and src[i] != '\n': i += 1
            continue
        if c == '/' and nx == '*':
            i += 2
            while i+1 < n and not (src[i] == '*' and src[i+1] == '/'): i += 1
            i += 2; continue
        if c == '"':
            i += 1
            while i < n and src[i] != '"':
                if src[i] == '\\': i += 2
                else: i += 1
            i += 1; out.append(' '); continue
        out.append(c); i += 1
    return ''.join(out)

def scan(path):
    s = strip(pathlib.Path(path).read_text())
    issues = []
    for op, cl, name in [('{','}','braces'), ('(',')','parens'), ('[',']','brackets')]:
        if s.count(op) != s.count(cl):
            issues.append(f"{name}: {s.count(op)} open vs {s.count(cl)} close")
    return issues or "OK"

for p in sys.argv[1:]:
    print(f"{p}: {scan(p)}")
