package com.finpilot.portfolio.repo;

import com.finpilot.portfolio.domain.HoldingEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface HoldingRepository extends JpaRepository<HoldingEntity, Long> {
    List<HoldingEntity> findByPortfolioId(Long portfolioId);

    Optional<HoldingEntity> findByPortfolioIdAndAssetId(Long portfolioId, Long assetId);
}