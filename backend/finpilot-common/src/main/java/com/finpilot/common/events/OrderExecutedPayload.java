package com.finpilot.common.events;

import java.math.BigDecimal;
import java.time.Instant;

public record OrderExecutedPayload(
        Long orderId,
        Long userId,
        Long portfolioId,
        Long assetId,
        String symbol,
        String side,
        BigDecimal executionPrice,
        BigDecimal executedQuantity,
        Instant executedAt
) {
}