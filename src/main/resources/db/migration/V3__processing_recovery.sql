ALTER TABLE media ADD COLUMN processing_started_at TIMESTAMPTZ;
CREATE INDEX idx_media_processing_recovery ON media(status, processing_started_at, created_at);
