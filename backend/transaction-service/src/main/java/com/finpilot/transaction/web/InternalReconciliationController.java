package com.finpilot.transaction.web;

import com.finpilot.transaction.service.ReconciliationService;
import com.finpilot.transaction.web.dto.ReconciliationReport;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/internal/reconciliation")
public class InternalReconciliationController {
    private final ReconciliationService reconciliationService;

    public InternalReconciliationController(ReconciliationService reconciliationService) {
        this.reconciliationService = reconciliationService;
    }

    @PostMapping("/run")
    public ReconciliationReport runNow() {
        return reconciliationService.run();
    }

    @GetMapping("/last")
    public ReconciliationReport last() {
        ReconciliationReport report = reconciliationService.lastReport();
        if (report == null) {
            return reconciliationService.run();
        }
        return report;
    }
}