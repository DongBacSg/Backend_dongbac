package com.dongbacsaigon.backend.article.dto;

import java.util.UUID;

import com.dongbacsaigon.backend.article.entity.ArticleMediaUsageType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ArticleMediaRequest(
        @NotNull UUID mediaId,
        @NotNull ArticleMediaUsageType usageType,
        @Min(0) int sortOrder,
        @Size(max = 500) String caption
) {
}
