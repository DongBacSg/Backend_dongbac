package com.dongbacsaigon.backend.article.service;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import com.dongbacsaigon.backend.article.dto.ArticleMediaResponse;
import com.dongbacsaigon.backend.article.dto.PublicArticlePageResponse;
import com.dongbacsaigon.backend.article.dto.PublicArticleResponse;
import com.dongbacsaigon.backend.article.dto.PublicArticleSummaryResponse;
import com.dongbacsaigon.backend.article.entity.ArticleMediaUsageType;
import com.dongbacsaigon.backend.article.entity.ArticlePublicationStatus;
import com.dongbacsaigon.backend.article.entity.ArticleRevision;
import com.dongbacsaigon.backend.article.entity.ArticleRevisionMedia;
import com.dongbacsaigon.backend.article.entity.ArticleRevisionStatus;
import com.dongbacsaigon.backend.article.entity.ArticleType;
import com.dongbacsaigon.backend.article.repository.ArticleRevisionMediaRepository;
import com.dongbacsaigon.backend.article.repository.ArticleRevisionRepository;
import com.dongbacsaigon.backend.catalog.service.SlugService;
import com.dongbacsaigon.backend.common.exception.ApiException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class PublicArticleService {

    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 100;
    private final ArticleRevisionRepository revisionRepository;
    private final ArticleRevisionMediaRepository mediaRepository;
    private final SlugService slugService;

    public PublicArticleService(ArticleRevisionRepository revisionRepository, ArticleRevisionMediaRepository mediaRepository, SlugService slugService) {
        this.revisionRepository = revisionRepository;
        this.mediaRepository = mediaRepository;
        this.slugService = slugService;
    }

    @Transactional(readOnly = true)
    public PublicArticlePageResponse list(int page, int size, ArticleType type, String search) {
        Page<ArticleRevision> revisions = revisionRepository.findPublicPage(
                ArticlePublicationStatus.PUBLISHED, ArticleRevisionStatus.PUBLISHED, type, normalizeSearch(search),
                PageRequest.of(validatePage(page), validateSize(size), Sort.by(Sort.Direction.DESC, "publishedAt").and(Sort.by("id")))
        );
        List<UUID> ids = revisions.getContent().stream().map(ArticleRevision::getId).toList();
        Map<UUID, List<ArticleRevisionMedia>> mediaByRevision = ids.isEmpty() ? Map.of() : mediaRepository
                .findByRevisionIdInOrderByRevisionIdAscUsageTypeAscSortOrderAsc(ids).stream()
                .collect(Collectors.groupingBy(item -> item.getRevision().getId()));
        List<PublicArticleSummaryResponse> content = revisions.getContent().stream()
                .map(revision -> toSummary(revision, mediaByRevision.getOrDefault(revision.getId(), List.of())))
                .toList();
        return new PublicArticlePageResponse(content, revisions.getNumber(), revisions.getSize(), revisions.getTotalElements(), revisions.getTotalPages());
    }

    @Transactional(readOnly = true)
    public PublicArticleResponse getBySlug(String value) {
        String slug = slugService.normalize(value);
        ArticleRevision revision = revisionRepository.findPublicBySlug(slug, ArticlePublicationStatus.PUBLISHED, ArticleRevisionStatus.PUBLISHED)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Article not found."));
        List<ArticleRevisionMedia> media = mediaRepository.findByRevisionIdOrderByUsageTypeAscSortOrderAsc(revision.getId());
        ArticleMediaResponse featured = media.stream().filter(item -> item.getUsageType() == ArticleMediaUsageType.FEATURED).findFirst().map(ArticleMapper::toMediaResponse).orElse(null);
        List<ArticleMediaResponse> contentMedia = media.stream().filter(item -> item.getUsageType() == ArticleMediaUsageType.CONTENT).map(ArticleMapper::toMediaResponse).toList();
        return new PublicArticleResponse(
                revision.getArticle().getId(), revision.getArticleType(), revision.getTitle(), revision.getSlug(), revision.getSummary(), revision.getContent(),
                revision.getSeoTitle(), revision.getSeoDescription(), featured, contentMedia, revision.getPublishedAt()
        );
    }

    private PublicArticleSummaryResponse toSummary(ArticleRevision revision, List<ArticleRevisionMedia> media) {
        ArticleMediaResponse featured = media.stream().filter(item -> item.getUsageType() == ArticleMediaUsageType.FEATURED).findFirst().map(ArticleMapper::toMediaResponse).orElse(null);
        return new PublicArticleSummaryResponse(revision.getArticle().getId(), revision.getArticleType(), revision.getTitle(), revision.getSlug(), revision.getSummary(), featured, revision.getPublishedAt());
    }

    private String normalizeSearch(String value) { return StringUtils.hasText(value) ? value.trim().toLowerCase(Locale.ROOT) : null; }
    private int validatePage(int page) { if (page < 0) throw new ApiException(HttpStatus.BAD_REQUEST, "Page must be greater than or equal to 0."); return page; }
    private int validateSize(int size) { if (size == 0) return DEFAULT_PAGE_SIZE; if (size < 1 || size > MAX_PAGE_SIZE) throw new ApiException(HttpStatus.BAD_REQUEST, "Size must be between 1 and 100."); return size; }
}
