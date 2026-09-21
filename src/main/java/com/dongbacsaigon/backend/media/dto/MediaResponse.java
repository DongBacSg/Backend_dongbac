package com.dongbacsaigon.backend.media.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.dongbacsaigon.backend.media.entity.MediaStatus;
import com.dongbacsaigon.backend.media.entity.MediaType;

public record MediaResponse(
        UUID id,
        String publicId,
        MediaType resourceType,
        String format,
        String secureUrl,
        Integer width,
        Integer height,
        long bytes,
        BigDecimal durationSeconds,
        String originalFilename,
        String altText,
        MediaStatus status,
        UUID uploadedBy,
        Instant createdAt,
        Instant updatedAt,
        Instant deletedAt,
        UUID deletedBy
) {
}
