package com.finpilot.order.client;

import com.finpilot.order.config.AppProperties;
import com.finpilot.order.config.ResilienceConfig;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.function.Supplier;

@Component
public class TransactionClient {
    private final WebClient webClient;
    private final CircuitBreaker circuitBreaker;

    public TransactionClient(WebClient.Builder builder, AppProperties properties, CircuitBreaker downstreamCircuitBreaker) {
        this.webClient = builder.baseUrl(properties.transaction()).build();
        this.circuitBreaker = downstreamCircuitBreaker;
    }

    public ReservationResponse reserve(ReserveRequest request) {
        return call(() -> webClient
                .post()
                .uri("/api/v1/internal/ledger/reserve")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(ReservationResponse.class)
                .block());
    }

    public void release(LedgerActionRequest request) {
        call(() -> {
            webClient
                    .post()
                    .uri("/api/v1/internal/ledger/release")
                    .bodyValue(request)
                    .retrieve()
                    .toBodilessEntity()
                    .block();
            return null;
        });
    }

    public void settle(LedgerActionRequest request) {
        call(() -> {
            webClient
                    .post()
                    .uri("/api/v1/internal/ledger/settle")
                    .bodyValue(request)
                    .retrieve()
                    .toBodilessEntity()
                    .block();
            return null;
        });
    }

    private <T> T call(Supplier<T> supplier) {
        try {
            return circuitBreaker.executeSupplier(supplier);
        } catch (CallNotPermittedException ex) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE, ResilienceConfig.DOWNSTREAM + " circuit open");
        }
    }

    public record ReserveRequest(Long userId, Long orderId, BigDecimal amount, String idempotencyKey) {
    }

    public record ReservationResponse(boolean accepted, String reason) {
    }

    public record LedgerActionRequest(Long userId, Long orderId, String idempotencyKey, BigDecimal amount) {
    }
}