-- V19__reconcile_existing_core_schema.sql
-- Reconciles missing JPA tables and columns from the core identity, auth, workspace,
-- profile, and community outbox domains without altering existing migrations V1-V18.

-- 1. Roles table
CREATE TABLE IF NOT EXISTS roles (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE
);

-- Seed authoritative platform roles deterministically and idempotently
INSERT INTO roles (name) VALUES
    ('ROLE_ADMIN'),
    ('ROLE_STUDENT'),
    ('ROLE_PROFESSIONAL'),
    ('ROLE_FOUNDER'),
    ('ROLE_COMPANY'),
    ('ROLE_INVESTOR'),
    ('ROLE_CREATOR'),
    ('ROLE_MENTOR'),
    ('ROLE_INFLUENCER'),
    ('ROLE_SERVICE_PROVIDER'),
    ('ROLE_OTHER'),
    ('ROLE_GUEST')
ON CONFLICT (name) DO NOTHING;

-- 2. User Roles Join Table (ManyToMany between User and Role)
CREATE TABLE IF NOT EXISTS user_roles (
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role_id BIGINT NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    PRIMARY KEY (user_id, role_id)
);

CREATE INDEX IF NOT EXISTS idx_user_roles_user_id ON user_roles(user_id);
CREATE INDEX IF NOT EXISTS idx_user_roles_role_id ON user_roles(role_id);

-- 3. Refresh Tokens table
CREATE TABLE IF NOT EXISTS refresh_tokens (
    id BIGSERIAL PRIMARY KEY,
    token VARCHAR(500) NOT NULL UNIQUE,
    user_id BIGINT NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    expires_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    revoked BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX IF NOT EXISTS idx_refresh_tokens_token ON refresh_tokens(token);
CREATE INDEX IF NOT EXISTS idx_refresh_tokens_user_id ON refresh_tokens(user_id);

-- 4. OTP table
CREATE TABLE IF NOT EXISTS otp (
    id BIGSERIAL PRIMARY KEY,
    otp VARCHAR(6) NOT NULL,
    user_id BIGINT NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    expires_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    verified BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX IF NOT EXISTS idx_otp_user_id ON otp(user_id);

-- 5. User Workspaces table
CREATE TABLE IF NOT EXISTS user_workspaces (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    workspace_type VARCHAR(50) NOT NULL,
    status VARCHAR(20) NOT NULL,
    priority VARCHAR(20) NOT NULL,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_user_workspace_type UNIQUE (user_id, workspace_type)
);

CREATE INDEX IF NOT EXISTS idx_user_workspaces_user_id ON user_workspaces(user_id);

-- 6. User Profile Extended Columns
ALTER TABLE users ADD COLUMN IF NOT EXISTS linkedin_url VARCHAR(500);
ALTER TABLE users ADD COLUMN IF NOT EXISTS portfolio_url VARCHAR(500);
ALTER TABLE users ADD COLUMN IF NOT EXISTS years_of_experience INTEGER;

-- 7. Community Outbox Event Tracing Columns
ALTER TABLE community_outbox ADD COLUMN IF NOT EXISTS correlation_id VARCHAR(100);
ALTER TABLE community_outbox ADD COLUMN IF NOT EXISTS causation_id VARCHAR(100);
