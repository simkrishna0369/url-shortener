# Scenario: Greenfield

Build the service from an empty repository using the normalized spec in `docs/requirements.md`.

## Plan

1. Scaffold Spring Boot (web, JPA, validation, cache) with H2 file mode.
2. Model `ShortUrl`, generate 7-char base62 codes, validate long URLs.
3. Ship `POST /api/v1/urls`, `GET /{shortCode}` (302), metadata GET.
4. Add Caffeine cache-aside on redirect lookup.
5. Add `ClickEvent`, async recording, analytics GET.
6. Add create-path rate limiting and structured logs.

## Execution notes

- Controllers stay thin; validation and encoding live in services/utilities so they are unit-testable without Spring.
- Redirect is 302-only (ADR-001).
- Cache stores `RedirectTarget`, not a Hibernate entity.

## Validation

`mvn test` covers encoder, scheme allow-list, create/redirect/analytics happy paths, 404, and rate limiting.

Greenfield baseline is “a working prototype”: create, redirect, count clicks. Delete semantics and analytics *depth* are the later scenarios.
