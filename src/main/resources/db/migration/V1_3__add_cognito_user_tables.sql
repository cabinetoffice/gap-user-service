CREATE TABLE IF NOT EXISTS gap_users (
  hashedEmail VARCHAR(512),
  encryptedEmail VARCHAR(1500),
  gap_user_id VARCHAR(64),
  sub VARCHAR(64),
  dept_id VARCHAR(64),
  PRIMARY KEY (sub)
);

CREATE TABLE IF NOT EXISTS departments (
  id VARCHAR(64),
  name VARCHAR(256),
  ggis_id VARCHAR(64),
  PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS roles (
  id VARCHAR(64),
  name VARCHAR(64),
  PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS user_roles (
  id VARCHAR(64),
  user_sub VARCHAR(64),
  role_id VARCHAR(64),
  PRIMARY KEY (id)
);