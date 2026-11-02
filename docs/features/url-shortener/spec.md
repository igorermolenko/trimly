---
status: Draft
owner: "igorermolenko"
reviewers: ["Tech Lead", "Security Lead"]
updated_at: "2026-11-02"
feature_size: "M"
---

# Spec — url-shortener

> **Glossary:** [CONTEXT](../../../CONTEXT.md) (repo-root, project-wide; no feature-level CONTEXT.md)
> **Reference module / docs / channels used:** `docs/idea-brief.md`, `docs/roadmap.md` (step 2), `docs/architecture-map.md`, ADR-0001…0003, skeleton code under `src/main/java/io/github/igorermolenko/trimly/` (`shared/`, `link/`, `redirect/` package docs).

## 1. Context

Long web addresses, especially ones carrying tracking parameters, look cluttered, break formatting in messages and are hard to type or remember (owner-reported, `docs/idea-brief.md` §2). trimly's first cut gives its owner a short, hand-typeable link for any web address, and gives anyone who opens that link an immediate trip to the original page. The owner is the only person who creates links at this stage; visitors are anyone on the internet (brief §3, §5).

There is no external trigger or deadline: trimly is a learning and portfolio project (brief §4). This is roadmap step 2, the first feature built on the skeleton, so it sets the pattern later steps follow. Success is defined by the owner as "a link created on the deployed service redirects correctly from a fresh browser, and the repo shows the spec → design → tasks → TDD trail" (brief §7).

**Committed approach.** The owner, recognised by a single owner credential, creates short links. Each creation gets a new 7-character generated short code that leaves out look-alike characters. Anyone who opens a short link goes to its target URL. Every visit passes through trimly, so when the owner disables a link it stops working for everyone at once. The owner can also list their links and re-enable a disabled link. Comparable self-hosted shorteners differ on whether shortening the same address twice returns the old code or a new one: YOURLS reuses the old code by default, while Shlink and Kutt reuse only when asked. trimly therefore makes "always a new short link" an explicit rule. Kutt is a verified precedent for two of trimly's other rules: dropping look-alike characters from codes and refusing targets on its own domain. The sharpest failure mode the adversarial review found is a temporary outage being shown to visitors as a dead link. §5 therefore keeps "temporarily unavailable" separate from "link not found".

Traceability:
- Scope is roadmap step 2 only. Invited users (step 4), custom aliases (step 5) and click analytics (step 6) are excluded on purpose.
- Owner-accepted direction and constraints: brief §5 and §7 (Q5, Q7, Q8). Budget, single always-on instance, and a free database tier that pauses after idle periods: `docs/architecture-map.md` §Constraints.
- The competitive research came back RESEARCH_LIMITED (2026-11-01). Sources only partly cover what visitors see for disabled links, how schemes are handled, and the redirect defaults; those cells were left unverified, not guessed.
- Strategist and analyst were not run, and no RICE score was computed: the direction was already accepted in brief §7 (Q8), and reuse rules forbid regenerating it.

## 2. Goals

- The owner can turn any web address into a short link that is clean to share and easy to type by hand.
- Anyone who opens a short link reaches its target URL without a noticeable wait, within the always-on budget of about $5 a month.
- The owner stays in control of every link: they can see all of their links and switch any one off for everyone at once.
- The feature is built through the full spec → design → tasks → TDD trail and becomes the pattern for later roadmap steps (process goal, so no user story; measured by the end-to-end and AC-under-test KPIs in §7).

## 3. Non-goals

