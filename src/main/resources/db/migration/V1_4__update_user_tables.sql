DROP TABLE user_roles;
DROP TABLE gap_users;
DROP TABLE departments;
DROP TABLE roles;

CREATE TABLE departments (
	id serial4 NOT NULL,
	ggis_id varchar(255) NULL,
	"name" varchar(255) NULL,
	CONSTRAINT departments_pkey PRIMARY KEY (id)
);

CREATE TABLE gap_users (
	gap_user_id serial4 NOT NULL,
	email varchar(255) NULL,
	sub varchar(255) NULL,
	dept_id int4 NULL,
	CONSTRAINT gap_users_pkey PRIMARY KEY (gap_user_id),
	CONSTRAINT fk9oau3dd02gl4ersfjey0pg5d5 FOREIGN KEY (dept_id) REFERENCES departments(id)
);

CREATE TABLE roles (
	id serial4 NOT NULL,
	"name" varchar(255) NULL,
	CONSTRAINT roles_pkey PRIMARY KEY (id)
);

CREATE TABLE roles_users (
	roles_id int4 NOT NULL,
	users_gap_user_id int4 NOT NULL,
	CONSTRAINT fk2mck5s7km22t2on8h2jpn44xq FOREIGN KEY (roles_id) REFERENCES roles(id) ON DELETE CASCADE,
	CONSTRAINT fkhu2gdj9we2ucvgwy1qdfm5a5s FOREIGN KEY (users_gap_user_id) REFERENCES gap_users(gap_user_id) ON DELETE CASCADE
);

INSERT INTO roles (name) VALUES
  ('APPLICANT'),
  ('FIND'),
  ('ADMIN'),
  ('SUPER_ADMIN');
