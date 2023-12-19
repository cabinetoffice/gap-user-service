CREATE SEQUENCE IF NOT EXISTS nonces_nonce_id_seq
    INCREMENT 1
    START 1;

CREATE TABLE IF NOT EXISTS public.nonces
(
    nonce_id   integer NOT NULL DEFAULT nextval('nonces_nonce_id_seq'::regclass),
    created_at timestamp(6) without time zone,
    nonce      character varying(255) COLLATE pg_catalog."default",
    CONSTRAINT nonces_pkey PRIMARY KEY (nonce_id)
)