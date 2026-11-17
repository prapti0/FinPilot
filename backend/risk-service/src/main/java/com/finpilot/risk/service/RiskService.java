package com.finpilot.risk.service;

import com.finpilot.risk.client.PortfolioClient;
import com.finpilot.risk.domain.RiskAlertEntity;
import com.finpilot.risk.repo.RiskAlertRepository;
import com.finpilot.risk.web.dto.*;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

@Service
public class RiskService {
    private static final BigDecimal UNUSUAL_ORDER_THRESHOLD = new BigDecimal("20000");

    private final RiskAlertRepository riskAlertRepository;
    private final PortfolioClient portfolioClient;
    private final JdbcTemplate jdbcTemplate;

    public RiskService(
            RiskAlertRepository riskAlertRepository, PortfolioClient portfolioClient, JdbcTemplate jdbcTemplate) {
        this.riskAlertRepository = riskAlertRepository;
        this.portfolioClient = portfolioClient;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional
    public OrderRiskCheckResponse checkOrder(OrderRiskCheckRequest request) {
        List<String> flags = new ArrayList<>();
        if (request.orderValue() != null
                && request.orderValue().compareTo(UNUSUAL_ORDER_THRESHOLD) > 0) {
            flags.add("UNUSUAL_ORDER_VALUE");
        }
        String sector = jdbcTemplate.query(
                        "SELECT sector FROM assets WHERE symbol = ?",
                        (rs, rowNum) -> rs.getString("sector"),
                        request.symbol().toUpperCase())
                .stream()
                .findFirst()
                .orElse("Unknown");
        BigDecimal concentration = estimateSectorConcentration(request.userId(), sector, request.orderValue());
        if (concentration.compareTo(new BigDecimal("0.45")) > 0) {
            flags.add("SECTOR_CONCENTRATION");
        }
        if (!flags.isEmpty()) {
            persistAlert(
                    request.userId(),
                    "ORDER_CHECK",
                    "WARN",
                    Map.of(
                            "symbol", request.symbol(),
                            "side", request.side(),
                            "flags", flags,
                            "orderValue", request.orderValue()));
        }
        // Demo platform allows trading but records warnings; block only on extreme concentration.
        boolean approved = !flags.contains("SECTOR_CONCENTRATION")
                || request.orderValue().compareTo(new BigDecimal("50000")) <= 0;
        String reason = approved ? null : "SECTOR_CONCENTRATION_LIMIT";
        return new OrderRiskCheckResponse(approved, reason, flags);
    }

    @Transactional(readOnly = true)
    public PortfolioRiskResponse portfolioRisk(Long portfolioId) {
        PortfolioClient.PortfolioRiskSummary summary = portfolioClient.fetchRiskSummary(portfolioId);
        if (summary == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Portfolio not found");
        }
        Map<String, BigDecimal> sectorTotals = new HashMap<>();
        for (PortfolioClient.PortfolioRiskSummary.HoldingLine line : summary.holdings()) {
            String sector = line.sector() != null ? line.sector() : "Unknown";
            sectorTotals.merge(sector, line.marketValue(), BigDecimal::add);
        }
        BigDecimal total = summary.totalValue() != null ? summary.totalValue() : BigDecimal.ONE;
        Map<String, BigDecimal> weights = new LinkedHashMap<>();
        sectorTotals.forEach((sector, value) ->
                weights.put(sector, value.divide(total.max(BigDecimal.ONE), 4, RoundingMode.HALF_UP)));
        return new PortfolioRiskResponse(portfolioId, summary.userId(), total, weights);
    }

    @Transactional(readOnly = true)
    public List<RiskAlertResponse> alertsForUser(Long userId) {
        return riskAlertRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(RiskAlertResponse::from)
                .toList();
    }

    @Transactional
    public void handleOrderCreated(Long userId, String symbol, BigDecimal orderValue, Long orderId) {
        if (orderValue != null && orderValue.compareTo(UNUSUAL_ORDER_THRESHOLD) > 0) {
            persistAlert(
                    userId,
                    "ORDER_CREATED_ASYNC",
                    "INFO",
                    Map.of("symbol", symbol, "orderId", orderId, "orderValue", orderValue));
        }
    }

    private BigDecimal estimateSectorConcentration(Long userId, String sector, BigDecimal orderValue) {
        BigDecimal baseline = new BigDecimal("0.15");
        if (orderValue == null) {
            return baseline;
        }
        BigDecimal incremental =
                orderValue.divide(new BigDecimal("100000"), 4, RoundingMode.HALF_UP);
        return baseline.add(incremental).min(new BigDecimal("0.9"));
    }

    private void persistAlert(Long userId, String type, String severity, Map<String, Object> payload) {
        RiskAlertEntity alert = new RiskAlertEntity();
        alert.setUserId(userId);
        alert.setAlertType(type);
        alert.setSeverity(severity);
        alert.setPayload(payload);
        riskAlertRepository.save(alert);
    }
}