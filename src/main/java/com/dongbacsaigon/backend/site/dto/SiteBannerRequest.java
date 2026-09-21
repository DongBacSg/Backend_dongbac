package com.dongbacsaigon.backend.site.dto;

import java.util.UUID;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record SiteBannerRequest(
        @NotNull
        UUID mediaId,

        @Size(max = 255)
        String altText,

        @Min(0)
        int sortOrder,

        boolean active
) {
}
