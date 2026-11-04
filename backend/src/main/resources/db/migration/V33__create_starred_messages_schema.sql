-- V33__create_starred_messages_schema.sql
-- Direct Messaging: Durable Starred Messages persistence for caller reference

CREATE TABLE IF NOT EXISTS starred_messages (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    conversation_id UUID NOT NULL REFERENCES conversations(id) ON DELETE CASCADE,
    message_id UUID NOT NULL REFERENCES direct_messages(id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    starred_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_starred_messages_user_msg UNIQUE (user_id, message_id)
);

CREATE INDEX IF NOT EXISTS idx_starred_messages_user_time 
    ON starred_messages (user_id, starred_at DESC);

CREATE INDEX IF NOT EXISTS idx_starred_messages_conv 
    ON starred_messages (conversation_id);

