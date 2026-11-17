#!/usr/bin/env bash
set -eu

ROOT="/app"
export SPRING_PROFILES_ACTIVE="${SPRING_PROFILES_ACTIVE:-prod}"
export FINPILOT_EVENTS_KAFKA_ENABLED="${FINPILOT_EVENTS_KAFKA_ENABLED:-false}"
export FINPILOT_CACHE_MODE="${FINPILOT_CACHE_MODE:-simple}"
export JWT_SECRET="${JWT_SECRET:-finpilot-dev-secret-change-in-production-32chars}"
export FINPILOT_JWT_SECRET="${JWT_SECRET}"
export FINPILOT_CORS_ORIGINS="${FINPILOT_CORS_ORIGINS:-*}"
GATEWAY_PORT="${PORT:-8080}"

start_jar() {
  java -XX:+UseContainerSupport -Xms32m -Xmx96m -jar "$1" > "/tmp/$(basename "$1" .jar).log" 2>&1 &
  echo "Started $(basename "$1")"
}

start_jar "$ROOT/jars/user-service.jar"
sleep 15
for jar in portfolio-service transaction-service market-data-service risk-service order-service ai-service api-gateway; do
  start_jar "$ROOT/jars/${jar}-0.1.0-SNAPSHOT.jar"
done

echo "Waiting for gateway..."
for i in $(seq 1 60); do
  if wget -q -O- "http://127.0.0.1:${GATEWAY_PORT}/actuator/health" >/dev/null 2>&1; then
    echo "FinPilot API ready on :${GATEWAY_PORT}"
    break
  fi
  sleep 2
done

# Keep container alive following gateway logs
tail -f /tmp/api-gateway-0.1.0-SNAPSHOT.log