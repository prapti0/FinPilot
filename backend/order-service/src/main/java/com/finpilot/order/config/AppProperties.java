package com.finpilot.order.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "finpilot.services")
public record AppProperties(String transaction, String risk, String market, String portfolio) {
    public String portfolio() {
        return portfolio == null || portfolio.isBlank() ? "http://127.0.0.1:8082" : portfolio;
    }
}