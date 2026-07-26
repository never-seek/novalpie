#!/usr/bin/env python3
"""User-visible string golden master for the NovalPie refactor.

The refactor moves ~5000 Chinese string literals out of Kotlin and into string
resources, restructures three god-files, and rebuilds every screen. The risk is
that a piece of user-visible copy silently disappears along the way. This script
is the tripwire for that.

It extracts every user-visible string from the app (Kotlin literals containing
CJK, plus res/values*/strings.xml values), normalises them so that a literal and
the string resource it later becomes compare equal, and diffs the result against
a committed baseline.

Removals are failures -- that is content loss. Additions are reported but allowed,
since a redesign legitimately introduces new copy.

Usage:
    py tools/golden_strings.py --write     # (re)create the baseline
    py tools/golden_strings.py            # verify against the baseline
    py tools/golden_strings.py --list     # print the current set, no comparison
"""

from __future__ import annotations

import argparse
import re
import sys
import unicodedata
import xml.etree.ElementTree as ET
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
MAIN_KOTLIN = ROOT / "app" / "src" / "main" / "java"
RES = ROOT / "app" / "src" / "main" / "res"
BASELINE = ROOT / "tools" / "golden" / "user-visible-strings.txt"

# A literal is treated as user-visible copy if it contains a CJK ideograph or
# CJK punctuation. In this codebase that marker is reliable: identifiers, paths,
# API fields and JSON keys are all ASCII.
CJK = re.compile(
    "["
    "　-〿"  # CJK punctuation
    "㐀-䶿"  # extension A
    "一-鿿"  # unified ideographs
    "豈-﫿"  # compatibility ideographs
    "＀-￯"  # fullwidth forms
    "]"
)

# Kotlin string templates: "共 $count 章" / "共 ${list.size} 章".
# String resources: "共 %1$d 章" / "共 %d 章".
# Both collapse to the same placeholder so they compare equal across Phase 3.
KOTLIN_TEMPLATE = re.compile(r"\$\{[^}]*\}|\$[A-Za-z_][A-Za-z0-9_]*")
RES_FORMAT = re.compile(r"%(\d+\$)?[sdfx]|%%")
PLACEHOLDER = "<>"


def decode_escapes(raw: str) -> str:
    """Decode Kotlin escape sequences, most importantly \\uXXXX.

    Roughly 5000 literals in this codebase are \\u-escaped, which is why the
    source is unreadable. Decoding is what makes them comparable to the readable
    Chinese that will live in strings.xml.
    """
    out: list[str] = []
    i = 0
    while i < len(raw):
        ch = raw[i]
        if ch != "\\" or i + 1 >= len(raw):
            out.append(ch)
            i += 1
            continue
        nxt = raw[i + 1]
        if nxt == "u" and i + 5 < len(raw) + 1:
            hexpart = raw[i + 2 : i + 6]
            if len(hexpart) == 4 and all(c in "0123456789abcdefABCDEF" for c in hexpart):
                out.append(chr(int(hexpart, 16)))
                i += 6
                continue
        simple = {"n": "\n", "t": "\t", "r": "\r", "\\": "\\", '"': '"', "'": "'", "$": "$", "b": "\b"}
        if nxt in simple:
            out.append(simple[nxt])
            i += 2
            continue
        out.append(nxt)
        i += 2
    return "".join(out)


def normalise(value: str) -> str:
    """Canonical form used for comparison.

    Collapses template/format placeholders, normalises unicode width and
    whitespace, and trims. Two spellings of the same visible copy -- a Kotlin
    template and the string resource it becomes -- normalise identically.
    """
    value = KOTLIN_TEMPLATE.sub(PLACEHOLDER, value)
    value = RES_FORMAT.sub(PLACEHOLDER, value)
    value = unicodedata.normalize("NFKC", value)
    value = re.sub(r"\s+", " ", value)
    return value.strip()


def strip_comments(src: str) -> str:
    """Blank out comments and char literals so their contents are not harvested.

    Runs a small state machine rather than regexes, because string literals may
    contain // and /* and comments may contain quotes.
    """
    out: list[str] = []
    i, n = 0, len(src)
    while i < n:
        two = src[i : i + 2]
        if two == "//":
            while i < n and src[i] != "\n":
                i += 1
            continue
        if two == "/*":
            i += 2
            depth = 1
            while i < n and depth:
                if src[i : i + 2] == "/*":
                    depth += 1
                    i += 2
                elif src[i : i + 2] == "*/":
                    depth -= 1
                    i += 2
                else:
                    i += 1
            continue
        if src[i : i + 3] == '"""':
            out.append(src[i : i + 3])
            i += 3
            while i < n and src[i : i + 3] != '"""':
                out.append(src[i])
                i += 1
            out.append('"""')
            i += 3
            continue
        if src[i] == '"':
            out.append('"')
            i += 1
            while i < n and src[i] != '"':
                if src[i] == "\\" and i + 1 < n:
                    out.append(src[i : i + 2])
                    i += 2
                    continue
                out.append(src[i])
                i += 1
            out.append('"')
            i += 1
            continue
        if src[i] == "'":
            i += 1
            while i < n and src[i] != "'":
                i += 2 if src[i] == "\\" else 1
            i += 1
            continue
        out.append(src[i])
        i += 1
    return "".join(out)


