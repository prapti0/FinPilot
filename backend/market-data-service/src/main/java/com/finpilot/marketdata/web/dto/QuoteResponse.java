package com.finpilot.marketdata.web.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record QuoteResponse(String symbol, BigDecimal price, Instant recordedAt) {
}