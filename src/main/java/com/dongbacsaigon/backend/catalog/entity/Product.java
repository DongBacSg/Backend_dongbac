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
@Table(name = "products")
public class Product {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "current_published_revision_id")
    private ProductRevision currentPublishedRevision;

    @Column(name = "latest_revision_number", nullable = false)
    private long latestRevisionNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "publication_status", nullable = false, length = 32)
    private ProductPublicationStatus publicationStatus;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(nullable = false)
    private long version;

    protected Product() {
    }

    public Product(User createdBy) {
        this.id = UUID.randomUUID();
        this.createdBy = createdBy;
        this.publicationStatus = ProductPublicationStatus.DRAFT;
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
    void preUpdate() {
        updatedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public ProductRevision getCurrentPublishedRevision() { return currentPublishedRevision; }
    public long getLatestRevisionNumber() { return latestRevisionNumber; }
    public ProductPublicationStatus getPublicationStatus() { return publicationStatus; }
    public User getCreatedBy() { return createdBy; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public long getVersion() { return version; }

    public long nextRevisionNumber() {
        latestRevisionNumber++;
        return latestRevisionNumber;
    }

    public void publish(ProductRevision revision) {
        currentPublishedRevision = revision;
        publicationStatus = ProductPublicationStatus.PUBLISHED;
    }

    public void unpublish() { publicationStatus = ProductPublicationStatus.UNPUBLISHED; }
    public void republish() { publicationStatus = ProductPublicationStatus.PUBLISHED; }
    public void archive() { publicationStatus = ProductPublicationStatus.ARCHIVED; }
}
