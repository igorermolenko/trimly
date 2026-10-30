---
status: Draft
owner: "igorermolenko"
updated_at: "2026-10-30"
depth: "medium"
---

# Idea brief — trimly

## 1. Raw idea

> Trimly is a simple and efficient URL shortener designed to convert long, unwieldy web links into short, memorable URLs.
>
> What it does: It takes long URLs and generates compact aliases, handles instant redirects to original links, and can later support custom aliases and basic click analytics.
>
> Who it's for: Content creators, marketers, and active internet users who regularly share links on social media, messengers, or presentations and need them to look clean and concise.
>
> The problem/annoyance behind it: Long URLs filled with tracking parameters look cluttered, break formatting in text messages, and are impossible to type or remember easily. Trimly fixes this by making link sharing clean, fast, and user-friendly.

## 2. Problem

Long URLs, especially ones carrying tracking parameters, look cluttered, break formatting in text messages, and are hard to type or remember (owner-reported; no incident data or user research supplied). The outcome the owner wants: a person sharing a link gets a short alias that redirects to the original instantly. Custom aliases and basic click analytics are named as possible later additions, not part of the first cut (owner-reported).

## 3. Users

- Content creators, marketers and active internet users who share links on social media, in messengers or in presentations (owner-reported target; no specific users or segments validated yet — assumption).
- People who click a short link and get redirected (implied by the redirect behaviour).
- Operator: the owner, who builds and runs it as a learning / portfolio project (owner decision).
- Portfolio readers (e.g. reviewers or interviewers looking at the code and deployment): an implied audience given the portfolio purpose (assumption).

## 4. Why now

No external trigger or deadline established. Trimly is a learning / portfolio project (owner decision). The owner wants to use it to learn agent development, system design and deployment (owner-reported). A URL shortener is a small enough domain to finish, and it still raises real design questions such as short-code generation, redirect latency and analytics.

## 5. Out of scope

Agreed exclusions:
- **AI or LLM features inside the product.** The "agent development" goal means building trimly *with* coding agents, not putting an agent in the product (owner decision).
- **Anonymous public link creation, and the abuse handling it would need** (phishing/spam detection, takedowns, rate limits for strangers). Only the owner and invited users can create links; following a short link stays public (owner accepted the recommendation).
- **Competing with commercial shorteners** such as Bitly or TinyURL. This is a learning / portfolio project (owner decision).

Deferred, not excluded: custom aliases and basic click analytics. The owner described these as things trimly "can later support", so they are outside the first cut (owner-reported).

Constraints:
- Language: Java or Python (owner-required). Not yet chosen; see §7 and §8.
- Development is agent-driven (owner-required, as the learning goal).
- The service is actually deployed, not only run locally (owner-required, as the learning goal).
- Hosting budget: about $2–5 per month for an always-on deployment. This replaces the original "free tier only" because free tiers can't give instant redirects or keep data (owner decision after the evidence in §6). Fly.io is one candidate that fits; the host itself is not yet chosen.

## 6. Risks

1. **Hosting cost versus keeping data.** Free tiers could not give instant redirects or durable data (source-verified, see the ledger), so the owner raised the budget to about $2–5 per month. The cheapest always-on Fly.io Machine costs $2.19 per 30 days (source-verified). The cost of the database or storage within that budget has not been checked; Fly's managed Postgres starts at $38/month, far over budget. A storage approach that fits about $5 per month is an assumption until the design stage checks it.
2. **Abuse of public redirects.** Only the owner and invited users can create links, which leaves little room for abuse. A leaked creation credential would reopen the risk (assumption). This choice is reversible: opening creation to everyone later would bring abuse handling back into scope.
3. **Agent-written code the owner cannot review well.** Development is agent-driven, so the owner's fluency in the chosen language limits how well they can review the code (assumption; fluency not yet stated).

Evidence ledger:

