package com.dongbacsaigon.backend.audit.service;

import java.util.UUID;

import com.dongbacsaigon.backend.audit.entity.AuditAction;
import com.dongbacsaigon.backend.audit.entity.AuditOutcome;
import com.dongbacsaigon.backend.audit.entity.AuditRecord;
import com.dongbacsaigon.backend.audit.entity.AuditTargetType;
import com.dongbacsaigon.backend.user.entity.User;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

@Service
public class AuditService {

    private final ApplicationEventPublisher eventPublisher;
    private final AuditLogWriter auditLogWriter;
    private final AuditRecordFactory auditRecordFactory;

    public AuditService(
            ApplicationEventPublisher eventPublisher,
            AuditLogWriter auditLogWriter,
            AuditRecordFactory auditRecordFactory
    ) {
        this.eventPublisher = eventPublisher;
        this.auditLogWriter = auditLogWriter;
        this.auditRecordFactory = auditRecordFactory;
    }

    public void recordSuccessAfterCommit(
            User actor,
            AuditAction action,
            AuditTargetType targetType,
            UUID targetId,
            String reason
    ) {
        recordSuccessAfterCommit(AuditActor.from(actor), action, targetType, targetId, reason);
    }

    public void recordSuccessAfterCommit(
            AuditActor actor,
            AuditAction action,
            AuditTargetType targetType,
            UUID targetId,
            String reason
    ) {
        eventPublisher.publishEvent(createRecord(actor, action, targetType, targetId, AuditOutcome.SUCCESS, reason));
    }

    public void recordFailureNow(
            AuditActor actor,
            AuditAction action,
            AuditTargetType targetType,
            UUID targetId,
            String reason
    ) {
        auditLogWriter.writeSafely(createRecord(actor, action, targetType, targetId, AuditOutcome.FAILURE, reason));
    }

    private AuditRecord createRecord(
            AuditActor actor,
            AuditAction action,
            AuditTargetType targetType,
            UUID targetId,
            AuditOutcome outcome,
            String reason
    ) {
        return auditRecordFactory.create(
                actor,
                action,
                targetType,
                targetId,
                outcome,
                reason,
                AuditRequestContextHolder.get()
        );
    }
}
