package com.dongbacsaigon.backend.catalog.entity;

import java.time.Instant;
import java.util.UUID;

import com.dongbacsaigon.backend.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(name = "product_revisions")
public class ProductRevision {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "revision_number", nullable = false)
    private long revisionNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private ProductRevisionStatus status;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(nullable = false, length = 220)
    private String slug;

    @Column(name = "short_description", columnDefinition = "text")
    private String shortDescription;

    @Column(columnDefinition = "text")
    private String content;

    @Column(name = "seo_title", length = 200)
    private String seoTitle;

    @Column(name = "seo_description", length = 500)
    private String seoDescription;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "published_at")
    private Instant publishedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "published_by")
    private User publishedBy;

    @Version
    @Column(nullable = false)
    private long version;

    protected ProductRevision() {
    }

    public ProductRevision(
            Product product,
            long revisionNumber,
            Category category,
            String name,
            String slug,
            String shortDescription,
            String content,
            String seoTitle,
            String seoDescription,
            User createdBy
    ) {
        this.id = UUID.randomUUID();
        this.product = product;
        this.revisionNumber = revisionNumber;
        this.status = ProductRevisionStatus.DRAFT;
        this.category = category;
        this.name = name;
        this.slug = slug;
        this.shortDescription = shortDescription;
        this.content = content;
        this.seoTitle = seoTitle;
        this.seoDescription = seoDescription;
        this.createdBy = createdBy;
    }

    @PrePersist
    void prePersist() {
        Instant now = Instant.now();
        if (id == null) {
            id = UUID.randomUUID();
        }
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void preUpdate() { updatedAt = Instant.now(); }

    public UUID getId() { return id; }
    public Product getProduct() { return product; }
    public long getRevisionNumber() { return revisionNumber; }
    public ProductRevisionStatus getStatus() { return status; }
    public Category getCategory() { return category; }
    public String getName() { return name; }
    public String getSlug() { return slug; }
    public String getShortDescription() { return shortDescription; }
    public String getContent() { return content; }
    public String getSeoTitle() { return seoTitle; }
    public String getSeoDescription() { return seoDescription; }
    public User getCreatedBy() { return createdBy; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public Instant getPublishedAt() { return publishedAt; }
    public User getPublishedBy() { return publishedBy; }
    public long getVersion() { return version; }

    public void updateDraft(
            Category category,
            String name,
            String slug,
            String shortDescription,
            String content,
            String seoTitle,
            String seoDescription
    ) {
        this.category = category;
        this.name = name;
        this.slug = slug;
        this.shortDescription = shortDescription;
        this.content = content;
        this.seoTitle = seoTitle;
        this.seoDescription = seoDescription;
    }

    public void submit() { status = ProductRevisionStatus.PENDING_REVIEW; }

    public void publish(User admin, Instant now) {
        status = ProductRevisionStatus.PUBLISHED;
        publishedBy = admin;
        publishedAt = now;
    }

    public void reject() { status = ProductRevisionStatus.REJECTED; }
    public void archive() { status = ProductRevisionStatus.ARCHIVED; }
}
