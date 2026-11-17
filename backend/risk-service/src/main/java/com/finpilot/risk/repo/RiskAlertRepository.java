package com.finpilot.risk.repo;

import com.finpilot.risk.domain.RiskAlertEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RiskAlertRepository extends JpaRepository<RiskAlertEntity, Long> {
    List<RiskAlertEntity> findByUserIdOrderByCreatedAtDesc(Long userId);
}