package com.finpilot.marketdata.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

@Component
public class MarketDataSeeder implements ApplicationRunner {
    private static final Map<String, BigDecimal> SEED_PRICES = Map.of(
            "RELIANCE", new BigDecimal("2850.5000"),
            "HDFCBANK", new BigDecimal("1680.2500"),
            "NIFTYBEES", new BigDecimal("245.7500"));

    private final JdbcTemplate jdbcTemplate;

    public MarketDataSeeder(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        Instant now = Instant.now();
        for (var entry : SEED_PRICES.entrySet()) {
            String symbol = entry.getKey();
            Long assetId = jdbcTemplate.query(
                            "SELECT id FROM assets WHERE symbol = ?",
                            (rs, rowNum) -> rs.getLong("id"),
                            symbol)
                    .stream()
                    .findFirst()
                    .orElse(null);
            if (assetId == null) {
                continue;
            }
            Boolean hasPrices = jdbcTemplate.queryForObject(
                    "SELECT EXISTS (SELECT 1 FROM market_prices WHERE asset_id = ?)", Boolean.class, assetId);
            if (Boolean.TRUE.equals(hasPrices)) {
                continue;
            }
            jdbcTemplate.update(
                    "INSERT INTO market_prices (asset_id, price, recorded_at) VALUES (?, ?, ?)",
                    assetId,
                    entry.getValue(),
                    java.sql.Timestamp.from(now));
        }
    }
}