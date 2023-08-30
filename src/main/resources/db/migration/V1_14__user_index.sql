CREATE INDEX IF NOT EXISTS email
    ON public.gap_users USING btree
    (email COLLATE pg_catalog."default" ASC NULLS LAST);