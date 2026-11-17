package com.finpilot.ai.client;

import com.finpilot.ai.config.AppProperties;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;
import java.util.Map;

@Component
public class RiskClient {
    private final WebClient webClient;

    public RiskClient(WebClient.Builder builder, AppProperties properties) {
        this.webClient = builder.baseUrl(properties.services().risk()).build();
    }

    public PortfolioRisk fetchPortfolioRisk(Long portfolioId) {
        return webClient
                .get()
                .uri("/api/v1/risk/portfolio/{id}", portfolioId)
                .retrieve()
                .bodyToMono(PortfolioRisk.class)
                .block();
    }

    public record PortfolioRisk(
            Long portfolioId, Long userId, BigDecimal totalValue, Map<String, BigDecimal> sectorWeights) {
    }
}