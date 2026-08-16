# ADR-002: 7-character base62 codes from auto-increment IDs

## Status

Accepted

## Context

Short codes must be unique. Options:

1. Sequential ID encoded as Base62 (**chosen**)
2. Random string + retry on collision
3. Hash of the long URL, truncated, **plus collision resolution** (re-hash / salt / counter until unique)

## Decision

Use (1): `short_code = pad(base62(id), 7)`.

Alphabet: `0-9a-zA-Z` (62 symbols). Uniqueness comes from the database `id`, not from a hash function. Base62 here is **number-base conversion**, not a hashing algorithm.

### How many URLs? (back-of-the-envelope)

A 7-character Base62 code has:

`62^7 = 3,521,614,606,208` ≈ **3.52 trillion** distinct values.

The encoder pads to 7 characters. Once `id` exceeds `62^7 - 1`, the string grows to 8+ characters. The redirect route today is `{shortCode:[0-9A-Za-z]{7}}`, so **the practical cap is those 3.52 trillion 7-character codes** unless we widen the path.

Time-to-exhaustion if every `POST` burns one new id:

| Create rate | Time to fill 7-char Base62 |
|---|---|
| 100 / sec (busy service) | ~1,100 years |
| 1,000 / sec (very high) | ~110 years |
| 10,000 / sec | ~11 years |

Six characters (`62^6` ≈ 57 billion) lasts only ~1.8 years at 1,000/sec — tight if this ever became a real platform. Seven was the planning lock and leaves room to steal a character later for a shard prefix.

This is **capacity of the code space**, not H2 throughput. The database and a single JVM will fall over long before we run out of Base62 strings.

### Why Base62, not Base64

Base64 was considered because it is the usual “binary → short ASCII” encoding and `64^7` ≈ 4.4 trillion is only ~25% more codes.

Standard Base64 uses `+`, `/`, and `=` padding. In a path like `GET /{shortCode}` those are harmful: `/` splits the URL, `+` is often a space, `=` is noise. Clients would have to percent-encode; logs and copy-paste get ugly.

URL-safe Base64 (`A-Za-z0-9-_`) avoids that, but `-` and `_` still break double-click selection and some parsers. The extra two symbols are not worth it: **25% more space does not change the ~110-year story at 1k/sec**, and Base62 is already URL-safe with no encoding.

So: Base62 = URL alphabet + enough space. Base64 = more symbols, worse URLs, same order of magnitude.

### Why Base62(id), not hash + collision resolution

Hashing the long URL (MD5/SHA-256/Murmur) and taking 7 Base62 characters **looks** like a shortener, but a 7-character slice is a tiny key space relative to the hash. Distinct URLs can land on the same prefix (birthday bound). You then need a **collision policy**:

- append a counter (`abc1234`, `abc1235`, …)
- salt and re-hash
- probe the next n bits of the digest

That works. It is also extra unique-index checks, retries under load, and a harder story to test. Two different URLs must never share a code; the same URL *might* be intended to share a code (idempotent shorten). Our spec (assumption A5) is the opposite: **each POST gets a new code**, even for a duplicate long URL. A content hash fights that requirement unless we add a salt on every create — at which point it is just “random + retry” with more steps.

| | Base62(`id`) | Hash + resolve collisions |
|---|---|---|
| Uniqueness | Free: `id` is unique | Must detect + retry |
| Create path | Encode after insert | Hash, insert, maybe loop |
| Same long URL twice | New code (what we want) | Same code unless salted |
| Guessable? | Yes (sequential) | No (until you leak the URL) |
| Hot-path complexity | None | Unique index + retry/backoff |

We went with Base62(`id`) because **correctness is trivial** and the take-home should not spend its complexity budget on a collision loop. Hash + collision resolution is the right family if we need **unguessable** codes or **idempotent** shorten — both are listed as future work, not v1.

Random-string + retry (option 2) is the usual production choice for unguessable links. Same retry machinery as hash collisions, without pretending the URL content defined the code.

## Consequences

- Collision-free without a retry loop.
- Duplicate `POST`s of the same long URL create **new** codes (assumption A5 in requirements). Hash-based designs can be idempotent but collide when truncated and leak whether a URL was seen before.
- Sequential codes are enumerable (`0000001`, `0000002`, …). Acceptable because v1 has no auth and codes are not a security boundary. A production system that needs unguessable links should switch to a high-entropy random code and document the collision/retry policy.
- Two writes on create (insert row, then set code once `id` exists). Simple and clear; a DB trigger or `@PostPersist` would hide the same step.

## Production-scale gap (not in this prototype)

| Gap | Why it matters | Typical upgrade |
|---|---|---|
| Single auto-increment | Hotspot / multi-instance ID allocation | Snowflake / HiLo / DB sequence per shard |
| Guessable codes | Scraping, spam of other people's links | 128-bit random + unique index |
| H2 file + local Caffeine | One JVM | Postgres + Redis, or a dedicated redirect tier |
