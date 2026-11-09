---
status: Accepted
owner: "igorermolenko"
reviewers: ["Tech Lead"]
updated_at: "2026-11-02"
feature_size: "M"
ticket: "roadmap step 2 — url-shortener"
---

# 0005 — Return short-link lookups as a sealed result and answer visitors in plain text

- **Status:** Accepted
- **Date:** 2026-11-02
- **Deciders:** igorermolenko (owner / Tech Lead), with Claude during the design Socratic walk

## Context

`redirect` depends on `link` to resolve a short code (repo ADR-0002: a synchronous call to a published `app` type). The lookup has three outcomes. Found: redirect to the target URL (AC-07). Not found: unknown, disabled or malformed, all looking identical, and a malformed code never touches storage (AC-12, AC-16, AC-16b). Unavailable: storage did not answer within 2 s (AC-17). Visitors must get a short human-readable sentence displayed as plain text. That is a deliberate exception to the project's Problem Details error format, which still applies to owner operations (spec §3). The project convention routes exceptions to one `@RestControllerAdvice` in `shared`, which renders Problem Details JSON.

## Decision drivers

- Quality goal 1 (sad.md §1): an outage must read "temporarily unavailable", never "link not found" and never a generic JSON 500.
- Spec AC-16: "link not found" gives no hint whether the code ever existed.
- Convention: a single `@RestControllerAdvice` in `shared`; `shared` holds no business logic and must not depend on feature modules.
- ADR-0002 (this feature): the short-code alphabet and shape rule live only in `link`'s domain.

## Considered options

1. **Sealed result, no exceptions.** `LinkLookup.resolve(rawCode)` returns a sealed `Resolution` (`Found(targetUrl)` / `NotFound` / `Unavailable`). `link` does the shape check and translates storage failures inside. `redirect` switches over the result and builds the response itself.
2. **Exceptions + a redirect-scoped advice.** `LinkLookup` returns the target URL or throws `LinkNotFoundException` / `StorageUnavailableException`. A second, higher-priority `@ControllerAdvice` limited to `redirect` turns them into plain text.

## Decision outcome

**Chosen:** Option 1, sealed result, no exceptions. The compiler's exhaustive `switch` forces `redirect` to handle all three outcomes. Visitor answers never reach the shared advice, so an unhandled case cannot silently become JSON. The single-advice convention stays intact, and `shared` is untouched.

## Consequences

**Positive**
- The visitor path has no exception-to-response mapping; every visitor answer is explicit in `redirect.app`.
- Malformed codes are answered "not found" inside `link` without a storage call, so AC-16b holds even during an outage.
- `link` alone owns the meaning of "not found" (unknown, disabled or malformed), which keeps AC-16's indistinguishability in one place.

**Negative**
- The project has two error styles: a result type on the visitor path, and exceptions rendered as Problem Details on the owner path (sad.md §8).
- A genuine bug, meaning an unexpected exception, on the redirect path still reaches the shared advice and becomes a generic 500. That is acceptable (it is not an outage answer), but it should show up in monitoring (§7).

**Neutral**
- Adding click analytics (roadmap step 6) can hook into `redirect.app` after a `Found` result without changing this contract.

## Links

- Spec: [[../spec.md]] US-02, US-06, AC-07, AC-12, AC-16, AC-16b, AC-17, §3
- SAD: [[../sad.md]] §5, §8
- Related ADR: [[0002-generate-short-codes-with-securerandom-and-enforce-uniqueness-with-a-unique-index-and-insert-retry]], [[0003-bound-every-link-storage-call-within-2-seconds-using-pool-and-driver-timeouts]]
