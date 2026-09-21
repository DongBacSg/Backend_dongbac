package com.dongbacsaigon.backend.catalog.dto;

import java.time.Instant;
import java.util.UUID;

import com.dongbacsaigon.backend.catalog.entity.ProductPublicationStatus;

public record AdminProductResponse(
        UUID id,
        ProductPublicationStatus publicationStatus,
        long latestRevisionNumber,
        ProductRevisionSummaryResponse currentPublishedRevision,
        ProductRevisionSummaryResponse latestRevision,
        ProductRevisionSummaryResponse currentDraft,
        CatalogUserSummary createdBy,
        Instant createdAt,
        Instant updatedAt
) {
}
