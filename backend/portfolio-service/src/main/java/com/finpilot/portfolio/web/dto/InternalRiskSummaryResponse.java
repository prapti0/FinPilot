package com.finpilot.portfolio.web.dto;

import java.math.BigDecimal;
import java.util.List;

public record InternalRiskSummaryResponse(
        Long userId, Long portfolioId, BigDecimal totalValue, List<HoldingLine> holdings) {
    public record HoldingLine(String symbol, String sector, BigDecimal quantity, BigDecimal marketValue) {
    }
}