---
id: T43
title: "Refuse to start with a public base URL that has a path, query, fragment or userinfo"
layer: "app"
deps: []
blocks: []
acs: ["AC-01", "AC-08"]
files_hint: ["src/main/java/io/github/igorermolenko/trimly/link/app/LinkProperties.java", "src/test/java/io/github/igorermolenko/trimly/link/app/LinkPropertiesTest.java"]
owner: "igorermolenko"
estimate: "XS"
origin: "review _review/review-2026-11-08.md R4 / F-27"
status: "done"
step: "COMMIT"
---

<!-- Follow-up task from the review gate. Work from what is inlined here; the review record and the
named upstream files win over this summary. -->

# T43 — Refuse to start with a public base URL that has a path, query, fragment or userinfo

## Place in the sequence

- **Blocked by:** —
- **Blocks:** —
- **Origin:** review finding R4 / F-27 — [_review/review-2026-11-08.md](../_review/review-2026-11-08.md)

## Why

Review R4: `TRIMLY_PUBLIC_BASE_URL=https://trimly.example.test/s` passes `LinkProperties`. Create then answers with `https://trimly.example.test/s/aB3dEf9`, a two-segment path that `RedirectController` answers with "Link not found.", so every link is dead (AC-01, AC-08). `https://trimly.example.test?x=1` produces `…?x=1/aB3dEf9`. sad.md §8 promises that the configuration is validated at startup, failing fast.

## Acceptance criteria

AC-01, AC-08 — verbatim text in [spec.md](../spec.md) §5.

## Checklist

- [x] `LinkProperties`: the base URL also has an empty path and no query, fragment or userinfo. Each `own-hosts` entry is a bare host name (no scheme, `/`, `:`, `@` or whitespace).
- [x] `LinkPropertiesTest`: `…/s`, `…?x=1`, `…#f`, `https://u@host` and an `own-hosts` entry `https://x.test` are refused; the existing valid cases stay green.

## Definition of Done

- [x] LinkPropertiesTest green; verify + spotless green.
