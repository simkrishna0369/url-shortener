# Task Decomposition — URL Shortener

Ordered task list with dependencies. Each task maps to one Cursor session/prompt using
`docs/cursor-implementation-prompt.md` as the standing rules, and produces one
traceability log entry in `docs/ai-traceability.md`. Do not batch multiple tasks into
one AI generation pass — the point is visible, reviewable increments.

Legend: **[G]** Greenfield · **[B]** Brownfield · **[A]** Ambiguous-requirement scenario
· **⚠** High-impact (requires explicit sign-off per §4.1 of the Cursor prompt)

---

## Phase 0 — Project Scaffold
| # | Task | Depends on | Notes |
|---|---|---|---|
| 0.1 | Spring Boot project init (Maven, Java 17, starters: web, data-jpa, validation, cache) | — | No business logic yet |
| 0.2 | H2 file-mode datasource config (`application.yml`) | 0.1 | |
| 0.3 | Caffeine cache config bean | 0.1 | Config only, not wired to any endpoint yet |
| 0.4 | Base package structure (`controller` / `service` / `repository` / `dto` / `exception`) | 0.1 | Enforces layering rule from Cursor prompt §1 |
| 0.5 | Global exception handler skeleton (`@ControllerAdvice`) | 0.4 | Empty handlers to start; filled in as exceptions are introduced |

## Phase 1 — Greenfield: Core Data Model & Create Flow **[G]**
| # | Task | Depends on | Notes |
|---|---|---|---|
| 1.1 | `ShortUrl` entity: id, longUrl, shortCode, createdAt, active (bool) | 0.4 | `active` included from the start — see note in §5 below on why |
| 1.2 | Repository interface (`ShortUrlRepository`) | 1.1 | |
| 1.3 | Base62 encoder utility (7-char, per ADR-002) + unit tests | 1.1 | Pure function, easy to test in isolation first |
| 1.4 | URL validation logic (scheme allow-list, length limit) + unit tests | 0.4 | Independent of persistence — test first |
| 1.5 | `POST /api/v1/urls` — create endpoint (DTO in/out, calls encoder + validation + repository) | 1.2, 1.3, 1.4 | |
| 1.6 | Integration test: create endpoint happy path + invalid-URL rejection path | 1.5 | |

## Phase 2 — Greenfield: Redirect & Caching **[G]**
| # | Task | Depends on | Notes |
|---|---|---|---|
| 2.1 | `GET /{shortCode}` — redirect endpoint, 302 only (ADR-001) | 1.2 | |
| 2.2 | Caffeine cache-aside wiring on short-code lookup | 2.1, 0.3 | ⚠ touches the redirect hot path — sign-off required per Cursor prompt §4.1 |
| 2.3 | 404 handling for missing/inactive short codes | 2.1 | Ties into exception handler from 0.5 |
| 2.4 | Integration tests: redirect happy path, missing code (404), cache hit vs. miss behavior | 2.2, 2.3 | |

## Phase 3 — Greenfield: Analytics Core (FR4-FR6) **[G]**
| # | Task | Depends on | Notes |
|---|---|---|---|
| 3.1 | `ClickEvent` entity (shortUrlId, timestamp) | 1.1 | |
| 3.2 | Record a click event on every successful redirect | 2.1, 3.1 | Must not slow down the redirect hot path — async or lightweight write, decide and document in traceability log |
| 3.3 | `GET /api/v1/urls/{shortCode}/analytics` — count + timestamp list | 3.1 | |
| 3.4 | Integration test: click count increments correctly across multiple redirects | 3.2, 3.3 | |

## Phase 4 — Greenfield: Reliability Features **[G]**
| # | Task | Depends on | Notes |
|---|---|---|---|
| 4.1 | Rate limiting on `POST /api/v1/urls` (per-IP, token bucket) | 1.5 | ⚠ security-sensitive logic — sign-off required |
| 4.2 | Structured logging pass (creation, redirect, rate-limit trips) | 2.1, 1.5, 4.1 | Per Cursor prompt §3.4 |
| 4.3 | Config externalization check — confirm no hardcoded tunables remain | all above | Cursor prompt §3.5 audit step |

**→ End of Greenfield. This is the "working prototype" baseline. Tag/commit as a
milestone before starting brownfield work.**

---

## Phase 5 — Brownfield: Hard Delete → Soft Delete **[B]** ⚠
> Prerequisite: hard-delete (`DELETE /api/v1/urls/{shortCode}` removing the row
> outright) must already exist from greenfield scope. This phase is the deliberate
> "discover the bug, fix existing code" scenario — see `docs/scenarios/brownfield.md`
> for the full write-up once complete.

