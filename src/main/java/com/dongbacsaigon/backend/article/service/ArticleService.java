package com.dongbacsaigon.backend.article.service;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

import com.dongbacsaigon.backend.approval.entity.ApprovalResourceType;
import com.dongbacsaigon.backend.approval.repository.ApprovalRequestRepository;
import com.dongbacsaigon.backend.approval.service.ApprovalService;
import com.dongbacsaigon.backend.article.dto.AdminArticleListItemResponse;
import com.dongbacsaigon.backend.article.dto.AdminArticlePageResponse;
import com.dongbacsaigon.backend.article.dto.AdminArticleResponse;
import com.dongbacsaigon.backend.article.dto.ArticleDraftRequest;
import com.dongbacsaigon.backend.article.dto.ArticleMediaResponse;
import com.dongbacsaigon.backend.article.dto.ArticleRevisionResponse;
import com.dongbacsaigon.backend.article.dto.ArticleRevisionSummaryResponse;
import com.dongbacsaigon.backend.article.entity.Article;
import com.dongbacsaigon.backend.article.entity.ArticleMediaUsageType;
import com.dongbacsaigon.backend.article.entity.ArticlePublicationStatus;
import com.dongbacsaigon.backend.article.entity.ArticleRevision;
import com.dongbacsaigon.backend.article.entity.ArticleRevisionMedia;
import com.dongbacsaigon.backend.article.entity.ArticleRevisionStatus;
import com.dongbacsaigon.backend.article.entity.ArticleType;
import com.dongbacsaigon.backend.article.repository.ArticleRepository;
import com.dongbacsaigon.backend.article.repository.ArticleRevisionMediaRepository;
import com.dongbacsaigon.backend.article.repository.ArticleRevisionRepository;
import com.dongbacsaigon.backend.audit.entity.AuditAction;
import com.dongbacsaigon.backend.audit.entity.AuditTargetType;
import com.dongbacsaigon.backend.audit.service.AuditService;
import com.dongbacsaigon.backend.catalog.service.CatalogMapper;
import com.dongbacsaigon.backend.common.exception.ApiException;
import com.dongbacsaigon.backend.user.entity.User;
import com.dongbacsaigon.backend.user.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class ArticleService {

    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 100;

    private final ArticleRepository articleRepository;
    private final ArticleRevisionRepository revisionRepository;
    private final ArticleRevisionMediaRepository mediaRepository;
    private final ApprovalRequestRepository approvalRequestRepository;
    private final UserRepository userRepository;
    private final ArticleValidationService validationService;
    private final ApprovalService approvalService;
    private final AuditService auditService;

    public ArticleService(
            ArticleRepository articleRepository,
            ArticleRevisionRepository revisionRepository,
            ArticleRevisionMediaRepository mediaRepository,
            ApprovalRequestRepository approvalRequestRepository,
            UserRepository userRepository,
            ArticleValidationService validationService,
            ApprovalService approvalService,
            AuditService auditService
    ) {
        this.articleRepository = articleRepository;
        this.revisionRepository = revisionRepository;
        this.mediaRepository = mediaRepository;
        this.approvalRequestRepository = approvalRequestRepository;
        this.userRepository = userRepository;
        this.validationService = validationService;
        this.approvalService = approvalService;
        this.auditService = auditService;
    }

    @Transactional
    public ArticleRevisionResponse create(ArticleDraftRequest request, UUID actorId) {
        User actor = requireUser(actorId);
        Article article = articleRepository.save(new Article(actor));
        ArticleValidationService.ResolvedDraft draft = validationService.resolveDraft(request, article.getId());
        ArticleRevision revision = createRevisionEntity(article, article.nextRevisionNumber(), draft, actor);
        revisionRepository.save(revision);
        saveMedia(revision, draft.media());
        auditService.recordSuccessAfterCommit(actor, AuditAction.ARTICLE_CREATED, AuditTargetType.ARTICLE, article.getId(), "revision 1 " + draft.articleType());
        return toResponse(revision);
    }

    @Transactional
    public ArticleRevisionResponse updateDraft(UUID articleId, UUID revisionId, ArticleDraftRequest request, UUID actorId) {
        Article article = requireForUpdate(articleId);
        requireNotArchived(article);
        ArticleRevision revision = requireRevision(articleId, revisionId);
        requireDraft(revision);
        ArticleValidationService.ResolvedDraft draft = validationService.resolveDraft(request, articleId);
        revision.updateDraft(draft.articleType(), draft.title(), draft.slug(), draft.summary(), draft.content(), draft.seoTitle(), draft.seoDescription());
        mediaRepository.deleteByRevisionId(revisionId);
        mediaRepository.flush();
        saveMedia(revision, draft.media());
        User actor = requireUser(actorId);
        auditService.recordSuccessAfterCommit(actor, AuditAction.ARTICLE_REVISION_UPDATED, AuditTargetType.ARTICLE_REVISION, revisionId, "revision " + revision.getRevisionNumber());
        return toResponse(revision);
    }

    @Transactional
    public ArticleRevisionResponse createRevision(UUID articleId, UUID actorId) {
        Article article = requireForUpdate(articleId);
        requireNotArchived(article);
        if (revisionRepository.existsByArticleIdAndStatusIn(articleId, List.of(ArticleRevisionStatus.DRAFT, ArticleRevisionStatus.PENDING_REVIEW))) {
            throw new ApiException(HttpStatus.CONFLICT, "Article already has an active draft or pending revision.");
        }
        ArticleRevision latest = revisionRepository.findFirstByArticleIdOrderByRevisionNumberDesc(articleId)
                .orElseThrow(() -> new ApiException(HttpStatus.CONFLICT, "Article has no revision to clone."));
        ArticleRevision source = latest.getStatus() == ArticleRevisionStatus.REJECTED ? latest : article.getCurrentPublishedRevision();
        if (source == null) throw new ApiException(HttpStatus.CONFLICT, "Article has no rejected or published revision to clone.");
        User actor = requireUser(actorId);
        ArticleRevision revision = new ArticleRevision(
                article, article.nextRevisionNumber(), source.getArticleType(), source.getTitle(), source.getSlug(), source.getSummary(),
                source.getContent(), source.getSeoTitle(), source.getSeoDescription(), actor
        );
        revisionRepository.save(revision);
        mediaRepository.saveAll(mediaRepository.findByRevisionIdOrderByUsageTypeAscSortOrderAsc(source.getId()).stream()
                .map(item -> new ArticleRevisionMedia(revision, item.getMedia(), item.getUsageType(), item.getSortOrder(), item.getCaption()))
                .toList());
        auditService.recordSuccessAfterCommit(actor, AuditAction.ARTICLE_REVISION_CREATED, AuditTargetType.ARTICLE_REVISION, revision.getId(), "revision " + revision.getRevisionNumber());
        return toResponse(revision);
    }

    @Transactional
    public ArticleRevisionResponse submit(UUID articleId, UUID revisionId, UUID actorId) {
        Article article = requireForUpdate(articleId);
        requireNotArchived(article);
        ArticleRevision revision = requireRevision(articleId, revisionId);
        requireDraft(revision);
        validationService.validateStoredRevision(revision);
        approvalService.submitForReview(ApprovalResourceType.ARTICLE, articleId, revision.getRevisionNumber(), actorId);
        revision.submit();
        User actor = requireUser(actorId);
        auditService.recordSuccessAfterCommit(actor, AuditAction.ARTICLE_REVISION_SUBMITTED, AuditTargetType.ARTICLE_REVISION, revisionId, "revision " + revision.getRevisionNumber());
        return toResponse(revision);
    }

    @Transactional(readOnly = true)
    public AdminArticlePageResponse list(int page, int size, String search, ArticleType type, ArticlePublicationStatus publicationStatus, ArticleRevisionStatus revisionStatus) {
        Page<ArticleRevision> revisions = revisionRepository.findAdminPage(
                normalizeSearch(search), type, publicationStatus, revisionStatus,
                PageRequest.of(validatePage(page), validateSize(size), Sort.by(Sort.Direction.DESC, "updatedAt"))
        );
        List<AdminArticleListItemResponse> content = revisions.getContent().stream().map(latest -> {
            Article article = latest.getArticle();
            return new AdminArticleListItemResponse(
                    article.getId(), article.getPublicationStatus(), article.getLatestRevisionNumber(), ArticleMapper.toSummary(latest),
                    ArticleMapper.toSummary(article.getCurrentPublishedRevision()), article.getCreatedAt(), article.getUpdatedAt()
            );
        }).toList();
        return new AdminArticlePageResponse(content, revisions.getNumber(), revisions.getSize(), revisions.getTotalElements(), revisions.getTotalPages());
    }

    @Transactional(readOnly = true)
    public AdminArticleResponse get(UUID articleId) {
        Article article = articleRepository.findDetailedById(articleId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Article not found."));
        List<ArticleRevision> revisions = revisionRepository.findByArticleIdOrderByRevisionNumberDesc(articleId);
        ArticleRevision latest = revisions.isEmpty() ? null : revisions.get(0);
        ArticleRevision draft = revisions.stream().filter(item -> item.getStatus() == ArticleRevisionStatus.DRAFT).findFirst().orElse(null);
        ArticleMediaResponse featured = article.getCurrentPublishedRevision() == null ? null : mediaRepository
                .findByRevisionIdOrderByUsageTypeAscSortOrderAsc(article.getCurrentPublishedRevision().getId()).stream()
                .filter(item -> item.getUsageType() == ArticleMediaUsageType.FEATURED)
                .findFirst().map(ArticleMapper::toMediaResponse).orElse(null);
        return new AdminArticleResponse(
                article.getId(), article.getPublicationStatus(), article.getLatestRevisionNumber(), ArticleMapper.toSummary(article.getCurrentPublishedRevision()),
                ArticleMapper.toSummary(latest), ArticleMapper.toSummary(draft), featured, CatalogMapper.toUserSummary(article.getCreatedBy()),
                article.getCreatedAt(), article.getUpdatedAt()
        );
    }

    @Transactional(readOnly = true)
    public List<ArticleRevisionSummaryResponse> listRevisions(UUID articleId) {
        requireArticle(articleId);
        return revisionRepository.findByArticleIdOrderByRevisionNumberDesc(articleId).stream().map(ArticleMapper::toSummary).toList();
    }

    @Transactional(readOnly = true)
    public ArticleRevisionResponse getRevision(UUID articleId, UUID revisionId) { return toResponse(requireRevision(articleId, revisionId)); }

    @Transactional
    public void deleteDraftArticle(UUID articleId, UUID actorId) {
        Article article = requireForUpdate(articleId);
        List<ArticleRevision> revisions = revisionRepository.findByArticleIdOrderByRevisionNumberDesc(articleId);
        boolean unsafe = article.getCurrentPublishedRevision() != null
                || article.getPublicationStatus() != ArticlePublicationStatus.DRAFT
                || revisions.stream().anyMatch(item -> item.getStatus() != ArticleRevisionStatus.DRAFT)
                || approvalRequestRepository.existsByResourceTypeAndResourceId(ApprovalResourceType.ARTICLE, articleId);
        if (unsafe) throw new ApiException(HttpStatus.CONFLICT, "Article cannot be hard deleted after submission or publication.");
        for (ArticleRevision revision : revisions) mediaRepository.deleteByRevisionId(revision.getId());
        mediaRepository.flush();
        revisionRepository.deleteAll(revisions);
        revisionRepository.flush();
        articleRepository.delete(article);
        User actor = requireUser(actorId);
        auditService.recordSuccessAfterCommit(actor, AuditAction.ARTICLE_DRAFT_DELETED, AuditTargetType.ARTICLE, articleId, null);
    }

    @Transactional
    public AdminArticleResponse unpublish(UUID articleId, UUID actorId) {
        Article article = requireForUpdate(articleId);
        if (article.getPublicationStatus() != ArticlePublicationStatus.PUBLISHED) throw new ApiException(HttpStatus.CONFLICT, "Only a published article can be unpublished.");
        article.unpublish();
        User actor = requireUser(actorId);
        auditService.recordSuccessAfterCommit(actor, AuditAction.ARTICLE_UNPUBLISHED, AuditTargetType.ARTICLE, articleId, null);
        return get(articleId);
    }

    @Transactional
    public AdminArticleResponse republish(UUID articleId, UUID actorId) {
        Article article = requireForUpdate(articleId);
        if (article.getPublicationStatus() != ArticlePublicationStatus.UNPUBLISHED || article.getCurrentPublishedRevision() == null
                || article.getCurrentPublishedRevision().getStatus() != ArticleRevisionStatus.PUBLISHED) {
            throw new ApiException(HttpStatus.CONFLICT, "Only an unpublished article with an approved current revision can be republished.");
        }
        validationService.validateStoredRevision(article.getCurrentPublishedRevision());
        article.republish();
        User actor = requireUser(actorId);
        auditService.recordSuccessAfterCommit(actor, AuditAction.ARTICLE_REPUBLISHED, AuditTargetType.ARTICLE, articleId, null);
        return get(articleId);
    }

    @Transactional
    public AdminArticleResponse archive(UUID articleId, UUID actorId) {
        Article article = requireForUpdate(articleId);
        if (article.getPublicationStatus() == ArticlePublicationStatus.ARCHIVED) throw new ApiException(HttpStatus.CONFLICT, "Article is already archived.");
        article.archive();
        User actor = requireUser(actorId);
        auditService.recordSuccessAfterCommit(actor, AuditAction.ARTICLE_ARCHIVED, AuditTargetType.ARTICLE, articleId, null);
        return get(articleId);
    }

    private ArticleRevision createRevisionEntity(Article article, long number, ArticleValidationService.ResolvedDraft draft, User actor) {
        return new ArticleRevision(article, number, draft.articleType(), draft.title(), draft.slug(), draft.summary(), draft.content(), draft.seoTitle(), draft.seoDescription(), actor);
    }

    private void saveMedia(ArticleRevision revision, List<ArticleValidationService.ResolvedMedia> media) {
        mediaRepository.saveAll(media.stream().map(item -> new ArticleRevisionMedia(revision, item.media(), item.usageType(), item.sortOrder(), item.caption())).toList());
    }

    private ArticleRevisionResponse toResponse(ArticleRevision revision) {
        return ArticleMapper.toResponse(revision, mediaRepository.findByRevisionIdOrderByUsageTypeAscSortOrderAsc(revision.getId()));
    }

    private Article requireForUpdate(UUID id) { return articleRepository.findByIdForUpdate(id).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Article not found.")); }
    private Article requireArticle(UUID id) { return articleRepository.findById(id).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Article not found.")); }
    private ArticleRevision requireRevision(UUID articleId, UUID revisionId) { return revisionRepository.findByIdAndArticleId(revisionId, articleId).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Article revision not found.")); }
    private User requireUser(UUID id) { return userRepository.findById(id).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found.")); }
    private void requireDraft(ArticleRevision revision) { if (revision.getStatus() != ArticleRevisionStatus.DRAFT) throw new ApiException(HttpStatus.CONFLICT, "Only a draft article revision can be modified or submitted."); }
    private void requireNotArchived(Article article) { if (article.getPublicationStatus() == ArticlePublicationStatus.ARCHIVED) throw new ApiException(HttpStatus.CONFLICT, "Archived articles cannot be changed."); }
    private String normalizeSearch(String search) { return StringUtils.hasText(search) ? search.trim().toLowerCase(Locale.ROOT) : null; }
    private int validatePage(int page) { if (page < 0) throw new ApiException(HttpStatus.BAD_REQUEST, "Page must be greater than or equal to 0."); return page; }
    private int validateSize(int size) { if (size == 0) return DEFAULT_PAGE_SIZE; if (size < 1 || size > MAX_PAGE_SIZE) throw new ApiException(HttpStatus.BAD_REQUEST, "Size must be between 1 and 100."); return size; }
}