| Claim / question | Class | Source / origin | Finding / limit |
|---|---|---|---|
| Purpose is learning / portfolio | owner-reported | interview Q2 | owner decision |
| Learning goals: agent development (building *with* agents), system design, deployment | owner-reported | interview Q3–Q4 | owner decision |
| Link creation limited to owner and invited users; redirects public | owner-reported | interview Q5 | owner accepted the recommendation |
| Budget: free tier only; candidate hosts Fly.io or Render | owner-reported | interview Q6 | Replaced in Q7 |
| Budget raised to about $2–5 per month for always-on hosting | owner-reported | interview Q7 | owner decision after seeing the free-tier evidence |
| Fly.io free tier for new organizations | source-verified | https://docs.fly.io/about/pricing, fetched 2026-10-30 | None. Trial of 2 machine-hours or 7 days; cheapest always-on Machine $2.19/30 days; managed Postgres from $38/month; legacy pre-October-2024 plans keep their free allowances |
| Render free web service behaviour | source-verified | https://render.com/docs/free, fetched 2026-10-30 | Spins down after 15 minutes without inbound traffic; spin-up "takes about one minute"; 750 free instance-hours per workspace per month |
| Render free Postgres | source-verified | https://render.com/docs/free, fetched 2026-10-30 | Expires 30 days after creation; 1 GB fixed storage |
| Open shorteners attract phishing and spam | assumption | general industry knowledge, not verified in this interview | Not checked; only matters if link creation is opened to everyone |

Critique: done inline by the interviewer, not by an independent reviewer. An independent devil's-advocate pass was not run because the decisions so far are reversible and low-stakes.

## 7. Recommendation

**Direction (owner accepted the proposed first cut, Q8).** Build trimly as a learning / portfolio URL shortener in increments, using the agent-driven SDD pipeline.

- **First cut:** the owner and invited users create short links with generated codes after authenticating. Anyone who opens a short link is redirected right away. The service runs always-on at a public URL for about $5 per month or less. Automated tests and CI deployment come with it.
- **Later increments:** custom aliases, then basic click analytics. Each is a system-design learning step (owner-reported as later features; ordering proposed, roadmap to confirm).
- **What success looks like:** a link created on the deployed service redirects correctly from a fresh browser, and the repo shows the spec → design → tasks → TDD trail (owner accepted).

**Main tradeoff.** A small monthly fee buys always-on hosting and data that persists, instead of $0 with about one-minute cold starts and a database deleted after 30 days (owner decision, Q7).

**Rejected credible alternatives:**
- A fully public, anonymous shortener: it would make abuse handling core scope (Q5).
- An AI feature inside the product: the agent learning goal is about the development workflow (Q4).
- Render's free tier: cold starts and 30-day database expiry (Q7, source-verified).
- Using an existing shortener with no build: it doesn't serve the learning goal (Q2).

**Language (proposal, not a decision).** Java or Python is an owner-required constraint. The interviewer suggests picking the language the owner reviews most fluently, because agent-written code has to be reviewed critically. The interviewer leans Java if that is the owner's stronger language. The final choice belongs to the foundation step (`survey`).

Outcome: ready-for-roadmap

Next action: run `/sdd:roadmap` to break trimly into increments, then `/sdd:survey` to fix the stack and foundation (greenfield repo) before specifying the first cut.

## 8. Open questions

| Question | Consequence | Blocking? | Verification action | Owner |
|---|---|---|---|---|
| Java or Python? | Shapes the whole stack, tooling and how well the owner can review agent-written code | Nonblocking for roadmap; must be settled before scaffolding | Owner states which language they review more fluently; settle it in `/sdd:survey` | igorermolenko |
| Can the database or storage plus always-on compute fit in about $5 per month on the chosen host? | Fly's managed Postgres ($38/month minimum) breaks the budget, so the storage approach must change or the budget must be revisited | Nonblocking for roadmap; blocking for design | Check current pricing for the candidate storage options during design | igorermolenko |
| How do invited users get access to link creation? | Affects the security surface and the first-cut scope | Nonblocking | Settle in `/sdd:specify` for the first cut | igorermolenko |
| Short domain name (buying a short custom domain or using the host's subdomain) | Affects cost and how "short" the links really are | Nonblocking | Owner decides; check domain cost against the budget | igorermolenko |
