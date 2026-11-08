# API sync report — url-shortener

> **Contract:** [openapi.yaml](openapi.yaml) (OpenAPI 3.1.0, `info.version` 0.1.0) · **Generated:** 2026-11-03
> **Inputs:** [data-model.md](../data-model.md) (present), [sad.md](../sad.md) §5/§6/§8 (present, `target_surfaces: [backend-service]`), [spec.md](../spec.md) §4/§5 (present), feature ADR-0001…0006, repo `CLAUDE.md` + `docs/architecture-map.md`.
> **Size:** M (from `.size`), route `standard`. **Events:** none. Every flow is synchronous (sad.md §6, §8 "Events"), so there is no `events.md` and no `Idempotency-Key`.

## Operations

| operationId | Method + path | User story | ACs | §6 flow |
|---|---|---|---|---|
| `createShortLink` | `POST /api/v1/links` | US-01, US-07 | AC-01…AC-06, AC-08, AC-17b, AC-18, AC-20, AC-20b | Critical flow 2; Cross-cutting lifecycle |
| `listShortLinks` | `GET /api/v1/links` | US-03, US-07 | AC-09, AC-10, AC-17b, AC-18 | List short links |
| `disableShortLink` | `POST /api/v1/links/{shortCode}/disable` | US-04, US-07 | AC-11, AC-13, AC-14, AC-17b, AC-18 | Disable or re-enable; lifecycle |
| `enableShortLink` | `POST /api/v1/links/{shortCode}/enable` | US-05, US-07 | AC-13, AC-14, AC-15, AC-17b, AC-18, AC-20 | Disable or re-enable; lifecycle |
| `followShortLink` | `GET /{shortCode}` (public) | US-02, US-06, US-07 | AC-07, AC-08, AC-12, AC-15, AC-16, AC-16b, AC-17, AC-19 | Critical flow 1; lifecycle |

Disable and re-enable are two endpoints, not one `PUT …/state`, because sad.md §7 breaks the p95 metric down per operation via the `uri` tag ("redirect, create, list, disable and re-enable"). Both are `POST` with no body. Their behaviour is still idempotent (AC-14).

## Section A — field origins

| schema_path | origin | confidence |
|---|---|---|
| `createShortLink.request.targetUrl` | data-model.md → `short_link.target_url` (TEXT, CHECK char_length 1..2048) + AC-05 printable-ASCII pattern | high |
| `createShortLink.201.shortCode` | data-model.md → `short_link.short_code` (7 chars, UNIQUE) + AC-01 alphabet pattern | high |
| `createShortLink.201.shortUrl` | derived: `trimly.public-base-url` (sad.md §8 Configuration) + `short_code`, per AC-01 | medium |
| `createShortLink.201.targetUrl` | data-model.md → `short_link.target_url` | high |
| `createShortLink.201.state` | data-model.md → `short_link.state` (CHECK ACTIVE, DISABLED) | high |
| `createShortLink.201.createdAt` | data-model.md → `short_link.created_at` (TIMESTAMPTZ) | high |
| `listShortLinks.query.after` | derived: keyset cursor = last link's `short_link.id`, opaque to the client (ADR-0004) | high |
| `listShortLinks.query.limit` | spec AC-09 (default 20, max 100) | high |
| `listShortLinks.200.items[]` | `$ref ShortLink`, same origins as `createShortLink.201.*` | high |
| `listShortLinks.200.hasNext` | derived: data-model "LIMIT :size + 1" (the extra row says whether more follow) | high |
| `listShortLinks.200.nextCursor` | derived: cursor wrapper convention, ADR-0004 | high |
| `disableShortLink.path.shortCode` / `enableShortLink.path.shortCode` | data-model.md → `short_link.short_code` | high |
| `disableShortLink.200.shortCode` / `enableShortLink.200.shortCode` | data-model.md → `UPDATE … RETURNING short_code` | high |
| `disableShortLink.200.state` / `enableShortLink.200.state` | data-model.md → `UPDATE … RETURNING state` | high |
| `followShortLink.path.shortCode` | raw path segment, deliberately unvalidated (ADR-0005: `link` checks the shape) | high |
| `followShortLink.302.Location` | data-model.md → `short_link.target_url` (exactly as stored) | high |
| `followShortLink.*.Cache-Control` | sad.md §8 "Redirect reuse" (`no-store`) | high |
| `Problem.*` (type, title, status, detail, instance) | RFC 9457 / Spring `ProblemDetail` (repo convention: CLAUDE.md, architecture-map.md) | high |
| `Problem.code` | this contract's proposal: neutral `module.error_name` extension member | medium |

