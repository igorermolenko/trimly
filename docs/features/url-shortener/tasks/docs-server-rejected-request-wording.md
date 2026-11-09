---
id: T46
title: "Describe the valve's trigger as any request the server itself rejects, not only a bad address"
layer: "docs"
deps: ["T45"]
blocks: []
acs: ["AC-16b"]
files_hint: ["src/main/java/io/github/igorermolenko/trimly/redirect/api/ServerRejectedAddressValve.java", "src/main/resources/application.yml", "docs/features/url-shortener/contracts/openapi.yaml", "docs/features/url-shortener/contracts/api-sync-report.md"]
owner: "igorermolenko"
estimate: "XS"
origin: "review _review/review-2026-11-08-r2.md S2 / F-30"
status: "done"
step: "COMMIT"
---

<!-- Follow-up task from the review gate. Work from what is inlined here; the review record and the
named upstream files win over this summary. -->

# T46 — Describe the valve's trigger as any request the server itself rejects, not only a bad address

## Place in the sequence

- **Blocked by:** T45 (same contract files)
- **Blocks:** —
- **Origin:** review finding S2 / F-30 — [_review/review-2026-11-08-r2.md](../_review/review-2026-11-08-r2.md)

## Why

Review S2. `ServerRejectedAddressValve` rewrites every body-less 400 Tomcat raises itself. That covers a missing or duplicate `Host` header and an unreadable `Content-Length`, as well as bad addresses. The javadoc, the `MalformedAddress` contract response and the yml comment say "while decoding it". The user chose to keep the behaviour (consistent with hide-existence) and reword the docs. **No code change.**

## Acceptance criteria

AC-16b — verbatim text in [spec.md](../spec.md) §5.

## Checklist

- [x] `ServerRejectedAddressValve` javadoc and the comment at the 400 check: "a request the server itself rejects before trimly runs (a bad address, a missing or duplicate Host, an unreadable Content-Length)".
- [x] `openapi.yaml`: `MalformedAddress` description and the visitor `/{shortCode}` 404 description say the same.
- [x] `application.yml` comment above `server.tomcat` says the same.
- [x] `api-sync-report.md`: hand-edit note.

## Definition of Done

- [x] No doc claims a narrower trigger than the code; verify + spotless green.
