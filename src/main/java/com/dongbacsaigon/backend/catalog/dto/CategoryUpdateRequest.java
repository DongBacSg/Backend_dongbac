package com.dongbacsaigon.backend.catalog.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public record CategoryUpdateRequest(
        @Size(min = 1, max = 160) String name,
        @Size(min = 1, max = 180) String slug,
        @Size(max = 5000) String description,
        @Min(0) Integer sortOrder,
        Boolean active
) {
}
