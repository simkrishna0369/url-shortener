# Final summary

## Plan and rationale

The assignment is as much about **how** the system was designed with AI as about the binary. Work started from a vague prompt, was normalized (`docs/requirements.md`), decomposed (`docs/decomposition.md`), locked (`docs/cursor-implementation-prompt.md`), then implemented as a Spring Boot API with tests and ADRs.

Stack choice (Java 17, Spring Boot, H2 file, Caffeine, Maven) favors a **single-command local run** with no Docker/Postgres/Redis, while still showing real layering, caching, and validation.

## Key technical choices

| Choice | Why |
|---|---|
| Base62(id), 7 chars | Collision-free; `62^7` ≈ 3.5 trillion codes; Base64 rejected for URL-unsafe `+/` (ADR-002) |
| HTTP 302 | Protects click accounting (ADR-001) |
| H2 file, not SQLite or Postgres | Spring/JPA-native embedded DB; SQLite is a worse JPA fit; Postgres needs an extra process (ADR-004) |
| Caffeine, not Redis, Guava, or a HashMap | Best-in-class **local** cache: TTL + max size, W-TinyLFU eviction, first-class Spring `CaffeineCacheManager`. Redis/Hazelcast win only when several JVMs must share cache and eviction; a raw map has no bounds. v1 is one process, so Caffeine is faster and simpler (ADR-005) |
| Async clicks | Redirect latency over perfectly live counts (ADR-003) |
| Soft delete | Analytics must outlive “stop redirecting” |
| No auth | Not in the prompt; would dominate the take-home |

## Risks and trade-offs

- **Guessable codes** — sequential IDs are enumerable. Fine for an anonymous demo; wrong if short links are a secret.
- **In-process rate limit and cache** — per JVM. Two instances will not share buckets or eviction.
- **Eventual click counts** — a read immediately after 302 may still show 0; clients should tolerate a short delay.
- **Capped click details** — `clickCount` and `breakdown` cover all events; `clicks` is the latest N (default 100) so one popular code cannot serialize its full history.
- **Dropped clicks** when the async queue is full — we chose redirect availability over perfect analytics.
- **Open redirects to user-supplied http(s) URLs** — that *is* the product. We only block dangerous schemes.

## Assumptions

- Duplicate long URLs get **new** short codes each time.
- No ownership model; anyone who knows the code can read analytics or delete.
- “Reliability” in v1 means validation, caching, rate limiting, and 404s — not HA.

## Limitations / future work

- Postgres (or similar) + Redis for multi-instance deploys
- Unguessable random codes if links are sensitive
- Optional idempotent shorten-by-hash
- Auth and per-user namespaces
- GeoIP **if** product and privacy owners accept IP retention
- Outbox/stream for click ingestion at high QPS

These are omitted on purpose so the prototype stays demonstrable in one process.
