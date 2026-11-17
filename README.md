# PetClinic QA: test automation framework for Spring PetClinic Microservices

An automated test framework for the [Spring PetClinic Microservices](https://github.com/spring-petclinic/spring-petclinic-microservices) sample application. The application lives in `app/` and is treated as the system under test: it is not changed here. Problems found in it are recorded as issues and covered by tests that stay red until the application is fixed.

**Latest E2E report:** https://d2cteh9u7mbf08.cloudfront.net/latest/index.html

## What is in the repository

| Path | What it is |
|---|---|
| `app/` | The system under test (Spring Boot and Spring Cloud, Java 17). Upstream code. |
| `test-framework/` | The test framework (Java 21, Maven, four modules). |
| `docker-compose.test.yml` | The stack the E2E tests run against. |
| `.github/workflows/` | CI: fast checks on every push, E2E nightly and on demand. |
| `terraform/` | AWS S3 and CloudFront that host the published Allure reports. |

## Test layers

| Layer | Module | What runs | What it checks |
|---|---|---|---|
| Unit | `core`, `ai-triage` | Plain JUnit | Date parsing, the AI triage client against a local fake endpoint. |
| Component | `component-tests` | One real service JAR and MySQL in Testcontainers | The HTTP contract of customers, vets and visits services, with the database checked through DAOs. |
| Component (gateway) | `component-tests` | The real api-gateway JAR against WireMock | Routing, fallback, retry, the owner aggregate, the circuit breaker opening and closing. |
| E2E API | `e2e-tests` | The whole stack in Docker Compose | Flows through the gateway, checked in the API and in MySQL. |
| E2E UI | `e2e-tests` | Playwright (Chromium) on the whole stack | The owner flow, form validation, the Veterinarians page, consistency between UI, API and database. |

Tools: JUnit 5, REST Assured, Playwright for Java, JDBI 3, Testcontainers, WireMock, Allure, GitHub Actions.

### Principles

- **A test never hides a defect.** No forced clicks, no scripts that change the page, no swallowed exceptions. If the application is wrong, the test is red.
- **Known defects are visible but do not hide new ones.** Tests for confirmed application defects carry the `known-defect` tag and the issue number. CI runs them in a separate informational step, so the main result is green unless something new breaks.
- **State is checked, not only status codes.** Tests read MySQL directly to confirm what was stored, and that a refused request stored nothing.
- **The UI is used as a user uses it.** Pages are reached by clicks; form validation is observed, not switched off.

## Running the tests

Requirements: JDK 21 for the framework (JDK 17 or newer for the application), Docker, and the Maven wrapper in each project.

```bash
# 1. Build the application JARs (component tests and the Docker stack use them)
cd app && ./mvnw -B clean package -DskipTests spring-boot:repackage && cd ..

# 2. Unit tests of the framework
cd test-framework
./mvnw -B test -pl core,ai-triage

# 3. Component tests (need Docker)
./mvnw -B install -DskipTests
./mvnw -B test -pl component-tests "-Dgroups=!known-defect"

# 4. E2E tests (need the stack)
cd .. && docker compose -f docker-compose.test.yml up -d && cd test-framework
./mvnw -B test -pl e2e-tests "-Dgroups=!known-defect"

# Allure report of the last run
./mvnw -B allure:serve
```

Tags: `smoke`, `api`, `ui`, `hybrid`, `negative`, `component`, `known-defect`. Run only the tests of known defects with `-Dgroups=known-defect`.

The first E2E run needs the Playwright browser:
`./mvnw -B exec:java -pl e2e-tests -Dexec.mainClass=com.microsoft.playwright.CLI -Dexec.args="install --with-deps chromium"`

More detail on commands, the stack and its startup quirks is in [`CLAUDE.md`](CLAUDE.md).

## CI

| Workflow | When | What it does |
|---|---|---|
| Fast Checks | Every push and pull request | Unit tests, component tests (without `known-defect`), Allure report as an artifact. |
| E2E Tests & Report | Nightly and on demand | Starts the stack and fails early if it is not ready; runs the E2E tests (must be green); runs the `known-defect` tests as an informational step; publishes the Allure report. |
| Terraform Validation | Changes in `terraform/` | `fmt`, `validate`, `tflint`. |

When an E2E run fails, the job uploads the logs of every container. A failed UI test has a screenshot, the Playwright trace and the browser errors attached in the Allure report.

## Defects found in the application

Each defect has a test that fails until it is fixed.

| Issue | Defect | Found by |
|---|---|---|
| [#1](https://github.com/igreukrnet/petclinic/issues/1) | The floating chat box covers the "Add Visit" link, so the click does not reach it. | E2E UI |
| [#2](https://github.com/igreukrnet/petclinic/issues/2) | Dates in the UI are one day early in time zones west of UTC. | Review of the frontend |
| [#3](https://github.com/igreukrnet/petclinic/issues/3) | Pet birth dates come as UTC date-times, visit dates as `yyyy-MM-dd`; the birth date depends on the server time zone. | Component |
| [#4](https://github.com/igreukrnet/petclinic/issues/4) | A visit for a missing pet returns 500 instead of 404. | E2E API, component |
| [#5](https://github.com/igreukrnet/petclinic/issues/5) | A pet with an unknown type returns 500 instead of 400. | E2E API, component |
| [#6](https://github.com/igreukrnet/petclinic/issues/6) | The gateway retries `POST` on 503, which can create a record twice. | Component (gateway) |
| [#7](https://github.com/igreukrnet/petclinic/issues/7) | The gateway passes a downstream 500 to the client instead of answering 503. | Component (gateway) |
| [#8](https://github.com/igreukrnet/petclinic/issues/8) | The gateway has no timeout for downstream calls. | Component (gateway) |
| [#9](https://github.com/igreukrnet/petclinic/issues/9) | One circuit breaker is shared by all routes: a failing service breaks the healthy ones. | Component (gateway) |
| [#10](https://github.com/igreukrnet/petclinic/issues/10) | Downstream 5xx answers do not count in the circuit breaker, so it never opens. | Component (gateway) |
| [#11](https://github.com/igreukrnet/petclinic/issues/11) | The owner aggregate returns 500 instead of 503 when customers-service is down. | Component (gateway) |
| [#12](https://github.com/igreukrnet/petclinic/issues/12) | The owner aggregate returns 500 instead of 404 for a missing owner. | Component (gateway) |
| [#13](https://github.com/igreukrnet/petclinic/issues/13) | The owner aggregate hides a visits-service outage behind an empty visits list. | Component (gateway); no test, the contract is undefined |
| [#14](https://github.com/igreukrnet/petclinic/issues/14) | Visit dates shift one day back when the server time zone is east of UTC. | Component |
| no issue yet | **Deep links do not work.** The gateway answers a direct request for an application page (for example `GET /owners/details/1`) with a 404 JSON instead of serving the single-page application. A page refresh or a bookmarked link outside the root shows an error. | Review while writing the page objects; the UI tests reach every page by clicks for this reason |

## Known limits

- The E2E stack uses pre-built images for the config, discovery, vets and visits services, so local changes in those modules reach only the component tests.
- Tests do not run in parallel yet: the E2E stack uses fixed ports and one shared database.
- Open product questions block some tests: empty visit descriptions, duplicate owner telephones, the date format of the API. They are listed in [`test-framework/BACKLOG.md`](test-framework/BACKLOG.md).
