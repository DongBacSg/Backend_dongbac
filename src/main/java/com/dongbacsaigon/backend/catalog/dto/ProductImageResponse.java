package com.dongbacsaigon.backend.catalog.dto;

import com.dongbacsaigon.backend.media.dto.PublicMediaResponse;

public record ProductImageResponse(
        PublicMediaResponse media,
        int sortOrder,
        boolean primary
) {
}
