# Scenario: Ambiguous requirement — analytics depth

## The underspecified phrase

The original prompt asked for “analytics” without saying whether that is a counter, a time series, or a marketing breakdown (geo / device / referrer).

## Reasonable readings

| Reading | What it needs | Fit for a redirect-only API |
|---|---|---|
| (a) Integer click count | One counter or `COUNT(*)` | Unambiguous, easy, weak for later charts |
| (b) Count + timestamps | One row per click | Enough for time series; still no “where from” |
| (c) Breakdown by device / referrer / geo | Headers and/or IP databases / JS beacons | Partially available from the redirect request |

## What a server-side 302 can actually see

Available without a client-side script:

- `User-Agent` → coarse device class (`mobile` / `desktop` / `tablet` / `bot` / `unknown`)
- `Referer` → previous page, often absent (`direct`)
- Client IP → **coarse geo only if** we add a GeoIP database and a retention/privacy policy

Not available:

- Precise location, logged-in user identity, on-page events, “time on site”
- Reliable bot filtering beyond UA heuristics

## v1 cut (locked)

Ship **(b) plus a subset of (c)**:

- Persist `userAgent`, `referrer`, `deviceType`
- Expose `breakdown.devices` and `breakdown.referrers` on `GET .../analytics`
- Missing/malformed UA must **not** break the redirect (`unknown`)

## Deliberately left out

- **Geo from IP** — would imply storing IPs, choosing a GeoIP dataset, and answering GDPR/retention questions. Documented as a limitation, not silently skipped as if geo were “done.”
- **Bot filtering** — UA substring checks are not a product-quality bot detector.
- **Client JS beacon** — out of scope (no frontend).

This interpretation is documented so a reviewer can disagree with the cut without reverse-engineering the code.
