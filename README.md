# FinPilot — AI-Powered Wealth & Trading Engineering Platform

**Simulated trading only** — production-style microservices with deterministic financial core, Kafka events, Redis cache, Gemini-ready AI, and engineering agent.

## Feature map (6-week roadmap — implemented)

| Week | Delivered in repo |
|------|-------------------|
| 1 | JWT auth, user/cash accounts, portfolio APIs, React dashboard |
| 2 | Buy simulation, double-entry ledger, reserves, idempotency, pessimistic cash lock |
| 3 | Kafka topics, transactional outbox, Redis quotes/cache, saga release on failure |
| 4 | AI portfolio Q&A, RAG (`knowledge_chunks`), risk explanations, `ai_reports` |
| 5 | Engineering agent PR review API (sandboxed, no auto-merge) |
| 6 | Docker Compose (PG, Redis, Kafka, Prometheus, Grafana), Jenkinsfile, actuator/prometheus |

## Quick start

```bash
# 1) Docker Desktop ON
cd /Users/praptiwamre/Desktop/FinPilot/infrastructure/docker
docker compose up -d

# 2) Backend + ordered boot (user first)
/Users/praptiwamre/Desktop/FinPilot/scripts/start-local.sh

# 3) Frontend (separate terminal)
cd /Users/praptiwamre/Desktop/FinPilot/frontend
npm install && npm run dev
```

### URLs

| Service | URL |
|---------|-----|
| App | http://localhost:5173 |
| API Gateway | http://localhost:8080 |
| Grafana | http://localhost:3001 (`admin` / `admin`) |
| Prometheus | http://localhost:9090 |

### Demo login

- **Email:** `demo@finpilot.com`
- **Password:** `demo1234`
- Starting cash: **₹50,000** (simulated)

Postgres on host **5433** (avoids local PostgreSQL on 5432).

## Architecture boundary

PostgreSQL ledger + account balances are updated only by **transaction-service** and **order** workflow. **AI** reads and explains; it never posts ledger entries.

## Project layout

```
FinPilot/
  backend/          # 8 Spring Boot services + finpilot-common
  frontend/         # React + TypeScript
  infrastructure/   # docker, jenkins
  monitoring/       # prometheus.yml
  docs/             # schema, API, events
```

## Build & test

```bash
cd backend && mvn verify
cd frontend && npm run build
```