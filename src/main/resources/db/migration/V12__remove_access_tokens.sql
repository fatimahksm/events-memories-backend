ALTER TABLE app_user DROP CONSTRAINT IF EXISTS ux_app_user_access_token;
ALTER TABLE app_user DROP COLUMN IF EXISTS access_token;
ALTER TABLE event DROP CONSTRAINT IF EXISTS ux_event_access_token;
ALTER TABLE event DROP COLUMN IF EXISTS access_token;
