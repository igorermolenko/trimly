---
id: T30
title: "Pin that the owner credential never reaches the logs"
layer: "tests"
deps: []
blocks: []
acs: ["AC-18"]
files_hint: ["src/test/java/io/github/igorermolenko/trimly/OwnerAuthDenialIT.java"]
owner: "igorermolenko"
estimate: "S"
origin: "review _review/review-2026-11-07-r2.md N5 / F-14"
status: "todo"
step: "—"
---

<!-- Follow-up task from the review gate. Work from what is inlined here; the review record and the
named upstream files win over this summary. -->

# T30 — Pin that the owner credential never reaches the logs

## Place in the sequence

- **Blocked by:** —
- **Blocks:** —
- **Origin:** review finding N5 / F-14 — [_review/review-2026-11-07-r2.md](../_review/review-2026-11-07-r2.md)

## Why

Review N5: spec §6.1 says the owner credential is "never written to logs" (`test-plan.md:79`), but no test captures log output.

## Acceptance criteria

AC-18 — verbatim text in [spec.md](../spec.md) §5; contract in [openapi.yaml](../contracts/openapi.yaml); decisions in [adr/](../adr/).

## Checklist

- [ ] `OwnerAuthDenialIT` uses `OutputCaptureExtension` and runs owner calls with the right and with a wrong credential.
- [ ] Assert the captured output contains neither the owner credential nor the wrong one.
- [ ] Prove the test can fail: a temporary log line printing the `Authorization` header must turn it red.

## Definition of Done

- [ ] OwnerAuthDenialIT green, and red with the credential logged; verify + spotless green.
- [ ] lint + vet clean (`./mvnw -B spotless:check`, `./mvnw -B verify`)
