package com.finpilot.portfolio.service;

import com.finpilot.portfolio.domain.AssetEntity;
import com.finpilot.portfolio.domain.HoldingEntity;
import com.finpilot.portfolio.domain.OutboxEventEntity;
import com.finpilot.portfolio.domain.PortfolioEntity;
import com.finpilot.portfolio.repo.AssetRepository;
import com.finpilot.portfolio.repo.HoldingRepository;
import com.finpilot.portfolio.repo.OutboxEventRepository;
import com.finpilot.portfolio.repo.PortfolioRepository;
import com.finpilot.portfolio.web.dto.*;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.*;

@Service
public class PortfolioService {
    private final PortfolioRepository portfolioRepository;
    private final HoldingRepository holdingRepository;
    private final AssetRepository assetRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final JdbcTemplate jdbcTemplate;

    public PortfolioService(
            PortfolioRepository portfolioRepository,
            HoldingRepository holdingRepository,
            AssetRepository assetRepository,
            OutboxEventRepository outboxEventRepository,
            JdbcTemplate jdbcTemplate) {
        this.portfolioRepository = portfolioRepository;
        this.holdingRepository = holdingRepository;
        this.assetRepository = assetRepository;
        this.outboxEventRepository = outboxEventRepository;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional(readOnly = true)
    public List<PortfolioSummaryResponse> listForUser(Long userId) {
        return portfolioRepository.findByUserIdOrderByCreatedAtAsc(userId).stream()
                .map(p -> summary(p.getId(), userId))
                .toList();
    }

    @Transactional(readOnly = true)
    public PortfolioDetailResponse getPortfolio(Long userId, Long portfolioId) {
        PortfolioEntity portfolio = requireOwned(portfolioId, userId);
        return new PortfolioDetailResponse(
                portfolio.getId(),
                portfolio.getName(),
                portfolio.getCreatedAt(),
                summary(portfolioId, userId));
    }

    @Cacheable(cacheNames = "portfolioSummary", key = "#portfolioId")
    @Transactional(readOnly = true)
    public PortfolioSummaryResponse summary(Long portfolioId, Long userId) {
        requireOwned(portfolioId, userId);
        List<HoldingEntity> holdings = holdingRepository.findByPortfolioId(portfolioId);
        BigDecimal invested = BigDecimal.ZERO;
        Map<String, BigDecimal> sectorWeights = new HashMap<>();
        for (HoldingEntity holding : holdings) {
            AssetEntity asset =
                    assetRepository.findById(holding.getAssetId()).orElse(null);
            BigDecimal lineValue = holding.getQuantity().multiply(holding.getAvgPrice());
            invested = invested.add(lineValue);
            if (asset != null && asset.getSector() != null) {
                sectorWeights.merge(asset.getSector(), lineValue, BigDecimal::add);
            }
        }
        return new PortfolioSummaryResponse(portfolioId, holdings.size(), invested.setScale(4, RoundingMode.HALF_UP), sectorWeights);
    }

    @Transactional(readOnly = true)
    public List<HoldingResponse> holdings(Long userId, Long portfolioId) {
        requireOwned(portfolioId, userId);
        return holdingRepository.findByPortfolioId(portfolioId).stream()
                .map(this::toHoldingResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public PerformanceResponse performance(Long userId, Long portfolioId) {
        requireOwned(portfolioId, userId);
        List<HoldingEntity> holdings = holdingRepository.findByPortfolioId(portfolioId);
        BigDecimal cost = BigDecimal.ZERO;
        BigDecimal marked = BigDecimal.ZERO;
        for (HoldingEntity holding : holdings) {
            cost = cost.add(holding.getQuantity().multiply(holding.getAvgPrice()));
            BigDecimal lastPrice = lastMarketPrice(holding.getAssetId());
            if (lastPrice != null) {
                marked = marked.add(holding.getQuantity().multiply(lastPrice));
            } else {
                marked = marked.add(holding.getQuantity().multiply(holding.getAvgPrice()));
            }
        }
        BigDecimal pnl = marked.subtract(cost);
        return new PerformanceResponse(
                portfolioId,
                cost.setScale(4, RoundingMode.HALF_UP),
                marked.setScale(4, RoundingMode.HALF_UP),
                pnl.setScale(4, RoundingMode.HALF_UP),
                Instant.now());
    }

    @Transactional(readOnly = true)
    public InternalRiskSummaryResponse riskSummary(Long portfolioId) {
        PortfolioEntity portfolio = portfolioRepository
                .findById(portfolioId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        List<HoldingEntity> holdings = holdingRepository.findByPortfolioId(portfolioId);
        BigDecimal total = BigDecimal.ZERO;
        List<InternalRiskSummaryResponse.HoldingLine> lines = new ArrayList<>();
        for (HoldingEntity holding : holdings) {
            AssetEntity asset = assetRepository.findById(holding.getAssetId()).orElseThrow();
            BigDecimal lastPrice = Optional.ofNullable(lastMarketPrice(holding.getAssetId()))
                    .orElse(holding.getAvgPrice());
            BigDecimal value = holding.getQuantity().multiply(lastPrice);
            total = total.add(value);
            lines.add(new InternalRiskSummaryResponse.HoldingLine(
                    asset.getSymbol(), asset.getSector(), holding.getQuantity(), value));
        }
        return new InternalRiskSummaryResponse(portfolio.getUserId(), portfolioId, total, lines);
    }

    @CacheEvict(cacheNames = "portfolioSummary", key = "#portfolioId")
    @Transactional
    public void applyBuyExecution(Long portfolioId, Long assetId, BigDecimal quantity, BigDecimal executionPrice) {
        HoldingEntity holding = holdingRepository
                .findByPortfolioIdAndAssetId(portfolioId, assetId)
                .orElseGet(() -> {
                    HoldingEntity created = new HoldingEntity();
                    created.setPortfolioId(portfolioId);
                    created.setAssetId(assetId);
                    created.setQuantity(BigDecimal.ZERO);
                    created.setAvgPrice(BigDecimal.ZERO);
                    return created;
                });
        BigDecimal oldQty = holding.getQuantity();
        BigDecimal newQty = oldQty.add(quantity);
        BigDecimal newAvg;
        if (oldQty.compareTo(BigDecimal.ZERO) == 0) {
            newAvg = executionPrice;
        } else {
            BigDecimal oldCost = oldQty.multiply(holding.getAvgPrice());
            BigDecimal addCost = quantity.multiply(executionPrice);
            newAvg = oldCost.add(addCost).divide(newQty, 4, RoundingMode.HALF_UP);
        }
        holding.setQuantity(newQty);
        holding.setAvgPrice(newAvg);
        holdingRepository.save(holding);
        enqueuePortfolioUpdated(portfolioId, assetId, "BUY_FILL");
    }

    private void enqueuePortfolioUpdated(Long portfolioId, Long assetId, String reason) {
        OutboxEventEntity outbox = new OutboxEventEntity();
        outbox.setAggregateType("PORTFOLIO");
        outbox.setAggregateId(String.valueOf(portfolioId));
        outbox.setEventType("PortfolioUpdated");
        Map<String, Object> payload = new HashMap<>();
        payload.put("kafkaTopic", "finpilot.portfolio-updated");
        payload.put("payload", Map.of(
                "portfolioId", portfolioId,
                "assetId", assetId,
                "reason", reason,
                "occurredAt", Instant.now().toString()));
        outbox.setPayload(payload);
        outboxEventRepository.save(outbox);
    }

    private PortfolioEntity requireOwned(Long portfolioId, Long userId) {
        return portfolioRepository
                .findByIdAndUserId(portfolioId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    private HoldingResponse toHoldingResponse(HoldingEntity holding) {
        AssetEntity asset = assetRepository.findById(holding.getAssetId()).orElseThrow();
        return new HoldingResponse(
                holding.getId(),
                asset.getSymbol(),
                asset.getName(),
                asset.getSector(),
                holding.getQuantity(),
                holding.getAvgPrice(),
                holding.getUpdatedAt());
    }

    private BigDecimal lastMarketPrice(Long assetId) {
        return jdbcTemplate.query(
                        """
                                SELECT price FROM market_prices
                                WHERE asset_id = ?
                                ORDER BY recorded_at DESC
                                LIMIT 1
                                """,
                        (rs, rowNum) -> rs.getBigDecimal("price"),
                        assetId)
                .stream()
                .findFirst()
                .orElse(null);
    }
}