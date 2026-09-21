CREATE TABLE dongbac.articles (
    id UUID PRIMARY KEY,
    current_published_revision_id UUID NULL,
    latest_revision_number BIGINT NOT NULL DEFAULT 0,
    publication_status VARCHAR(32) NOT NULL,
    created_by UUID NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL,
    CONSTRAINT fk_articles_created_by FOREIGN KEY (created_by) REFERENCES dongbac.users (id),
    CONSTRAINT ck_articles_latest_revision_number CHECK (latest_revision_number >= 0),
    CONSTRAINT ck_articles_publication_status CHECK (
        publication_status IN ('DRAFT', 'PUBLISHED', 'UNPUBLISHED', 'ARCHIVED')
    )
);

CREATE TABLE dongbac.article_revisions (
    id UUID PRIMARY KEY,
    article_id UUID NOT NULL,
    revision_number BIGINT NOT NULL,
    status VARCHAR(32) NOT NULL,
    article_type VARCHAR(32) NOT NULL,
    title VARCHAR(240) NOT NULL,
    slug VARCHAR(260) NOT NULL,
    summary TEXT NULL,
    content TEXT NOT NULL,
    seo_title VARCHAR(240) NULL,
    seo_description VARCHAR(500) NULL,
    created_by UUID NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    published_at TIMESTAMPTZ NULL,
    published_by UUID NULL,
    version BIGINT NOT NULL,
    CONSTRAINT uk_article_revisions_number UNIQUE (article_id, revision_number),
    CONSTRAINT fk_article_revisions_article FOREIGN KEY (article_id) REFERENCES dongbac.articles (id),
    CONSTRAINT fk_article_revisions_created_by FOREIGN KEY (created_by) REFERENCES dongbac.users (id),
    CONSTRAINT fk_article_revisions_published_by FOREIGN KEY (published_by) REFERENCES dongbac.users (id),
    CONSTRAINT ck_article_revisions_number CHECK (revision_number >= 1),
    CONSTRAINT ck_article_revisions_status CHECK (
        status IN ('DRAFT', 'PENDING_REVIEW', 'REJECTED', 'PUBLISHED', 'ARCHIVED')
    ),
    CONSTRAINT ck_article_revisions_type CHECK (
        article_type IN ('INTERNAL_ACTIVITY', 'NEWS', 'KNOWLEDGE')
    )
);

ALTER TABLE dongbac.articles
    ADD CONSTRAINT fk_articles_current_published_revision
    FOREIGN KEY (current_published_revision_id) REFERENCES dongbac.article_revisions (id);

CREATE INDEX ix_articles_publication_status ON dongbac.articles (publication_status);
CREATE INDEX ix_articles_current_published_revision ON dongbac.articles (current_published_revision_id);
CREATE INDEX ix_article_revisions_article ON dongbac.article_revisions (article_id, revision_number DESC);
CREATE INDEX ix_article_revisions_status ON dongbac.article_revisions (status);
CREATE INDEX ix_article_revisions_type ON dongbac.article_revisions (article_type);
CREATE INDEX ix_article_revisions_slug ON dongbac.article_revisions (slug);
CREATE INDEX ix_article_revisions_published_at ON dongbac.article_revisions (published_at DESC);
CREATE UNIQUE INDEX uk_article_revisions_published_slug
    ON dongbac.article_revisions (slug)
    WHERE status = 'PUBLISHED';
CREATE UNIQUE INDEX uk_article_revisions_one_draft
    ON dongbac.article_revisions (article_id)
    WHERE status = 'DRAFT';
CREATE UNIQUE INDEX uk_article_revisions_one_pending
    ON dongbac.article_revisions (article_id)
    WHERE status = 'PENDING_REVIEW';

CREATE TABLE dongbac.article_revision_media (
    id UUID PRIMARY KEY,
    article_revision_id UUID NOT NULL,
    media_id UUID NOT NULL,
    usage_type VARCHAR(16) NOT NULL,
    sort_order INTEGER NOT NULL,
    caption VARCHAR(500) NULL,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_article_revision_media UNIQUE (article_revision_id, media_id, usage_type),
    CONSTRAINT fk_article_revision_media_revision FOREIGN KEY (article_revision_id) REFERENCES dongbac.article_revisions (id),
    CONSTRAINT fk_article_revision_media_media FOREIGN KEY (media_id) REFERENCES dongbac.media (id),
    CONSTRAINT ck_article_revision_media_usage CHECK (usage_type IN ('FEATURED', 'CONTENT')),
    CONSTRAINT ck_article_revision_media_sort_order CHECK (sort_order >= 0)
);

CREATE UNIQUE INDEX uk_article_revision_featured_media
    ON dongbac.article_revision_media (article_revision_id)
    WHERE usage_type = 'FEATURED';
CREATE INDEX ix_article_revision_media_revision_sort
    ON dongbac.article_revision_media (article_revision_id, usage_type, sort_order);
CREATE INDEX ix_article_revision_media_media ON dongbac.article_revision_media (media_id);
