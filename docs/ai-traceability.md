# AI traceability log

One entry per implementation chunk. High-impact items are marked **⚠ HIGH-IMPACT**.

| ID | Task | Outcome | Notes |
|---|---|---|---|
| T0.1–0.5 | Scaffold, H2 file config, Caffeine bean, packages, exception handler | **Accepted** | Maven / Java 17 / Spring Boot 3.4.2 |
| T1.1–1.6 | Entity, repository, Base62, URL validation, create API, tests | **Accepted** | `active` column present from the start |
| T2.1–2.4 | Redirect 302, cache-aside, 404, tests | **Accepted ⚠** | Redirect hot path. Cached `RedirectTarget` DTO, not JPA entity |
| T3.1–3.4 | Click events, async record, analytics count/timestamps, tests | **Accepted ⚠** | Async write; eventual consistency on count |
| T4.1–4.3 | Rate limit, structured logging, config externalization | **Accepted ⚠** | In-process token bucket; not cluster-safe |
| T5.1–5.6 | Soft delete, query filters, analytics retention, cache eviction | **Accepted ⚠** | Brownfield: hard delete would break FR6 |
| T6.1–6.6 | Analytics depth interpretation + UA/referrer/device | **Accepted ⚠** | Schema + API shape. Geo omitted on purpose |
| T7 | README, ARCHITECTURE, PROMPTS, ADRs, scenarios, summary | **Accepted** | Assignment deliverables |

## ⚠ High-impact sign-offs

1. **Redirect hot path / cache** — Cache immutable `RedirectTarget`. Evict on deactivate. 302 only (ADR-001).
2. **Schema** — `short_urls.active`, `click_events` with optional analytics columns.
3. **Security** — Scheme allow-list; per-IP rate limit on create.
4. **Delete semantics** — Soft delete so analytics FK rows remain.
5. **Public API** — `/api/v1` JSON DTOs; analytics includes `breakdown`.
6. **Click recording** — Async; dropping clicks under overload is preferred to slowing redirects (ADR-003).
