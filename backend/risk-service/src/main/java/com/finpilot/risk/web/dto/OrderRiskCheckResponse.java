package com.finpilot.risk.web.dto;

import java.util.List;

public record OrderRiskCheckResponse(boolean approved, String reason, List<String> flags) {
}