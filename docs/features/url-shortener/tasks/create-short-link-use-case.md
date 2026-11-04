---
id: T7
title: "Implement the CreateShortLink use case with 5-attempt insert-retry and public-base-url"
layer: "app"
deps: ["T2", "T4", "T5"]
blocks: ["T13"]
acs: ["AC-01", "AC-02", "AC-20", "AC-20b", "AC-17b"]
files_hint: ["src/main/java/io/github/igorermolenko/trimly/link/app/CreateShortLink.java", "src/main/java/io/github/igorermolenko/trimly/link/app/LinkProperties.java", "src/main/java/io/github/igorermolenko/trimly/link/app/LinkConfiguration.java", "src/main/resources/application.yml", "src/test/java/io/github/igorermolenko/trimly/link/app/CreateShortLinkTest.java", "src/test/java/io/github/igorermolenko/trimly/link/app/CreateShortLinkIT.java"]
owner: "igorermolenko"
estimate: "M"
context_budget: "M"     # 95 inlined lines
status: "done"
step: "COMMIT"
---

<!-- To the executing agent: work from what is inlined here. If a slice is insufficient, ambiguous,
or contradicts the code in front of you, open the named file for the full text and follow that.
Do not invent the missing part. Every quote ends with its provenance signature; the source wins. -->

# T7 — Implement the CreateShortLink use case with 5-attempt insert-retry and public-base-url

## Place in the sequence

- **Blocked by:** T2 — Make shared.IdGenerator monotonic within a millisecond (RFC 9562 method 1) · T4 — Add TargetUrl validation rules (scheme, length, encoding, own hosts) in link.domain · T5 — Persist ShortLink through a Spring Data JDBC store with constraint-aware failure translation
- **Blocks:** T13 — Expose POST /api/v1/links and the Problem Details `code` mapping for owner errors
- **Wave:** 3 — after T2, T4, T5 (longest path).
- **Lane:** shares files with T6 (`files_hint` overlap) — serialized by `implement`.

## Why (user story)

> **As an** owner
> **I want** to submit a target URL and get back a new short link
> **So that** I can share a clean link that is easy to type by hand
>
> — `spec.md §4, US-01, verbatim` · full text: [spec.md](../spec.md)

This task turns a validated target URL into a stored active link with a fresh code and the full short URL.

## Inlined context

```text
sequenceDiagram
    participant link as link module
    participant db as Link storage
        alt target URL breaks a rule (scheme, length, encoding, points back to trimly)
            link-->>owner: Problem Details explaining the rule, nothing created
        else target URL accepted
            loop up to 5 freshly generated short codes
                link->>db: insert the link with a new random short code (2 s budget)
                db-->>link: stored, or the short code is already taken
            end
            alt stored
                link-->>owner: full short link (from the configured public address), short code, target URL
            else 5 codes taken, or storage does not answer within 2 s
                link-->>owner: Problem Details "temporarily unavailable, try again later"
            end
        end
```

— `sad.md §6, Critical flow 2 «create a short link», link-side branches, abridged` · full text: [sad.md](../sad.md)

> **Chosen:** Option 1, unique index + insert-retry. The database itself guarantees uniqueness, with no race window between a check and the insert. The common case costs one round trip.
>
> - The create use case must recognise the unique violation on `short_code` specifically (by constraint name). It must not treat other errors that way, such as a storage timeout, which must become "temporarily unavailable" (ADR-0003).
> - Each retry is a separate statement. A failed INSERT aborts a PostgreSQL transaction, so retries must not share one transaction.
> - The code generator is injectable, so tests can force collisions deterministically (AC-20b).
>
> — `adr/0002-generate-short-codes-with-securerandom-and-enforce-uniqueness-with-a-unique-index-and-insert-retry.md §Decision outcome + §Consequences, abridged` · full text: [adr/0002-generate-short-codes-with-securerandom-and-enforce-uniqueness-with-a-unique-index-and-insert-retry.md](../adr/0002-generate-short-codes-with-securerandom-and-enforce-uniqueness-with-a-unique-index-and-insert-retry.md)

> `trimly.public-base-url`: the single configured public address that every full short link is built from, whatever host the request arrived on (AC-01). `trimly.own-hosts`: trimly's own hosts, used to refuse redirect loops (AC-06); the actual domain is roadmap decision D2. The owner credential comes from an env var only. The datasource comes from `SPRING_DATASOURCE_*`. Everything is validated at startup, failing fast. `application.yml` holds non-secret defaults only
>
> — `sad.md §8, Configuration row, verbatim` · full text: [sad.md](../sad.md)

> Each owner write is a single SQL statement, with no multi-statement transactions. Create retries run as separate statements (a failed INSERT aborts a PostgreSQL transaction). Disable and re-enable are one idempotent `UPDATE … SET state`, so repeating one is harmless (AC-14) and the last write wins. No optimistic locking with a single owner. Every call has the 2 s budget
>
> — `sad.md §8, Storage access row, verbatim` · full text: [sad.md](../sad.md)

> **Hard rule:** Only the injected `java.time.Clock` (`shared.ClockConfiguration`), with no direct `now()`
>
> — `sad.md §8, Time row, verbatim` · full text: [sad.md](../sad.md)

**Fallback:** insufficient or contradicted by the code → read the named file in full
([spec.md](../spec.md) · [sad.md](../sad.md) · [data-model.md](../data-model.md) ·
[openapi.yaml](../contracts/openapi.yaml) · [adr/](../adr/)) and follow it. Do not guess.

## Data delta

