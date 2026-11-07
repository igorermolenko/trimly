---
id: T18
title: "Run Flyway migrations on their own connection, outside the request budget"
layer: "wiring"
deps: ["T17"]
blocks: ["T19"]
acs: []
files_hint: ["src/main/java/io/github/igorermolenko/trimly/link/infra/StorageConnectionConfiguration.java", "src/test/java/io/github/igorermolenko/trimly/link/infra/StorageTimeBudgetIT.java"]
owner: "igorermolenko"
estimate: "S"
origin: "review _review/review-2026-11-07.md R8 / F-8"
status: "done"
step: "COMMIT"
---

<!-- Follow-up task from the review gate. Work from what is inlined here; the review record and the
named upstream files win over this summary. -->

# T18 — Run Flyway migrations on their own connection, outside the request budget

## Place in the sequence

- **Blocked by:** T17
- **Blocks:** T19
- **Origin:** review finding R8 / F-8 — [_review/review-2026-11-07.md](../_review/review-2026-11-07.md)

## Why

Review R8: Flyway shares the Hikari pool, so migrations inherit `statement_timeout` and the short network timeout; a long migration, a migration-lock wait or a slow wake at startup would stop trimly from starting. ADR-0003 scopes the budget to link-storage calls only.

## Acceptance criteria

n/a (contract / ADR fidelity) — verbatim text in [spec.md](../spec.md) §5; contract in [openapi.yaml](../contracts/openapi.yaml); decisions in [adr/](../adr/).

## Checklist

- [ ] A `FlywayConfigurationCustomizer` points Flyway at a plain driver DataSource built from the same URL/user/password, without the init SQL or the network timeout.
- [ ] IT: a statement longer than the request statement budget succeeds on Flyway's connection.

## Definition of Done

- [ ] Flyway connection has no statement/network timeout (test); ApplicationBootIT green; verify + spotless green.
- [ ] lint + vet clean (`./mvnw -B spotless:check`, `./mvnw -B verify`)
