---
id: T11
title: "Add the access module's OwnerCredentialPolicy and fail-fast OwnerCredentialProperties"
layer: "domain"
deps: []
blocks: ["T12"]
acs: ["AC-18"]
files_hint: ["src/main/java/io/github/igorermolenko/trimly/access/package-info.java", "src/main/java/io/github/igorermolenko/trimly/access/domain/OwnerCredentialPolicy.java", "src/main/java/io/github/igorermolenko/trimly/access/app/OwnerCredentialProperties.java", "src/test/java/io/github/igorermolenko/trimly/access/domain/OwnerCredentialPolicyTest.java", "src/test/java/io/github/igorermolenko/trimly/access/app/OwnerCredentialPropertiesTest.java"]
owner: "igorermolenko"
estimate: "S"
context_budget: "S"     # 39 inlined lines
status: "done"
step: "COMMIT"
---

<!-- To the executing agent: work from what is inlined here. If a slice is insufficient, ambiguous,
or contradicts the code in front of you, open the named file for the full text and follow that.
Do not invent the missing part. Every quote ends with its provenance signature; the source wins. -->

# T11 — Add the access module's OwnerCredentialPolicy and fail-fast OwnerCredentialProperties

## Place in the sequence

- **Blocked by:** nothing — starts in wave 1
- **Blocks:** T12 — Guard the owner API with a stateless bearer SecurityFilterChain and a Problem Details entry point
- **Wave:** 1 — no dependencies.
- **Lane:** own lane.

## Why (user story)

> **As an** owner
> **I want** only someone presenting the owner credential to be able to create, list, disable or re-enable short links
> **So that** nobody else can publish links on trimly's domain
>
> — `spec.md §4, US-07, verbatim` · full text: [spec.md](../spec.md)

This task guarantees trimly never runs with a missing or weak owner credential.

## Inlined context

```text
access/                      NEW — owner authentication (ADR-0001)
├── api/       SecurityFilterChain (owner API: bearer owner credential; redirects: permitAll),
│              bearer-token authentication, Problem Details authentication entry point
├── app/       OwnerCredentialProperties — validated @ConfigurationProperties, fail-fast at startup
└── domain/    OwnerCredentialPolicy — ≥ 32 printable non-space ASCII, ≥ 16 distinct (plain Java)
```

— `sad.md §5, internal decomposition (access), verbatim` · full text: [sad.md](../sad.md)

> The owner generates the owner credential randomly (for example with a password manager). trimly cannot verify randomness, so at startup it checks a concrete rule instead: at least 32 characters, printable ASCII only with no spaces, and at least 16 distinct characters. trimly refuses to start if the credential is missing or breaks this rule. The credential is never written to logs or shown in any response. A missing credential and a wrong one get an identical answer (AC-18). To rotate it, the owner sets a new value and restarts trimly; the old value stops working immediately.
>
> — `spec.md §6.1, AuthZ/AuthN impact, abridged` · full text: [spec.md](../spec.md)

> **Hard rule:** `domain` has no Spring or persistence imports. Modules call only each other's published `api`/`app` types, through direct method calls. `ModularityTest` (Spring Modulith `verify()`) fails the build on any violation.
>
> — `sad.md §2, Technical constraints (Architecture bullet), abridged` · full text: [sad.md](../sad.md)

> The owner credential comes from an env var only. [...] Everything is validated at startup, failing fast.
>
> — `sad.md §8, Configuration row, abridged` · full text: [sad.md](../sad.md)

**Fallback:** insufficient or contradicted by the code → read the named file in full
([spec.md](../spec.md) · [sad.md](../sad.md) · [data-model.md](../data-model.md) ·
[openapi.yaml](../contracts/openapi.yaml) · [adr/](../adr/)) and follow it. Do not guess.

## Data delta

No DB changes.

## API contract

Internal — no API surface.

## Acceptance criteria

### AC-18 — authorization

> **Given** a person presents no owner credential, or a wrong one
> **When** they try to create, list, disable or re-enable short links
> **Then** the system denies the action, says authentication is required, creates or changes nothing, and reveals no link data; a missing credential and a wrong one get an identical answer
>
> — `spec.md §5, AC-18, verbatim` · full text: [spec.md](../spec.md)

## Checklist

- [ ] `src/main/java/io/github/igorermolenko/trimly/access/package-info.java`: declare the new module (mirror `link/package-info.java`).
- [ ] `src/main/java/io/github/igorermolenko/trimly/access/domain/OwnerCredentialPolicy.java`: plain Java; `check(String)` → violations: missing/blank, < 32 chars, any char outside `0x21–0x7E`, < 16 distinct chars. Messages never include the value.
- [ ] `src/main/java/io/github/igorermolenko/trimly/access/app/OwnerCredentialProperties.java`: `@ConfigurationProperties("trimly")` binding `owner-credential` (env `TRIMLY_OWNER_CREDENTIAL` — name proposed here, not fixed upstream), validated with the policy at startup; `toString()` redacts.
- [ ] Tests: `src/test/java/io/github/igorermolenko/trimly/access/domain/OwnerCredentialPolicyTest.java`, `src/test/java/io/github/igorermolenko/trimly/access/app/OwnerCredentialPropertiesTest.java` (`ApplicationContextRunner`: missing/weak → context fails; valid → starts).
- [ ] Give the IT/test-run profile a valid test credential (test resources / `TestTrimlyApplication`), never in `src/main/resources`.

## Edge cases

| Case | Behaviour |
|---|---|
| Unset / blank | Startup fails; error names the property, not a value. |
| 31 chars / 32 chars with 15 distinct | Startup fails. |
| Contains a space or non-ASCII | Startup fails. |
| 32+ printable chars, ≥ 16 distinct | Starts. |
| Failure message / `toString()` | Never contains the credential (spec §6.1). |

## Definition of Done

- [ ] `OwnerCredentialPolicyTest` covers every Edge-case row.
- [ ] `OwnerCredentialPropertiesTest` shows the context refuses to start on missing/weak and starts on valid.
- [ ] `ModularityTest` recognises `access`; `ApplicationBootIT` still green with the test credential.
- [ ] every Hard Rule inlined above still holds
- [ ] lint + vet clean (`./mvnw -B spotless:check`, `./mvnw -B verify`)
