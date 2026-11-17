package com.finpilot.marketdata.domain;

import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;

public class MarketPriceId implements Serializable {
    private Long assetId;
    private Instant recordedAt;

    public MarketPriceId() {
    }

    public MarketPriceId(Long assetId, Instant recordedAt) {
        this.assetId = assetId;
        this.recordedAt = recordedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof MarketPriceId that)) {
            return false;
        }
        return Objects.equals(assetId, that.assetId) && Objects.equals(recordedAt, that.recordedAt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(assetId, recordedAt);
    }
}