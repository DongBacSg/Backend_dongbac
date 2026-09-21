package com.dongbacsaigon.backend.audit.service;

import java.time.Instant;
import java.util.UUID;

import com.dongbacsaigon.backend.audit.entity.AuditAction;
import com.dongbacsaigon.backend.audit.entity.AuditOutcome;
import com.dongbacsaigon.backend.audit.entity.AuditRecord;
import com.dongbacsaigon.backend.audit.entity.AuditTargetType;
import org.springframework.stereotype.Component;

@Component
class AuditRecordFactory {

    private static final int MAX_REASON_LENGTH = 1000;
    private static final int MAX_USER_AGENT_LENGTH = 2000;

    AuditRecord create(
            AuditActor actor,
            AuditAction action,
            AuditTargetType targetType,
            UUID targetId,
            AuditOutcome outcome,
            String reason,
            AuditRequestContext context
    ) {
        AuditActor safeActor = actor == null ? AuditActor.anonymous() : actor;
        AuditRequestContext safeContext = context == null ? AuditRequestContext.empty() : context;

        return new AuditRecord(
                Instant.now(),
                safeActor.userId(),
                truncate(safeActor.emailSnapshot(), 320),
                truncate(safeActor.roleSnapshot(), 16),
                action,
                targetType,
                targetId,
                outcome,
                truncate(reason, MAX_REASON_LENGTH),
                truncate(safeContext.correlationId(), 100),
                truncate(safeContext.ipAddress(), 128),
                truncate(safeContext.userAgent(), MAX_USER_AGENT_LENGTH)
        );
    }

    private String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }
}
