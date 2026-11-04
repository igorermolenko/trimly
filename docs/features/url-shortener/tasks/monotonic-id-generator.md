---
id: T2
title: "Make shared.IdGenerator monotonic within a millisecond (RFC 9562 method 1)"
layer: "domain"
deps: []
blocks: ["T7", "T8"]
acs: ["AC-09"]
files_hint: ["src/main/java/io/github/igorermolenko/trimly/shared/IdGenerator.java", "src/test/java/io/github/igorermolenko/trimly/shared/IdGeneratorTest.java"]
owner: "igorermolenko"
estimate: "S"
context_budget: "S"     # 38 inlined lines
status: "done"
step: "COMMIT"
---

<!-- To the executing agent: work from what is inlined here. If a slice is insufficient, ambiguous,
or contradicts the code in front of you, open the named file for the full text and follow that.
Do not invent the missing part. Every quote ends with its provenance signature; the source wins. -->

# T2 — Make shared.IdGenerator monotonic within a millisecond (RFC 9562 method 1)

## Place in the sequence

- **Blocked by:** nothing — starts in wave 1
- **Blocks:** T7 — Implement the CreateShortLink use case with 5-attempt insert-retry and public-base-url · T8 — Implement ListShortLinks with keyset paging and an opaque continuation cursor
- **Wave:** 1 — no dependencies.
- **Lane:** own lane.

## Why (user story)

> **As an** owner
> **I want** to see all my short links, newest first and one page at a time, each with its short code, target URL, creation time and state (active or disabled)
> **So that** I know what exists and can find the link I want to act on
>
> — `spec.md §4, US-03, verbatim` · full text: [spec.md](../spec.md)

This task gives every link an id that sorts newest-first with a correct tie-break, which the keyset list (T8) pages on.

## Inlined context

> **Chosen:** Option 1, monotonic UUIDv7 + keyset on `id`. The primary-key index alone serves both the order and the cursor.
>
> - One index (the primary key) serves the list query; pages are `WHERE id < :cursor ORDER BY id DESC LIMIT :n`.
> - `shared.IdGenerator` changes behaviour for all modules, so `IdGeneratorTest` gains monotonicity cases, including counter overflow within a millisecond (RFC 9562: advance the timestamp).
> - If the system clock steps backwards, the generator must still not go backwards: it keeps the last timestamp and increments.
>
> — `adr/0004-page-the-link-list-by-keyset-on-a-monotonic-uuidv7-id.md §Decision outcome + §Consequences, abridged` · full text: [adr/0004-page-the-link-list-by-keyset-on-a-monotonic-uuidv7-id.md](../adr/0004-page-the-link-list-by-keyset-on-a-monotonic-uuidv7-id.md)

> `shared.IdGenerator` becomes monotonic within a millisecond (RFC 9562 §6.2 method 1: a counter in `rand_a`). The list is ordered `id DESC`, and the continuation cursor is the last link's id [...] The ordering guarantee holds per process, which is enough while production runs exactly one instance (§2).
>
> — `sad.md §4, choice 4, abridged` · full text: [sad.md](../sad.md)

> UUIDv7 primary keys from `shared.IdGenerator`, now monotonic within a millisecond. The short code is a separate public field and the id is never shown publicly. The creation time comes from the same `Clock` instant as the id
>
> — `sad.md §8, ID strategy row, verbatim` · full text: [sad.md](../sad.md)

> **Hard rule:** Only the injected `java.time.Clock` (`shared.ClockConfiguration`), with no direct `now()`
>
> — `sad.md §8, Time row, verbatim` · full text: [sad.md](../sad.md)

**Fallback:** insufficient or contradicted by the code → read the named file in full
([spec.md](../spec.md) · [sad.md](../sad.md) · [data-model.md](../data-model.md) ·
[openapi.yaml](../contracts/openapi.yaml) · [adr/](../adr/)) and follow it. Do not guess.

## Data delta

No DB changes.

## API contract

Internal — no API surface.

## Acceptance criteria

### AC-09 — happy path

> **Given** the owner has several short links, some active and some disabled
> **When** the owner lists their short links
> **Then** the owner sees each link's short code, target URL, creation time and state, newest first (links created at the same moment: the later-created one first), one page at a time: 20 links per page by default, up to 100 if the owner asks. Each page offers a way to continue right after its last link, so links created while the owner is paging never make a link appear twice or get skipped
>
> — `spec.md §5, AC-09, verbatim` · full text: [spec.md](../spec.md)

## Checklist

- [ ] `src/main/java/io/github/igorermolenko/trimly/shared/IdGenerator.java`: keep `lastMillis` + a 12-bit counter in `rand_a`; same or earlier millisecond → counter+1 (seeded randomly on a new millisecond); counter overflow → advance the timestamp by 1 ms; never go backwards when the `Clock` steps back. Make `newId` thread-safe (`synchronized` is fine at one instance).
- [ ] Add `UUID newId(Instant at)` (existing `newId()` delegates with `clock.instant()`), so callers can stamp `created_at` from the same instant (sad §8 ID strategy). Additive — no existing caller breaks.
- [ ] `src/test/java/io/github/igorermolenko/trimly/shared/IdGeneratorTest.java`: add cases with a fixed/mutable test `Clock`.

## Edge cases

| Case | Behaviour |
|---|---|
| Many ids in the same millisecond | Strictly increasing (`compareTo` on the UUID as unsigned 128-bit / string order). |
| Counter overflow (> 4096 ids in one ms) | Timestamp advances by 1 ms; order still strictly increasing. |
| Clock steps backwards | Generator keeps the last timestamp and increments; never returns a smaller id. |
| Version / variant bits | Still `7` and `10xx` (RFC 9562). |
| Second instance | Out of scope — ordering is per process (sad §11, accepted debt). |

## Definition of Done

- [ ] `IdGeneratorTest` covers same-ms ordering, overflow and backwards clock, and passes.
- [ ] Existing callers compile unchanged; `ModularityTest` green.
- [ ] `./mvnw -B spotless:check` clean.
- [ ] every Hard Rule inlined above still holds
- [ ] lint + vet clean (`./mvnw -B spotless:check`, `./mvnw -B verify`)
