package com.finpilot.portfolio.web;

import com.finpilot.common.web.UserIdResolver;
import com.finpilot.portfolio.service.PortfolioService;
import com.finpilot.portfolio.web.dto.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/portfolios")
public class PortfolioController {
    private final PortfolioService portfolioService;

    public PortfolioController(PortfolioService portfolioService) {
        this.portfolioService = portfolioService;
    }

    @GetMapping
    public List<PortfolioSummaryResponse> list(HttpServletRequest request) {
        return portfolioService.listForUser(UserIdResolver.requireUserId(request));
    }

    @GetMapping("/{id}")
    public PortfolioDetailResponse get(HttpServletRequest request, @PathVariable Long id) {
        return portfolioService.getPortfolio(UserIdResolver.requireUserId(request), id);
    }

    @GetMapping("/{id}/holdings")
    public List<HoldingResponse> holdings(HttpServletRequest request, @PathVariable Long id) {
        return portfolioService.holdings(UserIdResolver.requireUserId(request), id);
    }

    @GetMapping("/{id}/performance")
    public PerformanceResponse performance(HttpServletRequest request, @PathVariable Long id) {
        return portfolioService.performance(UserIdResolver.requireUserId(request), id);
    }
}