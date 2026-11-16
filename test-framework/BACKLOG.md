# Test framework backlog

Work that was planned but deferred, with the reason it is not done yet.
Update this file when an item is started, finished or dropped.

## Deferred from P3 (coverage)

- **Component tests against real services (plan item 23).** Replace the DAO-only component tests with tests that
  run a real service in a container next to MySQL (Testcontainers, image built from the service JAR), call it over
  HTTP, and check the database through the `core` DAOs. Start with customers-service, which runs standalone
  (recipe and feasibility check in `HANDOFF.md`, section 10). Add a real gateway circuit-breaker test later, with
  the gateway running against WireMock downstreams. Approach approved; the base class and owner tests of customers-service are done. Next: pets, pet types, `known-defect` tests, then the gateway. Detailed plan: `HANDOFF.md`, section 10.
- **UI validation of forms and the Veterinarians page (plan item 24).**
  No UI test checks what happens when a form is submitted with invalid data. `VetsPage` exists but no test uses it.
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
- #4 Visit to a missing pet returns 500 instead of 404. Test: `KnownDefectsE2ETest`.
- #5 Pet with an unknown type returns 500 and is not validated. Test: `KnownDefectsE2ETest`.

## Known weaknesses in the framework (not planned yet)

- `PetRowMapper` swallows `SQLException` when reading the type, which can hide a broken query.
- `deleteOwnerCascade` is not transactional: a failure in the middle can leave a partly deleted owner.
- `ai-triage` is tested only with the feature off; the live path is never exercised in CI.
- `VetsPage` is unused.
- `OwnerDetailsPage.hasPet` and `hasVisitDescription` return `false` on any timeout, which hides the reason.
