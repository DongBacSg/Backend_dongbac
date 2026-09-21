package com.dongbacsaigon.backend.audit.entity;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name = "audit_logs")
public class AuditLog {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(name = "occurred_at", nullable = false, updatable = false)
    private Instant occurredAt;

    @Column(name = "actor_user_id", updatable = false)
    private UUID actorUserId;

    @Column(name = "actor_email_snapshot", length = 320, updatable = false)
    private String actorEmailSnapshot;

    @Column(name = "actor_role_snapshot", length = 16, updatable = false)
    private String actorRoleSnapshot;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 64, updatable = false)
    private AuditAction action;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false, length = 64, updatable = false)
    private AuditTargetType targetType;

    @Column(name = "target_id", updatable = false)
    private UUID targetId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16, updatable = false)
    private AuditOutcome outcome;

    @Column(columnDefinition = "text", updatable = false)
    private String reason;

    @Column(name = "correlation_id", length = 100, updatable = false)
    private String correlationId;

    @Column(name = "ip_address", length = 128, updatable = false)
    private String ipAddress;

    @Column(name = "user_agent", columnDefinition = "text", updatable = false)
    private String userAgent;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected AuditLog() {
    }

    public AuditLog(AuditRecord auditRecord) {
        this.id = UUID.randomUUID();
        this.occurredAt = auditRecord.occurredAt();
        this.actorUserId = auditRecord.actorUserId();
        this.actorEmailSnapshot = auditRecord.actorEmailSnapshot();
        this.actorRoleSnapshot = auditRecord.actorRoleSnapshot();
        this.action = auditRecord.action();
        this.targetType = auditRecord.targetType();
        this.targetId = auditRecord.targetId();
        this.outcome = auditRecord.outcome();
        this.reason = auditRecord.reason();
        this.correlationId = auditRecord.correlationId();
        this.ipAddress = auditRecord.ipAddress();
        this.userAgent = auditRecord.userAgent();
    }

    @PrePersist
    void prePersist() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (occurredAt == null) {
            occurredAt = Instant.now();
        }
        createdAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    public UUID getActorUserId() {
        return actorUserId;
    }

    public String getActorEmailSnapshot() {
        return actorEmailSnapshot;
    }

    public String getActorRoleSnapshot() {
        return actorRoleSnapshot;
    }

    public AuditAction getAction() {
        return action;
    }

    public AuditTargetType getTargetType() {
        return targetType;
    }

    public UUID getTargetId() {
        return targetId;
    }

    public AuditOutcome getOutcome() {
        return outcome;
    }

    public String getReason() {
        return reason;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
