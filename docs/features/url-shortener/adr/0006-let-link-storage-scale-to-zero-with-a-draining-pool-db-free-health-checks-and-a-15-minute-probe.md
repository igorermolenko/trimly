---
status: Accepted
owner: "igorermolenko"
reviewers: ["Tech Lead"]
updated_at: "2026-11-02"
feature_size: "M"
ticket: "roadmap step 2 — url-shortener (resolves spec §8 open question on the 5-minute uptime probe)"
---

# 0006 — Let link storage scale to zero with a draining pool, DB-free health checks and a 15-minute probe

- **Status:** Accepted
- **Date:** 2026-11-02
- **Deciders:** igorermolenko (owner / Tech Lead), with Claude during the design Socratic walk

## Context

Link storage is Neon Free: 100 CU-hours of compute per month. That is about 400 awake hours at the minimum 0.25 CU, against about 730 hours in a month. Compute suspends 5 minutes after the last connection closes, which Free cannot turn off. When the allowance runs out, compute stays suspended until the next monthly window (sad.md §2). Spec §6 measures redirect availability (99.5 % per month) with "an external uptime probe every 5 min against a known active short link". The spec left open, due before design, whether that probe keeps compute permanently awake (spec §8). It does: a probe every 5 minutes never leaves a 5-minute idle gap. Two further sources would also keep compute awake permanently: Hikari's default pool, which holds idle connections open, and Spring Boot's default health check, which queries the database.

## Decision drivers

- Spec §6: availability 99.5 % per calendar month; running cost ≤ $5 per month.
- Constraint (sad.md §2): the Neon Free allowance, whose exhaustion is a month-long hard outage. A paid Neon plan breaks the $5 budget, so it is not an option here.
- Quality goals 1 and 2 (sad.md §1): honest, available redirects; fast within budget.
- Spec §6 after-idle latency targets (redirect ≤ 1 s, owner ≤ 1.5 s) already assume storage idles and wakes.

## Considered options

1. **Probe every 15 min against the known short link.** Same pass condition as the spec (redirected to the expected target within 5 s). About 35 % awake from probes, ≈ 65 CU-hours per month, leaving ≈ 35 CU-hours for real traffic.
2. **Probe every 30 min against the known short link.** ≈ 18 % awake, ≈ 32 CU-hours, leaving ≈ 68 CU-hours for traffic, but each failed probe counts as 30 minutes of downtime.
3. **Probe every 5 min against an endpoint that does not touch storage, plus one daily full probe.** Almost no CU cost, but the frequent probe no longer measures redirect availability, so a storage outage during the day goes unseen.

With every option, the pool drains when idle (`minimumIdle: 0`, short `idleTimeout`). Health checks use a liveness-only group with no database indicator, and compute is pinned to 0.25 CU.

## Decision outcome

**Chosen:** Option 1, a 15-minute probe, with the draining pool and DB-free health checks. It keeps the spec's definition of "down" (the redirect itself fails). It fits comfortably inside the allowance with headroom for real traffic. Each failure costs 15 of the 216 minutes of monthly downtime budget, which is still a meaningful measurement. Option 2 buys budget headroom at the cost of a measurement too coarse to judge 99.5 %. Option 3 stops measuring what the target is about.

## Consequences

**Positive**
- Link storage can idle between probes and visits, so the allowance lasts the month at personal-use traffic.
- The after-idle latency targets in spec §6 become real, measurable cases rather than moot ones.

**Negative**
- This overrides spec §6's measurement interval (5 min → 15 min). The spec must be synced (sad.md §1 Decision overrides, §11).
- Headroom is moderate: ≈ 140 awake hours a month for traffic. A sustained bot flood of well-formed codes can still use it up (spec §6.1 abuse case; rate limiting is decided in sad.md §8).
- The first request after every idle gap pays the connection + wake cost; the spec accepts this.

**Neutral**
- The arithmetic assumes the 0.25 CU minimum and a ~5.3 min awake window per wake. Verify both against Neon's console after the first deployed week (roadmap step 3).
- Roadmap step 6 (click analytics) will add storage writes on the redirect path. It must be re-checked against this budget.

## Links

- Spec: [[../spec.md]] §6 (availability, cost, after-idle latency), §6.1 (bot-flood abuse case), §8 (open question on the probe)
- SAD: [[../sad.md]] §2, §7, §11
- Related ADR: [[0003-bound-every-link-storage-call-within-2-seconds-using-pool-and-driver-timeouts]]
