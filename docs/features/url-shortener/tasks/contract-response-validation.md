---
id: T25
title: "Validate owner-API responses against contracts/openapi.yaml"
layer: "tests"
deps: ["T23", "T24"]
blocks: []
acs: ["AC-01", "AC-09"]
files_hint: ["pom.xml", "src/test/java/io/github/igorermolenko/trimly/link/api/OwnerApiContractIT.java"]
owner: "igorermolenko"
estimate: "S"
origin: "review _review/review-2026-11-07.md R9 (g) / F-9"
status: "done"
step: "COMMIT"
---

<!-- Follow-up task from the review gate. Work from what is inlined here; the review record and the
named upstream files win over this summary. -->

# T25 — Validate owner-API responses against contracts/openapi.yaml

## Place in the sequence

- **Blocked by:** T23, T24
- **Blocks:** —
- **Origin:** review finding R9 (g) / F-9 — [_review/review-2026-11-07.md](../_review/review-2026-11-07.md)

## Why

Review R9(g): test-plan rows for AC-01 and AC-09 say real responses are validated against `contracts/openapi.yaml`, but no test reads the contract.

## Acceptance criteria

AC-01, AC-09 — verbatim text in [spec.md](../spec.md) §5; contract in [openapi.yaml](../contracts/openapi.yaml); decisions in [adr/](../adr/).

## Checklist

- [ ] Load `docs/features/url-shortener/contracts/openapi.yaml` in tests and validate real owner-API responses (create 201, list 200, Problem answers) against their schemas.
- [ ] Any new dependency is test-scoped.

## Definition of Done

- [ ] OwnerApiContractIT green, and red when a response field is renamed; verify + spotless green.
- [ ] lint + vet clean (`./mvnw -B spotless:check`, `./mvnw -B verify`)
