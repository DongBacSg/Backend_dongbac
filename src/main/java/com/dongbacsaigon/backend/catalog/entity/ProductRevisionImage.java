package com.dongbacsaigon.backend.catalog.entity;

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
import jakarta.persistence.Table;

@Entity
@Table(name = "product_revision_images")
public class ProductRevisionImage {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_revision_id", nullable = false)
    private ProductRevision revision;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "media_id", nullable = false)
    private Media media;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Column(name = "is_primary", nullable = false)
    private boolean primary;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected ProductRevisionImage() {
    }

    public ProductRevisionImage(ProductRevision revision, Media media, int sortOrder, boolean primary) {
        this.id = UUID.randomUUID();
        this.revision = revision;
        this.media = media;
        this.sortOrder = sortOrder;
        this.primary = primary;
    }

    @PrePersist
    void prePersist() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        createdAt = Instant.now();
    }

    public UUID getId() { return id; }
    public ProductRevision getRevision() { return revision; }
    public Media getMedia() { return media; }
    public int getSortOrder() { return sortOrder; }
    public boolean isPrimary() { return primary; }
    public Instant getCreatedAt() { return createdAt; }
}
