package com.finpilot.transaction.job;

import com.finpilot.transaction.service.ReconciliationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ReconciliationJob {
    private static final Logger log = LoggerFactory.getLogger(ReconciliationJob.class);
    private final ReconciliationService reconciliationService;

    public ReconciliationJob(ReconciliationService reconciliationService) {
        this.reconciliationService = reconciliationService;
    }

    @Scheduled(fixedDelayString = "${finpilot.reconciliation.interval-ms:900000}")
    public void scheduledReconcile() {
        var report = reconciliationService.run();
        if (!report.healthy()) {
            log.warn("Reconciliation found {} issue(s): {}", report.issueCount(), report.issues());
        } else {
            log.debug(
                    "Reconciliation OK — {} accounts, debits={} credits={}",
                    report.accountsChecked(),
                    report.totalDebits(),
                    report.totalCredits());
        }
    }
}