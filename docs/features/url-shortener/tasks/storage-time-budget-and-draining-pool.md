---
id: T6
title: "Bound every storage call to 2 s, drain the idle pool and keep health checks off the DB"
layer: "wiring"
deps: ["T5"]
blocks: ["T16"]
acs: ["AC-17", "AC-17b"]
files_hint: ["src/main/resources/application.yml", "pom.xml", "src/test/java/io/github/igorermolenko/trimly/link/infra/StorageTimeBudgetIT.java"]
owner: "igorermolenko"
estimate: "M"
context_budget: "M"     # 43 inlined lines
status: "todo"
step: ""
---

<!-- To the executing agent: work from what is inlined here. If a slice is insufficient, ambiguous,
or contradicts the code in front of you, open the named file for the full text and follow that.
Do not invent the missing part. Every quote ends with its provenance signature; the source wins. -->

# T6 — Bound every storage call to 2 s, drain the idle pool and keep health checks off the DB

## Place in the sequence

- **Blocked by:** T5 — Persist ShortLink through a Spring Data JDBC store with constraint-aware failure translation
- **Blocks:** T16 — Add end-to-end ITs: link lifecycle, storage outage within 2 s, and auth denial parity
- **Wave:** 3 — after T5 (longest path).
- **Lane:** shares files with T5, T7, T12 (`files_hint` overlap) — serialized by `implement`.

## Why (user story)

> **As a** visitor
> **I want** a plain message that says either "link not found" or "temporarily unavailable"
> **So that** I know whether the link is dead or worth retrying later
>
> — `spec.md §4, US-06, verbatim` · full text: [spec.md](../spec.md)

This task makes «storage did not answer within 2 s» a real, bounded event, so the use cases can say «temporarily unavailable» in time.

## Inlined context

> **Chosen:** Option 1, pool and driver timeouts. Every request genuinely asks storage, so a storage that recovers is used at once, as AC-17 requires. A timed-out statement is cancelled by the driver itself instead of hanging in the background holding a connection.
>
> - Timeouts surface as Spring `DataAccessException` subtypes. `link`'s app layer translates them into an explicit "storage unavailable" outcome (sad.md §8 error handling).
> - No single per-request clock enforces the 2 s; the guarantee is the sum of budgets. It must be proven by an integration test that freezes the Postgres test container and asserts the answer within 2 s.
> - Starting split: the connection-acquire timeout covers the connect, with about 1.2–1.5 s available for connect + wake and the rest for the statement. Tune it in `tasks` against the test.
>
> — `adr/0003-bound-every-link-storage-call-within-2-seconds-using-pool-and-driver-timeouts.md §Decision outcome + §Consequences, abridged` · full text: [adr/0003-bound-every-link-storage-call-within-2-seconds-using-pool-and-driver-timeouts.md](../adr/0003-bound-every-link-storage-call-within-2-seconds-using-pool-and-driver-timeouts.md)

> - **The pool drains when idle.** Hikari keeps `minimumIdle: 0` and a short `idleTimeout` (seconds), so connections close soon after traffic stops. The first request after idle opens a fresh connection inside the 2 s budget.
> - **Health checks never touch link storage.** Fly's machine checks and the exposed health endpoint use a liveness-only group. Spring Boot's `db` health indicator is excluded.
> - **Metrics:** Spring Boot Actuator + Micrometer Prometheus registry. `http.server.requests` with a percentile histogram gives the per-operation p95 that spec §6 measures [...] The `prometheus` and `health` endpoints live on a separate management port that is not routed publicly.
>
> — `sad.md §7, «Keeping link storage within its compute allowance» + Monitoring, abridged` · full text: [sad.md](../sad.md)

> **Hard rule:** secrets only from env vars, never committed. [...] `application.yml` holds non-secret defaults only.
>
> — `CLAUDE.md §Conventions, Config / secrets bullet, abridged` · full text: [CLAUDE.md](../../../../CLAUDE.md)

