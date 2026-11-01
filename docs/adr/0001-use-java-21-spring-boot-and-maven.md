---
status: Accepted
owner: "igorermolenko"
reviewers: []
updated_at: "2026-11-01"
feature_size: "n/a (repo foundation)"
ticket: "n/a"
---

# 0001 — Use Java 21 with Spring Boot 4.1 and Maven

- **Status:** Accepted
- **Date:** 2026-11-01
- **Deciders:** igorermolenko (owner), with Claude during the `survey` foundation session

## Context

trimly is an empty repo. It will be a learning / portfolio URL shortener built mainly by coding agents, and it must be deployed always-on for about $5 per month (`docs/idea-brief.md` §5, §7). Every later feature depends on the language, framework and build tool, and changing them later means a rewrite.

## Decision drivers

- Agents write most of the code, so the owner must review it critically. The owner reviews Java most fluently (owner-reported, survey).
- The learning goals are system design and deployment, and Java backend material mostly assumes Spring (`docs/idea-brief.md` §4).
- Budget of about $5 per month: memory footprint decides the machine size. A 512 MB Fly.io Machine costs $3.69 per month (https://docs.fly.io/about/pricing, checked 2026-11-01).
- JDK 21 (Amazon Corretto 21.0.8) is already installed locally.

## Considered options

1. **Spring Boot + Maven**: the most widely used Java backend stack; build file in XML.
2. **Spring Boot + Gradle (Kotlin DSL)**: same framework, with a shorter, incremental build written as code.
3. **Quarkus**: container-oriented, with a smaller memory footprint and an optional native image; smaller ecosystem.
4. **Javalin**: a minimal HTTP library with very low memory; everything else is wired by hand.
5. **Python service**: considered in the interview; ruled out because the owner reviews Java more fluently.

## Decision outcome

**Chosen:** Java 21 with Spring Boot 4.1.x (Spring Initializr's default on 2026-11-01 was 4.1.2; the exact version is pinned at scaffold), built with Maven through the committed Maven Wrapper. Spring Boot gives the best documentation and agent familiarity for the learning goals. Maven's single conventional layout keeps agent-made build changes easy to review.

## Consequences

**Positive**
- Agents produce more reliable code in a stack they have seen widely, and the owner can review it fluently.
- The Maven Wrapper means no global build-tool install, locally or in CI.

**Negative**
- Spring Boot's idle memory is the highest of the candidates. The app needs the 512 MB machine ($3.69/month) rather than the 256 MB one ($2.19/month).
- Maven builds are slower on repeat runs than Gradle's incremental builds.

**Neutral**
- Java 25 is a newer LTS that Spring Initializr also offers. Moving from 21 to 25 later is a small, routine change.

## Links

- Brief: [[../idea-brief.md]] §5, §7
- Architecture map: [[../architecture-map.md]] §Stack
- Related ADR: [[0002-organize-code-as-feature-modules-with-layers]], [[0003-store-links-in-postgres-on-neon-via-spring-data-jdbc-and-flyway]]
