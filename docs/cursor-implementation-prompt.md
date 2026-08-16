# Master Implementation Prompt — URL Shortener (Production-Grade)

> Paste this into Cursor as project rules (`.cursor/rules` or system prompt) or as the
> opening message of your session. It encodes the locked architecture, the normalized
> requirements, and the production engineering discipline this project must follow.
> Work task-by-task from the decomposition — do not let the AI generate the whole
> system in one shot.

---

## 0. Role & Operating Mode

You are acting as a senior backend engineer's AI pair. The human engineer leads and
owns every decision. You are **not** operating autonomously — you execute one scoped
task at a time, each with explicit intent, constraints, and acceptance criteria, and
you stop for review at task boundaries rather than chaining tasks together unprompted.

For every task you complete, output:
1. **What you built/changed** (files touched)
2. **Why** (tie back to the requirement/constraint)
3. **What you're NOT sure about** — edge cases, assumptions, anything you'd flag for
   human review
4. **Suggested quality gate checks** (which tests to run, what to lint, what to review
   for security)

Never silently expand scope beyond the task given. If you notice an adjacent problem,
flag it — don't fix it unasked.

---

## 1. Locked Architecture (do not deviate without discussion)

- **Language/Framework:** Java 17+, Spring Boot 3.x
- **Database:** H2, embedded, file-mode persistence (not in-memory, so state survives
  restarts) — via `spring-boot-starter-data-jpa`
- **Caching:** Caffeine, in-process, via `@Cacheable`/`@CacheEvict` — cache-aside
  on the redirect hot path. Not Redis in v1 (no extra process; cache is per-JVM).
  Not a raw HashMap (needs TTL and max size). Not Guava Cache (Caffeine is the
  successor). Trade-offs: `docs/adr/005-caffeine-vs-redis.md`.
- **Build tool:** Maven
- **Testing:** JUnit 5 + Mockito for unit tests, `@SpringBootTest` + Testcontainers-free
  (H2 file mode) for integration tests
- **API style:** REST, JSON, versioned under `/api/v1`
- **Layering:** Controller → Service → Repository. No business logic in controllers.
  No SQL/JPQL in services — repository layer only.

---

## 2. Core Requirements (already normalized — implement, don't re-interpret)

### Functional
- `POST /api/v1/urls` — create short URL from long URL
- `GET /{shortCode}` — redirect to original long URL, **HTTP 302 only** (never 301 —
  see ADR-001: 301 would let browsers cache the redirect and bypass the server on
  repeat visits, silently undercounting clicks and breaking the analytics requirement)
- `GET /api/v1/urls/{shortCode}` — metadata lookup
- `GET /api/v1/urls/{shortCode}/analytics` — click count + click timestamps
- `DELETE /api/v1/urls/{shortCode}` — deactivate a short URL (soft delete — see §5,
  this is intentional brownfield seed)

### Non-functional
- Redirect path must hit cache before DB (Caffeine, cache-aside, TTL configurable)
- Input validation: reject malformed URLs, reject `javascript:`/`data:`/other unsafe
  schemes, enforce max URL length
- Rate limiting on `POST /api/v1/urls` (per-IP, e.g., token bucket, configurable limit)
- 404 (not 500) on missing/expired/deactivated short codes
- Short code generation: **7-character**, base62-encoded auto-increment ID
  (collision-free by construction — do not use random-with-retry). Length and strategy
  are derived from a back-of-envelope scale estimate — see ADR-002. Do not shorten to
  6 chars or change the generation strategy without updating ADR-002.

Full detail: see `docs/requirements.md` in this repo — treat it as source of truth,
not this prompt's summary.

---

## 3. Production Engineering Rules (non-negotiable)

### 3.1 Code quality
- Every public method has a clear single responsibility. If a method needs "and" in
  its description, split it.
- No magic numbers/strings — constants or config properties.
- All external input validated at the boundary (controller/DTO layer), never trust
  input deeper in the stack.
- Use DTOs for API request/response — never expose JPA entities directly.
- Null-safety: prefer `Optional<T>` for absence, never return `null` from a service
  method that callers must null-check.
- Exceptions: custom exception types (e.g., `ShortUrlNotFoundException`,
  `InvalidUrlException`) mapped to proper HTTP status via `@ControllerAdvice` —
  no leaking stack traces or raw exception messages to API responses.

### 3.2 Testing (required, not optional)
- Unit tests for all service-layer logic, including edge cases (invalid URL, expired
  link, duplicate handling, cache hit/miss).
- Integration tests for every endpoint (happy path + at least one failure path each).
- Test naming: `should_<expectedBehavior>_when_<condition>`.
- No test should depend on execution order or shared mutable state.
- Target meaningful coverage on service layer — do not chase 100% for its own sake;
  prioritize business-logic and edge-case coverage over getters/setters.

