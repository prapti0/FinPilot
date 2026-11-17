# Deploy FinPilot — Render + Vercel

## Architecture (production)

| Layer | Platform |
|-------|----------|
| React UI | **Vercel** |
| API Gateway + 7 JVM services (one container) | **Render** Docker Web Service |
| PostgreSQL | **Render** managed database |
| Events | **Sync HTTP** (`FINPILOT_EVENTS_KAFKA_ENABLED=false`) — no Kafka on PaaS |
| Cache | **In-memory** (`FINPILOT_CACHE_MODE=simple`) — optional Upstash Redis later |

Financial rules unchanged: ledger in PostgreSQL, AI read-only, idempotent orders.

### Finance & data integrity (production)

| Concern | How FinPilot handles it |
|---------|-------------------------|
| **Ledger** | `transaction-service` — reserve → settle / release; balances in `accounts` with optimistic `version` |
| **Idempotency** | `Idempotency-Key` on orders; duplicate keys return the same order |
| **Double execution** | Sync mode: HTTP `buy-execution` only after successful settle (same TX boundary + compensation on failure) |
| **Events** | Local: Kafka + outbox; Render: `finpilot.events.kafka-enabled=false` (outbox publishers disabled) |
| **Cache** | Quotes / portfolio summary: in-memory per instance (`finpilot.cache.mode=simple`); safe for read paths |
| **DB** | Single Render Postgres + Flyway `V1` schema; no `pgvector` required (RAG uses text search) |
| **Secrets** | `JWT_SECRET` generated on Render; never commit `GEMINI_API_KEY` |

---

## 1. Render (backend + database)

### Option A — Blueprint

1. Push repo to GitHub.
2. Render Dashboard → **New** → **Blueprint** → connect repo.
3. Confirm `render.yaml` creates `finpilot-db` + `finpilot-api`.
4. Set env **`FINPILOT_CORS_ORIGINS`** to your Vercel URL (exact, no trailing slash), e.g.  
   `https://fin-pilot-sepia-nine.vercel.app`  
   Add preview URLs comma-separated if you use branch deploys.
5. Optional: `GEMINI_API_KEY` for live AI (else mock narratives).

### Option B — Manual Docker

- **New Web Service** → Docker → Dockerfile path: `infrastructure/render/Dockerfile`
- Attach PostgreSQL; map `DATABASE_URL` automatically.
- Health check path: `/actuator/health` on port **8080**.

### First deploy

- Flyway on **user-service** runs `V1__schema.sql` + seed knowledge on startup.
- Demo user: `demo@finpilot.com` / `demo1234` (created if missing).

---

## 2. Vercel (frontend)

1. Import GitHub repo, root directory: **`frontend`**.
2. Framework: **Vite** (auto).
3. Environment variable (Production):

   `VITE_API_BASE_URL=https://<your-render-service>.onrender.com`

   No trailing slash.

4. Deploy.

Login calls `https://render-host/api/v1/...` with CORS from gateway.

---

## 3. Local vs production

| Setting | Local (`start-local.sh`) | Render |
|---------|--------------------------|--------|
| DB | Docker Postgres `:5433` | `DATABASE_URL` SSL |
| Kafka | On | Off (sync portfolio update) |
| Redis | On | Simple in-memory cache |
| API URL (UI) | Vite proxy `/api` | `VITE_API_BASE_URL` |

---

## 4. Optional upgrades

- **Upstash Redis**: set `REDIS_URL`, `FINPILOT_CACHE_MODE=redis`, enable Redis auto-config in prod profile.
- **Kafka**: run Confluent Cloud, set `FINPILOT_EVENTS_KAFKA_ENABLED=true` + `SPRING_KAFKA_BOOTSTRAP_SERVERS`.
- **Split microservices**: deploy each JAR as separate Render service (same env pattern).

---

## 5. Render troubleshooting

| Symptom | Likely cause | Fix |
|--------|----------------|-----|
| `No open ports detected` | Normal for **~60s after** `Starting API gateway...`; Render keeps scanning | Wait **2–3 min** for `Gateway is UP` or gateway log tail |
| `postgresql.Driver` on **api-gateway** | `RenderDataSourceConfiguration` without JDBC driver | Fixed: config only loads when driver is on classpath |
| `Tomcat started on port 10000` on **user-service** | `SERVER_PORT` / `PORT` applied to every JVM | Fixed: only gateway uses Render `$PORT`; others use 8081–8087 |
| `Gateway failed to start` in logs | Bad `DATABASE_URL` or JWT | Full **Internal** Postgres URL; regenerate JWT if leaked |
| Login works, orders 502 | Backend JVMs still booting | Wait 2–3 min after deploy |
| Browser CORS error | `FINPILOT_CORS_ORIGINS` wrong | Exact Vercel URL, comma-separated, then **Redeploy** |
| `*` CORS + auth fails | Wildcard blocks credentials | Use real origins, not `*` |
| AI page empty | `FINPILOT_RENDER_START_AI=false` | Set `true` only if RAM allows (extra JVM) |

**Env checklist:** `DATABASE_URL`, `SPRING_PROFILES_ACTIVE=prod`, `JWT_SECRET`, `FINPILOT_CORS_ORIGINS`, `FINPILOT_EVENTS_KAFKA_ENABLED=false`, `FINPILOT_CACHE_MODE=simple`.

**Render JVM footprint:** `FINPILOT_RENDER_PROFILE=core` (default — login + portfolios + market + risk), `lite` (auth only), or `FINPILOT_RENDER_START_ALL=true` (full stack on **2GB+**). `/portfolios` and `/risk/alerts` **500** in `lite` because those JVMs are not running.

**Vercel:** `VITE_API_BASE_URL=https://finpilot-7gpk.onrender.com` then **Redeploy** the frontend.

---

## 6. Smoke test after deploy

```bash
curl https://YOUR-API.onrender.com/actuator/health
curl -X POST https://YOUR-API.onrender.com/api/v1/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"demo@finpilot.com","password":"demo1234"}'
```