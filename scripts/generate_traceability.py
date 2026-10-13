#!/usr/bin/env python3
"""Regenerate docs/test-case-traceability.md from the @TmsLink annotations in src/test/java.

Usage:
    python scripts/generate_traceability.py                 # rewrite the doc (code + existing doc)
    python scripts/generate_traceability.py --check         # exit 1 if the doc is out of date (CI)
    python scripts/generate_traceability.py --feed Test_Cases_Feed.xlsx   # use the sheet as scenario source
    python scripts/generate_traceability.py --feed feed.csv               # CSV export of the sheet
    python scripts/generate_traceability.py --stdout        # print instead of writing

Inputs
  * Code: every @TmsLink("TC-nnn") with its enclosing method and class file; a test with
    enabled = false is a "skeleton", otherwise "implemented".
  * Feed (optional): sheet 'Playwright-Java Feed' of Test_Cases_Feed.xlsx (or a CSV export). It
    supplies the TC id + scenario rows. Without it, the rows (and scenario names / notes) are taken
    from the existing doc, so nothing written by hand is lost.

A TC id used by more than one scenario, a missing TC id, or a non-descriptive scenario name is
marked needs-clarification. Python 3 standard library only; openpyxl is needed only for .xlsx feeds.
"""
import argparse
import csv
import re
import sys
from collections import defaultdict
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
DOC = ROOT / "docs" / "test-case-traceability.md"
TEST_DIR = ROOT / "src" / "test" / "java"
SHEET = "Playwright-Java Feed"

HEADER = """# Test case traceability (Test_Cases_Feed.xlsx, sheet 'Playwright-Java Feed')

Generated from the `@TmsLink` annotations in `src/test/java`. Status values:

- **implemented** - an enabled web test already covers the scenario (existing test tagged with the TC id; the mapping is by scenario title, since the sheet has no steps).
- **skeleton** - a disabled test with TODOs exists; needs steps, locators, data or access (see the TODO in the method).
- **needs-clarification** - the sheet flags the row (TC id shared by several scenarios, no TC id, or a non-descriptive name). Nothing was created. Confirm the mapping first.
- **disabled-needs-Appium** - none: every flow checked has a web equivalent.

| TC ID | Scenario | File / method | Status | Note |
|---|---|---|---|---|
"""

NO_TC = "NO-TC"
AUTO_DUP_NOTE = re.compile(r"^TC id \S+ is used by \d+ different scenarios$")
NON_DESCRIPTIVE = re.compile(r"^\s*TC[\s_-]*\d+\s*$", re.IGNORECASE)


def norm_tc(raw):
    """'tc 401', 'TC-401', '401' -> 'TC-401'; empty -> 'NO-TC'."""
    raw = (raw or "").strip()
    if not raw or raw.upper() in (NO_TC, "NA", "N/A", "-"):
        return NO_TC
    m = re.search(r"(\d+)", raw)
    return "TC-%s" % m.group(1) if m else raw


def tc_sort_key(tc):
    m = re.match(r"TC-(\d+)$", tc)
    return (0, int(m.group(1)), "") if m else (1, 0, tc)


def cell(text):
    return (text or "").replace("|", "\\|").replace("\n", " ").strip()


# ---------------------------------------------------------------- code scan

TMS_RE = re.compile(r'@TmsLink\(\s*"([^"]+)"\s*\)')
METHOD_RE = re.compile(r"^\s*(?:(?:public|protected|private|static|final|synchronized)\s+)*[\w<>\[\],.? ]+?\s+(\w+)\s*\([^;]*$")