- **Invited users creating links.** That is roadmap step 4, and how they get access (open decision D1) is not settled. In this step only the owner creates links.
- **Custom aliases chosen by the creator.** That is roadmap step 5. Every short code here is generated.
- **Click analytics.** That is roadmap step 6, still unshaped ("fog"). Every visit passes through trimly, so analytics can be added later without changing how redirects behave.
- **Anonymous link creation and abuse handling for strangers.** Excluded by the owner (brief §5). Only the owner holds the credential that creates links.
- **Changing a link's target URL or deleting a link.** A short code must never start pointing somewhere new (CONTEXT invariants). The owner disables the link instead, and creates a new one if needed.
- **Checking that a target site exists or is safe.** The owner is the only, trusted creator, and a reachability check would slow down creation.
- **A web interface.** trimly is API-only at this stage (`docs/architecture-map.md` §Frontend). Visitors only ever receive redirects or a short "not found" / "unavailable" response.

## 4. User stories

### US-01: Create a short link

**As an** owner
**I want** to submit a target URL and get back a new short link
**So that** I can share a clean link that is easy to type by hand

### US-02: Follow a short link

**As a** visitor
**I want** to open a short link and land on its target URL
**So that** I reach the page without typing the long address

### US-03: List my short links

**As an** owner
**I want** to see all my short links, newest first and one page at a time, each with its short code, target URL, creation time and state (active or disabled)
**So that** I know what exists and can find the link I want to act on

### US-04: Disable a short link

**As an** owner
**I want** to switch a short link off
**So that** a mistaken or abused link stops working for everyone at once

### US-05: Re-enable a disabled link

**As an** owner
**I want** to switch a disabled link back on
**So that** I can undo a mistaken disable without sharing a new short code

### US-06: Understand a link that doesn't work

**As a** visitor
**I want** a plain message that says either "link not found" or "temporarily unavailable"
**So that** I know whether the link is dead or worth retrying later

### US-07: Keep link management to myself

**As an** owner
**I want** only someone presenting the owner credential to be able to create, list, disable or re-enable short links
**So that** nobody else can publish links on trimly's domain

## 5. Acceptance criteria

### AC-01 (US-01) — happy path

**Given** the owner presents the owner credential
**When** the owner submits an absolute web address using http or https, at most 2048 characters long, as the target URL
**Then** the system records a new active short link with a freshly generated 7-character short code made of Latin letters and digits without look-alike characters (such as 0, O, o, 1, l, I), where letter case matters and the code cannot be predicted from earlier codes, and returns the full short link, its short code and its target URL to the owner

### AC-02 (US-01) — domain invariant

**Given** the owner already has a short link for target URL X
**When** the owner submits target URL X again
**Then** the system creates a separate short link with a different short code, and the earlier short link stays unchanged ("every creation issues a new short link")

### AC-03 (US-01) — error

**Given** the owner presents the owner credential
**When** the owner submits a target URL that is not an absolute web address with a host using http or https (for example, no scheme at all, or a javascript, data or ftp address)
**Then** the system blocks the creation, tells the owner that only http or https web addresses are accepted, and creates nothing

### AC-04 (US-01) — error

**Given** the owner presents the owner credential
**When** the owner submits a target URL longer than 2048 characters
**Then** the system blocks the creation, tells the owner the maximum length is 2048 characters, and creates nothing

### AC-05 (US-01) — error

**Given** the owner presents the owner credential
**When** the owner submits a target URL that contains spaces, line breaks or other control characters
**Then** the system blocks the creation, tells the owner the address must be properly encoded, and creates nothing

### AC-06 (US-01) — domain invariant

**Given** the owner presents the owner credential
**When** the owner submits a target URL whose host is any of trimly's own configured domains, compared without regard to letter case or a trailing dot (including one of trimly's own short links)
**Then** the system blocks the creation and tells the owner that a short link cannot point back to trimly ("no redirect loops")

### AC-07 (US-02) — happy path

**Given** an active short link exists
**When** a visitor opens it
**Then** the visitor is sent straight to its target URL, with no trimly page in between

### AC-08 (US-02, US-01) — cross-context

**Given** the owner has just created a short link
**When** a visitor opens that short link immediately afterwards
**Then** the visitor reaches its target URL; a link is usable for redirects the moment its creation is confirmed

