package com.finpilot.order.repo;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Component;

import java.sql.PreparedStatement;
import java.sql.Types;

@Component
public class OrderPersistHelper {
    private final JdbcTemplate jdbcTemplate;

    public OrderPersistHelper(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public long insertOrder(
            long userId,
            long portfolioId,
            long assetId,
            String side,
            java.math.BigDecimal quantity,
            java.math.BigDecimal limitPrice,
            String status,
            String idempotencyKey) {
        var keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(
                con -> {
                    PreparedStatement ps = con.prepareStatement(
                            """
                    INSERT INTO orders (user_id, portfolio_id, asset_id, side, quantity, limit_price, status, idempotency_key)
                    VALUES (?, ?, ?, ?::order_side, ?, ?, ?::order_status, ?)
                    """,
                            new String[] {"id"});
                    ps.setLong(1, userId);
                    ps.setLong(2, portfolioId);
                    ps.setLong(3, assetId);
                    ps.setString(4, side);
                    ps.setBigDecimal(5, quantity);
                    if (limitPrice != null) {
                        ps.setBigDecimal(6, limitPrice);
                    } else {
                        ps.setNull(6, Types.NUMERIC);
                    }
                    ps.setString(7, status);
                    ps.setString(8, idempotencyKey);
                    return ps;
                },
                keyHolder);
        Number key = keyHolder.getKey();
        if (key == null) {
            throw new IllegalStateException("Order insert did not return id");
        }
        return key.longValue();
    }

    public void updateStatus(long orderId, String status, String rejectionReason) {
        jdbcTemplate.update(
                "UPDATE orders SET status = ?::order_status, rejection_reason = ?, updated_at = NOW() WHERE id = ?",
                status,
                rejectionReason,
                orderId);
    }
}