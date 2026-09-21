CREATE TABLE dongbac.users (
    id UUID PRIMARY KEY,
    email VARCHAR(320) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    full_name VARCHAR(160) NOT NULL,
    role VARCHAR(16) NOT NULL,
    status VARCHAR(16) NOT NULL,
    must_change_password BOOLEAN NOT NULL DEFAULT TRUE,
    failed_login_attempts INTEGER NOT NULL DEFAULT 0,
    locked_until TIMESTAMPTZ NULL,
    last_login_at TIMESTAMPTZ NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    created_by UUID NULL,
    CONSTRAINT uk_users_email UNIQUE (email),
    CONSTRAINT fk_users_created_by FOREIGN KEY (created_by) REFERENCES dongbac.users (id),
    CONSTRAINT ck_users_role CHECK (role IN ('ADMIN', 'STAFF')),
    CONSTRAINT ck_users_status CHECK (status IN ('ACTIVE', 'LOCKED')),
    CONSTRAINT ck_users_failed_login_attempts CHECK (failed_login_attempts >= 0),
    CONSTRAINT ck_users_email_normalized CHECK (email = lower(btrim(email)))
);

CREATE INDEX ix_users_role ON dongbac.users (role);
CREATE INDEX ix_users_status ON dongbac.users (status);
CREATE INDEX ix_users_created_by ON dongbac.users (created_by);

CREATE TABLE dongbac.refresh_tokens (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    token_hash VARCHAR(64) NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    revoked_at TIMESTAMPTZ NULL,
    created_at TIMESTAMPTZ NOT NULL,
    last_used_at TIMESTAMPTZ NULL,
    replacement_token_id UUID NULL,
    CONSTRAINT uk_refresh_tokens_token_hash UNIQUE (token_hash),
    CONSTRAINT fk_refresh_tokens_user FOREIGN KEY (user_id) REFERENCES dongbac.users (id),
    CONSTRAINT fk_refresh_tokens_replacement FOREIGN KEY (replacement_token_id) REFERENCES dongbac.refresh_tokens (id)
);

CREATE INDEX ix_refresh_tokens_user_id ON dongbac.refresh_tokens (user_id);
CREATE INDEX ix_refresh_tokens_expires_at ON dongbac.refresh_tokens (expires_at);
CREATE INDEX ix_refresh_tokens_revoked_at ON dongbac.refresh_tokens (revoked_at);
