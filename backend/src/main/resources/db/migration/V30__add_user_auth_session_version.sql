-- ============================================================================
-- Migration V30: Add auth_version to users for immediate account-wide realtime session revocation
-- ============================================================================

ALTER TABLE users ADD COLUMN IF NOT EXISTS auth_version BIGINT NOT NULL DEFAULT 0;

CREATE INDEX IF NOT EXISTS idx_users_auth_version ON users (id, auth_version);
