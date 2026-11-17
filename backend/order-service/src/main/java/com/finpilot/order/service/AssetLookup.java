package com.finpilot.order.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class AssetLookup {
    private final JdbcTemplate jdbcTemplate;

    public AssetLookup(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Optional<AssetRow> findBySymbol(String symbol) {
        return jdbcTemplate.query(
                        """
                                SELECT id, symbol, name, sector FROM assets WHERE symbol = ?
                                """,
                        (rs, rowNum) -> new AssetRow(
                                rs.getLong("id"),
                                rs.getString("symbol"),
                                rs.getString("name"),
                                rs.getString("sector")),
                        symbol.toUpperCase())
                .stream()
                .findFirst();
    }

    public record AssetRow(Long id, String symbol, String name, String sector) {
    }
}