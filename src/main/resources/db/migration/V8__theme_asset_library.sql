CREATE TABLE theme_asset (
  id UUID PRIMARY KEY,
  url VARCHAR(500) NOT NULL,
  content_type VARCHAR(100) NOT NULL,
  size_bytes BIGINT NOT NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_theme_asset_created ON theme_asset(created_at DESC);
