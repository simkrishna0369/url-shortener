# ADR-001: Use HTTP 302 for short-URL redirects

## Status

Accepted

## Context

Short links must both send the user to the original URL and record a click. HTTP 301 (Moved Permanently) is commonly used by public shorteners, but browsers and some HTTP clients cache 301 aggressively and may not contact the origin on later visits.

## Decision

`GET /{shortCode}` always returns **302 Found** with a `Location` header. Never 301.

## Consequences

- Every (non-cached-by-us) hop can increment analytics.
- CDNs or clients may still cache 302 in some setups; we do not send long-lived cache headers on the redirect.
- SEO-conscious products sometimes prefer 301; that is an explicit product trade-off we are not making here because analytics is a core requirement.
