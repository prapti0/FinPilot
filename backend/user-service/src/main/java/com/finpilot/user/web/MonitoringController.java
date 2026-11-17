package com.finpilot.user.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/monitoring")
public class MonitoringController {

    private static final List<ServiceTarget> TARGETS = List.of(
            new ServiceTarget("gateway", 8080),
            new ServiceTarget("user", 8081),
            new ServiceTarget("portfolio", 8082),
            new ServiceTarget("order", 8083),
            new ServiceTarget("transaction", 8084),
            new ServiceTarget("market-data", 8085),
            new ServiceTarget("risk", 8086),
            new ServiceTarget("ai", 8087));

    private final RestClient restClient = RestClient.create();

    @GetMapping("/services")
    public List<Map<String, String>> serviceHealth() {
        List<Map<String, String>> rows = new ArrayList<>();
        for (ServiceTarget target : TARGETS) {
            String status = "DOWN";
            try {
                @SuppressWarnings("unchecked")
                Map<String, Object> body = restClient
                        .get()
                        .uri("http://127.0.0.1:{port}/actuator/health", target.port())
                        .retrieve()
                        .body(Map.class);
                if (body != null && "UP".equals(String.valueOf(body.get("status")))) {
                    status = "UP";
                } else if (body != null) {
                    status = String.valueOf(body.get("status"));
                }
            } catch (Exception ignored) {
                status = "DOWN";
            }
            Map<String, String> row = new LinkedHashMap<>();
            row.put("name", target.name());
            row.put("status", status);
            rows.add(row);
        }
        return rows;
    }

    private record ServiceTarget(String name, int port) {}
}