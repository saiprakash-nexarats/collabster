-- V34: Create Storage Objects Schema for Phase 5 Secure Storage Foundation

CREATE TABLE IF NOT EXISTS storage_objects (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    owner_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    workspace_id UUID,
    storage_provider VARCHAR(32) NOT NULL DEFAULT 'LOCAL',
    storage_key VARCHAR(1024) NOT NULL UNIQUE,
    filename VARCHAR(255) NOT NULL,
    content_type VARCHAR(128) NOT NULL,
    expected_bytes BIGINT NOT NULL,
    verified_bytes BIGINT,
    checksum VARCHAR(128),
    upload_state VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP WITH TIME ZONE,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMP WITH TIME ZONE,
    CONSTRAINT chk_storage_object_state CHECK (upload_state IN ('PENDING', 'COMPLETED', 'FAILED', 'EXPIRED', 'QUARANTINED')),
    CONSTRAINT chk_storage_provider CHECK (storage_provider IN ('LOCAL', 'S3', 'MANAGED'))
);

CREATE INDEX IF NOT EXISTS idx_storage_objects_owner ON storage_objects(owner_id);
CREATE INDEX IF NOT EXISTS idx_storage_objects_workspace ON storage_objects(workspace_id);
CREATE INDEX IF NOT EXISTS idx_storage_objects_state ON storage_objects(upload_state);
CREATE INDEX IF NOT EXISTS idx_storage_objects_expires ON storage_objects(expires_at) WHERE upload_state = 'PENDING';

