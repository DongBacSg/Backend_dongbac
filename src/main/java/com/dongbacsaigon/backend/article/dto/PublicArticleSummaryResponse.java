package com.dongbacsaigon.backend.article.dto;

import java.time.Instant;
import java.util.UUID;

import com.dongbacsaigon.backend.article.entity.ArticleType;

public record PublicArticleSummaryResponse(
        UUID id, ArticleType articleType, String title, String slug, String summary,
        ArticleMediaResponse featuredImage, Instant publishedAt
) {
}
