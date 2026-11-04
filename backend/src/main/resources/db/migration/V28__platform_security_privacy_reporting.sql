-- V28__platform_security_privacy_reporting.sql
-- Phase 10: Platform-wide Security, Privacy, Global Reporting, Privileged Audit, and Account Lifecycle.

-- 1. Account Lifecycle flags on users
ALTER TABLE users ADD COLUMN IF NOT EXISTS deactivated BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE users ADD COLUMN IF NOT EXISTS deleted BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE users ADD COLUMN IF NOT EXISTS deactivated_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE users ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP WITH TIME ZONE;

CREATE INDEX IF NOT EXISTS idx_users_lifecycle_status
    ON users(account_enabled, deactivated, deleted);

-- 2. Platform-wide Reports Table
CREATE TABLE IF NOT EXISTS platform_reports (
    id UUID PRIMARY KEY,
    reporter_user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    target_type VARCHAR(50) NOT NULL,
    target_id VARCHAR(100) NOT NULL,
    reason VARCHAR(50) NOT NULL,
    details TEXT,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    resolved_by BIGINT REFERENCES users(id) ON DELETE SET NULL,
    resolved_at TIMESTAMP WITH TIME ZONE,
    resolution VARCHAR(50),
    resolution_notes TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_platform_reports_status_created
    ON platform_reports(status, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_platform_reports_target
    ON platform_reports(target_type, target_id);

CREATE INDEX IF NOT EXISTS idx_platform_reports_reporter_created
    ON platform_reports(reporter_user_id, created_at DESC);

-- 3. Platform Privileged Audit Logs Table
CREATE TABLE IF NOT EXISTS platform_audit_logs (
    id UUID PRIMARY KEY,
    actor_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    action VARCHAR(80) NOT NULL,
    target_type VARCHAR(50) NOT NULL,
    target_id VARCHAR(100) NOT NULL,
    metadata TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_platform_audit_time
    ON platform_audit_logs(created_at DESC);

CREATE INDEX IF NOT EXISTS idx_platform_audit_actor_time
    ON platform_audit_logs(actor_id, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_platform_audit_target
    ON platform_audit_logs(target_type, target_id);
