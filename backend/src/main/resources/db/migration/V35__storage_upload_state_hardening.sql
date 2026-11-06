-- V35: Add UPLOADING state to storage_objects check constraint for concurrency-safe upload claiming

ALTER TABLE storage_objects DROP CONSTRAINT IF EXISTS chk_storage_object_state;

ALTER TABLE storage_objects ADD CONSTRAINT chk_storage_object_state
    CHECK (upload_state IN ('PENDING', 'UPLOADING', 'COMPLETED', 'FAILED', 'EXPIRED', 'QUARANTINED'));

