package com.dongbacsaigon.backend.article.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.dongbacsaigon.backend.article.entity.ArticleRevisionStatus;
import com.dongbacsaigon.backend.article.entity.ArticleType;
import com.dongbacsaigon.backend.catalog.dto.CatalogUserSummary;

public record ArticleRevisionResponse(
        UUID id, UUID articleId, long revisionNumber, ArticleRevisionStatus status, ArticleType articleType,
        String title, String slug, String summary, String content, String seoTitle, String seoDescription,
        List<ArticleMediaResponse> media, CatalogUserSummary createdBy, Instant createdAt, Instant updatedAt, Instant publishedAt
) {
}
