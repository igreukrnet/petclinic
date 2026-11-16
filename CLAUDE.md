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

Run every Maven command from the repo root with `-f`, or `cd` into the matching directory first. CI uses plain `mvn`. On developer machines `mvn` is not always on PATH. Then use `app/mvnw` from inside `app/`, because its `.mvn/` wrapper lives there and it fails from the repo root:

```bash
cd app && ./mvnw -B -f ../test-framework/pom.xml test -pl e2e-tests
```

`test-framework` needs JDK 21. Set `JAVA_HOME` explicitly, because the default JDK on some machines is 17.

```bash
# 1. Build and install the framework modules (core must be installed before the other modules can use it)
mvn -B install -DskipTests -f test-framework/pom.xml

# 2. Build the app JARs (component tests and docker-compose.test.yml depend on them)
cd app && mvn -B clean package -DskipTests spring-boot:repackage

# Component tests (Testcontainers MySQL + WireMock; need a running Docker)
mvn -B test -pl component-tests -f test-framework/pom.xml

# E2E tests (Playwright + REST Assured + JDBI; need the whole stack running, see below)
mvn -B test -pl e2e-tests -f test-framework/pom.xml
mvn -B test -pl e2e-tests -f test-framework/pom.xml -Dheadless=false   # visible browser

# One test class / one method
mvn -B test -pl component-tests -f test-framework/pom.xml -Dtest=CustomersServiceComponentTest
mvn -B test -pl e2e-tests -f test-framework/pom.xml -Dtest=PetClinicApiE2ETest#someMethod

# Playwright browser install (once, before the first E2E run)
mvn -B exec:java -pl e2e-tests -Dexec.mainClass=com.microsoft.playwright.CLI -Dexec.args="install --with-deps chromium" -f test-framework/pom.xml

# Allure report (results are written to <repo>/target/allure-results)
mvn -B allure:report -f test-framework/pom.xml

# App unit tests
cd app && ./mvnw test

# Terraform (CI also runs fmt, validate and tflint)
cd terraform && terraform fmt -check && terraform validate
```

E2E stack: `docker compose -f docker-compose.test.yml up -d --wait`. After that, wait until the gateway answers 200 on `/api/customer/owners`, `/api/vet/vets`, `/api/visit/owners/1/pets/1/visits` and `/api/gateway/owners/1`. The last URL depends on visits-service being registered in Eureka. Right after startup the gateway logs `No servers available for service: visits-service` and the aggregate returns owners without visits, so E2E runs started too early fail on the visit assertions. The healthchecks do not cover Eureka registration, so the CI workflows poll these URLs.

Known startup and state issues:

- `visits-service` (a prebuilt image) can crash on first start with `Failed to open the referenced table 'pets'`. Its `schema.sql` references the `pets` table that customers-service creates, and compose only waits for `service_started`. Fix: `docker compose -f docker-compose.test.yml up -d visits-service` after customers-service is healthy.
- MySQL data lives in an anonymous volume and persists between runs. Tests delete their own rows only at the end of the test body, so a failed test leaves its rows behind. For a clean baseline run `docker compose -f docker-compose.test.yml down -v` first.

## Test-framework architecture

Three modules. `core` is the shared library; `component-tests` and `e2e-tests` depend on it.

- **`core`** (`org.springframework.samples.petclinic.test.core`)
  - `api`: `ApiClient` and `PetClinicClient`, REST Assured clients that call the gateway.
  - `db`: `ConnectionFactory` creates a JDBI instance and the DAOs, with row mappers per entity. Tests read and write MySQL directly to check state and to seed data.
  - `model.dto` and `model.request`: the DTOs and request bodies the tests use.
  - `data.TestDataGenerator`: generates test data with Datafaker.
  - `config`: `ConfigurationManager` and `TestConfig` (Owner library). Defaults live in `core/src/main/resources/config.properties` (gateway `localhost:8080`, MySQL `localhost:3306`, user `root`, password `petclinic`). Override them with system properties or environment variables.
  - `ai`: `AiFailureTriageExtension` is a JUnit `TestWatcher`. When a test fails and `ai.triage.enabled=true`, it sends the failure to an OpenAI-compatible endpoint (`ai.base.url`, `ai.model`) through `AiTriageClient` and attaches the analysis to the Allure report. It is off by default.
- **`component-tests`** — isolated tests. `BaseComponentTest` starts a single static Testcontainers `mysql:8.4.5` container with reuse enabled and loads `/db/init.sql` from the test resources. Downstream services are mocked with WireMock. There are no gateway resilience tests yet: the former `GatewayCircuitBreakerTest` only exercised WireMock itself and was removed. A real circuit-breaker test needs the gateway running against WireMock-backed downstreams.
- **`e2e-tests`** — runs against the live docker-compose stack. `BaseE2ETest` sets up Playwright Chromium (`-Dheadless`, default `true`), a `PetClinicClient` and the DAOs, and attaches a full-page screenshot to Allure after every test. The Page Object Model is in `src/main/java/.../e2e/pages`. Tests are split into `api/`, `ui/` and `hybrid/`. Hybrid tests check consistency between the UI, the API and the DB.

