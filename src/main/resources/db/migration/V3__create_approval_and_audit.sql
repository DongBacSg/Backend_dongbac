CREATE TABLE dongbac.approval_requests (
    id UUID PRIMARY KEY,
    resource_type VARCHAR(32) NOT NULL,
    resource_id UUID NOT NULL,
    resource_version BIGINT NOT NULL,
    status VARCHAR(32) NOT NULL,
    submitted_by UUID NOT NULL,
    submitted_at TIMESTAMPTZ NOT NULL,
    reviewed_by UUID NULL,
    reviewed_at TIMESTAMPTZ NULL,
    review_note TEXT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL,
    CONSTRAINT fk_approval_requests_submitted_by FOREIGN KEY (submitted_by) REFERENCES dongbac.users (id),
    CONSTRAINT fk_approval_requests_reviewed_by FOREIGN KEY (reviewed_by) REFERENCES dongbac.users (id),
    CONSTRAINT ck_approval_requests_resource_type CHECK (resource_type IN ('PRODUCT', 'ARTICLE')),
    CONSTRAINT ck_approval_requests_status CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED', 'CANCELLED')),
    CONSTRAINT ck_approval_requests_resource_version CHECK (resource_version >= 1),
    CONSTRAINT ck_approval_requests_review_note_length CHECK (review_note IS NULL OR char_length(review_note) <= 1000)
);

CREATE UNIQUE INDEX uk_approval_requests_pending_resource
    ON dongbac.approval_requests (resource_type, resource_id, resource_version)
    WHERE status = 'PENDING';

CREATE INDEX ix_approval_requests_status ON dongbac.approval_requests (status);
CREATE INDEX ix_approval_requests_resource_type ON dongbac.approval_requests (resource_type);
CREATE INDEX ix_approval_requests_submitted_by ON dongbac.approval_requests (submitted_by);
CREATE INDEX ix_approval_requests_submitted_at ON dongbac.approval_requests (submitted_at);
CREATE INDEX ix_approval_requests_reviewed_by ON dongbac.approval_requests (reviewed_by);

CREATE TABLE dongbac.audit_logs (
    id UUID PRIMARY KEY,
    occurred_at TIMESTAMPTZ NOT NULL,
    actor_user_id UUID NULL,
    actor_email_snapshot VARCHAR(320) NULL,
    actor_role_snapshot VARCHAR(16) NULL,
    action VARCHAR(64) NOT NULL,
    target_type VARCHAR(64) NOT NULL,
    target_id UUID NULL,
    outcome VARCHAR(16) NOT NULL,
    reason TEXT NULL,
    correlation_id VARCHAR(100) NULL,
    ip_address VARCHAR(128) NULL,
    user_agent TEXT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT fk_audit_logs_actor_user FOREIGN KEY (actor_user_id) REFERENCES dongbac.users (id),
    CONSTRAINT ck_audit_logs_outcome CHECK (outcome IN ('SUCCESS', 'FAILURE')),
    CONSTRAINT ck_audit_logs_reason_length CHECK (reason IS NULL OR char_length(reason) <= 1000)
);

CREATE INDEX ix_audit_logs_occurred_at ON dongbac.audit_logs (occurred_at);
CREATE INDEX ix_audit_logs_actor_user_id ON dongbac.audit_logs (actor_user_id);
CREATE INDEX ix_audit_logs_action ON dongbac.audit_logs (action);
CREATE INDEX ix_audit_logs_target ON dongbac.audit_logs (target_type, target_id);
CREATE INDEX ix_audit_logs_outcome ON dongbac.audit_logs (outcome);
