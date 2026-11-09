---
id: T27
title: "Answer firewall-rejected owner-API addresses with Problem Details shared.malformed_request"
layer: "ports"
deps: []
blocks: ["T28", "T32"]
acs: []
files_hint: ["src/main/java/io/github/igorermolenko/trimly/redirect/api/VisitorRequestRejectedHandler.java", "src/main/java/io/github/igorermolenko/trimly/shared/ProblemDetailsAdvice.java", "src/test/java/io/github/igorermolenko/trimly/redirect/api/RedirectIT.java", "src/test/java/io/github/igorermolenko/trimly/link/api/OwnerApiContractIT.java"]
owner: "igorermolenko"
estimate: "S"
origin: "review _review/review-2026-11-07-r2.md N2 / F-11"
status: "done"
step: "COMMIT"
---

<!-- Follow-up task from the review gate. Work from what is inlined here; the review record and the
named upstream files win over this summary. -->

# T27 — Answer firewall-rejected owner-API addresses with Problem Details shared.malformed_request

## Place in the sequence

- **Blocked by:** —
- **Blocks:** T28, T32
- **Origin:** review finding N2 / F-11 — [_review/review-2026-11-07-r2.md](../_review/review-2026-11-07-r2.md)

## Why

Review N2: under `/api/**` the firewall reject goes through `HttpStatusRequestRejectedHandler.sendError(400)`, so Boot's `BasicErrorController` answers `{timestamp,status,error,path}`. That breaks `openapi.yaml` `Problem.required` (`type`, `title`, `status`, `code`) and R3's resolution ("400 Problem Details kept for `/api/**`").

## Acceptance criteria

No AC of its own — contract in [openapi.yaml](../contracts/openapi.yaml); decisions in [adr/](../adr/).

## Checklist

- [ ] Route `/api/**` firewall rejects through the MVC `HandlerExceptionResolver`, so the single `ProblemDetailsAdvice` renders them (no second Problem writer).
- [ ] `ProblemDetailsAdvice` maps `RequestRejectedException` to 400 `application/problem+json`, `type` about:blank, `code: shared.malformed_request`, with no exception message in the body.
- [ ] `OwnerApiContractIT.everyOwnerErrorIsAProblem` includes `POST /api/v1/links/ab%2Fcdef/disable` and `POST /api/v1/links/abc2345;/enable`; the RedirectIT owner-API case asserts the Problem body.

## Definition of Done

- [ ] Firewall-rejected `/api/**` requests validate against `Problem`; visitor firewall paths unchanged; verify + spotless green.
- [ ] lint + vet clean (`./mvnw -B spotless:check`, `./mvnw -B verify`)
