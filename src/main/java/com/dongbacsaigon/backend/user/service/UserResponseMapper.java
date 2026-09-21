package com.dongbacsaigon.backend.user.service;

import com.dongbacsaigon.backend.user.dto.StaffAccountResponse;
import com.dongbacsaigon.backend.user.entity.User;

public final class UserResponseMapper {

    private UserResponseMapper() {
    }

    public static StaffAccountResponse toStaffAccount(User user) {
        return new StaffAccountResponse(
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                user.getRole(),
                user.getStatus(),
                user.isMustChangePassword(),
                user.getFailedLoginAttempts(),
                user.getLockedUntil(),
                user.getLastLoginAt(),
                user.getCreatedAt(),
                user.getUpdatedAt(),
                user.getCreatedBy()
        );
    }
}
