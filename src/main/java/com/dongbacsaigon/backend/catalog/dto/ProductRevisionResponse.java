package com.dongbacsaigon.backend.catalog.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.dongbacsaigon.backend.catalog.entity.ProductRevisionStatus;

public record ProductRevisionResponse(
        UUID id,
        UUID productId,
        long revisionNumber,
        ProductRevisionStatus status,
        CategoryResponse category,
        String name,
        String slug,
        String shortDescription,
        String content,
        String seoTitle,
        String seoDescription,
        List<ProductImageResponse> images,
        List<RelatedProductResponse> relatedProducts,
        CatalogUserSummary createdBy,
        Instant createdAt,
        Instant updatedAt,
        Instant publishedAt
) {
}
