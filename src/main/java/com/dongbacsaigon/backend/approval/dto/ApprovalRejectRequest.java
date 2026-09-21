package com.dongbacsaigon.backend.approval.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ApprovalRejectRequest(
        @NotBlank
        @Size(min = 3, max = 1000)
        String reason
) {
}
