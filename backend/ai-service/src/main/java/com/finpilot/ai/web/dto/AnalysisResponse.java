package com.finpilot.ai.web.dto;

import java.util.Map;

public record AnalysisResponse(String reportType, String narrative, Map<String, Object> metadata) {
}