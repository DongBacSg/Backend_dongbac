package com.dongbacsaigon.backend.approval.dto;

import java.util.UUID;

import com.dongbacsaigon.backend.user.entity.UserRole;

public record ApprovalUserSummary(
        UUID id,
        String email,
        String fullName,
        UserRole role
) {
}
