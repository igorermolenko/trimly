# trimly

A URL shortener: one Spring Boot service. Authenticated link creation API, public redirects.
The source of truth for architecture is [`docs/architecture-map.md`](docs/architecture-map.md) and the
ADRs in [`docs/adr/`](docs/adr/). If this file and the map ever disagree, the map wins — fix this file.

## Commands

| What | Command |
|---|---|
| Build | `./mvnw -B package` |
| Unit tests (`*Test`, Surefire) | `./mvnw -B test` |
| Unit + integration tests (`*IT`, Failsafe, needs Docker) | `./mvnw -B verify` |
| Formatting check (CI fails on it) | `./mvnw -B spotless:check` |
| Format code | `./mvnw spotless:apply` |
| Run locally against a throwaway Postgres | `./mvnw spring-boot:test-run` |

Stack: Java 21, Spring Boot 4.1, Maven via the committed wrapper (`./mvnw`) — [ADR-0001](docs/adr/0001-use-java-21-spring-boot-and-maven.md).

## Modules and layers

- Root package `io.github.igorermolenko.trimly`; `TrimlyApplication` sits at the root.
- Each top-level package under the root is a module: `link` (write path), `redirect` (read path),
  `access` (owner authentication: Spring Security bearer-token chain on `/api/**`, owner-credential
  policy), `shared` (cross-cutting only: error envelope, ID generator, clock — no business logic).
- Layers inside a module: `api` (HTTP controllers, DTOs) / `app` (use cases, transactions) /
  `domain` (plain Java — **no Spring or persistence imports**) / `infra` (persistence, outside adapters).
- Modules use only each other's published `api`/`app` types, through direct method calls (no events or
  HTTP between modules). `ModularityTest` (Spring Modulith `verify()`) fails the build on a violation.
- A new helper goes into `shared` only if at least two modules need it.
- Decision: [ADR-0002](docs/adr/0002-organize-code-as-feature-modules-with-layers.md).

## Conventions

- **Persistence:** Spring Data JDBC repositories, only in a module's `infra` layer. No JPA/Hibernate.
- **Migrations:** Flyway, plain SQL, forward-only, `src/main/resources/db/migration/V<n>__<snake_case_description>.sql`,
  applied on startup. Never edit an applied migration; add the next version.
- **IDs:** primary keys are UUIDv7 from `shared.IdGenerator`. Public short codes are a separate field.
- **Time:** inject `java.time.Clock` (bean in `shared`); don't call `now()` without it.
- **Errors:** RFC 9457 Problem Details (`ProblemDetail`) via the single `@RestControllerAdvice` in
  `shared`. Never expose stack traces or exception messages.
- **Config / secrets:** secrets only from env vars, never committed. The owner credential comes from
  `TRIMLY_OWNER_CREDENTIAL`; trimly refuses to start without a strong one (tests use
  `src/test/resources/config/application.yml`). The DB connection comes from
  `SPRING_DATASOURCE_URL` / `SPRING_DATASOURCE_USERNAME` / `SPRING_DATASOURCE_PASSWORD`.
  trimly also refuses to start without `TRIMLY_PUBLIC_BASE_URL` (the address short links are built
  from: http(s), host and optional port only) and `TRIMLY_OWN_HOSTS` (trimly's own host names,
  which targets may not point to). `application.yml` holds non-secret defaults only.
- Persistence, migrations and IDs: [ADR-0003](docs/adr/0003-store-links-in-postgres-on-neon-via-spring-data-jdbc-and-flyway.md).

## Tests

- JUnit 5 + AssertJ. Tests mirror the main package layout.
- Unit tests are `*Test`; integration tests are `*IT` and import `TestcontainersConfiguration`
  (PostgreSQL via `@ServiceConnection`). Integration tests never touch Neon.
- Formatting: google-java-format via Spotless.

## CI

`.github/workflows/ci.yml` runs `./mvnw -B verify` and `./mvnw -B spotless:check` on every push and
pull request (JDK 21, Corretto). No deploy step yet — Fly.io deployment is a later roadmap step.
