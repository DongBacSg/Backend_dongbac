package com.dongbacsaigon.backend.audit.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.dongbacsaigon.backend.audit.entity.AuditAction;
import com.dongbacsaigon.backend.audit.entity.AuditOutcome;
import com.dongbacsaigon.backend.audit.entity.AuditRecord;
import com.dongbacsaigon.backend.audit.entity.AuditTargetType;
import org.junit.jupiter.api.Test;

class AuditRecordFactoryTest {

    private final AuditRecordFactory auditRecordFactory = new AuditRecordFactory();

    @Test
    void createCapturesCorrelationContextAndTruncatesReason() {
        String longReason = "x".repeat(1100);

        AuditRecord auditRecord = auditRecordFactory.create(
                AuditActor.unknownEmail("missing@example.com"),
                AuditAction.AUTH_LOGIN_FAILURE,
                AuditTargetType.AUTH,
                null,
                AuditOutcome.FAILURE,
                longReason,
                new AuditRequestContext("corr-1", "127.0.0.1", "JUnit")
        );

        assertThat(auditRecord.actorEmailSnapshot()).isEqualTo("missing@example.com");
        assertThat(auditRecord.correlationId()).isEqualTo("corr-1");
        assertThat(auditRecord.ipAddress()).isEqualTo("127.0.0.1");
        assertThat(auditRecord.userAgent()).isEqualTo("JUnit");
        assertThat(auditRecord.reason()).hasSize(1000);
    }
}
