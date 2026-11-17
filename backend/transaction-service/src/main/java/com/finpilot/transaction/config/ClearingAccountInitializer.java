package com.finpilot.transaction.config;

import com.finpilot.transaction.domain.AccountEntity;
import com.finpilot.transaction.repo.AccountRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class ClearingAccountInitializer implements ApplicationRunner {
    private final JdbcTemplate jdbcTemplate;
    private final AccountRepository accountRepository;

    public ClearingAccountInitializer(JdbcTemplate jdbcTemplate, AccountRepository accountRepository) {
        this.jdbcTemplate = jdbcTemplate;
        this.accountRepository = accountRepository;
    }

    @Override
    public void run(ApplicationArguments args) {
        jdbcTemplate.query("select id from users", rs -> {
            long userId = rs.getLong("id");
            ensure(userId, "CLEARING");
            ensure(userId, "INVESTMENT");
        });
    }

    private void ensure(Long userId, String type) {
        if (accountRepository.findByUserIdAndAccountType(userId, type).isPresent()) {
            return;
        }
        AccountEntity a = new AccountEntity();
        a.setUserId(userId);
        a.setAccountType(type);
        a.setAvailableBalance(BigDecimal.ZERO);
        a.setReservedBalance(BigDecimal.ZERO);
        accountRepository.save(a);
    }
}