def scan_code(test_dir):
    """Return {tc: [(file_stem, method, enabled)]}."""
    found = defaultdict(list)
    for path in sorted(test_dir.rglob("*.java")):
        ids, disabled, test_buf = [], False, None
        in_block_comment = False
        for line in path.read_text(encoding="utf-8", errors="replace").splitlines():
            s = line.strip()
            if in_block_comment:
                in_block_comment = "*/" not in s
                continue
            if s.startswith("/*"):
                in_block_comment = "*/" not in s
                continue
            if s.startswith("//") or s.startswith("*") or s.startswith("import "):
                continue
            # an @Test(...) may span several lines: accumulate until the parens balance
            if test_buf is not None or s.startswith("@Test"):
                test_buf = (test_buf or "") + " " + s
                if test_buf.count("(") <= test_buf.count(")"):
                    if re.search(r"enabled\s*=\s*false", test_buf):
                        disabled = True
                    test_buf = None
                continue
            m = TMS_RE.search(s)
            if m:
                ids.append(norm_tc(m.group(1)))
                continue
            if s.startswith("@"):
                continue
            if re.search(r"\b(class|interface|enum)\s+\w+", s):
                ids, disabled = [], False  # class-level annotations are not per-test links
                continue
            if ids:
                mm = METHOD_RE.match(line)
                if mm:
                    for tc in ids:
                        found[tc].append((path.stem, mm.group(1), not disabled))
                    ids, disabled = [], False
    return found


# ------------------------------------------------------------ existing doc

def read_existing(doc):
    """Return [(tc, scenario, status, note)] parsed from the committed doc."""
    rows = []
    if not doc.exists():
        return rows
    for line in doc.read_text(encoding="utf-8").splitlines():
        if not line.startswith("| ") or line.startswith("| TC ID") or line.startswith("|---"):
            continue
        parts = [p.strip().replace("\\|", "|") for p in re.split(r"(?<!\\)\|", line.strip())[1:-1]]
        if len(parts) >= 5:
            rows.append((parts[0], parts[1], parts[3], parts[4]))
    return rows


# -------------------------------------------------------------------- feed

TC_HEADERS = ("tc id", "tc_id", "tcid", "test case id", "testcase id", "tc no", "tc number", "tms id")
SCENARIO_HEADERS = ("scenario", "test case name", "testcase name", "test case title", "scenario name", "title", "name")


def read_feed(path):
    """Return [(tc, scenario)] from an .xlsx (sheet 'Playwright-Java Feed') or a CSV export."""
    p = Path(path)
    if not p.exists():
        sys.exit("error: feed file not found: %s" % p)
    if p.suffix.lower() in (".xlsx", ".xlsm"):
        try:
            import openpyxl
        except ImportError:
            sys.exit("error: reading .xlsx needs openpyxl (pip install openpyxl), or pass a CSV export of the sheet.")
        wb = openpyxl.load_workbook(p, read_only=True, data_only=True)
        if SHEET not in wb.sheetnames:
            sys.exit("error: sheet '%s' not found in %s (sheets: %s)" % (SHEET, p, ", ".join(wb.sheetnames)))
        table = [["" if v is None else str(v) for v in row] for row in wb[SHEET].iter_rows(values_only=True)]
    else:
        with open(p, newline="", encoding="utf-8-sig") as fh:
            table = list(csv.reader(fh))
    # the header row is the first row that names both a TC id column and a scenario column
    for i, row in enumerate(table):
        low = [c.strip().lower() for c in row]
        tc_col = next((j for j, c in enumerate(low) if c in TC_HEADERS), None)
        sc_col = next((j for j, c in enumerate(low) if c in SCENARIO_HEADERS), None)
        if tc_col is not None and sc_col is not None:
            out = []
            for r in table[i + 1:]:
                if not any(c.strip() for c in r):
                    continue
                tc = r[tc_col] if tc_col < len(r) else ""
                sc = r[sc_col] if sc_col < len(r) else ""
                if sc.strip() or tc.strip():
                    out.append((norm_tc(tc), sc.strip()))
            return out
    sys.exit("error: could not find a TC id column (%s) and a scenario column (%s) in %s. "
             "Header rows seen: %r" % ("/".join(TC_HEADERS[:3]), "/".join(SCENARIO_HEADERS[:3]), p, table[:3]))


