-- V29__create_user_data_erasure_outbox.sql
-- Durable, transactional outbox for cross-system downstream user data erasure

CREATE TABLE IF NOT EXISTS user_data_erasure_outbox (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id BIGINT NOT NULL,
    target_system VARCHAR(64) NOT NULL DEFAULT 'RECOMMENDATION',
    operation VARCHAR(64) NOT NULL DEFAULT 'ERASE',
    status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    attempt_count INT NOT NULL DEFAULT 0,
    next_attempt_at TIMESTAMPTZ,
    locked_at TIMESTAMPTZ,
    lock_owner VARCHAR(128),
    last_error_code VARCHAR(64),
    last_error_message VARCHAR(1000),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    completed_at TIMESTAMPTZ,
    CONSTRAINT uq_user_erasure_job UNIQUE (user_id, target_system, operation)
);

CREATE INDEX IF NOT EXISTS idx_user_erasure_claimable ON user_data_erasure_outbox(status, next_attempt_at)
    WHERE status IN ('PENDING', 'RETRY');
CREATE INDEX IF NOT EXISTS idx_user_erasure_in_progress ON user_data_erasure_outbox(status, locked_at)
    WHERE status = 'IN_PROGRESS';
CREATE INDEX IF NOT EXISTS idx_user_erasure_user ON user_data_erasure_outbox(user_id);
