package com.finpilot.order.service;

import com.finpilot.common.config.EventTransportProperties;
import com.finpilot.order.client.MarketDataClient;
import com.finpilot.order.client.PortfolioClient;
import com.finpilot.order.client.RiskClient;
import com.finpilot.order.client.TransactionClient;
import com.finpilot.order.domain.OrderEntity;
import com.finpilot.order.repo.OrderRepository;
import com.finpilot.order.web.dto.CreateOrderRequest;
import com.finpilot.order.web.dto.OrderResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderIdempotencyTest {

    @Mock
    OrderRepository orderRepository;
    @Test
    void duplicateIdempotencyKeyReturnsExistingOrderWithoutReprocessing() {
        OrderService orderService = new OrderService(
                orderRepository,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                new EventTransportProperties(false));
        OrderEntity existing = new OrderEntity();
        existing.setId(42L);
        existing.setStatus("EXECUTED");
        existing.setIdempotencyKey("key-1");
        when(orderRepository.findByUserIdAndIdempotencyKey(1L, "key-1")).thenReturn(Optional.of(existing));

        OrderResponse response = orderService.placeOrder(
                1L,
                "key-1",
                new CreateOrderRequest(1L, "RELIANCE", "BUY", new BigDecimal("1"), null));

        assertEquals(42L, response.id());
        assertEquals("EXECUTED", response.status());
    }
}