package com.finpilot.portfolio.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class HealthController {
    @GetMapping("/api/v1/internal/health")
    public Map<String, String> health() {
        return Map.of("service", "portfolio-service", "status", "UP");
    }
}