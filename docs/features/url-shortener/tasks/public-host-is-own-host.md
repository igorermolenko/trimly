---
id: T19
title: "Count the public base URL host as an own host; no localhost defaults in production config"
layer: "app"
deps: ["T18"]
blocks: ["T20"]
acs: ["AC-06", "AC-01"]
files_hint: ["src/main/java/io/github/igorermolenko/trimly/link/app/LinkProperties.java", "src/main/resources/application.yml", "src/test/resources/config/application.yml", "src/test/java/io/github/igorermolenko/trimly/link/app/LinkPropertiesTest.java"]
owner: "igorermolenko"
estimate: "S"
origin: "review _review/review-2026-11-07.md R2 / F-2"
status: "todo"
step: "—"
---

<!-- Follow-up task from the review gate. Work from what is inlined here; the review record and the
named upstream files win over this summary. -->

# T19 — Count the public base URL host as an own host; no localhost defaults in production config

## Place in the sequence

- **Blocked by:** T18
- **Blocks:** T20
- **Origin:** review finding R2 / F-2 — [_review/review-2026-11-07.md](../_review/review-2026-11-07.md)

## Why

Review R2: `own-hosts` defaults to `[localhost]` and is never tied to `public-base-url`, so a production deploy that sets only `TRIMLY_PUBLIC_BASE_URL` accepts targets on trimly's own host (AC-06 loop); forgetting the base URL issues localhost links (AC-01).

## Acceptance criteria

AC-06, AC-01 — verbatim text in [spec.md](../spec.md) §5; contract in [openapi.yaml](../contracts/openapi.yaml); decisions in [adr/](../adr/).

## Checklist

- [ ] `LinkProperties.ownHosts()` always includes `publicBaseUrl.getHost()`.
- [ ] Move the localhost `trimly.public-base-url` / `trimly.own-hosts` values from `src/main/resources/application.yml` to the test config, so production refuses to start without them.
- [ ] Unit: the base URL host is an own host even when `own-hosts` omits it.

## Definition of Done

- [ ] LinkPropertiesTest covers the new rule; main application.yml has no localhost trimly defaults; verify + spotless green.
- [ ] lint + vet clean (`./mvnw -B spotless:check`, `./mvnw -B verify`)
