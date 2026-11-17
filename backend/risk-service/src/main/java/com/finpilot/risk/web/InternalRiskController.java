package com.finpilot.risk.web;

import com.finpilot.risk.service.RiskService;
import com.finpilot.risk.web.dto.OrderRiskCheckRequest;
import com.finpilot.risk.web.dto.OrderRiskCheckResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/internal/risk")
public class InternalRiskController {
    private final RiskService riskService;

    public InternalRiskController(RiskService riskService) {
        this.riskService = riskService;
    }

    @PostMapping("/check-order")
    public OrderRiskCheckResponse check(@Valid @RequestBody OrderRiskCheckRequest request) {
        return riskService.checkOrder(request);
    }
}