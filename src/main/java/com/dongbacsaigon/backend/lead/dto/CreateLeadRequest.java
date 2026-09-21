package com.dongbacsaigon.backend.lead.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateLeadRequest(
        @NotBlank @Size(max = 160) String fullName,
        @Size(max = 64) @Pattern(regexp = "^(?=.*\\d)[0-9+()\\-\\s]{3,64}$", message = "Phone format is invalid.") String phone,
        @Size(max = 320) String email,
        @Size(max = 200) String companyName,
        @Size(max = 5000) String message,
        @Size(max = 5000) String internalNote
) {
}
