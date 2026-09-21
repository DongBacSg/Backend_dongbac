package com.dongbacsaigon.backend.article.approval;

import java.time.Instant;
import java.util.UUID;

import com.dongbacsaigon.backend.approval.entity.ApprovalRequest;
import com.dongbacsaigon.backend.approval.entity.ApprovalResourceType;
import com.dongbacsaigon.backend.approval.service.ApprovalResourceHandler;
import com.dongbacsaigon.backend.article.entity.Article;
import com.dongbacsaigon.backend.article.entity.ArticlePublicationStatus;
import com.dongbacsaigon.backend.article.entity.ArticleRevision;
import com.dongbacsaigon.backend.article.entity.ArticleRevisionStatus;
import com.dongbacsaigon.backend.article.repository.ArticleRepository;
import com.dongbacsaigon.backend.article.repository.ArticleRevisionRepository;
import com.dongbacsaigon.backend.article.service.ArticleValidationService;
import com.dongbacsaigon.backend.audit.entity.AuditAction;
import com.dongbacsaigon.backend.audit.entity.AuditTargetType;
import com.dongbacsaigon.backend.audit.service.AuditService;
import com.dongbacsaigon.backend.common.exception.ApiException;
import com.dongbacsaigon.backend.user.entity.User;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class ArticleApprovalHandler implements ApprovalResourceHandler {

    private final ArticleRepository articleRepository;
    private final ArticleRevisionRepository revisionRepository;
    private final ArticleValidationService validationService;
    private final AuditService auditService;

    public ArticleApprovalHandler(ArticleRepository articleRepository, ArticleRevisionRepository revisionRepository, ArticleValidationService validationService, AuditService auditService) {
        this.articleRepository = articleRepository;
        this.revisionRepository = revisionRepository;
        this.validationService = validationService;
        this.auditService = auditService;
    }

    @Override
    public boolean supports(ApprovalResourceType resourceType) { return resourceType == ApprovalResourceType.ARTICLE; }

    @Override
    public void validateCanSubmit(ApprovalResourceType resourceType, UUID resourceId, long resourceVersion, UUID submittedBy) {
        Article article = requireArticle(resourceId);
        requireNotArchived(article);
        ArticleRevision revision = requireRevision(resourceId, resourceVersion);
        if (revision.getStatus() != ArticleRevisionStatus.DRAFT) throw new ApiException(HttpStatus.CONFLICT, "Only a draft article revision can be submitted.");
        validationService.validateStoredRevision(revision);
    }

    @Override
    public void onApproved(ApprovalRequest request) {
        if (request.getReviewedBy() == null) throw new ApiException(HttpStatus.CONFLICT, "Article approval requires a reviewer.");
        publish(request, request.getReviewedBy());
    }

    @Override
    public void onApproved(ApprovalRequest request, User reviewer) { publish(request, reviewer); }

    @Override
    public void onRejected(ApprovalRequest request) { reject(request, request.getReviewedBy()); }

    @Override
    public void onRejected(ApprovalRequest request, User reviewer) { reject(request, reviewer); }

    private void publish(ApprovalRequest request, User reviewer) {
        Article article = requireForUpdate(request.getResourceId());
        requireNotArchived(article);
        ArticleRevision revision = requireForUpdate(article.getId(), request.getResourceVersion());
        if (revision.getStatus() != ArticleRevisionStatus.PENDING_REVIEW) throw new ApiException(HttpStatus.CONFLICT, "Article revision is not pending review.");
        validationService.validateStoredRevision(revision);
        ArticleRevision previous = article.getCurrentPublishedRevision();
        if (previous != null && !previous.getId().equals(revision.getId())) {
            previous.archive();
            revisionRepository.flush();
        }
        revision.publish(reviewer, Instant.now());
        article.publish(revision);
        auditService.recordSuccessAfterCommit(reviewer, AuditAction.ARTICLE_REVISION_PUBLISHED, AuditTargetType.ARTICLE_REVISION, revision.getId(), "revision " + revision.getRevisionNumber());
    }

    private void reject(ApprovalRequest request, User reviewer) {
        Article article = requireForUpdate(request.getResourceId());
        ArticleRevision revision = requireForUpdate(article.getId(), request.getResourceVersion());
        if (revision.getStatus() != ArticleRevisionStatus.PENDING_REVIEW) throw new ApiException(HttpStatus.CONFLICT, "Article revision is not pending review.");
        revision.reject();
        if (reviewer != null) auditService.recordSuccessAfterCommit(reviewer, AuditAction.ARTICLE_REVISION_REJECTED, AuditTargetType.ARTICLE_REVISION, revision.getId(), "revision " + revision.getRevisionNumber());
    }

    private Article requireArticle(UUID id) { return articleRepository.findById(id).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Article not found.")); }
    private Article requireForUpdate(UUID id) { return articleRepository.findByIdForUpdate(id).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Article not found.")); }
    private ArticleRevision requireRevision(UUID id, long number) { return revisionRepository.findByArticleIdAndRevisionNumber(id, number).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Article revision not found.")); }
    private ArticleRevision requireForUpdate(UUID id, long number) { return revisionRepository.findByArticleAndNumberForUpdate(id, number).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Article revision not found.")); }
    private void requireNotArchived(Article article) { if (article.getPublicationStatus() == ArticlePublicationStatus.ARCHIVED) throw new ApiException(HttpStatus.CONFLICT, "Archived articles cannot be reviewed."); }
}
