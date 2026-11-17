package com.finpilot.transaction.repo;

import com.finpilot.transaction.domain.AccountEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface AccountRepository extends JpaRepository<AccountEntity, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from AccountEntity a where a.userId = :userId and a.accountType = :type")
    Optional<AccountEntity> findCashForUpdate(@Param("userId") Long userId, @Param("type") String type);

    Optional<AccountEntity> findByUserIdAndAccountType(Long userId, String accountType);
}