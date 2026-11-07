---
id: T17
title: "Re-split the storage budget so a wake within 2 s is answered normally"
layer: "wiring"
deps: []
blocks: ["T18"]
acs: ["AC-17", "AC-17b"]
files_hint: ["src/main/resources/application.yml", "src/main/java/io/github/igorermolenko/trimly/link/infra/StorageConnectionConfiguration.java", "src/test/java/io/github/igorermolenko/trimly/link/infra/StorageTimeBudgetIT.java"]
owner: "igorermolenko"
estimate: "M"
origin: "review _review/review-2026-11-07.md R1 / F-1"
status: "todo"
step: "—"
---

<!-- Follow-up task from the review gate. Work from what is inlined here; the review record and the
named upstream files win over this summary. -->

# T17 — Re-split the storage budget so a wake within 2 s is answered normally

## Place in the sequence

- **Blocked by:** —
- **Blocks:** T18
- **Origin:** review finding R1 / F-1 — [_review/review-2026-11-07.md](../_review/review-2026-11-07.md)

## Why

Review R1: `connection-timeout: 950` plus pgjdbc `socketTimeout`/`loginTimeout` of 1 s cap a Neon wake at about 1 s, so a wake that answers inside 2 s still yields 503 (AC-17). pgjdbc applies `socketTimeout` to the login handshake too, so it cannot stay at 1 s.

## Acceptance criteria

AC-17, AC-17b — verbatim text in [spec.md](../spec.md) §5; contract in [openapi.yaml](../contracts/openapi.yaml); decisions in [adr/](../adr/).

## Checklist

- [ ] Driver: `connectTimeout`/`loginTimeout`/`socketTimeout` = 2 s, so the login (incl. Neon wake) may take up to 2 s and the connection still lands in the pool.
- [ ] After login, set each pooled connection's network timeout to ~700 ms (`Connection.setNetworkTimeout`, ms precision) via a wrapping DataSource that Hikari reads and restores to.
- [ ] Hikari `connection-timeout` ~ 1.3 s; server `statement_timeout` ~ 600 ms; worst case acquire + statement ~ 2.0 s. Record the split in the YAML comment.
- [ ] IT: a connection that takes ~1.2 s to log in (slow wake) still serves the store call normally; the existing frozen-storage bound stays <= 2 s.

## Definition of Done

- [ ] StorageTimeBudgetIT green incl. a new slow-wake case; StorageOutageIT still <= 2 s; `./mvnw -B verify` + spotless green.
- [ ] lint + vet clean (`./mvnw -B spotless:check`, `./mvnw -B verify`)
