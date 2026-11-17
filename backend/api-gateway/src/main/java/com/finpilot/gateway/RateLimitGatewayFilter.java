package com.finpilot.gateway;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.InetSocketAddress;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class RateLimitGatewayFilter implements GlobalFilter, Ordered {

    private final int limitPerMinute;
    private final Map<String, Window> windows = new ConcurrentHashMap<>();

    public RateLimitGatewayFilter(
            @Value("${finpilot.rate-limit.requests-per-minute:120}") int limitPerMinute) {
        this.limitPerMinute = Math.max(limitPerMinute, 1);
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getPath().value();
        if (path.startsWith("/actuator")) {
            return chain.filter(exchange);
        }
        String clientKey = resolveClientKey(exchange);
        long epochMinute = Instant.now().getEpochSecond() / 60;
        Window window = windows.compute(clientKey, (k, existing) -> {
            if (existing == null || existing.epochMinute != epochMinute) {
                return new Window(epochMinute, new AtomicInteger(0));
            }
            return existing;
        });
        int count = window.count.incrementAndGet();
        if (count > limitPerMinute) {
            exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
            return exchange.getResponse().setComplete();
        }
        return chain.filter(exchange);
    }

    private static String resolveClientKey(ServerWebExchange exchange) {
        InetSocketAddress remote = exchange.getRequest().getRemoteAddress();
        if (remote != null && remote.getAddress() != null) {
            return remote.getAddress().getHostAddress();
        }
        return "unknown";
    }

  @Override
    public int getOrder() {
        return -90;
    }

    private static final class Window {
        final long epochMinute;
        final AtomicInteger count;

        Window(long epochMinute, AtomicInteger count) {
            this.epochMinute = epochMinute;
            this.count = count;
        }
    }
}