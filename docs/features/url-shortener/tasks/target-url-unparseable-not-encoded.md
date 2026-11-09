---
id: T39
title: "Answer an unparseable http(s) target URL with the AC-05 percent-encoding guidance"
layer: "domain"
deps: []
blocks: []
acs: ["AC-03", "AC-05"]
files_hint: ["src/main/java/io/github/igorermolenko/trimly/link/domain/TargetUrl.java", "src/test/java/io/github/igorermolenko/trimly/link/domain/TargetUrlTest.java"]
owner: "igorermolenko"
estimate: "XS"
origin: "review _review/review-2026-11-07-r4.md Q2 / F-23"
status: "done"
step: "COMMIT"
---

<!-- Follow-up task from the review gate. Work from what is inlined here; the review record and the
named upstream files win over this summary. -->

# T39 — Answer an unparseable http(s) target URL with the AC-05 percent-encoding guidance

## Place in the sequence

- **Blocked by:** —
- **Blocks:** —
- **Origin:** review finding Q2 / F-23 — [_review/review-2026-11-07-r4.md](../_review/review-2026-11-07-r4.md)

## Why

Review Q2: `TargetUrl.webHost` maps every `URISyntaxException` to `Reason.UNSUPPORTED`. So an https address with printable ASCII that `java.net.URI` refuses (`[ ] { } | ^ \` " < > \`, a bad escape such as `%zz`) gets "Only absolute http or https web addresses are accepted.", which is misleading. AC-05's guidance ("other characters percent-encoded") is the one that fits.

## Acceptance criteria

AC-03, AC-05 — verbatim text in [spec.md](../spec.md) §5.

## Checklist

- [x] An http(s) address (by its scheme prefix, ignoring case) that `java.net.URI` cannot parse is rejected as `Reason.NOT_ENCODED`; any other unparseable text stays `Reason.UNSUPPORTED`.
- [x] `TargetUrlTest` cases for `https://example.test/a|b`, `https://example.test/100%`, `%zz`, `{x}`, `"`, a second `#` → `NOT_ENCODED`; unparseable non-web text stays `UNSUPPORTED`; the existing AC-03 cases stay green. (`https://example.test/?a[]=1` turned out to parse, so it is accepted exactly as submitted and pinned in the accepted list.)

## Definition of Done

- [x] TargetUrlTest green; verify + spotless green.
- [x] lint + vet clean (`./mvnw -B spotless:check`, `./mvnw -B verify`)
