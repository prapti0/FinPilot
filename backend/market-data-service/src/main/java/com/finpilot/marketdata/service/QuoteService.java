package com.finpilot.marketdata.service;

import com.finpilot.marketdata.domain.MarketPriceEntity;
import com.finpilot.marketdata.repo.MarketPriceRepository;
import com.finpilot.marketdata.web.dto.PriceHistoryItem;
import com.finpilot.marketdata.web.dto.QuoteResponse;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class QuoteService {
    private final JdbcTemplate jdbcTemplate;
    private final MarketPriceRepository marketPriceRepository;

    public QuoteService(JdbcTemplate jdbcTemplate, MarketPriceRepository marketPriceRepository) {
        this.jdbcTemplate = jdbcTemplate;
        this.marketPriceRepository = marketPriceRepository;
    }

    @Cacheable(cacheNames = "quotes", key = "#symbol.toUpperCase()")
    @Transactional(readOnly = true)
    public QuoteResponse latestQuote(String symbol) {
        String normalized = symbol.toUpperCase();
        Long assetId = resolveAssetId(normalized);
        MarketPriceEntity latest = marketPriceRepository.findRecentByAssetId(assetId, 1).stream()
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No quote"));
        return new QuoteResponse(normalized, latest.getPrice(), latest.getRecordedAt());
    }

    @Transactional(readOnly = true)
    public List<PriceHistoryItem> history(String symbol) {
        String normalized = symbol.toUpperCase();
        Long assetId = resolveAssetId(normalized);
        return marketPriceRepository.findRecentByAssetId(assetId, 10).stream()
                .map(p -> new PriceHistoryItem(p.getPrice(), p.getRecordedAt()))
                .toList();
    }

    private Long resolveAssetId(String symbol) {
        return jdbcTemplate
                .query(
                        "SELECT id FROM assets WHERE symbol = ?",
                        (rs, rowNum) -> rs.getLong("id"),
                        symbol)
                .stream()
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Unknown symbol"));
    }
}