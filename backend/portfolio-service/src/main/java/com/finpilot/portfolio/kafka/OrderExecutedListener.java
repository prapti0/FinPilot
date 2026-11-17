package com.finpilot.portfolio.kafka;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.finpilot.portfolio.service.PortfolioService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@ConditionalOnProperty(name = "finpilot.events.kafka-enabled", havingValue = "true", matchIfMissing = true)
public class OrderExecutedListener {
    private static final Logger log = LoggerFactory.getLogger(OrderExecutedListener.class);
    private final ObjectMapper objectMapper;
    private final PortfolioService portfolioService;

    public OrderExecutedListener(ObjectMapper objectMapper, PortfolioService portfolioService) {
        this.objectMapper = objectMapper;
        this.portfolioService = portfolioService;
    }

    @KafkaListener(topics = "finpilot.order-executed", groupId = "portfolio-service")
    public void onMessage(String json) {
        try {
            JsonNode root = objectMapper.readTree(json);
            JsonNode payload = root.get("payload");
            if (payload == null) {
                return;
            }
            String side = payload.path("side").asText();
            if (!"BUY".equalsIgnoreCase(side)) {
                return;
            }
            Long portfolioId = payload.path("portfolioId").asLong();
            Long assetId = payload.path("assetId").asLong();
            BigDecimal qty = new BigDecimal(payload.path("executedQuantity").asText());
            BigDecimal price = new BigDecimal(payload.path("executionPrice").asText());
            portfolioService.applyBuyExecution(portfolioId, assetId, qty, price);
        } catch (Exception ex) {
            log.warn("Failed to process order-executed event: {}", ex.getMessage());
            throw new IllegalStateException("order-executed processing failed", ex);
        }
    }
}