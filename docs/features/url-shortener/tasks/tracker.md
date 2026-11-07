# Tracker — url-shortener

> Status of every task in the epic. `implement` updates **Status** and **Step** after every
> subagent it runs (and commits them with the task), so this table shows where the run is right now.
> Status: `todo` · `in_progress` · `blocked` · `review` · `done`.
> Step: `SELECT` · `RED` · `GREEN` · `REFACTOR` · `GATE` · `REVIEW` · `COMMIT` (`—` before the task starts).
> A `blocked` row carries its reason in Step, e.g. `GATE — lint red after 3 retries`.

| # | Task | Layer | Owner | Estimate | Blocked by | Status | Step |
|---|---|---|---|---|---|---|---|
| T1 | [Promote the staged short_link migration to V2__create_short_link.sql](./promote-short-link-migration.md) | migration | igorermolenko | S | — | done | COMMIT |
| T2 | [Make shared.IdGenerator monotonic within a millisecond (RFC 9562 method 1)](./monotonic-id-generator.md) | domain | igorermolenko | S | — | done | COMMIT |
| T3 | [Add ShortCode (alphabet, length, well-formedness) and the SecureRandom ShortCodeGenerator](./short-code-and-generator.md) | domain | igorermolenko | S | — | done | COMMIT |
| T4 | [Add TargetUrl validation rules (scheme, length, encoding, own hosts) in link.domain](./target-url-rules.md) | domain | igorermolenko | M | — | done | COMMIT |
| T5 | [Persist ShortLink through a Spring Data JDBC store with constraint-aware failure translation](./short-link-store.md) | infra | igorermolenko | M | T1, T3 | done | COMMIT |
| T6 | [Bound every storage call to 2 s, drain the idle pool and keep health checks off the DB](./storage-time-budget-and-draining-pool.md) | wiring | igorermolenko | M | T5 | done | COMMIT |
| T7 | [Implement the CreateShortLink use case with 5-attempt insert-retry and public-base-url](./create-short-link-use-case.md) | app | igorermolenko | M | T2, T4, T5 | done | COMMIT |
| T8 | [Implement ListShortLinks with keyset paging and an opaque continuation cursor](./list-short-links-use-case.md) | app | igorermolenko | S | T2, T5 | done | COMMIT |
| T9 | [Implement DisableShortLink and EnableShortLink as one idempotent state change](./disable-enable-use-cases.md) | app | igorermolenko | S | T5 | done | COMMIT |
| T10 | [Publish link.app.LinkLookup returning a sealed Resolution (Found / NotFound / Unavailable)](./link-lookup-sealed-resolution.md) | app | igorermolenko | S | T5 | done | COMMIT |
| T11 | [Add the access module's OwnerCredentialPolicy and fail-fast OwnerCredentialProperties](./owner-credential-policy.md) | domain | igorermolenko | S | — | done | COMMIT |
| T12 | [Guard the owner API with a stateless bearer SecurityFilterChain and a Problem Details entry point](./owner-api-security-filter-chain.md) | ports | igorermolenko | M | T11 | done | COMMIT |
| T13 | [Expose POST /api/v1/links and the Problem Details `code` mapping for owner errors](./create-short-link-endpoint.md) | ports | igorermolenko | M | T7, T12 | done | COMMIT |
| T14 | [Expose GET /api/v1/links and POST …/{shortCode}/disable|enable](./list-disable-enable-endpoints.md) | ports | igorermolenko | M | T8, T9, T13 | done | COMMIT |
| T15 | [Serve GET /{shortCode}: non-reusable 302, plain-text 404/503, query ignored](./redirect-endpoint.md) | ports | igorermolenko | M | T10, T12 | done | COMMIT |
| T16 | [Add end-to-end ITs: link lifecycle, storage outage within 2 s, and auth denial parity](./cross-cutting-integration-tests.md) | tests | igorermolenko | M | T6, T14, T15 | done | COMMIT |
| T17 | [Re-split the storage budget so a wake within 2 s is answered normally](./storage-wake-window.md) | wiring | igorermolenko | M | — | done | COMMIT |
| T18 | [Run Flyway migrations on their own connection, outside the request budget](./flyway-own-connection.md) | wiring | igorermolenko | S | T17 | done | COMMIT |
| T19 | [Count the public base URL host as an own host; no localhost defaults in production config](./public-host-is-own-host.md) | app | igorermolenko | S | T18 | done | COMMIT |
| T20 | [Expose the liveness-only health group ADR-0006 decides](./liveness-health-group.md) | wiring | igorermolenko | S | T19 | done | COMMIT |
| T21 | [Answer firewall-rejected visitor paths with the plain-text "Link not found."](./firewall-rejected-visitor-paths.md) | ports | igorermolenko | S | — | done | COMMIT |
| T22 | [Keep Spring Security's no-cache headers on the owner API](./owner-api-cache-headers.md) | ports | igorermolenko | S | T21 | done | COMMIT |
| T23 | [Give every owner-API Problem a code; a wrong Content-Type is shared.malformed_request](./problem-codes-everywhere.md) | ports | igorermolenko | M | — | todo | — |
| T24 | [Write the missing test-plan tests for AC-20b, AC-20, AC-19, AC-18, AC-11 and AC-04](./test-plan-gaps.md) | tests | igorermolenko | M | T23 | todo | — |
| T25 | [Validate owner-API responses against contracts/openapi.yaml](./contract-response-validation.md) | tests | igorermolenko | S | T23, T24 | todo | — |

**Total:** 25 tasks — 16 planned (~12.5 person-days) + 9 review follow-ups T17–T25 from `_review/review-2026-11-07.md`.
