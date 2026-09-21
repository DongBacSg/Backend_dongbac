package com.dongbacsaigon.backend.site.dto;

import java.time.Instant;
import java.util.UUID;

import com.dongbacsaigon.backend.media.dto.PublicMediaResponse;

public record SiteSettingsResponse(
        UUID id,
        String companyName,
        String slogan,
        String vision,
        String mission,
        String philosophy,
        String brandNarrative,
        String coreValues,
        PublicMediaResponse logo,
        String officeAddress,
        String factoryAddress,
        String phone,
        String email,
        String facebookUrl,
        String youtubeUrl,
        String zaloUrl,
        String mapUrl,
        String tvcUrl,
        Instant updatedAt
) {
}
