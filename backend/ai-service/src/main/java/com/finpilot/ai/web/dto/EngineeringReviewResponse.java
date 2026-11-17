package com.finpilot.ai.web.dto;

import java.util.List;

public record EngineeringReviewResponse(
        String prTitle, String repository, String summary, List<Finding> findings) {
    public record Finding(String severity, String title, String detail) {
    }
}