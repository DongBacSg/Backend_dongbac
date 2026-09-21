CREATE TABLE dongbac.media_upload_intents (
    id UUID PRIMARY KEY,
    public_id VARCHAR(160) NOT NULL,
    folder VARCHAR(255) NOT NULL,
    media_type VARCHAR(16) NOT NULL,
    original_filename VARCHAR(255) NULL,
    requested_by UUID NOT NULL,
    status VARCHAR(16) NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    completed_at TIMESTAMPTZ NULL,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT fk_media_upload_intents_requested_by FOREIGN KEY (requested_by) REFERENCES dongbac.users (id),
    CONSTRAINT ck_media_upload_intents_media_type CHECK (media_type IN ('IMAGE', 'VIDEO')),
    CONSTRAINT ck_media_upload_intents_status CHECK (status IN ('PENDING', 'COMPLETED', 'FAILED', 'EXPIRED'))
);

CREATE INDEX ix_media_upload_intents_requested_by ON dongbac.media_upload_intents (requested_by);
CREATE INDEX ix_media_upload_intents_status ON dongbac.media_upload_intents (status);
CREATE INDEX ix_media_upload_intents_expires_at ON dongbac.media_upload_intents (expires_at);

CREATE TABLE dongbac.media (
    id UUID PRIMARY KEY,
    asset_id VARCHAR(255) NOT NULL,
    public_id VARCHAR(255) NOT NULL,
    resource_type VARCHAR(16) NOT NULL,
    format VARCHAR(32) NOT NULL,
    secure_url TEXT NOT NULL,
    width INTEGER NULL,
    height INTEGER NULL,
    bytes BIGINT NOT NULL,
    duration_seconds NUMERIC(12, 3) NULL,
    folder VARCHAR(255) NULL,
    original_filename VARCHAR(255) NULL,
    alt_text VARCHAR(255) NULL,
    status VARCHAR(16) NOT NULL,
    uploaded_by UUID NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    deleted_at TIMESTAMPTZ NULL,
    deleted_by UUID NULL,
    version BIGINT NOT NULL,
    CONSTRAINT uk_media_asset_id UNIQUE (asset_id),
    CONSTRAINT uk_media_resource_public_id UNIQUE (resource_type, public_id),
    CONSTRAINT fk_media_uploaded_by FOREIGN KEY (uploaded_by) REFERENCES dongbac.users (id),
    CONSTRAINT fk_media_deleted_by FOREIGN KEY (deleted_by) REFERENCES dongbac.users (id),
    CONSTRAINT ck_media_resource_type CHECK (resource_type IN ('IMAGE', 'VIDEO')),
    CONSTRAINT ck_media_status CHECK (status IN ('ACTIVE', 'DELETED')),
    CONSTRAINT ck_media_bytes CHECK (bytes >= 0)
);

CREATE INDEX ix_media_resource_type ON dongbac.media (resource_type);
CREATE INDEX ix_media_status ON dongbac.media (status);
CREATE INDEX ix_media_uploaded_by ON dongbac.media (uploaded_by);
CREATE INDEX ix_media_created_at ON dongbac.media (created_at);

CREATE TABLE dongbac.site_banners (
    id UUID PRIMARY KEY,
    media_id UUID NOT NULL,
    alt_text VARCHAR(255) NULL,
    sort_order INTEGER NOT NULL,
    active BOOLEAN NOT NULL,
    created_by UUID NOT NULL,
    updated_by UUID NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL,
    CONSTRAINT fk_site_banners_media FOREIGN KEY (media_id) REFERENCES dongbac.media (id),
    CONSTRAINT fk_site_banners_created_by FOREIGN KEY (created_by) REFERENCES dongbac.users (id),
    CONSTRAINT fk_site_banners_updated_by FOREIGN KEY (updated_by) REFERENCES dongbac.users (id),
    CONSTRAINT ck_site_banners_sort_order CHECK (sort_order >= 0)
);

