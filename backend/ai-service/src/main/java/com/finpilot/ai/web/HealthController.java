package com.finpilot.ai.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class HealthController {
    @GetMapping("/api/v1/internal/health")
    public Map<String, String> health() {
        return Map.of("service", "ai-service", "status", "UP", "note", "LLM does not mutate ledger");
    }
}