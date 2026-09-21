package com.dongbacsaigon.backend.catalog.entity;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name = "product_revision_related_products")
public class ProductRevisionRelatedProduct {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_revision_id", nullable = false)
    private ProductRevision revision;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "related_product_id", nullable = false)
    private Product relatedProduct;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected ProductRevisionRelatedProduct() {
    }

    public ProductRevisionRelatedProduct(ProductRevision revision, Product relatedProduct, int sortOrder) {
        this.id = UUID.randomUUID();
        this.revision = revision;
        this.relatedProduct = relatedProduct;
        this.sortOrder = sortOrder;
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
    public Product getRelatedProduct() { return relatedProduct; }
    public int getSortOrder() { return sortOrder; }
    public Instant getCreatedAt() { return createdAt; }
}
