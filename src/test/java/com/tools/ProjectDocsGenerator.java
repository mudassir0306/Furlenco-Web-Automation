package com.tools;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Keeps README.md and docs/PROJECT_MASTER_PROMPT.md in sync with the code. Only the blocks between
 * {@code <!-- AUTO:name:START -->} and {@code <!-- AUTO:name:END -->} markers are rewritten; all hand-written
 * text is left alone. Output is deterministic (no timestamps) and never contains config values, phone numbers
 * or seed values (key / pool NAMES only). Not a test (no TestNG annotations, name does not match surefire
 * patterns). Run from the repository root with the JDK source launcher, stdlib only:
 *
 * <pre>
 *   java src/test/java/com/tools/ProjectDocsGenerator.java            # rewrite the AUTO blocks
 *   java src/test/java/com/tools/ProjectDocsGenerator.java --check    # exit 1 if any block is stale
 * </pre>
 */
public final class ProjectDocsGenerator {

    private static final Path ROOT = Paths.get("").toAbsolutePath();
    private static final Pattern BLOCK = Pattern.compile(
            "(<!-- AUTO:([A-Za-z0-9_-]+):START -->)(.*?)(<!-- AUTO:\\2:END -->)", Pattern.DOTALL);
    private static final Pattern ANY_MARKER = Pattern.compile("<!-- AUTO:([A-Za-z0-9_-]+):(START|END) -->");

    /** Doc file -> the blocks it must contain. */
    private static final Map<String, List<String>> DOCS = new LinkedHashMap<>();

    static {
        DOCS.put("README.md", List.of("tech-stack", "run-commands", "framework-tree", "page-objects",
                "test-classes", "config-files", "seed-pools", "traceability-summary"));
        DOCS.put("docs/PROJECT_MASTER_PROMPT.md", List.of("tech-stack", "framework-tree", "page-objects",
                "test-classes", "config-files", "seed-pools", "run-commands"));
    }

    private ProjectDocsGenerator() {
    }

    record TestClass(String name, int enabled, int skeleton, java.util.Set<String> groups) {
    }

    public static void main(String[] args) throws IOException {
        boolean check = false;
        for (String a : args) {
            if (a.equals("--check")) {
                check = true;
            } else {
                System.err.println("Unknown argument: " + a + " (supported: --check)");
                System.exit(2);
            }
        }
        if (!Files.isRegularFile(ROOT.resolve("pom.xml"))) {
            System.err.println("ProjectDocsGenerator: run from the repository root (pom.xml not found in " + ROOT + ")");
            System.exit(2);
        }

        List<TestClass> tests = scanTests();
        Map<String, String> blocks = new LinkedHashMap<>();
        blocks.put("tech-stack", techStack());
        blocks.put("framework-tree", frameworkTree());
        blocks.put("page-objects", pageObjects());
        blocks.put("test-classes", testClasses(tests));
        blocks.put("config-files", configFiles());
        blocks.put("seed-pools", seedPools());
        blocks.put("run-commands", runCommands(tests));
        blocks.put("traceability-summary", traceabilitySummary());

        List<String> stale = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        for (Map.Entry<String, List<String>> doc : DOCS.entrySet()) {
            Path file = ROOT.resolve(doc.getKey());
            if (!Files.isRegularFile(file)) {
                errors.add(doc.getKey() + ": file not found");
                continue;
            }
            String raw = Files.readString(file, StandardCharsets.UTF_8);
            boolean crlf = raw.contains("\r\n");
            String text = raw.replace("\r\n", "\n");
            // validate markers
            java.util.Set<String> seen = new TreeSet<>();
            Matcher mm = ANY_MARKER.matcher(text);
            while (mm.find()) {
                String n = mm.group(1);
                if (!blocks.containsKey(n)) {
                    errors.add(doc.getKey() + ": unknown AUTO block '" + n + "' (known: " + blocks.keySet() + ")");
                }
                seen.add(n);
            }
            Matcher bm = BLOCK.matcher(text);
            java.util.Set<String> complete = new TreeSet<>();
            while (bm.find()) {
                complete.add(bm.group(2));
            }
            for (String n : seen) {
                if (!complete.contains(n)) {
                    errors.add(doc.getKey() + ": AUTO block '" + n + "' has a START without a matching END (or vice versa)");
                }
            }
            for (String need : doc.getValue()) {
                if (!complete.contains(need)) {
                    errors.add(doc.getKey() + ": missing marker pair <!-- AUTO:" + need + ":START --> / <!-- AUTO:" + need + ":END -->");
                }
            }
            if (!errors.isEmpty()) {
                continue;
            }
            StringBuilder out = new StringBuilder();
            Matcher m = BLOCK.matcher(text);
            int last = 0;
            while (m.find()) {
                out.append(text, last, m.start());
                String body = "\n" + blocks.get(m.group(2)).strip() + "\n";
                out.append(m.group(1)).append(body).append(m.group(4));
                if (!m.group(3).equals(body)) {
                    stale.add(doc.getKey() + " [" + m.group(2) + "]");
                }
                last = m.end();
            }
            out.append(text.substring(last));
            String result = out.toString();
            if (!result.equals(text) && !check) {
                Files.writeString(file, crlf ? result.replace("\n", "\r\n") : result, StandardCharsets.UTF_8);
            }
        }

        if (!errors.isEmpty()) {
            errors.forEach(e -> System.err.println("ProjectDocsGenerator ERROR: " + e));
            System.exit(2);
        }
        if (check) {
            if (stale.isEmpty()) {
                System.out.println("Project docs are up to date.");
            } else {
                System.err.println("Project docs are STALE (" + stale.size() + " block(s)):");
                stale.forEach(s -> System.err.println("  - " + s));
                System.err.println("Fix: run `java src/test/java/com/tools/ProjectDocsGenerator.java` and commit README.md / docs/.");
                System.exit(1);
            }
        } else {
            System.out.println(stale.isEmpty() ? "Project docs already up to date."
                    : "Updated " + stale.size() + " block(s): " + String.join(", ", stale));
        }
    }

