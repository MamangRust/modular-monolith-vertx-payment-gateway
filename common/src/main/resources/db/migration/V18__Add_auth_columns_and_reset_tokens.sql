-- Align schema with auth service queries.
--
-- 1. UserRepositoryImpl inserts/reads users.verification_code and users.is_verified
--    (createUser, updateUserIsVerified, findByVerifiedEmail, findByVerificationCode).
--    V2 created "users" without those columns, so every auth register/verify call
--    failed with: ERROR: column "verification_code" of relation "users" does not exist.
--
-- 2. ResetTokenRepositoryImpl reads/writes a "reset_tokens" table that no migration
--    ever created (chaos.yaml also declares an sql policy targeting "reset_tokens").
--    Forgot-password flow failed with: ERROR: relation "reset_tokens" does not exist.
--
-- Added as ALTER/CREATE IF NOT EXISTS instead of editing V2, so already-migrated
-- databases keep their Flyway checksums valid.

ALTER TABLE "users"
    ADD COLUMN IF NOT EXISTS "verification_code" VARCHAR(100) DEFAULT NULL,
    ADD COLUMN IF NOT EXISTS "is_verified" BOOLEAN NOT NULL DEFAULT FALSE;

CREATE INDEX IF NOT EXISTS "idx_users_verification_code" ON "users" ("verification_code");

CREATE INDEX IF NOT EXISTS "idx_users_is_verified" ON "users" ("is_verified");

CREATE TABLE IF NOT EXISTS "reset_tokens" (
    "reset_token_id" SERIAL PRIMARY KEY,
    "user_id" INT NOT NULL REFERENCES "users" ("user_id") ON DELETE CASCADE,
    "token" VARCHAR(255) NOT NULL UNIQUE,
    "expiry_date" TIMESTAMP NOT NULL,
    "created_at" TIMESTAMP DEFAULT current_timestamp,
    "updated_at" TIMESTAMP DEFAULT current_timestamp,
    "deleted_at" TIMESTAMP DEFAULT NULL
);

CREATE INDEX IF NOT EXISTS "idx_reset_tokens_user_id" ON "reset_tokens" ("user_id");

CREATE INDEX IF NOT EXISTS "idx_reset_tokens_token" ON "reset_tokens" ("token");

CREATE INDEX IF NOT EXISTS "idx_reset_tokens_expiry_date" ON "reset_tokens" ("expiry_date");
