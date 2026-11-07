---
id: T32
title: "Record the shared.* Problem codes in api-sync-report.md"
layer: "docs"
deps: ["T27"]
blocks: []
acs: []
files_hint: ["docs/features/url-shortener/contracts/api-sync-report.md"]
owner: "igorermolenko"
estimate: "XS"
origin: "review _review/review-2026-11-07-r2.md N7 / F-16"
status: "todo"
step: "—"
---

<!-- Follow-up task from the review gate. Work from what is inlined here; the review record and the
named upstream files win over this summary. -->

# T32 — Record the shared.* Problem codes in api-sync-report.md

## Place in the sequence

- **Blocked by:** T27
- **Blocks:** —
- **Origin:** review finding N7 / F-16 — [_review/review-2026-11-07-r2.md](../_review/review-2026-11-07-r2.md)

## Why

Review N7: `openapi.yaml:427-433` now lists 14 `Problem.code` values (R5 added four `shared.*` fallbacks), but `api-sync-report.md:48` still says 10, and `:85` still says the unexpected 500 has no `code`.

## Acceptance criteria

No AC of its own — contract in [openapi.yaml](../contracts/openapi.yaml); decisions in [adr/](../adr/).

## Checklist

- [ ] Update Section B item 2 to the 14 codes and note the `shared.*` fallbacks from review R5 (and T27's firewall reject).
- [ ] Update the "Unexpected 500s" note: the 500 carries `shared.internal_error`.
- [ ] Add a short reconcile note dated 2026-11-07.

## Definition of Done

- [ ] api-sync-report.md matches openapi.yaml.
- [ ] lint + vet clean (`./mvnw -B spotless:check`, `./mvnw -B verify`)
