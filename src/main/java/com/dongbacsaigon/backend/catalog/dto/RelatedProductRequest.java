package com.dongbacsaigon.backend.catalog.dto;

import java.util.UUID;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record RelatedProductRequest(
        @NotNull UUID productId,
        @Min(0) int sortOrder
) {
}
