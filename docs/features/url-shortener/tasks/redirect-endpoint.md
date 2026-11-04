---
id: T15
title: "Serve GET /{shortCode}: non-reusable 302, plain-text 404/503, query ignored"
layer: "ports"
deps: ["T10", "T12"]
blocks: ["T16"]
acs: ["AC-07", "AC-12", "AC-16", "AC-16b", "AC-17", "AC-19"]
files_hint: ["src/main/java/io/github/igorermolenko/trimly/redirect/", "src/test/java/io/github/igorermolenko/trimly/redirect/"]
owner: "igorermolenko"
estimate: "M"
context_budget: "M"     # 100 inlined lines
status: "todo"
step: ""
---

<!-- To the executing agent: work from what is inlined here. If a slice is insufficient, ambiguous,
or contradicts the code in front of you, open the named file for the full text and follow that.
Do not invent the missing part. Every quote ends with its provenance signature; the source wins. -->

# T15 — Serve GET /{shortCode}: non-reusable 302, plain-text 404/503, query ignored

## Place in the sequence

- **Blocked by:** T10 — Publish link.app.LinkLookup returning a sealed Resolution (Found / NotFound / Unavailable) · T12 — Guard the owner API with a stateless bearer SecurityFilterChain and a Problem Details entry point
- **Blocks:** T16 — Add end-to-end ITs: link lifecycle, storage outage within 2 s, and auth denial parity
- **Wave:** 4 — after T10, T12 (longest path).
- **Lane:** own lane.

## Why (user story)

> **As a** visitor
> **I want** to open a short link and land on its target URL
> **So that** I reach the page without typing the long address
>
> — `spec.md §4, US-02, verbatim` · full text: [spec.md](../spec.md)

This task is the visitor's whole experience: a redirect, «link not found» or «temporarily unavailable».

## Inlined context

```mermaid
sequenceDiagram
    actor visitor
    participant redirect as redirect module
    participant link as link module
    participant db as Link storage
    visitor->>redirect: opens a short link (no credential, query string ignored)
    redirect->>link: resolve the raw short code via LinkLookup
    alt not a well-formed short code
        link-->>redirect: NotFound (storage not touched)
        redirect-->>visitor: plain text "link not found"
    else well-formed short code
        link->>db: read the link by short code (2 s budget)
        alt storage answers and the link is active
            db-->>link: link with its target URL
            link-->>redirect: Found with the target URL
            redirect-->>visitor: temporary redirect to the target URL, marked not reusable
        else storage answers and the link is unknown or disabled
            db-->>link: no active link
            link-->>redirect: NotFound
            redirect-->>visitor: plain text "link not found"
        else storage does not answer within 2 s
            db--xlink: timeout or connection failure
            link-->>redirect: Unavailable
            redirect-->>visitor: plain text "temporarily unavailable, try again later"
        end
    end
```

— `sad.md §6, Critical flow 1 «follow a short link», verbatim` · full text: [sad.md](../sad.md)

```text
redirect/                    read path
├── api/       redirect controller: one path segment (+ optional single trailing slash, query ignored)
└── app/       maps Resolution → redirect (not reusable) | "link not found" | "temporarily unavailable"
```

— `sad.md §5, internal decomposition (redirect), verbatim` · full text: [sad.md](../sad.md)

> The redirect is always a temporary redirect, never a permanent (cacheable) one, and carries `Cache-Control: no-store`, so no browser or intermediary reuses it without asking trimly. That gives 0 s disable propagation (AC-07, AC-12). Spring Security's default headers must not override it, so a test asserts the header
>
> — `sad.md §8, Redirect reuse row, verbatim` · full text: [sad.md](../sad.md)

> **Chosen:** Option 1, sealed result, no exceptions. The compiler's exhaustive `switch` forces `redirect` to handle all three outcomes. Visitor answers never reach the shared advice, so an unhandled case cannot silently become JSON.
>
> — `adr/0005-return-short-link-lookups-as-a-sealed-result-and-answer-visitors-in-plain-text.md §Decision outcome, abridged` · full text: [adr/0005-return-short-link-lookups-as-a-sealed-result-and-answer-visitors-in-plain-text.md](../adr/0005-return-short-link-lookups-as-a-sealed-result-and-answer-visitors-in-plain-text.md)

