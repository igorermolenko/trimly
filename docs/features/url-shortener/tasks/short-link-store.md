---
id: T5
title: "Persist ShortLink through a Spring Data JDBC store with constraint-aware failure translation"
layer: "infra"
deps: ["T1", "T3"]
blocks: ["T6", "T7", "T8", "T9", "T10"]
acs: ["AC-20b", "AC-17b"]
files_hint: ["src/main/java/io/github/igorermolenko/trimly/link/domain/ShortLink.java", "src/main/java/io/github/igorermolenko/trimly/link/domain/LinkState.java", "src/main/java/io/github/igorermolenko/trimly/link/domain/LinkStorageUnavailable.java", "src/main/java/io/github/igorermolenko/trimly/link/domain/ShortCodeTaken.java", "src/main/java/io/github/igorermolenko/trimly/link/infra/", "src/main/java/io/github/igorermolenko/trimly/link/app/StorageFailures.java", "src/test/java/io/github/igorermolenko/trimly/link/ShortLinkFixtures.java", "src/test/java/io/github/igorermolenko/trimly/link/infra/"]
owner: "igorermolenko"
estimate: "M"
context_budget: "M"     # 70 inlined lines
status: "todo"
step: ""
---

<!-- To the executing agent: work from what is inlined here. If a slice is insufficient, ambiguous,
or contradicts the code in front of you, open the named file for the full text and follow that.
Do not invent the missing part. Every quote ends with its provenance signature; the source wins. -->

# T5 — Persist ShortLink through a Spring Data JDBC store with constraint-aware failure translation

## Place in the sequence

- **Blocked by:** T1 — Promote the staged short_link migration to V2__create_short_link.sql · T3 — Add ShortCode (alphabet, length, well-formedness) and the SecureRandom ShortCodeGenerator
- **Blocks:** T6 — Bound every storage call to 2 s, drain the idle pool and keep health checks off the DB · T7 — Implement the CreateShortLink use case with 5-attempt insert-retry and public-base-url · T8 — Implement ListShortLinks with keyset paging and an opaque continuation cursor · T9 — Implement DisableShortLink and EnableShortLink as one idempotent state change · T10 — Publish link.app.LinkLookup returning a sealed Resolution (Found / NotFound / Unavailable)
- **Wave:** 2 — after T1, T3 (longest path).
- **Lane:** shares files with T6 (`files_hint` overlap) — serialized by `implement`.

## Why (user story)

> **As an** owner
> **I want** to submit a target URL and get back a new short link
> **So that** I can share a clean link that is easy to type by hand
>
> — `spec.md §4, US-01, verbatim` · full text: [spec.md](../spec.md)

This task gives the use cases one storage port with the four data-model access patterns and turns storage failures into explicit domain signals.

## Inlined context

```text
link/                        write path + the published lookup
├── api/       owner controllers (create, list, disable, re-enable) + DTOs; translates domain
│              failures into ErrorResponseException so the shared advice renders Problem Details
├── app/       use cases (CreateShortLink, ListShortLinks, DisableShortLink, EnableShortLink),
│              LinkLookup (published) + its sealed Resolution result, storage-failure translation
├── domain/    ShortLink, ShortCode (alphabet, length, well-formedness), ShortCodeGenerator
│              (SecureRandom), TargetUrl rules (AC-03…AC-06), LinkState
└── infra/     Spring Data JDBC repository + row mapping; unique-violation detection (ADR-0002)
```

— `sad.md §5, internal decomposition (link), verbatim` · full text: [sad.md](../sad.md)

> Each owner write is a single SQL statement, with no multi-statement transactions. Create retries run as separate statements (a failed INSERT aborts a PostgreSQL transaction). Disable and re-enable are one idempotent `UPDATE … SET state`, so repeating one is harmless (AC-14) and the last write wins. No optimistic locking with a single owner. Every call has the 2 s budget
>
> — `sad.md §8, Storage access row, verbatim` · full text: [sad.md](../sad.md)

