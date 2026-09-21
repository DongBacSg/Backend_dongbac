package com.dongbacsaigon.backend.site.dto;

import java.util.UUID;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

public record SiteSettingsRequest(
        @Size(max = 160)
        String companyName,
        String slogan,
        String vision,
        String mission,
        String philosophy,
        String brandNarrative,
        String coreValues,
        UUID logoMediaId,
        String officeAddress,
        String factoryAddress,
        @Size(max = 64)
        String phone,
        @Email
        @Size(max = 320)
        String email,
        String facebookUrl,
        String youtubeUrl,
        String zaloUrl,
        String mapUrl,
        String tvcUrl
) {
}
