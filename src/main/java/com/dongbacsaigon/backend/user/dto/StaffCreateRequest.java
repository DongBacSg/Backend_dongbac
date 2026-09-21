package com.dongbacsaigon.backend.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record StaffCreateRequest(
        @NotBlank
        @Email
        @Size(max = 320)
        String email,

        @NotBlank
        @Size(max = 160)
        String fullName,

        @NotBlank
        @Size(max = 128)
        String temporaryPassword
) {
}
