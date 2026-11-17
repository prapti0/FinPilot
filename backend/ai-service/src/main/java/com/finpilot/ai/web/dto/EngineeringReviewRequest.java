package com.finpilot.ai.web.dto;

import jakarta.validation.constraints.NotBlank;

public record EngineeringReviewRequest(
        @NotBlank String prTitle, @NotBlank String diff, @NotBlank String repository) {
}