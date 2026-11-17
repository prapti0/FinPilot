package com.finpilot.risk.kafka;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.finpilot.risk.service.RiskService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class OrderCreatedListener {
    private static final Logger log = LoggerFactory.getLogger(OrderCreatedListener.class);
    private final ObjectMapper objectMapper;
    private final RiskService riskService;

    public OrderCreatedListener(ObjectMapper objectMapper, RiskService riskService) {
        this.objectMapper = objectMapper;
        this.riskService = riskService;
    }

    @KafkaListener(topics = "finpilot.order-created", groupId = "risk-service")
    public void onMessage(String json) {
        try {
            JsonNode root = objectMapper.readTree(json);
            JsonNode payload = root.get("payload");
            if (payload == null) {
                return;
            }
            Long userId = payload.path("userId").asLong();
            Long orderId = payload.path("orderId").asLong();
            String symbol = payload.path("symbol").asText();
            BigDecimal orderValue = new BigDecimal(payload.path("estimatedValue").asText("0"));
            riskService.handleOrderCreated(userId, symbol, orderValue, orderId);
        } catch (Exception ex) {
            log.warn("Failed order-created event: {}", ex.getMessage());
        }
    }
}