### AC-09 (US-03) — happy path

**Given** the owner has several short links, some active and some disabled
**When** the owner lists their short links
**Then** the owner sees each link's short code, target URL, creation time and state, newest first, one page at a time, with a way to get the next page

### AC-10 (US-03) — happy path (edge)

**Given** the owner has not created any short links yet
**When** the owner lists their short links
**Then** the owner gets an empty list, not an error

### AC-11 (US-04) — happy path

**Given** an active short link exists
**When** the owner disables it
**Then** the system records the link as disabled and confirms it, and the owner's list shows it as disabled

### AC-12 (US-04, US-02) — cross-context

**Given** the owner has disabled a short link
**When** any visitor opens it, including a visitor who opened it before it was disabled
**Then** the visitor sees "link not found" and is not sent to the target URL

### AC-13 (US-04, US-05) — error

**Given** the owner presents the owner credential
**When** the owner tries to disable or re-enable a short code that does not exist
**Then** the system tells the owner the link was not found and nothing changes

### AC-14 (US-04, US-05) — domain invariant

**Given** a short link is already disabled (or already active)
**When** the owner disables it (or re-enables it) again
**Then** nothing changes and the system confirms the current state without reporting an error ("repeating a state change is harmless")

### AC-15 (US-05) — happy path

**Given** a disabled short link exists
**When** the owner re-enables it
**Then** visitors who open it are again sent to the same target URL under the same short code

### AC-16 (US-06) — happy path (edge)

**Given** a short code does not exist, or is not a well-formed short code
**When** a visitor opens it
**Then** the visitor sees "link not found", identical to what a disabled link shows, with no hint whether the code ever existed

### AC-16b (US-06) — error

**Given** a visitor opens an address whose shape cannot be a short code (wrong length, or characters outside the short-code alphabet)
**When** trimly handles it
**Then** the visitor gets "link not found" without trimly looking anything up in its link storage, so the answer stays "link not found" even while the link storage is unavailable

### AC-17 (US-06) — cross-context

**Given** trimly temporarily cannot look up links because its link storage is not responding
**When** a visitor opens a well-formed short code
**Then** the visitor sees "temporarily unavailable, try again later", not "link not found"

### AC-18 (US-07) — authorization

**Given** a person presents no owner credential, or a wrong one
**When** they try to create, list, disable or re-enable short links
**Then** the system denies the action, says authentication is required, creates or changes nothing, and reveals no link data

### AC-19 (US-02, US-07) — authorization

**Given** a visitor presents no credential at all
**When** the visitor opens an active short link
**Then** the visitor is sent to its target URL; following a short link never requires a credential

### AC-20 (US-04, US-05) — domain invariant

**Given** a short code has been issued, whether its link is now active or disabled
**When** the owner creates any number of new short links
**Then** none of them is ever given that short code, and re-enabling that code's link still leads to its original target URL ("a short code never points to a different target URL")

### AC-20b (US-01) — domain invariant

**Given** the generator produces a short code that is already taken
**When** the owner creates a short link
**Then** the owner still gets a new short link with a different, unused short code, not an error

## 6. Non-functional requirements

| Aspect | Target | Measurement |
|---|---|---|
| Redirect latency p95, link storage awake | ≤ 50 ms server-side | server-side request-duration metric for redirects, 7-day rolling window |
| Redirect latency p95, first request after the link storage has idled (≥ 5 min without requests) | ≤ 1 s server-side | same metric, filtered to the first request after an idle gap |
| Owner operations latency p95 (create, list, disable, re-enable) | ≤ 300 ms with link storage awake; ≤ 1.5 s for the first request after idle | server-side request-duration metric per operation |
| Throughput | ≥ 50 redirects/s sustained on one production-size instance, link storage awake | load test run before the deploy step (roadmap step 3) |
| Availability of redirects | 99.5 % per calendar month (≈ 3.6 h downtime allowed) | external uptime probe every 5 min against a known active short link |
| Disable propagation | 0 s: the first visit after a disable is confirmed already gets "link not found" | integration test for AC-12 |
| Running cost | ≤ $5 per month for everything needed to serve trimly | monthly hosting and database bills |

