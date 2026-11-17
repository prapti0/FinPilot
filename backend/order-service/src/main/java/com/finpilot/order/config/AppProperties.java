package com.finpilot.order.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "finpilot.services")
public record AppProperties(String transaction, String risk, String market) {
}