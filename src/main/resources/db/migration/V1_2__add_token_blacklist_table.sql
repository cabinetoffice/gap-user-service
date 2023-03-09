create table token_blacklist (id integer not null, expiry_date timestamp(6), jwt varchar(4000), primary key (id));
create index IDXs5liqeg06eex956w4sv6wuwau on token_blacklist (jwt);