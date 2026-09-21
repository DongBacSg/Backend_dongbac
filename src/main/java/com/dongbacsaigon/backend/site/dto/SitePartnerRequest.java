package com.dongbacsaigon.backend.site.dto;

import java.util.UUID;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record SitePartnerRequest(
        @NotBlank
        @Size(max = 160)
        String name,

        @NotNull
        UUID logoMediaId,

        String websiteUrl,

        @Min(0)
        int sortOrder,

        boolean active
) {
}
