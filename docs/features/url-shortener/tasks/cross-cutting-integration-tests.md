---
id: T16
title: "Add end-to-end ITs: link lifecycle, storage outage within 2 s, and auth denial parity"
layer: "tests"
deps: ["T6", "T14", "T15"]
blocks: []
acs: ["AC-02", "AC-08", "AC-12", "AC-15", "AC-17", "AC-17b", "AC-18", "AC-20"]
files_hint: ["src/test/java/io/github/igorermolenko/trimly/LinkLifecycleIT.java", "src/test/java/io/github/igorermolenko/trimly/StorageOutageIT.java", "src/test/java/io/github/igorermolenko/trimly/OwnerAuthDenialIT.java"]
owner: "igorermolenko"
estimate: "M"
context_budget: "M"     # 74 inlined lines
status: "done"
step: "COMMIT"
---

<!-- To the executing agent: work from what is inlined here. If a slice is insufficient, ambiguous,
or contradicts the code in front of you, open the named file for the full text and follow that.
Do not invent the missing part. Every quote ends with its provenance signature; the source wins. -->

# T16 — Add end-to-end ITs: link lifecycle, storage outage within 2 s, and auth denial parity

## Place in the sequence

- **Blocked by:** T6 — Bound every storage call to 2 s, drain the idle pool and keep health checks off the DB · T14 — Expose GET /api/v1/links and POST …/{shortCode}/disable|enable · T15 — Serve GET /{shortCode}: non-reusable 302, plain-text 404/503, query ignored
- **Blocks:** nothing — a leaf
- **Wave:** 6 — after T6, T14, T15 (longest path).
- **Lane:** own lane.

## Why (user story)

> **As an** owner
> **I want** to switch a short link off
> **So that** a mistaken or abused link stops working for everyone at once
>
> — `spec.md §4, US-04, verbatim` · full text: [spec.md](../spec.md)

This task proves the owner and visitor paths agree on one link over time, and that outages and auth failures look exactly as specified.

## Inlined context

> O creates X → A; V opens A → redirect to X (not reusable); O creates X again → B (A unchanged); O disables A; V opens A again (browser opened it before) → "link not found"; O re-enables A; V opens A → redirect to the same X. Postcondition: A and B both exist and both lead to X. No short code ever pointed at a different target URL
>
> — `sad.md §6, «Cross-cutting: link lifecycle», abridged` · full text: [sad.md](../sad.md)

> **How verify:** an integration test for AC-17 / AC-17b with link storage stopped (the Testcontainers PostgreSQL container paused), asserting the elapsed time is ≤ 2 s and the exact visitor text. A companion test shows that a malformed code still gets "link not found" during the outage (AC-16b).
>
> — `sad.md §10, QG-1a, verbatim` · full text: [sad.md](../sad.md)

> **How verify:** an integration test for AC-18 compares the two denial responses byte for byte and checks that storage is unchanged. A parity assertion checks that the denial has the same Problem Details shape (content type and field set) as an owner-path error rendered by `shared.ProblemDetailsAdvice`.
>
> — `sad.md §10, QG-3b, abridged` · full text: [sad.md](../sad.md)

**Fallback:** insufficient or contradicted by the code → read the named file in full
([spec.md](../spec.md) · [sad.md](../sad.md) · [data-model.md](../data-model.md) ·
[openapi.yaml](../contracts/openapi.yaml) · [adr/](../adr/)) and follow it. Do not guess.

## Data delta

No DB changes.

## API contract

- Uses all five operations as specified in T13–T15; asserts `302` + `Cache-Control: no-store`, `404`/`503` text bodies, `401`/`503` Problem Details.

— `contracts/openapi.yaml, operationId createShortLink, listShortLinks, disableShortLink, enableShortLink, followShortLink, abridged` · full text: [openapi.yaml](../contracts/openapi.yaml)

## Acceptance criteria

### AC-02 — domain invariant

> **Given** the owner already has a short link for target URL X
> **When** the owner submits target URL X again
> **Then** the system creates a separate short link with a different short code, and the earlier short link stays unchanged ("every creation issues a new short link")
>
> — `spec.md §5, AC-02, verbatim` · full text: [spec.md](../spec.md)

### AC-08 — cross-context

