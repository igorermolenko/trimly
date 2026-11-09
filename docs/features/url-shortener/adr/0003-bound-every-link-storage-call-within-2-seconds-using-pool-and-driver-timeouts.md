---
status: Accepted
owner: "igorermolenko"
reviewers: ["Tech Lead"]
updated_at: "2026-11-02"
feature_size: "M"
ticket: "roadmap step 2 — url-shortener"
---

# 0003 — Bound every link-storage call within 2 seconds using pool and driver timeouts

- **Status:** Accepted
- **Date:** 2026-11-02
- **Deciders:** igorermolenko (owner / Tech Lead), with Claude during the design Socratic walk

## Context

If link storage has not answered within 2 seconds, visitors and the owner must get "temporarily unavailable" no later than 2 seconds after their request, and never "link not found" (spec AC-17, AC-17b, §6 "Storage-unavailable answer time ≤ 2 s"). Storage that is waking from idle and answers within the 2 seconds is not unavailable (AC-17). A request can wait in three places: taking a connection from the pool, opening a new TCP/TLS connection (which also wakes a suspended Neon compute), and running the SQL statement. Both `link` (owner operations) and `redirect` (via `link`'s lookup) depend on this behaviour.

## Decision drivers

- Quality goal 1 (sad.md §1): an outage looks like an outage, never like a dead link.
- Spec §6: ≤ 2 s to "temporarily unavailable"; redirect p95 ≤ 1 s for the first request after idle.
- Spec AC-17: storage that wakes within 2 s must produce the normal outcome. Nothing may answer "unavailable" from a remembered failure while storage is actually back.
- Constraint (sad.md §2): no redirect cache; one small instance; prefer no new infrastructure.

## Considered options

1. **Pool and driver timeouts.** Split the 2 s between the Hikari connection-acquire timeout, the PostgreSQL JDBC driver's connect and socket timeouts, and a JDBC query timeout, so their sum stays below 2 s. Map any timeout or connection failure to "temporarily unavailable".
2. **Resilience4j TimeLimiter + CircuitBreaker.** Run each storage call on a separate thread under a 2 s wall-clock deadline. A circuit breaker answers "unavailable" immediately for a while after repeated failures.

## Decision outcome

**Chosen:** Option 1, pool and driver timeouts. Every request genuinely asks storage, so a storage that recovers is used at once, as AC-17 requires. A timed-out statement is cancelled by the driver itself instead of hanging in the background holding a connection. No dependency is added. Option 2's deadline does not stop the JDBC call, so it would still need option 1's driver timeouts. Its open circuit would also answer "unavailable" while storage might already be back, which works against AC-17 and the 99.5 % availability target.

## Consequences

**Positive**
- One configuration surface (`spring.datasource.hikari.*` + driver properties + a query timeout) covers both modules.
- Timeouts surface as Spring `DataAccessException` subtypes. `link`'s app layer translates them into an explicit "storage unavailable" outcome (sad.md §8 error handling).

**Negative**
- No single per-request clock enforces the 2 s; the guarantee is the sum of budgets. It must be proven by an integration test that freezes the Postgres test container and asserts the answer within 2 s (spec §6 measurement for AC-17 / AC-17b).
- The budget split is a trade-off. Too tight a connect timeout turns a slow Neon wake-up into "unavailable"; too loose leaves no room for the statement. Starting split: the connection-acquire timeout covers the connect, with about 1.2–1.5 s available for connect + wake and the rest for the statement. Tune it in `tasks` against the test.
- A create whose INSERT committed but whose answer timed out is reported as unavailable; retrying it makes a second link. The spec accepts this (AC-17b).

**Neutral**
- Pool sizing and idle behaviour are a separate cost concern (sad.md §7). This ADR fixes only the time budget.

## Links

- Spec: [[../spec.md]] AC-17, AC-17b, AC-20b, §6
- SAD: [[../sad.md]] §4 (choice 3), §6, §8
- Related ADR: [[0002-generate-short-codes-with-securerandom-and-enforce-uniqueness-with-a-unique-index-and-insert-retry]]
