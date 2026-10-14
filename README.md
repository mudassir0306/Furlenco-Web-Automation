# Playwright Hybrid Framework (Java)

Web UI and API test automation built with Playwright for Java, TestNG, and Allure.

## Prerequisites

- Java 25+
- Maven 3.9+
- Optional: [Allure CLI](https://allurereport.org/docs/install/) for local report viewing

## Quick start

```bash
mvn test
```

On first run on a new machine, Playwright downloads the required browsers automatically.

## Run commands

| Goal | Command |
|---|---|
| All tests | `mvn test` |
| Smoke tests only | `mvn test -Dgroups=smoke` |
| All Furlenco tests (CI default; needs staging VPN) | `mvn test -Dgroups=furlenco` |
| Web tests only | `mvn test -Dgroups=web` |
| Run on Firefox | `mvn test -Dbrowser=firefox` |
| Run headed | `mvn test -Dheadless=false` |
| Change parallel forks | `mvn test -Dforks=1` |
| Override environment | `mvn test -Denv=staging` |

Configuration is loaded by `com.framework.config.ConfigManager` from `src/test/resources/config/<env>.properties` (`-Denv=qa|staging|preprod|prod`, default `qa`). Keys resolve in this order:

1. `-Dkey=value`
2. OS env var (upper snake case, e.g. `WEB_BASE_URL`)
3. env-scoped key in the env file (e.g. `qa.web.base.url`)
4. plain key in the env file (e.g. `web.base.url`)


## Reports

Allure results are written to `target/allure-results`.

```bash
allure serve target/allure-results
```

When a web test fails, the framework also saves failure artifacts:

- screenshots: attached to the Allure report
- traces: `target/traces/<test>-trace.zip`

Open a trace locally with:

```bash
npx playwright show-trace target/traces/<test>-trace.zip
```

## Project layout

Single framework, `com.framework`:

- `src/main/java/com/framework` - `base/BasePage`, `config/ConfigManager`, `driver/PlaywrightManager`, `listeners/` (`TestListener`, retry), `utils/`
- `src/test/java/com/tests` - `base/` (BaseTest, BaseWebTest), `pages/furlenco` (page objects), `web/furlenco` tests
- `src/test/resources/config/<env>.properties` - environment configuration
- `src/test/resources/META-INF/services/org.testng.ITestNGListener` - registers `TestListener` and `RetryTransformer`
- `.github/workflows/tests.yml` - CI workflow for push, PR, and manual runs (default group `furlenco`; use a self-hosted runner on the staging VPN)

## Parallelism

Playwright Java is not safe to share a single browser instance across threads in one JVM. This project therefore uses Maven Surefire forks, with one JVM per fork, and test classes are distributed across those forks.

## CI

The GitHub Actions workflow in `.github/workflows/tests.yml` runs on push, pull requests, and manual dispatch, supports a test group and browser input, and uploads both Allure results and failure artifacts.

## Traceability

`docs/test-case-traceability.md` maps the test-case sheet (TC ids) to the tests tagged with `@TmsLink("TC-nnn")` in `src/test/java`. It is generated; do not edit the table by hand.

```bash
java src/test/java/com/tools/TraceabilityGenerator.java                  # regenerate the doc from the code + existing doc (run from the repo root, JDK 25)
java src/test/java/com/tools/TraceabilityGenerator.java --check          # exit 1 if the doc is stale (for CI)
java src/test/java/com/tools/TraceabilityGenerator.java --feed feed.csv  # use a CSV export of sheet 'Playwright-Java Feed' as scenario source (.xlsx not read)
```

A test with `enabled = false` is a `skeleton`, an enabled one is `implemented`. A TC id shared by several scenarios, a missing TC id or a non-descriptive scenario name is `needs-clarification`. Without `--feed`, scenario names and notes are read from the existing doc, so hand-written notes are kept.

## Login test data
Staging accepts any phone number with the default OTP (`test.user.otp`, 1234), so tests log in with a
**fresh random number each time** via `FurlencoLoginPage.loginAsFreshUser()` (new-user signup is
completed automatically) instead of a fixed, rate-limit-prone account. Tests that need pre-existing
orders, saved addresses or cancellable orders log in with a seed account via
`loginWithSeed(...)` (see below). Every login is recorded by `LoginDataRecorder`:

- Allure: a "Login test data" JSON attachment plus `login.*` parameters on the test.
- CSV: `target/test-data/login-attempts.csv` (timestamp, test, kind FRESH/EXISTING, label, masked
  phone, otp, name, email, outcome, otpWaitMs, submitWaitMs); safe across parallel forks.
- Phone numbers are **always masked** (first 2 + last 2 digits, e.g. `90******52`) and tagged with the
  seed label (`seed-cancel-buy-2`) or `fresh`; the full number is never written to any output.

**OTP lag:** the OTP request on staging can be slow (button disabled / `aria-busy`). The Continue
click waits for an idle button, then waits for the OTP boxes up to `login.otp.wait.ms` (default
60000) and for login to finish up to `login.submit.wait.ms` (default 60000). Elapsed times are logged
and recorded as `otpWaitMs` / `submitWaitMs`.

## Seed test accounts

A few tests need accounts that already have data. They come from `com.framework.utils.SeedUsers`:

| Pool (key / env var) | Used by |
|---|---|
| `seed.history.rentbuy` / `SEED_HISTORY_RENTBUY` | My Orders, order details (needs Rent and Buy history); read-only, round-robin |
| `seed.history.rent` / `SEED_HISTORY_RENT` | many fulfilled Rent orders; read-only |
| `seed.address` / `SEED_ADDRESS` | saved-address, change-address, checkout and successful-order tests; read-only |
| `seed.cancel.buy` / `SEED_CANCEL_BUY` | Cancel Buy order; **consumed** per run |
| `seed.cancel.unlimited` / `SEED_CANCEL_UNLIMITED` | Cancel Unlimited (Rent) order; **consumed** per run |

Supply them either locally in `src/test/resources/seed/seed-users.local.properties` (copy
`seed-users.example.properties`; comma-separated 10-digit numbers) or as environment variables /
CI secrets with the same comma-separated format (the file wins per pool). **Real numbers must never
be committed**: `*.local.properties` and `*.consumed` under `src/test/resources/seed/` are gitignored.

Cancel tests reserve an account via `SeedUsers.acquireCancelCandidate(pool)`, which records it in
`seed-consumed.local.consumed` under a file lock (safe for parallel forks) and never hands it out
again; an account whose cancel step never ran is released. In CI the consumed file is ephemeral, so
refill/rotate the cancel secrets after each run. When a pool is empty or exhausted the dependent
test is **skipped** (TestNG `SkipException`) with a clear reason rather than failed.
