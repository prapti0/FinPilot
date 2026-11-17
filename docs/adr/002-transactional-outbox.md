# ADR 002: Transactional outbox for Kafka

## Status

Accepted (local); optional in production

## Context

Publishing to Kafka in the same business transaction as a DB commit risks lost or duplicate events.

## Decision

Write `outbox_events` in the same database transaction as orders/ledger rows. A scheduler publishes to Kafka and marks rows published.

## Consequences

- At-least-once delivery to consumers; consumers must tolerate retries
- On Render, Kafka is disabled; portfolio updates use synchronous HTTP instead
- Failed consumers route to `*.dlq` topics after retries (see `KafkaDlqAutoConfiguration`)