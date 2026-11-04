CREATE TABLE IF NOT EXISTS community_rules (
    id UUID PRIMARY KEY,
    community_id UUID NOT NULL REFERENCES communities(id) ON DELETE CASCADE,
    title VARCHAR(160) NOT NULL,
    body TEXT NOT NULL,
    position INTEGER NOT NULL,
    version INTEGER NOT NULL DEFAULT 1,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_by BIGINT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_community_rule_order ON community_rules(community_id, active, position);

CREATE TABLE IF NOT EXISTS community_rule_revisions (
    id UUID PRIMARY KEY,
    rule_id UUID NOT NULL REFERENCES community_rules(id) ON DELETE CASCADE,
    version INTEGER NOT NULL,
    title VARCHAR(160) NOT NULL,
    body TEXT NOT NULL,
    active BOOLEAN NOT NULL,
    changed_by BIGINT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_community_rule_revision UNIQUE(rule_id, version)
);
