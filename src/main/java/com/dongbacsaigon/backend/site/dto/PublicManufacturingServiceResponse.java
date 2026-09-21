package com.dongbacsaigon.backend.site.dto;

import java.util.UUID;

import com.dongbacsaigon.backend.media.dto.PublicMediaResponse;

public record PublicManufacturingServiceResponse(
        UUID id,
        String slug,
        String title,
        String summary,
        String content,
        PublicMediaResponse featuredImage,
        String seoTitle,
        String seoDescription
) {
}
