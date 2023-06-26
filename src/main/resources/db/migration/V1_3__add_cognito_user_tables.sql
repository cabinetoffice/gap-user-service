CREATE TABLE IF NOT EXISTS gap_users (
  email VARCHAR(512),
  gap_user_id VARCHAR(64),
  sub VARCHAR(64),
  roles VARCHAR(64),
  dept_id VARCHAR(64),
  PRIMARY KEY (sub)
);

CREATE TABLE IF NOT EXISTS departments (
  id VARCHAR(64),
  name VARCHAR(64),
  GGIS_id VARCHAR(64),
  PRIMARY KEY (id)
);