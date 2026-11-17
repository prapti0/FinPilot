# FinPilot REST API (v1)

Base URL via gateway: `http://localhost:8080/api/v1`

All mutating order/transaction calls require header: `Idempotency-Key: <uuid>`

## Auth

| Method | Path | Service | Description |
|--------|------|---------|-------------|
| POST | `/auth/register` | user | Register user |
| POST | `/auth/login` | user | JWT access + refresh |
| GET | `/users/me` | user | Current profile |

## Portfolio

| Method | Path | Description |
|--------|------|-------------|
| GET | `/portfolios` | List user portfolios |
| POST | `/portfolios` | Create portfolio |
| GET | `/portfolios/{id}` | Detail + allocation |
| GET | `/portfolios/{id}/holdings` | Holdings with P&L |
| GET | `/portfolios/{id}/performance` | Snapshots / returns |

## Orders

| Method | Path | Description |
|--------|------|-------------|
| POST | `/orders` | Place buy/sell (simulated) |
| GET | `/orders/{id}` | Order status |
| POST | `/orders/{id}/cancel` | Cancel if not executed |

**POST /orders body**

```json
{
  "portfolioId": 1,
  "symbol": "RELIANCE",
  "side": "BUY",
  "quantity": 10,
  "limitPrice": 1000
}
```

## Transactions / ledger (read-only for users)

| Method | Path | Description |
|--------|------|-------------|
| GET | `/accounts/me/balance` | Available + reserved cash |
| GET | `/transactions` | Paginated history |
| GET | `/transactions/{id}/ledger` | Double-entry lines |

## Market data

| Method | Path | Description |
|--------|------|-------------|
| GET | `/market/quotes/{symbol}` | Latest simulated quote |
| GET | `/market/quotes/{symbol}/history` | OHLC history |

## Risk

| Method | Path | Description |
|--------|------|-------------|
| GET | `/risk/portfolio/{portfolioId}` | Concentration, limits |
| GET | `/risk/alerts` | User risk alerts |

## AI (analysis only)

| Method | Path | Description |
|--------|------|-------------|
| POST | `/ai/analyze-portfolio` | Natural language Q&A |
| POST | `/ai/explain-risk` | Grounded risk narrative |
| POST | `/ai/engineering/review` | PR diff review (agent) |

**POST /ai/analyze-portfolio**

```json
{
  "portfolioId": 1,
  "question": "Where is my risk exposure high?"
}
```

Response includes `deterministicRiskSummary` from risk-service + `narrative` from Gemini (never overrides limits).