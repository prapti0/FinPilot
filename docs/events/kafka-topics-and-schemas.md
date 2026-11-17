# Kafka topics and event contracts

**Principle:** Financial state changes commit in PostgreSQL first; events publish via transactional outbox. Consumers must be idempotent.

## Topics

| Topic | Key | Producer | Consumers |
|-------|-----|----------|-----------|
| `finpilot.order-created` | orderId | order-service | execution (order-service), risk-service |
| `finpilot.order-executed` | orderId | order-service | portfolio-service, notification |
| `finpilot.transaction-completed` | transactionId | transaction-service | notification, audit |
| `finpilot.risk-detected` | userId | risk-service | notification, ai-service |
| `finpilot.portfolio-updated` | portfolioId | portfolio-service | ai-service |

## Event envelope (all topics)

```json
{
  "eventId": "uuid",
  "eventType": "OrderCreated",
  "occurredAt": "2026-11-16T12:00:00Z",
  "correlationId": "uuid",
  "payload": { }
}
```

## OrderCreated

```json
{
  "orderId": 1001,
  "userId": 42,
  "portfolioId": 7,
  "assetId": 3,
  "symbol": "RELIANCE",
  "side": "BUY",
  "quantity": "10",
  "limitPrice": "1000.0000",
  "estimatedValue": "10000.0000",
  "idempotencyKey": "client-key-abc"
}
```

## OrderExecuted

```json
{
  "orderId": 1001,
  "executionPrice": "1000.0000",
  "executedQuantity": "10",
  "executedAt": "2026-11-16T12:00:05Z"
}
```

## TransactionCompleted

```json
{
  "transactionId": 500,
  "orderId": 1001,
  "idempotencyKey": "reserve-1001",
  "status": "POSTED",
  "ledgerEntryIds": [9001, 9002]
}
```

## RiskDetected

```json
{
  "userId": 42,
  "alertType": "SECTOR_CONCENTRATION",
  "severity": "HIGH",
  "ruleCode": "BANKING_EXPOSURE_30PCT",
  "metrics": {
    "bankingExposurePct": 45.0,
    "limitPct": 30.0
  }
}
```

## PortfolioUpdated

```json
{
  "portfolioId": 7,
  "userId": 42,
  "totalValue": "1000000.0000",
  "cashWeightPct": 10.0,
  "holdingsCount": 4
}
```

## DLQ

Failed messages after retries: `finpilot.dlq.<original-topic>`