---
id: T24
title: "Write the missing test-plan tests for AC-20b, AC-20, AC-19, AC-18, AC-11 and AC-04"
layer: "tests"
deps: ["T23"]
blocks: ["T25"]
acs: ["AC-20b", "AC-20", "AC-19", "AC-18", "AC-11", "AC-04"]
files_hint: ["src/test/java/io/github/igorermolenko/trimly/link/app/CreateShortLinkIT.java", "src/test/java/io/github/igorermolenko/trimly/link/infra/ShortLinkStoreIT.java", "src/test/java/io/github/igorermolenko/trimly/redirect/api/RedirectIT.java", "src/test/java/io/github/igorermolenko/trimly/access/api/OwnerCredentialFilterTest.java", "src/test/java/io/github/igorermolenko/trimly/LinkLifecycleIT.java", "src/test/java/io/github/igorermolenko/trimly/link/api/CreateShortLinkIT.java"]
owner: "igorermolenko"
estimate: "M"
origin: "review _review/review-2026-11-07.md R9 (a)-(f) / F-9"
status: "todo"
step: "—"
---

<!-- Follow-up task from the review gate. Work from what is inlined here; the review record and the
named upstream files win over this summary. -->

# T24 — Write the missing test-plan tests for AC-20b, AC-20, AC-19, AC-18, AC-11 and AC-04

## Place in the sequence

- **Blocked by:** T23
- **Blocks:** T25
- **Origin:** review finding R9 (a)-(f) / F-9 — [_review/review-2026-11-07.md](../_review/review-2026-11-07.md)

## Why

Review R9: the code satisfies these ACs, but the test-plan rows that pin them were never written, so a regression would go unnoticed.

## Acceptance criteria

AC-20b, AC-20, AC-19, AC-18, AC-11, AC-04 — verbatim text in [spec.md](../spec.md) §5; contract in [openapi.yaml](../contracts/openapi.yaml); decisions in [adr/](../adr/).

## Checklist

- [ ] (a) AC-20b ITs with real storage and a primed generator: 1 taken -> created; 4 taken then free -> created; 5 taken -> unavailable, nothing created.
- [ ] (b) AC-20: a taken code of an active or a disabled link is refused; the stored row is unchanged.
- [ ] (c) AC-19: `Authorization: Bearer wrong` on `/{code}` -> 302 to the target.
- [ ] (d) AC-18 unit: only the exact credential passes; prefix, credential+1 char, different case, empty -> rejected.
- [ ] (e) AC-11: after a real disable, the owner list shows the link `DISABLED`.
- [ ] (f) AC-04: the too-long URL test asserts nothing was created.
- [ ] Each new test is proven to bite by a temporary mutation of the code under test (then reverted).

## Definition of Done

- [ ] All new tests green and each shown red under a mutation; verify + spotless green.
- [ ] lint + vet clean (`./mvnw -B spotless:check`, `./mvnw -B verify`)
