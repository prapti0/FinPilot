# FinPilot

**Simulated wealth & trading platform** — Spring Boot microservices, PostgreSQL ledger, Kafka events, Redis caching, React dashboard, and optional Gemini-powered insights. Paper trading only; no real-money execution.

## Highlights

- **Microservices** behind an API gateway (auth, portfolio, orders, ledger, market data, risk, AI)
- **Financial core** — reserve / settle / release cash, double-entry ledger lines, pessimistic locking on balances, idempotent orders (`Idempotency-Key`)
- **Event-driven path (local)** — transactional outbox → Kafka → portfolio updates; **DLQ** topics (`*.dlq`) after retries
- **Reliability** — gateway rate limiting, correlation IDs, circuit breaker on ledger calls, scheduled ledger reconciliation job
- **Production deploy** — Render (API + Postgres) and Vercel (UI); Flyway migrations; sync HTTP + in-memory cache where Kafka/Redis are not available
- **Observability** — Spring Actuator, Prometheus, Grafana, Jaeger traces (gateway OTLP), k6 smoke script in `scripts/load/`
- **Engineering docs** — ADRs under `docs/adr/`, Kubernetes reference in `infrastructure/k8s/`

## Tech stack

| Layer | Technologies |
| ----- | ------------ |
| Backend | Java 21, Spring Boot 3, Spring Cloud Gateway, JPA/JDBC, WebClient |
| Data | PostgreSQL, Flyway, Redis |
| Messaging | Apache Kafka |
| Frontend | React, TypeScript, Vite |
| AI | Gemini API (optional), knowledge-base retrieval |
| Ops | Docker Compose, Jenkinsfile, Render, Vercel |

## Quick start

**Prerequisites:** Docker Desktop, Java 21, Maven, Node 18+

```bash
cd infrastructure/docker && docker compose up -d
bash scripts/start-local.sh
cd frontend && npm install && npm run dev
```

| | |
| --- | --- |
| App | http://localhost:5173 |
| API | http://localhost:8080 |
| Demo login | `demo@finpilot.com` / `demo1234` (₹50,000 simulated cash) |
| Postgres (host) | port **5433** |

Backend logs: `/tmp/finpilot-*.log` if a service fails to start.

## Services

| Service | Responsibility |
| ------- | ---------------- |
| api-gateway | Routing, JWT validation, CORS |
| user-service | Authentication, users, account profiles |
| portfolio-service | Portfolios, holdings, performance |
| order-service | Order orchestration, idempotency |
| transaction-service | Ledger and balance updates |
| market-data-service | Simulated quotes (cached) |
| risk-service | Pre-trade checks |
| ai-service | Portfolio Q&A and analysis (read-only vs ledger) |

Ledger and balance changes go through **order-service** and **transaction-service** only. AI explains data; it does not post transactions.

## Repository layout

```
FinPilot/
  backend/          Spring Boot services + finpilot-common
  frontend/         React application
  infrastructure/   Docker, Render image
  monitoring/       Prometheus configuration
  docs/             API reference, schema, deployment guide
```

## Build

```bash
cd backend && mvn verify
cd frontend && npm run build
```

## Deployment

Step-by-step setup: [docs/DEPLOY.md](docs/DEPLOY.md). API on **Render** (`render.yaml`), frontend on **Vercel** with `VITE_API_BASE_URL` set to the Render service URL.

## Disclaimer

Simulated trading for demonstration and engineering showcase only. Not a licensed brokerage or investment product.