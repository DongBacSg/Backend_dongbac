package com.dongbacsaigon.backend.media.dto;

import java.util.UUID;

import com.dongbacsaigon.backend.media.entity.MediaType;

public record PublicMediaResponse(
        UUID id,
        String secureUrl,
        MediaType resourceType,
        String format,
        Integer width,
        Integer height,
        String altText
) {
}
