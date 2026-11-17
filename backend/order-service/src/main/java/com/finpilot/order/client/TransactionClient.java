package com.finpilot.order.client;

import com.finpilot.order.config.AppProperties;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;

@Component
public class TransactionClient {
    private final WebClient webClient;

    public TransactionClient(WebClient.Builder builder, AppProperties properties) {
        this.webClient = builder.baseUrl(properties.transaction()).build();
    }

    public ReservationResponse reserve(ReserveRequest request) {
        return webClient
                .post()
                .uri("/api/v1/internal/ledger/reserve")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(ReservationResponse.class)
                .block();
    }

    public void release(LedgerActionRequest request) {
        webClient.post().uri("/api/v1/internal/ledger/release").bodyValue(request).retrieve().toBodilessEntity().block();
    }

    public void settle(LedgerActionRequest request) {
        webClient.post().uri("/api/v1/internal/ledger/settle").bodyValue(request).retrieve().toBodilessEntity().block();
    }

    public record ReserveRequest(Long userId, Long orderId, BigDecimal amount, String idempotencyKey) {
    }

    public record ReservationResponse(boolean accepted, String reason) {
    }

    public record LedgerActionRequest(Long userId, Long orderId, String idempotencyKey, BigDecimal amount) {
    }
}