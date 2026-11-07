---
id: T37
title: "Sync sad.md, data-model.md, spec \u00a78 and api-sync-report F1 with what landed"
layer: "docs"
deps: []
blocks: []
acs: []
files_hint: ["docs/features/url-shortener/sad.md", "docs/features/url-shortener/data-model.md", "docs/features/url-shortener/spec.md", "docs/features/url-shortener/contracts/api-sync-report.md"]
owner: "igorermolenko"
estimate: "XS"
origin: "review _review/review-2026-11-07-r3.md P4, P5 / F-21"
status: "todo"
step: "—"
---

<!-- Follow-up task from the review gate. Work from what is inlined here; the review record and the
named upstream files win over this summary. -->

# T37 — Sync sad.md, data-model.md, spec §8 and api-sync-report F1 with what landed

## Place in the sequence

- **Blocked by:** —
- **Blocks:** —
- **Origin:** review finding P4, P5 / F-21 — [_review/review-2026-11-07-r3.md](../_review/review-2026-11-07-r3.md)

## Why

Review P4/P5: `sad.md:102` still says "the spec says 5 min" and the `sad.md:555` §11 risk row is open, though spec §6 says 15 minutes now (T31). `data-model.md:12` says the migration is not yet promoted, but `V2__create_short_link.sql` is live (T1). api-sync-report flag F1 (does a malformed create body need its own AC?) is open and never reached spec §8, though the default is implemented and tested.

## Acceptance criteria

— (contract / docs consistency; no §5 AC) — verbatim text in [spec.md](../spec.md) §5; contract in [openapi.yaml](../contracts/openapi.yaml); decisions in [adr/](../adr/).

## Checklist

- [ ] sad.md: drop the "spec says 5 min" clause; close the §11 row as resolved by T31; bump `updated_at`.
- [ ] data-model.md:12: say the migration was promoted as `V2__create_short_link.sql`; bump `updated_at`.
- [ ] spec.md §8: add F1 as resolved (default kept: 400 `shared.malformed_request`, tested; unknown members rejected per T35); bump `updated_at`.
- [ ] api-sync-report.md: mark F1 resolved; no open flags left.

## Definition of Done

- [ ] No doc contradicts the spec, the ADRs or the live code on these points.
- [ ] lint + vet clean (`./mvnw -B spotless:check`, `./mvnw -B verify`)
