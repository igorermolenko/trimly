---
id: T47
title: "Pin https:///path as UNSUPPORTED in TargetUrlTest"
layer: "domain"
deps: []
blocks: []
acs: ["AC-03"]
files_hint: ["src/test/java/io/github/igorermolenko/trimly/link/domain/TargetUrlTest.java"]
owner: "igorermolenko"
estimate: "XS"
origin: "review _review/review-2026-11-08-r2.md S3 / F-31"
status: "todo"
step: "—"
---

<!-- Follow-up task from the review gate. Work from what is inlined here; the review record and the
named upstream files win over this summary. -->

# T47 — Pin https:///path as UNSUPPORTED in TargetUrlTest

## Place in the sequence

- **Blocked by:** —
- **Blocks:** —
- **Origin:** review finding S3 / F-31 — [_review/review-2026-11-08-r2.md](../_review/review-2026-11-08-r2.md)

## Why

Review S3. The T41 checklist is ticked with "`https:///path` gives `UNSUPPORTED`", but `TargetUrlTest` has no such case. The behaviour is already correct (`java.net.URI` parses it and finds no host), so this test passes on its first run. That first-run pass is expected: the test pins existing behaviour, it does not drive new code.

## Acceptance criteria

AC-03 — verbatim text in [spec.md](../spec.md) §5.

## Checklist

- [ ] Add `"https:///path"` to the `@ValueSource` of `aWebAddressWithoutAUsableHostIsUnsupportedNotNotEncoded`.

## Definition of Done

- [ ] TargetUrlTest green; verify + spotless green.
