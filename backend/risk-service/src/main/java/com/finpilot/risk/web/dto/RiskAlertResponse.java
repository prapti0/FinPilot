package com.finpilot.risk.web.dto;

import com.finpilot.risk.domain.RiskAlertEntity;

import java.time.Instant;
import java.util.Map;

public record RiskAlertResponse(
        Long id, String alertType, String severity, Map<String, Object> payload, Instant createdAt) {
    public static RiskAlertResponse from(RiskAlertEntity entity) {
        return new RiskAlertResponse(
                entity.getId(),
                entity.getAlertType(),
                entity.getSeverity(),
                entity.getPayload(),
                entity.getCreatedAt());
    }
}