CREATE INDEX ix_site_banners_active_sort ON dongbac.site_banners (active, sort_order);
CREATE INDEX ix_site_banners_media_id ON dongbac.site_banners (media_id);

CREATE TABLE dongbac.site_partners (
    id UUID PRIMARY KEY,
    name VARCHAR(160) NOT NULL,
    logo_media_id UUID NOT NULL,
    website_url TEXT NULL,
    sort_order INTEGER NOT NULL,
    active BOOLEAN NOT NULL,
    created_by UUID NOT NULL,
    updated_by UUID NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL,
    CONSTRAINT fk_site_partners_logo_media FOREIGN KEY (logo_media_id) REFERENCES dongbac.media (id),
    CONSTRAINT fk_site_partners_created_by FOREIGN KEY (created_by) REFERENCES dongbac.users (id),
    CONSTRAINT fk_site_partners_updated_by FOREIGN KEY (updated_by) REFERENCES dongbac.users (id),
    CONSTRAINT ck_site_partners_sort_order CHECK (sort_order >= 0)
);

CREATE INDEX ix_site_partners_active_sort ON dongbac.site_partners (active, sort_order);
CREATE INDEX ix_site_partners_logo_media_id ON dongbac.site_partners (logo_media_id);

CREATE TABLE dongbac.site_settings (
    id UUID PRIMARY KEY,
    company_name VARCHAR(160) NULL,
    slogan TEXT NULL,
    vision TEXT NULL,
    mission TEXT NULL,
    philosophy TEXT NULL,
    brand_narrative TEXT NULL,
    core_values TEXT NULL,
    logo_media_id UUID NULL,
    office_address TEXT NULL,
    factory_address TEXT NULL,
    phone VARCHAR(64) NULL,
    email VARCHAR(320) NULL,
    facebook_url TEXT NULL,
    youtube_url TEXT NULL,
    zalo_url TEXT NULL,
    map_url TEXT NULL,
    tvc_url TEXT NULL,
    updated_by UUID NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL,
    CONSTRAINT fk_site_settings_logo_media FOREIGN KEY (logo_media_id) REFERENCES dongbac.media (id),
    CONSTRAINT fk_site_settings_updated_by FOREIGN KEY (updated_by) REFERENCES dongbac.users (id)
);

CREATE TABLE dongbac.manufacturing_page (
    id UUID PRIMARY KEY,
    title VARCHAR(160) NULL,
    introduction TEXT NULL,
    hero_media_id UUID NULL,
    updated_by UUID NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL,
    CONSTRAINT fk_manufacturing_page_hero_media FOREIGN KEY (hero_media_id) REFERENCES dongbac.media (id),
    CONSTRAINT fk_manufacturing_page_updated_by FOREIGN KEY (updated_by) REFERENCES dongbac.users (id)
);

CREATE TABLE dongbac.manufacturing_sections (
    id UUID PRIMARY KEY,
    title VARCHAR(160) NOT NULL,
    content TEXT NULL,
    media_id UUID NULL,
    sort_order INTEGER NOT NULL,
    active BOOLEAN NOT NULL,
    created_by UUID NOT NULL,
    updated_by UUID NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL,
    CONSTRAINT fk_manufacturing_sections_media FOREIGN KEY (media_id) REFERENCES dongbac.media (id),
    CONSTRAINT fk_manufacturing_sections_created_by FOREIGN KEY (created_by) REFERENCES dongbac.users (id),
    CONSTRAINT fk_manufacturing_sections_updated_by FOREIGN KEY (updated_by) REFERENCES dongbac.users (id),
    CONSTRAINT ck_manufacturing_sections_sort_order CHECK (sort_order >= 0)
);

CREATE INDEX ix_manufacturing_sections_active_sort ON dongbac.manufacturing_sections (active, sort_order);
CREATE INDEX ix_manufacturing_sections_media_id ON dongbac.manufacturing_sections (media_id);
