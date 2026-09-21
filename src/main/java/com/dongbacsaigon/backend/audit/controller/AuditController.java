package com.dongbacsaigon.backend.audit.controller;

import java.time.Instant;
import java.util.UUID;

import com.dongbacsaigon.backend.audit.dto.AuditLogResponse;
import com.dongbacsaigon.backend.audit.dto.AuditPageResponse;
import com.dongbacsaigon.backend.audit.entity.AuditAction;
import com.dongbacsaigon.backend.audit.entity.AuditOutcome;
import com.dongbacsaigon.backend.audit.entity.AuditTargetType;
import com.dongbacsaigon.backend.audit.service.AuditQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/audit")
@PreAuthorize("hasRole('ADMIN')")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Admin Audit Trail")
class AuditController {

    private final AuditQueryService auditQueryService;

    AuditController(AuditQueryService auditQueryService) {
        this.auditQueryService = auditQueryService;
    }

    @GetMapping
    @Operation(summary = "List persistent audit logs", description = "ADMIN only.")
    AuditPageResponse listAuditLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) AuditAction action,
            @RequestParam(required = false) UUID actorUserId,
            @RequestParam(required = false) AuditTargetType targetType,
            @RequestParam(required = false) UUID targetId,
            @RequestParam(required = false) AuditOutcome outcome,
            @RequestParam(required = false) Instant dateFrom,
            @RequestParam(required = false) Instant dateTo
    ) {
        return auditQueryService.listAuditLogs(
                page,
                size,
                action,
                actorUserId,
                targetType,
                targetId,
                outcome,
                dateFrom,
                dateTo
        );
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a persistent audit log", description = "ADMIN only.")
    AuditLogResponse getAuditLog(@PathVariable UUID id) {
        return auditQueryService.getAuditLog(id);
    }
}
