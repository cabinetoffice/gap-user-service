ALTER TABLE roles_users DROP CONSTRAINT unique_user_role;
ALTER TABLE roles_users ADD CONSTRAINT roles_users_test_pkey PRIMARY KEY (roles_id, users_gap_user_id);
CREATE UNIQUE INDEX roles_users_index ON roles_users (roles_id, users_gap_user_id);
