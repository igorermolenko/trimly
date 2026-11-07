---
id: T31
title: "Sync spec.md with ADR-0006 and sad.md \u00a78 (15-minute probe, closed open questions)"
layer: "docs"
deps: []
blocks: []
acs: []
files_hint: ["docs/features/url-shortener/spec.md"]
owner: "igorermolenko"
estimate: "XS"
origin: "review _review/review-2026-11-07-r2.md N6 / F-15"
status: "todo"
step: "—"
---

<!-- Follow-up task from the review gate. Work from what is inlined here; the review record and the
named upstream files win over this summary. -->

# T31 — Sync spec.md with ADR-0006 and sad.md §8 (15-minute probe, closed open questions)

## Place in the sequence

- **Blocked by:** —
- **Blocks:** —
- **Origin:** review finding N6 / F-15 — [_review/review-2026-11-07-r2.md](../_review/review-2026-11-07-r2.md)

## Why

Review N6: spec §6 still measures availability with a 5-minute probe, and §8 open questions 1 (visitor rate limit) and 2 (probe interval) are unchecked, although `sad.md:474` and ADR-0006 already decided both.

## Acceptance criteria

No AC of its own — contract in [openapi.yaml](../contracts/openapi.yaml); decisions in [adr/](../adr/).

## Checklist

- [ ] §6 availability measurement: probe every 15 min, linked to ADR-0006.
- [ ] §8 questions 1 and 2 checked as resolved, with links to sad.md §8 and ADR-0006.
- [ ] Bump `updated_at`.

## Definition of Done

- [ ] spec.md consistent with sad.md §8 and ADR-0006; no AC text changed.
- [ ] lint + vet clean (`./mvnw -B spotless:check`, `./mvnw -B verify`)
