package com.dongbacsaigon.backend.audit.entity;

import java.time.Instant;
import java.util.UUID;

public record AuditRecord(
        Instant occurredAt,
        UUID actorUserId,
        String actorEmailSnapshot,
        String actorRoleSnapshot,
        AuditAction action,
        AuditTargetType targetType,
        UUID targetId,
        AuditOutcome outcome,
        String reason,
        String correlationId,
        String ipAddress,
        String userAgent
) {
}
