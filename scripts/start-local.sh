#!/usr/bin/env bash
set -eu
ROOT="$(cd "$(dirname "$0")/.." && pwd)"

echo "==> Docker (Postgres :5433, Redis, Kafka)"
cd "$ROOT/infrastructure/docker"
docker compose up -d postgres redis kafka zookeeper

echo "==> Build backend"
cd "$ROOT/backend"
mvn -q -DskipTests install

echo "==> Start microservices"
pkill -f 'spring-boot:run' 2>/dev/null || true
sleep 2

start_svc() {
  mvn -q -pl "$1" spring-boot:run > "/tmp/finpilot-$1.log" 2>&1 &
  echo "  started $1"
}

start_svc user-service
sleep 12
for svc in portfolio-service transaction-service market-data-service risk-service order-service ai-service api-gateway; do
  start_svc "$svc"
done

echo "Waiting for gateway..."
for i in $(seq 1 45); do
  if curl -sf http://127.0.0.1:8080/actuator/health >/dev/null; then
    echo "Gateway UP on http://localhost:8080"
    break
  fi
  sleep 2
done

echo ""
echo "App:     http://localhost:5173  (run: cd frontend && npm run dev)"
echo "Login:   demo@finpilot.com / demo1234"