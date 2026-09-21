CREATE TABLE dongbac.categories (
    id UUID PRIMARY KEY,
    name VARCHAR(160) NOT NULL,
    slug VARCHAR(180) NOT NULL,
    description TEXT NULL,
    sort_order INTEGER NOT NULL DEFAULT 0,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_by UUID NOT NULL,
    updated_by UUID NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL,
    CONSTRAINT uk_categories_slug UNIQUE (slug),
    CONSTRAINT fk_categories_created_by FOREIGN KEY (created_by) REFERENCES dongbac.users (id),
    CONSTRAINT fk_categories_updated_by FOREIGN KEY (updated_by) REFERENCES dongbac.users (id),
    CONSTRAINT ck_categories_sort_order CHECK (sort_order >= 0)
);

CREATE INDEX ix_categories_active_sort ON dongbac.categories (active, sort_order, name);

CREATE TABLE dongbac.products (
    id UUID PRIMARY KEY,
    current_published_revision_id UUID NULL,
    latest_revision_number BIGINT NOT NULL DEFAULT 0,
    publication_status VARCHAR(32) NOT NULL,
    created_by UUID NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL,
    CONSTRAINT fk_products_created_by FOREIGN KEY (created_by) REFERENCES dongbac.users (id),
    CONSTRAINT ck_products_latest_revision_number CHECK (latest_revision_number >= 0),
    CONSTRAINT ck_products_publication_status CHECK (
        publication_status IN ('DRAFT', 'PUBLISHED', 'UNPUBLISHED', 'ARCHIVED')
    )
);

CREATE TABLE dongbac.product_revisions (
    id UUID PRIMARY KEY,
    product_id UUID NOT NULL,
    revision_number BIGINT NOT NULL,
    status VARCHAR(32) NOT NULL,
    category_id UUID NOT NULL,
    name VARCHAR(200) NOT NULL,
    slug VARCHAR(220) NOT NULL,
    short_description TEXT NULL,
    content TEXT NULL,
    seo_title VARCHAR(200) NULL,
    seo_description VARCHAR(500) NULL,
    created_by UUID NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    published_at TIMESTAMPTZ NULL,
    published_by UUID NULL,
    version BIGINT NOT NULL,
    CONSTRAINT uk_product_revisions_number UNIQUE (product_id, revision_number),
    CONSTRAINT fk_product_revisions_product FOREIGN KEY (product_id) REFERENCES dongbac.products (id),
    CONSTRAINT fk_product_revisions_category FOREIGN KEY (category_id) REFERENCES dongbac.categories (id),
    CONSTRAINT fk_product_revisions_created_by FOREIGN KEY (created_by) REFERENCES dongbac.users (id),
    CONSTRAINT fk_product_revisions_published_by FOREIGN KEY (published_by) REFERENCES dongbac.users (id),
    CONSTRAINT ck_product_revisions_number CHECK (revision_number >= 1),
    CONSTRAINT ck_product_revisions_status CHECK (
        status IN ('DRAFT', 'PENDING_REVIEW', 'REJECTED', 'PUBLISHED', 'ARCHIVED')
    )
);

ALTER TABLE dongbac.products
    ADD CONSTRAINT fk_products_current_published_revision
    FOREIGN KEY (current_published_revision_id) REFERENCES dongbac.product_revisions (id);

CREATE INDEX ix_products_publication_status ON dongbac.products (publication_status);
CREATE INDEX ix_products_current_published_revision ON dongbac.products (current_published_revision_id);
CREATE INDEX ix_product_revisions_product ON dongbac.product_revisions (product_id, revision_number DESC);
CREATE INDEX ix_product_revisions_status ON dongbac.product_revisions (status);
CREATE INDEX ix_product_revisions_category ON dongbac.product_revisions (category_id);
CREATE INDEX ix_product_revisions_slug ON dongbac.product_revisions (slug);
CREATE UNIQUE INDEX uk_product_revisions_published_slug
    ON dongbac.product_revisions (slug)
    WHERE status = 'PUBLISHED';
CREATE UNIQUE INDEX uk_product_revisions_one_draft
    ON dongbac.product_revisions (product_id)
    WHERE status = 'DRAFT';
CREATE UNIQUE INDEX uk_product_revisions_one_pending
    ON dongbac.product_revisions (product_id)
    WHERE status = 'PENDING_REVIEW';

CREATE TABLE dongbac.product_revision_images (
    id UUID PRIMARY KEY,
    product_revision_id UUID NOT NULL,
    media_id UUID NOT NULL,
    sort_order INTEGER NOT NULL,
    is_primary BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_product_revision_images_media UNIQUE (product_revision_id, media_id),
    CONSTRAINT fk_product_revision_images_revision FOREIGN KEY (product_revision_id) REFERENCES dongbac.product_revisions (id),
    CONSTRAINT fk_product_revision_images_media FOREIGN KEY (media_id) REFERENCES dongbac.media (id),
    CONSTRAINT ck_product_revision_images_sort_order CHECK (sort_order >= 0)
);

CREATE UNIQUE INDEX uk_product_revision_images_primary
    ON dongbac.product_revision_images (product_revision_id)
    WHERE is_primary = TRUE;
CREATE INDEX ix_product_revision_images_revision_sort
    ON dongbac.product_revision_images (product_revision_id, sort_order);
CREATE INDEX ix_product_revision_images_media ON dongbac.product_revision_images (media_id);

CREATE TABLE dongbac.product_revision_related_products (
    id UUID PRIMARY KEY,
    product_revision_id UUID NOT NULL,
    related_product_id UUID NOT NULL,
    sort_order INTEGER NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_product_revision_related UNIQUE (product_revision_id, related_product_id),
    CONSTRAINT fk_product_revision_related_revision FOREIGN KEY (product_revision_id) REFERENCES dongbac.product_revisions (id),
    CONSTRAINT fk_product_revision_related_product FOREIGN KEY (related_product_id) REFERENCES dongbac.products (id),
    CONSTRAINT ck_product_revision_related_sort_order CHECK (sort_order >= 0)
);

CREATE INDEX ix_product_revision_related_revision_sort
    ON dongbac.product_revision_related_products (product_revision_id, sort_order);
CREATE INDEX ix_product_revision_related_product
    ON dongbac.product_revision_related_products (related_product_id);
