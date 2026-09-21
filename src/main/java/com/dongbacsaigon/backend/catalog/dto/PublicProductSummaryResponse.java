package com.dongbacsaigon.backend.catalog.dto;

import java.time.Instant;
import java.util.UUID;

public record PublicProductSummaryResponse(
        UUID id,
        String name,
        String slug,
        String shortDescription,
        CategoryResponse category,
        ProductImageResponse primaryImage,
        Instant publishedAt
) {
}
