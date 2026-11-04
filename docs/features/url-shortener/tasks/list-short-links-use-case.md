---
id: T8
title: "Implement ListShortLinks with keyset paging and an opaque continuation cursor"
layer: "app"
deps: ["T2", "T5"]
blocks: ["T14"]
acs: ["AC-09", "AC-10"]
files_hint: ["src/main/java/io/github/igorermolenko/trimly/link/app/ListShortLinks.java", "src/main/java/io/github/igorermolenko/trimly/link/app/PageCursor.java", "src/test/java/io/github/igorermolenko/trimly/link/app/ListShortLinksIT.java", "src/test/java/io/github/igorermolenko/trimly/link/app/PageCursorTest.java"]
owner: "igorermolenko"
estimate: "S"
context_budget: "M"     # 55 inlined lines
status: "todo"
step: ""
---

<!-- To the executing agent: work from what is inlined here. If a slice is insufficient, ambiguous,
or contradicts the code in front of you, open the named file for the full text and follow that.
Do not invent the missing part. Every quote ends with its provenance signature; the source wins. -->

# T8 — Implement ListShortLinks with keyset paging and an opaque continuation cursor

## Place in the sequence

- **Blocked by:** T2 — Make shared.IdGenerator monotonic within a millisecond (RFC 9562 method 1) · T5 — Persist ShortLink through a Spring Data JDBC store with constraint-aware failure translation
- **Blocks:** T14 — Expose GET /api/v1/links and POST …/{shortCode}/disable|enable
- **Wave:** 3 — after T2, T5 (longest path).
- **Lane:** own lane.

## Why (user story)

> **As an** owner
> **I want** to see all my short links, newest first and one page at a time, each with its short code, target URL, creation time and state (active or disabled)
> **So that** I know what exists and can find the link I want to act on
>
> — `spec.md §4, US-03, verbatim` · full text: [spec.md](../spec.md)

This task returns one stable newest-first page and the token for the next one.

## Inlined context

> **Chosen:** Option 1, monotonic UUIDv7 + keyset on `id`. The primary-key index alone serves both the order and the cursor.
>
> - One index (the primary key) serves the list query; pages are `WHERE id < :cursor ORDER BY id DESC LIMIT :n`.
> - `shared.IdGenerator` changes behaviour for all modules, so `IdGeneratorTest` gains monotonicity cases, including counter overflow within a millisecond (RFC 9562: advance the timestamp).
> - If the system clock steps backwards, the generator must still not go backwards: it keeps the last timestamp and increments.
>
> — `adr/0004-page-the-link-list-by-keyset-on-a-monotonic-uuidv7-id.md §Decision outcome + §Consequences, abridged` · full text: [adr/0004-page-the-link-list-by-keyset-on-a-monotonic-uuidv7-id.md](../adr/0004-page-the-link-list-by-keyset-on-a-monotonic-uuidv7-id.md)

> C->>S: asks for a page of short links (optional page size, optional continuation token)
> S->>D: read the next page of links, newest first, after the token if given (2 s budget)
> alt storage answers with one or more links: up to page-size links plus whether more follow
> else storage answers with no links: an empty list, not an error
> else storage does not answer within 2 s: "temporarily unavailable, try again later", nothing confirmed
>
> — `sad.md §6, «List short links», abridged` · full text: [sad.md](../sad.md)

**Fallback:** insufficient or contradicted by the code → read the named file in full
([spec.md](../spec.md) · [sad.md](../sad.md) · [data-model.md](../data-model.md) ·
[openapi.yaml](../contracts/openapi.yaml) · [adr/](../adr/)) and follow it. Do not guess.

## Data delta

| Column | Type | Constraints | Change |
|---|---|---|---|
| `id` | `UUID` | PK (`pk_short_link`) | read (order + cursor) |
| `short_code` | `TEXT COLLATE "C"` | NOT NULL, UNIQUE (`uq_short_link_short_code`), CHECK length = 7 | read |
| `target_url` | `TEXT` | NOT NULL, CHECK `char_length` 1..2048 | read |
| `state` | `TEXT` | NOT NULL, CHECK in (`ACTIVE`, `DISABLED`) | read |
| `created_at` | `TIMESTAMPTZ` | NOT NULL, set by the app from `Clock`, no DB default | read |

— `data-model.md §Entities, table short_link, abridged` · full text: [data-model.md](../data-model.md)

| Pattern | Statement shape |
|---|---|
| List page (AC-09, AC-10) | `SELECT … FROM short_link [WHERE id < :cursor] ORDER BY id DESC LIMIT :size + 1` (the extra row tells whether more follow) |

— `data-model.md §Entities, Access patterns, abridged` · full text: [data-model.md](../data-model.md)

## API contract

- `GET /api/v1/links?after=&limit=` → `200 ShortLinkPage {items[], hasNext, nextCursor}`; `nextCursor` is `null` when `hasNext` is false.
- `limit`: 1..100, default 20 → else `400 link.invalid_page_size`; `after`: a token this API issued (1..64 chars) → else `400 link.invalid_cursor`. Validation lives here (`PageCursor`, page-size check); HTTP mapping is T14.

— `contracts/openapi.yaml, operationId listShortLinks, abridged` · full text: [openapi.yaml](../contracts/openapi.yaml)

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

## Checklist

- [ ] `src/main/java/io/github/igorermolenko/trimly/link/app/PageCursor.java`: encode = the last link's id as its canonical UUID string; decode rejects anything that is not a UUIDv7 string → `InvalidCursor` signal.
- [ ] `src/main/java/io/github/igorermolenko/trimly/link/app/ListShortLinks.java`: validate size (default 20, 1..100 → else `InvalidPageSize`), `store.page(after, size)` via `StorageFailures`, trim the look-ahead row, set `hasNext` / `nextCursor`.
- [ ] Tests: `src/test/java/io/github/igorermolenko/trimly/link/app/PageCursorTest.java`, `src/test/java/io/github/igorermolenko/trimly/link/app/ListShortLinksIT.java` (fixtures with ids from the real monotonic generator).

## Edge cases

| Case | Behaviour |
|---|---|
| No links | Empty `items`, `hasNext=false`, `nextCursor=null` (AC-10). |
| Exactly `limit` links | `hasNext=false` (look-ahead row absent). |
| Links created in the same millisecond | Later-created first (monotonic id). |
| Link created while paging | Never appears on later pages, nothing skipped or duplicated (keyset). |
| `limit` 0 / 101 / `abc` | `InvalidPageSize`. |
| `after` = `xyz` or a non-v7 UUID | `InvalidCursor`. |
| Storage down | `LinkStorageUnavailable` (AC-17b, mapped in T14). |

## Definition of Done

- [ ] `ListShortLinksIT` passes: newest-first incl. same-ms ties, default 20 / max 100, concurrent-insert paging without duplicates or gaps, empty list.
- [ ] `PageCursorTest` passes for valid and garbled tokens.
- [ ] `ModularityTest` green.
- [ ] every Hard Rule inlined above still holds
- [ ] lint + vet clean (`./mvnw -B spotless:check`, `./mvnw -B verify`)
