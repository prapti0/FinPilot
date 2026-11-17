package com.finpilot.order.service;

import com.finpilot.common.events.OrderCreatedPayload;
import com.finpilot.common.events.OrderExecutedPayload;
import com.finpilot.order.client.MarketDataClient;
import com.finpilot.order.client.RiskClient;
import com.finpilot.order.client.TransactionClient;
import com.finpilot.order.domain.OrderEntity;
import com.finpilot.order.domain.OutboxEventEntity;
import com.finpilot.order.repo.OrderPersistHelper;
import com.finpilot.order.repo.OrderRepository;
import com.finpilot.order.repo.OutboxEventRepository;
import com.finpilot.order.web.dto.CreateOrderRequest;
import com.finpilot.order.web.dto.OrderResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class OrderService {
    private final OrderRepository orderRepository;
    private final OrderPersistHelper orderPersistHelper;
    private final OutboxEventRepository outboxEventRepository;
    private final AssetLookup assetLookup;
    private final MarketDataClient marketDataClient;
    private final RiskClient riskClient;
    private final TransactionClient transactionClient;

    public OrderService(
            OrderRepository orderRepository,
            OrderPersistHelper orderPersistHelper,
            OutboxEventRepository outboxEventRepository,
            AssetLookup assetLookup,
            MarketDataClient marketDataClient,
            RiskClient riskClient,
            TransactionClient transactionClient) {
        this.orderRepository = orderRepository;
        this.orderPersistHelper = orderPersistHelper;
        this.outboxEventRepository = outboxEventRepository;
        this.assetLookup = assetLookup;
        this.marketDataClient = marketDataClient;
        this.riskClient = riskClient;
        this.transactionClient = transactionClient;
    }

    @Transactional
    public OrderResponse placeOrder(Long userId, String idempotencyKey, CreateOrderRequest request) {
        return orderRepository
                .findByUserIdAndIdempotencyKey(userId, idempotencyKey)
                .map(OrderResponse::from)
                .orElseGet(() -> processNewOrder(userId, idempotencyKey, request));
    }

    private OrderResponse processNewOrder(Long userId, String idempotencyKey, CreateOrderRequest request) {
        String side = request.side().toUpperCase();
        if (!"BUY".equals(side) && !"SELL".equals(side)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "side must be BUY or SELL");
        }

        var asset = assetLookup
                .findBySymbol(request.symbol())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown symbol"));

        long orderId = orderPersistHelper.insertOrder(
                userId,
                request.portfolioId(),
                asset.id(),
                side,
                request.quantity(),
                request.limitPrice(),
                "CREATED",
                idempotencyKey);
        OrderEntity order = orderRepository.findById(orderId).orElseThrow();
        orderPersistHelper.updateStatus(orderId, "VALIDATED", null);
        order.setStatus("VALIDATED");

        MarketDataClient.QuoteResponse quote = marketDataClient.getQuote(asset.symbol());
        BigDecimal refPrice = request.limitPrice() != null ? request.limitPrice() : quote.price();
        if (refPrice == null || refPrice.compareTo(BigDecimal.ZERO) <= 0) {
            return reject(order, "INVALID_PRICE");
        }

        BigDecimal orderValue = request.quantity().multiply(refPrice).setScale(4, RoundingMode.HALF_UP);
        var risk = riskClient.checkOrder(new RiskClient.RiskCheckRequest(
                userId, asset.symbol(), side, request.quantity(), refPrice, orderValue));
        if (risk == null || !risk.approved()) {
            String reason = risk != null ? risk.reason() : "RISK_CHECK_FAILED";
            return reject(order, reason);
        }

        boolean reserved = false;
        BigDecimal reserveAmount = BigDecimal.ZERO;
        try {
            if ("BUY".equals(side)) {
                reserveAmount = orderValue;
                var reservation = transactionClient.reserve(new TransactionClient.ReserveRequest(
                        userId, order.getId(), reserveAmount, idempotencyKey + "-reserve"));
                if (reservation == null || !reservation.accepted()) {
                    String reason = reservation != null ? reservation.reason() : "RESERVE_FAILED";
                    return reject(order, reason);
                }
                reserved = true;
            }

            orderPersistHelper.updateStatus(order.getId(), "RESERVED", null);
            order.setStatus("RESERVED");

            BigDecimal executionPrice = pickExecutionPrice(request.limitPrice(), quote.price());
            orderPersistHelper.updateStatus(order.getId(), "EXECUTED", null);
            order.setStatus("EXECUTED");

            if ("BUY".equals(side)) {
                BigDecimal settleAmount =
                        request.quantity().multiply(executionPrice).setScale(4, RoundingMode.HALF_UP);
                transactionClient.settle(new TransactionClient.LedgerActionRequest(
                        userId, order.getId(), idempotencyKey + "-settle", settleAmount));
            }

            enqueueOrderCreated(order, asset.symbol(), orderValue, idempotencyKey);
            enqueueOrderExecuted(order, asset.symbol(), executionPrice);
            return OrderResponse.from(order);
        } catch (RuntimeException ex) {
            if (reserved) {
                transactionClient.release(new TransactionClient.LedgerActionRequest(
                        userId, order.getId(), idempotencyKey + "-release", reserveAmount));
            }
            return reject(order, "EXECUTION_FAILED: " + ex.getMessage());
        }
    }

    private BigDecimal pickExecutionPrice(BigDecimal limitPrice, BigDecimal quotePrice) {
        if (limitPrice != null) {
            return limitPrice;
        }
        return quotePrice;
    }

    private OrderResponse reject(OrderEntity order, String reason) {
        orderPersistHelper.updateStatus(order.getId(), "REJECTED", reason);
        order.setStatus("REJECTED");
        order.setRejectionReason(reason);
        return OrderResponse.from(order);
    }

    private void enqueueOrderCreated(
            OrderEntity order, String symbol, BigDecimal estimatedValue, String idempotencyKey) {
        OrderCreatedPayload payload = new OrderCreatedPayload(
                order.getId(),
                order.getUserId(),
                order.getPortfolioId(),
                order.getAssetId(),
                symbol,
                order.getSide(),
                order.getQuantity(),
                order.getLimitPrice(),
                estimatedValue,
                idempotencyKey);
        saveOutbox(order.getId(), "finpilot.order-created", "OrderCreated", payloadMap(payload));
    }

    private void enqueueOrderExecuted(OrderEntity order, String symbol, BigDecimal executionPrice) {
        OrderExecutedPayload payload = new OrderExecutedPayload(
                order.getId(),
                order.getUserId(),
                order.getPortfolioId(),
                order.getAssetId(),
                symbol,
                order.getSide(),
                executionPrice,
                order.getQuantity(),
                Instant.now());
        saveOutbox(order.getId(), "finpilot.order-executed", "OrderExecuted", payloadMap(payload));
    }

    private void saveOutbox(Long orderId, String eventType, String aggregateEvent, Object payload) {
        OutboxEventEntity outbox = new OutboxEventEntity();
        outbox.setAggregateType("ORDER");
        outbox.setAggregateId(String.valueOf(orderId));
        outbox.setEventType(aggregateEvent);
        Map<String, Object> body = new HashMap<>();
        body.put("kafkaTopic", eventType);
        body.put("payload", payloadMap(payload));
        outbox.setPayload(body);
        outboxEventRepository.save(outbox);
    }

  @SuppressWarnings("unchecked")
    private Map<String, Object> payloadMap(Object payload) {
        if (payload instanceof Map<?, ?> map) {
            return (Map<String, Object>) map;
        }
        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        mapper.findAndRegisterModules();
        return mapper.convertValue(payload, Map.class);
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrder(Long userId, Long orderId) {
        OrderEntity order = orderRepository
                .findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        return OrderResponse.from(order);
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> listOrders(Long userId) {
        return orderRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(OrderResponse::from)
                .toList();
    }
}