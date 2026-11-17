package com.finpilot.marketdata.web.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record PriceHistoryItem(BigDecimal price, Instant recordedAt) {
}