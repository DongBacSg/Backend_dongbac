package com.dongbacsaigon.backend.audit.service;

import com.dongbacsaigon.backend.audit.entity.AuditRecord;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
class AuditEventListener {

    private final AuditLogWriter auditLogWriter;

    AuditEventListener(AuditLogWriter auditLogWriter) {
        this.auditLogWriter = auditLogWriter;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    void handleAuditRecord(AuditRecord auditRecord) {
        auditLogWriter.writeSafely(auditRecord);
    }
}
