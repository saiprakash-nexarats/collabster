CREATE TABLE IF NOT EXISTS community_channels (
    id UUID PRIMARY KEY,
    community_id UUID NOT NULL REFERENCES communities(id) ON DELETE CASCADE,
    name VARCHAR(60) NOT NULL,
    description VARCHAR(300),
    read_only BOOLEAN NOT NULL DEFAULT FALSE,
    created_by BIGINT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_community_channel_name UNIQUE (community_id, name)
);

CREATE TABLE IF NOT EXISTS community_channel_messages (
    id UUID PRIMARY KEY,
    channel_id UUID NOT NULL REFERENCES community_channels(id) ON DELETE CASCADE,
    community_id UUID NOT NULL REFERENCES communities(id) ON DELETE CASCADE,
    sender_id BIGINT NOT NULL,
    body TEXT NOT NULL,
    parent_message_id UUID REFERENCES community_channel_messages(id) ON DELETE SET NULL,
    removed BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_channel_message_channel_time
    ON community_channel_messages(channel_id, created_at DESC);

INSERT INTO community_channels(id, community_id, name, read_only, created_by)
SELECT gen_random_uuid(), c.id, 'general', FALSE, c.owner_id
FROM communities c
WHERE NOT EXISTS (SELECT 1 FROM community_channels ch WHERE ch.community_id = c.id AND ch.name = 'general');
