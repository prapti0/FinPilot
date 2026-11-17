package com.finpilot.portfolio.web.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record PerformanceResponse(
        Long portfolioId, BigDecimal costBasis, BigDecimal marketValue, BigDecimal unrealizedPnl, Instant asOf) {
}