package com.finpilot.risk.web.dto;

import java.math.BigDecimal;
import java.util.Map;

public record PortfolioRiskResponse(
        Long portfolioId, Long userId, BigDecimal totalValue, Map<String, BigDecimal> sectorWeights) {
}