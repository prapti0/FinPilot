package com.finpilot.user.repo;

import com.finpilot.user.domain.AccountEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AccountRepository extends JpaRepository<AccountEntity, Long> {
    Optional<AccountEntity> findByUserIdAndAccountTypeAndCurrency(Long userId, String accountType, String currency);
}