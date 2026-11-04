CREATE TABLE IF NOT EXISTS community_invitations (
    id UUID PRIMARY KEY,
    community_id UUID NOT NULL REFERENCES communities(id) ON DELETE CASCADE,
    inviter_id BIGINT NOT NULL,
    invitee_id BIGINT NOT NULL,
    token_hash VARCHAR(64) NOT NULL UNIQUE,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMPTZ NOT NULL,
    accepted_at TIMESTAMPTZ,
    revoked_at TIMESTAMPTZ
);

CREATE INDEX IF NOT EXISTS idx_community_invitation_invitee_status
    ON community_invitations(invitee_id, status);
CREATE INDEX IF NOT EXISTS idx_community_invitation_community_status
    ON community_invitations(community_id, status);

CREATE INDEX IF NOT EXISTS idx_memberships_user_status_community
    ON memberships(user_id, status, community_id);

-- Restore the owner role from the authoritative community.owner_id field before
-- enforcing one owner membership per community.
UPDATE memberships m
SET role = 'ADMIN'
FROM communities c
WHERE m.community_id = c.id AND m.role = 'OWNER' AND m.user_id <> c.owner_id;

UPDATE memberships m
SET role = 'OWNER'
FROM communities c
WHERE m.community_id = c.id AND m.user_id = c.owner_id;

CREATE UNIQUE INDEX IF NOT EXISTS uq_membership_one_owner
    ON memberships(community_id) WHERE role = 'OWNER';
