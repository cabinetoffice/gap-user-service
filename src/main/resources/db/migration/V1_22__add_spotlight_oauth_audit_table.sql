CREATE TABLE spotlight_oauth_audit (
    id SERIAL PRIMARY KEY,
    event VARCHAR(255),
    status VARCHAR(255),
    user_id integer,
    timestamp timestamp without time zone,

    CONSTRAINT fk_spotlight_oauth_audit_user
        FOREIGN KEY (user_id)
        REFERENCES gap_users(gap_user_id)
);

-- We're going to be filtering on type and timestamp, so we need indexes on those columns
CREATE INDEX idx_spotlight_oauth_audit_event ON spotlight_oauth_audit(event);
CREATE INDEX idx_spotlight_oauth_audit_status ON spotlight_oauth_audit(status);
CREATE INDEX idx_spotlight_oauth_audit_timestamp ON spotlight_oauth_audit(timestamp);