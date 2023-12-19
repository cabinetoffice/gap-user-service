CREATE TABLE IF NOT EXISTS public.salts
(
    salt_id    character varying(255) COLLATE pg_catalog."default",
    created_at timestamp(6) without time zone,
    salt       character varying(255) COLLATE pg_catalog."default",
    CONSTRAINT salts_pkey PRIMARY KEY (salt_id)
)