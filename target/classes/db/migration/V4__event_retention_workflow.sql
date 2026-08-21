ALTER TABLE event ADD COLUMN retention_status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE';
ALTER TABLE event ADD COLUMN deletion_attempts INTEGER NOT NULL DEFAULT 0;
ALTER TABLE event ADD COLUMN deletion_started_at TIMESTAMPTZ;
ALTER TABLE event ADD COLUMN deleted_at TIMESTAMPTZ;
ALTER TABLE event ADD COLUMN last_deletion_error VARCHAR(500);
ALTER TABLE event ADD CONSTRAINT chk_event_retention_status CHECK (retention_status IN ('ACTIVE','PENDING_DELETION','DELETING','DELETION_FAILED','ARCHIVED'));
CREATE INDEX idx_event_retention_due ON event(retention_status, media_delete_at);
