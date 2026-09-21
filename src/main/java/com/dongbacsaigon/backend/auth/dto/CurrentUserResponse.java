package com.dongbacsaigon.backend.auth.dto;

import java.util.UUID;

import com.dongbacsaigon.backend.user.entity.UserRole;
import com.dongbacsaigon.backend.user.entity.UserStatus;

public record CurrentUserResponse(
        UUID id,
        String email,
        String fullName,
        UserRole role,
        UserStatus status,
        boolean mustChangePassword
) {
}
