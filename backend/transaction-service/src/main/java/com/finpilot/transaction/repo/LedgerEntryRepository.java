package com.finpilot.transaction.repo;

import com.finpilot.transaction.domain.LedgerEntryEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LedgerEntryRepository extends JpaRepository<LedgerEntryEntity, Long> {
    List<LedgerEntryEntity> findByTransactionId(Long transactionId);
}