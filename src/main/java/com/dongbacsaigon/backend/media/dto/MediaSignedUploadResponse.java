package com.dongbacsaigon.backend.media.dto;

import java.time.Instant;
import java.util.UUID;

public record MediaSignedUploadResponse(
        UUID intentId,
        String cloudName,
        String apiKey,
        long timestamp,
        String signature,
        String folder,
        String publicId,
        String resourceType,
        String uploadUrl,
        Instant expiresAt
) {
}
