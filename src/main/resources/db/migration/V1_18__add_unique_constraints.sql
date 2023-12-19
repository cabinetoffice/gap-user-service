ALTER TABLE gap_users
    ADD CONSTRAINT unique_email UNIQUE (email);
ALTER TABLE gap_users
    ADD CONSTRAINT unique_sub UNIQUE (sub);
ALTER TABLE gap_users
    ADD CONSTRAINT unique_cola_sub UNIQUE (cola_sub);
ALTER TABLE roles_users
    ADD CONSTRAINT unique_user_role UNIQUE (roles_id, users_gap_user_id);