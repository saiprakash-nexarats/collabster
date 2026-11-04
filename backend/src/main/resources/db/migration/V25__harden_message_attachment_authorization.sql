-- V25: Harden Message Attachment Authorization & Media Ownership

CREATE TABLE IF NOT EXISTS uploaded_media (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    owner_user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    storage_key VARCHAR(1024) NOT NULL UNIQUE,
    media_type VARCHAR(32) NOT NULL,
    mime_type VARCHAR(128) NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    file_size BIGINT NOT NULL,
    upload_status VARCHAR(32) NOT NULL DEFAULT 'COMPLETED',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_uploaded_media_type CHECK (media_type IN ('IMAGE', 'VIDEO', 'AUDIO', 'DOCUMENT')),
    CONSTRAINT chk_uploaded_media_status CHECK (upload_status IN ('PENDING', 'COMPLETED', 'FAILED'))
);

CREATE INDEX IF NOT EXISTS idx_uploaded_media_owner ON uploaded_media(owner_user_id);
CREATE INDEX IF NOT EXISTS idx_uploaded_media_key ON uploaded_media(storage_key);
