package com.dongbacsaigon.backend.article.dto;

import java.time.Instant;
import java.util.UUID;

import com.dongbacsaigon.backend.article.entity.ArticlePublicationStatus;

public record AdminArticleListItemResponse(
        UUID id, ArticlePublicationStatus publicationStatus, long latestRevisionNumber,
        ArticleRevisionSummaryResponse latestRevision, ArticleRevisionSummaryResponse currentPublishedRevision,
        Instant createdAt, Instant updatedAt
) {
}
