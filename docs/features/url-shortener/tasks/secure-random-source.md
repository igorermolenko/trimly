---
id: T29
title: "Pin that production short codes come from SecureRandom"
layer: "app"
deps: []
blocks: []
acs: ["AC-01"]
files_hint: ["src/main/java/io/github/igorermolenko/trimly/link/app/LinkConfiguration.java", "src/test/java/io/github/igorermolenko/trimly/link/app/LinkConfigurationTest.java"]
owner: "igorermolenko"
estimate: "S"
origin: "review _review/review-2026-11-07-r2.md N4 / F-13"
status: "todo"
step: "—"
---

<!-- Follow-up task from the review gate. Work from what is inlined here; the review record and the
named upstream files win over this summary. -->

# T29 — Pin that production short codes come from SecureRandom

## Place in the sequence

- **Blocked by:** —
- **Blocks:** —
- **Origin:** review finding N4 / F-13 — [_review/review-2026-11-07-r2.md](../_review/review-2026-11-07-r2.md)

## Why

Review N4: `LinkConfiguration.shortCodeGenerator()` builds `new SecureRandom()` inline and nothing asserts it, so switching to `java.util.Random` passes every test. `test-plan.md:32` asks that the code "comes from a secure random source" (AC-01, ADR-0002).

## Acceptance criteria

AC-01 — verbatim text in [spec.md](../spec.md) §5; contract in [openapi.yaml](../contracts/openapi.yaml); decisions in [adr/](../adr/).

## Checklist

- [ ] Extract the production random source into a package-private factory that the generator bean uses.
- [ ] `LinkConfigurationTest` asserts it is a `java.security.SecureRandom`.

## Definition of Done

- [ ] LinkConfigurationTest green, and red if the source is `java.util.Random`; verify + spotless green.
- [ ] lint + vet clean (`./mvnw -B spotless:check`, `./mvnw -B verify`)
