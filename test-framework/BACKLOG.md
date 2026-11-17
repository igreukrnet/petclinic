# Test framework backlog

Work that was planned but deferred, with the reason it is not done yet.
Update this file when an item is started, finished or dropped.

## Deferred from P3 (coverage)

- **Component tests: nothing planned.** Done for customers, vets, visits and the gateway. Open work there depends on the decisions below.
- **UI: more form cases.** Done: required fields, telephone pattern and `maxlength`, blank names, future birth date,
  the Veterinarians page. Not done, because the expected behaviour is undecided: a visit description of only spaces
  (see the decision on empty descriptions below) and a description over 8192 characters typed by a user.
- **Decision needed: empty visit description.** The API accepts `description: ""` and returns 201.
  Is this a business rule (allowed) or a missing validation? Until decided, no test asserts either way.
- **Decision needed: duplicate owner telephone.** The API accepts two owners with the same telephone (201).
  Until decided, no test asserts either way.
- **Decision needed: owner search.** The UI search box is labelled "Last name" but matches all owner fields.
  The API has no search at all. Decide whether a search endpoint is wanted before writing tests for it.

## Parked for later (CI and local run)

- **Switch Allure publishing from CloudFront to another host (GitHub Pages), if wanted.** Decided: every run
  publishes to CloudFront (scheduled and manual), with a link to the run's report in the job summary. Later we
  may switch to GitHub Pages. Both publish publicly, because the repository is public.
- **GenAI service.** Not run locally or in CI: it needs `OPENAI_API_KEY` or the Azure settings, and no test
  uses it yet. Add it when there is a test for it and a key to use.

## Blocked on a decision

- **Date contract (issue #3).** Tests read dates through `FlexibleLocalDateDeserializer`, which accepts both
  `yyyy-MM-dd` and ISO date-times. Revisit once the API format is decided. Then remove the dual format.

## Open application defects (tracked as issues)

- #1 Floating chatbox intercepts the "Add Visit" click. Test: `OwnerLifecycleUiE2ETest` (fails).
- #2 Owner and visit dates show one day early in timezones west of UTC (`utils/dates.js`).
- #3 API returns pet birth dates as UTC date-times and visit dates as `yyyy-MM-dd`.
- #6 to #13 Gateway: retry on POST, 500 passed through, no timeout, one shared breaker, 5xx not counted, aggregate returns 500 instead of 503 and 404, visits outage hidden. Tests: `GatewayComponentTest`, `GatewayCircuitBreakerComponentTest` (all `known-defect` except #13, which has no test).
- #14 Visit dates shift one day back when the server time zone is east of UTC. Test: `VisitsTimeZoneComponentTest` (`known-defect`).
- #4 Visit to a missing pet returns 500 instead of 404. Test: `KnownDefectsE2ETest`.
- #5 Pet with an unknown type returns 500 and is not validated. Test: `KnownDefectsE2ETest`.

## Known weaknesses in the framework (not planned yet)

- `-Dservice.jar=...` applies to every service test class of a run, so it only makes sense with `-Dtest=` for one
  service. The gateway tests use their own `-Dgateway.jar=...`.
- The component test of customers-service cleans up with separate deletes: its database has no `visits` table,
  so `Database.deleteOwnerCascade` cannot be used there.
- Every component test class starts its own MySQL and service. The service image is built once per run and
  reused; MySQL is not shared (see "From the audit, not done yet").

## Done

- Component tests of customers, vets, visits and the gateway; UI form validation and the Veterinarians page.
- Framework weaknesses fixed: `PetRowMapper` no longer swallows `SQLException`; `Database.deleteOwnerCascade` is one
  transaction; the live path of `ai-triage` is tested against a local fake endpoint (`AiTriageClientLocalEndpointTest`,
  run by Fast Checks); `VetsPage` is used; `OwnerDetailsPage.hasPet` and `hasVisitDescription` wait for the page
  heading first and then read the page, so they fail loudly if the page never loads.
- Audit fixes: E2E CI has a failing readiness check, container logs on failure and a separate informational step
  for `known-defect` tests; `ci.yml` is removed; `BaseUiE2ETest` holds the browser (no static state); pages are
  reached by clicks; `FormPage` records requests with `page.route`; HTTP timeouts in `ApiClient`; unique
  telephones in `TestDataGenerator`; AI triage has no committed key or model and a call limit per run.
- Component test resources (audit step 6): one named service image per JAR, built once per run
  (`BaseServiceTest.imageFor`, `petclinic-component-test/<jar>:local`, kept for the Docker layer cache);
  the circuit breaker tests stop their gateway after each test, so one gateway is alive at a time instead of four;
  Awaitility replaces the hand-written polling loop and the elapsed-time arithmetic. The static network of
  `BaseComponentTest` was checked and kept: it is one network per run, removed by Ryuk.

## From the audit, not done yet

- Component tests still start one MySQL per test class. One MySQL per run needs the data to be reset between
  classes (the services load their seed only at start), so it was left out of the resource clean-up.
- Infrastructure: OIDC instead of IAM access keys, a lifecycle rule for `builds/`, actions pinned by commit,
  image tags in `docker-compose.test.yml`.
- Coverage: contract tests between the gateway and the services, outages on the live stack, field limits, XSS.
- Deep links are answered with 404 by the gateway (see `README.md`): no issue is filed yet.
- The two visit tests of `FormValidationUiE2ETest` are blocked by issue #1 and run only in the informational step.
