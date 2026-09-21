package com.dongbacsaigon.backend.approval.service;

import java.util.UUID;

import com.dongbacsaigon.backend.approval.entity.ApprovalRequest;
import com.dongbacsaigon.backend.approval.entity.ApprovalResourceType;
import com.dongbacsaigon.backend.user.entity.User;

public interface ApprovalResourceHandler {

    boolean supports(ApprovalResourceType resourceType);

    void validateCanSubmit(
            ApprovalResourceType resourceType,
            UUID resourceId,
            long resourceVersion,
            UUID submittedBy
    );

    void onApproved(ApprovalRequest approvalRequest);

    default void onApproved(ApprovalRequest approvalRequest, User reviewer) {
        onApproved(approvalRequest);
    }

    void onRejected(ApprovalRequest approvalRequest);

    default void onRejected(ApprovalRequest approvalRequest, User reviewer) {
        onRejected(approvalRequest);
    }
}
