---
id: T13
title: "Expose POST /api/v1/links and the Problem Details `code` mapping for owner errors"
layer: "ports"
deps: ["T7", "T12"]
blocks: ["T14"]
acs: ["AC-01", "AC-03", "AC-04", "AC-05", "AC-06", "AC-17b"]
files_hint: ["src/main/java/io/github/igorermolenko/trimly/link/api/CreateShortLinkController.java", "src/main/java/io/github/igorermolenko/trimly/link/api/ShortLinkResponse.java", "src/main/java/io/github/igorermolenko/trimly/link/api/LinkProblems.java", "src/main/java/io/github/igorermolenko/trimly/shared/ProblemDetailsAdvice.java", "src/test/java/io/github/igorermolenko/trimly/link/api/CreateShortLinkIT.java"]
owner: "igorermolenko"
estimate: "M"
context_budget: "M"     # 65 inlined lines
status: "done"
step: "COMMIT"
---

<!-- To the executing agent: work from what is inlined here. If a slice is insufficient, ambiguous,
or contradicts the code in front of you, open the named file for the full text and follow that.
Do not invent the missing part. Every quote ends with its provenance signature; the source wins. -->

# T13 — Expose POST /api/v1/links and the Problem Details `code` mapping for owner errors

## Place in the sequence

- **Blocked by:** T7 — Implement the CreateShortLink use case with 5-attempt insert-retry and public-base-url · T12 — Guard the owner API with a stateless bearer SecurityFilterChain and a Problem Details entry point
- **Blocks:** T14 — Expose GET /api/v1/links and POST …/{shortCode}/disable|enable
- **Wave:** 4 — after T7, T12 (longest path).
- **Lane:** shares files with T14 (`files_hint` overlap) — serialized by `implement`.

## Why (user story)

> **As an** owner
> **I want** to submit a target URL and get back a new short link
> **So that** I can share a clean link that is easy to type by hand
>
> — `spec.md §4, US-01, verbatim` · full text: [spec.md](../spec.md)

This task is the owner's HTTP entry point for creating a link and sets the error-code pattern every owner endpoint reuses.

## Inlined context

> **Owner path:** `link.domain` signals rule breaks (AC-03…AC-06), unknown codes (AC-13) and storage unavailability (AC-17b). `link.api` turns them into Spring `ErrorResponseException`s, which the single `shared.ProblemDetailsAdvice` renders as RFC 9457 Problem Details with no stack trace and no exception message.
>
> — `sad.md §8, Error handling row (owner path), abridged` · full text: [sad.md](../sad.md)

> **Hard rule:** `domain` has no Spring or persistence imports. Modules call only each other's published `api`/`app` types, through direct method calls. `ModularityTest` (Spring Modulith `verify()`) fails the build on any violation.
>
> — `sad.md §2, Technical constraints (Architecture bullet), abridged` · full text: [sad.md](../sad.md)

> **Hard rule:** RFC 9457 Problem Details (`ProblemDetail`) via the single `@RestControllerAdvice` in `shared`. Never expose stack traces or exception messages.
>
> — `CLAUDE.md §Conventions, Errors bullet, verbatim` · full text: [CLAUDE.md](../../../../CLAUDE.md)

**Fallback:** insufficient or contradicted by the code → read the named file in full
([spec.md](../spec.md) · [sad.md](../sad.md) · [data-model.md](../data-model.md) ·
[openapi.yaml](../contracts/openapi.yaml) · [adr/](../adr/)) and follow it. Do not guess.

## Data delta

No DB changes.

## API contract

- `POST /api/v1/links`, body `ShortLinkCreate {targetUrl}` (`additionalProperties: false`) → `201 ShortLink {shortCode, shortUrl, targetUrl, state: ACTIVE, createdAt}`.
- `400` `code` + fixed `detail`: `link.target_url_unsupported` «Only absolute http or https web addresses are accepted.» (AC-03, also missing/empty `targetUrl`); `link.target_url_too_long` «The target URL must be at most 2048 characters long.»; `link.target_url_not_encoded` «The target URL must be properly encoded: non-Latin hosts in their ASCII xn-- form, other characters percent-encoded.»; `link.target_url_points_to_trimly` «A short link cannot point back to trimly.»; `shared.malformed_request` — body not a JSON object.
- `503` `link.temporarily_unavailable` «Temporarily unavailable, try again later.»; `401` from T12.
- `Problem` = `{type, title, status, detail?, instance?, code}`; `code` matches `^[a-z_]+\.[a-z_]+$`.

— `contracts/openapi.yaml, operationId createShortLink, components.schemas.Problem, abridged` · full text: [openapi.yaml](../contracts/openapi.yaml)

## Acceptance criteria

### AC-01 — happy path

