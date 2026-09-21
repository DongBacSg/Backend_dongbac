package com.dongbacsaigon.backend.site.dto;

import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record ManufacturingServiceRequest(
        @NotBlank @Size(max = 160) String title,
        @Size(max = 180) String slug,
        @Size(max = 2000) String summary,
        @Size(max = 200000) String content,
        UUID featuredMediaId,
        @Size(max = 240) String seoTitle,
        @Size(max = 500) String seoDescription,
        @PositiveOrZero Integer sortOrder,
        Boolean active
) {
}
