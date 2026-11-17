# Kubernetes (reference layout)

FinPilot’s default production path is **Render + Vercel**. This folder describes how you would map the same services to Kubernetes without maintaining full charts in-repo.

## Suggested mapping

| Workload | K8s kind | Notes |
| -------- | -------- | ----- |
| api-gateway | Deployment + Service (LoadBalancer/Ingress) | JWT, rate limit, tracing |
| user, portfolio, order, transaction, market, risk, ai | Deployments + ClusterIP Services | One deployment per JAR |
| PostgreSQL | Managed RDS/Cloud SQL or operator | Not in-cluster for prod |
| Redis | Managed cache | Quotes / portfolio cache |
| Kafka | Managed (Confluent/MSK) | Re-enable outbox consumers |

## Config

- `SPRING_PROFILES_ACTIVE=prod` only where applicable
- Secrets: `JWT_SECRET`, `DATABASE_URL`, `GEMINI_API_KEY` via External Secrets
- Ingress TLS termination; `FINPILOT_CORS_ORIGINS` for frontend origin

## Health

Liveness/readiness: `/actuator/health` on each service; gateway is the only public ingress.

For a single-node demo, use the existing `infrastructure/render/Dockerfile` pattern or Helm umbrella chart (not included here).