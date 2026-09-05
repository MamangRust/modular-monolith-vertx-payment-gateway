-- Idempotency keys for retry-safe financial commands (SUPERPLANNING B.7 #3).
-- A client may retry a create-topup / create-transfer with the same key; the
-- unique index below guarantees at most one applied operation per key.

ALTER TABLE "topups"
    ADD COLUMN IF NOT EXISTS "idempotency_key" VARCHAR(128) DEFAULT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS idx_topups_idempotency_key
    ON topups (idempotency_key)
    WHERE idempotency_key IS NOT NULL AND deleted_at IS NULL;

ALTER TABLE transfers
    ADD COLUMN IF NOT EXISTS "idempotency_key" VARCHAR(128) DEFAULT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS idx_transfers_idempotency_key
    ON transfers (idempotency_key)
    WHERE idempotency_key IS NOT NULL AND deleted_at IS NULL;
