# AI traceability log

One row per decomposition task. **Rejected** is required — a wall of Accepted is not a review record.

GitHub Actions **cannot** import Cursor or Claude chats. This file **is** the session record. CI fails if `src/` or `pom.xml` changes without this file changing in the same push/PR. Locally: `./scripts/check-ai-traceability.sh`. Docs-only diffs skip the check.

How to read git vs this table: [README — How to review](../README.md#how-to-review).

Legend: **⚠** high-impact (human sign-off in the section below).

| ID | Task | Outcome | Rejected (and why) |
|---|---|---|---|
| T0.1 | Spring Boot + Maven + Java 17 | **Accepted** | Gradle (team/Maven lock). Kotlin. Java 21-only (17 is the assignment floor). |
| T0.2 | H2 **file** datasource | **Accepted** | H2 in-memory for local runs (loses data on restart). SQLite (worse JPA fit, ADR-004). Postgres in v1 (extra process). |
| T0.3 | Caffeine cache bean | **Accepted** | Redis/Hazelcast in v1 (needs another process; one JVM). Guava Cache (Caffeine is the successor). Raw `ConcurrentHashMap` (no TTL/max size). |
| T0.4 | `controller` / `service` / `repository` / `dto` packages | **Accepted** | Single-package dump. Business logic in controllers. |
| T0.5 | `@ControllerAdvice` skeleton | **Accepted** | Stack traces in JSON. Ad-hoc `Map<String,Object>` error bodies. |
| T1.1 | `ShortUrl` entity including unused `active` | **Accepted** | Adding `active` only in brownfield (bigger schema diff; we wanted queries taught later). Hash of the URL as PK. |
| T1.2 | `ShortUrlRepository` | **Accepted** | JPQL in the service layer. |
| T1.3 | Base62(id), 7 chars + unit tests | **Accepted** | MD5/SHA truncated to 7 chars (collisions + retry loop, ADR-002). Base64 (`+/` in paths). 6-char codes (tight at 1k creates/sec). |
| T1.4 | URL scheme allow-list + length | **Accepted** | Allow `javascript:` / `data:` / `file:`. “Just check it starts with http” without a host. |
| T1.5 | `POST /api/v1/urls` DTOs | **Accepted** | Unversioned `/api/urls`. Returning the JPA entity. Idempotent shorten (same URL → same code) — assumed not wanted. |
| T1.6 | Create happy path + invalid URL tests | **Accepted** | Skipping the reject path. |
| T2.1 | `GET /{shortCode}` **302 only** ⚠ | **Accepted** | **301** (browser-cached hop skips later clicks, ADR-001). |
| T2.2 | Cache-aside of `RedirectTarget` ⚠ | **Accepted** | **Caching `ShortUrl` JPA entities** (lazy-load / detached-entity on the hot path). |
| T2.3 | 404 for missing/inactive | **Accepted** | 200 + error body. 500 on unknown codes. |
| T2.4 | Redirect / 404 tests | **Accepted** | Dedicated cache-eviction test was deferred; added later as T9 rather than pretending Phase 2 had it. |
| T3.1 | `ClickEvent` entity | **Accepted** | Increment-only counter with no event rows (blocks timestamps and breakdown). |
| T3.2 | Async click write ⚠ | **Accepted** | **Synchronous insert on the redirect thread** (latency). Unbounded async queue (can OOM). |
| T3.3 | Analytics count + timestamps | **Accepted** | Count-only API. Returning entities. |
| T3.4 | Multi-click integration test | **Accepted** | `Thread.sleep` instead of Awaitility. |
| T4.1 | Per-IP token bucket on create ⚠ | **Accepted** | No rate limit. Bucket4j+Redis in v1. Rate-limiting **redirects** (would hurt the product). |
| T4.2 | SLF4J + MDC `shortCode` | **Accepted** | `System.out.println`. Logging full rejected URLs (untrusted input). |
| T4.3 | Tunables in `application.yml` | **Accepted** | Hardcoded TTL / rate-limit / max length in Java. |
| T5.1 | Blast-radius write-up before code | **Accepted** | Editing queries first and reverse-engineering impact from the diff. |
| T5.2 | Redirect/metadata query split (`active=true` only on redirect) ⚠ | **Accepted** | Filtering analytics to active rows only (would hide history after deactivate). |
| T5.3 | DELETE → `active=false` ⚠ | **Accepted** | **Hard delete** (wipes or FK-breaks click history). Cascade-remove `click_events`. |
| T5.4 | Analytics still works after deactivate | **Accepted** | 404 on analytics for inactive codes. |
| T5.5 | Regression: other codes still redirect | **Accepted** | — (no alternate suggested). |
| T5.6 | Cache evict on deactivate ⚠ | **Accepted** | Leaving the Caffeine entry (stale 302 after DELETE). TTL-only invalidation. |
| T6.1 | Interpret “analytics” before coding | **Accepted** | Using **custom aliases** or **TTL/expiry** as the ambiguous demo (weaker interpretation space). Shipping **GeoIP** as if a 302 had location. |
| T6.2 | Persist UA, referrer, deviceType ⚠ | **Accepted** | **Storing client IP** in v1 (retention/privacy without a policy). |
| T6.3 | Device class from UA; missing UA → `unknown` | **Accepted** | Failing the redirect when UA is absent. Calling substring checks a bot detector. |
| T6.4 | `breakdown.devices` / `referrers` on the API ⚠ | **Accepted** | Precise geo fields with fake data. Client JS beacon (no frontend). |
| T6.5 | Breakdown + missing-UA tests | **Accepted** | — |
| T6.6 | Document geo/bot as limitations | **Accepted** | Silently skipping geo so it looks “done.” |
| T7.1–7.5 | ARCHITECTURE, scenarios, testing doc, final summary, README | **Accepted** | Inventing a frontend or OpenAPI-only deliverable instead of a runnable API. |
| T8 | GitHub Actions `mvn test` + log check | **Accepted** | Uploading Cursor transcripts (Actions cannot see the IDE). Checkstyle with no config. Failing docs-only PRs for a missing log. |
| T9 | Cache-eviction + DELETE contract tests | **Accepted** | **404 on second DELETE** (retries would look like “code never existed”). |
| T10 | Reviewer waypoints (README + tags + this split log) | **Accepted** | **Rewriting `git` history / force-push** to fake greenfield → brownfield → ambiguous SHAs. The code landed in one pass; tags must not lie. |
| T11 | Analytics SQL aggregates + cap `clicks` ⚠ | **Accepted** | Loading all `ClickEvent` rows in the service. Dropping `clicks[]`. Pagination query params in this pass. Redis aggregators. |

## Process note (not a product decision)

Decomposition said “one task per AI pass.” Implementation Session 3 generated the working system in **one delivery pass**, then later commits added CI, future-work docs, and T9 tests.

**Rejected:** leaving hard-delete in `main` so a clone would demonstrate the brownfield bug. Reviewers should get a safe DELETE; the bug and blast radius live in `docs/scenarios/brownfield.md`.

**Rejected:** interactive rebase of `f38b8a2` into three feature commits after it was already on `origin/main` (force-push, fake chronology).

## ⚠ High-impact sign-offs

1. **Redirect hot path / cache** — Cache immutable `RedirectTarget`. Evict on deactivate. 302 only (ADR-001).
2. **Schema** — `short_urls.active`, `click_events` with optional analytics columns. No client IP column.
3. **Security** — Scheme allow-list; per-IP rate limit on create (not on redirect).
4. **Delete semantics** — Soft delete; unknown code 404; already-inactive 204.
5. **Public API** — `/api/v1` JSON DTOs; analytics includes `breakdown`. `clicks` may be a recent window (`clicksTruncated`).
6. **Click recording** — Async; drop clicks under overload rather than slow redirects (ADR-003).

## Later sessions (copy this block)

Use one block per non-trivial **code** change. CI only checks that this file changed; the content is for humans.

### T8 — 2026-08-16 — Quality gate

- **Intent:** Machine-checked tests plus a reminder to log AI-assisted code changes.
- **Prompt:** Add GitHub Actions as a quality gate, and AI session logs when a change is non-trivial. Do not invent features GitHub cannot do.
- **Files:** `.github/workflows/quality-gate.yml`, `scripts/check-ai-traceability.sh`, this file, README / testing docs.
- **Accepted:** JDK 17 `mvn -B test`; fail if `src/` or `pom.xml` changed without this log.
- **Rejected:** Checkstyle (no config in the repo yet). Scraping Cursor history. Requiring a log for docs-only edits.
- **Validation:** `mvn test` locally; CI jobs `mvn test` and `AI session log` on GitHub.

### T9 — 2026-08-16 — Soft-delete / cache contract tests

- **Intent:** Cover cache eviction after DELETE, unknown-code DELETE, and already-inactive DELETE.
- **Prompt:** Implement the tests the plan promised but did not ship.
- **Files:** `UrlShortenerIntegrationTest`, this log, `ARCHITECTURE.md`, `docs/testing-and-validation.md`.
- **Accepted:** Repeat DELETE → **204**. Missing code → **404**. Redirect after deactivate → **404** even after a cache hit.
- **Rejected:** 404 on second DELETE.
- **Validation:** `mvn test`.

### T10 — 2026-08-16 — Process evidence for reviewers

- **Intent:** Make greenfield / brownfield / ambiguous reviewable without rewriting git history.
- **Prompt:** Reconstruct or narrate with commits/tags; split this log into per-task rows with rejected suggestions.
- **Files:** `README.md` (How to review), this file, annotated git tags on **existing** commits.
- **Accepted:** Honest mapping: logical phases in `docs/scenarios/` + `docs/decomposition.md`; git tags on commits that actually exist.
- **Rejected:** Force-push / rebase to invent `fix/soft-delete` and `feat/analytics-breakdown` SHAs. Three tags on the **same** commit pretending they are sequential landings.
- **Validation:** `git tag -n` and the README table; no `src/` change.

### T11 — 2026-08-16 — Analytics read-path brownfield

- **Intent:** Keep `clickCount` and `breakdown` correct for the full history without loading every click into the JVM or returning an unbounded JSON array.
- **Prompt:** Implement analytics SQL aggregates plus a configurable cap on `clicks` as a data-path brownfield change. Do not add Redis, pagination APIs, or drop the time-series list.
- **Files:** `ClickEventRepository`, `ShortUrlService`, `AnalyticsResponse`, `AppProperties`, `ClickEvent` index, `AnalyticsCapIntegrationTest`, `docs/scenarios/brownfield-analytics.md`, this log.
- **Accepted:** `COUNT` + `GROUP BY` in the repository; most recent N clicks (default 100); `clicksTruncated` when history is longer; composite index `(short_url_id, clicked_at)`.
- **Rejected:** In-service `groupingBy` over a full `findAll`. Dropping `clicks[]`. `?page=` pagination in this pass. Redis/stream aggregators.
- **Validation:** `mvn test` including `AnalyticsCapIntegrationTest` (limit=2, three redirects).
