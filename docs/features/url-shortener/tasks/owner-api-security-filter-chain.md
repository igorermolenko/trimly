---
id: T12
title: "Guard the owner API with a stateless bearer SecurityFilterChain and a Problem Details entry point"
layer: "ports"
deps: ["T11"]
blocks: ["T13", "T15"]
acs: ["AC-18", "AC-19"]
files_hint: ["pom.xml", "src/main/java/io/github/igorermolenko/trimly/access/api/", "src/test/java/io/github/igorermolenko/trimly/access/api/", "CLAUDE.md", "docs/architecture-map.md"]
owner: "igorermolenko"
estimate: "M"
context_budget: "M"     # 53 inlined lines
status: "todo"
step: ""
---

<!-- To the executing agent: work from what is inlined here. If a slice is insufficient, ambiguous,
or contradicts the code in front of you, open the named file for the full text and follow that.
Do not invent the missing part. Every quote ends with its provenance signature; the source wins. -->

# T12 — Guard the owner API with a stateless bearer SecurityFilterChain and a Problem Details entry point

## Place in the sequence

- **Blocked by:** T11 — Add the access module's OwnerCredentialPolicy and fail-fast OwnerCredentialProperties
- **Blocks:** T13 — Expose POST /api/v1/links and the Problem Details `code` mapping for owner errors · T15 — Serve GET /{shortCode}: non-reusable 302, plain-text 404/503, query ignored
- **Wave:** 2 — after T11 (longest path).
- **Lane:** shares files with T6 (`files_hint` overlap) — serialized by `implement`.

## Why (user story)

> **As an** owner
> **I want** only someone presenting the owner credential to be able to create, list, disable or re-enable short links
> **So that** nobody else can publish links on trimly's domain
>
> — `spec.md §4, US-07, verbatim` · full text: [spec.md](../spec.md)

This task puts the authentication boundary in front of every owner operation while leaving redirects open.

## Inlined context

> A stateless Spring Security filter chain in `access`. The owner API requires the owner credential as a bearer token, compared in constant time. Redirects need no credential. A missing and a wrong credential get an identical Problem Details answer. The credential is validated at startup, and trimly refuses to start without it
>
> — `sad.md §8, Authentication row, verbatim` · full text: [sad.md](../sad.md)

> **Chosen:** Option 1, `access` module + Spring Security.
>
> - Spring Security's defaults have to be switched off explicitly: form login, HTTP Basic, sessions, CSRF and the default login page. Its rejections bypass `shared.ProblemDetailsAdvice`, so `access` needs its own entry point that produces the same Problem Details shape.
> - `CLAUDE.md` and `docs/architecture-map.md` list three modules; they must be updated to add `access`.
>
> — `adr/0001-guard-owner-operations-with-spring-security-in-an-access-module.md §Decision outcome + §Consequences, abridged` · full text: [adr/0001-guard-owner-operations-with-spring-security-in-an-access-module.md](../adr/0001-guard-owner-operations-with-spring-security-in-an-access-module.md)

```text
access/                      NEW — owner authentication (ADR-0001)
├── api/       SecurityFilterChain (owner API: bearer owner credential; redirects: permitAll),
│              bearer-token authentication, Problem Details authentication entry point
├── app/       OwnerCredentialProperties — validated @ConfigurationProperties, fail-fast at startup
└── domain/    OwnerCredentialPolicy — ≥ 32 printable non-space ASCII, ≥ 16 distinct (plain Java)
```

— `sad.md §5, internal decomposition (access), verbatim` · full text: [sad.md](../sad.md)

> **Sanctioned exception:** "authentication required" is produced by `access`'s Spring Security authentication entry point, because the filter chain rejects the request before any controller runs. It builds the same Spring `ProblemDetail` type and shape (same fields, no stack trace or message details), and a test asserts the shape matches.
>
> — `sad.md §8, Error handling row, abridged` · full text: [sad.md](../sad.md)

