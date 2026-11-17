package com.finpilot.marketdata.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "finpilot.market")
public record AppProperties(int quoteCacheTtlSeconds) {
    public AppProperties {
        if (quoteCacheTtlSeconds <= 0) {
            quoteCacheTtlSeconds = 30;
        }
    }
}