# Rules for Claude Code in this repo

Full context: `docs/PROJECT_MASTER_PROMPT.md` (read it first). Overview: `README.md`.

- After any framework change (src/**, pom.xml, config), run `java src/test/java/com/tools/TraceabilityGenerator.java` and `java src/test/java/com/tools/ProjectDocsGenerator.java`, then update the hand-written parts of `README.md` and `docs/PROJECT_MASTER_PROMPT.md` if a convention, command or workflow changed. Never hand-edit text between `<!-- AUTO:... -->` markers.
- Never commit seed files (`src/test/resources/seed/*.local.properties`, `*.consumed`) or any real phone number, email or credential. Mask PII in logs and reports.
- No `Thread.sleep` / `waitForTimeout`; use condition-based waits (`BasePage` helpers, `WaitUtils.pollUntil`).
- Page objects hold selectors/actions, tests hold assertions. Log in with `loginAsFreshUser()` or `SeedUsers` pools.
- Do not run Furlenco tests (need staging VPN, consume seed accounts) unless asked.
- Before finishing: `mvn -q -B test-compile`, both generators with `--check`, and make sure `src/test/resources/config.properties` is not staged.
- Commit only when asked; small commits.
