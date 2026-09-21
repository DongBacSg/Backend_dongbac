package com.dongbacsaigon.backend.audit.dto;

import java.time.Instant;
import java.util.UUID;

import com.dongbacsaigon.backend.audit.entity.AuditAction;
import com.dongbacsaigon.backend.audit.entity.AuditOutcome;
import com.dongbacsaigon.backend.audit.entity.AuditTargetType;

public record AuditLogResponse(
        UUID id,
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
        String userAgent,
        Instant createdAt
) {
}
