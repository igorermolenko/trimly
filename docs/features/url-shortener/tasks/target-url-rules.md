---
id: T4
title: "Add TargetUrl validation rules (scheme, length, encoding, own hosts) in link.domain"
layer: "domain"
deps: []
blocks: ["T7"]
acs: ["AC-03", "AC-04", "AC-05", "AC-06"]
files_hint: ["src/main/java/io/github/igorermolenko/trimly/link/domain/TargetUrl.java", "src/main/java/io/github/igorermolenko/trimly/link/domain/TargetUrlRejected.java", "src/test/java/io/github/igorermolenko/trimly/link/domain/TargetUrlTest.java"]
owner: "igorermolenko"
estimate: "M"
context_budget: "M"     # 50 inlined lines
status: "todo"
step: ""
---

<!-- To the executing agent: work from what is inlined here. If a slice is insufficient, ambiguous,
or contradicts the code in front of you, open the named file for the full text and follow that.
Do not invent the missing part. Every quote ends with its provenance signature; the source wins. -->

# T4 — Add TargetUrl validation rules (scheme, length, encoding, own hosts) in link.domain

## Place in the sequence

- **Blocked by:** nothing — starts in wave 1
- **Blocks:** T7 — Implement the CreateShortLink use case with 5-attempt insert-retry and public-base-url
- **Wave:** 1 — no dependencies.
- **Lane:** own lane.

## Why (user story)

> **As an** owner
> **I want** to submit a target URL and get back a new short link
> **So that** I can share a clean link that is easy to type by hand
>
> — `spec.md §4, US-01, verbatim` · full text: [spec.md](../spec.md)

This task decides which target URLs may become short links, before any storage call.

## Inlined context

> Target-URL rules live in `link.domain` (plain Java) and run before any storage call: absolute http/https with a host, ≤ 2048 characters as submitted, ASCII-only with no spaces or control characters, host not one of `trimly.own-hosts` or their subdomains (case-insensitive, trailing dot ignored). The URL is stored and redirected to exactly as submitted, never trimmed, normalised or re-encoded (AC-01, AC-05)
>
> — `sad.md §8, Input validation row, verbatim` · full text: [sad.md](../sad.md)

> **Hard rule:** `domain` has no Spring or persistence imports. Modules call only each other's published `api`/`app` types, through direct method calls. `ModularityTest` (Spring Modulith `verify()`) fails the build on any violation.
>
> — `sad.md §2, Technical constraints (Architecture bullet), abridged` · full text: [sad.md](../sad.md)

> `trimly.own-hosts`: trimly's own hosts, used to refuse redirect loops (AC-06); the actual domain is roadmap decision D2.
>
> — `sad.md §8, Configuration row, abridged` · full text: [sad.md](../sad.md)

**Fallback:** insufficient or contradicted by the code → read the named file in full
([spec.md](../spec.md) · [sad.md](../sad.md) · [data-model.md](../data-model.md) ·
[openapi.yaml](../contracts/openapi.yaml) · [adr/](../adr/)) and follow it. Do not guess.

## Data delta

No DB changes.

## API contract

- Rejections map (in T13) to `400` with `code`: `link.target_url_unsupported` (AC-03, also missing/empty), `link.target_url_too_long` (AC-04), `link.target_url_not_encoded` (AC-05), `link.target_url_points_to_trimly` (AC-06). This task exposes the four reasons as a domain enum — no HTTP here.

— `contracts/openapi.yaml, operationId createShortLink, abridged` · full text: [openapi.yaml](../contracts/openapi.yaml)

## Acceptance criteria

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

## Checklist

- [ ] `src/main/java/io/github/igorermolenko/trimly/link/domain/TargetUrlRejected.java`: domain exception carrying a `Reason` enum `UNSUPPORTED | TOO_LONG | NOT_ENCODED | POINTS_TO_TRIMLY`.
- [ ] `src/main/java/io/github/igorermolenko/trimly/link/domain/TargetUrl.java`: `static TargetUrl parse(String raw, Set<String> ownHosts)`; checks in this order: null/empty → UNSUPPORTED; length > 2048 (on the raw text) → TOO_LONG; any char outside `0x21–0x7E` → NOT_ENCODED; not absolute http/https with a host (`java.net.URI`, case-insensitive scheme) → UNSUPPORTED; host equals or ends with `.` + an own host (ASCII, lower-cased, one trailing dot stripped on both sides) → POINTS_TO_TRIMLY. Keeps `raw` unchanged as its value.
- [ ] `src/test/java/io/github/igorermolenko/trimly/link/domain/TargetUrlTest.java`: parameterized table from Edge cases.

## Edge cases

| Case | Behaviour |
|---|---|
| `example.test/a` (no scheme), `javascript:alert(1)`, `data:…`, `ftp://h/`, `https:///path` | UNSUPPORTED (AC-03). |
| 2048 chars exactly / 2049 chars | Accepted / TOO_LONG (AC-04, counted on the raw text). |
| Leading/trailing space, `\n`, tab, `é`, `https://пример.рф/` | NOT_ENCODED; never trimmed or re-encoded (AC-05). |
| `https://xn--e1afmkfd.xn--p1ai/` | Accepted (ASCII form). |
| `HTTPS://Trimly.Example.Test./x`, `https://a.trimly.example.test/` | POINTS_TO_TRIMLY with own host `trimly.example.test` (AC-06). |
| `https://nottrimly.example.test/` | Accepted — only exact host or a real subdomain matches. |
| Accepted URL | `value()` equals the input byte for byte (AC-01 «no normalisation»). |

## Definition of Done

- [ ] `TargetUrlTest` covers every Edge-case row and passes.
- [ ] Rules run without any storage or Spring dependency (`ModularityTest` green).
- [ ] lint (`spotless:check`) clean.
- [ ] every Hard Rule inlined above still holds
- [ ] lint + vet clean (`./mvnw -B spotless:check`, `./mvnw -B verify`)
