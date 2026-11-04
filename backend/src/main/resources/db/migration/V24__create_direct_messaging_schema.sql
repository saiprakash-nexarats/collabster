-- V24__create_direct_messaging_schema.sql
-- Establishes the authoritative PostgreSQL schema for Platform Direct Messaging (Phase 6)
-- Enforces normalized 1:1 conversation pair uniqueness, participant state, soft deletion,
-- client-message-id idempotency, attachments, reactions, receipts, and keyset cursor indexes.

-- 1. Conversations (Direct 1:1 with normalized low/high user pair identity)
CREATE TABLE IF NOT EXISTS conversations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    type VARCHAR(32) NOT NULL DEFAULT 'DIRECT',
    direct_user_low_id BIGINT,
    direct_user_high_id BIGINT,
    last_message_id UUID,
    last_message_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_conversations_type CHECK (type IN ('DIRECT', 'GROUP')),
    CONSTRAINT chk_direct_pair_ordering CHECK (
        (type = 'DIRECT' AND direct_user_low_id IS NOT NULL AND direct_user_high_id IS NOT NULL AND direct_user_low_id < direct_user_high_id)
        OR (type <> 'DIRECT')
    ),
    CONSTRAINT uq_conversations_direct_pair UNIQUE (direct_user_low_id, direct_user_high_id),
    CONSTRAINT fk_conversations_low_user FOREIGN KEY (direct_user_low_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_conversations_high_user FOREIGN KEY (direct_user_high_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_conversations_last_message 
    ON conversations (last_message_at DESC NULLS LAST, id DESC);


-- 2. Conversation Participants (Membership, read cursor, mute, and per-user archive state)
CREATE TABLE IF NOT EXISTS conversation_participants (
    conversation_id UUID NOT NULL REFERENCES conversations(id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    joined_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_read_message_id UUID,
    last_read_at TIMESTAMP WITH TIME ZONE,
    notifications_muted BOOLEAN NOT NULL DEFAULT FALSE,
    archived BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_for_user_at TIMESTAMP WITH TIME ZONE,
    PRIMARY KEY (conversation_id, user_id)
);

CREATE INDEX IF NOT EXISTS idx_conv_participants_user_archived 
    ON conversation_participants (user_id, archived, last_read_at);


-- 3. Direct Messages (Authoritative message persistence, soft deletion, replies, versioning)
CREATE TABLE IF NOT EXISTS direct_messages (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    conversation_id UUID NOT NULL REFERENCES conversations(id) ON DELETE CASCADE,
    sender_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    client_message_id UUID,
    message_type VARCHAR(32) NOT NULL DEFAULT 'TEXT',
    body TEXT,
    reply_to_message_id UUID REFERENCES direct_messages(id) ON DELETE SET NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    edited_at TIMESTAMP WITH TIME ZONE,
    deleted_at TIMESTAMP WITH TIME ZONE,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT chk_direct_messages_type CHECK (message_type IN ('TEXT', 'ATTACHMENT', 'MIXED')),
    CONSTRAINT chk_direct_messages_status CHECK (status IN ('ACTIVE', 'DELETED'))
);

-- Idempotency constraint: within a conversation, a sender cannot create duplicate messages with the same client_message_id
CREATE UNIQUE INDEX IF NOT EXISTS uq_direct_messages_client_id 
    ON direct_messages (conversation_id, sender_id, client_message_id) 
    WHERE client_message_id IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_direct_messages_conv_cursor 
    ON direct_messages (conversation_id, created_at DESC, id DESC);

CREATE INDEX IF NOT EXISTS idx_direct_messages_sender_cursor 
    ON direct_messages (sender_id, created_at DESC, id DESC);

-- Circular FK reference from conversations.last_message_id to direct_messages(id)
ALTER TABLE conversations 
    ADD CONSTRAINT fk_conversations_last_message 
    FOREIGN KEY (last_message_id) REFERENCES direct_messages(id) ON DELETE SET NULL;


-- 4. Message Attachments (Storage references, MIME type, dimensions, display ordering)
CREATE TABLE IF NOT EXISTS message_attachments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    message_id UUID NOT NULL REFERENCES direct_messages(id) ON DELETE CASCADE,
    media_type VARCHAR(32) NOT NULL,
    storage_key VARCHAR(512) NOT NULL,
    mime_type VARCHAR(128) NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    file_size BIGINT NOT NULL,
    width INTEGER,
    height INTEGER,
    duration_ms INTEGER,
    display_order INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_msg_attachment_media_type CHECK (media_type IN ('IMAGE', 'VIDEO', 'AUDIO', 'DOCUMENT'))
);

CREATE INDEX IF NOT EXISTS idx_msg_attachments_msg_order 
    ON message_attachments (message_id, display_order ASC);


-- 5. Message Reactions (User reaction per message, atomic updates)
CREATE TABLE IF NOT EXISTS message_reactions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    message_id UUID NOT NULL REFERENCES direct_messages(id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    reaction VARCHAR(64) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_message_reactions_user UNIQUE (message_id, user_id)
);

CREATE INDEX IF NOT EXISTS idx_msg_reactions_msg 
    ON message_reactions (message_id);


-- 6. Message Delivery and Read Receipts (Per-message delivery and read tracking)
CREATE TABLE IF NOT EXISTS message_receipts (
    message_id UUID NOT NULL REFERENCES direct_messages(id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    delivered_at TIMESTAMP WITH TIME ZONE,
    read_at TIMESTAMP WITH TIME ZONE,
    PRIMARY KEY (message_id, user_id)
);

CREATE INDEX IF NOT EXISTS idx_msg_receipts_user_read 
    ON message_receipts (user_id, read_at);
