INSERT INTO public.gap_users(gap_user_id, email, sub, dept_id,
login_journey_state, cola_sub, created)
VALUES
(1, 'test.applicant@gov.uk',
'urn:fdc:gov.uk:2022:ibd2rz2CgyidndXyq2zyfcnQwyYI57h34vMlSr88AAa', null,
'PRIVACY_POLICY_PENDING', null, null),
(2, 'test.admin@gov.uk',
'urn:fdc:gov.uk:2022:ibd2rz2CgyidndXyq2zyfcnQwyYI57h34vMlSr22TTt', 1,
'PRIVACY_POLICY_PENDING', null, null),
(3, 'test.super-admin@gov.uk',
'urn:fdc:gov.uk:2022:ibd2rz2CgyidndXyq2zyfcnQwyYI57h34vMlSr97YUg', 1,
'PRIVACY_POLICY_PENDING', null, null),
(4, 'test.tech-supporter@gov.uk',
 'urn:fdc:gov.uk:2022:ibd2rz2CgyidndXyq2zyfcnQwyYI57h34vMlSr00Sup', 1,
  'PRIVACY_POLICY_PENDING', null, null);



INSERT INTO roles_users (roles_id, users_gap_user_id) VALUES(1, 1);
INSERT INTO roles_users (roles_id, users_gap_user_id) VALUES(2, 1);
INSERT INTO roles_users (roles_id, users_gap_user_id) VALUES(1, 2);
INSERT INTO roles_users (roles_id, users_gap_user_id) VALUES(2, 2);
INSERT INTO roles_users (roles_id, users_gap_user_id) VALUES(3, 2);
INSERT INTO roles_users (roles_id, users_gap_user_id) VALUES(2, 3);
INSERT INTO roles_users (roles_id, users_gap_user_id) VALUES(3, 3);
INSERT INTO roles_users (roles_id, users_gap_user_id) VALUES(4, 3);
INSERT INTO roles_users (roles_id, users_gap_user_id) VALUES(5, 3);
INSERT INTO roles_users (roles_id, users_gap_user_id) VALUES(1, 3);
INSERT INTO roles_users (roles_id, users_gap_user_id) VALUES(1, 4);
INSERT INTO roles_users (roles_id, users_gap_user_id) VALUES(2, 4);
INSERT INTO roles_users (roles_id, users_gap_user_id) VALUES(5, 4);


--remember to create the TECH_SUPPORTER user in the gapapply DB in the tech_support_user table
--INSERT INTO tech_support_user (id, funder_id, user_sub) VALUES(1, 1, 'urn:fdc:gov.uk:2022:ibd2rz2CgyidndXyq2zyfcnQwyYI57h34vMlSr00Sup');