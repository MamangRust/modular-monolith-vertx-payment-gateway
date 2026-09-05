CREATE TABLE "card_payments" (
    "payment_id"        UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    "reference_id"      VARCHAR(64) NOT NULL UNIQUE,
    "card_number"       VARCHAR(16) NOT NULL REFERENCES "cards" ("card_number"),
    "amount"            BIGINT NOT NULL,
    "payment_channel"   VARCHAR(30) NOT NULL,
    "payment_time"      TIMESTAMP NOT NULL DEFAULT current_timestamp,
    "status"            VARCHAR(20) NOT NULL DEFAULT 'POSTED',
    "statement_id"      INT REFERENCES "card_billing_statements" ("statement_id"),
    "created_at"        TIMESTAMP DEFAULT current_timestamp
);

CREATE INDEX idx_cp_card_time ON "card_payments" ("card_number", "payment_time");
CREATE INDEX idx_cp_reference ON "card_payments" ("reference_id");
