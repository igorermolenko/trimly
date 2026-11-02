---
status: Accepted
owner: "igorermolenko"
reviewers: ["Tech Lead"]
updated_at: "2026-11-02"
feature_size: "M"
ticket: "roadmap step 2 — url-shortener"
---

# 0002 — Generate short codes with SecureRandom and enforce uniqueness with a unique index and insert-retry

- **Status:** Accepted
- **Date:** 2026-11-02
- **Deciders:** igorermolenko (owner / Tech Lead), with Claude during the design Socratic walk

## Context

Every creation issues a new 7-character short code. Each character comes from a cryptographically secure random source and is drawn from a 56-character alphabet (A–Z, a–z, 2–9 minus 0, O, o, 1, l, I), and letter case matters (spec AC-01). A code must never be issued twice, even after its link is disabled (AC-20). A collision with an existing code must be retried, up to 5 freshly generated codes, before the owner is told "temporarily unavailable" (AC-20b). Someone has to detect the collision. The same alphabet also decides on the redirect side whether an address can be a short code at all (AC-16b).

## Decision drivers

- Spec §6 NFR: owner operations p95 ≤ 300 ms with link storage awake. Every extra round trip to Neon counts.
- Spec AC-20 / CONTEXT invariant: a short code is issued once and never points to a different target URL.
- Spec AC-20b: up to 5 attempts, then "temporarily unavailable" with nothing created.
- Constraint (sad.md §2): Neon Free CU-hours. A background job that wakes the database costs budget.

## Considered options

1. **Unique index + insert-retry.** The `short_code` column has a UNIQUE constraint. The create use case inserts directly, and on a unique violation of that specific constraint it generates a new code and retries, up to 5 attempts in total.
2. **SELECT first, then INSERT.** Check "does this code exist?" before inserting, keeping the UNIQUE constraint only as a safety net.
3. **Pre-generated code pool.** A table of free codes, filled ahead of time by a background job. Each create claims one.

## Decision outcome

**Chosen:** Option 1, unique index + insert-retry. The database itself guarantees uniqueness, with no race window between a check and the insert. The common case costs one round trip. Option 2 adds a round trip and still has to handle the unique violation. Option 3 adds a table and a background job that wakes Neon, to solve a collision chance that is negligible at 56⁷ ≈ 1.7 × 10¹² codes with one link creator.

## Consequences

**Positive**
- Create is a single INSERT, which fits the 300 ms owner p95.
- AC-20 holds structurally: rows are never deleted (spec §3), so the unique index keeps a code from ever being reissued.
- The alphabet, the length and the well-formedness rule live once, in `link`'s `domain`. `redirect` reaches them only through `link`'s published lookup (sad.md §5).

**Negative**
- The create use case must recognise the unique violation on `short_code` specifically (by constraint name). It must not treat other errors that way, such as a storage timeout, which must become "temporarily unavailable" (ADR-0003).
- Each retry is a separate statement. A failed INSERT aborts a PostgreSQL transaction, so retries must not share one transaction.

**Neutral**
- The code generator is injectable, so tests can force collisions deterministically (AC-20b).
- Moving to a code pool later would be additive (a new table, the same column), but it is not foreseen.

## Links

- Spec: [[../spec.md]] AC-01, AC-16b, AC-20, AC-20b
- SAD: [[../sad.md]] §4 (choice 2), §5
- Related ADR: [[0003-bound-every-link-storage-call-within-2-seconds-using-pool-and-driver-timeouts]]
