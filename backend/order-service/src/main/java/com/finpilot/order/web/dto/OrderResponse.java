package com.finpilot.order.web.dto;

import com.finpilot.order.domain.OrderEntity;

import java.math.BigDecimal;
import java.time.Instant;

public record OrderResponse(
        Long id,
        Long userId,
        Long portfolioId,
        Long assetId,
        String side,
        BigDecimal quantity,
        BigDecimal limitPrice,
        String status,
        String rejectionReason,
        String idempotencyKey,
        Instant createdAt,
        Instant updatedAt) {

    public static OrderResponse from(OrderEntity order) {
        return new OrderResponse(
                order.getId(),
                order.getUserId(),
                order.getPortfolioId(),
                order.getAssetId(),
                order.getSide(),
                order.getQuantity(),
                order.getLimitPrice(),
                order.getStatus(),
                order.getRejectionReason(),
                order.getIdempotencyKey(),
                order.getCreatedAt(),
                order.getUpdatedAt());
    }
}