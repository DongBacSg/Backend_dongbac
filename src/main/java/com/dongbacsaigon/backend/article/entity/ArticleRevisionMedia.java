package com.dongbacsaigon.backend.article.entity;

import java.time.Instant;
import java.util.UUID;

import com.dongbacsaigon.backend.media.entity.Media;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name = "article_revision_media")
public class ArticleRevisionMedia {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "article_revision_id", nullable = false)
    private ArticleRevision revision;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "media_id", nullable = false)
    private Media media;

    @Enumerated(EnumType.STRING)
    @Column(name = "usage_type", nullable = false, length = 16)
    private ArticleMediaUsageType usageType;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Column(length = 500)
    private String caption;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected ArticleRevisionMedia() {
    }

    public ArticleRevisionMedia(ArticleRevision revision, Media media, ArticleMediaUsageType usageType, int sortOrder, String caption) {
        this.id = UUID.randomUUID();
        this.revision = revision;
        this.media = media;
        this.usageType = usageType;
        this.sortOrder = sortOrder;
        this.caption = caption;
    }

    @PrePersist
    void prePersist() { if (id == null) id = UUID.randomUUID(); createdAt = Instant.now(); }

    public UUID getId() { return id; }
    public ArticleRevision getRevision() { return revision; }
    public Media getMedia() { return media; }
    public ArticleMediaUsageType getUsageType() { return usageType; }
    public int getSortOrder() { return sortOrder; }
    public String getCaption() { return caption; }
    public Instant getCreatedAt() { return createdAt; }
}
