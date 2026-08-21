ALTER TABLE event ADD COLUMN access_token VARCHAR(64);
ALTER TABLE event ADD CONSTRAINT ux_event_access_token UNIQUE (access_token);
