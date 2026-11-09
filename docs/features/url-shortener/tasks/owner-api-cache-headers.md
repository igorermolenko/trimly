---
id: T22
title: "Keep Spring Security's no-cache headers on the owner API"
layer: "ports"
deps: ["T21"]
blocks: []
acs: []
files_hint: ["src/main/java/io/github/igorermolenko/trimly/access/api/SecurityConfiguration.java", "src/test/java/io/github/igorermolenko/trimly/access/api/OwnerApiSecurityIT.java"]
owner: "igorermolenko"
estimate: "S"
origin: "review _review/review-2026-11-07.md R7 / F-7"
status: "done"
step: "COMMIT"
---

<!-- Follow-up task from the review gate. Work from what is inlined here; the review record and the
named upstream files win over this summary. -->

# T22 — Keep Spring Security's no-cache headers on the owner API

## Place in the sequence

- **Blocked by:** T21
- **Blocks:** —
- **Origin:** review finding R7 / F-7 — [_review/review-2026-11-07.md](../_review/review-2026-11-07.md)

## Why

Review R7: `headers.cacheControl(disable)` removes no-cache from the owner's private list, the 401s and disable/enable answers; Spring Security's writer already leaves a response that set its own `Cache-Control` alone, so redirects keep `no-store`.

## Acceptance criteria

n/a (contract / ADR fidelity) — verbatim text in [spec.md](../spec.md) §5; contract in [openapi.yaml](../contracts/openapi.yaml); decisions in [adr/](../adr/).

## Checklist

- [ ] Remove the `cacheControl.disable()` line.
- [ ] IT: `GET /api/v1/links` and a 401 carry `Cache-Control` containing `no-store`; RedirectIT keeps asserting exactly `no-store` on redirects.

## Definition of Done

- [ ] OwnerApiSecurityIT + RedirectIT green; verify + spotless green.
- [ ] lint + vet clean (`./mvnw -B spotless:check`, `./mvnw -B verify`)