LITERAL = re.compile(r'"""(.*?)"""|"((?:[^"\\\n]|\\.)*)"', re.S)


def kotlin_strings() -> dict[str, set[str]]:
    """Map normalised copy -> set of "file:line" origins."""
    found: dict[str, set[str]] = {}
    for path in sorted(MAIN_KOTLIN.rglob("*.kt")):
        src = path.read_text(encoding="utf-8", errors="replace")
        cleaned = strip_comments(src)
        rel = path.relative_to(ROOT).as_posix()
        for match in LITERAL.finditer(cleaned):
            raw = match.group(1) if match.group(1) is not None else match.group(2)
            if raw is None:
                continue
            decoded = decode_escapes(raw)
            if not CJK.search(decoded):
                continue
            key = normalise(decoded)
            if not key:
                continue
            line = cleaned.count("\n", 0, match.start()) + 1
            found.setdefault(key, set()).add(f"{rel}:{line}")
    return found


def resource_strings() -> dict[str, set[str]]:
    """Values from every res/values*/strings.xml. Empty until Phase 3."""
    found: dict[str, set[str]] = {}
    if not RES.exists():
        return found
    for path in sorted(RES.glob("values*/strings.xml")):
        rel = path.relative_to(ROOT).as_posix()
        try:
            tree = ET.parse(path)
        except ET.ParseError as exc:
            print(f"warning: cannot parse {rel}: {exc}", file=sys.stderr)
            continue
        for node in tree.getroot():
            if node.tag == "string":
                values = ["".join(node.itertext())]
            elif node.tag in ("plurals", "string-array"):
                values = ["".join(child.itertext()) for child in node]
            else:
                continue
            for value in values:
                if not value or not CJK.search(value):
                    continue
                key = normalise(value)
                if key:
                    found.setdefault(key, set()).add(f"{rel}:{node.get('name')}")
    return found


def collect() -> dict[str, set[str]]:
    merged = kotlin_strings()
    for key, origins in resource_strings().items():
        merged.setdefault(key, set()).update(origins)
    return merged


def main() -> int:
    # The Windows console here reports GBK, which cannot encode every character this script
    # prints -- notably the private-use codepoints that show up in mojibake. Without this the
    # script dies with UnicodeEncodeError while reporting its findings.
    for stream in (sys.stdout, sys.stderr):
        try:
            stream.reconfigure(encoding="utf-8", errors="replace")
        except (AttributeError, ValueError):
            pass

    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--write", action="store_true", help="write the baseline instead of verifying")
    parser.add_argument("--list", action="store_true", help="print the current set and exit")
    parser.add_argument("--show-origins", action="store_true", help="with --list, include file:line origins")
    args = parser.parse_args()

    current = collect()
    keys = sorted(current)

    if args.list:
        for key in keys:
            if args.show_origins:
                print(f"{key}\t{','.join(sorted(current[key]))}")
            else:
                print(key)
        return 0

    if args.write:
        BASELINE.parent.mkdir(parents=True, exist_ok=True)
        header = (
            "# NovalPie user-visible string golden master\n"
            "# Generated by tools/golden_strings.py --write\n"
            "# One normalised user-visible string per line. Placeholders are collapsed to <>.\n"
            "# Removing a line from the app is content loss and fails verification.\n"
        )
        BASELINE.write_text(header + "\n".join(keys) + "\n", encoding="utf-8")
        print(f"wrote {len(keys)} strings to {BASELINE.relative_to(ROOT).as_posix()}")
        return 0

    if not BASELINE.exists():
        print(f"error: no baseline at {BASELINE}. Run with --write first.", file=sys.stderr)
        return 2

    expected = {
        line
        for line in BASELINE.read_text(encoding="utf-8").splitlines()
        if line and not line.startswith("#")
    }
    have = set(keys)
    missing = sorted(expected - have)
    added = sorted(have - expected)

    print(f"baseline {len(expected)} · current {len(have)} · missing {len(missing)} · added {len(added)}")
    if added:
        print(f"\nadded ({len(added)}) -- allowed:")
        for key in added[:40]:
            print(f"  + {key}")
        if len(added) > 40:
            print(f"  ... and {len(added) - 40} more")
    if missing:
        print(f"\nMISSING ({len(missing)}) -- user-visible copy lost:", file=sys.stderr)
        for key in missing:
            print(f"  - {key}", file=sys.stderr)
        return 1

    print("\nOK: no user-visible copy lost.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