    // ---------------------------------------------------------------- blocks

    private static String techStack() throws IOException {
        String pom = Files.readString(ROOT.resolve("pom.xml"), StandardCharsets.UTF_8);
        Map<String, String> props = new LinkedHashMap<>();
        Matcher pm = Pattern.compile("<properties>(.*?)</properties>", Pattern.DOTALL).matcher(pom);
        if (pm.find()) {
            Matcher kv = Pattern.compile("<([\\w.\\-]+)>([^<]*)</\\1>").matcher(pm.group(1));
            while (kv.find()) {
                props.put(kv.group(1), kv.group(2).trim());
            }
        }
        StringBuilder sb = new StringBuilder("| Component | Version |\n|---|---|\n");
        sb.append("| Java (`maven.compiler.release`) | ").append(props.getOrDefault("maven.compiler.release", "?")).append(" |\n");
        // dependencies/plugins: artifactId + version
        Matcher dm = Pattern.compile(
                "<(dependency|plugin)>\\s*<groupId>([^<]+)</groupId>\\s*<artifactId>([^<]+)</artifactId>\\s*(?:<version>([^<]+)</version>)?",
                Pattern.DOTALL).matcher(pom);
        java.util.Set<String> done = new TreeSet<>();
        while (dm.find()) {
            String art = dm.group(3);
            String ver = dm.group(4);
            if (ver == null || !done.add(art)) {
                continue;
            }
            ver = resolve(ver, props);
            sb.append("| `").append(art).append("` | ").append(ver).append(" |\n");
        }
        return sb.toString();
    }

    private static String resolve(String v, Map<String, String> props) {
        Matcher m = Pattern.compile("\\$\\{([^}]+)}").matcher(v);
        StringBuilder out = new StringBuilder();
        while (m.find()) {
            m.appendReplacement(out, Matcher.quoteReplacement(props.getOrDefault(m.group(1), m.group(0))));
        }
        m.appendTail(out);
        return out.toString();
    }

