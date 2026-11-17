# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Repository layout

The repo root is **not** a Maven project. It holds three independent parts:

- `app/` — the system under test: Spring PetClinic Microservices (Spring Boot / Spring Cloud, Java 17, version 4.0.1). Multi-module Maven build with its own `mvnw`. Mostly upstream code; the services have only a few unit tests.
- `test-framework/` — the main work in this repo: an automated QA framework for the app (Java 21, separate Maven multi-module build, groupId `org.springframework.samples.petclinic.test`).
- `terraform/` — AWS infrastructure (S3 + CloudFront + IAM user) that hosts the published Allure reports.

Also at the root: `docker-compose.test.yml` (stack for E2E tests) and `.github/workflows/`.

The Java versions differ: `app` builds with 17, `test-framework` with 21 (CI uses JDK 21 for both).

## Commands

Run every Maven command from the repo root with `-f`, or `cd` into the matching directory first. CI uses plain `mvn`.

`test-framework` has its own Maven wrapper (Maven 3.9.8): `test-framework/mvnw` on Unix shells, `test-framework/mvnw.cmd` on Windows. Use it when `mvn` is not on PATH:

```bash
cd test-framework && ./mvnw -B test -pl e2e-tests
```

`test-framework` needs JDK 21. The build stops at `validate` with a clear message if the JDK is not 21. Set `JAVA_HOME` explicitly, because the default JDK on some machines is 17.

Run a subset with JUnit tags. Quote the property in PowerShell:

```bash
./mvnw -B test -pl e2e-tests -Dgroups=smoke        # the two main API and hybrid flows
./mvnw -B test -pl e2e-tests -Dgroups=negative     # validation and error responses
./mvnw -B test -pl e2e-tests "-Dgroups=!known-defect"   # everything except known application defects
```

Tags: `smoke`, `api`, `hybrid`, `ui`, `negative`, `component`, `known-defect`.

```bash
# 1. Build and install the framework modules (core must be installed before the other modules can use it)
mvn -B install -DskipTests -f test-framework/pom.xml

# 2. Build the app JARs (component tests and docker-compose.test.yml depend on them)
cd app && mvn -B clean package -DskipTests spring-boot:repackage

# Component tests (real service JAR + MySQL in Testcontainers; need a running Docker and the built app JARs)
mvn -B test -pl component-tests -f test-framework/pom.xml

# E2E tests (Playwright + REST Assured + JDBI; need the whole stack running, see below)
mvn -B test -pl e2e-tests -f test-framework/pom.xml
mvn -B test -pl e2e-tests -f test-framework/pom.xml -Dheadless=false   # visible browser
mvn -B test -pl e2e-tests -f test-framework/pom.xml "-Dgroups=!known-defect"   # skip tests for confirmed app defects

# One test class / one method
mvn -B test -pl component-tests -f test-framework/pom.xml -Dtest=CustomersServiceComponentTest
mvn -B test -pl e2e-tests -f test-framework/pom.xml -Dtest=PetClinicApiE2ETest#someMethod

# Playwright browser install (once, before the first E2E run)
mvn -B exec:java -pl e2e-tests -Dexec.mainClass=com.microsoft.playwright.CLI -Dexec.args="install --with-deps chromium" -f test-framework/pom.xml

# Allure report (results are written to test-framework/target/allure-results)
mvn -B allure:report -f test-framework/pom.xml

# App unit tests
cd app && ./mvnw test

# Terraform (CI also runs fmt, validate and tflint)
cd terraform && terraform fmt -check && terraform validate
```

E2E stack: `docker compose -f docker-compose.test.yml up -d --wait`. After that, wait until the gateway answers 200 on `/api/customer/owners`, `/api/vet/vets`, `/api/visit/owners/1/pets/1/visits` and `/api/gateway/owners/1`. The last URL depends on visits-service being registered in Eureka. Right after startup the gateway logs `No servers available for service: visits-service` and the aggregate returns owners without visits, so E2E runs started too early fail on the visit assertions. The healthchecks do not cover Eureka registration, so the CI workflows poll these URLs.

