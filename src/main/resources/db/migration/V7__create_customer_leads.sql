CREATE TABLE dongbac.customer_leads (
    id UUID PRIMARY KEY,
    full_name VARCHAR(160) NOT NULL,
    phone VARCHAR(64) NULL,
    email VARCHAR(320) NULL,
    company_name VARCHAR(200) NULL,
    message TEXT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'NEW',
    assigned_to UUID NULL,
    internal_note TEXT NULL,
    created_by UUID NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL,
    CONSTRAINT fk_customer_leads_assigned_to FOREIGN KEY (assigned_to) REFERENCES dongbac.users (id),
    CONSTRAINT fk_customer_leads_created_by FOREIGN KEY (created_by) REFERENCES dongbac.users (id),
    CONSTRAINT ck_customer_leads_full_name CHECK (char_length(trim(full_name)) > 0),
    CONSTRAINT ck_customer_leads_contact CHECK (
        (phone IS NOT NULL AND char_length(trim(phone)) > 0)
        OR (email IS NOT NULL AND char_length(trim(email)) > 0)
    ),
    CONSTRAINT ck_customer_leads_status CHECK (
        status IN ('NEW', 'CONTACTED', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED', 'SPAM')
    ),
    CONSTRAINT ck_customer_leads_message_length CHECK (message IS NULL OR char_length(message) <= 5000),
    CONSTRAINT ck_customer_leads_internal_note_length CHECK (internal_note IS NULL OR char_length(internal_note) <= 5000)
);

CREATE INDEX ix_customer_leads_status ON dongbac.customer_leads (status);
CREATE INDEX ix_customer_leads_assigned_to ON dongbac.customer_leads (assigned_to);
CREATE INDEX ix_customer_leads_created_at ON dongbac.customer_leads (created_at DESC, id DESC);
