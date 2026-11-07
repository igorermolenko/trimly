---
id: T26
title: "Prove a storage wake within 2 s still gives the visitor a normal redirect"
layer: "tests"
deps: []
blocks: []
acs: ["AC-17"]
files_hint: ["src/test/java/io/github/igorermolenko/trimly/StorageOutageIT.java"]
owner: "igorermolenko"
estimate: "S"
origin: "review _review/review-2026-11-07-r2.md N1 / F-10"
status: "todo"
step: "—"
---

<!-- Follow-up task from the review gate. Work from what is inlined here; the review record and the
named upstream files win over this summary. -->

# T26 — Prove a storage wake within 2 s still gives the visitor a normal redirect

## Place in the sequence

- **Blocked by:** —
- **Blocks:** —
- **Origin:** review finding N1 / F-10 — [_review/review-2026-11-07-r2.md](../_review/review-2026-11-07-r2.md)

## Why

Review N1: the slow-wake case in `StorageTimeBudgetIT` calls `store.page()` directly, so the visitor path (`RedirectController` → `FollowShortLink` → `LinkLookup`) is never timed through a wake. Its 1100 ms wake also leaves only about 300 ms against the 1400 ms acquire limit. `test-plan.md:63` asks that slow but answering storage "still produces the normal redirect".

## Acceptance criteria

AC-17 — verbatim text in [spec.md](../spec.md) §5; contract in [openapi.yaml](../contracts/openapi.yaml); decisions in [adr/](../adr/).

## Checklist

- [ ] HTTP-level IT in `StorageOutageIT`: store a link, empty the Hikari pool, pause storage, unpause after about 1.0 s, `GET /{code}`.
- [ ] Assert 302, the exact `Location`, `Cache-Control: no-store`, and that the answer came within 2 s.
- [ ] Prove the test can fail: with the round-1 acquire limit (950 ms) it must go red.

## Definition of Done

- [ ] StorageOutageIT green including the slow-wake redirect case, and red with `connection-timeout: 950`; verify + spotless green.
- [ ] lint + vet clean (`./mvnw -B spotless:check`, `./mvnw -B verify`)
