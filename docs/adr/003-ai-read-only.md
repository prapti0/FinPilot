# ADR 003: AI layer is read-only for money

## Status

Accepted

## Context

LLM outputs are non-deterministic. Letting them post ledger entries would be unsafe.

## Decision

`ai-service` only reads portfolio/account context and knowledge chunks. No endpoints on AI mutate `accounts`, `ledger_entries`, or `orders`.

## Consequences

- Users get explanations, not automated trades from the model
- RAG/context quality matters; hallucinations cannot drain balances