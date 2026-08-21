CREATE TABLE app_user (
  id UUID PRIMARY KEY,
  email VARCHAR(190) NOT NULL UNIQUE,
  password_hash VARCHAR(255) NOT NULL,
  display_name VARCHAR(120) NOT NULL,
  role VARCHAR(30) NOT NULL CHECK (role IN ('SUPER_ADMIN','OWNER')),
  enabled BOOLEAN NOT NULL DEFAULT TRUE,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE event (
  id UUID PRIMARY KEY,
  owner_id UUID NOT NULL REFERENCES app_user(id),
  slug VARCHAR(140) NOT NULL UNIQUE,
  names VARCHAR(180) NOT NULL,
  quote VARCHAR(500),
  event_date DATE,
  expires_at TIMESTAMPTZ NOT NULL,
  media_delete_at TIMESTAMPTZ NOT NULL,
  active BOOLEAN NOT NULL DEFAULT TRUE,
  template_key VARCHAR(50) NOT NULL DEFAULT 'elegant',
  background_image_url TEXT,
  primary_color VARCHAR(20) NOT NULL DEFAULT '#FFFFFF',
  accent_color VARCHAR(20) NOT NULL DEFAULT '#C8A96B',
  text_color VARCHAR(20) NOT NULL DEFAULT '#FFFFFF',
  overlay_opacity DOUBLE PRECISION NOT NULL DEFAULT 0.42,
  font_family VARCHAR(120) NOT NULL DEFAULT 'Georgia, serif',
  button_radius_px INTEGER NOT NULL DEFAULT 999,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_event_owner ON event(owner_id);

CREATE TABLE media (
  id UUID PRIMARY KEY,
  event_id UUID NOT NULL REFERENCES event(id) ON DELETE CASCADE,
  storage_key TEXT NOT NULL UNIQUE,
  original_file_name VARCHAR(255) NOT NULL,
  safe_display_name VARCHAR(255) NOT NULL,
  media_type VARCHAR(20) NOT NULL CHECK (media_type IN ('IMAGE','VIDEO')),
  mime_type VARCHAR(100) NOT NULL,
  file_size BIGINT NOT NULL,
  visibility VARCHAR(20) NOT NULL CHECK (visibility IN ('PUBLIC','PRIVATE')),
  guest_name VARCHAR(100),
  status VARCHAR(30) NOT NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  ready_at TIMESTAMPTZ,
  rejection_reason VARCHAR(80)
);
CREATE INDEX idx_media_event_status_visibility ON media(event_id,status,visibility,created_at DESC);

CREATE TABLE media_like (
  id UUID PRIMARY KEY,
  media_id UUID NOT NULL REFERENCES media(id) ON DELETE CASCADE,
  visitor_hash VARCHAR(128) NOT NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  CONSTRAINT uq_media_like UNIQUE(media_id, visitor_hash)
);

CREATE TABLE wish (
  id UUID PRIMARY KEY,
  event_id UUID NOT NULL REFERENCES event(id) ON DELETE CASCADE,
  guest_name VARCHAR(100),
  message VARCHAR(1000) NOT NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_wish_event ON wish(event_id,created_at DESC);
