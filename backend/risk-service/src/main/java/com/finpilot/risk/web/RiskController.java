package com.finpilot.risk.web;

import com.finpilot.common.web.UserIdResolver;
import com.finpilot.risk.service.RiskService;
import com.finpilot.risk.web.dto.PortfolioRiskResponse;
import com.finpilot.risk.web.dto.RiskAlertResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/risk")
public class RiskController {
    private final RiskService riskService;

    public RiskController(RiskService riskService) {
        this.riskService = riskService;
    }

    @GetMapping("/portfolio/{portfolioId}")
    public PortfolioRiskResponse portfolio(@PathVariable Long portfolioId) {
        return riskService.portfolioRisk(portfolioId);
    }

    @GetMapping("/alerts")
    public List<RiskAlertResponse> alerts(HttpServletRequest request) {
        return riskService.alertsForUser(UserIdResolver.requireUserId(request));
    }
}