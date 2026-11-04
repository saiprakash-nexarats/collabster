CREATE TABLE IF NOT EXISTS community_polls (
    id UUID PRIMARY KEY,
    community_id UUID NOT NULL REFERENCES communities(id) ON DELETE CASCADE,
    created_by BIGINT NOT NULL,
    question VARCHAR(200) NOT NULL,
    allow_multiple BOOLEAN NOT NULL DEFAULT FALSE,
    closes_at TIMESTAMPTZ,
    closed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_community_poll_created ON community_polls(community_id, created_at DESC);

CREATE TABLE IF NOT EXISTS community_poll_options (
    id UUID PRIMARY KEY,
    poll_id UUID NOT NULL REFERENCES community_polls(id) ON DELETE CASCADE,
    option_text VARCHAR(200) NOT NULL,
    position INTEGER NOT NULL,
    CONSTRAINT uq_poll_option_text UNIQUE (poll_id, option_text),
    CONSTRAINT uq_poll_option_position UNIQUE (poll_id, position)
);

CREATE TABLE IF NOT EXISTS community_poll_votes (
    id UUID PRIMARY KEY,
    poll_id UUID NOT NULL REFERENCES community_polls(id) ON DELETE CASCADE,
    option_id UUID NOT NULL REFERENCES community_poll_options(id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_poll_user_option UNIQUE (poll_id, user_id, option_id)
);
CREATE INDEX IF NOT EXISTS idx_poll_vote_user ON community_poll_votes(poll_id, user_id);
