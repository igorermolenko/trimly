---
status: Accepted
owner: "igorermolenko"
reviewers: ["igorermolenko (implementing engineer)", "Tech Lead"]
updated_at: "2026-11-04"
feature_size: "M"
---

# Test plan — url-shortener

The owner, recognised by one owner credential, creates, lists, disables and re-enables short links through an owner API. Anyone can follow a short link without a credential and gets either a non-reusable redirect, "link not found" or "temporarily unavailable". Every one of the 22 acceptance criteria in spec.md §5 maps to at least one test below.

> **Inputs:** [spec.md](spec.md) §5/§6/§6.1, [sad.md](sad.md) §6/§10 (`target_surfaces: [backend-service]`, so the UI tiers are not used), [data-model.md](data-model.md), [contracts/openapi.yaml](contracts/openapi.yaml), [tasks.json](tasks.json).

## Levels

| Level | Scope | Strategy (generic — no tool names) |
|---|---|---|
| Unit | Pure logic in `link.domain` / `access` / `shared`: short-code alphabet and well-formedness, the generator, target-URL rules, the credential policy, the cursor codec, the monotonic ID generator. | In memory, no Spring context, no I/O. Randomness is seeded and time comes from a fixed clock. |
| Integration | The service against the real link storage it owns: owner API endpoints, the redirect endpoint, the store and its constraint translation, the 2 s storage budget. | The full application context against an ephemeral real PostgreSQL in a throwaway container, imported through the repo's shared test configuration. Real HTTP calls to the running app. |
| Contract | The owner API and redirect boundary against `contracts/openapi.yaml`: status, media type, body field set and Problem Details `code` per operation. | Real responses from the integration suite are validated against the committed OpenAPI document. There are no hand-written stubs. |
| E2E | Cross-flow stories where the owner path and the visitor path meet on one link over time (sad.md §6 "Cross-cutting: link lifecycle"), plus the storage-outage and auth-parity scenarios. | Driven through the public HTTP entry points of the booted application against the ephemeral real PostgreSQL. In this single-service, API-only backend, e2e is a multi-step integration scenario. |
| Load | Numeric NFRs in spec.md §6 that can be reproduced off-production (throughput, awake p95 latency). | The load tool already in your repo, or e.g. k6 or Locust. |
| Component / Visual-regression / E2E-through-UI | <!-- N/A: target_surfaces is [backend-service]; there is no UI --> | — |

## AC coverage

<!-- Levels confirmed per AC with the owner on 2026-11-04; implement reads this column and does not re-decide. -->

