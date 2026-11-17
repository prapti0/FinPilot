package com.finpilot.user.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "finpilot")
public record AppProperties(String jwtSecret) {
}