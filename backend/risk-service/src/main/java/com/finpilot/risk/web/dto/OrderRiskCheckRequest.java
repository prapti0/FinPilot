package com.finpilot.risk.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record OrderRiskCheckRequest(
        @NotNull Long userId,
        @NotBlank String symbol,
        @NotBlank String side,
        @NotNull BigDecimal quantity,
        @NotNull BigDecimal price,
        @NotNull BigDecimal orderValue) {
}