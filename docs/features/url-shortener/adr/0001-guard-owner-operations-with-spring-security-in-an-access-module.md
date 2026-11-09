---
status: Accepted
owner: "igorermolenko"
reviewers: ["Tech Lead", "Security Lead"]
updated_at: "2026-11-02"
feature_size: "M"
ticket: "roadmap step 2 — url-shortener"
---

# 0001 — Guard owner operations with Spring Security in an access module

- **Status:** Accepted
- **Date:** 2026-11-02
- **Deciders:** igorermolenko (owner / Tech Lead), with Claude during the design Socratic walk

## Context

trimly gets its first authentication boundary. Creating, listing, disabling and re-enabling short links must require the owner credential, a single random secret held in an env var. Following a short link must never require a credential (spec AC-18, AC-19, §6.1). Two decisions are needed now: where that check lives in the module structure, and what it is built on. Roadmap step 4 (invited users) extends this same boundary. The roadmap's wave 3 runs steps 4 and 5 in parallel only if authentication is kept out of the `link` module.

## Decision drivers

- Quality goal 3 (sad.md §1): only the owner credential can manage links, and a missing and a wrong credential get identical answers (AC-18).
- Spec §6.1: the credential is validated at startup (≥ 32 printable non-space ASCII characters, ≥ 16 distinct), never logged, never echoed, and trimly refuses to start without it. A security review is required.
- Module rules (repo ADR-0002): modules use only each other's published types, and `shared` holds no business logic.
- `docs/roadmap.md` execution path: steps 4 ∥ 5 are disjoint only if step 2 keeps authentication out of `link`.
- Constraint: one 512 MB machine (sad.md §2), so memory overhead matters.

## Considered options

1. **New `access` module + Spring Security.** A stateless `SecurityFilterChain` with no session and no CSRF. A bearer-token check guards the owner API, every redirect is permitted, and a custom authentication entry point returns Problem Details.
2. **New `access` module + a hand-written servlet filter.** About 80 lines, no Spring Security: a constant-time comparison (`MessageDigest.isEqual`) on the owner API path prefix, returning Problem Details on rejection.
3. **The check inside `link`'s `api` layer.** A Spring MVC `HandlerInterceptor` in `link`, with no new module.

## Decision outcome

**Chosen:** Option 1, `access` module + Spring Security. It gives the security review well-known, maintained code instead of a hand-rolled check, adds safe default headers (HSTS, `X-Content-Type-Options`), and makes step 4's per-user credentials an extension rather than a rewrite. Keeping it in its own module preserves the step 4 ∥ step 5 parallelism that option 3 would lose.

## Consequences

**Positive**
- Authentication is a separate module that step 4 extends; `link` and `redirect` contain no credential logic.
- The security review audits a standard filter chain plus one small bearer-token authentication component.
- Startup validation of the credential lives next to the filter chain, as a validated `@ConfigurationProperties` binding in `access`.

**Negative**
- Spring Security's defaults have to be switched off explicitly: form login, HTTP Basic, sessions, CSRF and the default login page. Its rejections bypass `shared.ProblemDetailsAdvice`, so `access` needs its own entry point that produces the same Problem Details shape.
- It adds a few tens of MB of heap and filter-chain work on every request, including redirects (microseconds, but it must stay inside the 50 ms p95 redirect budget).
- `CLAUDE.md` and `docs/architecture-map.md` list three modules; they must be updated to add `access`.

**Neutral**
- Rotating the credential means changing the env var and restarting (spec §6.1). There is no runtime key store in this step.
- Moving to option 2 later would mean replacing one module's configuration, about a day of work.

## Links

- Spec: [[../spec.md]] US-07, AC-18, AC-19, §6.1
- SAD: [[../sad.md]] §4 (choice 1), §5, §8
- Related ADR: repo [ADR-0002](../../../adr/0002-organize-code-as-feature-modules-with-layers.md)
