package com.finpilot.transaction.web.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record ReconciliationReport(
        Instant ranAt,
        boolean healthy,
        BigDecimal totalDebits,
        BigDecimal totalCredits,
        int accountsChecked,
        int issueCount,
        List<String> issues) {}