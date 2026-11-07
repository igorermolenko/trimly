---
id: T35
title: "Reject unknown members in a create request as shared.malformed_request"
layer: "ports"
deps: ["T34"]
blocks: []
acs: ["AC-03"]
files_hint: ["src/main/resources/application.yml", "src/test/java/io/github/igorermolenko/trimly/link/api/CreateShortLinkIT.java", "src/test/java/io/github/igorermolenko/trimly/link/api/OwnerApiContractIT.java"]
owner: "igorermolenko"
estimate: "S"
origin: "review _review/review-2026-11-07-r3.md P3 / F-19"
status: "done"
step: "COMMIT"
---

<!-- Follow-up task from the review gate. Work from what is inlined here; the review record and the
named upstream files win over this summary. -->

# T35 — Reject unknown members in a create request as shared.malformed_request

## Place in the sequence

- **Blocked by:** T34
- **Blocks:** —
- **Origin:** review finding P3 / F-19 — [_review/review-2026-11-07-r3.md](../_review/review-2026-11-07-r3.md)

## Why

Review P3: `openapi.yaml:461-466` declares `ShortLinkCreate.additionalProperties: false`, but the JSON mapper ignores unknown members. `{"targetUrl":…,"extra":1}` gets a 201, and the typo `{"targetURL":…}` gets the misleading `link.target_url_unsupported`.

## Acceptance criteria

AC-03 — verbatim text in [spec.md](../spec.md) §5; contract in [openapi.yaml](../contracts/openapi.yaml); decisions in [adr/](../adr/).

## Checklist

- [ ] Make request-body binding fail on unknown members, so they get 400 `shared.malformed_request`.
- [ ] Add `CreateShortLinkIT` cases for an extra member and for the `targetURL` typo; nothing is stored.
- [ ] Validate the answer against `Problem` in `OwnerApiContractIT`.

## Definition of Done

- [ ] CreateShortLinkIT and OwnerApiContractIT green with the new cases; verify + spotless green.
- [ ] lint + vet clean (`./mvnw -B spotless:check`, `./mvnw -B verify`)
