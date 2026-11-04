CREATE TABLE IF NOT EXISTS community_outbox (
    id UUID PRIMARY KEY,
    community_id UUID NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    aggregate_id VARCHAR(100),
    payload TEXT NOT NULL,
    attempts INTEGER NOT NULL DEFAULT 0,
    available_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    published_at TIMESTAMPTZ,
    failed_at TIMESTAMPTZ,
    last_error TEXT,
    version BIGINT NOT NULL DEFAULT 0
);
CREATE INDEX IF NOT EXISTS idx_community_outbox_pending
    ON community_outbox(published_at, failed_at, available_at, created_at);
