---
name: furlenco-automation
description: Work on the Furlenco Playwright-Java web automation framework — add or fix page objects and tests, handle login/seed accounts, run staging tests, debug failures from traces, and keep README/master-prompt docs in sync. Use for any task in this repo (selectors, waits, login, test data, CI, traceability).
---

# Furlenco automation framework

Read `docs/PROJECT_MASTER_PROMPT.md` first (full rules and architecture) and `README.md` for commands.
Both contain AUTO blocks generated from source; never hand-edit text between `<!-- AUTO:... -->` markers.

## Workflow

1. **Understand the change.** Find the page object in `src/test/java/com/tests/pages/furlenco/` and the test in `src/test/java/com/tests/web/furlenco/`. Infrastructure lives in `src/main/java/com/framework/`.
2. **Implement** following the hard rules below.
3. **Verify.**
   - `mvn -q -B test-compile` (mvn may not be on PATH; use the Maven install and a JDK 25 `JAVA_HOME`).
   - Staging tests need the company VPN: check `stag.furlenco.com` resolves to private 192.168.x.x addresses first.
   - Run a focused class: `mvn -B test -Dtest='ClassName#method' -Dgroups=furlenco -Dheadless=true -Denv=staging`.
4. **Docs.** Run `java src/test/java/com/tools/ProjectDocsGenerator.java` and `java src/test/java/com/tools/TraceabilityGenerator.java`, then update the hand-written parts of README / master prompt if a convention changed. Both generators accept `--check`.
5. **Before finishing:** compile passes, both `--check` pass, leak grep is clean, and `src/test/resources/config.properties` is not staged unless intended.

## Hard rules

- No `Thread.sleep` / `waitForTimeout`. Use condition-based waits (`BasePage` helpers such as `waitQuietly`, `settle`, `WaitUtils.pollUntil`).
- Page objects hold selectors and actions; tests hold assertions.
- Selectors must tolerate A/B layout variants (MUI vs Radix). Prefer role/text/href-based fallbacks; use `>> visible=true` to skip hidden duplicates.
- Never reuse a lazy `Locator` after navigating: collect hrefs/values first (this caused 30s timeouts in the PLP).
- Login: `loginAsFreshUser()` (random number + default OTP from `test.user.otp`, completes signup with letters-only name) or a seed account via `SeedUsers` pools for tests needing history. Wait for the login *outcome*, not for the submit button: the form auto-submits on the 4th OTP digit.
- Signup inputs may have no placeholder: match `input[type='text']` / `input[type='email']` as fallbacks.
- Never hardcode or commit credentials, phone numbers, emails or seed files (`src/test/resources/seed/*.local.properties`, `*.consumed`). Reports show masked numbers only.
- Cancel-order tests consume seed accounts; do not run them casually.

## Debugging a failure

1. Read the failing test's `Call log:` in the Surefire output to see which locator timed out.
2. Failure traces are in `target/traces/*.zip` (unzip; `trace.trace` holds DOM snapshots, `trace.network` the requests).
3. Compare with the live page (public pages can be inspected in a browser without logging in).
4. Decide whether it is a locator problem, a test-data/product-selection problem, or an environment problem (VPN, availability, lag) before changing code.

## Test data

- Login data is recorded per attempt (masked) in Allure and `target/test-data/login-attempts.csv`.
- Dedicated staging test accounts exist (see the seed pools in the README); the numbers are secret and stay in the local seed file or CI secrets.
