---
id: T1
title: "Promote the staged short_link migration to V2__create_short_link.sql"
layer: "migration"
deps: []
blocks: ["T5"]
acs: ["AC-20"]
files_hint: ["docs/features/url-shortener/migrations/01_create_short_link.up.sql", "docs/features/url-shortener/migrations/01_create_short_link.down.sql", "src/main/resources/db/migration/"]
owner: "igorermolenko"
estimate: "S"
context_budget: "M"     # 41 inlined lines
status: "done"
step: "COMMIT"
---

<!-- To the executing agent: work from what is inlined here. If a slice is insufficient, ambiguous,
or contradicts the code in front of you, open the named file for the full text and follow that.
Do not invent the missing part. Every quote ends with its provenance signature; the source wins. -->

# T1 — Promote the staged short_link migration to V2__create_short_link.sql

## Place in the sequence

- **Blocked by:** nothing — starts in wave 1
- **Blocks:** T5 — Persist ShortLink through a Spring Data JDBC store with constraint-aware failure translation
- **Wave:** 1 — no dependencies.
- **Lane:** own lane.

## Why (user story)

> **As an** owner
> **I want** to switch a short link off
> **So that** a mistaken or abused link stops working for everyone at once
>
> — `spec.md §4, US-04, verbatim` · full text: [spec.md](../spec.md)

This task creates the table whose unique short-code constraint and no-delete lifecycle make a code impossible to reissue, so a re-enabled link always leads to its original target URL.

## Inlined context

> **Hard rule:** Flyway, plain SQL, forward-only, `src/main/resources/db/migration/V<n>__<snake_case_description>.sql`, applied on startup. Never edit an applied migration; add the next version.
>
> — `CLAUDE.md §Conventions, Migrations bullet, verbatim` · full text: [CLAUDE.md](../../../../CLAUDE.md)

> Insert once, then update only `state` and `updated_at`. `short_code`, `target_url` and `created_at` never change (AC-20, spec §3). Rows are never deleted, so the unique constraint keeps a code from ever being reissued (ADR-0002).
>
> — `data-model.md §Entities, short_link Lifecycle, verbatim` · full text: [data-model.md](../data-model.md)

> Flyway in this repo is forward-only (repo ADR-0003): this file is NOT promoted into src/main/resources/db/migration/. It is a manual rollback script, run by hand before the table holds real links.
>
> — `data-model.md migrations/01_create_short_link.down.sql header, abridged` · full text: [data-model.md](../data-model.md)

**Fallback:** insufficient or contradicted by the code → read the named file in full
([spec.md](../spec.md) · [sad.md](../sad.md) · [data-model.md](../data-model.md) ·
[openapi.yaml](../contracts/openapi.yaml) · [adr/](../adr/)) and follow it. Do not guess.

## Data delta

| Column | Type | Constraints | Change |
|---|---|---|---|
| `id` | `UUID` | PK (`pk_short_link`) | added |
| `short_code` | `TEXT COLLATE "C"` | NOT NULL, UNIQUE (`uq_short_link_short_code`), CHECK length = 7 | added |
| `target_url` | `TEXT` | NOT NULL, CHECK `char_length` 1..2048 | added |
| `state` | `TEXT` | NOT NULL, CHECK in (`ACTIVE`, `DISABLED`) | added |
| `created_at` | `TIMESTAMPTZ` | NOT NULL, set by the app from `Clock`, no DB default | added |
| `updated_at` | `TIMESTAMPTZ` | NOT NULL, last real state change, from `Clock` | added |

— `data-model.md §Entities, table short_link, abridged` · full text: [data-model.md](../data-model.md)

Constraints: `pk_short_link`, `uq_short_link_short_code`, `ck_short_link_short_code_length`, `ck_short_link_target_url_length`, `ck_short_link_state`. No extra indexes.

**Staged pair:** `docs/features/url-shortener/migrations/01_create_short_link.up.sql` → promoted as `src/main/resources/db/migration/V2__create_short_link.sql` (V1 is `V1__baseline.sql`). `01_create_short_link.down.sql` stays staged — a manual rollback, never promoted.

## API contract

Internal — no API surface.

## Acceptance criteria

### AC-20 — domain invariant

> **Given** a short code has been issued, whether its link is now active or disabled
> **When** the owner creates any number of new short links
> **Then** none of them is ever given that short code, and re-enabling that code's link still leads to its original target URL ("a short code never points to a different target URL")
>
> — `spec.md §5, AC-20, verbatim` · full text: [spec.md](../spec.md)

## Checklist

- [ ] Copy `01_create_short_link.up.sql` verbatim to `src/main/resources/db/migration/V2__create_short_link.sql` (drop only the «STAGED» comment line).
- [ ] Do not promote the `.down.sql` (forward-only Flyway).
- [ ] Add `src/test/java/io/github/igorermolenko/trimly/link/infra/ShortLinkMigrationIT.java` (imports `TestcontainersConfiguration`): asserts the table, the five named constraints, that a duplicate `short_code` is rejected by `uq_short_link_short_code`, and that `'abc2345' <> 'ABC2345'` are both insertable (COLLATE "C", case matters).

## Edge cases

| Case | Behaviour |
|---|---|
| Duplicate `short_code` insert | Rejected with a unique violation naming `uq_short_link_short_code` (ADR-0002 relies on the name). |
| Codes differing only in case | Both rows accepted — `COLLATE "C"` compares byte-wise (AC-01 «letter case matters»). |
| `state` other than ACTIVE/DISABLED, `target_url` empty or > 2048 | CHECK violation — a bug signal, domain validates first. |
| Down script | Run by hand only; dropping the table destroys every issued code (AC-20). |

## Definition of Done

- [ ] `V2__create_short_link.sql` applies on a fresh Testcontainers Postgres at startup (`ApplicationBootIT` still green).
- [ ] `ShortLinkMigrationIT` passes: constraints present by name, duplicate code rejected, case-distinct codes accepted.
- [ ] The staged `.down.sql` drops the table cleanly when run by hand against the IT database (manual check, noted in the PR).
- [ ] every Hard Rule inlined above still holds
- [ ] lint + vet clean (`./mvnw -B spotless:check`, `./mvnw -B verify`)
