#!/usr/bin/env bash
set -eu

JAR_DIR="/app/backend/jars"
export SPRING_PROFILES_ACTIVE="${SPRING_PROFILES_ACTIVE:-prod}"
export FINPILOT_EVENTS_KAFKA_ENABLED="${FINPILOT_EVENTS_KAFKA_ENABLED:-false}"
export FINPILOT_CACHE_MODE="${FINPILOT_CACHE_MODE:-simple}"
export JWT_SECRET="${JWT_SECRET:-finpilot-dev-secret-change-in-production-32chars}"
export FINPILOT_JWT_SECRET="${JWT_SECRET}"
# Set on Render to your Vercel URL(s), comma-separated — do not use * with browser cookies/JWT
export FINPILOT_CORS_ORIGINS="${FINPILOT_CORS_ORIGINS:-https://fin-pilot-sepia-nine.vercel.app}"
export FINPILOT_RENDER_START_AI="${FINPILOT_RENDER_START_AI:-false}"

# Render sets PORT=10000 (public). Only the gateway may listen on it — do NOT export SERVER_PORT globally.
GATEWAY_PORT="${PORT:-8080}"

JAVA_USER=(java -XX:+UseContainerSupport -Xms48m -Xmx140m -XX:MaxMetaspaceSize=112m -XX:+ExitOnOutOfMemoryError)
JAVA_SMALL=(java -XX:+UseContainerSupport -Xms16m -Xmx44m -XX:MaxMetaspaceSize=64m -XX:+ExitOnOutOfMemoryError)
JAVA_GATEWAY=(java -XX:+UseContainerSupport -Xms48m -Xmx160m -XX:MaxMetaspaceSize=96m -XX:+ExitOnOutOfMemoryError)
GATEWAY_LOG="/tmp/api-gateway-0.1.0-SNAPSHOT.log"

# Override Render's PORT/SERVER_PORT so Spring Boot does not bind microservices on the public port.
start_background() {
  local port="$1"
  local jar="$2"
  env -u PORT SERVER_PORT="${port}" \
    "${JAVA_SMALL[@]}" -Dserver.port="${port}" -jar "${jar}" \
    >> "/tmp/$(basename "${jar}" .jar).log" 2>&1 &
  echo "Started $(basename "${jar}") on port ${port} pid=$!"
}

wait_for_gateway() {
  echo "Waiting for gateway on port ${GATEWAY_PORT}..."
  local attempt=0
  while [ "${attempt}" -lt 90 ]; do
    attempt=$((attempt + 1))
    if wget -q -O- "http://127.0.0.1:${GATEWAY_PORT}/actuator/health" 2>/dev/null | grep -qE '"status"\s*:\s*"UP"'; then
      echo "Gateway is UP on port ${GATEWAY_PORT}"
      return 0
    fi
    if [ $((attempt % 15)) -eq 0 ]; then
      echo "Still waiting for gateway... attempt ${attempt}/90"
      if command -v ss >/dev/null 2>&1; then
        ss -tln 2>/dev/null | grep ":${GATEWAY_PORT} " || true
      fi
    fi
    sleep 2
  done
  echo "===== gateway log (tail) ====="
  tail -n 80 "${GATEWAY_LOG}" 2>/dev/null || true
  return 1
}

echo "Container memory (MiB):"
free -m 2>/dev/null || true

env -u PORT SERVER_PORT=8081 \
  "${JAVA_USER[@]}" -Dserver.port=8081 -jar "${JAR_DIR}/user-service.jar" \
  >> "/tmp/user-service.log" 2>&1 &
echo "Started user-service.jar on port 8081 pid=$! (Flyway may still be running)"

echo "Starting API gateway on port ${GATEWAY_PORT} (bind 0.0.0.0)..."
env SERVER_PORT="${GATEWAY_PORT}" PORT="${GATEWAY_PORT}" \
  "${JAVA_GATEWAY[@]}" \
  -Dserver.port="${GATEWAY_PORT}" \
  -Dserver.address=0.0.0.0 \
  -Dspring.main.lazy-initialization=true \
  -jar "${JAR_DIR}/api-gateway-0.1.0-SNAPSHOT.jar" >> "${GATEWAY_LOG}" 2>&1 &
GATEWAY_PID=$!

if ! wait_for_gateway; then
  echo "===== user-service log (tail) ====="
  tail -n 40 "/tmp/user-service.log" 2>/dev/null || true
  echo "Gateway failed to start — see logs above. Check DATABASE_URL and JWT_SECRET on Render."
  exit 1
fi

start_background 8082 "${JAR_DIR}/portfolio-service-0.1.0-SNAPSHOT.jar"
sleep 4
start_background 8084 "${JAR_DIR}/transaction-service-0.1.0-SNAPSHOT.jar"
sleep 4
start_background 8085 "${JAR_DIR}/market-data-service-0.1.0-SNAPSHOT.jar"
sleep 4
start_background 8086 "${JAR_DIR}/risk-service-0.1.0-SNAPSHOT.jar"
sleep 4
start_background 8083 "${JAR_DIR}/order-service-0.1.0-SNAPSHOT.jar"
sleep 4

if [ "${FINPILOT_RENDER_START_AI}" = "true" ]; then
  start_background 8087 "${JAR_DIR}/ai-service-0.1.0-SNAPSHOT.jar"
else
  echo "Skipping ai-service on Render (set FINPILOT_RENDER_START_AI=true to enable)"
fi

echo "All services launched; holding gateway pid=${GATEWAY_PID}"
wait "${GATEWAY_PID}"