**Fallback:** insufficient or contradicted by the code → read the named file in full
([spec.md](../spec.md) · [sad.md](../sad.md) · [data-model.md](../data-model.md) ·
[openapi.yaml](../contracts/openapi.yaml) · [adr/](../adr/)) and follow it. Do not guess.

## Data delta

No DB changes.

## API contract

Internal — no API surface.

## Acceptance criteria

### AC-17 — cross-context

> **Given** trimly temporarily cannot look up links because its link storage has not answered within 2 seconds
> **When** a visitor opens a well-formed short code
> **Then** the visitor sees "temporarily unavailable, try again later" as a short human-readable sentence, no later than 2 seconds after opening the link, not "link not found"; trimly does not serve the redirect from any remembered copy of the link. Link storage that is waking from idle and answers within the 2 seconds is not unavailable: the visitor gets the normal outcome (AC-07, AC-12 or AC-16)
>
> — `spec.md §5, AC-17, verbatim` · full text: [spec.md](../spec.md)

### AC-17b — error

> **Given** the owner presents the owner credential and the link storage has not answered within 2 seconds
> **When** the owner creates, lists, disables or re-enables short links
> **Then** the owner is told "temporarily unavailable, try again later" and nothing is confirmed; if an unconfirmed creation was in fact recorded, retrying it creates a second, separate short link (AC-02), which is accepted: the owner sees both in the list and can disable the extra one
>
> — `spec.md §5, AC-17b, verbatim` · full text: [spec.md](../spec.md)

## Checklist

- [ ] `src/main/resources/application.yml`: `spring.datasource.hikari.minimum-idle: 0`, a short `idle-timeout` (≥ 10 s, Hikari's floor), `connection-timeout` ≈ 1.2–1.5 s; PostgreSQL driver `connectTimeout` / `socketTimeout` and a statement budget (JDBC query timeout or `options=-c statement_timeout=…ms`) so connect + statement stays < 2 s. Note: driver and `JdbcTemplate` query timeouts are whole seconds — prefer `statement_timeout` (ms) for the statement share.
- [ ] `pom.xml`: add `spring-boot-starter-actuator` and `micrometer-registry-prometheus`; `application.yml`: `management.server.port` (separate, non-public), expose `health,prometheus`, `management.health.db.enabled: false`, percentile histogram on `http.server.requests`.
- [ ] `src/test/java/io/github/igorermolenko/trimly/link/infra/StorageTimeBudgetIT.java`: pause the Testcontainers Postgres (`DockerClient.pauseContainerCmd`), call each `ShortLinkStore` operation through `StorageFailures`, assert `LinkStorageUnavailable` within 2 s; unpause and assert the next call succeeds (no circuit breaker).
- [ ] Record the chosen split in a YAML comment next to the values (ADR-0003 «tune it in tasks»).

## Edge cases

| Case | Behaviour |
|---|---|
| Postgres paused (no answer) | `LinkStorageUnavailable` in ≤ 2 s for read, insert, page and state update. |
| Postgres resumes | The very next call succeeds — no remembered failure state (AC-17). |
| Pool idle > idle-timeout | No open connections remain (`HikariPoolMXBean.getTotalConnections() == 0`). |
| `GET /actuator/health` on the management port | `UP` with storage paused — no `db` component. |
| Slow wake within budget | Normal answer — covered by the split, verified post-deploy (sad §10 QG-2b). |

## Definition of Done

- [ ] `StorageTimeBudgetIT` passes: every store operation answers `LinkStorageUnavailable` ≤ 2 s with the container paused, and succeeds after unpause.
- [ ] Health endpoint stays `UP` with storage paused (test).
- [ ] No secret added to `application.yml`; `./mvnw -B verify` green.
- [ ] every Hard Rule inlined above still holds
- [ ] lint + vet clean (`./mvnw -B spotless:check`, `./mvnw -B verify`)