## 6.1 Security / privacy

- **Data classification:** internal. A target URL is not a secret, but the owner's list of links and its states is not public, so only the owner can see it.
- **Personal data touched:** none. A short link stores only its short code, target URL, creation time and state. This feature stores nothing about visitors, and click analytics are out of scope (§3).
- **AuthZ/AuthN impact:** this adds one capability: the owner credential check. It guards creating, listing, disabling and re-enabling links. Following a short link needs no credential (AC-19). The owner credential is random and at least 32 characters long. trimly refuses to start without a credential that meets this rule. The credential is never written to logs or shown in any response. To rotate it, the operator sets a new value and restarts trimly; the old value stops working immediately. Failed credential attempts are not rate-limited, because guessing a 32-character random credential is not practical.
- **Abuse cases:**
  - Leaked owner credential, used to publish phishing links on trimly's domain. Response: the operator rotates the credential. The owner then lists the links (AC-09) and disables the foreign ones (AC-11, AC-12). A disable takes effect for every visitor at once, and an issued short code can never be pointed at a different target (AC-20).
  - Injection through the target URL (`javascript:` or `data:` addresses, control characters, redirect loops). Response: creation is blocked with a plain explanation (AC-03, AC-05, AC-06).
  - Guessing short codes to discover links. Response: codes are random and not sequential. An unknown code and a disabled link look identical (AC-16), and the list of links requires the owner credential (AC-18).
  - Flood of scanner or bot requests on the redirect side, which keeps link storage awake and uses up its free compute budget. Response: addresses that cannot be short codes are answered without touching link storage (AC-16b). Whether to also rate-limit visitors is an open question (§8).
  - An outage that looks like a dead link. Response: visitors see "temporarily unavailable", never "link not found" (AC-17).
- **Security review:** Required. This feature introduces trimly's first authentication boundary and serves public redirects to arbitrary targets.

## 7. Metrics / KPIs

- **End-to-end success from a fresh browser** (brief §7 success criterion): a short link created on the deployed service sends a fresh browser, with no cache or cookies, to its target URL. Baseline: not possible today. Target: shown within 7 days of the first deploy (roadmap step 3); evidence recorded at `ship`.
- **Acceptance criteria under automated test:** the share of §5 ACs covered by at least one passing automated test in CI. Baseline: 0 %. Target: 100 % when this feature merges, traced in the AC → test table produced by `plan-tests`.
- **Owner dogfooding:** short links the owner actually creates and shares, counted from the owner's link list (AC-09). Baseline: 0. Target: at least 10 within the first 30 days after deploy.
- **Time to neutralise a bad link:** time from the owner noticing a mistaken or malicious link until visitors get "link not found". Baseline: impossible without direct database access. Target: under 1 minute (one disable action), checked once by hand at `ship` from a fresh browser.

## 8. Open questions

- [ ] Should trimly limit how many redirect lookups one visitor can make per minute, to protect the link storage's free compute budget from code-guessing floods? Default now: no rate limit; AC-16b is the only guard. — owner: igorermolenko, due: before `sdd:design`
- [ ] Will the 5-minute uptime probe (§6 availability) keep the link storage from ever idling, so it uses up the free compute budget and makes the after-idle latency rows moot? Default now: probe every 5 min against a known active short link. — owner: igorermolenko, due: before `sdd:design`
- [ ] Are the after-idle latency targets (redirect ≤ 1 s, owner operations ≤ 1.5 s) achievable? They rest on the storage vendor's "a few hundred milliseconds" wake-up claim and have not been measured. Default now: keep both targets as written. — owner: igorermolenko, due: after roadmap step 3 (first deploy)
