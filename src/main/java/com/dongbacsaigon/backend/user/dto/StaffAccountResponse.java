package com.dongbacsaigon.backend.user.dto;

import java.time.Instant;
import java.util.UUID;

import com.dongbacsaigon.backend.user.entity.UserRole;
import com.dongbacsaigon.backend.user.entity.UserStatus;

public record StaffAccountResponse(
        UUID id,
        String email,
        String fullName,
        UserRole role,
        UserStatus status,
        boolean mustChangePassword,
        int failedLoginAttempts,
        Instant lockedUntil,
        Instant lastLoginAt,
        Instant createdAt,
        Instant updatedAt,
        UUID createdBy
) {
}
