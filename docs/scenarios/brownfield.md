# Scenario: Brownfield — hard delete → soft delete

## The bug

Greenfield `DELETE /api/v1/urls/{shortCode}` removed the `short_urls` row. `click_events.short_url_id` is a foreign key. That either:

- cascades and **wipes click history** (FR6 fails for retired links), or
- fails the delete with a constraint error (API looks broken).

Analytics for a deactivated campaign is a realistic after-the-fact requirement: “stop redirecting, but keep the numbers.”

## Impacted modules (written before changing behavior)

| Module | How it touched `ShortUrl` | Required change |
|---|---|---|
| `ShortUrl` entity | Needed a lifecycle flag | Use `active` (column already existed, unused by queries) |
| `ShortUrlRepository` | `findByShortCode` used everywhere | Redirect uses `findByShortCodeAndActiveTrue`; metadata/analytics still load inactive rows |
| `RedirectController` / `requireActive` | Would 302 a deleted row if we only flipped a flag and forgot filters | Must 404 when `active = false` |
| Caffeine `shortUrls` cache | Could serve a stale target after delete | `@CacheEvict` on deactivate |
| `ClickEvent` / analytics query | Join/FK to `short_urls` | Keep the parent row; do not cascade-remove events |
| `DELETE` handler | Was a repository `delete` | Set `active = false` and save |

## Fix

Shipped behavior is soft delete:

1. `DELETE` sets `active = false` and evicts the cache.
2. Redirect looks up **active** rows only → 404.
3. Metadata still returns the row with `"active": false`.
4. Analytics still returns historical clicks.

## Regression

Integration test `should_keepAnalytics_when_linkIsSoftDeleted` asserts 404 on redirect, `active=false` on metadata, and unchanged `clickCount`. Create/redirect for *other* codes is unchanged (new rows default `active=true`).