    private static String frameworkTree() throws IOException {
        Path base = ROOT.resolve("src/main/java/com/framework");
        StringBuilder sb = new StringBuilder("```text\nsrc/main/java/com/framework/\n");
        if (!Files.isDirectory(base)) {
            return sb.append("```\n").toString();
        }
        List<Path> dirs;
        try (Stream<Path> s = Files.list(base)) {
            dirs = s.filter(Files::isDirectory).sorted().toList();
        }
        for (Path d : dirs) {
            sb.append("  ").append(d.getFileName()).append("/\n");
            List<Path> files;
            try (Stream<Path> s = Files.list(d)) {
                files = s.filter(p -> p.toString().endsWith(".java")).sorted().toList();
            }
            for (Path f : files) {
                String name = f.getFileName().toString().replace(".java", "");
                String sum = summary(f);
                sb.append("    ").append(name).append(sum.isEmpty() ? "" : "  - " + sum).append("\n");
            }
        }
        return sb.append("```\n").toString();
    }

    private static String pageObjects() throws IOException {
        Path dir = ROOT.resolve("src/test/java/com/tests/pages/furlenco");
        StringBuilder sb = new StringBuilder("| Class | Summary |\n|---|---|\n");
        if (Files.isDirectory(dir)) {
            List<Path> files;
            try (Stream<Path> s = Files.list(dir)) {
                files = s.filter(p -> p.toString().endsWith(".java")).sorted().toList();
            }
            for (Path f : files) {
                String name = f.getFileName().toString().replace(".java", "");
                sb.append("| `").append(name).append("` | ").append(summary(f).replace("|", "\\|")).append(" |\n");
            }
        }
        return sb.toString();
    }

    private static List<TestClass> scanTests() throws IOException {
        Path dir = ROOT.resolve("src/test/java/com/tests/web");
        List<TestClass> out = new ArrayList<>();
        if (!Files.isDirectory(dir)) {
            return out;
        }
        List<Path> files;
        try (Stream<Path> s = Files.walk(dir)) {
            files = s.filter(p -> p.toString().endsWith(".java")).sorted().toList();
        }
        Pattern test = Pattern.compile("@Test\\b\\s*(\\(([^)]*)\\))?");
        Pattern groups = Pattern.compile("groups\\s*=\\s*(\\{[^}]*\\}|\"[^\"]*\")");
        for (Path f : files) {
            String src = Files.readString(f, StandardCharsets.UTF_8);
            int en = 0;
            int sk = 0;
            java.util.Set<String> g = new TreeSet<>();
            Matcher m = test.matcher(src);
            while (m.find()) {
                String args = m.group(2) == null ? "" : m.group(2);
                if (args.replaceAll("\\s", "").contains("enabled=false")) {
                    sk++;
                } else {
                    en++;
                }
                Matcher gm = groups.matcher(args);
                if (gm.find()) {
                    Matcher q = Pattern.compile("\"([^\"]+)\"").matcher(gm.group(1));
                    while (q.find()) {
                        g.add(q.group(1));
                    }
                }
            }
            if (en + sk > 0) {
                out.add(new TestClass(f.getFileName().toString().replace(".java", ""), en, sk, g));
            }
        }
        return out;
    }

    private static String testClasses(List<TestClass> tests) {
        StringBuilder sb = new StringBuilder("| Test class | Enabled | Skeleton | Groups |\n|---|---|---|---|\n");
        int e = 0;
        int s = 0;
        for (TestClass t : tests) {
            sb.append("| `").append(t.name()).append("` | ").append(t.enabled()).append(" | ").append(t.skeleton())
                    .append(" | ").append(String.join(", ", t.groups())).append(" |\n");
            e += t.enabled();
            s += t.skeleton();
        }
        sb.append("| **Total (").append(tests.size()).append(" classes)** | **").append(e).append("** | **").append(s)
                .append("** | |\n");
        return sb.toString();
    }

    private static List<String> keys(Path f) throws IOException {
        TreeSet<String> k = new TreeSet<>();
        for (String line : Files.readAllLines(f, StandardCharsets.UTF_8)) {
            String t = line.strip();
            if (t.isEmpty() || t.startsWith("#") || t.startsWith("!")) {
                continue;
            }
            int i = t.indexOf('=');
            if (i > 0) {
                k.add(t.substring(0, i).strip());
            }
        }
        return new ArrayList<>(k);
    }

