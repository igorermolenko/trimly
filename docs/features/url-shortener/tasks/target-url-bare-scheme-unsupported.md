---
id: T41
title: "Answer a bare https:// or http:// (no host) with the AC-03 answer, not the AC-05 encoding guidance"
layer: "domain"
deps: []
blocks: []
acs: ["AC-03", "AC-05"]
files_hint: ["src/main/java/io/github/igorermolenko/trimly/link/domain/TargetUrl.java", "src/test/java/io/github/igorermolenko/trimly/link/domain/TargetUrlTest.java"]
owner: "igorermolenko"
estimate: "XS"
origin: "review _review/review-2026-11-08.md R1 / F-25"
status: "done"
step: "COMMIT"
---

<!-- Follow-up task from the review gate. Work from what is inlined here; the review record and the
named upstream files win over this summary. -->

# T41 — Answer a bare https:// or http:// (no host) with the AC-03 answer, not the AC-05 encoding guidance

## Place in the sequence

- **Blocked by:** —
- **Blocks:** —
- **Origin:** review finding R1 / F-25 — [_review/review-2026-11-08.md](../_review/review-2026-11-08.md)

## Why

Review R1, a regression from T39: `TargetUrl.looksLikeWeb` checks only the scheme prefix. `new URI("https://")` throws "Expected authority", so `https://` and `http://` now get `link.target_url_not_encoded` ("must be percent-encoded"). AC-03 covers an address that is not an absolute web address with a host, and there is nothing in it to encode. `https://[::1` (an unclosed IPv6 host) also gets `NOT_ENCODED`.

## Acceptance criteria

AC-03, AC-05 — verbatim text in [spec.md](../spec.md) §5.

## Checklist

- [x] In the `catch` of `TargetUrl.webHost`, return `NOT_ENCODED` only when the text after `scheme://` has a non-empty authority (at least one character before the first `/`, `?` or `#`); otherwise return `UNSUPPORTED`.
- [x] `TargetUrlTest`: `https://`, `http://`, `HTTPS://` and `https:///path` give `UNSUPPORTED`; the T39 `NOT_ENCODED` cases stay green.

## Definition of Done

- [x] TargetUrlTest green; verify + spotless green.
