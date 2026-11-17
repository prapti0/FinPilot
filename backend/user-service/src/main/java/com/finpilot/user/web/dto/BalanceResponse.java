package com.finpilot.user.web.dto;

import java.math.BigDecimal;

public record BalanceResponse(
        BigDecimal available, BigDecimal reserved, BigDecimal totalCash) {
}