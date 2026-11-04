-- Phase 11.1 Community Member Pagination Covering-Order Index
-- Replaces idx_memberships_comm_status with covering-order composite index
-- to eliminate sorting overhead for large membership listings.

CREATE INDEX IF NOT EXISTS idx_memberships_comm_status_joined_id
    ON memberships (community_id, status, joined_at DESC, id DESC);

DROP INDEX IF EXISTS idx_memberships_comm_status;
