CREATE SEQUENCE IF NOT EXISTS token_blacklist_seq
    INCREMENT 1
    START 1;

create table if not exists token_blacklist
(
    id          integer not null default nextval('token_blacklist_seq'::regclass),
    expiry_date timestamp(6),
    jwt         varchar(4000),
    primary key (id)
);
create index if not exists IDXs5liqeg06eex956w4sv6wuwau on token_blacklist (jwt);