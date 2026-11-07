---
id: T40
title: "List every shared.malformed_request trigger in openapi.yaml and close the stale sad.md §11 doc-drift risk"
layer: "docs"
deps: []
blocks: []
acs: []
files_hint: ["docs/features/url-shortener/contracts/openapi.yaml", "docs/features/url-shortener/contracts/api-sync-report.md", "docs/features/url-shortener/sad.md"]
owner: "igorermolenko"
estimate: "XS"
origin: "review _review/review-2026-11-07-r4.md Q3, Q4 / F-24"
status: "todo"
step: "—"
---

<!-- Follow-up task from the review gate. Work from what is inlined here; the review record and the
named upstream files win over this summary. -->

# T40 — List every shared.malformed_request trigger in openapi.yaml and close the stale sad.md §11 doc-drift risk

## Place in the sequence

- **Blocked by:** —
- **Blocks:** —
- **Origin:** review findings Q3, Q4 / F-24 — [_review/review-2026-11-07-r4.md](../_review/review-2026-11-07-r4.md)

## Why

Review Q3: `openapi.yaml:86` says `shared.malformed_request` means only "the body is not a JSON object". The code also uses it for a wrong or missing Content-Type (T23), an unknown or misspelt member (T35) and an owner-API address the firewall rejects (T27). Review Q4: `sad.md:561` §11 still lists the CLAUDE.md / architecture-map drift as an open risk, though 6e6d643 resolved it.

## Checklist

- [ ] `openapi.yaml:86` lists all four triggers; `api-sync-report.md` records the hand edit.
- [ ] `sad.md:561`: strike the row through, mark it "Resolved 2026-11-07 (6e6d643)" as the row at `:555` is, and bump `updated_at`.

## Definition of Done

- [ ] No doc contradicts the code on these points.
