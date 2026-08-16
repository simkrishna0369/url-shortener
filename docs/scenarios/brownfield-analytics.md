# Scenario: Brownfield — analytics read path

## The bug

Greenfield `GET /api/v1/urls/{shortCode}/analytics` loaded **every** `click_events` row into the service and aggregated device/referrer maps in Java. `clickCount` was `events.size()`. That is correct for a handful of demo clicks and wrong as soon as a code is popular: heap and JSON grow with history, while the redirect path stays fast.

## Impacted modules (written before changing behavior)

| Module | How it touched clicks | Required change |
|---|---|---|
| `ShortUrlService.getAnalytics` | Full list + stream `groupingBy` | Orchestrate count / group / window queries only |
| `ClickEventRepository` | `findByShortUrlIdOrderByClickedAtAsc` | `COUNT`, `GROUP BY` device/referrer, paged recent rows |
| `AnalyticsResponse` | `clicks` implied complete history | Keep `clickCount` as the true total; cap `clicks`; add `clicksTruncated` |
| `ClickEvent` indexes | Lookup by `short_url_id` only | Composite `(short_url_id, clicked_at)` for the recent-window query |
| Config | No bound on payload size | `app.analytics.clicks-limit` (default 100) |

## Fix

1. `clickCount` and `breakdown` come from SQL aggregates (all rows).
2. `clicks` is the **most recent** N events, returned oldest-to-newest inside that window so small demos still look chronological.
3. `clicksTruncated` is true when `clickCount > clicks.length`.

## Rejected

- Dropping `clicks[]` entirely (breaks the time-series reading we locked for analytics).
- Pagination API (`?page=`) in this pass (bigger contract change than a cap).
- Redis / stream aggregators (future work; this is still one JVM + H2).

## Regression

Existing create/redirect/analytics tests still expect full `clicks` when count is below the cap. `AnalyticsCapIntegrationTest` sets the cap to 2 and asserts three redirects → `clickCount=3`, two click DTOs, `clicksTruncated=true`.
