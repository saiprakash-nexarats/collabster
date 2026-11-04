-- V27: Create Recommendation Feedback Outbox Schema
-- Durable outbox table for asynchronous propagation of authoritative user behavioral events to Python ML pipeline.

CREATE TABLE IF NOT EXISTS recommendation_feedback_outbox (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    event_id UUID NOT NULL,
    contract_version VARCHAR(32) NOT NULL DEFAULT '1',
    event_type VARCHAR(64) NOT NULL,
    user_id BIGINT NOT NULL,
    content_type VARCHAR(64),
    content_id UUID,
    occurred_at TIMESTAMP WITH TIME ZONE NOT NULL,
    payload JSONB NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    attempt_count INT NOT NULL DEFAULT 0,
    next_attempt_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    locked_at TIMESTAMP WITH TIME ZONE,
    lock_owner VARCHAR(128),
    last_error_code VARCHAR(64),
    last_error_message VARCHAR(512),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_rec_feedback_outbox_event_id UNIQUE (event_id)
);

-- Index for worker claiming pending/retry jobs via SELECT ... FOR UPDATE SKIP LOCKED
CREATE INDEX IF NOT EXISTS idx_rec_feedback_outbox_claimable 
    ON recommendation_feedback_outbox (next_attempt_at ASC, id ASC) 
    WHERE status IN ('PENDING', 'RETRY');

-- Index for detecting and reclaiming stale locked jobs
CREATE INDEX IF NOT EXISTS idx_rec_feedback_outbox_in_progress
    ON recommendation_feedback_outbox (locked_at ASC)
    WHERE status = 'IN_PROGRESS';

-- Index for retention cleanup of delivered or permanently failed jobs
CREATE INDEX IF NOT EXISTS idx_rec_feedback_outbox_retention
    ON recommendation_feedback_outbox (updated_at ASC)
    WHERE status IN ('DELIVERED', 'FAILED');

-- Index for user event auditability
CREATE INDEX IF NOT EXISTS idx_rec_feedback_outbox_user
    ON recommendation_feedback_outbox (user_id, occurred_at DESC);
