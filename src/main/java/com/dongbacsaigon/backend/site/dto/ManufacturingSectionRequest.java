package com.dongbacsaigon.backend.site.dto;

import java.util.UUID;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ManufacturingSectionRequest(
        @NotBlank
        @Size(max = 160)
        String title,
        String content,
        UUID mediaId,
        @Min(0)
        int sortOrder,
        boolean active
) {
}
