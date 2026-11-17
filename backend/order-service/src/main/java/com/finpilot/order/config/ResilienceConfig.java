package com.finpilot.order.config;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.netty.channel.ChannelOption;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;

@Configuration
public class ResilienceConfig {

    public static final String DOWNSTREAM = "downstream";

    @Bean
    CircuitBreakerRegistry circuitBreakerRegistry() {
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
                .failureRateThreshold(50)
                .slidingWindowSize(20)
                .minimumNumberOfCalls(5)
                .waitDurationInOpenState(Duration.ofSeconds(15))
                .permittedNumberOfCallsInHalfOpenState(3)
                .build();
        return CircuitBreakerRegistry.of(config);
    }

    @Bean
    CircuitBreaker downstreamCircuitBreaker(CircuitBreakerRegistry registry) {
        return registry.circuitBreaker(DOWNSTREAM);
    }

    @Bean
    WebClient.Builder webClientBuilder() {
        HttpClient httpClient = HttpClient.create()
                .responseTimeout(Duration.ofSeconds(8))
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 3_000);
        return WebClient.builder().clientConnector(new ReactorClientHttpConnector(httpClient));
    }
}