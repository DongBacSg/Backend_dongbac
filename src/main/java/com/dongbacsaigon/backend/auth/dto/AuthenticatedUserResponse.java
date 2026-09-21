package com.dongbacsaigon.backend.auth.dto;

import java.util.UUID;

import com.dongbacsaigon.backend.user.entity.UserRole;

public record AuthenticatedUserResponse(
        UUID id,
        String email,
        String fullName,
        UserRole role,
        boolean mustChangePassword
) {
}
