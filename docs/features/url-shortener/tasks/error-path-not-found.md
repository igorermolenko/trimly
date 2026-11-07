---
id: T33
title: "Answer a direct GET /error with the plain-text \"Link not found.\""
layer: "ports"
deps: []
blocks: []
acs: ["AC-16b"]
files_hint: ["src/main/java/io/github/igorermolenko/trimly/redirect/api/VisitorErrorController.java", "src/test/java/io/github/igorermolenko/trimly/redirect/api/RedirectIT.java"]
owner: "igorermolenko"
estimate: "S"
origin: "review _review/review-2026-11-07-r3.md P1 / F-17"
status: "todo"
step: "—"
---

<!-- Follow-up task from the review gate. Work from what is inlined here; the review record and the
named upstream files win over this summary. -->

# T33 — Answer a direct GET /error with the plain-text "Link not found."

## Place in the sequence

- **Blocked by:** —
- **Blocks:** —
- **Origin:** review finding P1 / F-17 — [_review/review-2026-11-07-r3.md](../_review/review-2026-11-07-r3.md)

## Why

Review P1: Boot's `BasicErrorController` owns the literal `/error` path, which beats `/{segment}`. A direct `GET /error` gets HTTP 500 `{"status":999,"error":"None",…}` instead of the visitor's plain-text answer. Spec §3 says visitors only ever get a redirect or a plain-text sentence, and AC-16b says a wrong code is "link not found".

## Acceptance criteria

AC-16b — verbatim text in [spec.md](../spec.md) §5; contract in [openapi.yaml](../contracts/openapi.yaml); decisions in [adr/](../adr/).

## Checklist

- [ ] Replace `BasicErrorController` with an `ErrorController` in `redirect.api`. A direct request (no `jakarta.servlet.error.status_code` attribute) gets `VisitorAnswer.Message.NOT_FOUND` (text/plain, `no-store`) without a storage lookup.
- [ ] A real error dispatch keeps a Problem Details answer with a `code`, rendered by the shared advice.
- [ ] Add `/error` to the malformed cases in `RedirectIT`.

## Definition of Done

- [ ] RedirectIT green including `/error`; verify + spotless green.
- [ ] lint + vet clean (`./mvnw -B spotless:check`, `./mvnw -B verify`)
