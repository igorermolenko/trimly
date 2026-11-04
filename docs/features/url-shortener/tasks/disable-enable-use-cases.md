---
id: T9
title: "Implement DisableShortLink and EnableShortLink as one idempotent state change"
layer: "app"
deps: ["T5"]
blocks: ["T14"]
acs: ["AC-11", "AC-13", "AC-14", "AC-15"]
files_hint: ["src/main/java/io/github/igorermolenko/trimly/link/app/ChangeLinkState.java", "src/main/java/io/github/igorermolenko/trimly/link/app/DisableShortLink.java", "src/main/java/io/github/igorermolenko/trimly/link/app/EnableShortLink.java", "src/main/java/io/github/igorermolenko/trimly/link/domain/LinkNotFound.java", "src/test/java/io/github/igorermolenko/trimly/link/app/ChangeLinkStateIT.java"]
owner: "igorermolenko"
estimate: "S"
context_budget: "M"     # 61 inlined lines
status: "todo"
step: ""
---

<!-- To the executing agent: work from what is inlined here. If a slice is insufficient, ambiguous,
or contradicts the code in front of you, open the named file for the full text and follow that.
Do not invent the missing part. Every quote ends with its provenance signature; the source wins. -->

# T9 — Implement DisableShortLink and EnableShortLink as one idempotent state change

## Place in the sequence

- **Blocked by:** T5 — Persist ShortLink through a Spring Data JDBC store with constraint-aware failure translation
- **Blocks:** T14 — Expose GET /api/v1/links and POST …/{shortCode}/disable|enable
- **Wave:** 3 — after T5 (longest path).
- **Lane:** own lane.

## Why (user story)

> **As an** owner
> **I want** to switch a short link off
> **So that** a mistaken or abused link stops working for everyone at once
>
> — `spec.md §4, US-04, verbatim` · full text: [spec.md](../spec.md)

This task switches a link off or back on in one statement and confirms its current state.

## Inlined context

> Each owner write is a single SQL statement, with no multi-statement transactions. Create retries run as separate statements (a failed INSERT aborts a PostgreSQL transaction). Disable and re-enable are one idempotent `UPDATE … SET state`, so repeating one is harmless (AC-14) and the last write wins. No optimistic locking with a single owner. Every call has the 2 s budget
>
> — `sad.md §8, Storage access row, verbatim` · full text: [sad.md](../sad.md)

> S->>D: set the state of the link with this short code to the target state, in one statement (2 s budget)
> alt storage answers and a link matched: confirms the link's current state (also when it was already in that state)
> else storage answers and no link has this short code: "link not found", nothing changed
> else storage does not answer within 2 s: "temporarily unavailable, try again later", nothing confirmed
> Postcondition: the link is in the target state and the next visit sees it. The target URL and short code never change
>
> — `sad.md §6, «Disable or re-enable a short link», abridged` · full text: [sad.md](../sad.md)

**Fallback:** insufficient or contradicted by the code → read the named file in full
([spec.md](../spec.md) · [sad.md](../sad.md) · [data-model.md](../data-model.md) ·
[openapi.yaml](../contracts/openapi.yaml) · [adr/](../adr/)) and follow it. Do not guess.

## Data delta

| Column | Type | Constraints | Change |
|---|---|---|---|
| `short_code` | `TEXT COLLATE "C"` | NOT NULL, UNIQUE (`uq_short_link_short_code`), CHECK length = 7 | read (WHERE) |
| `state` | `TEXT` | NOT NULL, CHECK in (`ACTIVE`, `DISABLED`) | updated |
| `updated_at` | `TIMESTAMPTZ` | NOT NULL, last real state change, from `Clock` | updated only on a real change |

— `data-model.md §Entities, table short_link, abridged` · full text: [data-model.md](../data-model.md)

| Pattern | Statement shape |
|---|---|
| Disable / re-enable (AC-11, AC-13, AC-14, AC-15) | `UPDATE short_link SET updated_at = CASE WHEN state <> :state THEN :now ELSE updated_at END, state = :state WHERE short_code = :code RETURNING short_code, state`. No row returned → "link not found" (AC-13). A repeat matches the row but changes nothing, so it confirms the current state (AC-14) |

— `data-model.md §Entities, Access patterns, abridged` · full text: [data-model.md](../data-model.md)

## API contract

- `POST /api/v1/links/{shortCode}/disable` and `/enable` → `200 ShortLinkState {shortCode, state}`; `404 link.not_found`; `503 link.temporarily_unavailable`. HTTP mapping is T14.
- A path value that cannot be a short code is `404 link.not_found` **without a storage lookup** (`ShortCodePath` parameter).

— `contracts/openapi.yaml, operationId disableShortLink, enableShortLink, abridged` · full text: [openapi.yaml](../contracts/openapi.yaml)

## Acceptance criteria

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

## Checklist

- [ ] `src/main/java/io/github/igorermolenko/trimly/link/app/ChangeLinkState.java`: `ShortCode.isWellFormed` false → `LinkNotFound` (no store call); else `store.setState(code, target, clock.instant())` via `StorageFailures`; empty → `LinkNotFound`; else return `(shortCode, state)`.
- [ ] `src/main/java/io/github/igorermolenko/trimly/link/app/DisableShortLink.java` / `EnableShortLink.java`: thin use cases over it (separate classes keep per-operation metrics/handlers distinct).
- [ ] `src/main/java/io/github/igorermolenko/trimly/link/domain/LinkNotFound.java`: domain signal.
- [ ] `src/test/java/io/github/igorermolenko/trimly/link/app/ChangeLinkStateIT.java`.

## Edge cases

| Case | Behaviour |
|---|---|
| Disable an active link | State DISABLED, `updated_at` changes (AC-11). |
| Disable an already disabled link | Confirms DISABLED, no error, `updated_at` unchanged (AC-14). |
| Enable a disabled link | State ACTIVE; same code and target URL (AC-15, AC-20). |
| Unknown well-formed code | `LinkNotFound`, nothing changes (AC-13). |
| Malformed code (`abc2345.`) | `LinkNotFound` with zero storage calls. |
| Storage down | `LinkStorageUnavailable` (AC-17b). |

## Definition of Done

- [ ] `ChangeLinkStateIT` passes for every Edge-case row (store spy asserts no call for a malformed code).
- [ ] `short_code` and `target_url` are never in the UPDATE's SET list (review check).
- [ ] `ModularityTest` green.
- [ ] every Hard Rule inlined above still holds
- [ ] lint + vet clean (`./mvnw -B spotless:check`, `./mvnw -B verify`)
