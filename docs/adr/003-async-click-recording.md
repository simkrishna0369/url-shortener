# ADR-003: Record clicks asynchronously

## Status

Accepted

## Context

Task 3.2: writing a click row on every successful redirect adds latency and DB load to the hottest path. Alternatives:

- Synchronous insert in the request thread (simple, strong consistency, slower p99)
- Async executor / queue (fast redirect, eventual count)
- Dual-write: in-memory counter + periodic flush (more moving parts)

## Decision

Publish the click to a bounded Spring `@Async` executor **after** the mapping is resolved and **before** returning 302. If the queue is full, **drop** the click and log a warning.

## Consequences

- Analytics `clickCount` may lag a few milliseconds (tests wait with Awaitility).
- Under overload we prefer a correct, fast redirect over a perfect count.
- A multi-instance production system would replace this with a log/stream (Kafka, Kinesis) and an aggregator, not a JVM thread pool.
