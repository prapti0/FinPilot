package com.finpilot.ai.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.finpilot.ai.config.AppProperties;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class GeminiClient {
    private final AppProperties properties;
    private final WebClient webClient;

    public GeminiClient(AppProperties properties, WebClient.Builder builder) {
        this.properties = properties;
        this.webClient = builder.build();
    }

    public Optional<String> generate(String prompt) {
        String apiKey = properties.gemini().apiKey();
        if (apiKey == null || apiKey.isBlank()) {
            return Optional.empty();
        }
        String model = properties.gemini().model();
        String url = "https://generativelanguage.googleapis.com/v1beta/models/"
                + model
                + ":generateContent?key="
                + apiKey;
        Map<String, Object> body = Map.of(
                "contents",
                List.of(Map.of("parts", List.of(Map.of("text", prompt)))));
        JsonNode response = webClient
                .post()
                .uri(url)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .block();
        if (response == null) {
            return Optional.empty();
        }
        String text = response.at("/candidates/0/content/parts/0/text").asText(null);
        return Optional.ofNullable(text);
    }
}