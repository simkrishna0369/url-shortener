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
| Create rate limit → 429 | `RateLimitIntegrationTest` |

## What is not tested (and why)

- **Load / p99 redirect latency** — needs a benchmark harness, not a unit suite.
- **Multi-instance cache invalidation** — Caffeine is in-process; a Redis test would pretend at a production topology we did not build.
- **GeoIP accuracy** — geo is out of v1.
- **H2 file durability across JVM crash** — file mode is configured for local demo, not chaos-tested.
- **Token-bucket refill timing** — we assert “exceed capacity → 429”, not the exact refill clock.

## How to run

```bash
mvn test
```
