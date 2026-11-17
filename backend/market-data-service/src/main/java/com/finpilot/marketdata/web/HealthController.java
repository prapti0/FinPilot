package com.finpilot.marketdata.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class HealthController {
    @GetMapping("/api/v1/internal/health")
    public Map<String, String> health() {
        return Map.of("service", "market-data-service", "status", "UP");
    }
}