| AC (spec.md §5) | Test name (intent-based) | Level | Expected outcome |
|---|---|---|---|
| AC-01 create — happy path | generated short code is 7 characters from the 56-character alphabet | unit | every generated code has length 7 and uses only the alphabet; the look-alikes 0 O o 1 l I never appear; it comes from a secure random source |
| AC-01 | owner creates a short link for a valid https address | integration | an active link is recorded; the owner gets back the full short link, short code and target URL, and the target URL matches the submission byte for byte |
| AC-01 | full short link is built from the configured public address, not the request's host | integration | the full short link starts with the configured public address even when the request arrives with a different host header |
| AC-01 | create response matches the contract | contract | the response body matches the `createShortLink` success schema |
| AC-02 domain invariant | creating the same target URL twice issues two separate links | integration | a second link with a different short code exists; the first link's code, target and state are unchanged |
| AC-03 error | non-http(s) or relative target URLs are rejected | unit | `javascript:`, `data:`, `ftp:`, scheme-less and host-less inputs are each rejected with the "only http or https" rule |
| AC-03 | owner submitting a non-http(s) target URL is refused and nothing is created | integration | refused with the "only http or https web addresses" explanation; the link count is unchanged |
| AC-04 error | target URL length boundary at 2048 | unit | 2048 characters are accepted; 2049 are rejected with the maximum-length rule |
| AC-04 | owner submitting an over-long target URL is refused and nothing is created | integration | refused with "maximum length is 2048 characters"; the link count is unchanged |
| AC-05 error | unencoded target URLs are rejected without being trimmed or re-encoded | unit | leading or trailing spaces, inner spaces, line breaks, control characters, `é` in the path and a non-Latin host are each rejected; an `xn--` host and a percent-encoded path are accepted unchanged |
| AC-05 | owner submitting an unencoded target URL is refused and nothing is created | integration | refused with the "must be properly encoded" explanation; the link count is unchanged |
| AC-06 domain invariant | target URL pointing back to trimly is rejected | unit | each configured own host, any subdomain of one, upper-case variants and a trailing-dot variant are rejected; a host that only shares a suffix (e.g. `nottrimly.example`) is accepted |
| AC-06 | owner submitting a trimly short link as a target is refused | integration | refused with "a short link cannot point back to trimly"; the link count is unchanged |
| AC-07 follow — happy path | visitor opening an active link is redirected without reuse | integration | a temporary redirect to the stored target URL, exactly as stored, marked as not reusable by browsers or intermediaries |
| AC-07 | trailing slash and query string on a short link are tolerated and not forwarded | integration | a single trailing slash and any query string both still redirect to the stored target URL, and the query is not appended |
| AC-08 cross-context | a just-created link is followable immediately | e2e | the owner creates a link and the very next visitor request is redirected to its target URL |
| AC-09 list — happy path | owner lists links newest first, one page at a time | integration | each item shows short code, target URL, creation time and state; the order is newest first, and a same-instant tie puts the later-created one first; the default page size is 20 and up to 100 can be requested |
| AC-09 | paging stays stable while links are created mid-paging | integration | links created between page requests never make an already-listed link reappear or get skipped |
| AC-09 | continuation cursor round-trips and rejects garbage | unit | an encoded cursor decodes to the same position; a tampered or garbled cursor is reported as invalid |
| AC-09 | list response matches the contract | contract | the body matches the `listShortLinks` schema, and invalid page-size and invalid cursor errors carry their documented codes |
| AC-10 edge | listing with no links returns an empty list | integration | an empty list with no continuation, not an error |
| AC-11 disable — happy path | owner disables an active link | integration | the response confirms the disabled state; the owner's list then shows the link as disabled |
| AC-12 cross-context | disabled link is not followed, even by a visitor who followed it earlier | e2e | the visitor follows the link (redirected), the owner disables it, and the same visitor's next visit gets "link not found" with no redirect |
| AC-12 | redirect response forbids reuse without asking trimly | integration | the redirect carries no-reuse caching directives, so no cached copy can outlive a disable |
| AC-13 error | disabling or re-enabling an unknown short code reports not found | integration | for both disable and enable: "link not found"; nothing in storage changes |
| AC-14 domain invariant | repeating disable or re-enable is harmless | integration | the second disable (or enable) confirms the current state without an error, and the link's last-change time is unchanged, with the test clock advanced between the two calls so a rewrite of the time would be visible |
| AC-15 re-enable — happy path | re-enabled link redirects again to the same target under the same code | e2e | after disable then enable, a visitor is again redirected to the original target URL under the same short code |
| AC-16 edge | unknown, malformed and disabled codes all look identical to a visitor | integration | the same status, plain-text media type and exact "link not found" sentence for all three, with no hint whether the code existed |
| AC-16b error | shape check rejects anything that cannot be a short code | unit | wrong length, characters outside the alphabet and trailing punctuation are not well-formed; exactly 7 alphabet characters are |
| AC-16b | malformed visitor paths get "link not found" without a storage lookup | integration | extra path segments, a wrong length, `abc2345.` and out-of-alphabet characters all get "link not found" while link storage is paused |
| AC-17 cross-context | storage outage shows "temporarily unavailable" to visitors within 2 s | e2e | with link storage paused, a well-formed code gets the "temporarily unavailable, try again later" plain text within 2 s, never "link not found" and never a redirect |
| AC-17 | slow but answering storage is not an outage | integration | storage delayed below the 2 s budget (link storage paused for about 1 s and resumed in the background) still produces the normal redirect |
| AC-17b error | storage outage on owner operations reports temporarily unavailable within 2 s | e2e | with link storage paused, create, list, disable and enable each answer "temporarily unavailable" within 2 s and confirm nothing |
| AC-18 authorization | owner operations without a credential are denied | integration | create, list, disable and enable each refused with "authentication required"; nothing is created or changed and no link data is revealed |
| AC-18 | missing and wrong credentials get byte-identical denials | e2e | for each owner operation the two denial responses have identical status, media type and body byte for byte (headers that vary per request, such as the date, are excluded) and the same Problem Details shape as other owner errors |
| AC-18 | owner credential comparison accepts only the exact credential | unit | the exact credential is accepted; a prefix, the credential plus one extra character, a different case and an empty value are rejected |
| AC-19 authorization | following a link needs no credential | integration | a request with no credential is redirected; a request carrying a wrong credential is redirected too and not refused |
| AC-20 domain invariant | an issued short code is never reissued, even after disable | integration | inserting a link with an existing (active or disabled) short code is refused by storage and turned into a retry, never an overwrite |
| AC-20 | re-enabled code still leads to its original target | e2e | covered by the lifecycle scenario: after more links are created and A is re-enabled, A still leads to the original X |
| AC-20b domain invariant | a taken generated code is retried with a fresh one | integration | with a generator primed to return a taken code first, creation still succeeds with a different unused code |
| AC-20b | four taken codes then a free one still succeed | integration | with a generator primed to return 4 taken codes and then a free one, creation succeeds with the free code (pins the 5-attempt boundary) |
| AC-20b | five taken codes in a row report temporarily unavailable | integration | with a generator primed to return 5 taken codes, the owner gets "temporarily unavailable, try again later" and nothing is created |

