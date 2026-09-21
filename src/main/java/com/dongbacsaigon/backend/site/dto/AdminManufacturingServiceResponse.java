package com.dongbacsaigon.backend.site.dto;

import java.time.Instant;
import java.util.UUID;

import com.dongbacsaigon.backend.media.dto.PublicMediaResponse;

public record AdminManufacturingServiceResponse(
        UUID id,
        String slug,
        String title,
        String summary,
        String content,
        PublicMediaResponse featuredImage,
        String seoTitle,
        String seoDescription,
        int sortOrder,
        boolean active,
        Instant createdAt,
        Instant updatedAt
) {
}
