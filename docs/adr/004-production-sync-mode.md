# ADR 004: Sync mode on managed PaaS

## Status

Accepted

## Context

Running Kafka, Redis, Zookeeper, and eight JVMs on a small Render plan is impractical.

## Decision

Production profile sets `finpilot.events.kafka-enabled=false` and `finpilot.cache.mode=simple`. After a successful buy, `order-service` calls portfolio internal HTTP. Flyway migrates Postgres via `DATABASE_URL`.

## Consequences

- Easier demo hosting; weaker decoupling than full event mesh
- Cache is per-instance, not shared
- Local Docker stack still exercises Kafka, Redis, and outbox for development