package com.dongbacsaigon.backend.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

public record StaffUpdateRequest(
        @Email
        @Size(max = 320)
        String email,

        @Size(max = 160)
        String fullName
) {
}
