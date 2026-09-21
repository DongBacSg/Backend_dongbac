package com.dongbacsaigon.backend.media.cloudinary;

import java.math.BigDecimal;

import com.dongbacsaigon.backend.media.entity.MediaType;

public record CloudinaryAsset(
        String assetId,
        String publicId,
        MediaType resourceType,
        String format,
        String secureUrl,
        Integer width,
        Integer height,
        long bytes,
        BigDecimal durationSeconds,
        String folder
) {
}
