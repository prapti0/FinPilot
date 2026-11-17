#!/usr/bin/env bash
set -eu

JAR_DIR="/app/backend/jars"
export SPRING_PROFILES_ACTIVE="${SPRING_PROFILES_ACTIVE:-prod}"
export FINPILOT_EVENTS_KAFKA_ENABLED="${FINPILOT_EVENTS_KAFKA_ENABLED:-false}"
export FINPILOT_CACHE_MODE="${FINPILOT_CACHE_MODE:-simple}"
export JWT_SECRET="${JWT_SECRET:-finpilot-dev-secret-change-in-production-32chars}"
export FINPILOT_JWT_SECRET="${JWT_SECRET}"
# Set on Render to your Vercel URL(s), comma-separated — do not use * with browser cookies/JWT
export FINPILOT_CORS_ORIGINS="${FINPILOT_CORS_ORIGINS:-https://fin-pilot.vercel.app}"
export FINPILOT_RENDER_START_AI="${FINPILOT_RENDER_START_AI:-false}"

LISTEN_PORT="${PORT:-8080}"
export SERVER_PORT="${LISTEN_PORT}"

JAVA_USER=(java -XX:+UseContainerSupport -Xms48m -Xmx140m -XX:MaxMetaspaceSize=80m -XX:+ExitOnOutOfMemoryError)
JAVA_SMALL=(java -XX:+UseContainerSupport -Xms16m -Xmx44m -XX:MaxMetaspaceSize=48m -XX:+ExitOnOutOfMemoryError)
JAVA_GATEWAY=(java -XX:+UseContainerSupport -Xms48m -Xmx160m -XX:MaxMetaspaceSize=96m -XX:+ExitOnOutOfMemoryError)
GATEWAY_LOG="/tmp/api-gateway-0.1.0-SNAPSHOT.log"

start_background() {
  "${JAVA_SMALL[@]}" -jar "$1" >> "/tmp/$(basename "$1" .jar).log" 2>&1 &
  echo "Started $(basename "$1") pid=$!"
}

wait_for_gateway() {
  echo "Waiting for gateway on port ${LISTEN_PORT} (Spring may take 30-120s on free tier — Render 'no open ports' before ~60s is normal)..."
  local attempt=0
  while [ "${attempt}" -lt 90 ]; do
    attempt=$((attempt + 1))
    if wget -q -O- "http://127.0.0.1:${LISTEN_PORT}/actuator/health" 2>/dev/null | grep -qE '"status"\s*:\s*"UP"'; then
      echo "Gateway is UP on port ${LISTEN_PORT}"
      return 0
    fi
    if [ $((attempt % 15)) -eq 0 ]; then
      echo "Still waiting for gateway... attempt ${attempt}/90"
      if command -v ss >/dev/null 2>&1; then
        ss -tln 2>/dev/null | grep ":${LISTEN_PORT} " || true
      fi
    fi
    sleep 2
  done
  echo "===== gateway log (tail) ====="
  tail -n 80 "${GATEWAY_LOG}" 2>/dev/null || true
  return 1
}

# 1) user-service (Flyway) + gateway in parallel — Render scans $PORT from deploy start; don't block gateway 45s
echo "Container memory (MiB):"
free -m 2>/dev/null || true
"${JAVA_USER[@]}" -jar "${JAR_DIR}/user-service.jar" >> "/tmp/user-service.log" 2>&1 &
echo "Started user-service.jar pid=$! (Flyway may still be running)"

echo "Starting API gateway on port ${LISTEN_PORT} (bind 0.0.0.0)..."
"${JAVA_GATEWAY[@]}" \
  -Dserver.port="${LISTEN_PORT}" \
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

# 3) Remaining services (ai optional — extra JVM on small instances)
for jar in portfolio-service transaction-service market-data-service risk-service order-service; do
  start_background "${JAR_DIR}/${jar}-0.1.0-SNAPSHOT.jar"
  sleep 4
done

if [ "${FINPILOT_RENDER_START_AI}" = "true" ]; then
  start_background "${JAR_DIR}/ai-service-0.1.0-SNAPSHOT.jar"
else
  echo "Skipping ai-service on Render (set FINPILOT_RENDER_START_AI=true to enable)"
fi

echo "All services launched; holding gateway pid=${GATEWAY_PID}"
wait "${GATEWAY_PID}"