> **Chosen:** Option 1, unique index + insert-retry. The database itself guarantees uniqueness, with no race window between a check and the insert. The common case costs one round trip.
>
> - The create use case must recognise the unique violation on `short_code` specifically (by constraint name). It must not treat other errors that way, such as a storage timeout, which must become "temporarily unavailable" (ADR-0003).
> - Each retry is a separate statement. A failed INSERT aborts a PostgreSQL transaction, so retries must not share one transaction.
> - The code generator is injectable, so tests can force collisions deterministically (AC-20b).
>
> — `adr/0002-generate-short-codes-with-securerandom-and-enforce-uniqueness-with-a-unique-index-and-insert-retry.md §Decision outcome + §Consequences, abridged` · full text: [adr/0002-generate-short-codes-with-securerandom-and-enforce-uniqueness-with-a-unique-index-and-insert-retry.md](../adr/0002-generate-short-codes-with-securerandom-and-enforce-uniqueness-with-a-unique-index-and-insert-retry.md)

> Timeouts surface as Spring `DataAccessException` subtypes. `link`'s app layer translates them into an explicit "storage unavailable" outcome (sad.md §8 error handling).
>
> — `adr/0003-bound-every-link-storage-call-within-2-seconds-using-pool-and-driver-timeouts.md §Consequences (Positive), verbatim` · full text: [adr/0003-bound-every-link-storage-call-within-2-seconds-using-pool-and-driver-timeouts.md](../adr/0003-bound-every-link-storage-call-within-2-seconds-using-pool-and-driver-timeouts.md)

> **Hard rule:** Spring Data JDBC repositories, only in a module's `infra` layer. No JPA/Hibernate.
>
> — `CLAUDE.md §Conventions, Persistence bullet, verbatim` · full text: [CLAUDE.md](../../../../CLAUDE.md)

**Fallback:** insufficient or contradicted by the code → read the named file in full
([spec.md](../spec.md) · [sad.md](../sad.md) · [data-model.md](../data-model.md) ·
[openapi.yaml](../contracts/openapi.yaml) · [adr/](../adr/)) and follow it. Do not guess.

## Data delta

| Column | Type | Constraints | Change |
|---|---|---|---|
| `id` | `UUID` | PK (`pk_short_link`) | written on insert |
| `short_code` | `TEXT COLLATE "C"` | NOT NULL, UNIQUE (`uq_short_link_short_code`), CHECK length = 7 | written on insert, read by code |
| `target_url` | `TEXT` | NOT NULL, CHECK `char_length` 1..2048 | written on insert |
| `state` | `TEXT` | NOT NULL, CHECK in (`ACTIVE`, `DISABLED`) | written; updated by disable/enable |
| `created_at` | `TIMESTAMPTZ` | NOT NULL, set by the app from `Clock`, no DB default | written on insert |
| `updated_at` | `TIMESTAMPTZ` | NOT NULL, last real state change, from `Clock` | written; changed only on a real state change |

— `data-model.md §Entities, table short_link, abridged` · full text: [data-model.md](../data-model.md)

| Pattern | Statement shape |
|---|---|
| Create (AC-01, AC-02, AC-20b) | `INSERT INTO short_link (id, short_code, target_url, state, created_at, updated_at) VALUES (…, 'ACTIVE', :now, :now)`. On a violation of `uq_short_link_short_code` (matched by constraint name), regenerate the code and retry, up to 5 attempts, each its own statement |
| Follow (AC-07, AC-12, AC-16, AC-17) | `SELECT target_url, state FROM short_link WHERE short_code = :code` (a disabled link maps to `NotFound` in `link.app`) |
| List page (AC-09, AC-10) | `SELECT … FROM short_link [WHERE id < :cursor] ORDER BY id DESC LIMIT :size + 1` (the extra row tells whether more follow) |
| Disable / re-enable (AC-11, AC-13, AC-14, AC-15) | `UPDATE short_link SET updated_at = CASE WHEN state <> :state THEN :now ELSE updated_at END, state = :state WHERE short_code = :code RETURNING short_code, state`. No row returned → "link not found" (AC-13). A repeat matches the row but changes nothing, so it confirms the current state (AC-14) |

— `data-model.md §Entities, Access patterns, abridged` · full text: [data-model.md](../data-model.md)

## API contract

Internal — no API surface.

## Acceptance criteria

