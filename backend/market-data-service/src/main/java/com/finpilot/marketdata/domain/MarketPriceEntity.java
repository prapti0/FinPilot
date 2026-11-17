package com.finpilot.marketdata.domain;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "market_prices")
@IdClass(MarketPriceId.class)
public class MarketPriceEntity {
    @Id
    @Column(name = "asset_id")
    private Long assetId;

    @Id
    @Column(name = "recorded_at")
    private Instant recordedAt;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal price;

    public Long getAssetId() {
        return assetId;
    }

    public void setAssetId(Long assetId) {
        this.assetId = assetId;
    }

    public Instant getRecordedAt() {
        return recordedAt;
    }

    public void setRecordedAt(Instant recordedAt) {
        this.recordedAt = recordedAt;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }
}