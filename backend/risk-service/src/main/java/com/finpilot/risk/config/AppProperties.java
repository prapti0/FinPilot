package com.finpilot.risk.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "finpilot.services")
public record AppProperties(String portfolio) {
}