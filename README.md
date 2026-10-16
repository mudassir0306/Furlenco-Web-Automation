# Furlenco Web Automation (Java)

Web UI test automation for the Furlenco storefront, built with Playwright for Java, TestNG, AssertJ and Allure.
It is a Furlenco-only suite: page objects and tests live under `com.tests`, shared infrastructure under `com.framework`.

> Sections marked `AUTO` are generated from the code (see [Docs auto-update](#docs-auto-update)). Do not edit between the markers.
> For AI assistants and new engineers there is also a deeper context file: [docs/PROJECT_MASTER_PROMPT.md](docs/PROJECT_MASTER_PROMPT.md).

## Quick start

Prerequisites: JDK 25+, Maven 3.9+. Optional: [Allure CLI](https://allurereport.org/docs/install/) and Node (`npx`) for trace viewing.
Playwright downloads its browsers on the first run.

Staging (`stag.furlenco.com`) requires the company VPN. Run locally on the VPN or use a self-hosted CI runner on it.

```bash
mvn -q -B test-compile          # compile everything
mvn test -Dgroups=smoke         # run the smoke group
```

## Run commands

<!-- AUTO:run-commands:START -->
| Goal | Command |
|---|---|
| Compile only | `mvn -q -B test-compile` |
| All tests | `mvn test` |
| Group `auth-profile` | `mvn test -Dgroups=auth-profile` |
| Group `backlog` | `mvn test -Dgroups=backlog` |
| Group `cart` | `mvn test -Dgroups=cart` |
| Group `checkout-payments` | `mvn test -Dgroups=checkout-payments` |
| Group `city-selector` | `mvn test -Dgroups=city-selector` |
| Group `furlenco` | `mvn test -Dgroups=furlenco` |
| Group `offers` | `mvn test -Dgroups=offers` |
| Group `orders-returns` | `mvn test -Dgroups=orders-returns` |
| Group `payment-critical` | `mvn test -Dgroups=payment-critical` |
| Group `regression` | `mvn test -Dgroups=regression` |
| Group `renewals` | `mvn test -Dgroups=renewals` |
| Group `smoke` | `mvn test -Dgroups=smoke` |
| Group `web` | `mvn test -Dgroups=web` |
| Headed browser | `mvn test -Dheadless=false` |
| Other browser | `mvn test -Dbrowser=firefox` |
| Parallel forks (default 3) | `mvn test -Dforks=1` |
| Environment (`qa`, `staging`, `preprod`, `prod`) | `mvn test -Denv=staging` |
| Regenerate docs | `java src/test/java/com/tools/ProjectDocsGenerator.java` |
| Check docs are current | `java src/test/java/com/tools/ProjectDocsGenerator.java --check` |
| Regenerate traceability | `java src/test/java/com/tools/TraceabilityGenerator.java` |

TestNG groups in use: `auth-profile`, `backlog`, `cart`, `checkout-payments`, `city-selector`, `furlenco`, `offers`, `orders-returns`, `payment-critical`, `regression`, `renewals`, `smoke`, `web`.
<!-- AUTO:run-commands:END -->

Properties resolve in this order: `-Dkey=value`, OS env var (upper snake case, e.g. `WEB_BASE_URL`), env-scoped key (`qa.web.base.url`), plain key in the env file.

## Architecture

- **Page Object Model.** Page objects (`com.tests.pages.furlenco`) hold selectors and actions and extend `com.framework.base.BasePage`. Tests (`com.tests.web.furlenco`) hold the assertions.
- **Browser lifecycle.** `BaseWebTest` (`@BeforeMethod` / `@AfterMethod`) starts and tears down a Playwright session per test through `PlaywrightManager` (thread-local).
- **Listeners.** `TestListener` (screenshots, traces, Allure attachments) and `RetryTransformer` (attaches `RetryAnalyzer` to every `@Test`) are registered in `META-INF/services/org.testng.ITestNGListener`.
- **Parallelism.** Playwright Java is not safe to share across threads, so Surefire forks (one JVM each, `-Dforks`) distribute test classes.
- **Waits.** No `Thread.sleep` / `waitForTimeout`. Use condition-based waits (`BasePage` helpers, `WaitUtils.pollUntil`).

## Project layout

<!-- AUTO:framework-tree:START -->
```text
src/main/java/com/framework/
  base/
    BasePage  - Common actions for all Page Objects.
  config/
    ConfigManager  - Thread-safe singleton config loader.
  driver/
    PlaywrightManager  - Manages Playwright / Browser / BrowserContext / Page lifecycles per test thread.
  listeners/
    RetryAnalyzer  - Retries a failed test up to retry.max.count times (default 2, configurable per env).
    RetryTransformer  - Registered in testng.xml under listeners.
    TestListener  - Registered in testng.xml under listeners.
  utils/
    EnvironmentCheck  - Fails fast when the target environment is unreachable (typically: staging while the company VPN is down), so a run produces one clear message instead of doze...
    LoginDataRecorder  - Records every login attempt's test data: an Allure attachment + parameters on the current test, and a row in target/test-data/login-attempts.csv.
    LoginTestData  - Immutable record of one login attempt's test data, written to the Allure report and to target/test-data/login-attempts.csv by LoginDataRecorder.
    RandomDataUtils  - Generates random, non-sensitive test data — primarily Indian mobile numbers for flows that need a phone number guaranteed not to already exist in the target...
    SeedUsers  - Pre-existing staging test accounts (with orders, saved addresses, cancellable orders), loaded from a local, gitignored file (src/test/resources/seed/seed-use...
    WaitUtils  - Backend-state polling helper — for cases Playwright's built-in auto-waiting doesn't cover, e.g. "poll the payment status API until it flips to SUCCESS/FAILED...
```
<!-- AUTO:framework-tree:END -->

Other locations: `src/test/java/com/tests/base` (BaseTest, BaseWebTest), `src/test/java/com/tools` (generators, not tests), `src/test/resources/config` (env files), `src/test/resources/seed` (seed example), `.github/workflows/tests.yml` (CI), `.githooks/` (pre-commit).

### Tech stack

<!-- AUTO:tech-stack:START -->
| Component | Version |
|---|---|
| Java (`maven.compiler.release`) | 25 |
| `playwright` | 1.47.0 |
| `testng` | 7.10.2 |
| `assertj-core` | 3.26.3 |
| `jackson-databind` | 2.17.2 |
| `log4j-core` | 2.24.1 |
| `log4j-api` | 2.24.1 |
| `allure-testng` | 2.29.0 |
| `maven-surefire-plugin` | 3.6.0 |
| `aspectjweaver` | 1.9.25 |
| `maven-compiler-plugin` | 3.14.1 |
| `exec-maven-plugin` | 3.5.0 |
<!-- AUTO:tech-stack:END -->

### Page objects

<!-- AUTO:page-objects:START -->
| Class | Summary |
|---|---|
| `FurlencoCartDrawer` | Page Object for the Furlenco Cart screen — a real page at /cart, verified against a live session, not a drawer overlay (the class name is kept for backward c... |
| `FurlencoCartPage` | The full-page cart at /cart, verified live on stag.furlenco.com 2026-10-06. |
| `FurlencoCheckoutAddressPage` | Page Object for the "Select Delivery Address" checkout step, verified live at /checkout/address?vertical=rent\|buy&cartId=id. |
| `FurlencoExperiments` | Pins Furlenco's live A/B experiment cookie (_experiments) to fixed, known variants before navigation, so automated runs get a deterministic DOM instead of a... |
| `FurlencoHomePage` | Page Object for Furlenco Home Page (www.furlenco.com). |
| `FurlencoLoginPage` | Page Object for the Furlenco Login dialog: phone number + 4-digit OTP (not a password) — test.user.otp in the environment properties files holds the default... |
| `FurlencoMyOrdersPage` | Page Object for "My Orders" (/my/orders) and an individual order's detail page (/my/orders/cart-checkouts/id) — verified live. |
| `FurlencoNewAddressPage` | Page Object for the "Add New Address" flow, verified live 2026-10-05 at /checkout/address/new?vertical=rent&cartId=id: a map with a search box (location-sea... |
| `FurlencoNewUserSignupPage` | Page Object for the new-user signup step reached after OTP verification for a phone number that isn't already registered: "Hey, Looks like you are new here!"... |
| `FurlencoOrderResultPage` | Page Object for the post-payment result screen (Order Processing -> Order Success / Order Failure in the manual test suite). |
| `FurlencoOrderSummaryPage` | Page Object for the "Order Summary" checkout step, verified live — reached after confirming a delivery address (FurlencoCheckoutAddressPage). |
| `FurlencoPaymentPage` | Page Object for the Payment screen, verified live at /my/payment?payment_id=id: a Razorpay test-mode integration with "UPI" and "Credit Card/Debit Card" tabs... |
| `FurlencoPdpPage` | Rent product details page (/rent/products/slug), layout verified live on stag.furlenco.com 2026-10-06. |
| `FurlencoPlpPage` | Page Object for the Product Listing Page (PLP) shared across Rent / Buy / Unlmtd verticals. |
| `FurlencoProductPage` | Page Object for Furlenco Product Details Page (PDP). |
| `FurlencoSearchResultsPage` | Page Object for Furlenco Search Results / Product Listing Page. |
| `FurlencoStockPage` | Rent category listing page by city, e.g. /bengaluru/appliances-on-rent?collectionType=CATEGORY_RENT (verified live on stag.furlenco.com 2026-10-06). |
| `LoginDefaults` | Config lookups for login test data, so tests don't repeat config.get chains. |
<!-- AUTO:page-objects:END -->

### Test classes

<!-- AUTO:test-classes:START -->
| Test class | Enabled | Skeleton | Groups |
|---|---|---|---|
| `FurlencoAddressTest` | 7 | 0 | furlenco, regression, smoke, web |
| `FurlencoB2BSmokeTest` | 1 | 0 | furlenco, smoke, web |
| `FurlencoCancelOrderTest` | 2 | 0 | furlenco, payment-critical, regression, web |
| `FurlencoCartCouponTest` | 4 | 0 | furlenco, regression, web |
| `FurlencoCartEmptyStateTest` | 1 | 0 | furlenco, regression, web |
| `FurlencoCartFlowTest` | 2 | 0 | furlenco, regression, smoke, web |
| `FurlencoCartItemsTest` | 2 | 0 | furlenco, regression, web |
| `FurlencoCartQuantityAndPersistenceTest` | 4 | 0 | furlenco, regression, web |
| `FurlencoCartRenderingTest` | 3 | 0 | furlenco, regression, web |
| `FurlencoCartSummaryTest` | 0 | 6 | backlog, cart, furlenco, offers |
| `FurlencoCartTenureSelectionTest` | 4 | 0 | furlenco, regression, web |
| `FurlencoCartVasTest` | 3 | 0 | furlenco, regression, web |
| `FurlencoCheckoutAutopayTest` | 0 | 2 | backlog, checkout-payments, furlenco |
| `FurlencoCheckoutFlowTest` | 11 | 0 | furlenco, payment-critical, regression, smoke, web |
| `FurlencoCityAndPincodeTest` | 5 | 0 | furlenco, regression, smoke, web |
| `FurlencoHomePageTest` | 4 | 0 | furlenco, regression, smoke, web |
| `FurlencoLoginTest` | 6 | 1 | backlog, city-selector, furlenco, payment-critical, regression, smoke, web |
| `FurlencoNewUserSignupTest` | 5 | 0 | furlenco, regression, web |
| `FurlencoPdpLayoutTest` | 1 | 0 | furlenco, smoke, web |
| `FurlencoPdpNavigationTest` | 6 | 0 | furlenco, regression, smoke, web |
| `FurlencoPincodeAvailabilityTest` | 3 | 0 | furlenco, regression, smoke, web |
| `FurlencoPlpNavigationTest` | 13 | 0 | furlenco, regression, smoke, web |
| `FurlencoProfileAndOrdersTest` | 5 | 4 | auth-profile, backlog, furlenco, orders-returns, regression, smoke, web |
| `FurlencoRenewalTest` | 0 | 5 | backlog, furlenco, renewals |
| `FurlencoSearchAndBrowseTest` | 2 | 0 | furlenco, regression, smoke, web |
| `FurlencoStockAvailabilityTest` | 2 | 0 | furlenco, regression, smoke, web |
| `FurlencoSuccessfulOrderTest` | 2 | 0 | furlenco, payment-critical, regression, web |
| **Total (27 classes)** | **98** | **18** | |
<!-- AUTO:test-classes:END -->

## Writing a test

1. **Page object** in `src/test/java/com/tests/pages/furlenco`: extend `BasePage`, keep selectors private, expose intent-level methods (`@Step` for Allure), return `this` or the next page. Selectors must tolerate Furlenco's A/B experiment variants (see `FurlencoExperiments`). Never use `Thread.sleep`.
2. **Test class** in `src/test/java/com/tests/web/furlenco`: extend `BaseWebTest`, annotate with `@Epic` / `@Feature`, and each test with `@Test(groups = {...})`, `@Severity`, `@Description` and `@TmsLink("TC-nnn")` when it maps to a test-case sheet row. Assertions (AssertJ) live in the test.
3. **Groups:** always include `web` and `furlenco`; add `smoke`, `regression` or `payment-critical` as appropriate.
4. **Retries** are applied automatically (`RetryTransformer`); do not add retry logic to tests.
5. Run `mvn -q -B test-compile`, then the regeneration commands in [Docs auto-update](#docs-auto-update).

## Login and test data

Staging accepts any phone number with the default OTP (`test.user.otp`, `1234`), so tests log in with a fresh random number
each time via `FurlencoLoginPage.loginAsFreshUser()` (new-user signup is completed automatically). Tests that need existing
orders, saved addresses or cancellable orders use `loginWithSeed(...)` (see below).

Every login is recorded by `LoginDataRecorder` as `LoginTestData`:

- Allure: a "Login test data" JSON attachment plus `login.*` parameters.
- CSV: `target/test-data/login-attempts.csv` (safe across parallel forks).
- Phone numbers are always masked (first 2 + last 2 digits) and tagged with `fresh` or the seed label. The full number is never written to any output.

**OTP and signup lag.** The OTP request can be slow and the OTP auto-submits on the 4th digit, so the code waits for an outcome
(dialog closed or signup prompt), not for a button. Limits: `login.otp.wait.ms` and `login.submit.wait.ms` (default 60000).
Elapsed times are logged and recorded as `otpWaitMs` / `submitWaitMs`.

## Seed accounts

<!-- AUTO:seed-pools:START -->
| Pool key | Environment variable |
|---|---|
| `seed.address` | `SEED_ADDRESS` |
| `seed.cancel.buy` | `SEED_CANCEL_BUY` |
| `seed.cancel.unlimited` | `SEED_CANCEL_UNLIMITED` |
| `seed.history.rent` | `SEED_HISTORY_RENT` |
| `seed.history.rentbuy` | `SEED_HISTORY_RENTBUY` |
<!-- AUTO:seed-pools:END -->

Pool semantics: `history.*` and `address` are read-only (round-robin); `cancel.*` are consumed per run through `SeedUsers.acquireCancelCandidate(pool)`.

- **Locally:** copy `src/test/resources/seed/seed-users.example.properties` to `seed-users.local.properties` in the same folder and fill in comma-separated 10-digit numbers.
- **CI:** provide each pool as an environment variable / secret with the same comma-separated format. The file wins per pool.
- **Never commit** real numbers. `*.local.properties` and `*.consumed` under `src/test/resources/seed/` are gitignored.
- If a pool is empty or exhausted, the dependent test is skipped (`SkipException`) with a clear reason. In CI the consumed file is ephemeral, so rotate the cancel secrets after each run.

## Configuration and environments

`ConfigManager` loads `src/test/resources/config/<env>.properties`; select with `-Denv=qa|staging|preprod|prod` (default `qa`).
Only non-sensitive values belong in these files; secrets come from env vars / CI secrets.

<!-- AUTO:config-files:START -->
Environment files (`src/test/resources/config/`): `preprod.properties`, `prod.properties`, `qa.properties`, `staging.properties`. Key names only; values are never listed here.

Keys present in every env file (23): `ai.enabled`, `ai.failure.analysis.enabled`, `ai.locator.runtime.allowed.environments`, `ai.locator.runtime.validation.enabled`, `ai.model`, `ai.provider`, `ai.timeout.seconds`, `api.base.url`, `api.token`, `browser`, `db.host`, `db.name`, `db.password`, `db.pool.size`, `db.port`, `db.user`, `default.timeout.ms`, `headless`, `retry.max.count`, `slowmo.ms`, `test.user.password`, `thread.count`, `web.base.url`


Keys set only in some env files:

| Key(s) | Defined in |
|---|---|
| `appium.app.*` (3 keys) | preprod, qa, staging |
| `appium.bundle.id` | preprod, qa, staging |
| `appium.command.timeout.seconds` | preprod, qa, staging |
| `appium.device.name` | preprod, qa, staging |
| `appium.no.reset` | preprod, qa, staging |
| `appium.platform`, `appium.platform.version` | preprod, qa, staging |
| `appium.server.url` | preprod, qa, staging |
| `business.furlenco.base.url` | preprod |
| `furlenco.base.url` | preprod, qa, staging |
| `furlenco.experiment.new.desktop.variant` | preprod |
| `furlenco.experiment.checkout.revamp.variant` | qa |
| `furlenco.pin.experiments` | preprod, qa |
| `login.otp.wait.ms` | preprod, qa, staging |
| `login.submit.wait.ms` | preprod, qa, staging |
| `test.delivery.pincode` | preprod, qa, staging |
| `test.new.user.registered.email` | preprod |
| `test.payment.*` (14 keys) | preprod |
| `test.user.otp` | preprod, qa, staging |
| `test.user.username` | prod |
<!-- AUTO:config-files:END -->

## Reports

- Allure results: `target/allure-results`; view with `allure serve target/allure-results`.
- On failure: screenshots (attached to Allure) and traces at `target/traces/<test>-trace.zip`; open with `npx playwright show-trace <zip>`.
- Login CSV: `target/test-data/login-attempts.csv`.

## CI

`.github/workflows/tests.yml` has two jobs:

- **`checks`** runs on every push to `main` and every pull request, on a GitHub-hosted runner: generated-docs freshness (`--check` for both generators), `mvn test-compile`, and a leak scan for phone-number-like values. It needs no access to staging.
- **`e2e`** (the browser tests) runs **only on manual dispatch** (inputs: `groups` default `smoke`, `runner` default `self-hosted`, `browser`, `run_e2e`) and needs a self-hosted runner on the staging VPN. `stag.furlenco.com` resolves to a private address, so on a GitHub-hosted runner it returns a Cloudflare "Error 1002: DNS points to local or disallowed IP" page and every test times out; the connectivity pre-check (`EnvironmentCheck`) also stops such a run immediately with a clear message. Seed accounts can be supplied as repository secrets (`SEED_HISTORY_RENTBUY`, `SEED_HISTORY_RENT`, `SEED_ADDRESS`, `SEED_CANCEL_BUY`, `SEED_CANCEL_UNLIMITED`).

## Traceability

`docs/test-case-traceability.md` maps test-case sheet ids to tests tagged `@TmsLink("TC-nnn")`. It is generated; do not edit the table by hand.

<!-- AUTO:traceability-summary:START -->
Totals from `docs/test-case-traceability.md`: implemented: 28; needs-clarification: 47; skeleton: 17.
<!-- AUTO:traceability-summary:END -->

```bash
java src/test/java/com/tools/TraceabilityGenerator.java                  # regenerate (from the repo root, JDK 25)
java src/test/java/com/tools/TraceabilityGenerator.java --check          # exit 1 if stale
java src/test/java/com/tools/TraceabilityGenerator.java --feed feed.csv  # use a CSV export of the sheet as source
```

An enabled test is `implemented`; `enabled = false` is a `skeleton`; duplicate/missing TC ids or non-descriptive names are `needs-clarification`.

## Docs auto-update

`README.md` and `docs/PROJECT_MASTER_PROMPT.md` contain `AUTO:<name>:START` / `AUTO:<name>:END` HTML comment blocks that
`src/test/java/com/tools/ProjectDocsGenerator.java` rewrites from the code (pom, framework packages, page objects, `@Test` scan, config key names, seed pool names, traceability totals). Hand-written text is never touched.

```bash
java src/test/java/com/tools/ProjectDocsGenerator.java            # rewrite the AUTO blocks
java src/test/java/com/tools/ProjectDocsGenerator.java --check    # exit 1 if stale (CI uses this)
```

How it stays current:

- **Maven (opt-in):** `mvn -Pdocs test-compile` runs the generators (traceability, then docs) at `process-test-classes` via exec-maven-plugin. Generator failures never fail the build; skip with `-Ddocs.skip=true`. It is opt-in so the default build stays offline-safe and does not rewrite tracked files.
- **Git hook (optional, one-time):** `git config core.hooksPath .githooks`. The pre-commit hook regenerates both docs when `src/**`, `pom.xml` or config is staged and re-stages `README.md` and `docs/*.md`. Disable by unsetting it (`git config --unset core.hooksPath`) or committing with `--no-verify`.
- **CI:** both `--check` commands run before the tests and fail on drift.

To add a new auto block: add a case in `ProjectDocsGenerator` and the marker pair to the doc.

## Troubleshooting

| Symptom | Cause / fix |
|---|---|
| Timeouts or DNS errors on `stag.furlenco.com` | Not on the staging VPN. |
| Login stalls after Continue, or signup prompt appears late | Staging OTP/signup lag. Raise `login.otp.wait.ms` / `login.submit.wait.ms`; do not add sleeps. |
| Strict-mode violation | A selector matched several elements (often both A/B variants). Narrow it (scope to a container, `.first()` only when intended). |
| "Element not found" that changes between runs | A/B experiment layout. Pin variants (`furlenco.pin.experiments`, `FurlencoExperiments`) and write selectors tolerant of variants. |
| Locator works, then fails after navigation | Locators are lazy: re-create the locator after navigating instead of reusing an old one. |
| Seed test skipped | Pool empty or consumed; supply numbers locally or via CI secret. |
| `ProjectDocsGenerator` reports stale docs | Run the generator and commit the result. |
