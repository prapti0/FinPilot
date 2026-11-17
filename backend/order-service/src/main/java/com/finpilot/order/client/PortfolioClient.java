package com.finpilot.order.client;

import com.finpilot.order.config.AppProperties;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;
import java.util.Map;

@Component
public class PortfolioClient {
    private final WebClient webClient;

    public PortfolioClient(WebClient.Builder builder, AppProperties properties) {
        this.webClient = builder.baseUrl(properties.portfolio()).build();
    }

    public void applyBuyExecution(Long portfolioId, Long assetId, BigDecimal quantity, BigDecimal executionPrice) {
        webClient
                .post()
                .uri("/api/v1/internal/portfolios/{id}/buy-execution", portfolioId)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of(
                        "assetId", assetId,
                        "quantity", quantity,
                        "executionPrice", executionPrice))
                .retrieve()
                .toBodilessEntity()
                .block();
    }
}