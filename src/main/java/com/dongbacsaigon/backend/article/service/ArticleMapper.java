package com.dongbacsaigon.backend.article.service;

import java.util.List;

import com.dongbacsaigon.backend.article.dto.ArticleMediaResponse;
import com.dongbacsaigon.backend.article.dto.ArticleRevisionResponse;
import com.dongbacsaigon.backend.article.dto.ArticleRevisionSummaryResponse;
import com.dongbacsaigon.backend.article.entity.ArticleRevision;
import com.dongbacsaigon.backend.article.entity.ArticleRevisionMedia;
import com.dongbacsaigon.backend.catalog.service.CatalogMapper;
import com.dongbacsaigon.backend.media.service.MediaMapper;

public final class ArticleMapper {

    private ArticleMapper() {
    }

    public static ArticleRevisionSummaryResponse toSummary(ArticleRevision revision) {
        if (revision == null) return null;
        return new ArticleRevisionSummaryResponse(
                revision.getId(), revision.getRevisionNumber(), revision.getStatus(), revision.getArticleType(),
                revision.getTitle(), revision.getSlug(), CatalogMapper.toUserSummary(revision.getCreatedBy()),
                revision.getCreatedAt(), revision.getUpdatedAt(), revision.getPublishedAt()
        );
    }

    public static ArticleMediaResponse toMediaResponse(ArticleRevisionMedia relation) {
        return new ArticleMediaResponse(
                MediaMapper.toPublicResponse(relation.getMedia()), relation.getUsageType(), relation.getSortOrder(), relation.getCaption()
        );
    }

    public static ArticleRevisionResponse toResponse(ArticleRevision revision, List<ArticleRevisionMedia> media) {
        return new ArticleRevisionResponse(
                revision.getId(), revision.getArticle().getId(), revision.getRevisionNumber(), revision.getStatus(), revision.getArticleType(),
                revision.getTitle(), revision.getSlug(), revision.getSummary(), revision.getContent(), revision.getSeoTitle(), revision.getSeoDescription(),
                media.stream().map(ArticleMapper::toMediaResponse).toList(), CatalogMapper.toUserSummary(revision.getCreatedBy()),
                revision.getCreatedAt(), revision.getUpdatedAt(), revision.getPublishedAt()
        );
    }
}
