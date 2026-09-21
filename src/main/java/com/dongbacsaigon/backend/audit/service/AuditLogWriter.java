package com.dongbacsaigon.backend.audit.service;

import com.dongbacsaigon.backend.audit.entity.AuditLog;
import com.dongbacsaigon.backend.audit.entity.AuditRecord;
import com.dongbacsaigon.backend.audit.repository.AuditLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
class AuditLogWriter {

    private static final Logger LOGGER = LoggerFactory.getLogger(AuditLogWriter.class);

    private final AuditLogRepository auditLogRepository;

    AuditLogWriter(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void writeSafely(AuditRecord auditRecord) {
        try {
            auditLogRepository.save(new AuditLog(auditRecord));
        } catch (RuntimeException exception) {
            LOGGER.error("Failed to persist audit log for action {}", auditRecord.action(), exception);
        }
    }
}
