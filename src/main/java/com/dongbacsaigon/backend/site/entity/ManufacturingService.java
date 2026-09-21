package com.dongbacsaigon.backend.site.entity;

import java.time.Instant;
import java.util.UUID;

import com.dongbacsaigon.backend.media.entity.Media;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(name = "manufacturing_services")
public class ManufacturingService {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(nullable = false, unique = true, length = 180)
    private String slug;

    @Column(nullable = false, length = 160)
    private String title;

    @Column(columnDefinition = "text")
    private String summary;

    @Column(columnDefinition = "text")
    private String content;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "featured_media_id")
    private Media featuredMedia;

    @Column(name = "seo_title", length = 240)
    private String seoTitle;

    @Column(name = "seo_description", length = 500)
    private String seoDescription;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Column(nullable = false)
    private boolean active;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(nullable = false)
    private long version;

    protected ManufacturingService() {
    }

    public ManufacturingService(
            String slug, String title, String summary, String content, Media featuredMedia,
            String seoTitle, String seoDescription, int sortOrder, boolean active
    ) {
        id = UUID.randomUUID();
        update(slug, title, summary, content, featuredMedia, seoTitle, seoDescription, sortOrder, active);
    }

    public void update(
            String slug, String title, String summary, String content, Media featuredMedia,
            String seoTitle, String seoDescription, int sortOrder, boolean active
    ) {
        this.slug = slug;
        this.title = title;
        this.summary = summary;
        this.content = content;
        this.featuredMedia = featuredMedia;
        this.seoTitle = seoTitle;
        this.seoDescription = seoDescription;
        this.sortOrder = sortOrder;
        this.active = active;
    }

    @PrePersist
    void prePersist() {
        Instant now = Instant.now();
        if (id == null) id = UUID.randomUUID();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public String getSlug() { return slug; }
    public String getTitle() { return title; }
    public String getSummary() { return summary; }
    public String getContent() { return content; }
    public Media getFeaturedMedia() { return featuredMedia; }
    public String getSeoTitle() { return seoTitle; }
    public String getSeoDescription() { return seoDescription; }
    public int getSortOrder() { return sortOrder; }
    public boolean isActive() { return active; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public long getVersion() { return version; }
}
