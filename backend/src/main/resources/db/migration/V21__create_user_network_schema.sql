-- V21__create_user_network_schema.sql
-- Establishes the authoritative schema for User Social Graph (Follow, Connection, Block, Mute)
-- Implements deterministic pair uniqueness, relational integrity, and keyset cursor indexes.

-- 1. Directional User Follows
CREATE TABLE IF NOT EXISTS user_follows (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    follower_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    following_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_no_self_follow CHECK (follower_id <> following_id),
    CONSTRAINT uq_user_follows UNIQUE (follower_id, following_id)
);

CREATE INDEX IF NOT EXISTS idx_user_follows_follower_cursor 
    ON user_follows (follower_id, created_at DESC, id DESC);

CREATE INDEX IF NOT EXISTS idx_user_follows_following_cursor 
    ON user_follows (following_id, created_at DESC, id DESC);


-- 2. Mutual User Connections (Request / Accept / Decline lifecycle)
CREATE TABLE IF NOT EXISTS user_connections (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    requester_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    addressee_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    responded_at TIMESTAMP WITH TIME ZONE,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_no_self_connection CHECK (requester_id <> addressee_id),
    CONSTRAINT chk_user_connections_status CHECK (status IN ('PENDING', 'ACCEPTED', 'DECLINED', 'CANCELLED'))
);

-- Deterministic pair uniqueness: prevents concurrent reverse requests (A->B and B->A) from coexisting as pending or accepted
CREATE UNIQUE INDEX IF NOT EXISTS idx_user_connections_active_pair 
    ON user_connections (LEAST(requester_id, addressee_id), GREATEST(requester_id, addressee_id)) 
    WHERE status IN ('PENDING', 'ACCEPTED');

CREATE INDEX IF NOT EXISTS idx_user_connections_requester_cursor 
    ON user_connections (requester_id, status, created_at DESC, id DESC);

CREATE INDEX IF NOT EXISTS idx_user_connections_addressee_cursor 
    ON user_connections (addressee_id, status, created_at DESC, id DESC);


-- 3. Directional User Blocks (Security Primitive)
CREATE TABLE IF NOT EXISTS user_blocks (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    blocker_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    blocked_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_no_self_block CHECK (blocker_id <> blocked_id),
    CONSTRAINT uq_user_blocks UNIQUE (blocker_id, blocked_id)
);

CREATE INDEX IF NOT EXISTS idx_user_blocks_blocker_cursor 
    ON user_blocks (blocker_id, created_at DESC, id DESC);

CREATE INDEX IF NOT EXISTS idx_user_blocks_blocked 
    ON user_blocks (blocked_id);


-- 4. Directional User Mutes (Private Feed Suppression)
CREATE TABLE IF NOT EXISTS user_mutes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    muter_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    muted_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_no_self_mute CHECK (muter_id <> muted_id),
    CONSTRAINT uq_user_mutes UNIQUE (muter_id, muted_id)
);

CREATE INDEX IF NOT EXISTS idx_user_mutes_muter_cursor 
    ON user_mutes (muter_id, created_at DESC, id DESC);
