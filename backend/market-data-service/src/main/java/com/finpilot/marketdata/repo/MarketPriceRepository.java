package com.finpilot.marketdata.repo;

import com.finpilot.marketdata.domain.MarketPriceEntity;
import com.finpilot.marketdata.domain.MarketPriceId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface MarketPriceRepository extends JpaRepository<MarketPriceEntity, MarketPriceId> {
    @Query(
            value = """
                    SELECT * FROM market_prices WHERE asset_id = :assetId
                    ORDER BY recorded_at DESC LIMIT :limit
                    """,
            nativeQuery = true)
    List<MarketPriceEntity> findRecentByAssetId(@Param("assetId") Long assetId, @Param("limit") int limit);

    @Query(
            value = "SELECT COUNT(*) > 0 FROM market_prices WHERE asset_id = :assetId",
            nativeQuery = true)
    boolean existsForAsset(Long assetId);
}