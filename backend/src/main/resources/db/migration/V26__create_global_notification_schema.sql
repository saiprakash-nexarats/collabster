-- V26: Create Global Notification Schema (Notification Center, Preferences, Devices, Outbox, Deliveries)

CREATE TABLE IF NOT EXISTS notifications (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    recipient_user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    actor_user_id BIGINT REFERENCES users(id) ON DELETE SET NULL,
    type VARCHAR(60) NOT NULL,
    category VARCHAR(40) NOT NULL,
    source_type VARCHAR(60),
    source_id VARCHAR(128),
    context_type VARCHAR(60),
    context_id VARCHAR(128),
    event_key VARCHAR(200) NOT NULL,
    title_key VARCHAR(120),
    body_key VARCHAR(120),
    target_type VARCHAR(60),
    target_id VARCHAR(128),
    payload JSONB,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    seen_at TIMESTAMP WITH TIME ZONE,
    read_at TIMESTAMP WITH TIME ZONE,
    archived_at TIMESTAMP WITH TIME ZONE,
    CONSTRAINT uq_notifications_event_key UNIQUE (event_key)
);

CREATE INDEX IF NOT EXISTS idx_notifications_recipient_created 
    ON notifications(recipient_user_id, created_at DESC, id DESC);

CREATE INDEX IF NOT EXISTS idx_notifications_recipient_read 
    ON notifications(recipient_user_id, read_at, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_notifications_recipient_category 
    ON notifications(recipient_user_id, category, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_notifications_recipient_unread 
    ON notifications(recipient_user_id) 
    WHERE read_at IS NULL AND archived_at IS NULL;

-- 2. Notification Preferences
CREATE TABLE IF NOT EXISTS notification_preferences (
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    category VARCHAR(40) NOT NULL,
    in_app_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    push_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    email_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id, category)
);

-- 3. Mobile / Web Device Token Registrations
CREATE TABLE IF NOT EXISTS notification_devices (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    platform VARCHAR(32) NOT NULL,
    provider VARCHAR(32) NOT NULL,
    token VARCHAR(1024) NOT NULL,
    token_hash VARCHAR(64) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_seen_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    revoked_at TIMESTAMP WITH TIME ZONE,
    CONSTRAINT uq_notification_devices_token_hash UNIQUE (token_hash),
    CONSTRAINT chk_notification_device_platform CHECK (platform IN ('ANDROID', 'IOS', 'WEB')),
    CONSTRAINT chk_notification_device_provider CHECK (provider IN ('FCM', 'APNS', 'WEB_PUSH'))
);

CREATE INDEX IF NOT EXISTS idx_notification_devices_user 
    ON notification_devices(user_id, enabled);

-- 4. Durable Outbox for Asynchronous Dispatch
CREATE TABLE IF NOT EXISTS notification_outbox (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    notification_id UUID NOT NULL REFERENCES notifications(id) ON DELETE CASCADE,
    channel VARCHAR(32) NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    attempt_count INT NOT NULL DEFAULT 0,
    next_attempt_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    locked_at TIMESTAMP WITH TIME ZONE,
    locked_by VARCHAR(128),
    last_error_code VARCHAR(64),
    last_error_message VARCHAR(512),
    CONSTRAINT uq_notification_outbox_channel UNIQUE (notification_id, channel),
    CONSTRAINT chk_notification_outbox_channel CHECK (channel IN ('REALTIME', 'PUSH', 'EMAIL')),
    CONSTRAINT chk_notification_outbox_status CHECK (status IN ('PENDING', 'IN_PROGRESS', 'SENT', 'FAILED', 'SKIPPED'))
);

CREATE INDEX IF NOT EXISTS idx_notification_outbox_poll 
    ON notification_outbox(status, next_attempt_at) 
    WHERE status IN ('PENDING', 'IN_PROGRESS');

-- 5. Delivery Audit History
CREATE TABLE IF NOT EXISTS notification_deliveries (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    notification_id UUID NOT NULL REFERENCES notifications(id) ON DELETE CASCADE,
    channel VARCHAR(32) NOT NULL,
    destination_reference VARCHAR(255),
    status VARCHAR(32) NOT NULL,
    provider_message_id VARCHAR(255),
    attempt_count INT NOT NULL,
    sent_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    error_code VARCHAR(64),
    CONSTRAINT chk_notification_deliveries_status CHECK (status IN ('SUCCESS', 'FAILED', 'SKIPPED'))
);

CREATE INDEX IF NOT EXISTS idx_notification_deliveries_notification 
    ON notification_deliveries(notification_id, sent_at DESC);
