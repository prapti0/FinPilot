package com.finpilot.ai.repo;

import com.finpilot.ai.domain.AiReportEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AiReportRepository extends JpaRepository<AiReportEntity, Long> {
}