package com.finpilot.transaction.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record LedgerActionRequest(
        @NotNull Long userId,
        @NotNull Long orderId,
        @NotNull @Positive BigDecimal amount,
        @NotBlank String idempotencyKey) {
}