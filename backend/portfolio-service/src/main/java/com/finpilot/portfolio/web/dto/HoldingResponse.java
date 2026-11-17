package com.finpilot.portfolio.web.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record HoldingResponse(
        Long id,
        String symbol,
        String name,
        String sector,
        BigDecimal quantity,
        BigDecimal avgPrice,
        Instant updatedAt) {
}