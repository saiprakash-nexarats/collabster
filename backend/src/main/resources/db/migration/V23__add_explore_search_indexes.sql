-- V23__add_explore_search_indexes.sql
-- Production Search and Trending Indexes for Phase 5 Explore & Discovery Core

-- 1. Full-Text Search GIN Index for Platform Posts
CREATE INDEX IF NOT EXISTS idx_platform_posts_search_tsv
    ON platform_posts USING gin (to_tsvector('english', coalesce(body, '')));

-- 2. Full-Text Search GIN Index for Community Posts (title + body)
CREATE INDEX IF NOT EXISTS idx_posts_search_tsv
    ON posts USING gin (to_tsvector('english', coalesce(title, '') || ' ' || coalesce(body, '')));

-- 3. Full-Text Search GIN Index for Communities (name + description)
CREATE INDEX IF NOT EXISTS idx_communities_search_tsv
    ON communities USING gin (to_tsvector('english', coalesce(name, '') || ' ' || coalesce(description, '')));

-- 4. Prefix / Case-insensitive Indexes for Communities
CREATE INDEX IF NOT EXISTS idx_communities_name_lower
    ON communities (lower(name));

-- 5. Full-Text and Prefix Search Indexes for Users (People Discovery & Search)
CREATE INDEX IF NOT EXISTS idx_users_search_tsv
    ON users USING gin (to_tsvector('simple', coalesce(username, '') || ' ' || coalesce(first_name, '') || ' ' || coalesce(last_name, '')));

CREATE INDEX IF NOT EXISTS idx_users_username_lower
    ON users (lower(username) varchar_pattern_ops);

CREATE INDEX IF NOT EXISTS idx_users_first_name_lower
    ON users (lower(first_name) varchar_pattern_ops);

CREATE INDEX IF NOT EXISTS idx_users_last_name_lower
    ON users (lower(last_name) varchar_pattern_ops);

-- 6. Interaction Time-Window Indexes for High-Performance Trending Aggregations
CREATE INDEX IF NOT EXISTS idx_content_likes_trending
    ON content_likes (created_at DESC, content_type, content_id);

CREATE INDEX IF NOT EXISTS idx_content_saves_trending
    ON content_saves (created_at DESC, content_type, content_id);

CREATE INDEX IF NOT EXISTS idx_interaction_events_trending
    ON content_interaction_events (created_at DESC, event_type, content_type, content_id);
