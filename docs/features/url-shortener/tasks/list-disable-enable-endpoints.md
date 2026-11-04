---
id: T14
title: "Expose GET /api/v1/links and POST …/{shortCode}/disable|enable"
layer: "ports"
deps: ["T8", "T9", "T13"]
blocks: ["T16"]
acs: ["AC-09", "AC-10", "AC-11", "AC-13", "AC-14", "AC-15", "AC-17b"]
files_hint: ["src/main/java/io/github/igorermolenko/trimly/link/api/ListShortLinksController.java", "src/main/java/io/github/igorermolenko/trimly/link/api/ChangeLinkStateController.java", "src/main/java/io/github/igorermolenko/trimly/link/api/LinkProblems.java", "src/test/java/io/github/igorermolenko/trimly/link/api/ListShortLinksIT.java", "src/test/java/io/github/igorermolenko/trimly/link/api/ChangeLinkStateIT.java"]
owner: "igorermolenko"
estimate: "M"
context_budget: "M"     # 67 inlined lines
status: "todo"
step: ""
---

<!-- To the executing agent: work from what is inlined here. If a slice is insufficient, ambiguous,
or contradicts the code in front of you, open the named file for the full text and follow that.
Do not invent the missing part. Every quote ends with its provenance signature; the source wins. -->

# T14 — Expose GET /api/v1/links and POST …/{shortCode}/disable|enable

## Place in the sequence

- **Blocked by:** T8 — Implement ListShortLinks with keyset paging and an opaque continuation cursor · T9 — Implement DisableShortLink and EnableShortLink as one idempotent state change · T13 — Expose POST /api/v1/links and the Problem Details `code` mapping for owner errors
- **Blocks:** T16 — Add end-to-end ITs: link lifecycle, storage outage within 2 s, and auth denial parity
- **Wave:** 5 — after T8, T9, T13 (longest path).
- **Lane:** shares files with T13 (`files_hint` overlap) — serialized by `implement`.

## Why (user story)

> **As an** owner
> **I want** to see all my short links, newest first and one page at a time, each with its short code, target URL, creation time and state (active or disabled)
> **So that** I know what exists and can find the link I want to act on
>
> — `spec.md §4, US-03, verbatim` · full text: [spec.md](../spec.md)

This task gives the owner the list and the off/on switches over HTTP.

## Inlined context

> **Owner path:** `link.domain` signals rule breaks (AC-03…AC-06), unknown codes (AC-13) and storage unavailability (AC-17b). `link.api` turns them into Spring `ErrorResponseException`s, which the single `shared.ProblemDetailsAdvice` renders as RFC 9457 Problem Details with no stack trace and no exception message.
>
> — `sad.md §8, Error handling row (owner path), abridged` · full text: [sad.md](../sad.md)

> Disable and re-enable are two endpoints, not one `PUT …/state`, because sad.md §7 breaks the p95 metric down per operation via the `uri` tag ("redirect, create, list, disable and re-enable"). Both are `POST` with no body. Their behaviour is still idempotent (AC-14).
>
> — `contracts/api-sync-report.md §Operations, verbatim` · full text: [contracts/api-sync-report.md](../contracts/api-sync-report.md)

**Fallback:** insufficient or contradicted by the code → read the named file in full
([spec.md](../spec.md) · [sad.md](../sad.md) · [data-model.md](../data-model.md) ·
[openapi.yaml](../contracts/openapi.yaml) · [adr/](../adr/)) and follow it. Do not guess.

## Data delta

No DB changes.

## API contract

- `GET /api/v1/links?after&limit` → `200 ShortLinkPage {items: ShortLink[], hasNext, nextCursor|null}`; `400 link.invalid_page_size` «The page size must be a whole number from 1 to 100.»; `400 link.invalid_cursor` «The continuation token is not valid. Start again from the first page.»; `503`.
- `POST /api/v1/links/{shortCode}/disable` → `200 {shortCode, state: DISABLED}`; `/enable` → `200 {shortCode, state: ACTIVE}`; `404 link.not_found` «The link was not found.»; `503 link.temporarily_unavailable`. No request body.
- `ShortLink.shortUrl` in list items is built from `trimly.public-base-url` as in T13.

— `contracts/openapi.yaml, operationId listShortLinks, disableShortLink, enableShortLink, abridged` · full text: [openapi.yaml](../contracts/openapi.yaml)

