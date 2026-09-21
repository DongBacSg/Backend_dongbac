package com.dongbacsaigon.backend.approval.entity;

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
@Table(name = "approval_requests")
public class ApprovalRequest {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(name = "resource_type", nullable = false, length = 32)
    private ApprovalResourceType resourceType;

    @Column(name = "resource_id", nullable = false)
    private UUID resourceId;

    @Column(name = "resource_version", nullable = false)
    private long resourceVersion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private ApprovalRequestStatus status;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "submitted_by", nullable = false)
    private User submittedBy;

    @Column(name = "submitted_at", nullable = false)
    private Instant submittedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by")
    private User reviewedBy;

    @Column(name = "reviewed_at")
    private Instant reviewedAt;

    @Column(name = "review_note", columnDefinition = "text")
    private String reviewNote;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(nullable = false)
    private long version;

    protected ApprovalRequest() {
    }

    public ApprovalRequest(
            ApprovalResourceType resourceType,
            UUID resourceId,
            long resourceVersion,
            User submittedBy,
            Instant submittedAt
    ) {
        this.id = UUID.randomUUID();
        this.resourceType = resourceType;
        this.resourceId = resourceId;
        this.resourceVersion = resourceVersion;
        this.status = ApprovalRequestStatus.PENDING;
        this.submittedBy = submittedBy;
        this.submittedAt = submittedAt;
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

    public ApprovalResourceType getResourceType() {
        return resourceType;
    }

    public UUID getResourceId() {
        return resourceId;
    }

    public long getResourceVersion() {
        return resourceVersion;
    }

    public ApprovalRequestStatus getStatus() {
        return status;
    }

    public User getSubmittedBy() {
        return submittedBy;
    }

    public Instant getSubmittedAt() {
        return submittedAt;
    }

    public User getReviewedBy() {
        return reviewedBy;
    }

    public Instant getReviewedAt() {
        return reviewedAt;
    }

    public String getReviewNote() {
        return reviewNote;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public long getVersion() {
        return version;
    }

    public boolean isPending() {
        return status == ApprovalRequestStatus.PENDING;
    }

    public void approve(User reviewedBy, Instant reviewedAt, String note) {
        this.status = ApprovalRequestStatus.APPROVED;
        this.reviewedBy = reviewedBy;
        this.reviewedAt = reviewedAt;
        this.reviewNote = note;
    }

    public void reject(User reviewedBy, Instant reviewedAt, String reason) {
        this.status = ApprovalRequestStatus.REJECTED;
        this.reviewedBy = reviewedBy;
        this.reviewedAt = reviewedAt;
        this.reviewNote = reason;
    }

    public void cancel(User reviewedBy, Instant reviewedAt, String reason) {
        this.status = ApprovalRequestStatus.CANCELLED;
        this.reviewedBy = reviewedBy;
        this.reviewedAt = reviewedAt;
        this.reviewNote = reason;
    }
}
