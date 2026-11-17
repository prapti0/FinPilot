package com.finpilot.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "finpilot.events")
public record EventTransportProperties(boolean kafkaEnabled) {
    public EventTransportProperties() {
        this(true);
    }
}