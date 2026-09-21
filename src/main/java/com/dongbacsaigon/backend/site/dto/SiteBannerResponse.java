package com.dongbacsaigon.backend.site.dto;

import java.time.Instant;
import java.util.UUID;

import com.dongbacsaigon.backend.media.dto.PublicMediaResponse;

public record SiteBannerResponse(
        UUID id,
        PublicMediaResponse media,
        String altText,
        int sortOrder,
        boolean active,
        Instant createdAt,
        Instant updatedAt
) {
}
