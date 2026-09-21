package com.dongbacsaigon.backend.site.dto;

import java.time.Instant;
import java.util.UUID;

import com.dongbacsaigon.backend.media.dto.PublicMediaResponse;

public record ManufacturingSectionResponse(
        UUID id,
        String title,
        String content,
        PublicMediaResponse media,
        int sortOrder,
        boolean active,
        Instant createdAt,
        Instant updatedAt
) {
}
