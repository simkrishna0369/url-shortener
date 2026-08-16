# Requirements — URL Shortener

## 1. Source Requirement (as given)

> "Build a URL shortener service from scratch with core APIs, analytics, and reliability
> features."

This is intentionally high-level. This document normalizes it into a concrete, buildable
engineering spec, and explicitly separates **stated requirements**, **reasonable inferred
requirements**, and **ambiguities with a chosen interpretation**.

---

## 2. Functional Requirements (Normalized)

### 2.1 Core APIs (explicitly required)
| # | Requirement | Notes |
|---|---|---|
| FR1 | Create a short URL from a long URL | `POST /api/urls` |
| FR2 | Redirect from short URL to original long URL | `GET /{shortCode}` |
| FR3 | Retrieve metadata for a short URL | `GET /api/urls/{shortCode}` |

### 2.2 Analytics (explicitly required, scope ambiguous — see §3)
| # | Requirement |
|---|---|
| FR4 | Track click count per short URL |
| FR5 | Track click timestamp per click (enables time-series views later) |
| FR6 | Expose analytics via `GET /api/urls/{shortCode}/analytics` |

### 2.3 Reliability (explicitly required, scope ambiguous — see §3)
| # | Requirement |
|---|---|
| FR7 | Redirect path must be low-latency and resilient to DB load → caching (Caffeine) on hot lookups |
| FR8 | Input validation on URL creation (malformed/unsafe URLs rejected) |
| FR9 | Rate limiting on URL creation to prevent abuse |
| FR10 | Graceful handling of non-existent / expired short codes (404, not 500) |

### 2.4 Inferred but reasonable (not explicitly stated, standard for this domain)
| # | Requirement | Rationale |
|---|---|---|
| FR11 | Short codes must be collision-free | Core correctness requirement, implicit in "URL shortener" |
| FR12 | Optional expiry (TTL) on short URLs | Common, low-cost addition; also useful as the **ambiguous-requirement demo scenario** |
| FR13 | Optional custom alias support | Common feature; candidate for ambiguous-scenario demo instead of expiry — one will be chosen, not both, to keep scope tight |

---

## 3. Ambiguities Identified & Normalization Decisions

The assignment explicitly rewards **identifying ambiguity**, so these are called out rather
than silently assumed.

### A1. What does "analytics" mean?
- **Possible interpretations:** (a) raw click count only, (b) count + timestamps, (c) full
  breakdown by referrer/geo/device/user-agent.
- **Decision:** Start with (b) — count + timestamp per click — as the core deliverable
  (FR4-FR6). Interpretation (c) is the **locked ambiguous-requirement scenario**
  (`docs/scenarios/ambiguous-requirement.md`) — analytics depth (geo/device/referrer
  breakdown), chosen over custom-alias and expiry-semantics alternatives because it has
  the richest interpretation space to reason through (data availability constraints,
  privacy considerations, and genuine ambiguity in what "device" or "geo" should mean
  from a redirect request alone, without a client-side JS beacon).

### A2. What does "reliability" mean?
- **Possible interpretations:** (a) basic input validation + error handling, (b) caching for
  performance under load, (c) rate limiting / abuse prevention, (d) high-availability /
  multi-instance deployment concerns.
- **Decision:** In scope for this prototype: (a), (b), (c) — all achievable and demonstrable
  in a single-instance prototype. (d) is explicitly **out of scope** and documented as a
  limitation/future work item, since HA is an infra/deployment concern beyond a take-home
  prototype's reasonable boundary.

### A3. Are short URLs mutable/deletable, and do they belong to a user?
- Nothing in the prompt implies authentication or multi-tenancy.
- **Decision:** No auth/ownership model for v1 (all links are effectively public/anonymous).
  Deletion/deactivation of a link is included as a minimal admin capability
  (`DELETE /api/urls/{shortCode}`) since without it there's no way to retire a bad link —
  this becomes a natural **brownfield scenario** candidate later (e.g., "soft delete" instead
  of hard delete, once initial hard-delete behavior turns out to be wrong for analytics
  retention).

### A4. Short code generation strategy
- **Possible interpretations:** sequential ID + base62 encoding, random string + collision
  check, hash-based (e.g., MD5 of URL truncated).
- **Decision:** Base62-encoded auto-increment ID. Deterministic, collision-free by
  construction, simple to reason about. Documented as ADR-002 with trade-offs vs.
  alternatives (e.g., hash-based approach allows idempotent re-shortening of the same URL
  but risks collisions at truncation length).

### A5. Redirect cache (in-process vs distributed)
- **Possible interpretations:** no cache (always hit H2), `ConcurrentHashMap`, Guava Cache,
  Caffeine, Redis / Hazelcast for a shared cache.
- **Decision:** Caffeine cache-aside on short-code → `RedirectTarget`. Bounded TTL and max
  size, first-class Spring Boot support, no extra process. Redis is the upgrade when more
  than one JVM must share hits and eviction after delete (ADR-005; `docs/future-work.md`).

### A6. What happens on duplicate submissions of the same long URL?
- **Decision (assumption, not spec-driven):** Each `POST` creates a new short code, even for
  a duplicate long URL (simplest, avoids surprising shared-state behavior). Documented as an
  assumption; noted as a reasonable alternative design in the final summary.

---

## 4. Non-Functional Requirements

- **Testability:** Unit tests for core logic (encoding, validation, cache), integration
  tests for API endpoints.
- **Security baseline:** input sanitization on submitted URLs (reject `javascript:`, `data:`
  schemes etc. to prevent open-redirect/XSS-adjacent abuse), rate limiting on write endpoint.
- **Maintainability:** layered architecture (controller / service / repository), no business
  logic in controllers.
- **Runnability:** must run locally with a single command, no external service dependencies
  (hence embedded H2, in-process Caffeine cache).

## 5. Explicitly Out of Scope (v1)

- Authentication / user accounts / ownership
- Multi-instance horizontal scaling / distributed cache (Redis) — noted as production
  upgrade path in final summary
- Geo/device/referrer analytics breakdown — see ambiguous-requirement scenario for how this
  *would* be approached
- Admin UI (API-only prototype)

---

*This document is a living artifact — updates as decomposition and implementation surface
new ambiguity are expected and will be appended, not silently edited out.*
