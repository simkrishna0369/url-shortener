# Architecture — URL Shortener

## 1. High-level design

Single Spring Boot 3 process. Persistence is H2 in **file mode** so data survives restarts (why H2 rather than SQLite: [ADR-004](docs/adr/004-h2-vs-sqlite.md)). Redirect lookups are cache-aside in **Caffeine** (why not Redis or a HashMap: [ADR-005](docs/adr/005-caffeine-vs-redis.md)). Click writes are **asynchronous** so they do not sit on the redirect hot path.

```
Client
  |  POST /api/v1/urls
  |  GET  /{shortCode}          ──► 302 Location: long URL
  |  GET  /api/v1/urls/{code}
  |  GET  /api/v1/urls/{code}/analytics
  |  DELETE /api/v1/urls/{code}
  v
+------------------+     +------------------+     +------------------+
| Controllers      | --> | Services         | --> | Repositories     |
| (HTTP only)      |     | validation,      |     | Spring Data JPA  |
|                  |     | codes, analytics |     |                  |
+------------------+     +--------+---------+     +--------+---------+
                                  |                        |
                         +--------v---------+     +--------v---------+
                         | Caffeine cache   |     | H2 file DB       |
                         | key=shortCode    |     | short_urls       |
                         | val=RedirectTarget|    | click_events     |
                         +------------------+     +------------------+
```

Layering is strict: controllers do not contain business rules; services do not contain JPQL.

## 2. Database schema

### `short_urls`

| Column | Type | Notes |
|---|---|---|
| `id` | BIGINT PK | Auto-increment; source of the short code |
| `long_url` | VARCHAR(2048) | Original URL |
| `short_code` | VARCHAR(16) UNIQUE | 7-char base62 encoding of `id` |
| `created_at` | TIMESTAMP | Set on insert |
| `active` | BOOLEAN | Soft-delete flag (`false` = deactivated) |

### `click_events`

| Column | Type | Notes |
|---|---|---|
| `id` | BIGINT PK | |
| `short_url_id` | BIGINT FK | Stays after deactivation so analytics survive |
| `clicked_at` | TIMESTAMP | |
| `user_agent` | VARCHAR(512) | Raw header, truncated |
| `referrer` | VARCHAR(2048) | `Referer` header if present |
| `device_type` | VARCHAR(32) | Derived: `mobile` / `desktop` / `tablet` / `bot` / `unknown` |

## 3. API endpoints

| Method | Path | Status | Purpose |
|---|---|---|---|
| `POST` | `/api/v1/urls` | 201 | Create short URL. Body: `{"url":"https://..."}` |
| `GET` | `/{shortCode}` | 302 | Redirect. 404 if missing or inactive |
| `GET` | `/api/v1/urls/{shortCode}` | 200 | Metadata (includes `active`) |
| `GET` | `/api/v1/urls/{shortCode}/analytics` | 200 | Count, timestamps, device/referrer breakdown |
| `DELETE` | `/api/v1/urls/{shortCode}` | 204 | Soft delete (`active=false`) + cache eviction |

Error body shape:

```json
{"error":"NOT_FOUND","message":"...","timestamp":"..."}
```

Common codes: `400 INVALID_URL` / `VALIDATION_ERROR`, `404 NOT_FOUND`, `429 RATE_LIMITED`.

## 4. Redirect flow

1. Match `/{7-char base62}`.
2. Lookup `RedirectTarget` from Caffeine; on miss, load `active = true` row from H2 and cache it.
3. Enqueue an async click write (user-agent, referrer, device class).
4. Return **302** with `Location: longUrl`. Never 301 — browsers would cache the hop and skip later clicks ([ADR-001](docs/adr/001-redirect-status.md)).

## 5. Short-code generation

`short_code = base62(id)` padded to 7 characters. Collision-free by construction. **~3.52 trillion** 7-char codes (`62^7`); Base64 was rejected (URL-unsafe `+/`). Capacity math and trade-offs: [ADR-002](docs/adr/002-short-code-generation.md).

## 6. Redirect cache (Caffeine)

We cache `shortCode` → `RedirectTarget` **in-process** with TTL and a max size. That is the right default for a **single JVM**: a hit is a memory lookup, Spring Boot wires Caffeine natively, and we still evict on soft-delete.

We did **not** pick Redis (or Hazelcast) for v1: those are the right tools when **more than one instance** must share entries and invalidation. They add a process, a network hop, and a “cache down” failure mode this prototype does not have. We also did **not** use a `ConcurrentHashMap` (no TTL/size) or Guava Cache (Caffeine is its successor and Spring’s first-class local cache). Full trade-offs: [ADR-005](docs/adr/005-caffeine-vs-redis.md). Redis remains item 2 in [docs/future-work.md](docs/future-work.md).

## 7. Reliability choices (prototype)

| Concern | Choice |
|---|---|
| Redirect latency vs DB | Caffeine cache-aside (not Redis/HashMap); TTL/size in config ([ADR-005](docs/adr/005-caffeine-vs-redis.md)) |
| Click write vs redirect latency | Async executor; drop clicks if the queue is full ([ADR-003](docs/adr/003-async-click-recording.md)) |
| Abuse of create | Per-IP token bucket on `POST /api/v1/urls` |
| Unsafe destinations | Allow-list `http`/`https`; reject `javascript:`, `data:`, `file:` |
| Deletion vs analytics | Soft delete so click rows remain ([brownfield scenario](docs/scenarios/brownfield.md)) |

## 8. What this prototype is not

Horizontal scale, Redis, auth, and precise geo are out of scope. See [docs/final-summary.md](docs/final-summary.md).
