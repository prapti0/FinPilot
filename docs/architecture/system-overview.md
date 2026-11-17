# FinPilot system overview

## Layers

1. **Client** — React + TypeScript trading dashboard
2. **API Gateway** — JWT, RBAC, rate limiting, routing
3. **Microservices** — User, Portfolio, Order, Transaction, Market Data, Risk, AI
4. **Events** — Apache Kafka (async workflows)
5. **Data** — PostgreSQL (ledger source of truth), Redis (cache)
6. **AI** — Gemini + RAG (read-only analysis; never mutates ledger)
7. **Ops** — Docker Compose, Jenkins, Prometheus, Grafana

## Critical boundary

```
[Deterministic financial core]  ←→  Kafka  ←→  [AI explanation layer]
        PostgreSQL ledger              No direct balance updates from LLM
```

## Buy flow (summary)

Gateway → Order (validate) → Risk (rules) → Transaction (reserve + ledger) → Outbox → Kafka → Execute → Portfolio update.

On execution failure: compensating transaction releases reserved cash (saga).

## Service ports (local)

| Service | Port |
|---------|------|
| api-gateway | 8080 |
| user-service | 8081 |
| portfolio-service | 8082 |
| order-service | 8083 |
| transaction-service | 8084 |
| market-data-service | 8085 |
| risk-service | 8086 |
| ai-service | 8087 |