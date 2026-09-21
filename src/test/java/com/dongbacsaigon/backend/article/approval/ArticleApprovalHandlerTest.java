package com.dongbacsaigon.backend.article.approval;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import com.dongbacsaigon.backend.approval.entity.ApprovalRequest;
import com.dongbacsaigon.backend.approval.entity.ApprovalResourceType;
import com.dongbacsaigon.backend.article.entity.Article;
import com.dongbacsaigon.backend.article.entity.ArticlePublicationStatus;
import com.dongbacsaigon.backend.article.entity.ArticleRevision;
import com.dongbacsaigon.backend.article.entity.ArticleRevisionStatus;
import com.dongbacsaigon.backend.article.entity.ArticleType;
import com.dongbacsaigon.backend.article.repository.ArticleRepository;
import com.dongbacsaigon.backend.article.repository.ArticleRevisionRepository;
import com.dongbacsaigon.backend.article.service.ArticleValidationService;
import com.dongbacsaigon.backend.audit.service.AuditService;
import com.dongbacsaigon.backend.common.exception.ApiException;
import com.dongbacsaigon.backend.user.entity.User;
import org.junit.jupiter.api.Test;

class ArticleApprovalHandlerTest {

    private final ArticleRepository articleRepository = mock(ArticleRepository.class);
    private final ArticleRevisionRepository revisionRepository = mock(ArticleRevisionRepository.class);
    private final ArticleValidationService validationService = mock(ArticleValidationService.class);
    private final AuditService auditService = mock(AuditService.class);
    private final ArticleApprovalHandler handler = new ArticleApprovalHandler(articleRepository, revisionRepository, validationService, auditService);

    @Test
    void firstApprovalPublishesArticleAndRevision() {
        Fixture fixture = fixture();
        stub(fixture);

        handler.onApproved(fixture.request(), fixture.admin());

        assertThat(fixture.article().getPublicationStatus()).isEqualTo(ArticlePublicationStatus.PUBLISHED);
        assertThat(fixture.article().getCurrentPublishedRevision()).isSameAs(fixture.revision());
        assertThat(fixture.revision().getStatus()).isEqualTo(ArticleRevisionStatus.PUBLISHED);
        assertThat(fixture.revision().getPublishedAt()).isNotNull();
    }

    @Test
    void newerApprovalArchivesPreviousRevision() {
        Fixture fixture = fixture();
        ArticleRevision previous = new ArticleRevision(fixture.article(), 2, ArticleType.NEWS, "Old", "old", null, "Old", null, null, fixture.admin());
        previous.publish(fixture.admin(), Instant.now());
        fixture.article().publish(previous);
        stub(fixture);

        handler.onApproved(fixture.request(), fixture.admin());

        assertThat(previous.getStatus()).isEqualTo(ArticleRevisionStatus.ARCHIVED);
        assertThat(fixture.article().getCurrentPublishedRevision()).isSameAs(fixture.revision());
        verify(revisionRepository).flush();
    }

    @Test
    void rejectionDoesNotChangePreviousPublicRevision() {
        Fixture fixture = fixture();
        ArticleRevision previous = new ArticleRevision(fixture.article(), 2, ArticleType.NEWS, "Old", "old", null, "Old", null, null, fixture.admin());
        previous.publish(fixture.admin(), Instant.now());
        fixture.article().publish(previous);
        stub(fixture);

        handler.onRejected(fixture.request(), fixture.admin());

        assertThat(fixture.revision().getStatus()).isEqualTo(ArticleRevisionStatus.REJECTED);
        assertThat(fixture.article().getCurrentPublishedRevision()).isSameAs(previous);
    }

    @Test
    void revalidationFailureLeavesPendingRevisionUnpublished() {
        Fixture fixture = fixture();
        stub(fixture);
        org.mockito.Mockito.doThrow(new ApiException(org.springframework.http.HttpStatus.CONFLICT, "Invalid media."))
                .when(validationService).validateStoredRevision(fixture.revision());

        assertThatThrownBy(() -> handler.onApproved(fixture.request(), fixture.admin()))
                .isInstanceOf(ApiException.class);
        assertThat(fixture.revision().getStatus()).isEqualTo(ArticleRevisionStatus.PENDING_REVIEW);
    }

    private Fixture fixture() {
        User admin = User.admin("admin@example.com", "$2a$12$hash", "Admin");
        User staff = User.staff("staff@example.com", "$2a$12$hash", "Staff", admin.getId());
        Article article = new Article(staff);
        ArticleRevision revision = new ArticleRevision(article, article.nextRevisionNumber(), ArticleType.NEWS, "Title", "title", null, "Content", null, null, staff);
        revision.submit();
        ApprovalRequest request = new ApprovalRequest(ApprovalResourceType.ARTICLE, article.getId(), 1, staff, Instant.now());
        return new Fixture(admin, article, revision, request);
    }

    private void stub(Fixture fixture) {
        when(articleRepository.findByIdForUpdate(fixture.article().getId())).thenReturn(Optional.of(fixture.article()));
        when(revisionRepository.findByArticleAndNumberForUpdate(fixture.article().getId(), 1)).thenReturn(Optional.of(fixture.revision()));
    }

    private record Fixture(User admin, Article article, ArticleRevision revision, ApprovalRequest request) { }
}
