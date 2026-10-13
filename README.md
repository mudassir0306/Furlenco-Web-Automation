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

Environment values are resolved by `Config.get(...)` in this order:

1. `-Dkey=value`
2. `env`-scoped key from `src/test/resources/config.properties` (for example `qa.web.baseUrl`)
3. plain key from `src/test/resources/config.properties`

Example:

```properties
env=qa
qa.web.baseUrl=https://www.saucedemo.com
qa.api.baseUrl=https://jsonplaceholder.typicode.com
qa.web.username=standard_user
qa.web.password=secret_sauce
```

To test another environment, set `-Denv=staging` and add matching `staging.*` keys to `src/test/resources/config.properties`.

## Reports

Allure results are written to `target/allure-results`.

```bash
allure serve target/allure-results
```

When a web test fails, the framework also saves failure artifacts in `artifacts/`:

- screenshots: `artifacts/screenshots/<test>.png`
- traces: `artifacts/traces/<test>.zip`

Open a trace locally with:

```bash
npx playwright show-trace artifacts/traces/<test>.zip
```

## Project layout

- `src/main/java/com/playwright/framework/config` - configuration loading (`Config`)
- `src/main/java/com/playwright/framework/core` - browser lifecycle and tracing (`PlaywrightManager`)
- `src/main/java/com/playwright/framework/pages` - page object models
- `src/main/java/com/playwright/framework/api` - API helper (`ApiClient`)
- `src/test/java/com/playwright/tests` - TestNG tests and listeners
- `src/test/resources/config.properties` - default configuration values
- `.github/workflows/tests.yml` - CI workflow for push, PR, and manual runs

## Parallelism

Playwright Java is not safe to share a single browser instance across threads in one JVM. This project therefore uses Maven Surefire forks, with one JVM per fork, and test classes are distributed across those forks.

## CI

The GitHub Actions workflow in `.github/workflows/tests.yml` runs on push, pull requests, and manual dispatch, supports a test group and browser input, and uploads both Allure results and failure artifacts.
