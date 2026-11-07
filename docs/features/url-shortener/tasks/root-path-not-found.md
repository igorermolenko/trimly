---
id: T28
title: "Answer GET / with the plain-text \"Link not found.\""
layer: "ports"
deps: ["T27"]
blocks: []
acs: ["AC-16b"]
files_hint: ["src/main/java/io/github/igorermolenko/trimly/redirect/api/RedirectController.java", "src/test/java/io/github/igorermolenko/trimly/redirect/api/RedirectIT.java"]
owner: "igorermolenko"
estimate: "S"
origin: "review _review/review-2026-11-07-r2.md N3 / F-12"
status: "todo"
step: "—"
---

<!-- Follow-up task from the review gate. Work from what is inlined here; the review record and the
named upstream files win over this summary. -->

# T28 — Answer GET / with the plain-text "Link not found."

## Place in the sequence

- **Blocked by:** T27
- **Blocks:** —
- **Origin:** review finding N3 / F-12 — [_review/review-2026-11-07-r2.md](../_review/review-2026-11-07-r2.md)

## Why

Review N3: `/{segment}` does not match an empty segment, so `GET /` ends in `NoResourceFoundException` and gets a JSON Problem `shared.not_found`. Spec §3 says visitors only ever get a redirect or a plain-text sentence, and AC-16b says a code of the wrong length is "link not found".

## Acceptance criteria

AC-16b — verbatim text in [spec.md](../spec.md) §5; contract in [openapi.yaml](../contracts/openapi.yaml); decisions in [adr/](../adr/).

## Checklist

- [ ] Map `/` in `RedirectController` to `VisitorAnswer.Message.NOT_FOUND` (text/plain, `no-store`), without a storage lookup.
- [ ] Add `/` to the malformed cases in `RedirectIT`.

## Definition of Done

- [ ] RedirectIT green including `/`; verify + spotless green.
- [ ] lint + vet clean (`./mvnw -B spotless:check`, `./mvnw -B verify`)