# ------------------------------------------------------------------ render

def build(code, existing, feed):
    notes = {(norm_tc(tc), sc.casefold()): note for tc, sc, _st, note in existing}
    if feed is not None:
        pairs = feed
    else:
        pairs = [(norm_tc(tc), sc) for tc, sc, _st, _n in existing]
        known = {tc for tc, _ in pairs}
        pairs += [(tc, "(scenario unknown - not in doc)") for tc in code if tc not in known]
    pairs = sorted(set((tc, sc) for tc, sc in pairs), key=lambda r: (tc_sort_key(r[0]), r[1].casefold()))

    scenarios = defaultdict(set)
    for tc, sc in pairs:
        scenarios[tc].add(sc.casefold())

    rows = []
    for tc, sc in pairs:
        n = len(scenarios[tc])
        if tc == NO_TC:
            status, loc, auto = "needs-clarification", "-", "no TC id"
        elif NON_DESCRIPTIVE.match(sc):
            status, loc, auto = "needs-clarification", "-", "non-descriptive name"
        elif n > 1:
            status, loc, auto = "needs-clarification", "-", "TC id %s is used by %d different scenarios" % (tc, n)
        elif tc in code:
            hits = code[tc]
            status = "implemented" if any(en for _f, _m, en in hits) else "skeleton"
            loc, auto = ", ".join("`%s#%s`" % (f, m) for f, m, _e in hits), ""
        else:
            status, loc, auto = "needs-clarification", "-", "no test tagged with this TC id"
        old = notes.get((tc, sc.casefold()), "")
        note = old if old and not AUTO_DUP_NOTE.match(old) and not (status == "needs-clarification" and old == "no test tagged with this TC id") else auto
        if status in ("implemented", "skeleton") and AUTO_DUP_NOTE.match(old):
            note = ""
        rows.append((tc, sc, loc, status, note))
    return rows


def render(rows):
    out = [HEADER.rstrip("\n")]
    for tc, sc, loc, status, note in rows:
        out.append("| %s | %s | %s | %s | %s |" % (tc, cell(sc), loc, status, cell(note)))
    totals = defaultdict(int)
    for r in rows:
        totals[r[3]] += 1
    out += ["", "## Totals", ""]
    out += ["- %s: %d" % (k, totals[k]) for k in sorted(totals)]
    return "\n".join(out) + "\n"


def main():
    ap = argparse.ArgumentParser(description=__doc__.split("\n")[0])
    ap.add_argument("--feed", help="Test_Cases_Feed.xlsx (sheet '%s') or a CSV export of it" % SHEET)
    ap.add_argument("--check", action="store_true", help="exit 1 if the committed doc differs from the generated output")
    ap.add_argument("--stdout", action="store_true", help="print the result instead of writing the doc")
    ap.add_argument("--doc", default=str(DOC), help="output doc (default: docs/test-case-traceability.md)")
    ap.add_argument("--tests", default=str(TEST_DIR), help="test source root (default: src/test/java)")
    args = ap.parse_args()

    doc = Path(args.doc)
    code = scan_code(Path(args.tests))
    feed = read_feed(args.feed) if args.feed else None
    text = render(build(code, read_existing(doc), feed))

    if args.stdout:
        sys.stdout.write(text)
        return 0
    current = doc.read_text(encoding="utf-8").replace("\r\n", "\n") if doc.exists() else None
    if args.check:
        if current == text:
            print("OK: %s is up to date" % doc.name)
            return 0
        print("FAIL: %s is out of date; run: python scripts/generate_traceability.py" % doc.name, file=sys.stderr)
        return 1
    if current == text:
        print("%s already up to date" % doc.name)
    else:
        doc.parent.mkdir(parents=True, exist_ok=True)
        with open(doc, "w", encoding="utf-8", newline="\n") as fh:
            fh.write(text)
        print("wrote %s" % doc)
    return 0


if __name__ == "__main__":
    sys.exit(main())
