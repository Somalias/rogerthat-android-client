ALTER TABLE my_identity ADD COLUMN avatar_id INTEGER NOT NULL DEFAULT -1;

ALTER TABLE friend ADD COLUMN user_data TEXT;
ALTER TABLE friend ADD COLUMN app_data TEXT;