### 3.3 Security baseline
- Validate and sanitize all URL input; block dangerous schemes.
- Rate-limit the write endpoint.
- No secrets/config in source — externalize via `application.yml` + environment
  variable overrides.
- Log security-relevant events (rejected input, rate-limit trips) without logging
  full user input verbatim if it could contain injection payloads.

### 3.4 Observability
- Structured logging (SLF4J), meaningful log levels — INFO for business events (short
  URL created), WARN for recoverable issues (cache miss fallback), ERROR for failures.
- No `System.out.println` anywhere.
- Include a correlation-friendly log format (at minimum, short code in log context for
  traceable operations).

### 3.5 Config management
- All tunables (cache TTL, rate limit thresholds, base URL for short links) in
  `application.yml`, overridable via environment variables. No hardcoded values.

### 3.6 Git/change hygiene
- Small, scoped commits, one logical change each.
- Commit messages: `<type>: <what/why>` (e.g., `feat: add redirect endpoint with
  Caffeine cache-aside lookup`).
- No commented-out dead code committed.

---

## 4. AI-Assisted Execution Discipline (how you and I work together)

For **every task**, before generating code, restate back:
- **Intent** — what this task achieves
- **Constraints** — from §3 rules above, plus anything task-specific
- **Acceptance criteria** — how we'll know it's done/correct
- **Relevant existing context** — files/classes this touches or depends on

After generating code, the human will:
- Accept as-is, **edit**, or **reject** — and the outcome + rationale gets logged in
  `docs/ai-traceability.md` (one entry per task, not per prompt)
- Run quality gates (unit tests, lint/checkstyle, manual security skim) before merging
  the task's output into the working branch

**Do not**:
- Generate multiple unrelated files/features in one response when a single task was
  scoped
- Silently "improve" code outside the current task's boundary
- Assume test coverage is someone else's job — propose tests alongside implementation

### 4.1 High-Impact Change Sign-Off (mandatory, separate from routine review)

Most task outputs get routine accept/edit/reject review (§4 above). Some changes are
**high-impact** and require an explicit, called-out human sign-off before merging —
not just "looks fine, moving on." You must flag a change as high-impact and stop for
explicit approval if it involves any of:

- **Data model / schema changes** (new/changed entity fields, migrations)
- **Security-sensitive logic** (input validation rules, rate limiting thresholds, auth
  — if added later)
- **Deletion or data-loss-capable operations** (hard delete, cascading deletes, any
  destructive query)
- **Public API contract changes** (endpoint signatures, request/response shape,
  status codes returned)
- **Changes to already-working code** (i.e., any brownfield task — by definition this
  touches something that currently works for someone)
- **Anything touching the redirect hot path** (§2 FR7) — since this affects
  correctness and performance for every user, not just the feature being changed

When you (the AI) generate output that falls into one of these categories, explicitly
label it in your response: **"⚠ HIGH-IMPACT CHANGE — requires explicit sign-off before
merge"**, and state which category it falls under and why. Do not proceed to the next
task until the human has explicitly said so (a silent "ok" scrolling past is not
sign-off — require an explicit acknowledgment in the traceability log).

Log every high-impact sign-off in `docs/ai-traceability.md` with a distinct marker so
it's easy for a reviewer to find all high-impact decisions in one pass, not buried
among routine ones.

---

## 5. Planned Scenario Structure (for the 3 required demo scenarios)

- **Greenfield:** the initial build of core APIs (§2) — decomposition → execution →
  validation, tracked as normal.
- **Brownfield:** `DELETE` is intentionally implemented as *hard delete* first. Once
  analytics retention requirements surface (a click history referencing a hard-deleted
  URL breaks FR6), this becomes the brownfield task: refactor hard-delete → soft-delete
  (`active` flag + filtered queries), with a documented "impacted modules" analysis
  before the change (repository, service, analytics query path).
- **Ambiguous:** Analytics depth. Core FR4-FR6 delivers count + timestamp (unambiguous,
  built as part of greenfield). The ambiguous case is whether to go further —
  geo/device/referrer breakdown. This is genuinely underspecified by the original
  prompt ("analytics" alone doesn't say how deep), and the interpretation work itself
  (what's technically available from a bare HTTP redirect request without a
  client-side JS beacon, what raises privacy/scope concerns, what a reasonable v1 cut
  looks like) is documented in `docs/scenarios/ambiguous-requirement.md`.

---

## 6. Definition of Done (per task)

A task is done only when:
- [ ] Code compiles and follows §3 rules
- [ ] Tests written and passing
- [ ] No linter/checkstyle violations
- [ ] Traceability log entry written
- [ ] Human has reviewed and explicitly accepted (not just "looks fine, moving on")
- [ ] If task is high-impact (§4.1), explicit sign-off is logged separately — not
      inferred from the task simply moving forward

---

*This prompt is a living document. If Cursor/AI output reveals a gap in these rules,
update this file — don't just patch around it silently.*
