# FinPilot — single Render web service (all JVM microservices + gateway)
FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY backend backend
RUN cd backend && mvn -B -DskipTests package

FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
RUN apt-get update && apt-get install -y --no-install-recommends wget && rm -rf /var/lib/apt/lists/*

COPY --from=build /app/backend/user-service/target/user-service-0.1.0-SNAPSHOT.jar /app/backend/jars/user-service.jar
COPY --from=build /app/backend/portfolio-service/target/portfolio-service-0.1.0-SNAPSHOT.jar /app/backend/jars/
COPY --from=build /app/backend/transaction-service/target/transaction-service-0.1.0-SNAPSHOT.jar /app/backend/jars/
COPY --from=build /app/backend/market-data-service/target/market-data-service-0.1.0-SNAPSHOT.jar /app/backend/jars/
COPY --from=build /app/backend/risk-service/target/risk-service-0.1.0-SNAPSHOT.jar /app/backend/jars/
COPY --from=build /app/backend/order-service/target/order-service-0.1.0-SNAPSHOT.jar /app/backend/jars/
COPY --from=build /app/backend/ai-service/target/ai-service-0.1.0-SNAPSHOT.jar /app/backend/jars/
COPY --from=build /app/backend/api-gateway/target/api-gateway-0.1.0-SNAPSHOT.jar /app/backend/jars/

COPY infrastructure/render/start-render.sh /app/start-render.sh
RUN chmod +x /app/start-render.sh

EXPOSE 8080
ENV PORT=8080
HEALTHCHECK --interval=30s --timeout=5s --start-period=120s --retries=5 CMD wget -q -O- http://127.0.0.1:${PORT:-8080}/actuator/health || exit 1
CMD ["/app/start-render.sh"]