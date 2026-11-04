CREATE TABLE IF NOT EXISTS community_notifications (
    id UUID PRIMARY KEY,
    user_id BIGINT NOT NULL,
    community_id UUID,
    notification_type VARCHAR(50) NOT NULL,
    title VARCHAR(200) NOT NULL,
    body TEXT,
    target_type VARCHAR(50),
    target_id VARCHAR(100),
    read_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_community_notification_user_read
    ON community_notifications(user_id, read_at, created_at DESC);
