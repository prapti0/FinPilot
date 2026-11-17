package com.finpilot.portfolio.web.dto;

import java.math.BigDecimal;
import java.util.Map;

public record PortfolioSummaryResponse(
        Long portfolioId, int holdingCount, BigDecimal investedValue, Map<String, BigDecimal> sectorWeights) {
}