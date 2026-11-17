package com.finpilot.gateway;

import com.finpilot.common.http.CorrelationHeaders;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Component
public class CorrelationIdGatewayFilter implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String incoming = exchange.getRequest().getHeaders().getFirst(CorrelationHeaders.CORRELATION_ID);
        String correlationId =
                (incoming == null || incoming.isBlank()) ? UUID.randomUUID().toString() : incoming;
        ServerHttpRequest request = exchange.getRequest().mutate()
                .header(CorrelationHeaders.CORRELATION_ID, correlationId)
                .build();
        exchange.getResponse().getHeaders().add(CorrelationHeaders.CORRELATION_ID, correlationId);
        return chain.filter(exchange.mutate().request(request).build());
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}