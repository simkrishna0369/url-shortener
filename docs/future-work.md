# Future work

Intentional non-goals for v1. Another LLM must **not** implement these unless the human explicitly asks.

These items came from trade-offs in ADRs and `docs/final-summary.md`. They are the production upgrade path, not missing homework.

## Ranked backlog (if we continue this product)

| Priority | Item | Why it is next | What it replaces |
|---|---|---|---|
| 1 | Postgres (or similar) instead of H2 file | Real durability, backups, concurrent writers | `jdbc:h2:file:...` |
| 2 | Redis (or equivalent) for redirect cache + rate limit | Multi-instance; Caffeine and the token bucket are per-JVM today | Caffeine + in-memory `RateLimitFilter` |
| 3 | High-entropy random short codes | Sequential Base62 ids are enumerable (ADR-002) | `Base62Encoder.encode(id)` |
| 4 | Click ingestion via outbox / log / queue | `@Async` executor drops clicks when the queue is full (ADR-003) | `ClickRecordingService` thread pool |
| 5 | Optional idempotent shorten | Same long URL → same code, if product wants that | Current “every POST is a new code” |
| 6 | OAuth2 sign-in, authorization, and ownership (see roles below) | Stop anonymous delete/analytics; identify who created a link | Anonymous v1 API. **Do not add Spring Security in this prototype unless asked** |
| 7 | Optional per-link 301 | Only if a creator opts into cacheable redirects and accepts weaker analytics | 302-only default (ADR-001) |
| 8 | GeoIP breakdown | Needs an IP DB + retention/privacy decision | Device/referrer from headers only |
| 9 | Web / app server scaling (more Spring Boot instances behind a load balancer) | More users than one JVM can serve for create + redirect | Single-process deploy. **Depends on #1 and #2** so IDs, cache, and rate limits are shared — extra instances without Redis will split buckets and serve stale redirects after delete |
| 10 | Database scaling (connection pooling, read replicas, failover) after Postgres | More concurrent readers/writers and HA than one file DB | H2 file (one writer, no replica story). **Depends on #1** — do not shard or replica H2; move to Postgres first, then scale that store |
| 11 | AWS production deploy (example topology below) | Run the API for real users with ops, not `java -jar` on one laptop | Local Maven + H2 file + in-process Caffeine. **Depends on #1, #9, #10** |
| 12 | Sign-in page + role-based screens (admin vs normal user) | People log in and only see what their role allows | API-only v1 (no UI). **Depends on #6** — screens without OAuth2 would be a fake login |

## Sign-in, OAuth2, and roles (example)

v1 has no login, no IdP, and no frontend. If we add product accounts later:

- **Sign-in page** — users authenticate here (not by knowing a short code).
- **OAuth2** — authentication (who you are) and authorization (what you may do), via an identity provider (Google, GitHub, Cognito, etc.) rather than a homemade password store.
- **Normal users** — only some screens: e.g. create/manage **their** short links, view **their** analytics. Redirect `GET /{shortCode}` stays public.
- **Admins** — many more screens: e.g. all links, deactivate any code, rate-limit/config, operational metrics.

Do not add a UI, OAuth2 client, or role tables unless the human asks for items 6 or 12.

## Production deploy on AWS (example)

v1 has no cloud account, Terraform, or runbooks. If we later assume **AWS**, these are the action items that implement scaling and availability — not homework for this repo.

| Action | AWS service | Why |
|---|---|---|
| Run app servers (horizontal scale, item 9) | **EC2** (ASG behind a load balancer) | More users than one JVM; replace instances independently of the DB |
| Managed Postgres (items 1 and 10) | **RDS** | Durability, backups, replicas/failover instead of `jdbc:h2:file:...` |
| Metrics, alarms, and on-call paging | **CloudWatch** (metrics + alarms; SNS/PagerDuty or similar for pages) | Alert when error rate, latency, or 5xx spikes — page someone if the API is down or congested |
| Higher availability | **Multiple Availability Zones** | App (and RDS Multi-AZ) in more than one AZ so one AZ outage does not take the product down |

Do not add AWS SDK, CloudWatch agents, or deploy scripts unless the human asks for this row. Other clouds can map the same jobs (compute, managed Postgres, metrics/paging, multi-zone).

## Explicitly not planned

- Hashing the long URL as the primary code generator (collisions + we already rejected it).
- Shipping 301 as the only redirect type.
- Building the sign-in UI or OAuth2 in **v1** (assignment is API-only). That work is items 6 and 12, not this repo until asked.

## How to pick work up later

1. Confirm the human wants an item from this table (not a new idea from the model).
2. Write or update an ADR **before** swapping H2, Caffeine, or 302. Same for items 6, 9–12: do not add OAuth2, a sign-in page, role screens, a load balancer, extra app nodes, DB replicas, EC2/RDS, CloudWatch, or multi-AZ wiring in this prototype unless the human named those rows.
3. Add tasks to `docs/decomposition.md` rather than generating a parallel design.
4. Keep `docs/coding-standards.md` and `/api/v1` contracts stable unless the ADR says otherwise.
