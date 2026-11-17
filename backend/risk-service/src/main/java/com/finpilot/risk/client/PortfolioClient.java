package com.finpilot.risk.client;

import com.finpilot.risk.config.AppProperties;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;
import java.util.List;

@Component
public class PortfolioClient {
    private final WebClient webClient;

    public PortfolioClient(WebClient.Builder builder, AppProperties properties) {
        this.webClient = builder.baseUrl(properties.portfolio()).build();
    }

    public PortfolioRiskSummary fetchRiskSummary(Long portfolioId) {
        return webClient
                .get()
                .uri("/api/v1/internal/portfolios/{id}/risk-summary", portfolioId)
                .retrieve()
                .bodyToMono(PortfolioRiskSummary.class)
                .block();
    }

    public record PortfolioRiskSummary(
            Long userId, Long portfolioId, BigDecimal totalValue, List<HoldingLine> holdings) {
        public record HoldingLine(String symbol, String sector, BigDecimal quantity, BigDecimal marketValue) {
        }
    }
}