### AC-20b — domain invariant

> **Given** the generator produces a short code that is already taken
> **When** the owner creates a short link
> **Then** the owner still gets a new short link with a different, unused short code, not an error. trimly tries up to 5 freshly generated codes. If all 5 are taken (practically impossible with 56⁷ ≈ 1.7 × 10¹² codes), the owner is told "temporarily unavailable, try again later", as in AC-17b, and nothing is created
>
> — `spec.md §5, AC-20b, verbatim` · full text: [spec.md](../spec.md)

### AC-17b — error

> **Given** the owner presents the owner credential and the link storage has not answered within 2 seconds
> **When** the owner creates, lists, disables or re-enables short links
> **Then** the owner is told "temporarily unavailable, try again later" and nothing is confirmed; if an unconfirmed creation was in fact recorded, retrying it creates a second, separate short link (AC-02), which is accepted: the owner sees both in the list and can disable the extra one
>
> — `spec.md §5, AC-17b, verbatim` · full text: [spec.md](../spec.md)

## Checklist

- [ ] `src/main/java/io/github/igorermolenko/trimly/link/domain/`: `LinkState` enum (`ACTIVE`, `DISABLED`), `ShortLink` record (id, shortCode, targetUrl, state, createdAt, updatedAt) — no Spring imports; signals `ShortCodeTaken` and `LinkStorageUnavailable` (runtime exceptions).
- [ ] `src/main/java/io/github/igorermolenko/trimly/link/infra/`: row type + Spring Data JDBC repository (`@Query` for the keyset page and the `UPDATE … RETURNING`) behind a `ShortLinkStore` class with `insert(ShortLink)`, `findByShortCode(String)`, `page(UUID afterOrNull, int size)` (fetches `size + 1`), `setState(String code, LinkState, Instant now)` → `Optional<(code, state)>`.
- [ ] Insert: a `DuplicateKeyException` whose cause names `uq_short_link_short_code` → `ShortCodeTaken`; any other `DataIntegrityViolationException` is rethrown as-is (bug).
- [ ] `src/main/java/io/github/igorermolenko/trimly/link/app/StorageFailures.java`: one helper that wraps a storage call and maps every other `DataAccessException` (incl. `QueryTimeoutException`, `CannotGetJdbcConnectionException`, `TransientDataAccessResourceException`) to `LinkStorageUnavailable`. Use cases (T7–T10) call the store through it.
- [ ] `src/test/java/io/github/igorermolenko/trimly/link/ShortLinkFixtures.java` per data-model §Test fixtures (`aShortLink()`, `.disabled()`, `.withShortCode()`, `.withTargetUrl()`, `.createdAt()`; `example.test` hosts only).
- [ ] `src/test/java/io/github/igorermolenko/trimly/link/infra/ShortLinkStoreIT.java` (Testcontainers): every access pattern round-trips; duplicate code → `ShortCodeTaken`.

## Edge cases

| Case | Behaviour |
|---|---|
| Insert with a taken code | `ShortCodeTaken` — never `LinkStorageUnavailable` (ADR-0002). |
| Other constraint violation (CHECK) | Propagates unchanged — a bug, not a user error. |
| Connection refused / query timeout | `LinkStorageUnavailable` via `StorageFailures`. |
| `setState` to the state it already has | Row returned with current state; `updated_at` unchanged (AC-14). |
| `setState` for an unknown code | Empty result (→ AC-13 in T9). |
| `page` on an empty table | Empty list, no error (AC-10). |
| Retries | Never share a transaction — no `@Transactional` around the insert loop. |

## Definition of Done

- [ ] `ShortLinkStoreIT` passes for insert, find-by-code, keyset page (incl. `size + 1` look-ahead) and state update (changed / unchanged / unknown).
- [ ] Unit test of `StorageFailures` shows timeout/connection exceptions → `LinkStorageUnavailable` and a `uq_short_link_short_code` violation → `ShortCodeTaken`.
- [ ] No JPA; repository only in `link.infra`; `ModularityTest` green.
- [ ] every Hard Rule inlined above still holds
- [ ] lint + vet clean (`./mvnw -B spotless:check`, `./mvnw -B verify`)
