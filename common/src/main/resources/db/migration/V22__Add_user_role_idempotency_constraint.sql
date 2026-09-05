-- Ensure role assignment is idempotent for active rows while retaining soft-deleted history.
-- Keep the oldest active assignment if a pre-existing database contains duplicates.
DELETE FROM user_roles older
USING user_roles newer
WHERE older.user_role_id > newer.user_role_id
  AND older.user_id = newer.user_id
  AND older.role_id = newer.role_id
  AND older.deleted_at IS NULL
  AND newer.deleted_at IS NULL;

CREATE UNIQUE INDEX IF NOT EXISTS idx_user_roles_active_user_role
    ON user_roles (user_id, role_id)
    WHERE deleted_at IS NULL;
