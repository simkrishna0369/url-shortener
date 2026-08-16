# Coding standards

Source of truth for style and engineering rules. The longer operating prompt is `docs/cursor-implementation-prompt.md`. This file is what another LLM should follow when editing code.

## Language and layout

- Java 17, Spring Boot 3.x, Maven.
- Package root: `com.urlshortener`.
- Packages: `controller`, `service`, `repository`, `dto`, `entity`, `exception`, `config`, `util`.

## Layering

- Controllers: HTTP mapping, status codes, DTO in/out only.
- Services: validation, orchestration, cache annotations, transactions.
- Repositories: Spring Data only — no SQL/JPQL in services.
- Never return a JPA entity from an HTTP endpoint.

## Code quality

- One responsibility per public method. If the name needs “and”, split it.
- No magic numbers/strings — constants or `app.*` properties.
- Validate all external input at the controller/DTO boundary (`@Valid` + `UrlValidationService`).
- Absence: `Optional<T>` from services that look up data. Do not return `null` for callers to check.
- Errors: typed exceptions (`InvalidUrlException`, `ShortUrlNotFoundException`) mapped in `GlobalExceptionHandler`. JSON error body only — no stack traces to clients.

## Config and security

- Cache TTL, rate limits, max URL length, base URL: `application.yml`, overridable by env (`APP_*`).
- Allow `http`/`https` only. Reject `javascript:`, `data:`, `file:`.
- Rate-limit `POST /api/v1/urls` per IP.
- Do not commit secrets. H2 local credentials stay in config as empty/`sa` for the prototype only.

## Logging

- SLF4J only. No `System.out.println`.
- INFO: create, redirect, deactivate. WARN: rate-limit, dropped clicks. ERROR: unexpected failures.
- Put `shortCode` in MDC for traceable operations. Do not log full untrusted URL strings on reject paths.

## Testing

- Unit tests for encoder, URL validation, UA parsing, service logic.
- Integration tests (`@SpringBootTest` + MockMvc) for each endpoint: happy path + at least one failure.
- Method names: `should_<expectedBehavior>_when_<condition>`.
- Tests must not depend on order or leftover rate-limit buckets across classes (use a separate `@TestPropertySource` context for 429).
- Async click writes: wait with Awaitility, do not `Thread.sleep` with a guessed constant unless unavoidable.

## Git

- Small commits. Message form: `feat|fix|docs|test|refactor: <why>`.
- No commented-out dead code.
