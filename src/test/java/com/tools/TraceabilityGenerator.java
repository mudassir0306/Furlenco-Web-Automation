package com.tools;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Regenerates docs/test-case-traceability.md from the {@code @TmsLink} annotations in src/test/java.
 * Java port of the former scripts/generate_traceability.py. Not a test (name does not match surefire's
 * test patterns, no TestNG annotations). Run from the repository root with the JDK source launcher:
 *
 * <pre>
 *   java src/test/java/com/tools/TraceabilityGenerator.java                  # rewrite the doc (code + existing doc)
 *   java src/test/java/com/tools/TraceabilityGenerator.java --check          # exit 1 if the doc is out of date (CI)
 *   java src/test/java/com/tools/TraceabilityGenerator.java --feed feed.csv  # CSV export of sheet 'Playwright-Java Feed'
 *   java src/test/java/com/tools/TraceabilityGenerator.java --stdout         # print instead of writing
 * </pre>
 *
 * Inputs: the code (every @TmsLink("TC-nnn") with its enclosing method and class file; a test with
 * enabled = false is a "skeleton", otherwise "implemented") and optionally a CSV feed that supplies the
 * TC id + scenario rows. Without a feed the rows, scenario names and notes come from the existing doc,
 * so nothing written by hand is lost. A TC id used by more than one scenario, a missing TC id or a
 * non-descriptive scenario name is marked needs-clarification. (.xlsx feeds are not read: export the
 * sheet to CSV.)
 */
public final class TraceabilityGenerator {

    private static final String SHEET = "Playwright-Java Feed";
    private static final String DEFAULT_DOC = "docs/test-case-traceability.md";
    private static final String DEFAULT_TESTS = "src/test/java";

    private static final String HEADER = String.join("\n",
            "# Test case traceability (Test_Cases_Feed.xlsx, sheet 'Playwright-Java Feed')",
            "",
            "Generated from the `@TmsLink` annotations in `src/test/java`. Status values:",
            "",
            "- **implemented** - an enabled web test already covers the scenario (existing test tagged with the TC id; the mapping is by scenario title, since the sheet has no steps).",
            "- **skeleton** - a disabled test with TODOs exists; needs steps, locators, data or access (see the TODO in the method).",
            "- **needs-clarification** - the sheet flags the row (TC id shared by several scenarios, no TC id, or a non-descriptive name). Nothing was created. Confirm the mapping first.",
            "- **disabled-needs-Appium** - none: every flow checked has a web equivalent.",
            "",
            "| TC ID | Scenario | File / method | Status | Note |",
            "|---|---|---|---|---|");

    private static final String NO_TC = "NO-TC";
    private static final Pattern AUTO_DUP_NOTE = Pattern.compile("^TC id \\S+ is used by \\d+ different scenarios$");
    private static final Pattern NON_DESCRIPTIVE = Pattern.compile("^\\s*TC[\\s_-]*\\d+\\s*$", Pattern.CASE_INSENSITIVE);
    private static final Pattern TMS_RE = Pattern.compile("@TmsLink\\(\\s*\"([^\"]+)\"\\s*\\)");
    private static final Pattern METHOD_RE = Pattern.compile(
            "^\\s*(?:(?:public|protected|private|static|final|synchronized)\\s+)*[\\w<>\\[\\],.? ]+?\\s+(\\w+)\\s*\\([^;]*$");
    private static final Pattern ENABLED_FALSE = Pattern.compile("enabled\\s*=\\s*false");
    private static final Pattern TYPE_DECL = Pattern.compile("\\b(class|interface|enum)\\s+\\w+");
    private static final Pattern DIGITS = Pattern.compile("(\\d+)");
    private static final Pattern TC_ID = Pattern.compile("TC-(\\d+)");