> **Hard rule:** The app writes nothing about visitors: no per-redirect log line, no IP, no user agent.
>
> — `sad.md §8, Logging row, abridged` · full text: [sad.md](../sad.md)

**Fallback:** insufficient or contradicted by the code → read the named file in full
([spec.md](../spec.md) · [sad.md](../sad.md) · [data-model.md](../data-model.md) ·
[openapi.yaml](../contracts/openapi.yaml) · [adr/](../adr/)) and follow it. Do not guess.

## Data delta

No DB changes.

## API contract

- `GET /{shortCode}` (`security: []`): one segment, optional single trailing slash; query string ignored and never forwarded; extra segments → 404 without lookup.
- `302` — `Location` = target URL exactly as stored, empty body, `Cache-Control: no-store`.
- `404` — `text/plain` body exactly `Link not found.`, `Cache-Control: no-store`.
- `503` — `text/plain` body exactly `Temporarily unavailable, try again later.`, `Cache-Control: no-store`, ≤ 2 s after the request.

— `contracts/openapi.yaml, operationId followShortLink, abridged` · full text: [openapi.yaml](../contracts/openapi.yaml)

## Acceptance criteria

### AC-07 — happy path

> **Given** an active short link exists
> **When** a visitor opens it
> **Then** the visitor is sent straight to its target URL exactly as stored, with no trimly page in between. This also holds when the visitor's address ends with a single trailing slash or carries a query string; that query string is ignored and never passed on to the target URL. No browser or intermediary may reuse that redirect later without asking trimly again (so every visit reaches trimly and a later disable applies to it)
>
> — `spec.md §5, AC-07, verbatim` · full text: [spec.md](../spec.md)

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

### AC-19 — authorization

> **Given** a visitor presents no credential at all
> **When** the visitor opens an active short link
> **Then** the visitor is sent to its target URL; following a short link never requires a credential
>
> — `spec.md §5, AC-19, verbatim` · full text: [spec.md](../spec.md)

## Checklist

- [ ] `src/main/java/io/github/igorermolenko/trimly/redirect/app/`: `FollowShortLink` maps `Resolution` with an exhaustive `switch` (no `default`) to a visitor answer value.
- [ ] `src/main/java/io/github/igorermolenko/trimly/redirect/api/`: controller for `/{segment}` and `/{segment}/`, plus a catch-all for multi-segment paths outside `/api/**` → 404 text without calling `LinkLookup`; builds `ResponseEntity` with `no-store` itself — never throws to `shared.ProblemDetailsAdvice` for expected outcomes.
- [ ] `src/test/java/io/github/igorermolenko/trimly/redirect/`: unit test of the mapping; `RedirectIT` (Testcontainers + real `link`).

## Edge cases

| Case | Behaviour |
|---|---|
| `/aB3dEf9/` | Same as `/aB3dEf9` (AC-07). |
| `/aB3dEf9?utm=x` | 302 to the stored URL; `utm=x` not appended (AC-07). |
| `/aB3dEf9/extra`, `/abc2345.`, `/toolongcode` | 404 text, no storage lookup (AC-16b). |
| Disabled link | 404 text identical to an unknown code (AC-12, AC-16). |
| Storage paused, well-formed code | 503 text in ≤ 2 s (AC-17). |
| Storage paused, malformed code | 404 text (AC-16b). |
| Any answer | `Cache-Control: no-store` exactly, even with Spring Security headers on; never `301`/`308`. |

## Definition of Done

- [ ] `RedirectIT` passes AC-07 (302, `Location`, `no-store`, slash/query), AC-12, AC-16, AC-16b (no lookup — spy), AC-19 (no credential).
- [ ] AC-17 case passes with a stubbed `Unavailable` (end-to-end outage test lives in T16).
- [ ] No log line per redirect; `ModularityTest` green (`redirect` uses only `link.app` published types).
- [ ] every Hard Rule inlined above still holds
- [ ] lint + vet clean (`./mvnw -B spotless:check`, `./mvnw -B verify`)
