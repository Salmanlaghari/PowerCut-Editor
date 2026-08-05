#!/usr/bin/env python3
"""C++ brace/paren/bracket balance — unicode-safe, handles strings, char
literals, line + block comments. Scans both the engine source tree and the
mirrored app/src/main/cpp/engine tree + native_export.cpp."""
import os, re, sys

def strip(src):
    out = []
    i, n = 0, len(src)
    in_str = False
    in_char = False
    while i < n:
        c = src[i]
        nx = src[i+1] if i+1 < n else ''
        # comments (only when not in string/char)
        if not in_str and not in_char:
            if c == '/' and nx == '/':
                while i < n and src[i] != '\n': i += 1
                continue
            if c == '/' and nx == '*':
                i += 2
                while i+1 < n and not (src[i] == '*' and src[i+1] == '/'): i += 1
                i += 2
                continue
        if in_str:
            if c == '\\': i += 2; continue
            if c == '"': in_str = False; out.append('"')
            i += 1; continue
        if in_char:
            if c == '\\': i += 2; continue
            if c == "'": in_char = False; out.append("'")
            i += 1; continue
        if c == '"': in_str = True; i += 1; continue
        # char literal: ' followed by (escaped char or single non-digit char) then '
        # BUT skip C++14 digit separators like 50'000 — only treat ' as char-lit
        # start when preceded by a non-digit, non-identifier char.
        if c == "'":
            prev = out[-1] if out else '\n'
            # digit separator: prev is a digit or the char after ' is a digit
            nxt2 = src[i+1] if i+1 < n else ''
            if prev.isdigit() or nxt2.isdigit():
                out.append("'"); i += 1; continue  # keep as ordinary char
            in_char = True; i += 1; continue
        out.append(c); i += 1
    return ''.join(out)

def check(path):
    with open(path, encoding='utf-8') as f:
        s = strip(f.read())
    stack = []
    pairs = {')':'(', ']':'[', '}':'{'}
    line = 1
    probs = []
    for ch in s:
        if ch == '\n': line += 1; continue
        if ch in '([{':
            stack.append((ch, line))
        elif ch in pairs:
            if not stack or stack[-1][0] != pairs[ch]:
                probs.append(f"  unmatched '{ch}' line {line}")
                if stack: stack.pop()
            else: stack.pop()
    if stack:
        for ch, ln in stack:
            probs.append(f"  unclosed '{ch}' line {ln}")
    return probs, s.count('{'), s.count('}'), s.count('('), s.count(')'), s.count('['), s.count(']')

def main():
    bad = False
    files = []
    for base in ['src', 'include', 'app/src/main/cpp/engine',
                 'app/src/main/cpp/native_export.cpp']:
        if os.path.isfile(base):
            files.append(base)
        else:
            for dp, _, fns in os.walk(base):
                for fn in fns:
                    if fn.endswith(('.cpp','.h','.cc','.hpp')):
                        files.append(os.path.join(dp, fn))
    files = sorted(set(files))
    for p in files:
        probs, ob, cb, op, cp, obr, cbr = check(p)
        st = "OK" if not probs else "FAIL"
        if probs: bad = True
        print(f"[{st}] {p}  {{ {ob}/{cb} }}  ( {op}/{cp} )  [ {obr}/{cbr} ]")
        for pr in probs: print(pr)
    print("\nRESULT:", "ALL OK" if not bad else "FAILURES")
    sys.exit(1 if bad else 0)

if __name__ == '__main__':
    main()
