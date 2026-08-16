# Testing and validation

## Approach

- **Unit tests** for pure logic: base62 encoding, URL scheme allow-list, UA device classification, short-code assignment.
- **Slice/integration tests** (`@SpringBootTest` + MockMvc) for HTTP contracts: create, 302 redirect, 404, analytics, soft delete, rate limit.
- Async click writes are asserted with Awaitility (eventual consistency by design).

## What is covered

| Area | Tests |
|---|---|
| Encoder padding and uniqueness-by-construction | `Base62EncoderTest` |
| Reject `javascript:` / `data:` / `file:` / oversize | `UrlValidationServiceTest` |
| Device parsing including missing UA | `UserAgentParserTest` |
| Create + redirect + analytics + multi-click | `UrlShortenerIntegrationTest` |
| Soft-delete keeps analytics, blocks redirect | `UrlShortenerIntegrationTest` |
| Cached redirect 404 after DELETE (eviction, not stale hit) | `UrlShortenerIntegrationTest` |
| DELETE unknown code → 404; DELETE already-inactive → 204 | `UrlShortenerIntegrationTest` |
| Create rate limit → 429 | `RateLimitIntegrationTest` |
| Analytics cap: `clickCount` stays total, `clicks` limited | `AnalyticsCapIntegrationTest` |

## What is not tested (and why)

- **Load / p99 redirect latency** — needs a benchmark harness, not a unit suite.
- **Multi-instance cache invalidation** — Caffeine is in-process; a Redis test would pretend at a production topology we did not build.
- **GeoIP accuracy** — geo is out of v1.
- **H2 file durability across JVM crash** — file mode is configured for local demo, not chaos-tested.
- **Token-bucket refill timing** — we assert “exceed capacity → 429”, not the exact refill clock.
- **Analytics over millions of clicks** — aggregates are in SQL; we do not load-test H2 grouping.

## How to run

```bash
mvn test
```

GitHub Actions workflow `.github/workflows/quality-gate.yml` runs `mvn -B test` on `push` to `main` and on pull requests. A second job requires `docs/ai-traceability.md` to change when `src/` or `pom.xml` changes (not when you only edit docs).

This is not a load test, linter farm, or security scanner. Those are still out of scope for this prototype.
