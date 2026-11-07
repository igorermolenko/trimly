---
id: T36
title: "Give firewall-rejected owner-API answers the owner-API cache and nosniff headers"
layer: "ports"
deps: ["T33"]
blocks: []
acs: []
files_hint: ["src/main/java/io/github/igorermolenko/trimly/redirect/api/VisitorRequestRejectedHandler.java", "src/test/java/io/github/igorermolenko/trimly/redirect/api/RedirectIT.java"]
owner: "igorermolenko"
estimate: "XS"
origin: "review _review/review-2026-11-07-r3.md P6 / F-20"
status: "todo"
step: "—"
---

<!-- Follow-up task from the review gate. Work from what is inlined here; the review record and the
named upstream files win over this summary. -->

# T36 — Give firewall-rejected owner-API answers the owner-API cache and nosniff headers

## Place in the sequence

- **Blocked by:** T33
- **Blocks:** —
- **Origin:** review finding P6 / F-20 — [_review/review-2026-11-07-r3.md](../_review/review-2026-11-07-r3.md)

## Why

Review P6: the firewall rejects the request before Spring Security's `HeaderWriterFilter` runs, so the 400 Problem for an address like `/api/v1/links;x=1` has no `Cache-Control: no-cache, no-store, max-age=0, must-revalidate` and no `X-Content-Type-Options: nosniff`, unlike every other owner-API answer (round-1 R7).

## Acceptance criteria

— (contract / docs consistency; no §5 AC) — verbatim text in [spec.md](../spec.md) §5; contract in [openapi.yaml](../contracts/openapi.yaml); decisions in [adr/](../adr/).

## Checklist

- [ ] In `VisitorRequestRejectedHandler`, write Spring Security's cache-control and content-type-options headers before handing `/api` rejections to the resolver.
- [ ] Assert both headers in `RedirectIT.anOwnerApiAddressTheFirewallRejectsIsNotAVisitorAnswer`.

## Definition of Done

- [ ] RedirectIT green with both headers asserted; verify + spotless green.
- [ ] lint + vet clean (`./mvnw -B spotless:check`, `./mvnw -B verify`)
