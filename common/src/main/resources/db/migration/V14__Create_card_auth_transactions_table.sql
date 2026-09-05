CREATE TABLE "card_auth_transactions" (
    "txn_id"            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    "card_number"       VARCHAR(16) NOT NULL REFERENCES "cards" ("card_number"),
    "merchant_id"       INT REFERENCES "merchants" ("merchant_id"),
    "amount"            BIGINT NOT NULL,
    "currency"          CHAR(3) NOT NULL DEFAULT 'IDR',
    "status"            VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    "decline_code"      VARCHAR(5),
    "auth_code"         VARCHAR(12),
    "pos_entry_mode"    VARCHAR(10),
    "mcc"               VARCHAR(4),
    "risk_score"        INT DEFAULT 0,
    "idempotency_key"   VARCHAR(64) UNIQUE,
    "txn_time"          TIMESTAMP NOT NULL DEFAULT current_timestamp,
    "settled_at"        TIMESTAMP,
    "created_at"        TIMESTAMP DEFAULT current_timestamp
);

CREATE INDEX idx_cat_card_time ON "card_auth_transactions" ("card_number", "txn_time");
CREATE INDEX idx_cat_status ON "card_auth_transactions" ("status");
CREATE INDEX idx_cat_idem_key ON "card_auth_transactions" ("idempotency_key");