**Columns not exposed:** `short_link.id` stays internal. data-model.md says it is "never shown publicly"; the list cursor carries it opaquely, and ADR-0004 accepts that the cursor reveals only the creation time. `short_link.updated_at` is internal bookkeeping with no story. Both are accepted.

## Section B — drift findings

1. **Endpoint ↔ data-model** *(core)* ✓. Every operation reads or writes `short_link`. Create maps to INSERT, list to the keyset SELECT, disable/enable to the state UPDATE, and follow to the SELECT by short code. Each one matches a data-model access pattern.
2. **Error code ↔ repo error definition** *(core)* ✓ (proposal). There is no error registry: `shared.ProblemDetailsAdvice` only maps unexpected exceptions to a generic 500. The 10 codes in `Problem.code` are the contract's proposal, and `implement` must define them in the form it chooses (for example a code enum in each module's `api` layer, set as the `ProblemDetail` `code` property). Reconcile when they exist. *Reconciled 2026-11-07:* the contract now lists 14 codes. The 10 module codes are set by the modules; review R5 added four `shared.*` fallbacks (`shared.not_found`, `shared.method_not_allowed`, `shared.not_acceptable`, `shared.internal_error`) that `shared.ProblemDetailsAdvice` sets on framework-level answers that carry no module code. `shared.malformed_request` also covers an owner-API address Spring Security's firewall rejects (for example `/api/v1/links/ab%2Fcdef/disable`), which the advice answers as 400 (review round 2, N2). *Hand edit 2026-11-07 (review r4 Q3, task T40):* the create 400 description now lists every `shared.malformed_request` trigger (not a JSON object, wrong or missing Content-Type, an unknown member, a firewall-rejected address) and the extended `link.target_url_not_encoded` trigger from T39 (an http/https address with characters that must be percent-encoded). No schema or code-enum change. *Hand edit 2026-11-08 (review 2026-11-08 R2, R3; task T44):* a new shared response `components/responses/MalformedAddress` (400 `shared.malformed_request`) covers an address the firewall or the server itself rejects. Disable and enable reference it, and the list and create 400 descriptions name it. The visitor 404 description says such an address gets "Link not found." too. Tomcat's own rejections (a bad percent-escape, an encoded NUL or backslash, a path above the root) are answered by `redirect.api.ServerRejectedAddressValve` (T42), which writes the same Problem as the advice; `RedirectIT` pins the two bodies as equal. No schema or code-enum change. *Hand edit 2026-11-08 (review 2026-11-08-r2 S1; task T45):* the list 400 description now names a query that is not validly percent-encoded (`?after=%zz`, a lone `%`). Tomcat then throws `InvalidParameterException` on every parameter read, and `shared.ProblemDetailsAdvice` answers it as `shared.malformed_request`, as it does a firewall-rejected address. Before this edit it was a 500. No schema or code-enum change.
3. **Validation ↔ constraint** *(core)* ✓. `ShortCode` is length 7, matching the CHECK; the contract adds the AC-01 alphabet pattern, which is stricter and is enforced in `link.domain` (data-model notes SQL checks length only). `TargetUrl` is 1..2048 characters, matching the CHECK; the contract adds the AC-05 `^[\x21-\x7E]+$` pattern, which is stricter and is a domain rule. `LinkState` is the enum `ACTIVE`/`DISABLED`, matching the CHECK. There are no conflicts.
4. **OpenAPI ↔ sequence** *(supporting)* ✓. Each §6 `alt`-branch maps to a response:
   - Critical flow 1: malformed → 404 text; active → 302; unknown or disabled → 404 text (identical); timeout → 503 text.
   - Critical flow 2: credential missing or wrong → 401; rule break → 400 (4 codes); stored → 201; 5 codes taken or timeout → 503.
   - List: 401; page → 200; empty → 200 with `items: []`; timeout → 503.
   - Disable or re-enable: 401; matched → 200 (also when already in that state); no match → 404 `link.not_found`; timeout → 503.

