package com.dongbacsaigon.backend.site.dto;

import java.time.Instant;
import java.util.UUID;

import com.dongbacsaigon.backend.media.dto.PublicMediaResponse;

public record SitePartnerResponse(
        UUID id,
        String name,
        PublicMediaResponse logo,
        String websiteUrl,
        int sortOrder,
        boolean active,
        Instant createdAt,
        Instant updatedAt
) {
}
