# Future work

Intentional non-goals for v1. Another LLM must **not** implement these unless the human explicitly asks.

These items came from trade-offs in ADRs and `docs/final-summary.md`. They are the production upgrade path, not missing homework.

## Ranked backlog (if we continue this product)

| Priority | Item | Why it is next | What it replaces |
|---|---|---|---|
| 1 | Postgres (or similar) instead of H2 file | Real durability, backups, concurrent writers | `jdbc:h2:file:...` |
| 2 | Redis (or equivalent) for redirect cache + rate limit | Multi-instance; Caffeine and the token bucket are per-JVM today | Caffeine + in-memory `RateLimitFilter` |
| 3 | High-entropy random short codes | Sequential Base62 ids are enumerable (ADR-002) | `Base62Encoder.encode(id)` |
| 4 | Click ingestion via outbox / log / queue | `@Async` executor drops clicks when the queue is full (ADR-003) | `ClickRecordingService` thread pool |
| 5 | Optional idempotent shorten | Same long URL → same code, if product wants that | Current “every POST is a new code” |
| 6 | Auth + ownership | Delete/analytics are public if you know the code | Anonymous v1 |
| 7 | Optional per-link 301 | Only if a creator opts into cacheable redirects and accepts weaker analytics | 302-only default (ADR-001) |
| 8 | GeoIP breakdown | Needs an IP DB + retention/privacy decision | Device/referrer from headers only |

## Explicitly not planned

- Frontend / admin UI (assignment is API-only).
- Hashing the long URL as the primary code generator (collisions + we already rejected it).
- Shipping 301 as the only redirect type.

## How to pick work up later

1. Confirm the human wants an item from this table (not a new idea from the model).
2. Write or update an ADR **before** swapping H2, Caffeine, or 302.
3. Add tasks to `docs/decomposition.md` rather than generating a parallel design.
4. Keep `docs/coding-standards.md` and `/api/v1` contracts stable unless the ADR says otherwise.
