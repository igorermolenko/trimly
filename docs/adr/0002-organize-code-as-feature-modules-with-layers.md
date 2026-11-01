---
status: Accepted
owner: "igorermolenko"
reviewers: []
updated_at: "2026-11-01"
feature_size: "n/a (repo foundation)"
ticket: "n/a"
---

# 0002 — Organize code as feature modules with layers inside, with boundaries enforced by tests

- **Status:** Accepted
- **Date:** 2026-11-01
- **Deciders:** igorermolenko (owner), with Claude during the `survey` foundation session

## Context

trimly ships as one deployable service, but it will grow in increments: first the core shortener, then custom aliases, then click analytics (`docs/idea-brief.md` §7). How packages are organized decides whether those increments slot in cleanly. Changing the structure later means moving most files.

## Decision drivers

- System design is a learning goal, so boundaries between the read path (redirects) and the write path (link creation) should be explicit (`docs/idea-brief.md` §4).
- Agent-written code needs structure that a machine can check, so drift is caught by the build rather than in review.
- Keep the ceremony proportionate to a small service.

## Considered options

1. **Feature modules with layers inside**: modules such as `link` and `redirect`, each split into `api` / `app` / `domain` / `infra`, with a boundary test.
2. **Classic layers**: top-level `controller` / `service` / `repository` packages.
3. **Full hexagonal**: ports and adapters for every outside dependency, in every module.

## Decision outcome

**Chosen:** feature modules with layers inside. It's one Spring Boot app. Each top-level package under `io.github.igorermolenko.trimly` is a module. `domain` holds no Spring or persistence imports. Modules use only each other's published `api`/`app` types. A Spring Modulith verification test fails the build on a boundary violation. This gives a clean read/write split and a place for future features, without full hexagonal's boilerplate.

## Consequences

**Positive**
- New increments (aliases, analytics) become new modules or slot into existing ones without tangling.
- Boundary violations by agents fail `./mvnw test`, not just code review.

**Negative**
- More packages and a bit more ceremony than classic layers for a very small codebase.
- Adds the Spring Modulith dependency.

**Neutral**
- A module can later be extracted into its own service, because its boundary is already explicit. Doing that is a design decision, not part of this ADR.

## Links

- Brief: [[../idea-brief.md]] §7
- Architecture map: [[../architecture-map.md]] §Module inventory, §Conventions
- Related ADR: [[0001-use-java-21-spring-boot-and-maven]]
