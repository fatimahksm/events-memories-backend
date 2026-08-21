ALTER TABLE media ADD COLUMN client_upload_id UUID;
ALTER TABLE media ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE media ADD CONSTRAINT uq_media_event_client_upload UNIQUE (event_id, client_upload_id);
CREATE INDEX idx_media_event_ready_cursor ON media(event_id, status, visibility, created_at DESC, id DESC);
