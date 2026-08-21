ALTER TABLE app_user ADD COLUMN access_token VARCHAR(64);
ALTER TABLE app_user ADD CONSTRAINT ux_app_user_access_token UNIQUE (access_token);
