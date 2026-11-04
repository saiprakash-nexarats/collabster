CREATE TABLE IF NOT EXISTS community_events (
    id UUID PRIMARY KEY,
    community_id UUID NOT NULL REFERENCES communities(id) ON DELETE CASCADE,
    created_by BIGINT NOT NULL,
    title VARCHAR(200) NOT NULL,
    description TEXT,
    starts_at TIMESTAMPTZ NOT NULL,
    ends_at TIMESTAMPTZ NOT NULL,
    location VARCHAR(500),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_community_event_start ON community_events(community_id, starts_at);

CREATE TABLE IF NOT EXISTS community_event_rsvps (
    id UUID PRIMARY KEY,
    event_id UUID NOT NULL REFERENCES community_events(id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_event_rsvp_user UNIQUE(event_id, user_id)
);
