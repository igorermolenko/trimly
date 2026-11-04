---
id: T10
title: "Publish link.app.LinkLookup returning a sealed Resolution (Found / NotFound / Unavailable)"
layer: "app"
deps: ["T5"]
blocks: ["T15"]
acs: ["AC-12", "AC-16", "AC-16b", "AC-17"]
files_hint: ["src/main/java/io/github/igorermolenko/trimly/link/app/LinkLookup.java", "src/main/java/io/github/igorermolenko/trimly/link/app/Resolution.java", "src/test/java/io/github/igorermolenko/trimly/link/app/LinkLookupTest.java", "src/test/java/io/github/igorermolenko/trimly/link/app/LinkLookupIT.java"]
owner: "igorermolenko"
estimate: "S"
context_budget: "M"     # 61 inlined lines
status: "todo"
step: ""
---

<!-- To the executing agent: work from what is inlined here. If a slice is insufficient, ambiguous,
or contradicts the code in front of you, open the named file for the full text and follow that.
Do not invent the missing part. Every quote ends with its provenance signature; the source wins. -->

# T10 — Publish link.app.LinkLookup returning a sealed Resolution (Found / NotFound / Unavailable)

## Place in the sequence

- **Blocked by:** T5 — Persist ShortLink through a Spring Data JDBC store with constraint-aware failure translation
- **Blocks:** T15 — Serve GET /{shortCode}: non-reusable 302, plain-text 404/503, query ignored
- **Wave:** 3 — after T5 (longest path).
- **Lane:** own lane.

## Why (user story)

> **As a** visitor
> **I want** a plain message that says either "link not found" or "temporarily unavailable"
> **So that** I know whether the link is dead or worth retrying later
>
> — `spec.md §4, US-06, verbatim` · full text: [spec.md](../spec.md)

This task is the only door the redirect path uses into link storage, and it alone decides «not found» vs «unavailable».

## Inlined context

> The one cross-module contract is `link.app.LinkLookup` ([ADR-0005]). `redirect` passes it the raw path segment. `link` checks the shape (AC-16b, no storage access when malformed), reads storage within the 2 s budget, and returns a sealed result: `Found(targetUrl)`, `NotFound` (unknown, disabled or malformed, indistinguishable per AC-16) or `Unavailable`. The short-code alphabet never leaves `link`. `redirect` maps each result to a visitor answer itself, so visitor answers never pass through `shared.ProblemDetailsAdvice`.
>
> — `sad.md §5, LinkLookup paragraph, verbatim` · full text: [sad.md](../sad.md)

> **Chosen:** Option 1, sealed result, no exceptions. The compiler's exhaustive `switch` forces `redirect` to handle all three outcomes. Visitor answers never reach the shared advice, so an unhandled case cannot silently become JSON.
>
> — `adr/0005-return-short-link-lookups-as-a-sealed-result-and-answer-visitors-in-plain-text.md §Decision outcome, abridged` · full text: [adr/0005-return-short-link-lookups-as-a-sealed-result-and-answer-visitors-in-plain-text.md](../adr/0005-return-short-link-lookups-as-a-sealed-result-and-answer-visitors-in-plain-text.md)

> **Hard rule:** `domain` has no Spring or persistence imports. Modules call only each other's published `api`/`app` types, through direct method calls. `ModularityTest` (Spring Modulith `verify()`) fails the build on any violation.
>
> — `sad.md §2, Technical constraints (Architecture bullet), abridged` · full text: [sad.md](../sad.md)

> **Hard rule (no redirect caching):** AC-17 says trimly "does not serve the redirect from any remembered copy of the link", and spec §6 requires a disable to reach the next visit in 0 s. Every redirect is therefore resolved against link storage.
>
> — `sad.md §2, Technical constraints (No redirect caching), abridged` · full text: [sad.md](../sad.md)

**Fallback:** insufficient or contradicted by the code → read the named file in full
([spec.md](../spec.md) · [sad.md](../sad.md) · [data-model.md](../data-model.md) ·
[openapi.yaml](../contracts/openapi.yaml) · [adr/](../adr/)) and follow it. Do not guess.

## Data delta

| Column | Type | Constraints | Change |
|---|---|---|---|
| `short_code` | `TEXT COLLATE "C"` | NOT NULL, UNIQUE (`uq_short_link_short_code`), CHECK length = 7 | read (WHERE) |
| `target_url` | `TEXT` | NOT NULL, CHECK `char_length` 1..2048 | read |
| `state` | `TEXT` | NOT NULL, CHECK in (`ACTIVE`, `DISABLED`) | read |