| Column | Type | Constraints | Change |
|---|---|---|---|
| `id` | `UUID` | PK (`pk_short_link`) | written (IdGenerator) |
| `short_code` | `TEXT COLLATE "C"` | NOT NULL, UNIQUE (`uq_short_link_short_code`), CHECK length = 7 | written (generator) |
| `target_url` | `TEXT` | NOT NULL, CHECK `char_length` 1..2048 | written as submitted |
| `state` | `TEXT` | NOT NULL, CHECK in (`ACTIVE`, `DISABLED`) | written `ACTIVE` |
| `created_at` | `TIMESTAMPTZ` | NOT NULL, set by the app from `Clock`, no DB default | written (same instant as id) |
| `updated_at` | `TIMESTAMPTZ` | NOT NULL, last real state change, from `Clock` | written = created_at |

— `data-model.md §Entities, table short_link, abridged` · full text: [data-model.md](../data-model.md)

| Pattern | Statement shape |
|---|---|
| Create (AC-01, AC-02, AC-20b) | `INSERT INTO short_link (id, short_code, target_url, state, created_at, updated_at) VALUES (…, 'ACTIVE', :now, :now)`. On a violation of `uq_short_link_short_code` (matched by constraint name), regenerate the code and retry, up to 5 attempts, each its own statement |

— `data-model.md §Entities, Access patterns, abridged` · full text: [data-model.md](../data-model.md)

## API contract

- Returns what `201 ShortLink` needs: `shortCode`, `shortUrl` (= `trimly.public-base-url` + "/" + shortCode), `targetUrl`, `state: ACTIVE`, `createdAt`. HTTP mapping is T13.
- Signals for T13: `TargetUrlRejected(reason)` → 400; `LinkStorageUnavailable` (incl. 5 codes taken) → `503 link.temporarily_unavailable`.

— `contracts/openapi.yaml, operationId createShortLink, abridged` · full text: [openapi.yaml](../contracts/openapi.yaml)

## Acceptance criteria

### AC-01 — happy path

> **Given** the owner presents the owner credential
> **When** the owner submits an absolute web address using http or https, at most 2048 characters long, as the target URL
> **Then** the system records a new active short link with a freshly generated 7-character short code drawn from the short-code alphabet (A–Z, a–z and 2–9 with exactly these six look-alike characters removed: 0, O, o, 1, l, I; 56 characters in all), where letter case matters and each character is chosen by a cryptographically secure random source, so the code cannot be predicted from earlier codes. The target URL is stored and later redirected to exactly as submitted, with no normalisation. The owner gets back the full short link, its short code and its target URL. The full short link is always built from trimly's single configured public address, whatever address the request arrived on
>
> — `spec.md §5, AC-01, verbatim` · full text: [spec.md](../spec.md)

### AC-02 — domain invariant

> **Given** the owner already has a short link for target URL X
> **When** the owner submits target URL X again
> **Then** the system creates a separate short link with a different short code, and the earlier short link stays unchanged ("every creation issues a new short link")
>
> — `spec.md §5, AC-02, verbatim` · full text: [spec.md](../spec.md)

### AC-20 — domain invariant

> **Given** a short code has been issued, whether its link is now active or disabled
> **When** the owner creates any number of new short links
> **Then** none of them is ever given that short code, and re-enabling that code's link still leads to its original target URL ("a short code never points to a different target URL")
>
> — `spec.md §5, AC-20, verbatim` · full text: [spec.md](../spec.md)

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

- [ ] `src/main/java/io/github/igorermolenko/trimly/link/app/LinkProperties.java`: `@ConfigurationProperties("trimly")` + `@Validated`: `publicBaseUrl` (absolute http/https URI, no trailing slash, required) and `ownHosts` (non-empty set). Fail fast at startup.
- [ ] `src/main/java/io/github/igorermolenko/trimly/link/app/LinkConfiguration.java`: beans for `ShortCodeGenerator` (with `SecureRandom`).
- [ ] `src/main/resources/application.yml`: non-secret local defaults `trimly.public-base-url: http://localhost:8080`, `trimly.own-hosts: [localhost]`.
- [ ] `src/main/java/io/github/igorermolenko/trimly/link/app/CreateShortLink.java`: `TargetUrl.parse` first (nothing stored on reject); then `Instant now = clock.instant()`, `id = idGenerator.newId(now)`; up to 5 attempts, each a fresh code + its own `insert` via `StorageFailures`; `ShortCodeTaken` → next attempt; 5 × taken → `LinkStorageUnavailable`. No transaction around the loop.
- [ ] Tests: `src/test/java/io/github/igorermolenko/trimly/link/app/CreateShortLinkTest.java` (stub generator forces collisions), `src/test/java/io/github/igorermolenko/trimly/link/app/CreateShortLinkIT.java` (real store).

## Edge cases

| Case | Behaviour |
|---|---|
| Same target URL twice | Two links, two different codes; first unchanged (AC-02). |
| Generator returns 1–4 taken codes, then a free one | Created with the free code, no error (AC-20b). |
| Generator returns 5 taken codes | `LinkStorageUnavailable`; nothing created (AC-20b). |
| Storage timeout on an attempt | `LinkStorageUnavailable` immediately — not treated as «taken» (ADR-0002). |
| Request arrived on another host | `shortUrl` still built from `trimly.public-base-url` (AC-01). |
| Missing `trimly.public-base-url` / `own-hosts` | Application refuses to start. |

## Definition of Done

- [ ] `CreateShortLinkTest` passes for AC-02 and every AC-20b branch with a stub generator.
- [ ] `CreateShortLinkIT` shows a created link is stored ACTIVE with `target_url` byte-identical to the input and `created_at` = `updated_at`.
- [ ] Startup fails with a missing `trimly.public-base-url` (context test); `ModularityTest` green.
- [ ] every Hard Rule inlined above still holds
- [ ] lint + vet clean (`./mvnw -B spotless:check`, `./mvnw -B verify`)
