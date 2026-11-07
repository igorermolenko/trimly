---
id: T20
title: "Expose the liveness-only health group ADR-0006 decides"
layer: "wiring"
deps: ["T19"]
blocks: []
acs: []
files_hint: ["src/main/resources/application.yml", "src/test/java/io/github/igorermolenko/trimly/link/infra/StorageTimeBudgetIT.java"]
owner: "igorermolenko"
estimate: "S"
origin: "review _review/review-2026-11-07.md R6 / F-6"
status: "todo"
step: "—"
---

<!-- Follow-up task from the review gate. Work from what is inlined here; the review record and the
named upstream files win over this summary. -->

# T20 — Expose the liveness-only health group ADR-0006 decides

## Place in the sequence

- **Blocked by:** T19
- **Blocks:** —
- **Origin:** review finding R6 / F-6 — [_review/review-2026-11-07.md](../_review/review-2026-11-07.md)

## Why

Review R6: only the `db` indicator is disabled; ADR-0006 specifies a liveness-only group, and off Kubernetes no `/actuator/health/liveness` exists for Fly to probe.

## Acceptance criteria

n/a (contract / ADR fidelity) — verbatim text in [spec.md](../spec.md) §5; contract in [openapi.yaml](../contracts/openapi.yaml); decisions in [adr/](../adr/).

## Checklist

- [ ] `management.endpoint.health.probes.enabled: true` (liveness group = `livenessState`).
- [ ] IT: `/actuator/health/liveness` on the management port is UP within 2 s while storage is paused.

## Definition of Done

- [ ] Liveness IT green; verify + spotless green.
- [ ] lint + vet clean (`./mvnw -B spotless:check`, `./mvnw -B verify`)
