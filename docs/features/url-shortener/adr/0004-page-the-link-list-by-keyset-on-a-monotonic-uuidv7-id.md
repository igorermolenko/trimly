---
status: Accepted
owner: "igorermolenko"
reviewers: ["Tech Lead"]
updated_at: "2026-11-02"
feature_size: "M"
ticket: "roadmap step 2 — url-shortener"
---

# 0004 — Page the link list by keyset on a monotonic UUIDv7 id

- **Status:** Accepted
- **Date:** 2026-11-02
- **Deciders:** igorermolenko (owner / Tech Lead), with Claude during the design Socratic walk

## Context

The owner's list shows links newest first. Links created at the same moment are shown later-created first. Pages hold 20 links by default and up to 100 on request. Each page continues right after its last link, so links created while the owner is paging never cause duplicates or gaps (spec AC-09). That requires keyset pagination: the client sends the key of the last link it saw instead of a page number. Offset paging would shift under concurrent inserts, which AC-09 forbids. Primary keys are UUIDv7 from `shared.IdGenerator` (repo ADR-0003). The current generator fills `rand_a` with random bits, so two ids made in the same millisecond sort in random order, which breaks "later-created first" on ties.

## Decision drivers

- Spec AC-09: a strict newest-first order with a creation-order tie-break, and no duplicates or gaps across pages.
- Spec §6: owner operations p95 ≤ 300 ms.
- Repo ADR-0003: UUIDv7 primary keys from one `shared` generator, and the public short code is a separate field.
- Constraint (sad.md §2): production runs exactly one instance.

## Considered options

1. **Monotonic UUIDv7 + keyset on `id`.** Make `shared.IdGenerator` monotonic within a millisecond (RFC 9562 §6.2 method 1: a counter in the 12-bit `rand_a`, seeded randomly each new millisecond and incremented per id). Order by `id DESC`; the cursor is the last link's id, given to the owner as an opaque token.
2. **Database identity sequence.** Add `seq bigint GENERATED ALWAYS AS IDENTITY` with its own index, order and page by it, and leave `IdGenerator` unchanged.

## Decision outcome

**Chosen:** Option 1, monotonic UUIDv7 + keyset on `id`. The primary-key index alone serves both the order and the cursor, with no second key, no second index, and no internal counter to hide in the cursor. Fixing the generator once also gives every future module time-ordered ids with a correct tie-break.

## Consequences

**Positive**
- One index (the primary key) serves the list query; pages are `WHERE id < :cursor ORDER BY id DESC LIMIT :n`.
- The creation time shown to the owner comes from the same `Clock` instant as the id's timestamp, so the shown order and shown times agree.
- The cursor reveals nothing beyond a link's creation time, which the list already shows.

**Negative**
- Monotonicity is guaranteed only within one process. If trimly ever runs two instances, ids from different instances in the same millisecond tie-break arbitrarily. Today there is exactly one instance (§2); revisit if that changes (§11).
- `shared.IdGenerator` changes behaviour for all modules, so `IdGeneratorTest` gains monotonicity cases, including counter overflow within a millisecond (RFC 9562: advance the timestamp).
- If the system clock steps backwards, the generator must still not go backwards: it keeps the last timestamp and increments.

**Neutral**
- Moving to option 2 later is additive: a new column and a backfill in id order, plus a new cursor format.

## Links

- Spec: [[../spec.md]] US-03, AC-09, AC-10
- SAD: [[../sad.md]] §4 (choice 4), §8 (ID strategy)
- Related ADR: repo [ADR-0003](../../../adr/0003-store-links-in-postgres-on-neon-via-spring-data-jdbc-and-flyway.md)