> **Given** the owner presents the owner credential
> **When** the owner submits an absolute web address using http or https, at most 2048 characters long, as the target URL
> **Then** the system records a new active short link with a freshly generated 7-character short code drawn from the short-code alphabet (A–Z, a–z and 2–9 with exactly these six look-alike characters removed: 0, O, o, 1, l, I; 56 characters in all), where letter case matters and each character is chosen by a cryptographically secure random source, so the code cannot be predicted from earlier codes. The target URL is stored and later redirected to exactly as submitted, with no normalisation. The owner gets back the full short link, its short code and its target URL. The full short link is always built from trimly's single configured public address, whatever address the request arrived on
>
> — `spec.md §5, AC-01, verbatim` · full text: [spec.md](../spec.md)

### AC-03 — error

> **Given** the owner presents the owner credential
> **When** the owner submits a target URL that is not an absolute web address with a host using http or https (for example, no scheme at all, or a javascript, data or ftp address)
> **Then** the system blocks the creation, tells the owner that only http or https web addresses are accepted, and creates nothing
>
> — `spec.md §5, AC-03, verbatim` · full text: [spec.md](../spec.md)

### AC-04 — error

> **Given** the owner presents the owner credential
> **When** the owner submits a target URL longer than 2048 characters (counted on the submitted text as is)
> **Then** the system blocks the creation, tells the owner the maximum length is 2048 characters, and creates nothing
>
> — `spec.md §5, AC-04, verbatim` · full text: [spec.md](../spec.md)

### AC-05 — error

> **Given** the owner presents the owner credential
> **When** the owner submits a target URL that contains spaces (including leading or trailing ones), line breaks, other control characters, or any non-ASCII character (for example "é" in the path or a host written in non-Latin letters)
> **Then** the system blocks the creation, tells the owner the address must be properly encoded (non-Latin hosts in their ASCII "xn--" form, other characters percent-encoded), and creates nothing; trimly never trims or re-encodes the address itself
>
> — `spec.md §5, AC-05, verbatim` · full text: [spec.md](../spec.md)

### AC-06 — domain invariant

> **Given** the owner presents the owner credential
> **When** the owner submits a target URL whose host is any of trimly's own configured domains or a subdomain of one, compared in ASCII form without regard to letter case or a trailing dot (including one of trimly's own short links); the configured domains are exactly trimly's own hosts, never a shared parent domain of its hosting provider, and which domain trimly uses is roadmap decision D2
> **Then** the system blocks the creation and tells the owner that a short link cannot point back to trimly ("no redirect loops")
>
> — `spec.md §5, AC-06, verbatim` · full text: [spec.md](../spec.md)

### AC-17b — error

> **Given** the owner presents the owner credential and the link storage has not answered within 2 seconds
> **When** the owner creates, lists, disables or re-enables short links
> **Then** the owner is told "temporarily unavailable, try again later" and nothing is confirmed; if an unconfirmed creation was in fact recorded, retrying it creates a second, separate short link (AC-02), which is accepted: the owner sees both in the list and can disable the extra one
>
> — `spec.md §5, AC-17b, verbatim` · full text: [spec.md](../spec.md)

## Checklist

- [ ] `src/main/java/io/github/igorermolenko/trimly/link/api/LinkProblems.java`: the link error codes + fixed details; builds `ErrorResponseException` with `ProblemDetail.setProperty("code", …)`. Reused by T14.
- [ ] `src/main/java/io/github/igorermolenko/trimly/link/api/CreateShortLinkController.java` + `ShortLinkResponse.java`: map `TargetUrlRejected.Reason` → 400 codes, `LinkStorageUnavailable` → 503; never put `ex.getMessage()` in a response.
- [ ] `src/main/java/io/github/igorermolenko/trimly/shared/ProblemDetailsAdvice.java`: override `handleHttpMessageNotReadable` to add `code: shared.malformed_request` (only change to `shared`).
- [ ] `src/test/java/io/github/igorermolenko/trimly/link/api/CreateShortLinkIT.java` (bearer = test credential): 201 shape incl. `shortUrl` from `trimly.public-base-url` with a different `Host` header; each 400 code; 503 via a stubbed use case.

## Edge cases

| Case | Behaviour |
|---|---|
| Body `{}` or `targetUrl: ""` | 400 `link.target_url_unsupported`. |
| Body `[]` / not JSON | 400 `shared.malformed_request`. |
| Request `Host: other.example.test` | `shortUrl` still from `trimly.public-base-url` (AC-01). |
| Target URL with `?utm_source=x#frag` | Echoed in `targetUrl` byte for byte. |
| Storage down / 5 codes taken | 503 `link.temporarily_unavailable`, nothing confirmed (AC-17b). |
| Any 4xx/5xx | `application/problem+json`, no stack trace, no exception message. |

## Definition of Done

- [ ] `CreateShortLinkIT` passes for AC-01 (201 shape, `shortUrl` host independence), AC-03…AC-06 (one 400 code each, nothing stored) and AC-17b (503).
- [ ] Every error body carries a `code` from the contract enum; no exception message leaks (assertion on body).
- [ ] `ModularityTest`, `spotless:check` green.
- [ ] every Hard Rule inlined above still holds
- [ ] lint + vet clean (`./mvnw -B spotless:check`, `./mvnw -B verify`)
