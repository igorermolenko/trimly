# Changelog — url-shortener

## url-shortener — the owner creates short links, anyone who opens them is redirected

**What:** trimly now works as a URL shortener. The owner, recognised by a single owner
credential, sends a web address and gets back a new short link with a random 7-character code.
Anyone who opens that link is sent straight to the original address. The owner can list their
links (newest first, one page at a time), disable a link so it stops working for everyone at once,
and re-enable it under the same code. A visitor whose link does not work sees a plain sentence:
either "link not found" or "temporarily unavailable, try again later", so an outage never looks
like a dead link.

**Why:** long addresses, especially ones with tracking parameters, are cluttered and hard to type
([spec §1](spec.md#1-context)). This is roadmap step 2, the first feature built on the skeleton, so
it sets the pattern later steps follow ([spec §2](spec.md#2-goals)). The decisions that matter most:

- Owner operations are guarded by Spring Security in a dedicated `access` module, so `link` and
  `redirect` hold no authentication code ([ADR-0001](adr/0001-guard-owner-operations-with-spring-security-in-an-access-module.md)).
- Short codes come from `SecureRandom` over a 56-character alphabet without look-alike characters.
  A unique index plus up to 5 retries means a code is never reissued
  ([ADR-0002](adr/0002-generate-short-codes-with-securerandom-and-enforce-uniqueness-with-a-unique-index-and-insert-retry.md)).
- Every link-storage call answers within 2 seconds, enforced by pool and driver timeouts, which is
  where "temporarily unavailable" comes from
  ([ADR-0003](adr/0003-bound-every-link-storage-call-within-2-seconds-using-pool-and-driver-timeouts.md)).
- The list is paged by keyset on the monotonic UUIDv7 id, so a link created while the owner is
  paging is never shown twice or skipped
  ([ADR-0004](adr/0004-page-the-link-list-by-keyset-on-a-monotonic-uuidv7-id.md)).
- Visitors get plain text, while owner operations get RFC 9457 Problem Details
  ([ADR-0005](adr/0005-return-short-link-lookups-as-a-sealed-result-and-answer-visitors-in-plain-text.md)).
- The connection pool drains when idle and health checks never touch the database, so Neon can
  scale to zero. The uptime probe runs every 15 minutes, not every 5
  ([ADR-0006](adr/0006-let-link-storage-scale-to-zero-with-a-draining-pool-db-free-health-checks-and-a-15-minute-probe.md)).

**How to use** (full contract: [openapi.yaml](contracts/openapi.yaml)):

```bash
# Create (owner credential required)
curl -X POST https://<public-base-url>/api/v1/links \
  -H "Authorization: Bearer $TRIMLY_OWNER_CREDENTIAL" -H 'Content-Type: application/json' \
  -d '{"targetUrl":"https://example.test/articles/1?utm_source=newsletter"}'
# → 201 {"shortCode":"aB3dEf9","shortUrl":"https://<public-base-url>/aB3dEf9", "targetUrl":…, "state":"ACTIVE", "createdAt":…}

# Follow (public, no credential) → 302 Location: <targetUrl>, never cached
curl -i https://<public-base-url>/aB3dEf9

# List: newest first, ?limit= 1–100 (default 20); next page with ?after=<nextCursor> while hasNext is true
curl -H "Authorization: Bearer $TRIMLY_OWNER_CREDENTIAL" "https://<public-base-url>/api/v1/links?limit=20"

# Disable / re-enable (both idempotent)
curl -X POST -H "Authorization: Bearer $TRIMLY_OWNER_CREDENTIAL" https://<public-base-url>/api/v1/links/aB3dEf9/disable
curl -X POST -H "Authorization: Bearer $TRIMLY_OWNER_CREDENTIAL" https://<public-base-url>/api/v1/links/aB3dEf9/enable
```

**Operational notes:**

- **Migration:** adds `V2__create_short_link.sql` (the `short_link` table, a unique index on
  `short_code`, and length and state checks). Flyway applies it on startup. Flyway in this repo is
  forward-only, so there is no automatic down migration. The manual rollback is
  [`migrations/01_create_short_link.down.sql`](migrations/01_create_short_link.down.sql) (`DROP TABLE`,
  plus deleting the matching `flyway_schema_history` row). It is safe only before the table holds
  real links, because afterwards it destroys every issued short code.
- **Config: trimly refuses to start without these settings.**
  - `TRIMLY_OWNER_CREDENTIAL`: at least 32 printable ASCII characters, no spaces, at least 16
    distinct characters. Generate it randomly. To rotate it, set a new value and restart.
  - `TRIMLY_PUBLIC_BASE_URL`: the http(s) address short links are built from (scheme, host and
    optional port only).
  - `TRIMLY_OWN_HOSTS`: trimly's own host names, which a target URL may not point to. List only
    trimly's own hosts, never a hosting provider's shared parent domain (roadmap D2).
  - `SPRING_DATASOURCE_URL` / `_USERNAME` / `_PASSWORD`, as before.
- **Ports:** the app listens on 8080. Health (`/actuator/health/liveness`) and Prometheus metrics are
  on management port **8081**, which must never be routed publicly. Health checks don't touch the
  database.
- **Feature flag:** none.
- **Rollback:** revert the deploy. The table can stay, because the previous build ignores it. Drop
  it by hand only if no real links exist yet (see Migration above).
- **Known open items** ([spec §8](spec.md#8-open-questions)): the after-idle latency targets are not
  measured yet (due after the first deploy). The detail wording for an unreadable form body on create
  is to be reconsidered before the first deploy (review U1).

**Acceptance criteria delivered:** AC-01 – AC-20b (all 23 in [spec §5](spec.md#5-acceptance-criteria)):
create with a secure random code and strict target-URL rules (AC-01 – AC-06, AC-20b), instant
uncached redirects (AC-07, AC-08, AC-19), paged owner list (AC-09, AC-10), disable and re-enable
taking effect for every visitor at once (AC-11 – AC-15), visitor answers that keep "not found"
apart from "unavailable" (AC-16, AC-16b, AC-17, AC-17b), owner-only management (AC-18), and a
short code that never points to a different target (AC-20).
