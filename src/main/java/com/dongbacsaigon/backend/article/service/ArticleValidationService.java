package com.dongbacsaigon.backend.article.service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.dongbacsaigon.backend.article.dto.ArticleDraftRequest;
import com.dongbacsaigon.backend.article.dto.ArticleMediaRequest;
import com.dongbacsaigon.backend.article.entity.ArticleMediaUsageType;
import com.dongbacsaigon.backend.article.entity.ArticleRevision;
import com.dongbacsaigon.backend.article.entity.ArticleRevisionMedia;
import com.dongbacsaigon.backend.article.entity.ArticleRevisionStatus;
import com.dongbacsaigon.backend.article.entity.ArticleType;
import com.dongbacsaigon.backend.article.repository.ArticleRevisionMediaRepository;
import com.dongbacsaigon.backend.article.repository.ArticleRevisionRepository;
import com.dongbacsaigon.backend.catalog.service.SlugService;
import com.dongbacsaigon.backend.common.exception.ApiException;
import com.dongbacsaigon.backend.media.entity.Media;
import com.dongbacsaigon.backend.media.service.MediaService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class ArticleValidationService {

    private final ArticleRevisionRepository revisionRepository;
    private final ArticleRevisionMediaRepository mediaRepository;
    private final MediaService mediaService;
    private final SlugService slugService;
    private final ArticleContentSanitizer contentSanitizer;

    public ArticleValidationService(
            ArticleRevisionRepository revisionRepository,
            ArticleRevisionMediaRepository mediaRepository,
            MediaService mediaService,
            SlugService slugService,
            ArticleContentSanitizer contentSanitizer
    ) {
        this.revisionRepository = revisionRepository;
        this.mediaRepository = mediaRepository;
        this.mediaService = mediaService;
        this.slugService = slugService;
        this.contentSanitizer = contentSanitizer;
    }

    public ResolvedDraft resolveDraft(ArticleDraftRequest request, UUID articleId) {
        ArticleType type = request.articleType();
        if (type == null) throw new ApiException(HttpStatus.BAD_REQUEST, "Article type is required.");
        String title = normalizeRequired(request.title(), "Article title is required.");
        String slug = slugService.normalize(StringUtils.hasText(request.slug()) ? request.slug() : title);
        requirePublishedSlugAvailable(slug, articleId);
        String content = contentSanitizer.sanitizeRequired(request.content());
        List<ArticleMediaRequest> requests = request.media() == null ? List.of() : request.media();
        if (requests.stream().filter(item -> item.usageType() == ArticleMediaUsageType.FEATURED).count() > 1) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "An article revision can have at most one featured image.");
        }
        Set<MediaKey> keys = new HashSet<>();
        List<ResolvedMedia> resolvedMedia = new ArrayList<>();
        for (ArticleMediaRequest item : requests) {
            MediaKey key = new MediaKey(item.mediaId(), item.usageType());
            if (!keys.add(key)) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Article media relationship is duplicated.");
            }
            Media media = mediaService.requireActiveImage(item.mediaId());
            resolvedMedia.add(new ResolvedMedia(media, item.usageType(), item.sortOrder(), normalizeOptional(item.caption())));
        }
        return new ResolvedDraft(
                type, title, slug, normalizeOptional(request.summary()), content,
                normalizeOptional(request.seoTitle()), normalizeOptional(request.seoDescription()), resolvedMedia
        );
    }

    public void validateStoredRevision(ArticleRevision revision) {
        if (revision.getArticleType() == null) throw new ApiException(HttpStatus.CONFLICT, "Article type is invalid.");
        normalizeRequired(revision.getTitle(), "Article title is required.");
        contentSanitizer.sanitizeRequired(revision.getContent());
        requirePublishedSlugAvailable(revision.getSlug(), revision.getArticle().getId());
        List<ArticleRevisionMedia> relations = mediaRepository.findByRevisionIdOrderByUsageTypeAscSortOrderAsc(revision.getId());
        if (relations.stream().filter(item -> item.getUsageType() == ArticleMediaUsageType.FEATURED).count() > 1) {
            throw new ApiException(HttpStatus.CONFLICT, "Article revision has multiple featured images.");
        }
        Set<MediaKey> keys = new HashSet<>();
        for (ArticleRevisionMedia relation : relations) {
            if (!keys.add(new MediaKey(relation.getMedia().getId(), relation.getUsageType()))) {
                throw new ApiException(HttpStatus.CONFLICT, "Article revision contains duplicate media.");
            }
            mediaService.requireActiveImage(relation.getMedia().getId());
        }
    }

    public void requirePublishedSlugAvailable(String slug, UUID articleId) {
        if (revisionRepository.existsByStatusAndSlugAndArticleIdNot(ArticleRevisionStatus.PUBLISHED, slug, articleId)) {
            throw new ApiException(HttpStatus.CONFLICT, "Article slug is already used by another published article.");
        }
    }

    private String normalizeRequired(String value, String message) {
        if (!StringUtils.hasText(value)) throw new ApiException(HttpStatus.BAD_REQUEST, message);
        return value.trim();
    }

    private String normalizeOptional(String value) { return StringUtils.hasText(value) ? value.trim() : null; }

    private record MediaKey(UUID mediaId, ArticleMediaUsageType usageType) { }

    public record ResolvedMedia(Media media, ArticleMediaUsageType usageType, int sortOrder, String caption) { }

    public record ResolvedDraft(
            ArticleType articleType, String title, String slug, String summary, String content,
            String seoTitle, String seoDescription, List<ResolvedMedia> media
    ) { }
}
