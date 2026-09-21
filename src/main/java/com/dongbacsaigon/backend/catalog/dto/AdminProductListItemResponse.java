package com.dongbacsaigon.backend.catalog.dto;

import java.time.Instant;
import java.util.UUID;

import com.dongbacsaigon.backend.catalog.entity.ProductPublicationStatus;

public record AdminProductListItemResponse(
        UUID id,
        ProductPublicationStatus publicationStatus,
        long latestRevisionNumber,
        ProductRevisionSummaryResponse latestRevision,
        ProductRevisionSummaryResponse currentPublishedRevision,
        Instant createdAt,
        Instant updatedAt
) {
}
