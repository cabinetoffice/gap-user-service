ALTER TABLE gap_users DROP COLUMN accepted_privacy_policy;
ALTER TABLE gap_users ALTER COLUMN login_journey_state SET DEFAULT 'PRIVACY_POLICY_PENDING';