Known startup and state issues:

- `visits-service` (a prebuilt image) can crash on first start with `Failed to open the referenced table 'pets'`. Its `schema.sql` references the `pets` table that customers-service creates, and compose only waits for `service_started`. Fix: `docker compose -f docker-compose.test.yml up -d visits-service` after customers-service is healthy.
- MySQL data lives in an anonymous volume and persists between runs. Tests register the owners they create with `trackOwner(...)`, and an `@AfterEach` deletes them even after a failure. A run killed by a signal can still leave rows behind. For a clean baseline run `docker compose -f docker-compose.test.yml down -v` first.

## Test-framework architecture

Four modules. `core` is the shared library. `ai-triage` is optional and registered by both test modules. `component-tests` and `e2e-tests` depend on `core`.

- **`core`** (`org.springframework.samples.petclinic.test.core`)
  - `api`: `ApiClient` and `PetClinicClient`, REST Assured clients that call the gateway. `ApiClient` sets connect and read timeouts (`http.connect.timeout.ms`, `http.read.timeout.ms`) and logs to the console only when a validation fails; the full exchange is in Allure.
  - `db`: `Database` holds a HikariCP pool, a JDBI instance and the DAOs, with row mappers per entity; `Database.deleteOwnerCascade` removes an owner with pets and visits in one transaction. Tests read and write MySQL directly to check state and to seed data.
  - `model.dto` and `model.request`: the DTOs and request bodies the tests use.
  - `data.TestDataGenerator`: generates test data with Datafaker.
  - `config`: `ConfigurationManager` and `TestConfig` (Owner library). Defaults live in `core/src/main/resources/config.properties` (gateway `localhost:8080`, MySQL `localhost:3306`, user `root`, password `petclinic`). Override them with system properties or environment variables.
