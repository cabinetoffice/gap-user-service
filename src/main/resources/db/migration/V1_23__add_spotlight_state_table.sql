CREATE TABLE IF NOT EXISTS public.spotlight_oauth_state
(
    state_id integer UNIQUE default(1),
    state VARCHAR(255),
    last_updated timestamp(6) without time zone,
    CONSTRAINT spotlight_oauth_state_single_row CHECK (state_id = 1)
);
INSERT INTO spotlight_oauth_state (state) VALUES
    ('');