    private static final List<String> TC_HEADERS = List.of("tc id", "tc_id", "tcid", "test case id", "testcase id",
            "tc no", "tc number", "tms id");
    private static final List<String> SCENARIO_HEADERS = List.of("scenario", "test case name", "testcase name",
            "test case title", "scenario name", "title", "name");

    private TraceabilityGenerator() {
    }

    record Hit(String file, String method, boolean enabled) {
    }

    record Existing(String tc, String scenario, String status, String note) {
    }

    record Pair(String tc, String scenario) {
    }

    record Row(String tc, String scenario, String loc, String status, String note) {
    }

    /** Thrown for user-facing errors (printed as "error: ..." and exit 1). */
    static final class Fatal extends RuntimeException {
        Fatal(String message) {
            super(message);
        }
    }

    // ------------------------------------------------------------ helpers

    static String casefold(String s) {
        return s.toLowerCase();
    }

    /** 'tc 401', 'TC-401', '401' -> 'TC-401'; empty -> 'NO-TC'. */
    static String normTc(String rawIn) {
        String raw = rawIn == null ? "" : rawIn.strip();
        String up = raw.toUpperCase();
        if (raw.isEmpty() || up.equals(NO_TC) || up.equals("NA") || up.equals("N/A") || up.equals("-")) {
            return NO_TC;
        }
        Matcher m = DIGITS.matcher(raw);
        return m.find() ? "TC-" + m.group(1) : raw;
    }

    /** Orders TC-<n> numerically first, then everything else lexically. */
    static int compareTc(String a, String b) {
        Matcher ma = TC_ID.matcher(a);
        Matcher mb = TC_ID.matcher(b);
        boolean na = ma.matches();
        boolean nb = mb.matches();
        if (na && nb) {
            return Integer.compare(Integer.parseInt(ma.group(1)), Integer.parseInt(mb.group(1)));
        }
        if (na != nb) {
            return na ? -1 : 1;
        }
        return a.compareTo(b);
    }

    static String cell(String text) {
        return (text == null ? "" : text).replace("|", "\\|").replace("\n", " ").strip();
    }

    private static List<String> lines(String text) {
        return new ArrayList<>(Arrays.asList(text.split("\r\n|\r|\n")));
    }

    private static int count(String s, char c) {
        int n = 0;
        for (int i = 0; i < s.length(); i++) {
            if (s.charAt(i) == c) {
                n++;
            }
        }
        return n;
    }

    // ---------------------------------------------------------- code scan

    /** Returns {tc: [(file stem, method, enabled)]}. */
    static Map<String, List<Hit>> scanCode(Path testDir) throws IOException {
        Map<String, List<Hit>> found = new LinkedHashMap<>();
        List<Path> files;
        try (Stream<Path> walk = Files.walk(testDir)) {
            files = walk.filter(p -> p.toString().endsWith(".java")).sorted().toList();
        }
        for (Path path : files) {
            String fileName = path.getFileName().toString();
            String stem = fileName.substring(0, fileName.length() - ".java".length());
            List<String> ids = new ArrayList<>();
            boolean disabled = false;
            String testBuf = null;
            boolean inBlockComment = false;
            for (String line : lines(new String(Files.readAllBytes(path), StandardCharsets.UTF_8))) {
                String s = line.strip();
                if (inBlockComment) {
                    inBlockComment = !s.contains("*/");
                    continue;
                }
                if (s.startsWith("/*")) {
                    inBlockComment = !s.contains("*/");
                    continue;
                }
                if (s.startsWith("//") || s.startsWith("*") || s.startsWith("import ")) {
                    continue;
                }
                // an @Test(...) may span several lines: accumulate until the parens balance
                if (testBuf != null || s.startsWith("@Test")) {
                    testBuf = (testBuf == null ? "" : testBuf) + " " + s;
                    if (count(testBuf, '(') <= count(testBuf, ')')) {
                        if (ENABLED_FALSE.matcher(testBuf).find()) {
                            disabled = true;
                        }
                        testBuf = null;
                    }
                    continue;
                }
                Matcher m = TMS_RE.matcher(s);
                if (m.find()) {
                    ids.add(normTc(m.group(1)));
                    continue;
                }
                if (s.startsWith("@")) {
                    continue;
                }
                if (TYPE_DECL.matcher(s).find()) {
                    ids.clear(); // class-level annotations are not per-test links
                    disabled = false;
                    continue;
                }
                if (!ids.isEmpty()) {
                    Matcher mm = METHOD_RE.matcher(line);
                    if (mm.find()) {
                        for (String tc : ids) {
                            found.computeIfAbsent(tc, k -> new ArrayList<>()).add(new Hit(stem, mm.group(1), !disabled));
                        }
                        ids.clear();
                        disabled = false;
                    }
                }
            }
        }
        return found;
    }

