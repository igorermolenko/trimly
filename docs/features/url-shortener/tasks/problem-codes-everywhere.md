---
id: T23
title: "Give every owner-API Problem a code; a wrong Content-Type is shared.malformed_request"
layer: "ports"
deps: []
blocks: ["T24", "T25"]
acs: []
files_hint: ["src/main/java/io/github/igorermolenko/trimly/shared/ProblemDetailsAdvice.java", "docs/features/url-shortener/contracts/openapi.yaml", "src/test/java/io/github/igorermolenko/trimly/shared/ProblemDetailsAdviceIT.java"]
owner: "igorermolenko"
estimate: "M"
origin: "review _review/review-2026-11-07.md R4 + R5 / F-4, F-5"
status: "done"
step: "COMMIT"
---

<!-- Follow-up task from the review gate. Work from what is inlined here; the review record and the
named upstream files win over this summary. -->

# T23 — Give every owner-API Problem a code; a wrong Content-Type is shared.malformed_request

## Place in the sequence

- **Blocked by:** —
- **Blocks:** T24, T25
- **Origin:** review finding R4 + R5 / F-4, F-5 — [_review/review-2026-11-07.md](../_review/review-2026-11-07.md)

## Why

Review R4: a `text/plain` or missing Content-Type on create answers Spring's 415 without `code`; the contract and api-sync-report F1 require 400 `shared.malformed_request`. Review R5: the 500, the unknown-`/api` 404 and the framework 405/406 answers have no `code`, which `Problem.required` demands.

## Acceptance criteria

n/a (contract / ADR fidelity) — verbatim text in [spec.md](../spec.md) §5; contract in [openapi.yaml](../contracts/openapi.yaml); decisions in [adr/](../adr/).

## Checklist

- [ ] Override `handleHttpMediaTypeNotSupported` -> 400 `shared.malformed_request`.
- [ ] Fallback `code` on every ProblemDetail the advice emits that lacks one: `shared.not_found`, `shared.method_not_allowed`, `shared.not_acceptable`, `shared.internal_error` (any other 4xx -> `shared.malformed_request`).
- [ ] Add those values to the `Problem.code` enum in `contracts/openapi.yaml`.
- [ ] IT: text/plain create -> 400 malformed_request, nothing created; `/api/v1/nope/x` -> 404 shared.not_found; `DELETE /api/v1/links` -> 405 shared.method_not_allowed.

## Definition of Done

- [ ] ProblemDetailsAdviceIT green; verify + spotless green.
- [ ] lint + vet clean (`./mvnw -B spotless:check`, `./mvnw -B verify`)
