package com.finpilot.ai.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "finpilot")
public record AppProperties(Gemini gemini, Services services) {
    public record Gemini(String apiKey, String model) {
    }

    public record Services(String portfolio, String risk) {
    }
}