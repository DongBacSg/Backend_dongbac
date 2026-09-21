package com.dongbacsaigon.backend.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record StaffPasswordResetRequest(
        @NotBlank
        @Size(max = 128)
        String temporaryPassword
) {
}
