# ADR 001: Microservice boundaries

## Status

Accepted

## Context

FinPilot mixes user flows, market data, ledger writes, and AI explanations. A single deployable would be simpler to operate but couples riskier domains.

## Decision

Split into gateway + domain services. Ledger mutations live only in `transaction-service`, orchestrated by `order-service`. AI and risk never call ledger APIs.

## Consequences

- Clear ownership for financial correctness
- Requires idempotency, sync HTTP or Kafka, and compensation on order failures
- Higher local/dev cost (many JVMs or a bundled Render image)