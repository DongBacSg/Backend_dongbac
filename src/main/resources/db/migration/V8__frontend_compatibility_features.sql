ALTER TABLE dongbac.customer_leads
    ALTER COLUMN created_by DROP NOT NULL,
    ADD COLUMN subject VARCHAR(240) NULL;

ALTER TABLE dongbac.article_revisions
    DROP CONSTRAINT ck_article_revisions_type;

ALTER TABLE dongbac.article_revisions
    ADD CONSTRAINT ck_article_revisions_type CHECK (
        article_type IN ('INTERNAL_ACTIVITY', 'NEWS', 'KNOWLEDGE', 'RECRUITMENT', 'ANNOUNCEMENT')
    );

CREATE TABLE dongbac.manufacturing_services (
    id UUID PRIMARY KEY,
    slug VARCHAR(180) NOT NULL,
    title VARCHAR(160) NOT NULL,
    summary TEXT NULL,
    content TEXT NULL,
    featured_media_id UUID NULL,
    seo_title VARCHAR(240) NULL,
    seo_description VARCHAR(500) NULL,
    sort_order INTEGER NOT NULL DEFAULT 0,
    active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL,
    CONSTRAINT uk_manufacturing_services_slug UNIQUE (slug),
    CONSTRAINT fk_manufacturing_services_featured_media FOREIGN KEY (featured_media_id) REFERENCES dongbac.media (id),
    CONSTRAINT ck_manufacturing_services_title CHECK (char_length(trim(title)) > 0),
    CONSTRAINT ck_manufacturing_services_sort_order CHECK (sort_order >= 0),
    CONSTRAINT ck_manufacturing_services_summary_length CHECK (summary IS NULL OR char_length(summary) <= 2000),
    CONSTRAINT ck_manufacturing_services_content_length CHECK (content IS NULL OR char_length(content) <= 200000)
);

CREATE INDEX ix_manufacturing_services_active_sort
    ON dongbac.manufacturing_services (active, sort_order, title, id);
CREATE INDEX ix_manufacturing_services_featured_media
    ON dongbac.manufacturing_services (featured_media_id);
