package com.finpilot.portfolio.web;

import com.finpilot.portfolio.service.PortfolioService;
import com.finpilot.portfolio.web.dto.InternalRiskSummaryResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/internal/portfolios")
public class InternalPortfolioController {
    private final PortfolioService portfolioService;

    public InternalPortfolioController(PortfolioService portfolioService) {
        this.portfolioService = portfolioService;
    }

    @GetMapping("/{id}/risk-summary")
    public InternalRiskSummaryResponse riskSummary(@PathVariable Long id) {
        return portfolioService.riskSummary(id);
    }
}