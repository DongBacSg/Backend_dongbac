package com.dongbacsaigon.backend.lead.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PublicContactRequest(
        @NotBlank @Size(max = 160) String name,
        @Size(max = 64) String phone,
        @Size(max = 320) String email,
        @Size(max = 240) String subject,
        @NotBlank @Size(max = 5000) String message
) {
}
