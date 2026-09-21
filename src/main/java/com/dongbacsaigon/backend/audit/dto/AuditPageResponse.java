package com.dongbacsaigon.backend.audit.dto;

import java.util.List;

public record AuditPageResponse(
        List<AuditLogResponse> items,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
}
