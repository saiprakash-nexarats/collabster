-- Phase 11 Performance and Stable Pagination Indexes

-- 1. Composite index for community member listing by status (findByCommunityIdAndStatus)
CREATE INDEX IF NOT EXISTS idx_memberships_comm_status
    ON memberships(community_id, status);

-- 2. Composite index for community channel message listing with stable pagination tiebreaker
CREATE INDEX IF NOT EXISTS idx_channel_messages_comm_channel_desc
    ON community_channel_messages(community_id, channel_id, created_at DESC, id DESC);