    // -------------------------------------------------------- existing doc

    /** Returns [(tc, scenario, status, note)] parsed from the committed doc. */
    static List<Existing> readExisting(Path doc) throws IOException {
        List<Existing> rows = new ArrayList<>();
        if (!Files.exists(doc)) {
            return rows;
        }
        for (String line : lines(Files.readString(doc, StandardCharsets.UTF_8))) {
            if (!line.startsWith("| ") || line.startsWith("| TC ID") || line.startsWith("|---")) {
                continue;
            }
            String[] raw = line.strip().split("(?<!\\\\)\\|", -1);
            List<String> parts = new ArrayList<>();
            for (int i = 1; i < raw.length - 1; i++) {
                parts.add(raw[i].strip().replace("\\|", "|"));
            }
            if (parts.size() >= 5) {
                rows.add(new Existing(parts.get(0), parts.get(1), parts.get(3), parts.get(4)));
            }
        }
        return rows;
    }

    // ---------------------------------------------------------------- feed

    /** Minimal RFC 4180 CSV reader (quoted fields, doubled quotes, embedded newlines). */
    static List<List<String>> parseCsv(String text) {
        List<List<String>> table = new ArrayList<>();
        List<String> row = new ArrayList<>();
        StringBuilder field = new StringBuilder();
        boolean quoted = false;
        boolean any = false;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (quoted) {
                if (c == '"') {
                    if (i + 1 < text.length() && text.charAt(i + 1) == '"') {
                        field.append('"');
                        i++;
                    } else {
                        quoted = false;
                    }
                } else {
                    field.append(c);
                }
            } else if (c == '"') {
                quoted = true;
                any = true;
            } else if (c == ',') {
                row.add(field.toString());
                field.setLength(0);
                any = true;
            } else if (c == '\n' || c == '\r') {
                if (c == '\r' && i + 1 < text.length() && text.charAt(i + 1) == '\n') {
                    i++;
                }
                if (any || field.length() > 0) {
                    row.add(field.toString());
                    table.add(row);
                } else {
                    table.add(new ArrayList<>()); // blank line (csv.reader yields [])
                }
                row = new ArrayList<>();
                field.setLength(0);
                any = false;
            } else {
                field.append(c);
                any = true;
            }
        }
        if (any || field.length() > 0) {
            row.add(field.toString());
            table.add(row);
        }
        return table;
    }

    /** Returns [(tc, scenario)] from a CSV export of sheet 'Playwright-Java Feed'. */
    static List<Pair> readFeed(String feedPath) throws IOException {
        Path p = Paths.get(feedPath);
        if (!Files.exists(p)) {
            throw new Fatal("feed file not found: " + p);
        }
        String name = p.getFileName().toString().toLowerCase();
        if (name.endsWith(".xlsx") || name.endsWith(".xlsm")) {
            throw new Fatal("reading .xlsx is not supported; export sheet '" + SHEET + "' to CSV and pass that.");
        }
        String text = Files.readString(p, StandardCharsets.UTF_8);
        if (text.startsWith("\uFEFF")) {
            text = text.substring(1);
        }
        List<List<String>> table = parseCsv(text);
        // the header row is the first row that names both a TC id column and a scenario column
        for (int i = 0; i < table.size(); i++) {
            List<String> row = table.get(i);
            int tcCol = -1;
            int scCol = -1;
            for (int j = 0; j < row.size(); j++) {
                String c = row.get(j).strip().toLowerCase();
                if (tcCol < 0 && TC_HEADERS.contains(c)) {
                    tcCol = j;
                }
                if (scCol < 0 && SCENARIO_HEADERS.contains(c)) {
                    scCol = j;
                }
            }
            if (tcCol >= 0 && scCol >= 0) {
                List<Pair> out = new ArrayList<>();
                for (List<String> r : table.subList(i + 1, table.size())) {
                    if (r.stream().noneMatch(c -> !c.strip().isEmpty())) {
                        continue;
                    }
                    String tc = tcCol < r.size() ? r.get(tcCol) : "";
                    String sc = scCol < r.size() ? r.get(scCol) : "";
                    if (!sc.strip().isEmpty() || !tc.strip().isEmpty()) {
                        out.add(new Pair(normTc(tc), sc.strip()));
                    }
                }
                return out;
            }
        }
        throw new Fatal("could not find a TC id column (" + String.join("/", TC_HEADERS.subList(0, 3))
                + ") and a scenario column (" + String.join("/", SCENARIO_HEADERS.subList(0, 3)) + ") in " + p);
    }

    // -------------------------------------------------------------- render

    static List<Row> build(Map<String, List<Hit>> code, List<Existing> existing, List<Pair> feed) {
        Map<String, String> notes = new HashMap<>();
        for (Existing e : existing) {
            notes.put(normTc(e.tc()) + "\u0000" + casefold(e.scenario()), e.note());
        }
        List<Pair> pairs = new ArrayList<>();
        if (feed != null) {
            pairs.addAll(feed);
        } else {
            Set<String> known = new HashSet<>();
            for (Existing e : existing) {
                String tc = normTc(e.tc());
                pairs.add(new Pair(tc, e.scenario()));
                known.add(tc);
            }
            for (String tc : code.keySet()) {
                if (!known.contains(tc)) {
                    pairs.add(new Pair(tc, "(scenario unknown - not in doc)"));
                }
            }
        }
        List<Pair> sorted = new ArrayList<>(new LinkedHashSet<>(pairs));
        sorted.sort(Comparator.<Pair, String>comparing(Pair::tc, TraceabilityGenerator::compareTc)
                .thenComparing(p -> casefold(p.scenario())));

        Map<String, Set<String>> scenarios = new HashMap<>();
        for (Pair p : sorted) {
            scenarios.computeIfAbsent(p.tc(), k -> new HashSet<>()).add(casefold(p.scenario()));
        }

        List<Row> rows = new ArrayList<>();
        for (Pair p : sorted) {
            String tc = p.tc();
            String sc = p.scenario();
            int n = scenarios.get(tc).size();
            String status;
            String loc;
            String auto;
            if (tc.equals(NO_TC)) {
                status = "needs-clarification";
                loc = "-";
                auto = "no TC id";
            } else if (NON_DESCRIPTIVE.matcher(sc).matches()) {
                status = "needs-clarification";
                loc = "-";
                auto = "non-descriptive name";
            } else if (n > 1) {
                status = "needs-clarification";
                loc = "-";
                auto = "TC id " + tc + " is used by " + n + " different scenarios";
            } else if (code.containsKey(tc)) {
                List<Hit> hits = code.get(tc);
                status = hits.stream().anyMatch(Hit::enabled) ? "implemented" : "skeleton";
                StringBuilder sb = new StringBuilder();
                for (Hit h : hits) {
                    if (sb.length() > 0) {
                        sb.append(", ");
                    }
                    sb.append('`').append(h.file()).append('#').append(h.method()).append('`');
                }
                loc = sb.toString();
                auto = "";
            } else {
                status = "needs-clarification";
                loc = "-";
                auto = "no test tagged with this TC id";
            }
            String old = notes.getOrDefault(tc + "\u0000" + casefold(sc), "");
            String note = !old.isEmpty() && !AUTO_DUP_NOTE.matcher(old).matches()
                    && !(status.equals("needs-clarification") && old.equals("no test tagged with this TC id"))
                    ? old : auto;
            if ((status.equals("implemented") || status.equals("skeleton")) && AUTO_DUP_NOTE.matcher(old).matches()) {
                note = "";
            }
            rows.add(new Row(tc, sc, loc, status, note));
        }
        return rows;
    }

    static String render(List<Row> rows) {
        List<String> out = new ArrayList<>();
        out.add(HEADER);
        for (Row r : rows) {
            out.add("| " + r.tc() + " | " + cell(r.scenario()) + " | " + r.loc() + " | " + r.status() + " | "
                    + cell(r.note()) + " |");
        }
        Map<String, Integer> totals = new TreeMap<>();
        for (Row r : rows) {
            totals.merge(r.status(), 1, Integer::sum);
        }
        out.add("");
        out.add("## Totals");
        out.add("");
        totals.forEach((k, v) -> out.add("- " + k + ": " + v));
        return String.join("\n", out) + "\n";
    }

    // ---------------------------------------------------------------- main

    public static void main(String[] argv) throws IOException {
        String feed = null;
        String docArg = DEFAULT_DOC;
        String testsArg = DEFAULT_TESTS;
        boolean check = false;
        boolean stdout = false;
        for (int i = 0; i < argv.length; i++) {
            switch (argv[i]) {
                case "--feed" -> feed = valueOf(argv, ++i, "--feed");
                case "--doc" -> docArg = valueOf(argv, ++i, "--doc");
                case "--tests" -> testsArg = valueOf(argv, ++i, "--tests");
                case "--check" -> check = true;
                case "--stdout" -> stdout = true;
                default -> {
                    System.err.println("error: unknown argument " + argv[i]
                            + "\nusage: [--feed feed.csv] [--check] [--stdout] [--doc path] [--tests dir]");
                    System.exit(2);
                }
            }
        }
        int exit = 0;
        try {
            exit = run(Paths.get(docArg), Paths.get(testsArg), feed, check, stdout);
        } catch (Fatal e) {
            System.err.println("error: " + e.getMessage());
            exit = 1;
        }
        System.exit(exit);
    }

    private static String valueOf(String[] argv, int i, String flag) {
        if (i >= argv.length) {
            throw new Fatal(flag + " needs a value");
        }
        return argv[i];
    }

    static int run(Path doc, Path tests, String feedPath, boolean check, boolean stdout) throws IOException {
        Map<String, List<Hit>> code = scanCode(tests);
        List<Pair> feed = feedPath != null ? readFeed(feedPath) : null;
        String text = render(build(code, readExisting(doc), feed));

        if (stdout) {
            System.out.print(text);
            return 0;
        }
        String docName = doc.getFileName().toString();
        String current = Files.exists(doc) ? Files.readString(doc, StandardCharsets.UTF_8).replace("\r\n", "\n") : null;
        if (check) {
            if (text.equals(current)) {
                System.out.println("OK: " + docName + " is up to date");
                return 0;
            }
            System.err.println("FAIL: " + docName
                    + " is out of date; run: java src/test/java/com/tools/TraceabilityGenerator.java");
            return 1;
        }
        if (text.equals(current)) {
            System.out.println(docName + " already up to date");
        } else {
            if (doc.toAbsolutePath().getParent() != null) {
                Files.createDirectories(doc.toAbsolutePath().getParent());
            }
            Files.writeString(doc, text, StandardCharsets.UTF_8);
            System.out.println("wrote " + doc.toAbsolutePath());
        }
        return 0;
    }
}
