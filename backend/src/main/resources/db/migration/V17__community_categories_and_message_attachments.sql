ALTER TABLE communities ADD COLUMN IF NOT EXISTS category VARCHAR(50);
UPDATE communities SET category = 'OTHER' WHERE category IS NULL;
ALTER TABLE communities ALTER COLUMN category SET NOT NULL;

CREATE TABLE IF NOT EXISTS community_channel_message_attachments (
    id UUID PRIMARY KEY,
    message_id UUID NOT NULL REFERENCES community_channel_messages(id) ON DELETE CASCADE,
    attachment_url VARCHAR(2048) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_channel_message_attachment_message
    ON community_channel_message_attachments(message_id);
