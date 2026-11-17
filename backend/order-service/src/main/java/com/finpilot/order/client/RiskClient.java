package com.finpilot.order.client;

import com.finpilot.order.config.AppProperties;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;

@Component
public class RiskClient {
    private final WebClient webClient;

    public RiskClient(WebClient.Builder builder, AppProperties properties) {
        this.webClient = builder.baseUrl(properties.risk()).build();
    }

    public RiskCheckResponse checkOrder(RiskCheckRequest request) {
        return webClient
                .post()
                .uri("/api/v1/internal/risk/check-order")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(RiskCheckResponse.class)
                .block();
    }

    public record RiskCheckRequest(
            Long userId,
            String symbol,
            String side,
            BigDecimal quantity,
            BigDecimal price,
            BigDecimal orderValue) {
    }

    public record RiskCheckResponse(boolean approved, String reason) {
    }
}