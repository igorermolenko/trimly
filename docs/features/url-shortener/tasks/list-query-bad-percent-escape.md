---
id: T45
title: "Answer a bad percent-escape in an owner-API query parameter with 400 shared.malformed_request, not 500"
layer: "ports"
deps: []
blocks: ["T46"]
acs: ["AC-09"]
files_hint: ["src/main/java/io/github/igorermolenko/trimly/shared/ProblemDetailsAdvice.java", "src/test/java/io/github/igorermolenko/trimly/shared/ProblemDetailsAdviceIT.java", "docs/features/url-shortener/contracts/openapi.yaml", "docs/features/url-shortener/contracts/api-sync-report.md"]
owner: "igorermolenko"
estimate: "XS"
origin: "review _review/review-2026-11-08-r2.md S1 / F-29"
status: "todo"
step: "—"
---

<!-- Follow-up task from the review gate. Work from what is inlined here; the review record and the
named upstream files win over this summary. -->

# T45 — Answer a bad percent-escape in an owner-API query parameter with 400 shared.malformed_request, not 500

## Place in the sequence

- **Blocked by:** —
- **Blocks:** T46 (both edit `openapi.yaml` and `api-sync-report.md`)
- **Origin:** review finding S1 / F-29 — [_review/review-2026-11-08-r2.md](../_review/review-2026-11-08-r2.md)

## Why

Review S1 (dates from T14). With a valid owner credential, `GET /api/v1/links?after=%zz` or `?limit=%` makes Tomcat 11.0.27 fail to decode the query. Every `getParameter*` call then throws `org.apache.tomcat.util.http.InvalidParameterException` (an `IllegalStateException`), without `sendError`. Spring's argument resolution propagates it, and `ProblemDetailsAdvice`'s catch-all answers 500 `shared.internal_error` and logs an ERROR stack trace. The contract (`openapi.yaml` list 400) promises a 400 for an invalid paging parameter. MockMvc (`ListShortLinksIT`) never runs Tomcat's parameter parser, so it cannot catch this.

## Acceptance criteria

AC-09 — verbatim text in [spec.md](../spec.md) §5. Contract: `openapi.yaml` list `400`.

## Checklist

- [ ] RED: raw-socket owner-API cases in `ProblemDetailsAdviceIT` (`GET /api/v1/links?after=%zz`, `?limit=%` with the owner credential) assert 400 `application/problem+json`, `code: shared.malformed_request`, the owner-API no-cache and nosniff headers, and no exception text.
- [ ] Map exactly `InvalidParameterException` (not the broad `IllegalStateException`) to 400 `shared.malformed_request` in `ProblemDetailsAdvice`, without an ERROR log.
- [ ] `openapi.yaml` list 400: name the trigger (a query parameter that is not validly percent-encoded); add a hand-edit note to `api-sync-report.md`.

## Definition of Done

- [ ] ProblemDetailsAdviceIT green with the new cases; verify + spotless green.
