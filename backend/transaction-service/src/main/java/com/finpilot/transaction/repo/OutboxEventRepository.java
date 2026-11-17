package com.finpilot.transaction.repo;

import com.finpilot.transaction.domain.OutboxEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface OutboxEventRepository extends JpaRepository<OutboxEventEntity, Long> {
    @Query("select o from OutboxEventEntity o where o.publishedAt is null order by o.id")
    List<OutboxEventEntity> findUnpublished();
}