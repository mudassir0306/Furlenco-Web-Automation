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
| Public sites only (what CI runs by default) | `mvn test -Dgroups=public` |
| Web tests only | `mvn test -Dgroups=web` |
| API tests only | `mvn test -Dgroups=api` |
| Run on Firefox | `mvn test -Dbrowser=firefox` |
| Run headed | `mvn test -Dheadless=false` |
| Change parallel forks | `mvn test -Dforks=1` |
| Override environment | `mvn test -Denv=staging` |

Configuration is loaded by `com.framework.config.ConfigManager` from `src/test/resources/config/<env>.properties` (`-Denv=qa|staging|preprod|prod`, default `qa`). Keys resolve in this order:

1. `-Dkey=value`
2. OS env var (upper snake case, e.g. `WEB_BASE_URL`)
3. env-scoped key in the env file (e.g. `qa.web.base.url`)
4. plain key in the env file (e.g. `web.base.url`)

The sample public-site tests (`com.tests.web.LoginTest`, `com.tests.api.PostsApiTest`) read `sample.web.*` / `sample.api.base.url` from `config/qa.properties`, with built-in defaults if absent. The root `src/test/resources/config.properties` is no longer read by the framework.

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

- `src/main/java/com/framework` - `base/BasePage`, `config/ConfigManager`, `driver/PlaywrightManager`, `api/ApiClient`, `listeners/` (`TestListener`, retry), `utils/`
- `src/test/java/com/tests` - `base/` (BaseTest, BaseWebTest, BaseApiTest), `pages/` (page objects), `web/`, `api/` tests
- `src/test/resources/config/<env>.properties` - environment configuration
- `src/test/resources/META-INF/services/org.testng.ITestNGListener` - registers `TestListener` and `RetryTransformer`
- `.github/workflows/tests.yml` - CI workflow for push, PR, and manual runs

## Parallelism

Playwright Java is not safe to share a single browser instance across threads in one JVM. This project therefore uses Maven Surefire forks, with one JVM per fork, and test classes are distributed across those forks.

## CI

The GitHub Actions workflow in `.github/workflows/tests.yml` runs on push, pull requests, and manual dispatch, supports a test group and browser input, and uploads both Allure results and failure artifacts.

## Traceability

`docs/test-case-traceability.md` maps the test-case sheet (TC ids) to the tests tagged with `@TmsLink("TC-nnn")` in `src/test/java`. It is generated; do not edit the table by hand.

```bash
python scripts/generate_traceability.py                  # regenerate the doc from the code + existing doc
python scripts/generate_traceability.py --check          # exit 1 if the doc is stale (for CI)
python scripts/generate_traceability.py --feed Test_Cases_Feed.xlsx   # use the sheet ('Playwright-Java Feed'); needs openpyxl, or pass a CSV export
```

A test with `enabled = false` is a `skeleton`, an enabled one is `implemented`. A TC id shared by several scenarios, a missing TC id or a non-descriptive scenario name is `needs-clarification`. Without `--feed`, scenario names and notes are read from the existing doc, so hand-written notes are kept.
