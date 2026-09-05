-- Idempotency key for retry-safe transaction creation.
-- Soft-deleted rows do not reserve a key, matching topup and transfer behavior.

ALTER TABLE transactions
    ADD COLUMN IF NOT EXISTS idempotency_key VARCHAR(128) DEFAULT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS idx_transactions_idempotency_key
    ON transactions (idempotency_key)
    WHERE idempotency_key IS NOT NULL AND deleted_at IS NULL;
