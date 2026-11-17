package com.finpilot.order.web;

import com.finpilot.common.web.UserIdResolver;
import com.finpilot.order.service.OrderService;
import com.finpilot.order.web.dto.CreateOrderRequest;
import com.finpilot.order.web.dto.OrderResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {
    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
    public OrderResponse create(
            HttpServletRequest request,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody CreateOrderRequest body) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Idempotency-Key required");
        }
        Long userId = UserIdResolver.requireUserId(request);
        return orderService.placeOrder(userId, idempotencyKey, body);
    }

    @GetMapping("/{id}")
    public OrderResponse getById(HttpServletRequest request, @PathVariable Long id) {
        return orderService.getOrder(UserIdResolver.requireUserId(request), id);
    }

    @GetMapping
    public List<OrderResponse> list(HttpServletRequest request) {
        return orderService.listOrders(UserIdResolver.requireUserId(request));
    }
}