> **Given** the owner has just created a short link
> **When** a visitor opens that short link immediately afterwards
> **Then** the visitor reaches its target URL; a link is usable for redirects the moment its creation is confirmed
>
> — `spec.md §5, AC-08, verbatim` · full text: [spec.md](../spec.md)

### AC-12 — cross-context

> **Given** the owner has disabled a short link
> **When** any visitor opens it, including a visitor who opened it before it was disabled
> **Then** the visitor sees "link not found" and is not sent to the target URL
>
> — `spec.md §5, AC-12, verbatim` · full text: [spec.md](../spec.md)

### AC-15 — happy path

> **Given** a disabled short link exists
> **When** the owner re-enables it
> **Then** visitors who open it are again sent to the same target URL under the same short code
>
> — `spec.md §5, AC-15, verbatim` · full text: [spec.md](../spec.md)

### AC-17 — cross-context

> **Given** trimly temporarily cannot look up links because its link storage has not answered within 2 seconds
> **When** a visitor opens a well-formed short code
> **Then** the visitor sees "temporarily unavailable, try again later" as a short human-readable sentence, no later than 2 seconds after opening the link, not "link not found"; trimly does not serve the redirect from any remembered copy of the link. Link storage that is waking from idle and answers within the 2 seconds is not unavailable: the visitor gets the normal outcome (AC-07, AC-12 or AC-16)
>
> — `spec.md §5, AC-17, verbatim` · full text: [spec.md](../spec.md)

### AC-17b — error

> **Given** the owner presents the owner credential and the link storage has not answered within 2 seconds
> **When** the owner creates, lists, disables or re-enables short links
> **Then** the owner is told "temporarily unavailable, try again later" and nothing is confirmed; if an unconfirmed creation was in fact recorded, retrying it creates a second, separate short link (AC-02), which is accepted: the owner sees both in the list and can disable the extra one
>
> — `spec.md §5, AC-17b, verbatim` · full text: [spec.md](../spec.md)

### AC-18 — authorization

> **Given** a person presents no owner credential, or a wrong one
> **When** they try to create, list, disable or re-enable short links
> **Then** the system denies the action, says authentication is required, creates or changes nothing, and reveals no link data; a missing credential and a wrong one get an identical answer
>
> — `spec.md §5, AC-18, verbatim` · full text: [spec.md](../spec.md)

### AC-20 — domain invariant

> **Given** a short code has been issued, whether its link is now active or disabled
> **When** the owner creates any number of new short links
> **Then** none of them is ever given that short code, and re-enabling that code's link still leads to its original target URL ("a short code never points to a different target URL")
>
> — `spec.md §5, AC-20, verbatim` · full text: [spec.md](../spec.md)

## Checklist

- [ ] `src/test/java/io/github/igorermolenko/trimly/LinkLifecycleIT.java`: the sad §6 lifecycle over real HTTP (MockMvc or `RestClient` against a random port, redirects not followed).
- [ ] `src/test/java/io/github/igorermolenko/trimly/StorageOutageIT.java`: pause the container; time each owner op (503 Problem Details) and a visitor well-formed code (503 text) ≤ 2 s; malformed code → 404 text; unpause → normal answers.
- [ ] `src/test/java/io/github/igorermolenko/trimly/OwnerAuthDenialIT.java`: missing vs wrong credential on all four owner ops → byte-identical; row count unchanged; denial `Content-Type` and field set equal to a `ProblemDetailsAdvice`-rendered 400 from T13.

## Edge cases

| Case | Behaviour |
|---|---|
| Container paused mid-suite | Unpause in `@AfterEach` so later tests aren't poisoned. |
| Timing flakiness | Assert ≤ 2 s with the configured split, not a tighter bound; no `Thread.sleep`. |
| Unconfirmed create retried | Second link exists; both listed (AC-17b) — asserted after unpause if the INSERT committed. |
| Redirect followed by the client | Disable auto-follow so the 302 itself is asserted. |

## Definition of Done

- [ ] All three ITs pass in `./mvnw -B verify` (Docker).
- [ ] Every AC in this task's `acs` is asserted by name in a test display name or comment, for the plan-tests AC → test table.
- [ ] `ModularityTest` + `spotless:check` green — the feature's full gate.
- [ ] every Hard Rule inlined above still holds
- [ ] lint + vet clean (`./mvnw -B spotless:check`, `./mvnw -B verify`)
