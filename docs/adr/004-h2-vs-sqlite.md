# ADR-004: H2 file database instead of SQLite (or Postgres)

## Status

Accepted

## Context

v1 must persist short URLs and click events, run with **one local command**, and stay inside Spring Data JPA. Candidates:

| Option | What it is |
|---|---|
| H2 file mode | JVM-embedded SQL DB; data in `./data/urlshortener` |
| SQLite | Embedded file DB, ubiquitous in other ecosystems |
| Postgres | Real production RDBMS; separate process |

## Decision

Use **H2 in file mode** for local/demo persistence. Tests use H2 **in-memory**. Postgres is the documented upgrade (`docs/future-work.md`), not v1.

## Why H2 over SQLite

Spring Boot’s default embedded database is H2. The JDBC driver, Hibernate dialect, `ddl-auto`, and `/h2-console` all work with no extra adapter layer. Reviewers can inspect tables in a browser.

SQLite is an excellent embedded engine, but in a **Java / JPA** prototype it is the less native fit:

- Hibernate/SQLite historically needs extra dialect care (types, `ALTER TABLE`, boolean, migration).
- SQLite allows **one writer** at a time. That is fine for a laptop demo and a problem the moment create + async click inserts overlap under load — we would hit that limit before we hit H2’s.
- We already chose JPA. H2 behaves closer to the Postgres we would move to later (sequences, broader SQL). SQLite would teach slightly the wrong production shape.

We did **not** pick H2 because it is “better than SQLite in general.” We picked it because this is a Spring Boot take-home: lowest integration friction, inspectable console, easy test/prod-local split (mem vs file).

## Why not Postgres in v1

Postgres is the right production store (backups, concurrent writers, ops story). It requires Docker or a local install, which breaks the “no external services” constraint in `docs/requirements.md`.

## Consequences

- Data survives `mvn spring-boot:run` restarts (file mode), unlike H2 `mem`.
- File DB is not a backup/HA story. Do not pretend it is.
- Moving to Postgres later should be mostly datasource + dialect; avoid H2-only SQL.
