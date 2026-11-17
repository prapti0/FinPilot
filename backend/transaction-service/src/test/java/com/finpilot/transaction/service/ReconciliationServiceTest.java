package com.finpilot.transaction.service;

import com.finpilot.transaction.web.dto.ReconciliationReport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;

import javax.sql.DataSource;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ReconciliationServiceTest {

    private ReconciliationService reconciliationService;

    @BeforeEach
    void setUp() {
        DataSource dataSource = new EmbeddedDatabaseBuilder()
                .setType(EmbeddedDatabaseType.H2)
                .build();
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        jdbc.execute(
                """
                CREATE TABLE ledger_entries (
                  transaction_id BIGINT,
                  debit DECIMAL(19,4),
                  credit DECIMAL(19,4)
                )
                """);
        jdbc.execute(
                """
                CREATE TABLE accounts (
                  id BIGINT PRIMARY KEY,
                  available_balance DECIMAL(19,4),
                  reserved_balance DECIMAL(19,4)
                )
                """);
        jdbc.update("INSERT INTO ledger_entries (transaction_id, debit, credit) VALUES (1, 100, 100)");
        jdbc.update("INSERT INTO accounts (id, available_balance, reserved_balance) VALUES (1, 50000, 0)");
        reconciliationService = new ReconciliationService(jdbc);
    }

    @Test
    void runWhenBalancedReturnsHealthy() {
        ReconciliationReport report = reconciliationService.run();
        assertTrue(report.healthy());
    }
}