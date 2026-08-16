# URL Shortener

Backend URL shortener built as an AI-assisted SDLC take-home: design, architecture, implementation, tests, and a prompt/decision log.

## What it does

- Shorten a long URL into a 7-character base62 code
- Redirect `GET /{shortCode}` with **HTTP 302** (not 301) so every click can be counted
- Track click count, timestamps, device class, and referrer
- Soft-delete a link without wiping its analytics history
- Validate URL schemes, rate-limit creates, and cache redirect lookups in **Caffeine** (not Redis or a raw map) so a single JVM stays fast without another process

No frontend. No external services. H2 file DB + in-process cache. Why Caffeine vs Redis / Guava / HashMap: [ADR-005](docs/adr/005-caffeine-vs-redis.md).

## Prerequisites

- Java 17+
- Maven 3.9+

## Run locally

```bash
mvn spring-boot:run
```

The API listens on `http://localhost:8080`. H2 console (optional): `http://localhost:8080/h2-console`  
JDBC URL: `jdbc:h2:file:./data/urlshortener`

## Tests

```bash
mvn test
```

## API examples

Create:

```bash
curl -s -X POST http://localhost:8080/api/v1/urls \
  -H 'Content-Type: application/json' \
  -d '{"url":"https://example.com/very/long/path"}'
```

Redirect (follow with `-L` if you want the client to hop):

```bash
curl -i http://localhost:8080/0000001
```

Metadata:

```bash
curl -s http://localhost:8080/api/v1/urls/0000001
```

Analytics:

```bash
curl -s http://localhost:8080/api/v1/urls/0000001/analytics
```

Deactivate (soft delete):

```bash
curl -i -X DELETE http://localhost:8080/api/v1/urls/0000001
```

## Configuration

Tunables live in `src/main/resources/application.yml` and can be overridden with env vars:

| Env var | Meaning |
|---|---|
| `APP_BASE_URL` | Public origin used when building short URLs |
| `APP_CACHE_TTL_SECONDS` | Redirect cache TTL |
| `APP_CACHE_MAX_SIZE` | Max cached redirect entries |
| `APP_RATE_LIMIT_CAPACITY` | Token-bucket size for `POST /api/v1/urls` |
| `APP_URL_MAX_LENGTH` | Max accepted long-URL length |

## Continuing on another machine / another LLM

Copy the whole repo. Agent instructions live next to the tool, not in the root:

- Cursor: `.cursor/rules/url-shortener.mdc` (loads automatically)
- Claude Code: `.claude/CLAUDE.md`
- Canonical copy: `docs/agent-instructions.md`

Root stays assignment-facing: `README.md`, `ARCHITECTURE.md`, `PROMPTS.md`.

## Project docs

| File | Purpose |
|---|---|
| [ARCHITECTURE.md](ARCHITECTURE.md) | Components, schema, APIs, request flow |
| [PROMPTS.md](PROMPTS.md) | Assignment-required AI prompt log |
| [docs/agent-instructions.md](docs/agent-instructions.md) | Instructions for any AI continuing this repo |
| [docs/requirements.md](docs/requirements.md) | Normalized requirements and ambiguities |
| [docs/decomposition.md](docs/decomposition.md) | Task breakdown used to implement |
| [docs/coding-standards.md](docs/coding-standards.md) | Layering, testing, logging, git conventions |
| [docs/future-work.md](docs/future-work.md) | Ranked backlog — do not invent extra scope |
| [docs/ai-traceability.md](docs/ai-traceability.md) | Per-task accept/reject and high-impact sign-offs |
| [docs/adr/](docs/adr/) | ADRs (302, Base62, async clicks, H2 vs SQLite, Caffeine vs Redis) |
| [docs/scenarios/](docs/scenarios/) | Greenfield / brownfield / ambiguous write-ups |
| [docs/testing-and-validation.md](docs/testing-and-validation.md) | Test approach |
| [docs/final-summary.md](docs/final-summary.md) | Risks, trade-offs, limitations |
