package com.dongbacsaigon.backend.catalog.dto;

import java.time.Instant;
import java.util.UUID;

public record CategoryResponse(
        UUID id,
        String name,
        String slug,
        String description,
        int sortOrder,
        boolean active,
        Instant createdAt,
        Instant updatedAt
) {
}
