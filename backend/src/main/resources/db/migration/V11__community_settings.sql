ALTER TABLE communities ADD COLUMN IF NOT EXISTS logo_url VARCHAR(1000);
ALTER TABLE communities ADD COLUMN IF NOT EXISTS cover_url VARCHAR(1000);

CREATE TABLE IF NOT EXISTS community_settings (
    community_id UUID PRIMARY KEY REFERENCES communities(id) ON DELETE CASCADE,
    member_posts_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    comments_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    channels_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    messaging_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    polls_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    events_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    member_invites_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    ai_moderation_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    channel_creation_role VARCHAR(20) NOT NULL DEFAULT 'MODERATOR',
    message_send_role VARCHAR(20) NOT NULL DEFAULT 'MEMBER',
    poll_creation_role VARCHAR(20) NOT NULL DEFAULT 'MEMBER'
);

INSERT INTO community_settings(community_id)
SELECT id FROM communities ON CONFLICT (community_id) DO NOTHING;
