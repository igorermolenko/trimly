---
id: T38
title: "Let browser-sent raw [ ] { } | ^ ` characters reach trimly instead of Tomcat's bare 400"
layer: "ports"
deps: []
blocks: []
acs: ["AC-07", "AC-16b"]
files_hint: ["src/main/resources/application.yml", "src/test/java/io/github/igorermolenko/trimly/redirect/api/RedirectIT.java"]
owner: "igorermolenko"
estimate: "S"
origin: "review _review/review-2026-11-07-r4.md Q1 / F-22"
status: "todo"
step: "—"
---

<!-- Follow-up task from the review gate. Work from what is inlined here; the review record and the
named upstream files win over this summary. -->

# T38 — Let browser-sent raw [ ] { } | ^ ` characters reach trimly instead of Tomcat's bare 400

## Place in the sequence

- **Blocked by:** —
- **Blocks:** —
- **Origin:** review finding Q1 / F-22 — [_review/review-2026-11-07-r4.md](../_review/review-2026-11-07-r4.md)

## Why

Review Q1: by default Tomcat rejects a request target that contains a raw `{ } | [ ] ^ \`` with its own 400, before the servlet runs. Browsers send these characters unencoded in the query (and `| [ ] ^` in the path). So a valid code opened as `/aB3dEf9?a[]=1` never redirects (AC-07: any query string is ignored), and `/abc|234` misses the plain-text "Link not found." (AC-16b).

## Acceptance criteria

AC-07, AC-16b — verbatim text in [spec.md](../spec.md) §5; contract in [openapi.yaml](../contracts/openapi.yaml); decisions in [adr/](../adr/).

## Checklist

- [ ] Set `server.tomcat.relaxed-query-chars` and `server.tomcat.relaxed-path-chars` in `application.yml`. Whatever the Spring Security firewall still rejects is answered by `VisitorRequestRejectedHandler`.
- [ ] Add `RedirectIT` cases sent over a raw socket (`java.net.URI` refuses these characters): a valid code with `?a[]=1` (and `{x}`, `|`, `^`, `` ` ``) → 302 with the exact `Location` and `no-store`; `/abc|234` → 404 "Link not found." with `no-store` and no storage lookup.

## Definition of Done

- [ ] RedirectIT green with the raw-socket cases; verify + spotless green.
- [ ] lint + vet clean (`./mvnw -B spotless:check`, `./mvnw -B verify`)