| # | Task | Depends on | Notes |
|---|---|---|---|
| 5.1 | **Codebase reasoning task (no code yet):** identify every module touching `ShortUrl` rows — repository queries, analytics query path, redirect lookup, cache — and document blast radius of a hard-delete-to-soft-delete change | Phase 1-4 complete | This is the "impacted modules" deliverable required by the assignment's brownfield requirement — write it up before touching code |
| 5.2 | Add `active` flag usage: update repository queries to filter `active = true` where appropriate (redirect lookup, metadata lookup) | 5.1 | ⚠ changes already-working query behavior |
| 5.3 | Change `DELETE` endpoint from hard delete to setting `active = false` | 5.1, 5.2 | ⚠ changes an existing endpoint's persistence behavior |
| 5.4 | Confirm analytics endpoint still returns click history for a deactivated link (this is the bug being fixed) | 5.3 | This is the actual validation of the brownfield fix |
| 5.5 | Regression test pass: confirm create/redirect/analytics for **non-deleted** links still behave identically to before the change | 5.2, 5.3 | Brownfield validation must prove nothing else broke |
| 5.6 | Cache invalidation check: ensure a deactivated short code is evicted from Caffeine cache, not served stale from cache after deactivation | 5.3, 2.2 | Easy to miss — cache was built before soft-delete existed |

---

## Phase 6 — Ambiguous Requirement: Analytics Depth **[A]**
> See `docs/scenarios/ambiguous-requirement.md` for the full interpretation write-up.
> Core count/timestamp analytics (Phase 3) already exists; this phase extends it.

| # | Task | Depends on | Notes |
|---|---|---|---|
| 6.1 | **Interpretation task (no code yet):** document 2-3 reasonable readings of "analytics" depth, what's technically available from a server-side redirect request alone (user-agent header → device/browser inference; `Referer` header → referrer; IP → coarse geo via lookup) vs. what would require a client-side JS beacon (not applicable here — this is a pure redirect service), and pick a scoped v1 cut | Phase 3 complete | |
| 6.2 | Extend `ClickEvent` entity with chosen fields (e.g., userAgent, referrer, coarse geo) | 6.1 | ⚠ schema change |
| 6.3 | Parse/derive device + referrer from request at click-record time | 6.2, 3.2 | |
| 6.4 | Extend analytics endpoint response to include the new breakdown | 6.2 | |
| 6.5 | Tests: verify breakdown populates correctly; verify missing/malformed headers degrade gracefully (don't break the redirect if `User-Agent` is absent) | 6.3, 6.4 | |
| 6.6 | Explicitly document what was deliberately left out (e.g., precise geo via IP lookup, bot filtering) as a stated limitation, not silently ignored | 6.1 | Ties into final summary |

---

## Phase 7 — Documentation & Final Pass
| # | Task | Depends on |
|---|---|---|
| 7.1 | `ARCHITECTURE.md` — components, control flow, key decisions (references ADRs) | All phases |
| 7.2 | `docs/scenarios/greenfield.md`, `brownfield.md`, `ambiguous-requirement.md` write-ups | Phases 1-6 |
| 7.3 | `docs/testing-and-validation.md` — approach, coverage summary, what's not tested and why | All phases |
| 7.4 | `docs/final-summary.md` — plan/rationale, risks/trade-offs, assumptions, limitations (pull from ADR-002's production-scale gap table) | All phases |
| 7.5 | `README.md` — verified, copy-paste-runnable setup instructions | All phases |

---

## Notes on Sequencing Decisions

- **Why `active` flag is added in Phase 1.1, not invented fresh in Phase 5:** it needs
  to exist on the entity from the start so hard-delete in greenfield can plausibly
  ignore it (simplicity bias, matches how a real engineer might ship v1 without
  thinking through delete semantics), while brownfield's job is to make the *rest of
  the system* (queries, cache) actually respect it. If you'd rather the brownfield
  scenario also include adding the column itself (bigger diff, more obviously
  brownfield), tell me and I'll move it to Phase 5.
- **Why analytics core (Phase 3) is fully greenfield and depth (Phase 6) is separate:**
  keeps the ambiguous scenario isolated and diffable — you can show a clean "before"
  (Phase 3 state) and "after" (Phase 6 state) rather than tangling interpretation work
  into the initial build.
- **Click recording performance (task 3.2):** flagged as a real decision point, not
  glossed over — writing a click event synchronously on every redirect adds latency to
  the hot path; async (e.g., `@Async` or a lightweight queue) avoids that but adds
  complexity and eventual-consistency risk on the count. Worth an ADR once you reach
  that task if you want the reasoning documented.

---

## Phase 11 — Brownfield: analytics read path (post-v1)

| # | Task | Depends on | Notes |
|---|---|---|---|
| 11.1 | Stop loading all click rows in `getAnalytics`; `COUNT` + `GROUP BY` in the repository; cap `clicks` via `app.analytics.clicks-limit` | Phase 6 | See `docs/scenarios/brownfield-analytics.md` |
