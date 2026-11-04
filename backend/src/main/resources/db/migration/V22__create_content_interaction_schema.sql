-- V22__create_content_interaction_schema.sql
-- Establishes the authoritative schema for shared ContentRef interactions (Likes, Saves, Events)
-- Decoupled from entity tables, supporting both PLATFORM_POST and COMMUNITY_POST polymorphic references.

-- 1. Stateful Content Likes
CREATE TABLE IF NOT EXISTS content_likes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    content_type VARCHAR(30) NOT NULL,
    content_id UUID NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_content_like_type CHECK (content_type IN ('PLATFORM_POST', 'COMMUNITY_POST')),
    CONSTRAINT uq_content_likes UNIQUE (user_id, content_type, content_id)
);

CREATE INDEX IF NOT EXISTS idx_content_likes_content 
    ON content_likes (content_type, content_id);

CREATE INDEX IF NOT EXISTS idx_content_likes_user_cursor 
    ON content_likes (user_id, created_at DESC, id DESC);


-- 2. Stateful Content Saves (Bookmarks)
CREATE TABLE IF NOT EXISTS content_saves (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    content_type VARCHAR(30) NOT NULL,
    content_id UUID NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_content_save_type CHECK (content_type IN ('PLATFORM_POST', 'COMMUNITY_POST')),
    CONSTRAINT uq_content_saves UNIQUE (user_id, content_type, content_id)
);

CREATE INDEX IF NOT EXISTS idx_content_saves_user_cursor 
    ON content_saves (user_id, created_at DESC, id DESC);

CREATE INDEX IF NOT EXISTS idx_content_saves_content 
    ON content_saves (content_type, content_id);


-- 3. Append-Only Content Interaction Events (Telemetry, Shares, Views, Dwells, Negative Signals)
CREATE TABLE IF NOT EXISTS content_interaction_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    content_type VARCHAR(30) NOT NULL,
    content_id UUID NOT NULL,
    event_type VARCHAR(30) NOT NULL,
    duration_ms BIGINT,
    session_id VARCHAR(100),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_interaction_event_content_type CHECK (content_type IN ('PLATFORM_POST', 'COMMUNITY_POST')),
    CONSTRAINT chk_interaction_event_type CHECK (event_type IN ('SHARE', 'VIEW', 'WATCH_TIME', 'HIDE', 'NOT_INTERESTED')),
    CONSTRAINT chk_interaction_event_duration CHECK (duration_ms IS NULL OR (duration_ms >= 0 AND duration_ms <= 86400000))
);

CREATE INDEX IF NOT EXISTS idx_interaction_events_user_cursor 
    ON content_interaction_events (user_id, created_at DESC, id DESC);

CREATE INDEX IF NOT EXISTS idx_interaction_events_content 
    ON content_interaction_events (content_type, content_id, event_type);

CREATE INDEX IF NOT EXISTS idx_interaction_events_dedup 
    ON content_interaction_events (user_id, content_type, content_id, event_type, session_id, created_at DESC);
