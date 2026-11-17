package com.finpilot.ai.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.finpilot.ai.config.AppProperties;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
public class PortfolioClient {
    private final WebClient webClient;

    public PortfolioClient(WebClient.Builder builder, AppProperties properties) {
        this.webClient = builder.baseUrl(properties.services().portfolio()).build();
    }

    public PortfolioDetail fetchPortfolio(Long portfolioId, Long userId) {
        return webClient
                .get()
                .uri("/api/v1/portfolios/{id}", portfolioId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer internal")
                .header("X-User-Id", String.valueOf(userId))
                .retrieve()
                .bodyToMono(PortfolioDetail.class)
                .block();
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record PortfolioDetail(Long id, String name, Summary summary) {
        @JsonIgnoreProperties(ignoreUnknown = true)
        public record Summary(
                Long portfolioId, int holdingCount, java.math.BigDecimal investedValue, java.util.Map<String, java.math.BigDecimal> sectorWeights) {
        }
    }
}