## Edge cases / error paths

- Owner credential missing at startup → expected: the application refuses to start.
- Owner credential shorter than 32 characters, containing a space or non-printable/non-ASCII character, or with fewer than 16 distinct characters → expected: the application refuses to start; the credential value never appears in the startup failure output.
- Owner credential never appears in logs or responses → expected: no log line or response body produced during the AC-18 tests contains the credential value.
- Empty target URL / missing target URL field in the create request → expected: refused with the documented validation error; nothing created.
- Page size 0, negative, or above 100 → expected: refused with the invalid-page-size error, no data returned.
- Garbled or tampered continuation cursor → expected: refused with the invalid-cursor error, no data returned.
- Malformed short code on the owner disable/enable path → expected: "link not found", nothing changed (AC-13; whether storage is touched is not asserted).
- Owner-path errors never expose stack traces or exception messages → expected: every owner error body contains only the documented Problem Details fields.
- Two IDs generated in the same millisecond → expected: the second sorts after the first (monotonic UUIDv7), which keeps the AC-09 tie-break deterministic.
- Link storage unavailable (paused container) → expected: visitors get "temporarily unavailable" for well-formed codes (AC-17) and "link not found" for malformed ones (AC-16b); owners get "temporarily unavailable" (AC-17b). Nothing is served from a remembered copy.

## Test data

- Seed strategy: the `ShortLinkFixtures` builder from data-model.md §Test fixtures (`aShortLink()`, `.disabled()`, `.withShortCode(…)`, `.withTargetUrl(…)`, `.createdAt(…)`), using a seeded random source for codes and a fixed clock for times. Integration tests persist fixtures through the real repository, never by raw SQL, so the row mapping is exercised. Target URLs use only `example.test` hosts, with no real domains or personal data.
- Owner credential in tests: one fixed test-only random value of 32+ characters set through test configuration, never a production secret.
- Integration dependency: an ephemeral real PostgreSQL in a throwaway container, started once per test run with the repo's migrations applied on startup. The link storage is never mocked. Outage tests pause and resume the same container; every test that pauses it resumes it in its cleanup, so other tests are not affected.
- Cleanup boundary: per test. Each integration and e2e test empties `short_link` before it runs, so tests are order-independent and can assert exact counts. The container itself is per suite.
- Collision tests (AC-20b) replace only the short-code generator with a test double that returns predetermined codes. Storage stays real, so "taken" is the real unique-constraint violation.

## NFR validation (load)

Run before the deploy step (roadmap step 3) against one production-size instance with the link storage awake and an active fixture link. Use the load tool already in your repo, or e.g. k6 or Locust.

- **Throughput ≥ 50 redirects/s sustained:** follow one active short link at a constant 50 requests/s for 5 minutes after a 30 s warm-up. Pass when the error rate (anything other than the expected redirect) is 0 %.
- **Redirect p95 ≤ 50 ms server-side (storage awake):** in the same run, read the server-side request-duration metric for redirects. Pass when p95 ≤ 50 ms.
- **Owner operations p95 ≤ 300 ms (storage awake):** a mixed owner workload (create, list page, disable, enable) at 5 requests/s for 2 minutes. Pass when the server-side p95 for each operation is ≤ 300 ms.
- **After-idle latency (redirect ≤ 1 s, owner ≤ 1.5 s)** cannot be reproduced against a throwaway container, because it depends on the hosted database waking from idle. It is measured in production after step 3, per spec.md §8 OQ 3 and §6. It is not part of the load suite.
- **Storage-unavailable answer ≤ 2 s and disable propagation 0 s** are asserted by the AC-17/AC-17b and AC-12 tests above, not by load.
- **Availability 99.5 % and running cost ≤ $5/month** are operational measures (uptime probe, bills), not tests.

## CI placement

- On every push and PR (the existing CI verify gate): unit, integration, contract and e2e. They all run against the throwaway container, the suite is small and e2e here is in-process HTTP, so nothing needs to move to a schedule.
- Pre-release (before roadmap step 3's deploy) and on demand: the load scenarios above. They are not wired into PR CI.
