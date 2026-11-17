package com.finpilot.ai.web.dto;

import jakarta.validation.constraints.NotNull;

public record ExplainRiskRequest(@NotNull Long portfolioId) {
}