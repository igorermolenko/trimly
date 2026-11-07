---
id: T21
title: "Answer firewall-rejected visitor paths with the plain-text \"Link not found.\""
layer: "ports"
deps: []
blocks: ["T22"]
acs: ["AC-16b"]
files_hint: ["src/main/java/io/github/igorermolenko/trimly/redirect/api/VisitorRequestRejectedHandler.java", "src/test/java/io/github/igorermolenko/trimly/redirect/api/RedirectIT.java"]
owner: "igorermolenko"
estimate: "S"
origin: "review _review/review-2026-11-07.md R3 / F-3"
status: "todo"
step: "—"
---

<!-- Follow-up task from the review gate. Work from what is inlined here; the review record and the
named upstream files win over this summary. -->

# T21 — Answer firewall-rejected visitor paths with the plain-text "Link not found."

## Place in the sequence

- **Blocked by:** —
- **Blocks:** T22
- **Origin:** review finding R3 / F-3 — [_review/review-2026-11-07.md](../_review/review-2026-11-07.md)

## Why

Review R3: Spring Security's StrictHttpFirewall rejects `/aB3dEf9;`, `%25`, `%2F`, `//` paths before RedirectController, answering a bare 400 without `no-store`; the contract says every answer on `GET /{shortCode}` is text/plain with `no-store`.

## Acceptance criteria

AC-16b — verbatim text in [spec.md](../spec.md) §5; contract in [openapi.yaml](../contracts/openapi.yaml); decisions in [adr/](../adr/).

## Checklist

- [ ] A `RequestRejectedHandler` bean: outside `/api/**` answer 404 `text/plain;charset=UTF-8` "Link not found." with `Cache-Control: no-store`; under `/api/**` keep the default 400.
- [ ] RedirectIT: `/abc2345;`, `/abc2345;x=1`, `/abc%25345`, `/abc%2F2345`, `//abc2345` all give that 404.

## Definition of Done

- [ ] RedirectIT green incl. firewall cases; verify + spotless green.
- [ ] lint + vet clean (`./mvnw -B spotless:check`, `./mvnw -B verify`)
