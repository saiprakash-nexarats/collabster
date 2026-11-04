-- V20__create_platform_posts.sql
-- Creates schema for the platform-level Post domain (distinct from Community posts).

CREATE TABLE IF NOT EXISTS platform_posts (
    id UUID PRIMARY KEY,
    author_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    body TEXT,
    visibility VARCHAR(30) NOT NULL DEFAULT 'PUBLIC',
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMPTZ,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_platform_posts_author_created
    ON platform_posts(author_id, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_platform_posts_feed_access
    ON platform_posts(status, visibility, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_platform_posts_author_cursor
    ON platform_posts(author_id, status, created_at DESC, id DESC);

CREATE TABLE IF NOT EXISTS platform_post_media (
    id UUID PRIMARY KEY,
    post_id UUID NOT NULL REFERENCES platform_posts(id) ON DELETE CASCADE,
    media_type VARCHAR(30) NOT NULL,
    storage_key VARCHAR(1024) NOT NULL,
    mime_type VARCHAR(100) NOT NULL,
    display_order INT NOT NULL DEFAULT 0,
    width INT,
    height INT,
    duration_ms BIGINT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_platform_post_media_order UNIQUE (post_id, display_order)
);

CREATE INDEX IF NOT EXISTS idx_platform_post_media_post
    ON platform_post_media(post_id, display_order ASC);
