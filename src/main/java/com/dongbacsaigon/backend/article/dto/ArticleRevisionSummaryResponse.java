package com.dongbacsaigon.backend.article.dto;

import java.time.Instant;
import java.util.UUID;

import com.dongbacsaigon.backend.article.entity.ArticleRevisionStatus;
import com.dongbacsaigon.backend.article.entity.ArticleType;
import com.dongbacsaigon.backend.catalog.dto.CatalogUserSummary;

public record ArticleRevisionSummaryResponse(
        UUID id, long revisionNumber, ArticleRevisionStatus status, ArticleType articleType,
        String title, String slug, CatalogUserSummary createdBy, Instant createdAt, Instant updatedAt, Instant publishedAt
) {
}
