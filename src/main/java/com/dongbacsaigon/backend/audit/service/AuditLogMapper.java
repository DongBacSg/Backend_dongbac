package com.dongbacsaigon.backend.audit.service;

import com.dongbacsaigon.backend.audit.dto.AuditLogResponse;
import com.dongbacsaigon.backend.audit.entity.AuditLog;

public final class AuditLogMapper {

    private AuditLogMapper() {
    }

    public static AuditLogResponse toResponse(AuditLog auditLog) {
        return new AuditLogResponse(
                auditLog.getId(),
                auditLog.getOccurredAt(),
                auditLog.getActorUserId(),
                auditLog.getActorEmailSnapshot(),
                auditLog.getActorRoleSnapshot(),
                auditLog.getAction(),
                auditLog.getTargetType(),
                auditLog.getTargetId(),
                auditLog.getOutcome(),
                auditLog.getReason(),
                auditLog.getCorrelationId(),
                auditLog.getIpAddress(),
                auditLog.getUserAgent(),
                auditLog.getCreatedAt()
        );
    }
}