- **`ai-triage`** (`org.springframework.samples.petclinic.test.ai`) — optional. `AiFailureTriageExtension` is a JUnit `TestWatcher`. When a test fails and `ai.triage.enabled=true`, it sends the failure to an OpenAI-compatible endpoint through `AiTriageClient` and attaches the analysis to the Allure report. It is off by default and has no default endpoint, key or model: pass `-Dai.base.url`, `-Dai.api.key` and `-Dai.model` (never commit a key). At most `ai.triage.max.calls` failures (default 5) are analysed per run. The live path is tested in `AiTriageClientLocalEndpointTest` against a local fake endpoint, which Fast Checks runs.
- **`component-tests`** — tests of one real service at a time. `BaseComponentTest` starts MySQL `mysql:8.4.5` and the service (an image built from the service's fat JAR in `app/`, `TZ=UTC`, no config server, no Eureka) in one Testcontainers network, without reuse. Tests call the service over HTTP directly and check the database through the `core` DAOs. customers-service (`CustomersServiceComponentTest`, `CustomersTimeZoneComponentTest` with `TZ=Europe/Kyiv`), vets-service (`VetsServiceComponentTest`) and visits-service (`VisitsServiceComponentTest`, `VisitsTimeZoneComponentTest`) are covered; visits tests load the customers-service schema and seed from the application sources first (`BaseVisitsTest`); `known-defect` tests there fail on purpose (issues #3, #4, #5, #14) and Fast Checks excludes them with `-Dgroups=!known-defect`; the JAR path is overridable with `-Dservice.jar=...`. The real api-gateway is tested in `GatewayComponentTest` (routing, fallback, retry, owner aggregate) and `GatewayCircuitBreakerComponentTest` (circuit opens and closes; each test starts its own gateway because circuit state lives in the gateway). The gateway runs from its JAR without Eureka and config server (`SimpleDiscoveryClient` list, set in `BaseGatewayTest`) against WireMock servers in the test JVM, on dynamic ports. `BaseServiceTest` holds what both bases share. Gateway `known-defect` tests fail on purpose (issues #6 to #12; #13 has no test, its response contract is not defined).
- **`e2e-tests`** — runs against the live docker-compose stack. `BaseE2ETest` holds the `PetClinicClient`, the DAOs and the cleanup of created owners, and starts no browser. UI and hybrid tests extend `BaseUiE2ETest`, which adds Playwright Chromium (`-Dheadless`, default `true`), one context and page per test, and failure diagnostics: screenshot, Playwright trace and browser errors are attached to Allure only when a test fails. Nothing is static, so test classes share no state. The Page Object Model is in `src/main/java/.../e2e/pages`. Tests are split into `api/`, `ui/` and `hybrid/`. Hybrid tests check consistency between the UI, the API and the DB. UI coverage: `OwnerLifecycleUiE2ETest` (full flow), `FormValidationUiE2ETest` (what the owner, pet and visit forms do with invalid input), `VetsPageUiE2ETest` (the Veterinarians page against the DB and the API).

All base classes use `@TestInstance(PER_CLASS)` and register `AiFailureTriageExtension`. The Surefire config in `test-framework/pom.xml` attaches the AspectJ weaver as a `-javaagent` (needed for Allure `@Step` aspects). It sets the single Allure results directory (`test-framework/target/allure-results`) and `DOCKER_API_VERSION=1.44` for Testcontainers. New test modules should inherit this config from the parent pom.

## Page Object rules

- Do not make the UI look valid or reachable with JavaScript. Do not set `form.noValidate`, override `reportValidity`, remove `required` or `pattern`, force clicks with `setForce(true)`, hide overlays with `display:none`, or write `input.value` plus synthetic events. If a click or fill fails, that is an application defect. Report it and let the test fail.
- Do not swallow exceptions in waits (`catch (Exception ignored)`). A failed wait must fail the test with its original message. The only accepted `catch` is one that rethrows with more context.
- Use Playwright waits (`waitFor`, or `PlaywrightAssertions`), not `Thread.sleep`. There is no `Thread.sleep` in the framework; keep it that way.
- A check method that returns a boolean must not wait for something that may never come and then answer `false` on any timeout. Wait for a sure signal first (for example the page heading), which fails loudly, then read the page (`OwnerDetailsPage.hasPet`).
- Form pages extend `FormPage`. It only observes validation: `invalidReason`, `validationMessage`, `writeRequestsSent` (recorded with `page.route`, requests pass on unchanged), `errorAlertText`. `submitExpectingRefusal` clicks submit and waits for a sure sign of refusal (the form is `:invalid`, or the application shows its error box); do not use `networkidle`. Typing is `fillField` (like `fill`, which goes through the browser editing and respects `maxlength`) or `typeInto` (key by key).
- Reach pages by clicks, as a user does: `BasePage.clickNavigation` and links on the pages. Do not call the Vue router from a script and do not load deep links: the gateway answers them with 404, which is an application defect (see the defect table in `README.md`).
- Page objects do not change test data (no stripping or reformatting of values before typing).

## Known failing tests

Every test of a confirmed application defect has the `known-defect` tag and `Issue #N` in its display name. A run with `-Dgroups=!known-defect` must be fully green; a red test there is a new problem.

- `OwnerLifecycleUiE2ETest` and the two visit tests of `FormValidationUiE2ETest` fail on the Add Visit click. The floating `.chatbox` intercepts pointer events on the owner details page (https://github.com/igreukrnet/petclinic/issues/1), and that link is the only way to the visit form. Do not work around it in the tests.
- `KnownDefectsE2ETest` fails on purpose. A visit to a missing pet gives 500 instead of 404 (issue #4), and a pet with an unknown type gives 500 instead of 400 (issue #5). Do not change the expected status to match the current behaviour.
- Component tests: issues #3, #4, #5, #14 (services) and #6 to #12 (gateway).

## docker-compose.test.yml specifics

- Only `customers-service` and `api-gateway` are built from source (`context: ./app`, `docker/Dockerfile`, with `ARTIFACT_NAME=.../target/<module>-4.0.1`). The `app` JARs must be built first. If you bump the app version in `app/pom.xml`, update the `ARTIFACT_NAME` values.
- The other services (config, discovery, vets, visits) use pre-built `springcommunity/*` images. They are not built from the local source, so changes in those modules do not reach the E2E stack.
- Services use the `docker,mysql` Spring profiles and MySQL at `mysql:3306`, with a 512 MB memory limit each.
- `docker-compose.local-full.yml` adds the non-test services (admin, Zipkin, Prometheus, Grafana) to the test stack. Use both files for a full local run: `docker compose -f docker-compose.test.yml -f docker-compose.local-full.yml up -d --build`. GenAI is not included: it needs `OPENAI_API_KEY` (see `test-framework/BACKLOG.md`).

## CI (`.github/workflows`)

- `fast-checks.yml` — on push and PR: installs `test-framework`, builds the app JARs, runs component tests (without `known-defect`) and the unit tests of `core` and `ai-triage`, and uploads the Allure results and report as artifacts.
- `e2e-tests.yml` — manual (`workflow_dispatch`) or nightly at 02:00 UTC. Steps: start docker-compose and fail the job if the stack is not ready (the three routes answer 200 and the aggregate of owner 6 contains visits); run the E2E tests without `known-defect` (must be green); run the `known-defect` tests as an informational step (`continue-on-error`, result written to the job summary); on failure upload the logs of every container; generate the Allure report and publish it (scheduled runs always; manual runs when `publish_report` is true). The report goes to S3 and is served through CloudFront. The run history is restored from `s3://…/latest/history`, and every run is archived under `builds/<run_number>/`. The job summary links to the report of the run and to `latest`.
- A red E2E run means a new problem. Known defects never make it red; do not move a new failure into `known-defect` without an issue.
- `terraform.yml` (fmt, validate, tflint on `terraform/` changes) and `terraform-deploy.yml` (manual plan or apply).
- S3/CloudFront publishing is skipped unless the secrets `AWS_ACCESS_KEY_ID` and `AWS_SECRET_ACCESS_KEY` and the variables `S3_BUCKET_NAME`, `CLOUDFRONT_DISTRIBUTION_ID`, `CLOUDFRONT_DOMAIN` and `AWS_REGION` (default `eu-north-1`) are set. `CLOUDFRONT_DOMAIN` is the distribution's domain (for example `d2cteh9u7mbf08.cloudfront.net`); the summary links are built from it, because the S3 bucket is private and its URLs return AccessDenied. `terraform output` produces the other values; see `terraform/README.md`.

## App architecture (`app/`)

Service-discovery pattern. The **config-server** (8888) and **discovery-server** (Eureka, 8761) must start first. Then customers, vets, visits and genai-service register in Eureka, usually on random ports. The **api-gateway** (8080) serves the Vue.js frontend and routes `/api/customer/**`, `/api/vet/**` and `/api/visit/**`. It uses Resilience4j circuit breakers, and `FallbackController` provides the fallbacks. `spring-petclinic-admin-server` (Spring Boot Admin, 9090) is optional, as are Zipkin, Prometheus and Grafana.

The default DB is in-memory HSQLDB. Profile `mysql` switches to MySQL. Docker images are built with `./mvnw clean install -P buildDocker` (add `-Dcontainer.platform=linux/arm64` on Apple Silicon). `app/scripts/run_all.sh` starts everything locally (docker compose plus `java -jar`), and `stop_all.sh` stops it. `scripts/chaos/` holds Chaos Monkey helpers. The GenAI service needs `OPENAI_API_KEY` (or the Azure OpenAI variables). More details are in `app/README.md`.

## Notes

- `test-framework/target/` holds the Allure results and report. It is git-ignored.
- `.idea/`, `.kiro/`, `AUDIT_REPORT.md`, `test-framework/HANDOFF.md` and `test-framework/NEXT_SESSION_PROMPT.md` are local working files. They are git-ignored; do not add them back.
- `terraform.tfstate` is local state (git-ignored). The remote S3 backend in `terraform/main.tf` is commented out.
