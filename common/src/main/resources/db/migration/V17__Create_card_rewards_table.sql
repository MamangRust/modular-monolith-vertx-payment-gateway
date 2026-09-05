CREATE TABLE "card_rewards" (
    "reward_id"     SERIAL PRIMARY KEY,
    "card_number"   VARCHAR(16) NOT NULL REFERENCES "cards" ("card_number"),
    "txn_id"        UUID REFERENCES "card_auth_transactions" ("txn_id"),
    "reward_type"   VARCHAR(20) NOT NULL DEFAULT 'POINTS',
    "amount"        BIGINT NOT NULL,
    "description"   VARCHAR(200),
    "expires_at"    DATE,
    "created_at"    TIMESTAMP DEFAULT current_timestamp
);

CREATE INDEX idx_cr_card ON "card_rewards" ("card_number");
