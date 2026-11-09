---
id: T3
title: "Add ShortCode (alphabet, length, well-formedness) and the SecureRandom ShortCodeGenerator"
layer: "domain"
deps: []
blocks: ["T5"]
acs: ["AC-01", "AC-16b"]
files_hint: ["src/main/java/io/github/igorermolenko/trimly/link/domain/ShortCode.java", "src/main/java/io/github/igorermolenko/trimly/link/domain/ShortCodeGenerator.java", "src/test/java/io/github/igorermolenko/trimly/link/domain/ShortCodeTest.java", "src/test/java/io/github/igorermolenko/trimly/link/domain/ShortCodeGeneratorTest.java"]
owner: "igorermolenko"
estimate: "S"
context_budget: "S"     # 38 inlined lines
status: "done"
step: "COMMIT"
---

<!-- To the executing agent: work from what is inlined here. If a slice is insufficient, ambiguous,
or contradicts the code in front of you, open the named file for the full text and follow that.
Do not invent the missing part. Every quote ends with its provenance signature; the source wins. -->

# T3 — Add ShortCode (alphabet, length, well-formedness) and the SecureRandom ShortCodeGenerator

## Place in the sequence

- **Blocked by:** nothing — starts in wave 1
- **Blocks:** T5 — Persist ShortLink through a Spring Data JDBC store with constraint-aware failure translation
- **Wave:** 1 — no dependencies.
- **Lane:** own lane.

## Why (user story)

> **As an** owner
> **I want** to submit a target URL and get back a new short link
> **So that** I can share a clean link that is easy to type by hand
>
> — `spec.md §4, US-01, verbatim` · full text: [spec.md](../spec.md)

This task fixes the one place that defines what a short code is and how a fresh one is drawn.

## Inlined context

> Each create inserts a fresh 7-character code from the 56-character alphabet. [...] The alphabet and the "is this a well-formed short code" rule live in exactly one place, `link`'s domain.
>
> — `sad.md §4, choice 2, abridged` · full text: [sad.md](../sad.md)

> - The code generator is injectable, so tests can force collisions deterministically (AC-20b).
>
> — `adr/0002-generate-short-codes-with-securerandom-and-enforce-uniqueness-with-a-unique-index-and-insert-retry.md §Consequences (Neutral), verbatim` · full text: [adr/0002-generate-short-codes-with-securerandom-and-enforce-uniqueness-with-a-unique-index-and-insert-retry.md](../adr/0002-generate-short-codes-with-securerandom-and-enforce-uniqueness-with-a-unique-index-and-insert-retry.md)

> **Hard rule:** `domain` has no Spring or persistence imports. Modules call only each other's published `api`/`app` types, through direct method calls. `ModularityTest` (Spring Modulith `verify()`) fails the build on any violation.
>
> — `sad.md §2, Technical constraints (Architecture bullet), abridged` · full text: [sad.md](../sad.md)

**Fallback:** insufficient or contradicted by the code → read the named file in full
([spec.md](../spec.md) · [sad.md](../sad.md) · [data-model.md](../data-model.md) ·
[openapi.yaml](../contracts/openapi.yaml) · [adr/](../adr/)) and follow it. Do not guess.

## Data delta

No DB changes.

## API contract

- `ShortCode` schema: `minLength: 7`, `maxLength: 7`, `pattern: "^[A-HJ-NP-Za-km-np-z2-9]{7}$"` — this task is the Java source of truth the pattern mirrors.

— `contracts/openapi.yaml, operationId components.schemas.ShortCode, abridged` · full text: [openapi.yaml](../contracts/openapi.yaml)

## Acceptance criteria

### AC-01 — happy path

> **Given** the owner presents the owner credential
> **When** the owner submits an absolute web address using http or https, at most 2048 characters long, as the target URL
> **Then** the system records a new active short link with a freshly generated 7-character short code drawn from the short-code alphabet (A–Z, a–z and 2–9 with exactly these six look-alike characters removed: 0, O, o, 1, l, I; 56 characters in all), where letter case matters and each character is chosen by a cryptographically secure random source, so the code cannot be predicted from earlier codes. The target URL is stored and later redirected to exactly as submitted, with no normalisation. The owner gets back the full short link, its short code and its target URL. The full short link is always built from trimly's single configured public address, whatever address the request arrived on
>
> — `spec.md §5, AC-01, verbatim` · full text: [spec.md](../spec.md)

### AC-16b — error

> **Given** a visitor opens an address whose shape cannot be a short code (wrong length, characters outside the short-code alphabet defined in AC-01, extra path segments, or trailing punctuation such as "abc2345.")
> **When** trimly handles it
> **Then** the visitor gets "link not found" without trimly looking anything up in its link storage, so the answer stays "link not found" even while the link storage is unavailable. A single trailing slash after a short code and any query string the visitor's address carries do not make it malformed: they are accepted as in AC-07
>
> — `spec.md §5, AC-16b, verbatim` · full text: [spec.md](../spec.md)

## Checklist

- [ ] `src/main/java/io/github/igorermolenko/trimly/link/domain/ShortCode.java`: value type (record) with `ALPHABET` (56 chars, exactly A–Z a–z 2–9 minus `0 O o 1 l I`), `LENGTH = 7`, `static boolean isWellFormed(String raw)` and `static ShortCode of(String)` (throws on malformed). No Spring imports.
- [ ] `src/main/java/io/github/igorermolenko/trimly/link/domain/ShortCodeGenerator.java`: draws 7 chars uniformly from `ALPHABET` using an injected `java.util.random.RandomGenerator` (production: `SecureRandom`). Plain Java; Spring wiring comes later (T7).
- [ ] Tests: `src/test/java/io/github/igorermolenko/trimly/link/domain/ShortCodeTest.java`, `src/test/java/io/github/igorermolenko/trimly/link/domain/ShortCodeGeneratorTest.java`.

## Edge cases

| Case | Behaviour |
|---|---|
| `abc2345.` / 6 or 8 chars / `abc 234` | `isWellFormed` = false (AC-16b). |
| Contains `0`, `O`, `o`, `1`, `l` or `I` | `isWellFormed` = false; generator never emits them. |
| `null` or empty | `isWellFormed` = false, no exception. |
| Same letters, different case | Two different codes (case matters). |
| Alphabet size | Exactly 56 distinct characters — asserted in a test. |

## Definition of Done

- [ ] `ShortCodeTest` passes: alphabet = 56 chars, every look-alike excluded, well-formedness table from Edge cases.
- [ ] `ShortCodeGeneratorTest` passes: 10 000 generated codes are all well-formed; a seeded generator is deterministic.
- [ ] No Spring/persistence import in `link.domain` (`ModularityTest` green).
- [ ] every Hard Rule inlined above still holds
- [ ] lint + vet clean (`./mvnw -B spotless:check`, `./mvnw -B verify`)
