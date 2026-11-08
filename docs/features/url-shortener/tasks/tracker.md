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
| T23 | [Give every owner-API Problem a code; a wrong Content-Type is shared.malformed_request](./problem-codes-everywhere.md) | ports | igorermolenko | M | — | done | COMMIT |
| T24 | [Write the missing test-plan tests for AC-20b, AC-20, AC-19, AC-18, AC-11 and AC-04](./test-plan-gaps.md) | tests | igorermolenko | M | T23 | done | COMMIT |
| T25 | [Validate owner-API responses against contracts/openapi.yaml](./contract-response-validation.md) | tests | igorermolenko | S | T23, T24 | done | COMMIT |
| T26 | [Prove a storage wake within 2 s still gives the visitor a normal redirect](./slow-wake-redirect-it.md) | tests | igorermolenko | S | — | done | COMMIT |
| T27 | [Answer firewall-rejected owner-API addresses with Problem Details shared.malformed_request](./owner-api-firewall-problem.md) | ports | igorermolenko | S | — | done | COMMIT |
| T28 | [Answer GET / with the plain-text "Link not found."](./root-path-not-found.md) | ports | igorermolenko | S | T27 | done | COMMIT |
| T29 | [Pin that production short codes come from SecureRandom](./secure-random-source.md) | app | igorermolenko | S | — | done | COMMIT |
| T30 | [Pin that the owner credential never reaches the logs](./credential-never-logged.md) | tests | igorermolenko | S | — | done | COMMIT |
| T31 | [Sync spec.md with ADR-0006 and sad.md §8 (15-minute probe, closed open questions)](./spec-sync-probe-and-open-questions.md) | docs | igorermolenko | XS | — | done | COMMIT |
| T32 | [Record the shared.* Problem codes in api-sync-report.md](./api-sync-report-codes.md) | docs | igorermolenko | XS | T27 | done | COMMIT |
| T33 | [Answer a direct GET /error with the plain-text "Link not found."](./error-path-not-found.md) | ports | igorermolenko | S | — | done | COMMIT |
| T34 | [Answer GET /api and /api/ for the owner with a Problem Details 404](./api-root-problem.md) | ports | igorermolenko | XS | T33 | done | COMMIT |
| T35 | [Reject unknown members in a create request as shared.malformed_request](./reject-unknown-create-members.md) | ports | igorermolenko | S | T34 | done | COMMIT |
| T36 | [Give firewall-rejected owner-API answers the owner-API cache and nosniff headers](./firewall-problem-owner-headers.md) | ports | igorermolenko | XS | T33 | done | COMMIT |
| T37 | [Sync sad.md, data-model.md, spec §8 and api-sync-report F1 with what landed](./docs-sync-sad-data-model-f1.md) | docs | igorermolenko | XS | — | done | COMMIT |
| T38 | [Let browser-sent raw [ ] { } \| ^ ` characters reach trimly instead of Tomcat's bare 400](./tomcat-relaxed-url-chars.md) | ports | igorermolenko | S | — | done | COMMIT |
| T39 | [Answer an unparseable http(s) target URL with the AC-05 percent-encoding guidance](./target-url-unparseable-not-encoded.md) | domain | igorermolenko | XS | — | done | COMMIT |
| T40 | [List every shared.malformed_request trigger in openapi.yaml and close the stale sad.md §11 doc-drift risk](./docs-sync-malformed-request-and-sad-risk.md) | docs | igorermolenko | XS | — | done | COMMIT |
| T41 | [Answer a bare https:// or http:// (no host) with the AC-03 answer, not the AC-05 encoding guidance](./target-url-bare-scheme-unsupported.md) | domain | igorermolenko | XS | — | done | COMMIT |
| T42 | [Answer an address Tomcat itself rejects (bad percent-escape) with "Link not found." or a shared.malformed_request Problem](./tomcat-rejected-uri-answers.md) | ports | igorermolenko | S | — | todo | — |
| T43 | [Refuse to start with a public base URL that has a path, query, fragment or userinfo](./public-base-url-strict-validation.md) | app | igorermolenko | XS | — | todo | — |
| T44 | [Document the shared 400 on list/disable/enable, the required TRIMLY_* config vars, and the 23-AC count](./docs-sync-firewall-400-and-config.md) | docs | igorermolenko | XS | T42 | todo | — |

**Total:** 44 tasks — 16 planned (~12.5 person-days) + 9 review follow-ups T17–T25 from `_review/review-2026-11-07.md` + 7 review follow-ups T26–T32 from `_review/review-2026-11-07-r2.md` + 5 review follow-ups T33–T37 from `_review/review-2026-11-07-r3.md` + 3 review follow-ups T38–T40 from `_review/review-2026-11-07-r4.md` + 4 review follow-ups T41–T44 from `_review/review-2026-11-08.md`.
