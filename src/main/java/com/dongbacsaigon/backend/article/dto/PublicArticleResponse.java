package com.dongbacsaigon.backend.article.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.dongbacsaigon.backend.article.entity.ArticleType;

public record PublicArticleResponse(
        UUID id, ArticleType articleType, String title, String slug, String summary, String content,
        String seoTitle, String seoDescription, ArticleMediaResponse featuredImage,
        List<ArticleMediaResponse> contentMedia, Instant publishedAt
) {
}
