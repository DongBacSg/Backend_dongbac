package com.dongbacsaigon.backend.site.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.dongbacsaigon.backend.media.dto.PublicMediaResponse;

public record ManufacturingPageResponse(
        UUID id,
        String title,
        String introduction,
        PublicMediaResponse heroMedia,
        List<ManufacturingSectionResponse> sections,
        Instant updatedAt
) {
}