    private static String configFiles() throws IOException {
        Path dir = ROOT.resolve("src/test/resources/config");
        StringBuilder sb = new StringBuilder();
        List<Path> files = new ArrayList<>();
        if (Files.isDirectory(dir)) {
            try (Stream<Path> s = Files.list(dir)) {
                files = s.filter(p -> p.toString().endsWith(".properties")).sorted().toList();
            }
        }
        Map<String, List<String>> perEnv = new TreeMap<>();
        for (Path f : files) {
            perEnv.put(f.getFileName().toString(), keys(f));
        }
        sb.append("Environment files (`src/test/resources/config/`): ")
                .append(perEnv.keySet().stream().map(n -> "`" + n + "`").collect(Collectors.joining(", ")))
                .append(". Key names only; values are never listed here.\n\n");
        if (perEnv.isEmpty()) {
            return sb.toString();
        }
        TreeSet<String> common = new TreeSet<>(perEnv.values().iterator().next());
        TreeSet<String> all = new TreeSet<>();
        perEnv.values().forEach(v -> {
            common.retainAll(v);
            all.addAll(v);
        });
        sb.append("Keys present in every env file (").append(common.size()).append("): ")
                .append(common.stream().map(k -> "`" + k + "`").collect(Collectors.joining(", "))).append("\n");
        // keys not in every env file, grouped by shared prefix (3+ keys) and the set of envs defining them
        Map<String, List<String>> byPrefix = new TreeMap<>();
        for (String k : all) {
            if (!common.contains(k)) {
                String[] seg = k.split("\\.");
                String prefix = seg.length >= 3 ? seg[0] + "." + seg[1] : k;
                byPrefix.computeIfAbsent(prefix, x -> new ArrayList<>()).add(k);
            }
        }
        List<String[]> rows = new ArrayList<>();
        for (Map.Entry<String, List<String>> e : byPrefix.entrySet()) {
            Map<String, List<String>> byEnv = new TreeMap<>();
            for (String k : e.getValue()) {
                String envs = perEnv.entrySet().stream().filter(x -> x.getValue().contains(k))
                        .map(x -> x.getKey().replace(".properties", "")).collect(Collectors.joining(", "));
                byEnv.computeIfAbsent(envs, x -> new ArrayList<>()).add(k);
            }
            for (Map.Entry<String, List<String>> g : byEnv.entrySet()) {
                String label = g.getValue().size() >= 3
                        ? "`" + e.getKey() + ".*` (" + g.getValue().size() + " keys)"
                        : g.getValue().stream().map(k -> "`" + k + "`").collect(Collectors.joining(", "));
                rows.add(new String[] {label, g.getKey()});
            }
        }
        if (!rows.isEmpty()) {
            sb.append("\n\nKeys set only in some env files:\n\n| Key(s) | Defined in |\n|---|---|\n");
            for (String[] r : rows) {
                sb.append("| ").append(r[0]).append(" | ").append(r[1]).append(" |\n");
            }
        }
        return sb.toString() + "\n";
    }

    private static String seedPools() throws IOException {
        Path f = ROOT.resolve("src/test/resources/seed/seed-users.example.properties");
        StringBuilder sb = new StringBuilder("| Pool key | Environment variable |\n|---|---|\n");
        if (Files.isRegularFile(f)) {
            for (String k : keys(f)) {
                sb.append("| `").append(k).append("` | `").append(k.replace('.', '_').toUpperCase()).append("` |\n");
            }
        }
        return sb.toString();
    }

