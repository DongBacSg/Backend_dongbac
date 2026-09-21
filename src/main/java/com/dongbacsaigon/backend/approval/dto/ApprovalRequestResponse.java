package com.dongbacsaigon.backend.approval.dto;

import java.time.Instant;
import java.util.UUID;

import com.dongbacsaigon.backend.approval.entity.ApprovalRequestStatus;
import com.dongbacsaigon.backend.approval.entity.ApprovalResourceType;

public record ApprovalRequestResponse(
        UUID id,
        ApprovalResourceType resourceType,
        UUID resourceId,
        long resourceVersion,
        ApprovalRequestStatus status,
        ApprovalUserSummary submittedBy,
        Instant submittedAt,
        ApprovalUserSummary reviewedBy,
        Instant reviewedAt,
        String reviewNote,
        Instant createdAt,
        Instant updatedAt
) {
}
