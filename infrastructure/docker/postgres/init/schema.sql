-- FinPilot PostgreSQL schema (source of truth for financial records)
-- Simulated trading only — no real-money execution

CREATE EXTENSION IF NOT EXISTS "pgcrypto";
CREATE EXTENSION IF NOT EXISTS "vector";

-- Roles
CREATE TABLE roles (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(50) NOT NULL UNIQUE
);

INSERT INTO roles (name) VALUES ('USER'), ('ADMIN'), ('ANALYST');

CREATE TABLE users (
    id              BIGSERIAL PRIMARY KEY,
    name            VARCHAR(255) NOT NULL,
    email           VARCHAR(255) NOT NULL UNIQUE,
    password_hash   VARCHAR(255) NOT NULL,
    role_id         BIGINT NOT NULL REFERENCES roles(id),
    status          VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE accounts (
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT NOT NULL REFERENCES users(id),
    account_type    VARCHAR(30) NOT NULL, -- CASH, INVESTMENT
    currency        CHAR(3) NOT NULL DEFAULT 'INR',
    available_balance NUMERIC(19, 4) NOT NULL DEFAULT 0,
    reserved_balance  NUMERIC(19, 4) NOT NULL DEFAULT 0,
    version         BIGINT NOT NULL DEFAULT 0, -- optimistic locking
    status          VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  CONSTRAINT accounts_balance_non_negative CHECK (available_balance >= 0 AND reserved_balance >= 0)
);

CREATE UNIQUE INDEX ux_accounts_user_type ON accounts(user_id, account_type, currency);

CREATE TABLE portfolios (
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT NOT NULL REFERENCES users(id),
    name            VARCHAR(255) NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE assets (
    id              BIGSERIAL PRIMARY KEY,
    symbol          VARCHAR(32) NOT NULL UNIQUE,
    name            VARCHAR(255) NOT NULL,
    asset_type      VARCHAR(30) NOT NULL, -- EQUITY, DEBT, MF, CASH
    sector          VARCHAR(100),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE holdings (
    id              BIGSERIAL PRIMARY KEY,
    portfolio_id    BIGINT NOT NULL REFERENCES portfolios(id),
    asset_id        BIGINT NOT NULL REFERENCES assets(id),
    quantity        NUMERIC(19, 6) NOT NULL DEFAULT 0,
    avg_price       NUMERIC(19, 4) NOT NULL DEFAULT 0,
    version         BIGINT NOT NULL DEFAULT 0,
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (portfolio_id, asset_id)
);

CREATE TYPE order_side AS ENUM ('BUY', 'SELL');
CREATE TYPE order_status AS ENUM (
    'CREATED', 'VALIDATED', 'RESERVED', 'EXECUTED', 'REJECTED', 'CANCELLED', 'FAILED'
);

CREATE TABLE orders (
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT NOT NULL REFERENCES users(id),
    portfolio_id    BIGINT NOT NULL REFERENCES portfolios(id),
    asset_id        BIGINT NOT NULL REFERENCES assets(id),
    side            order_side NOT NULL,
    quantity        NUMERIC(19, 6) NOT NULL,
    limit_price     NUMERIC(19, 4),
    status          order_status NOT NULL DEFAULT 'CREATED',
    idempotency_key VARCHAR(128) NOT NULL,
    rejection_reason TEXT,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (user_id, idempotency_key)
);

CREATE INDEX ix_orders_user_status ON orders(user_id, status);

CREATE TYPE txn_status AS ENUM ('PENDING', 'POSTED', 'REVERSED', 'FAILED');

CREATE TABLE transactions (
    id              BIGSERIAL PRIMARY KEY,
    order_id        BIGINT REFERENCES orders(id),
    idempotency_key VARCHAR(128) NOT NULL UNIQUE,
    description     VARCHAR(500),
    status          txn_status NOT NULL DEFAULT 'PENDING',
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Immutable ledger lines (corrections via reversal transactions only)
CREATE TABLE ledger_entries (
    id              BIGSERIAL PRIMARY KEY,
    transaction_id  BIGINT NOT NULL REFERENCES transactions(id),
    account_id      BIGINT NOT NULL REFERENCES accounts(id),
    debit           NUMERIC(19, 4) NOT NULL DEFAULT 0,
    credit          NUMERIC(19, 4) NOT NULL DEFAULT 0,
    entry_type      VARCHAR(50) NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT ledger_debit_credit_xor CHECK (
        (debit > 0 AND credit = 0) OR (credit > 0 AND debit = 0)
    )
);

CREATE INDEX ix_ledger_account ON ledger_entries(account_id);
CREATE INDEX ix_ledger_txn ON ledger_entries(transaction_id);

-- Enforce balanced journal per transaction (debits = credits)
CREATE OR REPLACE FUNCTION check_ledger_balanced() RETURNS TRIGGER AS $$
DECLARE
    total_debit NUMERIC(19, 4);
    total_credit NUMERIC(19, 4);
BEGIN
    SELECT COALESCE(SUM(debit), 0), COALESCE(SUM(credit), 0)
    INTO total_debit, total_credit
    FROM ledger_entries
  WHERE transaction_id = COALESCE(NEW.transaction_id, OLD.transaction_id);

    IF total_debit <> total_credit THEN
        RAISE EXCEPTION 'Ledger transaction % unbalanced: debit=% credit=%',
            COALESCE(NEW.transaction_id, OLD.transaction_id), total_debit, total_credit;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Deferred constraint trigger would be ideal; application-layer posts all lines in one TX first

CREATE TABLE market_prices (
    asset_id        BIGINT NOT NULL REFERENCES assets(id),
    price           NUMERIC(19, 4) NOT NULL,
    recorded_at     TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (asset_id, recorded_at)
);

CREATE TABLE risk_alerts (
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT NOT NULL REFERENCES users(id),
    alert_type      VARCHAR(50) NOT NULL,
    severity        VARCHAR(20) NOT NULL,
    payload         JSONB NOT NULL DEFAULT '{}',
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE ai_reports (
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT NOT NULL REFERENCES users(id),
    report_type     VARCHAR(50) NOT NULL,
    prompt_hash     VARCHAR(64),
    response        TEXT NOT NULL,
    metadata        JSONB NOT NULL DEFAULT '{}',
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE audit_logs (
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT REFERENCES users(id),
    action          VARCHAR(100) NOT NULL,
    resource_type   VARCHAR(50),
    resource_id     VARCHAR(100),
    details         JSONB,
    ip_address      INET,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Transactional outbox for Kafka
CREATE TABLE outbox_events (
    id              BIGSERIAL PRIMARY KEY,
    aggregate_type  VARCHAR(50) NOT NULL,
    aggregate_id    VARCHAR(100) NOT NULL,
    event_type      VARCHAR(100) NOT NULL,
    payload         JSONB NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    published_at    TIMESTAMPTZ
);

CREATE INDEX ix_outbox_unpublished ON outbox_events(published_at) WHERE published_at IS NULL;

-- RAG document store (pgvector)
CREATE TABLE knowledge_chunks (
    id              BIGSERIAL PRIMARY KEY,
    document_id     VARCHAR(100) NOT NULL,
    title           VARCHAR(500),
    chunk_text      TEXT NOT NULL,
    embedding       vector(768),
    metadata        JSONB NOT NULL DEFAULT '{}',
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX ix_knowledge_embedding ON knowledge_chunks USING ivfflat (embedding vector_cosine_ops);

-- Seed demo assets (simulated)
INSERT INTO assets (symbol, name, asset_type, sector) VALUES
    ('RELIANCE', 'Reliance Industries (simulated)', 'EQUITY', 'Energy'),
    ('HDFCBANK', 'HDFC Bank (simulated)', 'EQUITY', 'Banking'),
    ('NIFTYBEES', 'Nifty ETF (simulated)', 'MF', 'Index');