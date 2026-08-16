# Agent instructions

This repository is meant to be continued on another machine or with another LLM.
**Read this file first.** Do not re-interpret the product from a blank slate.

Tool-specific loaders (not in the repo root):

- Cursor: `.cursor/rules/url-shortener.mdc`
- Claude Code: `.claude/CLAUDE.md`

## Locked stack (do not change without an ADR)

- Java 17+, Spring Boot 3.x, Maven
- H2 **file** mode (not in-memory for local runs)
- Caffeine cache-aside on redirect lookup — see `docs/adr/005-caffeine-vs-redis.md` (not Redis/Guava/HashMap in v1)
- REST under `/api/v1`
- Layering: Controller → Service → Repository
- Redirect is **HTTP 302 only** (never 301) — see `docs/adr/001-redirect-status.md`
- Short codes: 7-char **Base62 of auto-increment id**, not a URL hash — see `docs/adr/002-short-code-generation.md`

## How to work

1. Treat `docs/requirements.md` as the product spec.
2. Use `docs/decomposition.md` for remaining / historical task order. Do not dump a whole new feature in one shot.
3. Follow `docs/coding-standards.md`.
4. If you change architecture, write or update an ADR under `docs/adr/`.
5. Log the prompt + decision in `PROMPTS.md` or `docs/ai-traceability.md`.
6. Run `mvn test` before considering work done.

## Doc map

| Need | File |
|---|---|
| Run locally | `README.md` |
| Design / APIs / schema | `ARCHITECTURE.md` |
| Task breakdown | `docs/decomposition.md` |
| Coding standards | `docs/coding-standards.md` |
| Future work (do not invent extra scope) | `docs/future-work.md` |
| Standing implementation rules (verbose) | `docs/cursor-implementation-prompt.md` |
| Why 302 / Base62 / async clicks / H2 / Caffeine | `docs/adr/` |
| Greenfield / brownfield / ambiguous | `docs/scenarios/` |

## Out of scope unless the human explicitly asks

Auth, Redis, Postgres, 301 redirects, hashing the long URL, frontend, GeoIP.
Those belong in `docs/future-work.md`, not in a drive-by refactor.
