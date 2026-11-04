CREATE TABLE IF NOT EXISTS community_audit_logs (
    id UUID PRIMARY KEY,
    community_id UUID NOT NULL,
    actor_id BIGINT NOT NULL,
    action VARCHAR(80) NOT NULL,
    target_type VARCHAR(50),
    target_id VARCHAR(100),
    metadata TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_community_audit_community_time
    ON community_audit_logs(community_id, created_at DESC);
