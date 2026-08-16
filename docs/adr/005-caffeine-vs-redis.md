# ADR-005: Caffeine cache-aside instead of Redis (or a raw map)

## Status

Accepted

## Context

Redirect `GET /{shortCode}` is the hot path. Hitting the DB on every hop is correct but not what we want under repeated traffic. Options:

| Option | Process | TTL / size limits | Multi-instance |
|---|---|---|---|
| `ConcurrentHashMap` | Same JVM | We would build them | No |
| Guava Cache | Same JVM | Yes | No |
| **Caffeine** | Same JVM | Yes (W-TinyLFU) | No |
| Redis | Separate | Yes | Yes |

## Decision

Use **Caffeine** via Spring `@Cacheable` / `@CacheEvict` as **cache-aside** on short-code → `RedirectTarget`. TTL and max size are `app.cache.*` in `application.yml`.

We went with Caffeine because v1 is **one JVM**, the redirect path needs a **bounded** local cache (TTL + max size), and Caffeine is the cache Spring Boot documents next to `spring-boot-starter-cache`. Redis (or Hazelcast / Infinispan) is the right family when several instances must **share** entries and eviction after `DELETE` — that is future work, not this prototype.

## Why Caffeine (not a HashMap)

A map has no TTL and no eviction. A deactivated link, or a burst of distinct codes, would grow until the JVM suffers. Caffeine gives bounded size, expire-after-write, and is the cache Spring Boot documents next to `spring-boot-starter-cache`.

## Why Caffeine (not Guava Cache)

Caffeine is Guava Cache’s successor: better default eviction (W-TinyLFU), and first-class `CaffeineCacheManager` in Spring. No reason to take the older API for new code.

## Why Caffeine (not Redis) in v1

Redis is the right cache when **more than one JVM** serves redirects: shared entries, shared eviction after `DELETE`. It is also another process to install, network to the cache, and a failure mode (“Redis down → what does redirect do?”).

v1 is a **single process**. An in-process cache demonstrates the **pattern** (aside lookup, DTO not entity, evict on soft-delete) without pretending we have a cluster. Redis is item 2 in `docs/future-work.md`.

## What we cache

`RedirectTarget` (`id`, `shortCode`, `longUrl`) — not a JPA `ShortUrl` entity. Cached Hibernate objects go stale or throw when used off-thread (click recording).

On `DELETE` (deactivate), `@CacheEvict` so a 302 cannot be served from a stale hit.

## Consequences

- Fast repeats for the same code; first hop still pays for DB.
- Two app instances will **not** see each other’s cache or evictions.
- Click recording stays async and does not go through this cache.
