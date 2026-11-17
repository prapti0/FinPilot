package com.finpilot.ai.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AnalyzePortfolioRequest(@NotNull Long portfolioId, @NotBlank String question) {
}