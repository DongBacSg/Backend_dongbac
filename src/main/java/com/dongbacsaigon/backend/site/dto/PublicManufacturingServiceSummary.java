package com.dongbacsaigon.backend.site.dto;

import java.util.UUID;

import com.dongbacsaigon.backend.media.dto.PublicMediaResponse;

public record PublicManufacturingServiceSummary(
        UUID id,
        String slug,
        String title,
        String summary,
        PublicMediaResponse featuredImage,
        String seoTitle
) {
}
