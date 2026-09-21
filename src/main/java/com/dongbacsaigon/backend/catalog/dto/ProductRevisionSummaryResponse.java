package com.dongbacsaigon.backend.catalog.dto;

import java.time.Instant;
import java.util.UUID;

import com.dongbacsaigon.backend.catalog.entity.ProductRevisionStatus;

public record ProductRevisionSummaryResponse(
        UUID id,
        long revisionNumber,
        ProductRevisionStatus status,
        String name,
        String slug,
        CategoryResponse category,
        CatalogUserSummary createdBy,
        Instant createdAt,
        Instant updatedAt,
        Instant publishedAt
) {
}
