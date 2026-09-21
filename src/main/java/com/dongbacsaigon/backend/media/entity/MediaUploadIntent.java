package com.dongbacsaigon.backend.media.entity;

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
import jakarta.persistence.Table;

@Entity
@Table(name = "media_upload_intents")
public class MediaUploadIntent {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(name = "public_id", nullable = false, length = 160)
    private String publicId;

    @Column(nullable = false, length = 255)
    private String folder;

    @Enumerated(EnumType.STRING)
    @Column(name = "media_type", nullable = false, length = 16)
    private MediaType mediaType;

    @Column(name = "original_filename", length = 255)
    private String originalFilename;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "requested_by", nullable = false)
    private User requestedBy;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private MediaUploadIntentStatus status;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected MediaUploadIntent() {
    }

    public MediaUploadIntent(
            String publicId,
            String folder,
            MediaType mediaType,
            String originalFilename,
            User requestedBy,
            Instant expiresAt
    ) {
        this.id = UUID.randomUUID();
        this.publicId = publicId;
        this.folder = folder;
        this.mediaType = mediaType;
        this.originalFilename = originalFilename;
        this.requestedBy = requestedBy;
        this.status = MediaUploadIntentStatus.PENDING;
        this.expiresAt = expiresAt;
    }

    @PrePersist
    void prePersist() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        createdAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public String getPublicId() {
        return publicId;
    }

    public String getFolder() {
        return folder;
    }

    public MediaType getMediaType() {
        return mediaType;
    }

    public String getOriginalFilename() {
        return originalFilename;
    }

    public User getRequestedBy() {
        return requestedBy;
    }

    public MediaUploadIntentStatus getStatus() {
        return status;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public String expectedCloudinaryPublicId() {
        return folder + "/" + publicId;
    }

    public boolean isExpired(Instant now) {
        return !expiresAt.isAfter(now);
    }

    public void complete(Instant completedAt) {
        this.status = MediaUploadIntentStatus.COMPLETED;
        this.completedAt = completedAt;
    }

    public void fail() {
        this.status = MediaUploadIntentStatus.FAILED;
    }

    public void expire() {
        this.status = MediaUploadIntentStatus.EXPIRED;
    }
}