### Back-feed (coverage)

- Every §5 AC maps to at least one operation or response (table above). AC-02 and AC-20 are invariants with no response of their own: they hold because every create returns 201 with a new `shortCode`.
- Every operation maps to a §4 user story and at least one AC.
- Every §6 `alt`-branch has a response (point 4).

### Decisions taken here (flagged to `api` by sad.md §6)

- **Page size outside 1..100** → 400 `link.invalid_page_size`. It is rejected, not clamped, so a client bug is visible.
- **Garbled continuation token** → 400 `link.invalid_cursor`.
- **Malformed short code on the owner path** → 404 `link.not_found` without a storage call, which mirrors AC-16b. It is indistinguishable from an unknown code.
- **Redirect status** → `302 Found` with `Cache-Control: no-store` (sad.md §8: temporary, never permanent).
- **Visitor sentences** are fixed as `Link not found.` and `Temporarily unavailable, try again later.` in `text/plain`.
- **401** always carries a bare `WWW-Authenticate: Bearer`, with no RFC 6750 `error=` attribute, so that a wrong credential and a missing one get identical answers (AC-18).

### Flags

| # | Kind | Finding | Resolution |
|---|---|---|---|
| F1 | sequence gap (supporting) | A request body that is not a JSON object (or has a wrong content type) has no §6 branch and no AC. The contract answers it with 400 `shared.malformed_request`. A missing or empty `targetUrl` is folded into AC-03 (`link.target_url_unsupported`). | **Resolved (review 2026-11-07 r3).** The framework-level 400 is enough; no new AC. A body that is not a JSON object, has a wrong content type, or carries an unknown member (`ShortLinkCreate` is `additionalProperties: false`) gets 400 `shared.malformed_request`. Recorded in spec §8. |
| F2 | deviation by repo convention | The error body is RFC 9457 Problem Details plus a `code` extension member, not the plugin's `{code, message, details?}` envelope. `message` is carried by `detail`. | **Accepted.** CLAUDE.md §Errors, architecture-map.md §Error handling, sad.md §8, ADR-0001. |
| F3 | deviation by ADR | The list pages forward only: there is no `before` and no `hasPrev`. JSON members are camelCase (the Spring/Jackson default, consistent with `ProblemDetail`). | **Accepted.** ADR-0004 (keyset `id < :cursor` only); AC-09 asks only to "continue right after its last link". |

There are no open flags: F1 is resolved and 2 deviations are accepted. No core point fails, so the run does not pause.

### Implementation notes for `tasks` (not drift)

- **AC-16b on unmatched paths.** A request such as `/aB3dEf9/extra` or `/a/b/c` does not reach the redirect controller. Spring's default 404 would then render Problem Details JSON, which breaks AC-16b ("link not found" in plain text). `redirect` needs a catch-all (or an error-path mapping) for non-`/api/**` paths that answers the plain-text 404 with `Cache-Control: no-store`. Paths under `/api/**` keep Problem Details.
- **`Cache-Control: no-store` on the redirect path** must survive Spring Security's default header writer (sad.md §8). A test asserts it.
- **Unexpected 500s.** On the owner path, `shared.ProblemDetailsAdvice` returns a generic `ProblemDetail` with `code: shared.internal_error` and no exception message. That 500 is not an operation response in the contract (sad.md §7 treats it as a bug signal), but it still satisfies `Problem.required`: every owner-API `Problem` carries a `code` (*reconciled 2026-11-07*, review R5).

## Lint

Spectral or Redocly linting has not been run here, because neither is installed and fetching one through `npx` was blocked. The YAML parses (4 paths, 8 schemas). Suggested check:

```
npx @stoplight/spectral-cli lint docs/features/url-shortener/contracts/openapi.yaml
```

Wiring it into CI is optional and is not done by this stage.
