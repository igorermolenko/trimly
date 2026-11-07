---
id: T34
title: "Answer GET /api and /api/ for the owner with a Problem Details 404"
layer: "ports"
deps: ["T33"]
blocks: []
acs: []
files_hint: ["src/main/java/io/github/igorermolenko/trimly/redirect/api/RedirectController.java", "src/test/java/io/github/igorermolenko/trimly/link/api/OwnerApiContractIT.java"]
owner: "igorermolenko"
estimate: "XS"
origin: "review _review/review-2026-11-07-r3.md P2 / F-18"
status: "todo"
step: "—"
---

<!-- Follow-up task from the review gate. Work from what is inlined here; the review record and the
named upstream files win over this summary. -->

# T34 — Answer GET /api and /api/ for the owner with a Problem Details 404

## Place in the sequence

- **Blocked by:** T33
- **Blocks:** —
- **Origin:** review finding P2 / F-18 — [_review/review-2026-11-07-r3.md](../_review/review-2026-11-07-r3.md)

## Why

Review P2: with a valid credential, `/api` and `/api/` match `/{segment}` and get the visitor's text/plain "Link not found.". The contract (`openapi.yaml:24-25`) says owner-API errors are Problem Details with a `code`, and deeper unknown `/api/…` paths already get a Problem 404.

## Acceptance criteria

— (contract / docs consistency; no §5 AC) — verbatim text in [spec.md](../spec.md) §5; contract in [openapi.yaml](../contracts/openapi.yaml); decisions in [adr/](../adr/).

## Checklist

- [ ] In `RedirectController.follow`, treat segment `api` like `tooManySegments` does: throw `ErrorResponseException(NOT_FOUND)`.
- [ ] Add `/api` and `/api/` to `OwnerApiContractIT.everyOwnerErrorIsAProblem`.

## Definition of Done

- [ ] OwnerApiContractIT green with `/api` and `/api/` answering a `Problem` 404 `shared.not_found`; verify + spotless green.
- [ ] lint + vet clean (`./mvnw -B spotless:check`, `./mvnw -B verify`)
