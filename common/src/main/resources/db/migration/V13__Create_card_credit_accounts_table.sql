CREATE TABLE "card_credit_accounts" (
    "account_id"          SERIAL PRIMARY KEY,
    "card_number"         VARCHAR(16) NOT NULL REFERENCES "cards" ("card_number"),
    "credit_limit"        BIGINT NOT NULL DEFAULT 0,
    "used_credit"         BIGINT NOT NULL DEFAULT 0,
    "available_credit"    BIGINT GENERATED ALWAYS AS (credit_limit - used_credit) STORED,
    "billing_cycle_day"   INT NOT NULL DEFAULT 1,
    "payment_due_days"    INT NOT NULL DEFAULT 20,
    "annual_rate_bps"     INT NOT NULL DEFAULT 1800,
    "status"              VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    "last_statement_date" DATE,
    "next_statement_date" DATE,
    "delinquency_bucket"  INT NOT NULL DEFAULT 0,
    "dpd"                 INT NOT NULL DEFAULT 0,
    "created_at"          TIMESTAMP DEFAULT current_timestamp,
    "updated_at"          TIMESTAMP DEFAULT current_timestamp,
    CONSTRAINT "uq_credit_account_card" UNIQUE ("card_number")
);

CREATE INDEX idx_cca_status ON "card_credit_accounts" ("status");
CREATE INDEX idx_cca_billing_day ON "card_credit_accounts" ("billing_cycle_day");
