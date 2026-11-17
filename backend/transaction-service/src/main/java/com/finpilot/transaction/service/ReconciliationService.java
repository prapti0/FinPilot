package com.finpilot.transaction.service;

import com.finpilot.transaction.web.dto.ReconciliationReport;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class ReconciliationService {
    private final JdbcTemplate jdbcTemplate;
    private volatile ReconciliationReport lastReport;

    public ReconciliationService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public ReconciliationReport lastReport() {
        return lastReport;
    }

    public ReconciliationReport run() {
        List<String> issues = new ArrayList<>();

        Map<String, Object> totals = jdbcTemplate.queryForMap(
                "SELECT COALESCE(SUM(debit), 0) AS debits, COALESCE(SUM(credit), 0) AS credits FROM ledger_entries");
        BigDecimal debits = toBigDecimal(totals.get("debits"));
        BigDecimal credits = toBigDecimal(totals.get("credits"));
        boolean ledgerBalanced = debits.compareTo(credits) == 0;
        if (!ledgerBalanced) {
            issues.add("Global ledger imbalance: debits=" + debits + " credits=" + credits);
        }

        List<Map<String, Object>> unbalancedTx = jdbcTemplate.queryForList(
                """
                SELECT transaction_id,
                       COALESCE(SUM(debit), 0) AS debits,
                       COALESCE(SUM(credit), 0) AS credits
                  FROM ledger_entries
                 GROUP BY transaction_id
                HAVING COALESCE(SUM(debit), 0) <> COALESCE(SUM(credit), 0)
                """);
        for (Map<String, Object> row : unbalancedTx) {
            issues.add("Transaction " + row.get("transaction_id") + " entries do not balance");
        }

        List<Map<String, Object>> badAccounts = jdbcTemplate.queryForList(
                """
                SELECT id, available_balance, reserved_balance
                  FROM accounts
                 WHERE available_balance < 0 OR reserved_balance < 0
                """);
        for (Map<String, Object> row : badAccounts) {
            issues.add("Account " + row.get("id") + " has negative balance fields");
        }

        int accountsChecked = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM accounts", Integer.class);

        ReconciliationReport report = new ReconciliationReport(
                Instant.now(),
                ledgerBalanced && unbalancedTx.isEmpty() && badAccounts.isEmpty(),
                debits,
                credits,
                accountsChecked,
                unbalancedTx.size() + badAccounts.size(),
                issues);
        lastReport = report;
        return report;
    }

    private static BigDecimal toBigDecimal(Object value) {
        if (value == null) {
            return BigDecimal.ZERO;
        }
        if (value instanceof BigDecimal bd) {
            return bd;
        }
        return new BigDecimal(value.toString());
    }
}