CREATE TABLE "card_billing_statements" (
    "statement_id"      SERIAL PRIMARY KEY,
    "card_number"       VARCHAR(16) NOT NULL REFERENCES "cards" ("card_number"),
    "statement_date"    DATE NOT NULL,
    "due_date"          DATE NOT NULL,
    "opening_balance"   BIGINT NOT NULL DEFAULT 0,
    "purchases"         BIGINT NOT NULL DEFAULT 0,
    "cash_advances"     BIGINT NOT NULL DEFAULT 0,
    "payments"          BIGINT NOT NULL DEFAULT 0,
    "fees"              BIGINT NOT NULL DEFAULT 0,
    "interest_charged"  BIGINT NOT NULL DEFAULT 0,
    "closing_balance"   BIGINT NOT NULL DEFAULT 0,
    "minimum_payment"   BIGINT NOT NULL DEFAULT 0,
    "payment_status"    VARCHAR(20) NOT NULL DEFAULT 'UNPAID',
    "created_at"        TIMESTAMP DEFAULT current_timestamp,
    CONSTRAINT "uq_statement_cycle" UNIQUE ("card_number", "statement_date")
);
