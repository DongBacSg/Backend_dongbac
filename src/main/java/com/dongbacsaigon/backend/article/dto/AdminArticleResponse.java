package com.dongbacsaigon.backend.article.dto;

import java.time.Instant;
import java.util.UUID;

import com.dongbacsaigon.backend.article.entity.ArticlePublicationStatus;
import com.dongbacsaigon.backend.catalog.dto.CatalogUserSummary;

public record AdminArticleResponse(
        UUID id, ArticlePublicationStatus publicationStatus, long latestRevisionNumber,
        ArticleRevisionSummaryResponse currentPublishedRevision, ArticleRevisionSummaryResponse latestRevision,
        ArticleRevisionSummaryResponse currentDraft, ArticleMediaResponse featuredImage,
        CatalogUserSummary createdBy, Instant createdAt, Instant updatedAt
) {
}