    private static String runCommands(List<TestClass> tests) throws IOException {
        String pom = Files.readString(ROOT.resolve("pom.xml"), StandardCharsets.UTF_8);
        Matcher fm = Pattern.compile("<forks>([^<]*)</forks>").matcher(pom);
        String forks = fm.find() ? fm.group(1) : "?";
        Map<String, Integer> groupCounts = new TreeMap<>();
        // count tests per group across classes (class-level approximation: number of enabled tests in classes using the group)
        for (TestClass t : tests) {
            for (String g : t.groups()) {
                groupCounts.merge(g, t.enabled(), Integer::sum);
            }
        }
        StringBuilder sb = new StringBuilder("| Goal | Command |\n|---|---|\n");
        sb.append("| Compile only | `mvn -q -B test-compile` |\n");
        sb.append("| All tests | `mvn test` |\n");
        for (String g : groupCounts.keySet()) {
            sb.append("| Group `").append(g).append("` | `mvn test -Dgroups=").append(g).append("` |\n");
        }
        sb.append("| Headed browser | `mvn test -Dheadless=false` |\n");
        sb.append("| Other browser | `mvn test -Dbrowser=firefox` |\n");
        sb.append("| Parallel forks (default ").append(forks).append(") | `mvn test -Dforks=1` |\n");
        sb.append("| Environment (`qa`, `staging`, `preprod`, `prod`) | `mvn test -Denv=staging` |\n");
        sb.append("| Regenerate docs | `java src/test/java/com/tools/ProjectDocsGenerator.java` |\n");
        sb.append("| Check docs are current | `java src/test/java/com/tools/ProjectDocsGenerator.java --check` |\n");
        sb.append("| Regenerate traceability | `java src/test/java/com/tools/TraceabilityGenerator.java` |\n");
        sb.append("\nTestNG groups in use: ")
                .append(groupCounts.keySet().stream().map(g -> "`" + g + "`").collect(Collectors.joining(", ")))
                .append(".\n");
        return sb.toString();
    }

    private static String traceabilitySummary() throws IOException {
        Path f = ROOT.resolve("docs/test-case-traceability.md");
        if (!Files.isRegularFile(f)) {
            return "Traceability document not generated yet.\n";
        }
        String text = Files.readString(f, StandardCharsets.UTF_8).replace("\r\n", "\n");
        int i = text.indexOf("## Totals");
        if (i < 0) {
            return "Traceability totals not found in `docs/test-case-traceability.md`.\n";
        }
        String tail = text.substring(i + "## Totals".length()).strip();
        List<String> lines = tail.lines().map(String::strip).filter(l -> l.startsWith("- ")).toList();
        return "Totals from `docs/test-case-traceability.md`: "
                + lines.stream().map(l -> l.substring(2)).collect(Collectors.joining("; ")) + ".\n";
    }

    // ---------------------------------------------------------------- helpers

    /** First sentence of the javadoc directly above the top-level type declaration, cleaned of markup. */
    private static String summary(Path f) throws IOException {
        String src = Files.readString(f, StandardCharsets.UTF_8);
        Matcher m = Pattern.compile("/\\*\\*((?:(?!\\*/).)*?)\\*/\\s*(?:@\\w+(?:\\([^)]*\\))?\\s*)*"
                + "(?:public\\s+|final\\s+|abstract\\s+)*(?:class|interface|enum|record)\\s+\\w+", Pattern.DOTALL).matcher(src);
        if (!m.find()) {
            return "";
        }
        String doc = m.group(1).lines().map(l -> l.strip().replaceFirst("^\\*+\\s?", "")).collect(Collectors.joining(" "));
        doc = doc.replaceAll("\\{@(?:code|link|linkplain)\\s+([^}]*)}", "$1")
                .replace("&lt;", "<").replace("&gt;", ">").replace("&amp;", "&")
                .replaceAll("</?(?:b|i|p|ul|ol|li|pre|code|br|em|strong|tt)\\s*/?>", " ")
                .replaceAll("<([^<>]*)>", "$1")
                .replaceAll("@\\w+.*$", "")
                .replaceAll("\\s+", " ").strip();
        Matcher sm = Pattern.compile("(?<!e\\.g)(?<!i\\.e)\\.\\s+(?=[A-Z])").matcher(doc);
        if (sm.find()) {
            doc = doc.substring(0, sm.start() + 1);
        }
        if (doc.length() > 160) {
            doc = doc.substring(0, 157).stripTrailing() + "...";
        }
        return doc.replace("#", "").strip();
    }
}
