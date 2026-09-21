package com.dongbacsaigon.backend.catalog.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record PublicProductResponse(
        UUID id,
        String name,
        String slug,
        String shortDescription,
        String content,
        String seoTitle,
        String seoDescription,
        CategoryResponse category,
        ProductImageResponse primaryImage,
        List<ProductImageResponse> images,
        List<PublicProductSummaryResponse> relatedProducts,
        Instant publishedAt
) {
}
