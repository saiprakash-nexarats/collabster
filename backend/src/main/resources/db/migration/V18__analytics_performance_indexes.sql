-- V18__analytics_performance_indexes.sql
-- Composite performance indexes for dashboard counts and analytics time-series aggregations

CREATE INDEX IF NOT EXISTS idx_comments_comm_created
    ON comments(community_id, created_at);

CREATE INDEX IF NOT EXISTS idx_comments_comm_moderation
    ON comments(community_id, moderation_status);

CREATE INDEX IF NOT EXISTS idx_posts_comm_moderation
    ON posts(community_id, moderation_status);

CREATE INDEX IF NOT EXISTS idx_resources_comm_created
    ON resources(community_id, created_at);

CREATE INDEX IF NOT EXISTS idx_resources_comm_moderation
    ON resources(community_id, moderation_status);

CREATE INDEX IF NOT EXISTS idx_memberships_comm_score_user
    ON memberships(community_id, contribution_score DESC, user_id ASC);

CREATE INDEX IF NOT EXISTS idx_memberships_comm_joined_at
    ON memberships(community_id, joined_at);

CREATE INDEX IF NOT EXISTS idx_channel_messages_comm_channel_created
    ON community_channel_messages(community_id, channel_id, created_at);
