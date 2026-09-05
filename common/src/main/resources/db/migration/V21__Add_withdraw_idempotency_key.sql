-- Retry-safe withdrawal creation. Soft-deleted rows release their key.
ALTER TABLE withdraws
    ADD COLUMN IF NOT EXISTS idempotency_key VARCHAR(128) DEFAULT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS idx_withdraws_idempotency_key
    ON withdraws (idempotency_key)
    WHERE idempotency_key IS NOT NULL AND deleted_at IS NULL;
