CREATE TABLE IF NOT EXISTS community_channel_message_reactions (
    id UUID PRIMARY KEY,
    message_id UUID NOT NULL REFERENCES community_channel_messages(id) ON DELETE CASCADE,
    community_id UUID NOT NULL REFERENCES communities(id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL,
    reaction VARCHAR(32) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_channel_message_reaction UNIQUE (message_id, user_id, reaction)
);

CREATE INDEX IF NOT EXISTS idx_channel_message_reaction_message
    ON community_channel_message_reactions(message_id);

CREATE TABLE IF NOT EXISTS community_channel_read_states (
    id UUID PRIMARY KEY,
    channel_id UUID NOT NULL REFERENCES community_channels(id) ON DELETE CASCADE,
    community_id UUID NOT NULL REFERENCES communities(id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL,
    last_read_message_id UUID REFERENCES community_channel_messages(id) ON DELETE SET NULL,
    last_read_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_channel_read_state UNIQUE (channel_id, user_id)
);

CREATE INDEX IF NOT EXISTS idx_channel_read_state_user_community
    ON community_channel_read_states(user_id, community_id);