— `data-model.md §Entities, table short_link, abridged` · full text: [data-model.md](../data-model.md)

| Pattern | Statement shape |
|---|---|
| Follow (AC-07, AC-12, AC-16, AC-17) | `SELECT target_url, state FROM short_link WHERE short_code = :code` (a disabled link maps to `NotFound` in `link.app`) |

— `data-model.md §Entities, Access patterns, abridged` · full text: [data-model.md](../data-model.md)

## API contract

Internal — no API surface. (published in-process contract for `redirect`; HTTP is T15.)

## Acceptance criteria

### AC-12 — cross-context

> **Given** the owner has disabled a short link
> **When** any visitor opens it, including a visitor who opened it before it was disabled
> **Then** the visitor sees "link not found" and is not sent to the target URL
>
> — `spec.md §5, AC-12, verbatim` · full text: [spec.md](../spec.md)

### AC-16 — happy path (edge)

> **Given** a short code does not exist, or is not a well-formed short code
> **When** a visitor opens it
> **Then** the visitor sees "link not found" as a short human-readable sentence that a browser displays as plain text, identical to what a disabled link shows, with no hint whether the code ever existed
>
> — `spec.md §5, AC-16, verbatim` · full text: [spec.md](../spec.md)

### AC-16b — error

> **Given** a visitor opens an address whose shape cannot be a short code (wrong length, characters outside the short-code alphabet defined in AC-01, extra path segments, or trailing punctuation such as "abc2345.")
> **When** trimly handles it
> **Then** the visitor gets "link not found" without trimly looking anything up in its link storage, so the answer stays "link not found" even while the link storage is unavailable. A single trailing slash after a short code and any query string the visitor's address carries do not make it malformed: they are accepted as in AC-07
>
> — `spec.md §5, AC-16b, verbatim` · full text: [spec.md](../spec.md)

### AC-17 — cross-context

> **Given** trimly temporarily cannot look up links because its link storage has not answered within 2 seconds
> **When** a visitor opens a well-formed short code
> **Then** the visitor sees "temporarily unavailable, try again later" as a short human-readable sentence, no later than 2 seconds after opening the link, not "link not found"; trimly does not serve the redirect from any remembered copy of the link. Link storage that is waking from idle and answers within the 2 seconds is not unavailable: the visitor gets the normal outcome (AC-07, AC-12 or AC-16)
>
> — `spec.md §5, AC-17, verbatim` · full text: [spec.md](../spec.md)

## Checklist

- [ ] `src/main/java/io/github/igorermolenko/trimly/link/app/Resolution.java`: `sealed interface Resolution permits Found, NotFound, Unavailable`; `Found(String targetUrl)`; `NotFound` and `Unavailable` singletons/records.
- [ ] `src/main/java/io/github/igorermolenko/trimly/link/app/LinkLookup.java`: `Resolution resolve(String rawSegment)`; malformed → `NotFound` with no store call; else `store.findByShortCode` via `StorageFailures`; empty or DISABLED → `NotFound`; ACTIVE → `Found`; `LinkStorageUnavailable` → `Unavailable`. No cache, no exceptions escape for expected outcomes.
- [ ] Mark `link.app` (or just `LinkLookup` + `Resolution`) as published to Spring Modulith (`@NamedInterface` or package-info) so `redirect` may use it.
- [ ] Tests: `src/test/java/io/github/igorermolenko/trimly/link/app/LinkLookupTest.java` (stub store), `src/test/java/io/github/igorermolenko/trimly/link/app/LinkLookupIT.java`.

## Edge cases

| Case | Behaviour |
|---|---|
| Malformed segment while storage is down | `NotFound`, zero storage calls (AC-16b). |
| Unknown code / disabled link | Both `NotFound`, indistinguishable (AC-12, AC-16). |
| Storage timeout on a well-formed code | `Unavailable`, never `NotFound` (AC-17). |
| Disabled then re-enabled | Next call `Found` — read every time, nothing remembered. |
| Unexpected exception (bug) | Propagates — becomes a generic 500 via the shared advice (ADR-0005), not swallowed. |

## Definition of Done

- [ ] `LinkLookupTest` passes: three outcomes + no store call on malformed input.
- [ ] `LinkLookupIT` passes: active → Found with exact `target_url`, disabled/unknown → NotFound.
- [ ] `ModularityTest` green with `LinkLookup` exposed as a published type.
- [ ] every Hard Rule inlined above still holds
- [ ] lint + vet clean (`./mvnw -B spotless:check`, `./mvnw -B verify`)
