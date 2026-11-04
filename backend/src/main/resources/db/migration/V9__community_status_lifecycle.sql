ALTER TABLE communities
    ADD COLUMN IF NOT EXISTS community_status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE';
ALTER TABLE communities
    ADD COLUMN IF NOT EXISTS deletion_requested_at TIMESTAMPTZ;

UPDATE communities SET community_status = 'ARCHIVED'
WHERE archived_at IS NOT NULL AND community_status = 'ACTIVE';

CREATE INDEX IF NOT EXISTS idx_communities_status_deletion
    ON communities(community_status, deletion_requested_at);
