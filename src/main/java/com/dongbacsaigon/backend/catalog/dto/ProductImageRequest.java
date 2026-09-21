package com.dongbacsaigon.backend.catalog.dto;

import java.util.UUID;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record ProductImageRequest(
        @NotNull UUID mediaId,
        @Min(0) int sortOrder,
        boolean primary
) {
}
