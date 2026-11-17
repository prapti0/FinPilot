package com.finpilot.portfolio.web.dto;

import java.time.Instant;

public record PortfolioDetailResponse(
        Long id, String name, Instant createdAt, PortfolioSummaryResponse summary) {
}