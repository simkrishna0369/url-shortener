# AI traceability log

One entry per implementation chunk. High-impact items are marked **⚠ HIGH-IMPACT**.

GitHub Actions **cannot** import Cursor or Claude chats. The log in this file **is** the session record a reviewer can `git show`. CI enforces that: if `src/` or `pom.xml` changes, this file must change in the same push/PR. Docs, README, and workflow-only diffs skip the check. Locally: `./scripts/check-ai-traceability.sh`.

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
| T8 | CI quality gate + AI log check | **Accepted** | `mvn test` on push/PR. Traceability job requires this file when `src/` or `pom.xml` changes. Rejected: uploading IDE transcripts (not available to GitHub). |
| T9 | Soft-delete contract tests (cache eviction, unknown DELETE, idempotent DELETE) | **Accepted** | Locked already-inactive DELETE as 204, not 404. |

## ⚠ High-impact sign-offs

1. **Redirect hot path / cache** — Cache immutable `RedirectTarget`. Evict on deactivate. 302 only (ADR-001).
2. **Schema** — `short_urls.active`, `click_events` with optional analytics columns.
3. **Security** — Scheme allow-list; per-IP rate limit on create.
4. **Delete semantics** — Soft delete so analytics FK rows remain.
5. **Public API** — `/api/v1` JSON DTOs; analytics includes `breakdown`.
6. **Click recording** — Async; dropping clicks under overload is preferred to slowing redirects (ADR-003).

## Later sessions (copy this block)

Use one block per non-trivial code change. CI only checks that this file changed; the content is for humans.

### T8 — 2026-08-16 — Quality gate

- **Intent:** Machine-checked tests plus a reminder to log AI-assisted code changes.
- **Prompt:** Add GitHub Actions as a quality gate, and AI session logs when the change is non-trivial. Keep it practical; do not invent features GitHub cannot do.
- **Files:** `.github/workflows/quality-gate.yml`, `scripts/check-ai-traceability.sh`, this file, README / testing docs.
- **Accepted:** JDK 17 `mvn -B test`; fail if `src/` or `pom.xml` changed without this log.
- **Rejected:** Checkstyle (no config in the repo yet). Scraping Cursor history. Requiring a log for docs-only edits.
- **Validation:** `mvn test` locally; CI jobs `mvn test` and `AI session log` on GitHub.

### T9 — 2026-08-16 — Soft-delete / cache contract tests

- **Intent:** Cover cache eviction after DELETE, unknown-code DELETE, and already-inactive DELETE (previously unspecified).
- **Prompt:** Implement the tests the plan promised but did not ship: cached code 404 after DELETE; deactivate unknown → 404; idempotent DELETE on already-inactive.
- **Files:** `UrlShortenerIntegrationTest`, this log, `ARCHITECTURE.md`, `docs/testing-and-validation.md`.
- **Accepted:** Repeat DELETE on an inactive code returns **204** (idempotent deactivate). Missing code still **404**. Redirect after deactivate is **404** even when Caffeine had a hit.
- **Rejected:** 404 on second DELETE (would make retries look like “not found” for a code the client just deactivated).
- **Validation:** `mvn test`.
