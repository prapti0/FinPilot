package com.finpilot.portfolio.repo;

import com.finpilot.portfolio.domain.PortfolioEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PortfolioRepository extends JpaRepository<PortfolioEntity, Long> {
    List<PortfolioEntity> findByUserIdOrderByCreatedAtAsc(Long userId);

    Optional<PortfolioEntity> findByIdAndUserId(Long id, Long userId);

    Optional<PortfolioEntity> findByUserIdAndName(Long userId, String name);
}