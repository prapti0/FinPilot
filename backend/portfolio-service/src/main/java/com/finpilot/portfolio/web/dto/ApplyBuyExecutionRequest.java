package com.finpilot.portfolio.web.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record ApplyBuyExecutionRequest(
        @NotNull Long assetId,
        @NotNull @Positive BigDecimal quantity,
        @NotNull @Positive BigDecimal executionPrice) {
}