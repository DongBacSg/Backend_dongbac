package com.dongbacsaigon.backend.media.entity;

import java.math.BigDecimal;
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
@Table(name = "media")
public class Media {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(name = "asset_id", nullable = false, unique = true, length = 255)
    private String assetId;

    @Column(name = "public_id", nullable = false, length = 255)
    private String publicId;

    @Enumerated(EnumType.STRING)
    @Column(name = "resource_type", nullable = false, length = 16)
    private MediaType resourceType;

    @Column(nullable = false, length = 32)
    private String format;

    @Column(name = "secure_url", nullable = false, columnDefinition = "text")
    private String secureUrl;

    @Column
    private Integer width;

    @Column
    private Integer height;

    @Column(nullable = false)
    private long bytes;

    @Column(name = "duration_seconds")
    private BigDecimal durationSeconds;

    @Column(length = 255)
    private String folder;

    @Column(name = "original_filename", length = 255)
    private String originalFilename;

    @Column(name = "alt_text", length = 255)
    private String altText;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private MediaStatus status;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "uploaded_by", nullable = false)
    private User uploadedBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "deleted_by")
    private User deletedBy;

    @Version
    @Column(nullable = false)
    private long version;

    protected Media() {
    }

    public Media(
            String assetId,
            String publicId,
            MediaType resourceType,
            String format,
            String secureUrl,
            Integer width,
            Integer height,
            long bytes,
            BigDecimal durationSeconds,
            String folder,
            String originalFilename,
            User uploadedBy
    ) {
        this.id = UUID.randomUUID();
        this.assetId = assetId;
        this.publicId = publicId;
        this.resourceType = resourceType;
        this.format = format;
        this.secureUrl = secureUrl;
        this.width = width;
        this.height = height;
        this.bytes = bytes;
        this.durationSeconds = durationSeconds;
        this.folder = folder;
        this.originalFilename = originalFilename;
        this.uploadedBy = uploadedBy;
        this.status = MediaStatus.ACTIVE;
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

    public UUID getId() {
        return id;
    }

    public String getAssetId() {
        return assetId;
    }

    public String getPublicId() {
        return publicId;
    }

    public MediaType getResourceType() {
        return resourceType;
    }

    public String getFormat() {
        return format;
    }

    public String getSecureUrl() {
        return secureUrl;
    }

    public Integer getWidth() {
        return width;
    }

    public Integer getHeight() {
        return height;
    }

    public long getBytes() {
        return bytes;
    }

    public BigDecimal getDurationSeconds() {
        return durationSeconds;
    }

    public String getFolder() {
        return folder;
    }

    public String getOriginalFilename() {
        return originalFilename;
    }

    public String getAltText() {
        return altText;
    }

    public MediaStatus getStatus() {
        return status;
    }

    public User getUploadedBy() {
        return uploadedBy;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public Instant getDeletedAt() {
        return deletedAt;
    }

    public User getDeletedBy() {
        return deletedBy;
    }

    public long getVersion() {
        return version;
    }

    public boolean isActive() {
        return status == MediaStatus.ACTIVE;
    }

    public void updateAltText(String altText) {
        this.altText = altText;
    }

    public void markDeleted(User deletedBy, Instant deletedAt) {
        this.status = MediaStatus.DELETED;
        this.deletedBy = deletedBy;
        this.deletedAt = deletedAt;
    }
}
