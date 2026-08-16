# Prompts and AI interaction log

This file is the assignment-required log of how AI was used. Detailed per-task accept/reject notes live in [docs/ai-traceability.md](docs/ai-traceability.md). Locked working rules used during implementation: [docs/cursor-implementation-prompt.md](docs/cursor-implementation-prompt.md).

---

## Session 0 — Requirements normalization (pre-code)

**Goal:** Turn “build a URL shortener with core APIs, analytics, and reliability” into a buildable spec.

**Prompt (summary):** Identify stated vs inferred requirements, list ambiguities, and lock interpretations for analytics, reliability, deletion, and short-code strategy.

**AI suggestion:** Treat analytics as count+timestamps first; treat geo/device/referrer as a separate ambiguous scenario; use base62(id); no auth in v1; H2 + Caffeine for a zero-dependency local run.

**What I kept:** Ambiguity table, base62, 302-only redirects, anonymous links, analytics-depth as the ambiguous scenario.

**What I rejected / deferred:** Custom aliases and TTL-as-the-ambiguous-demo (weaker interpretation space). Redis/HA in v1 (infra, not a fair prototype boundary).

**Why:** The assignment scores reasoning about ambiguity and “why”, not feature count.

**Outcome:** `docs/requirements.md`

---

## Session 1 — Decomposition

**Goal:** Ordered tasks with greenfield / brownfield / ambiguous labels.

**Prompt (summary):** Produce a dependency-ordered task list. Do not generate the whole system in one shot. Seed brownfield as hard-delete → soft-delete. Isolate analytics depth as phase 6.

**AI suggestion:** Add `active` on the entity in phase 1 but ignore it for delete until brownfield; cache-aside as a high-impact redirect-path change; async vs sync click writes as an explicit decision.

**What I kept:** Phased plan, high-impact flags, `active` column from day one with queries taught to respect it in brownfield.

**What I changed:** Final committed code is the *post-brownfield* system (soft delete). The hard-delete bug is documented in the brownfield write-up rather than left in HEAD, because a reviewer cloning the repo should get a working analytics-safe delete.

**Outcome:** `docs/decomposition.md`

---

## Session 2 — Master implementation prompt

**Goal:** Standing rules so later generation does not silently reinterpret architecture.

**Prompt (summary):** Encode locked stack (Java 17, Spring Boot 3, H2 file, Caffeine, Maven), layering, testing, security, and high-impact sign-off.

**AI suggestion:** 7-char codes, `/api/v1`, DTOs not entities, `@ControllerAdvice`, no secrets in source.

**What I kept:** All of the locked architecture.

**What I tightened during implementation:** Cache a `RedirectTarget` DTO, not a JPA entity (detached-entity footgun on the hot path).

**Outcome:** `docs/cursor-implementation-prompt.md`

---

## Session 3 — Implementation (this Cursor session)

**Goal:** Complete the assignment: working backend + tests + `README` / `ARCHITECTURE` / `PROMPTS` and supporting docs.

**Prompt (user):** Complete the interview assignment using the existing markdown planning files.

**Prompt (engineer rules restated to the model):** Follow locked architecture; implement core APIs, cache-aside, validation, rate limit, async clicks, soft delete, analytics breakdown; document ADRs and the three scenarios.

**AI response:** Scaffolded Spring Boot, implemented layered code, tests, and documentation in one delivery pass (the planning docs asked for task-by-task review; this session optimized for a complete, reviewable repo).

**What I accepted:**

- Base62 padded to 7 characters from auto-increment id
- HTTP 302 only
- Caffeine cache-aside of `RedirectTarget`
- Async click recording with queue-full drop
- Soft delete + cache eviction
- Device/referrer from request headers; no IP geo lookup

**What I rejected:**

- Caching JPA `ShortUrl` entities in Caffeine
- Synchronous click inserts on the redirect thread
- Hard delete in the shipped API
- Precise geo (needs a GeoIP dataset and a privacy story we did not want to fake)

**High-impact items (see traceability log):** schema, redirect path, rate limiter, delete semantics, analytics response shape.

---

## Session 4 — Document Caffeine trade-offs in the same places as 302 / H2 / Base62

**Goal:** Make the Caffeine vs Redis / Guava / HashMap rationale visible where other stack trade-offs already appear (README, architecture, final summary, agent instructions, requirements).

**Prompt (user):** Also mention why we went with Caffeine just like where we mentioned all the tradeoffs.

**What I kept:** ADR-005 as the canonical decision. No code change; Redis stays future work.

**Outcome:** README, `ARCHITECTURE.md` §6, `docs/final-summary.md`, `docs/agent-instructions.md`, `docs/requirements.md` A5, `docs/cursor-implementation-prompt.md`.

---

## Session 5 — GitHub Actions quality gate

**Goal:** A real CI gate, plus a practical AI-log check for non-trivial code changes.

**Prompt (user):** Add GitHub Actions for a quality gate and AI session logs when a change is non-trivial. Do not invent something GitHub cannot do.

**What I kept:** `mvn -B test` on Java 17; require `docs/ai-traceability.md` when `src/` or `pom.xml` changes.

**What I rejected:** Checkstyle with no existing config. Uploading Cursor transcripts (Actions never sees the IDE). Failing docs-only PRs for a missing log.

**Outcome:** `.github/workflows/quality-gate.yml`, `scripts/check-ai-traceability.sh`

---

## How to read this with the code

1. Requirements and ambiguities → `docs/requirements.md`
2. Decisions → `docs/adr/`
3. Scenario narratives → `docs/scenarios/`
4. What shipped vs what was left out → `docs/final-summary.md`
