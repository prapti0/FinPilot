package com.finpilot.order.client;

import com.finpilot.order.config.AppProperties;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;
import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@Component
public class MarketDataClient {
    private final WebClient webClient;

    public MarketDataClient(WebClient.Builder builder, AppProperties properties) {
        this.webClient = builder.baseUrl(properties.market()).build();
    }

    public QuoteResponse getQuote(String symbol) {
        return webClient
                .get()
                .uri("/api/v1/market/quotes/{symbol}", symbol)
                .retrieve()
                .bodyToMono(QuoteResponse.class)
                .block();
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record QuoteResponse(String symbol, BigDecimal price, Instant recordedAt) {
    }
}