## Acceptance criteria

### AC-09 — happy path

> **Given** the owner has several short links, some active and some disabled
> **When** the owner lists their short links
> **Then** the owner sees each link's short code, target URL, creation time and state, newest first (links created at the same moment: the later-created one first), one page at a time: 20 links per page by default, up to 100 if the owner asks. Each page offers a way to continue right after its last link, so links created while the owner is paging never make a link appear twice or get skipped
>
> — `spec.md §5, AC-09, verbatim` · full text: [spec.md](../spec.md)

### AC-10 — happy path (edge)

> **Given** the owner has not created any short links yet
> **When** the owner lists their short links
> **Then** the owner gets an empty list, not an error
>
> — `spec.md §5, AC-10, verbatim` · full text: [spec.md](../spec.md)

### AC-11 — happy path

> **Given** an active short link exists
> **When** the owner disables it
> **Then** the system records the link as disabled and confirms it, and the owner's list shows it as disabled
>
> — `spec.md §5, AC-11, verbatim` · full text: [spec.md](../spec.md)

### AC-13 — error

> **Given** the owner presents the owner credential
> **When** the owner tries to disable or re-enable a short code that does not exist
> **Then** the system tells the owner the link was not found and nothing changes
>
> — `spec.md §5, AC-13, verbatim` · full text: [spec.md](../spec.md)

### AC-14 — domain invariant

> **Given** a short link is already disabled (or already active)
> **When** the owner disables it (or re-enables it) again
> **Then** nothing changes and the system confirms the current state without reporting an error ("repeating a state change is harmless")
>
> — `spec.md §5, AC-14, verbatim` · full text: [spec.md](../spec.md)

### AC-15 — happy path

> **Given** a disabled short link exists
> **When** the owner re-enables it
> **Then** visitors who open it are again sent to the same target URL under the same short code
>
> — `spec.md §5, AC-15, verbatim` · full text: [spec.md](../spec.md)

### AC-17b — error

> **Given** the owner presents the owner credential and the link storage has not answered within 2 seconds
> **When** the owner creates, lists, disables or re-enables short links
> **Then** the owner is told "temporarily unavailable, try again later" and nothing is confirmed; if an unconfirmed creation was in fact recorded, retrying it creates a second, separate short link (AC-02), which is accepted: the owner sees both in the list and can disable the extra one
>
> — `spec.md §5, AC-17b, verbatim` · full text: [spec.md](../spec.md)

## Checklist

- [ ] `src/main/java/io/github/igorermolenko/trimly/link/api/LinkProblems.java`: add `invalid_page_size`, `invalid_cursor`, `not_found` (shared lane with T13).
- [ ] `src/main/java/io/github/igorermolenko/trimly/link/api/ListShortLinksController.java`: bind `limit` as a string/Integer so `abc` → `link.invalid_page_size` (not a generic 400).
- [ ] `src/main/java/io/github/igorermolenko/trimly/link/api/ChangeLinkStateController.java`: two `@PostMapping`s with distinct URI templates.
- [ ] Tests: `src/test/java/io/github/igorermolenko/trimly/link/api/ListShortLinksIT.java`, `src/test/java/io/github/igorermolenko/trimly/link/api/ChangeLinkStateIT.java`.

## Edge cases

| Case | Behaviour |
|---|---|
| No links | `200 {items: [], hasNext: false, nextCursor: null}` (AC-10). |
| `limit=0`, `101`, `abc` | 400 `link.invalid_page_size`. |
| `after=garbage` | 400 `link.invalid_cursor`. |
| Disable twice | 200 DISABLED both times (AC-14). |
| Unknown or malformed code | 404 `link.not_found`, nothing changes (AC-13). |
| Storage down | 503 `link.temporarily_unavailable` (AC-17b). |

## Definition of Done

- [ ] `ListShortLinksIT` passes AC-09 (order, default/max size, continuation), AC-10 and the two 400 codes.
- [ ] `ChangeLinkStateIT` passes AC-11, AC-13, AC-14, AC-15 and a 503 case.
- [ ] Responses match the contract field sets (no extra members, `additionalProperties: false`).
- [ ] every Hard Rule inlined above still holds
- [ ] lint + vet clean (`./mvnw -B spotless:check`, `./mvnw -B verify`)
