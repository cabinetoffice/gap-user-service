ALTER TABLE gap_users ADD COLUMN accepted_privacy_policy BOOLEAN DEFAULT FALSE;

UPDATE gap_users SET accepted_privacy_policy = FALSE;