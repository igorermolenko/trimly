---
id: T48
title: "Name every trigger of the owner-API unreadable-parameters 400, and pin ?=x"
layer: "docs"
deps: []
blocks: []
acs: ["AC-09"]
files_hint: ["src/main/java/io/github/igorermolenko/trimly/shared/ProblemDetailsAdvice.java", "src/test/java/io/github/igorermolenko/trimly/shared/ProblemDetailsAdviceIT.java", "docs/features/url-shortener/contracts/openapi.yaml", "docs/features/url-shortener/contracts/api-sync-report.md"]
owner: "igorermolenko"
estimate: "XS"
origin: "review _review/review-2026-11-09.md T1 / F-32"
status: "todo"
step: "SELECT"
---

<!-- Follow-up task from the review gate. Work from what is inlined here; the review record and the
named upstream files win over this summary. -->

# T48 — Name every trigger of the owner-API unreadable-parameters 400, and pin ?=x

## Place in the sequence

- **Blocked by:** —
- **Blocks:** —
- **Origin:** review finding T1 / F-32 — [_review/review-2026-11-09.md](../_review/review-2026-11-09.md)

## Why

Review T1. `ProblemDetailsAdvice` answers every Tomcat `InvalidParameterException` with 400
`shared.malformed_request`. Besides a query that is not validly percent-encoded, Tomcat 11.0.27
throws it for a parameter with an empty name (`?=x`), more than `maxParameterCount` parameters, and an
unreadable or oversized urlencoded form body. The javadoc, the openapi list 400 and the api-sync note
name only the first. The javadoc's "every parameter read" is also wrong: a query decode failure
throws only on the first read. **No behaviour change.**

## Acceptance criteria

AC-09 — verbatim text in [spec.md](../spec.md) §5.

## Checklist

- [ ] `ProblemDetailsAdviceIT`: `?=x` answers 400 `shared.malformed_request` with the owner-API headers.
- [ ] `ProblemDetailsAdvice` javadoc and the IT comment: "the server cannot read the request's parameters" + the four triggers; "the first parameter read throws".
- [ ] `openapi.yaml` list 400: the same wording.
- [ ] `api-sync-report.md`: correct the T45 note ("every parameter read") and add a hand-edit note.

## Definition of Done

- [ ] No doc claims a narrower trigger than the code; verify + spotless green.
