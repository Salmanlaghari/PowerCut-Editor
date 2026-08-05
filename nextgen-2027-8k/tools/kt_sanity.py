#!/usr/bin/env python3
"""Structural sanity check for Kotlin files: brace/paren balance using a
unicode-safe char-by-char stripper (handles emojis in strings). Reports
imbalance per file."""
import os, re, sys

ROOT = "app/src/main/java/com/powercut"

def strip_kt(src: str) -> str:
    """Remove comments + string + char literals, unicode-safe."""
    out = []
    i = 0
    n = len(src)
    in_line = False
    in_block = 0
    in_str = False
    in_triple = False
    while i < n:
        c = src[i]
        nxt = src[i + 1] if i + 1 < n else ''
        if in_block > 0:
            if c == '*' and nxt == '/':
                in_block -= 1
                i += 2
                continue
            i += 1
            continue
        if in_line:
            if c == '\n':
                in_line = False
            i += 1
            continue
        if in_triple:
            if c == '"' and src[i:i + 3] == '"""':
                in_triple = False
                out.append('""')
                i += 3
                continue
            i += 1
            continue
        if in_str:
            if c == '\\':
                i += 2
                continue
            if c == '"':
                in_str = False
                out.append('""')
            i += 1
            continue
        if c == '/' and nxt == '/':
            in_line = True
            i += 2
            continue
        if c == '/' and nxt == '*':
            in_block += 1
            i += 2
            continue
        if c == '"' and src[i:i + 3] == '"""':
            in_triple = True
            i += 3
            continue
        if c == '"':
            in_str = True
            i += 1
            continue
        out.append(c)
        i += 1
    s = ''.join(out)
    # remove char literals '...' (after string stripping)
    s = re.sub(r"'(?:\\.|[^'\\])'", "''", s)
    return s

def check(path):
    with open(path, 'r', encoding='utf-8') as f:
        src = f.read()
    s = strip_kt(src)
    stack = []
    pairs = {')': '(', ']': '[', '}': '{'}
    openers = '([{'
    line = 1
    problems = []
    for ch in s:
        if ch == '\n':
            line += 1
            continue
        if ch in openers:
            stack.append((ch, line))
        elif ch in pairs:
            if not stack or stack[-1][0] != pairs[ch]:
                problems.append(f"  unmatched '{ch}' at line {line}")
                if stack:
                    stack.pop()
            else:
                stack.pop()
    if stack:
        for ch, ln in stack:
            problems.append(f"  unclosed '{ch}' opened at line {ln}")
    return problems, s.count('{'), s.count('}'), s.count('('), s.count(')')

def main():
    bad = False
    files = []
    for dirpath, _, fnames in os.walk(ROOT):
        for fn in fnames:
            if fn.endswith('.kt'):
                files.append(os.path.join(dirpath, fn))
    files.sort()
    for p in files:
        probs, ob, cb, op, cp = check(p)
        status = "OK" if not probs else "FAIL"
        if probs:
            bad = True
        print(f"[{status}] {p}  braces {ob}/{cb}  parens {op}/{cp}")
        for pr in probs:
            print(pr)
    print("\nRESULT:", "ALL OK" if not bad else "FAILURES ABOVE")
    sys.exit(1 if bad else 0)

if __name__ == '__main__':
    main()
