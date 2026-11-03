# Audit — data-model url-shortener — 2026-11-03

**Mode:** greenfield (the live schema holds only `V1__baseline.sql`, a no-op). **Drift:** none possible. The `link` module has no domain or infra code yet (`package-info.java` only), so there is nothing to map fields against.

> **The migrations are staged, not yet in the live `src/main/resources/db/migration/` tree. `implement` promotes them.**

## Staged migrations

| Staged file | Promote as | Notes |
|---|---|---|
| `docs/features/url-shortener/migrations/01_create_short_link.up.sql` | `src/main/resources/db/migration/V<n>__create_short_link.sql` | Flyway, plain SQL. |
| `docs/features/url-shortener/migrations/01_create_short_link.down.sql` | **not promoted** | Manual rollback script only (see the deviations below). |

**Promote-time hint:** Flyway versioned, `V<n>__<snake_case_description>.sql` (architecture-map §Conventions, repo ADR-0003). The next version is currently ≈ `V2` (only `V1__baseline.sql` exists). `implement` assigns the real number at promotion, because another feature may promote first. sad.md §5 already names the file `V2__…sql`.

## Conventions followed (source)

- PK `UUID`, app-generated UUIDv7 via `shared.IdGenerator` (repo ADR-0003, feature ADR-0004).
- Unique `short_code` + insert-retry matched by constraint name `uq_short_link_short_code` (feature ADR-0002).
- Keyset list on `id DESC` served by the PK alone, with no extra index (feature ADR-0004).
- No DB `DEFAULT now()`. Times come from the injected `Clock` (CLAUDE.md §Time).
- Hard schema, no deletes: rows are never deleted (spec §3, ADR-0002).

## Greenfield choices confirmed with the owner (2026-11-03)

| Topic | Choice |
|---|---|
| State | `state TEXT` + `CHECK IN ('ACTIVE','DISABLED')` |
| Constraints | A length/state CHECK backstop. **No alphabet regex**, so the alphabet stays in `link.domain` only (sad.md §4.2) |
| Audit columns | `created_at` **+ `updated_at`** (the owner chose this over the created_at-only recommendation) |
| `target_url` type | `TEXT` + `CHECK char_length BETWEEN 1 AND 2048` |

## Deviations and new conventions (flagged)

1. **`.down.sql` vs forward-only Flyway.** The repo is forward-only (repo ADR-0003, CLAUDE.md), so the `.down.sql` file is a hand-run rollback script that is **never** promoted into the Flyway tree. A real rollback after release is a new forward migration.
2. **`updated_at` and the sad.md §8 "one idempotent `UPDATE … SET state`".** It is still one statement, but it now uses `SET updated_at = CASE WHEN state <> :state THEN :now ELSE updated_at END, state = :state … RETURNING`, so a repeat changes nothing (AC-14). Verified on Postgres 17. `implement` must use this shape (a plain `SET updated_at = :now` would make a repeat bump the timestamp, which breaks "nothing changes").
3. **New conventions this migration introduces** (the repo had no precedent): explicit constraint names with `pk_` / `uq_` / `ck_` prefixes (the `uq_` name is load-bearing for ADR-0002), and `COLLATE "C"` on `short_code` for case-sensitive, locale-independent equality.

## Self-check (4 mandatory)

| Check | Result |
|---|---|
| Naming matches the repo convention | PASS: snake_case singular table `short_link`, Flyway naming at promotion |
| Down reversibility | PASS: `DROP TABLE IF EXISTS short_link` drops the table, the PK, the UNIQUE and the CHECKs (verified: the table is gone, and running down twice is a no-op) |
| FK indexes | PASS (N/A): no foreign keys |
| Convention adherence | PASS with deviations 1–3 flagged above |

**Verified on a throwaway `postgres:17`:** up ×2 (idempotent), case-sensitive codes coexist, a repeat disable keeps `updated_at`, an unknown code returns 0 rows, the list uses `Index Only Scan Backward using pk_short_link`, lookup by code uses `uq_short_link_short_code`, all four constraint names fire on bad rows, down ×2 (idempotent).

## Breaking-change decompositions

None. This is a new table.

## TBDs

None in `data-model.md`. Carried forward: roadmap step 4 (invited users) will need an owner reference on `short_link`, added as an expand migration (nullable column → backfill to the owner → NOT NULL).

## Next

`/sdd:api url-shortener`.