> **Hard rule:** The owner credential and the `Authorization` header are never logged or echoed (spec §6.1).
>
> — `sad.md §8, Logging row, abridged` · full text: [sad.md](../sad.md)

**Fallback:** insufficient or contradicted by the code → read the named file in full
([spec.md](../spec.md) · [sad.md](../sad.md) · [data-model.md](../data-model.md) ·
[openapi.yaml](../contracts/openapi.yaml) · [adr/](../adr/)) and follow it. Do not guess.

## Data delta

No DB changes.

## API contract

- All `/api/v1/links…` operations: `security: BearerAuth`; `/{shortCode}`: `security: []`.
- `401 AuthenticationRequired`: `application/problem+json` `{type: about:blank, title: Unauthorized, status: 401, detail: "Authentication is required.", instance, code: access.authentication_required}`; header `WWW-Authenticate: Bearer` exactly — never an `error=` attribute. Missing and wrong credential: byte-identical.

— `contracts/openapi.yaml, operationId createShortLink, listShortLinks, disableShortLink, enableShortLink, followShortLink, abridged` · full text: [openapi.yaml](../contracts/openapi.yaml)

## Acceptance criteria

### AC-18 — authorization

> **Given** a person presents no owner credential, or a wrong one
> **When** they try to create, list, disable or re-enable short links
> **Then** the system denies the action, says authentication is required, creates or changes nothing, and reveals no link data; a missing credential and a wrong one get an identical answer
>
> — `spec.md §5, AC-18, verbatim` · full text: [spec.md](../spec.md)

### AC-19 — authorization

> **Given** a visitor presents no credential at all
> **When** the visitor opens an active short link
> **Then** the visitor is sent to its target URL; following a short link never requires a credential
>
> — `spec.md §5, AC-19, verbatim` · full text: [spec.md](../spec.md)

## Checklist

- [ ] `pom.xml`: add `spring-boot-starter-security` (+ `spring-security-test` test scope).
- [ ] `src/main/java/io/github/igorermolenko/trimly/access/api/`: `SecurityFilterChain` bean — stateless sessions, CSRF/form login/HTTP Basic/logout/default login page off; `/api/**` authenticated, everything else `permitAll`; a bearer filter/`AuthenticationProvider` comparing with `MessageDigest.isEqual` on UTF-8 bytes; `AuthenticationEntryPoint` writing the 401 Problem Details above.
- [ ] Do not let Security's cache-control header writer replace `Cache-Control: no-store` on redirects (disable `headers.cacheControl` or verify it leaves an explicitly set header — T15 asserts).
- [ ] `src/test/java/io/github/igorermolenko/trimly/access/api/OwnerApiSecurityIT.java`: no header vs `Bearer wrong` → identical status, headers and body bytes; `Bearer <valid>` passes the filter; `/{anything}` reachable without a header.
- [ ] `CLAUDE.md` §Modules and `docs/architecture-map.md`: add `access` (sad §11 doc-drift row).

## Edge cases

| Case | Behaviour |
|---|---|
| No `Authorization` header | 401 Problem Details, `WWW-Authenticate: Bearer`. |
| `Bearer <wrong>` / `Basic …` / `Bearer` with no token | Same 401, byte-identical to the missing case (AC-18). |
| Valid credential | Request proceeds to the controller. |
| Visitor `GET /aB3dEf9` with no header | Not intercepted by auth (AC-19). |
| Any request | No log line contains the credential or the `Authorization` header. |
| Session cookie | Never issued (stateless). |

## Definition of Done

- [ ] `OwnerApiSecurityIT` passes: missing vs wrong credential byte-identical 401 with `code: access.authentication_required`; visitor path needs no credential.
- [ ] `CLAUDE.md` and `docs/architecture-map.md` list `access`.
- [ ] `./mvnw -B verify` and `spotless:check` green.
- [ ] every Hard Rule inlined above still holds
- [ ] lint + vet clean (`./mvnw -B spotless:check`, `./mvnw -B verify`)
