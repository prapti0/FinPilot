package com.finpilot.common.events;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record OrderCreatedPayload(
        @NotNull Long orderId,
        @NotNull Long userId,
        @NotNull Long portfolioId,
        @NotNull Long assetId,
        @NotBlank String symbol,
        @NotBlank String side,
        @NotNull BigDecimal quantity,
        BigDecimal limitPrice,
        @NotNull BigDecimal estimatedValue,
        @NotBlank String idempotencyKey
) {
}