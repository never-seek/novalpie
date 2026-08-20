#!/usr/bin/env python3
"""Decode \\uXXXX escapes in Kotlin sources into the characters they denote.

Roughly 5000 Chinese string literals in this codebase are written as \\u escapes, which is the main
reason `NovalPieApp.kt` (1155 of them) and `NovalPieViewModel.kt` (1977) are hard to read. Kotlin
source is UTF-8, so the escapes carry no benefit -- they are an artefact of however the text was
originally written out.

This is a readability change only: an escape and the character it denotes compile to the same
constant, so `tools/golden_strings.py` must report zero difference afterwards. That is the check
that makes this safe to apply in bulk.

Escapes below U+0080 are left alone. Decoding those would change the source's meaning rather than
just its spelling -- \\u0024 would introduce a Kotlin string template, \\u0022 would terminate the
literal, \\u005C would start a new escape -- and they are not what makes the file unreadable.

Usage:
    py tools/decode_unicode_escapes.py --check    # report what would change
    py tools/decode_unicode_escapes.py --apply
"""

from __future__ import annotations

import argparse
import collections
import pathlib
import re
import sys
import unicodedata

ROOT = pathlib.Path(__file__).resolve().parent.parent
SOURCE_ROOT = ROOT / "app" / "src"

# A backslash run followed by uXXXX. The run length decides whether the escape is real: an even
# number of backslashes means the last pair is an escaped backslash, so the "u" is literal text and
# must be left alone.
ESCAPE = re.compile(r"(\\+)u([0-9a-fA-F]{4})")

MIN_DECODABLE = 0x80


def decode_source(text: str) -> tuple[str, int, collections.Counter]:
    skipped: collections.Counter = collections.Counter()
    decoded_count = 0

    def replace(match: re.Match[str]) -> str:
        nonlocal decoded_count
        backslashes, hexpart = match.group(1), match.group(2)
        if len(backslashes) % 2 == 0:
            # Escaped backslash: the text is a literal "\uXXXX", not an escape.
            return match.group(0)
        codepoint = int(hexpart, 16)
        if codepoint < MIN_DECODABLE:
            skipped[hexpart] += 1
            return match.group(0)
        category = unicodedata.category(chr(codepoint))
        # Format/control/surrogate/unassigned characters are invisible or unpaired; leaving them
        # escaped keeps them reviewable.
        if category in ("Cc", "Cf", "Cs", "Cn", "Zl", "Zp"):
            skipped[hexpart] += 1
            return match.group(0)
        decoded_count += 1
        return backslashes[:-1] + chr(codepoint)

    decoded = ESCAPE.sub(replace, text)
    return brace_wrap_ambiguous_templates(decoded), decoded_count, skipped


# A bare $identifier template followed by a character that Kotlin treats as part of an identifier.
AMBIGUOUS_TEMPLATE = re.compile(r"\$([A-Za-z_][A-Za-z0-9_]*)(?=[^\x00-\x7F])")

# Categories Kotlin accepts as identifier continuation. CJK ideographs are Lo, so they qualify --
# which is the whole problem.
IDENTIFIER_CONTINUATION = frozenset({"Lu", "Ll", "Lt", "Lm", "Lo", "Nd", "Nl", "Mn", "Mc", "Pc"})


def brace_wrap_ambiguous_templates(text: str) -> str:
    """Wrap `$name` in braces where decoding would otherwise extend the identifier.

    Decoding changes how Kotlin parses a template, because an escape terminates an identifier but
    the character it denotes may not. `"$label\\u5df2\\u540c\\u6b65"` is `label` followed by three
    escapes; `"$label已同步"` is a reference to a variable named `label已同步`, since CJK letters are
    legal in Kotlin identifiers. This is a compile error rather than a silent change, but it should
    not be left for the compiler to find one site at a time.

    Note that most CJK punctuation is safe: `，` `。` `）` are Po/Pe and do not continue an
    identifier, so those templates are left exactly as they were.
    """

    def replace(match: re.Match[str]) -> str:
        following = text[match.end():match.end() + 1]
        if not following or unicodedata.category(following) not in IDENTIFIER_CONTINUATION:
            return match.group(0)
        return "${" + match.group(1) + "}"

    return AMBIGUOUS_TEMPLATE.sub(replace, text)


def main() -> int:
    for stream in (sys.stdout, sys.stderr):
        try:
            stream.reconfigure(encoding="utf-8", errors="replace")
        except (AttributeError, ValueError):
            pass

    parser = argparse.ArgumentParser(description=__doc__)
    group = parser.add_mutually_exclusive_group(required=True)
    group.add_argument("--check", action="store_true", help="report changes without writing")
    group.add_argument("--apply", action="store_true", help="rewrite the files in place")
    args = parser.parse_args()

    total_decoded = 0
    total_skipped: collections.Counter = collections.Counter()
    changed_files = []

    for path in sorted(SOURCE_ROOT.rglob("*.kt")):
        original = path.read_text(encoding="utf-8")
        updated, decoded, skipped = decode_source(original)
        total_skipped.update(skipped)
        if decoded:
            total_decoded += decoded
            changed_files.append((path.relative_to(ROOT).as_posix(), decoded))
            if args.apply:
                path.write_text(updated, encoding="utf-8", newline="")

    verb = "decoded" if args.apply else "would decode"
    print(f"{verb} {total_decoded} escapes across {len(changed_files)} files")
    for name, count in sorted(changed_files, key=lambda item: -item[1])[:12]:
        print(f"  {count:5d}  {name}")
    if len(changed_files) > 12:
        print(f"  ... and {len(changed_files) - 12} more files")

    if total_skipped:
        print(f"\nleft escaped on purpose ({sum(total_skipped.values())} occurrences):")
        for hexpart, count in sorted(total_skipped.items(), key=lambda item: -item[1]):
            codepoint = int(hexpart, 16)
            try:
                name = unicodedata.name(chr(codepoint))
            except ValueError:
                name = "unnamed"
            print(f"  \\u{hexpart}  x{count}  ({name})")

    if args.apply:
        print("\nNow run: py tools/golden_strings.py  -- it must report zero difference.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