Both base classes use `@TestInstance(PER_CLASS)` and register `AiFailureTriageExtension`. The Surefire config in `test-framework/pom.xml` attaches the AspectJ weaver as a `-javaagent` (needed for Allure `@Step` aspects). It also sets `DOCKER_API_VERSION=1.44` for Testcontainers. New test modules should inherit this config from the parent pom.

## Page Object rules

- Do not make the UI look valid or reachable with JavaScript. Do not set `form.noValidate`, override `reportValidity`, remove `required` or `pattern`, force clicks with `setForce(true)`, hide overlays with `display:none`, or write `input.value` plus synthetic events. If a click or fill fails, that is an application defect. Report it and let the test fail.
- Do not swallow exceptions in waits (`catch (Exception ignored)`). A failed wait must fail the test with its original message. The only accepted `catch` is one that rethrows with more context.
- Use Playwright waits (`waitFor`, or `PlaywrightAssertions`), not `Thread.sleep`. The remaining `Thread.sleep` calls in `OwnersPage` and `OwnerDetailsPage` are known debt.

## Known failing tests

- `OwnerLifecycleUiE2ETest` fails on the Add Visit click. The floating `.chatbox` intercepts pointer events on the owner details page. Tracked in https://github.com/igreukrnet/petclinic/issues/1. Keep it failing until the application is fixed. Do not work around it in the test.

## docker-compose.test.yml specifics

- Only `customers-service` and `api-gateway` are built from source (`context: ./app`, `docker/Dockerfile`, with `ARTIFACT_NAME=.../target/<module>-4.0.1`). The `app` JARs must be built first. If you bump the app version in `app/pom.xml`, update the `ARTIFACT_NAME` values.
- The other services (config, discovery, vets, visits) use pre-built `springcommunity/*` images. They are not built from the local source, so changes in those modules do not reach the E2E stack.
- Services use the `docker,mysql` Spring profiles and MySQL at `mysql:3306`, with a 512 MB memory limit each.

## CI (`.github/workflows`)

- `fast-checks.yml` — on push and PR: installs `test-framework`, builds the app JARs, runs component tests.
- `e2e-tests.yml` — manual (`workflow_dispatch`) or nightly at 02:00 UTC: starts docker-compose, runs the E2E tests, generates the Allure report and publishes it to S3 and CloudFront. The run history is restored from `s3://…/latest/history`, and every run is archived under `builds/<run_number>/`.
- `ci.yml` — legacy combined workflow, kept only for backwards compatibility (manual dispatch).
- `terraform.yml` (fmt, validate, tflint on `terraform/` changes) and `terraform-deploy.yml` (manual plan or apply).
- S3/CloudFront publishing is skipped unless the secrets `AWS_ACCESS_KEY_ID` and `AWS_SECRET_ACCESS_KEY` and the variables `S3_BUCKET_NAME`, `CLOUDFRONT_DISTRIBUTION_ID` and `AWS_REGION` (default `eu-north-1`) are set. `terraform output` produces these values; see `terraform/README.md`.

## App architecture (`app/`)

Service-discovery pattern. The **config-server** (8888) and **discovery-server** (Eureka, 8761) must start first. Then customers, vets, visits and genai-service register in Eureka, usually on random ports. The **api-gateway** (8080) serves the Vue.js frontend and routes `/api/customer/**`, `/api/vet/**` and `/api/visit/**`. It uses Resilience4j circuit breakers, and `FallbackController` provides the fallbacks. `spring-petclinic-admin-server` (Spring Boot Admin, 9090) is optional, as are Zipkin, Prometheus and Grafana.

The default DB is in-memory HSQLDB. Profile `mysql` switches to MySQL. Docker images are built with `./mvnw clean install -P buildDocker` (add `-Dcontainer.platform=linux/arm64` on Apple Silicon). `app/scripts/run_all.sh` starts everything locally (docker compose plus `java -jar`), and `stop_all.sh` stops it. `scripts/chaos/` holds Chaos Monkey helpers. The GenAI service needs `OPENAI_API_KEY` (or the Azure OpenAI variables). More details are in `app/README.md`.

## Notes

- `target/` (repo root) holds CI/test artifacts such as Allure results, logs and failure screenshots. It is git-ignored.
- `.kiro/steering/no-shell-browse.md` is a rule for another agent (Kiro). It forbids shell commands for navigating and reading files.
- `terraform.tfstate` is local state (git-ignored). The remote S3 backend in `terraform/main.tf` is commented out.
