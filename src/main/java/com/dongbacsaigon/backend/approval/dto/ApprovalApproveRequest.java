package com.dongbacsaigon.backend.approval.dto;

import jakarta.validation.constraints.Size;

public record ApprovalApproveRequest(
        @Size(max = 1000)
        String note
) {
}
