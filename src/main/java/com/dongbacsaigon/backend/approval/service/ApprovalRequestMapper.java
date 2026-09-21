package com.dongbacsaigon.backend.approval.service;

import com.dongbacsaigon.backend.approval.dto.ApprovalRequestResponse;
import com.dongbacsaigon.backend.approval.dto.ApprovalUserSummary;
import com.dongbacsaigon.backend.approval.entity.ApprovalRequest;
import com.dongbacsaigon.backend.user.entity.User;

public final class ApprovalRequestMapper {

    private ApprovalRequestMapper() {
    }

    public static ApprovalRequestResponse toResponse(ApprovalRequest approvalRequest) {
        return new ApprovalRequestResponse(
                approvalRequest.getId(),
                approvalRequest.getResourceType(),
                approvalRequest.getResourceId(),
                approvalRequest.getResourceVersion(),
                approvalRequest.getStatus(),
                toUserSummary(approvalRequest.getSubmittedBy()),
                approvalRequest.getSubmittedAt(),
                toUserSummary(approvalRequest.getReviewedBy()),
                approvalRequest.getReviewedAt(),
                approvalRequest.getReviewNote(),
                approvalRequest.getCreatedAt(),
                approvalRequest.getUpdatedAt()
        );
    }

    private static ApprovalUserSummary toUserSummary(User user) {
        if (user == null) {
            return null;
        }
        return new ApprovalUserSummary(
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                user.getRole()
        );
    }
}
