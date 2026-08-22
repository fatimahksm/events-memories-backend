CREATE TABLE audit_log (
  id UUID PRIMARY KEY,
  actor_id UUID,
  actor_email VARCHAR(190) NOT NULL,
  action VARCHAR(60) NOT NULL,
  target_type VARCHAR(40) NOT NULL,
  target_id UUID,
  details VARCHAR(500),
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_audit_log_created_at ON audit_log(created_at DESC);
