package com.finpilot.order.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record CreateOrderRequest(
        @NotNull Long portfolioId,
        @NotBlank String symbol,
        @NotBlank String side,
        @NotNull @Positive BigDecimal quantity,
        BigDecimal limitPrice) {
}