---
id: T44
title: "Document the shared 400 on list/disable/enable, the required TRIMLY_* config vars, and the 23-AC count"
layer: "docs"
deps: ["T42"]
blocks: []
acs: []
files_hint: ["docs/features/url-shortener/contracts/openapi.yaml", "docs/features/url-shortener/contracts/api-sync-report.md", "CLAUDE.md", "docs/architecture-map.md", "docs/features/url-shortener/test-plan.md"]
owner: "igorermolenko"
estimate: "XS"
origin: "review _review/review-2026-11-08.md R2, R5 / F-28"
status: "done"
step: "COMMIT"
---

<!-- Follow-up task from the review gate. Work from what is inlined here; the review record and the
named upstream files win over this summary. -->

# T44 — Document the shared 400 on list/disable/enable, the required TRIMLY_* config vars, and the 23-AC count

## Place in the sequence

- **Blocked by:** T42 (the contract describes the Tomcat-level 400 that T42 turns into a Problem)
- **Blocks:** —
- **Origin:** review findings R2, R5 / F-28 — [_review/review-2026-11-08.md](../_review/review-2026-11-08.md)

## Why

Review R2: the firewall-rejected 400 `shared.malformed_request` is documented only on create. Disable and enable have no 400, and the list operation's 400 names only the paging codes. Review R5: production refuses to start without `TRIMLY_PUBLIC_BASE_URL` and `TRIMLY_OWN_HOSTS`, yet CLAUDE.md and the architecture map do not list them; test-plan.md says 22 ACs where spec §5 has 23.

## Checklist

- [x] `openapi.yaml`: a shared `components/responses` 400 (`shared.malformed_request`: an address the firewall or the server rejects), referenced from list, disable and enable; `api-sync-report.md` records the hand edit.
- [x] `CLAUDE.md` Config bullet and `docs/architecture-map.md` §Conventions name `TRIMLY_PUBLIC_BASE_URL` and `TRIMLY_OWN_HOSTS`.
- [x] `test-plan.md`: 22 → 23 acceptance criteria.

## Definition of Done

- [x] No doc contradicts the code on these points; OwnerApiContractIT still green.
