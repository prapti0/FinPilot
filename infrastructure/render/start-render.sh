#!/usr/bin/env bash
set -eu

JAR_DIR="/app/backend/jars"
export SPRING_PROFILES_ACTIVE="${SPRING_PROFILES_ACTIVE:-prod}"
export FINPILOT_EVENTS_KAFKA_ENABLED="${FINPILOT_EVENTS_KAFKA_ENABLED:-false}"
export FINPILOT_CACHE_MODE="${FINPILOT_CACHE_MODE:-simple}"
export JWT_SECRET="${JWT_SECRET:-finpilot-dev-secret-change-in-production-32chars}"
export FINPILOT_JWT_SECRET="${JWT_SECRET}"
export FINPILOT_CORS_ORIGINS="${FINPILOT_CORS_ORIGINS:-*}"
LISTEN_PORT="${PORT:-8080}"
export SERVER_PORT="${LISTEN_PORT}"

JAVA_SMALL=(java -XX:+UseContainerSupport -Xms24m -Xmx56m -XX:MaxMetaspaceSize=64m)
JAVA_GATEWAY=(java -XX:+UseContainerSupport -Xms48m -Xmx160m -XX:MaxMetaspaceSize=96m)

start_background() {
  "${JAVA_SMALL[@]}" -jar "$1" > "/tmp/$(basename "$1" .jar).log" 2>&1 &
  echo "Started $(basename "$1") pid=$!"
}

start_background "${JAR_DIR}/user-service.jar"
echo "Waiting for user-service (Flyway + DB)..."
sleep 35

for jar in portfolio-service transaction-service market-data-service risk-service order-service ai-service; do
  start_background "${JAR_DIR}/${jar}-0.1.0-SNAPSHOT.jar"
  sleep 2
done

echo "Starting API gateway in foreground on port ${LISTEN_PORT}..."
exec "${JAVA_GATEWAY[@]}" -Dserver.port="${LISTEN_PORT}" -jar "${JAR_DIR}/api-gateway-0.1.0-SNAPSHOT.jar"