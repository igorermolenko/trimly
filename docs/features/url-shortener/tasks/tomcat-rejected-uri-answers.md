---
id: T42
title: "Answer an address Tomcat itself rejects (bad percent-escape) with \"Link not found.\" or a shared.malformed_request Problem"
layer: "ports"
deps: []
blocks: ["T44"]
acs: ["AC-16b"]
files_hint: ["src/main/java/io/github/igorermolenko/trimly/redirect/api/", "src/main/resources/application.yml", "src/test/java/io/github/igorermolenko/trimly/redirect/api/RedirectIT.java"]
owner: "igorermolenko"
estimate: "S"
origin: "review _review/review-2026-11-08.md R3 / F-26"
status: "todo"
step: "—"
---

<!-- Follow-up task from the review gate. Work from what is inlined here; the review record and the
named upstream files win over this summary. -->

# T42 — Answer an address Tomcat itself rejects with "Link not found." or a shared.malformed_request Problem

## Place in the sequence

- **Blocked by:** —
- **Blocks:** T44
- **Origin:** review finding R3 / F-26 — [_review/review-2026-11-08.md](../_review/review-2026-11-08.md)

## Why

Review R3: Tomcat rejects `GET /abc%zz`, `/abc%` (a browser sends a typed lone `%` as is), `/abc%2`, `/abc%00`, `/abc%5C` and `/%2e%2e/x` while it decodes or normalises the URI. This happens before any filter runs and before the `/error` dispatch, and the answer is Tomcat's own `text/html` 400 without `no-store`. Under `/api/...` the same requests get a 400 with no Problem `code`. The comment at `application.yml:51-54` wrongly says that everything still rejected reaches `VisitorRequestRejectedHandler`.

## Acceptance criteria

AC-16b — verbatim text in [spec.md](../spec.md) §5; openapi `/{shortCode}` 404 and the owner-API `Problem` in [openapi.yaml](../contracts/openapi.yaml).

## Checklist

- [ ] Install a custom Tomcat `ErrorReportValve` on the Host through a `WebServerFactoryCustomizer`, replacing the one Boot adds. When the server raises a 400 itself and nothing has been written yet, the valve writes:
  - outside `/api`: 404 `text/plain` "Link not found." with `Cache-Control: no-store`;
  - under `/api`: 400 `application/problem+json` with `code: shared.malformed_request`, plus the owner-API no-cache and nosniff headers.
  Every other status keeps Tomcat's default body-less behaviour (no server info, no report).
- [ ] Add raw-socket `RedirectIT` cases: `/abc%zz` and `/abc%` → 404 "Link not found.", `text/plain`, `no-store`, no lookup; `/api/v1/links/abc%zz/disable` with the credential → 400 `shared.malformed_request` Problem.
- [ ] Correct the comment in `application.yml`.

## Definition of Done

- [ ] RedirectIT green with the new cases; verify + spotless green.
