package com.dongbacsaigon.backend.article.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.dongbacsaigon.backend.approval.entity.ApprovalResourceType;
import com.dongbacsaigon.backend.approval.repository.ApprovalRequestRepository;
import com.dongbacsaigon.backend.approval.service.ApprovalService;
import com.dongbacsaigon.backend.article.dto.ArticleDraftRequest;
import com.dongbacsaigon.backend.article.dto.ArticleRevisionResponse;
import com.dongbacsaigon.backend.article.entity.Article;
import com.dongbacsaigon.backend.article.entity.ArticlePublicationStatus;
import com.dongbacsaigon.backend.article.entity.ArticleRevision;
import com.dongbacsaigon.backend.article.entity.ArticleRevisionMedia;
import com.dongbacsaigon.backend.article.entity.ArticleRevisionStatus;
import com.dongbacsaigon.backend.article.entity.ArticleType;
import com.dongbacsaigon.backend.article.repository.ArticleRepository;
import com.dongbacsaigon.backend.article.repository.ArticleRevisionMediaRepository;
import com.dongbacsaigon.backend.article.repository.ArticleRevisionRepository;
import com.dongbacsaigon.backend.audit.service.AuditService;
import com.dongbacsaigon.backend.common.exception.ApiException;
import com.dongbacsaigon.backend.media.entity.Media;
import com.dongbacsaigon.backend.media.entity.MediaType;
import com.dongbacsaigon.backend.user.entity.User;
import com.dongbacsaigon.backend.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class ArticleServiceTest {

    private final ArticleRepository articleRepository = mock(ArticleRepository.class);
    private final ArticleRevisionRepository revisionRepository = mock(ArticleRevisionRepository.class);
    private final ArticleRevisionMediaRepository mediaRepository = mock(ArticleRevisionMediaRepository.class);
    private final ApprovalRequestRepository approvalRequestRepository = mock(ApprovalRequestRepository.class);
    private final UserRepository userRepository = mock(UserRepository.class);
    private final ArticleValidationService validationService = mock(ArticleValidationService.class);
    private final ApprovalService approvalService = mock(ApprovalService.class);
    private final AuditService auditService = mock(AuditService.class);
    private final ArticleService service = new ArticleService(
            articleRepository, revisionRepository, mediaRepository, approvalRequestRepository,
            userRepository, validationService, approvalService, auditService
    );

    @Test
    void recruitmentAndAnnouncementFollowSameDraftAndApprovalWorkflow() {
        User staff = staff();
        when(userRepository.findById(staff.getId())).thenReturn(Optional.of(staff));
        when(articleRepository.save(any(Article.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(revisionRepository.save(any(ArticleRevision.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(mediaRepository.findByRevisionIdOrderByUsageTypeAscSortOrderAsc(any())).thenReturn(List.of());

        for (ArticleType type : List.of(ArticleType.RECRUITMENT, ArticleType.ANNOUNCEMENT)) {
            ArticleDraftRequest request = new ArticleDraftRequest(
                    type, "Title", null, "Summary", "<p>Content</p>", null, null, List.of()
            );
            when(validationService.resolveDraft(any(), any())).thenReturn(new ArticleValidationService.ResolvedDraft(
                    type, "Title", "title", "Summary", "<p>Content</p>", null, null, List.of()
            ));
            ArticleRevisionResponse created = service.create(request, staff.getId());
            assertThat(created.status()).isEqualTo(ArticleRevisionStatus.DRAFT);
            assertThat(created.articleType()).isEqualTo(type);
        }

        ArgumentCaptor<ArticleRevision> captor = ArgumentCaptor.forClass(ArticleRevision.class);
        verify(revisionRepository, times(2)).save(captor.capture());
        for (ArticleRevision revision : captor.getAllValues()) {
            Article article = revision.getArticle();
            when(articleRepository.findByIdForUpdate(article.getId())).thenReturn(Optional.of(article));
            when(revisionRepository.findByIdAndArticleId(revision.getId(), article.getId()))
                    .thenReturn(Optional.of(revision));
            ArticleRevisionResponse submitted = service.submit(article.getId(), revision.getId(), staff.getId());
            assertThat(submitted.status()).isEqualTo(ArticleRevisionStatus.PENDING_REVIEW);
            verify(approvalService).submitForReview(ApprovalResourceType.ARTICLE, article.getId(), 1, staff.getId());
        }
    }

    @Test
    void createBuildsDraftIdentityAndServerNumberedRevisionOne() {
        User staff = staff();
        when(userRepository.findById(staff.getId())).thenReturn(Optional.of(staff));
        when(articleRepository.save(any(Article.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(validationService.resolveDraft(any(), any())).thenReturn(resolved());
        when(revisionRepository.save(any(ArticleRevision.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(mediaRepository.findByRevisionIdOrderByUsageTypeAscSortOrderAsc(any())).thenReturn(List.of());

        ArticleRevisionResponse response = service.create(request(), staff.getId());

        assertThat(response.revisionNumber()).isEqualTo(1);
        assertThat(response.status()).isEqualTo(ArticleRevisionStatus.DRAFT);
        assertThat(response.articleType()).isEqualTo(ArticleType.NEWS);
    }

    @Test
    void submitUsesStableArticleIdAndRevisionNumber() {
        Fixture fixture = draftFixture();
        stubDraft(fixture);
        when(userRepository.findById(fixture.user().getId())).thenReturn(Optional.of(fixture.user()));
        when(mediaRepository.findByRevisionIdOrderByUsageTypeAscSortOrderAsc(any())).thenReturn(List.of());

        ArticleRevisionResponse response = service.submit(fixture.article().getId(), fixture.revision().getId(), fixture.user().getId());

        assertThat(response.status()).isEqualTo(ArticleRevisionStatus.PENDING_REVIEW);
        verify(approvalService).submitForReview(ApprovalResourceType.ARTICLE, fixture.article().getId(), 1, fixture.user().getId());
    }

    @Test
    void failedApprovalSubmissionLeavesDraftUnchanged() {
        Fixture fixture = draftFixture();
        stubDraft(fixture);
        org.mockito.Mockito.doThrow(new IllegalStateException("approval failed")).when(approvalService)
                .submitForReview(ApprovalResourceType.ARTICLE, fixture.article().getId(), 1, fixture.user().getId());

        assertThatThrownBy(() -> service.submit(fixture.article().getId(), fixture.revision().getId(), fixture.user().getId()))
                .isInstanceOf(IllegalStateException.class);
        assertThat(fixture.revision().getStatus()).isEqualTo(ArticleRevisionStatus.DRAFT);
    }

    @Test
    void pendingRevisionIsImmutable() {
        Fixture fixture = draftFixture();
        fixture.revision().submit();
        stubDraft(fixture);

        assertThatThrownBy(() -> service.updateDraft(fixture.article().getId(), fixture.revision().getId(), request(), fixture.user().getId()))
                .isInstanceOf(ApiException.class)
                .hasMessage("Only a draft article revision can be modified or submitted.");
    }

    @Test
    void everySubmittedOrHistoricalRevisionIsImmutable() {
        Fixture rejected = draftFixture();
        rejected.revision().submit();
        rejected.revision().reject();
        assertUpdateRejected(rejected);

        Fixture published = draftFixture();
        published.revision().publish(admin(), java.time.Instant.now());
        assertUpdateRejected(published);

        Fixture archived = draftFixture();
        archived.revision().publish(admin(), java.time.Instant.now());
        archived.revision().archive();
        assertUpdateRejected(archived);
    }

    @Test
    void nextRevisionClonesLatestRejectedContentAndMediaIntoNewDraft() {
        Fixture fixture = draftFixture();
        fixture.revision().submit();
        fixture.revision().reject();
        Media sourceMedia = image(fixture.user());
        ArticleRevisionMedia sourceRelation = new ArticleRevisionMedia(
                fixture.revision(), sourceMedia, com.dongbacsaigon.backend.article.entity.ArticleMediaUsageType.CONTENT, 4, "Caption"
        );
        when(articleRepository.findByIdForUpdate(fixture.article().getId())).thenReturn(Optional.of(fixture.article()));
        when(revisionRepository.existsByArticleIdAndStatusIn(fixture.article().getId(), List.of(ArticleRevisionStatus.DRAFT, ArticleRevisionStatus.PENDING_REVIEW)))
                .thenReturn(false);
        when(revisionRepository.findFirstByArticleIdOrderByRevisionNumberDesc(fixture.article().getId())).thenReturn(Optional.of(fixture.revision()));
        when(userRepository.findById(fixture.user().getId())).thenReturn(Optional.of(fixture.user()));
        when(revisionRepository.save(any(ArticleRevision.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(mediaRepository.findByRevisionIdOrderByUsageTypeAscSortOrderAsc(any())).thenAnswer(invocation ->
                fixture.revision().getId().equals(invocation.getArgument(0)) ? List.of(sourceRelation) : List.of()
        );

        ArticleRevisionResponse response = service.createRevision(fixture.article().getId(), fixture.user().getId());

        assertThat(response.revisionNumber()).isEqualTo(2);
        assertThat(response.status()).isEqualTo(ArticleRevisionStatus.DRAFT);
        assertThat(response.content()).isEqualTo(fixture.revision().getContent());
        ArgumentCaptor<Iterable<ArticleRevisionMedia>> mediaCaptor = iterableCaptor();
        verify(mediaRepository).saveAll(mediaCaptor.capture());
        ArticleRevisionMedia clone = mediaCaptor.getValue().iterator().next();
        assertThat(clone.getRevision().getId()).isEqualTo(response.id());
        assertThat(clone.getRevision()).isNotSameAs(fixture.revision());
        assertThat(clone.getMedia()).isSameAs(sourceMedia);
        assertThat(clone.getSortOrder()).isEqualTo(4);
        assertThat(clone.getCaption()).isEqualTo("Caption");
    }

    @Test
    void nextRevisionIsRejectedWhenWorkingRevisionAlreadyExists() {
        Fixture fixture = draftFixture();
        when(articleRepository.findByIdForUpdate(fixture.article().getId())).thenReturn(Optional.of(fixture.article()));
        when(revisionRepository.existsByArticleIdAndStatusIn(fixture.article().getId(), List.of(ArticleRevisionStatus.DRAFT, ArticleRevisionStatus.PENDING_REVIEW)))
                .thenReturn(true);

        assertThatThrownBy(() -> service.createRevision(fixture.article().getId(), fixture.user().getId()))
                .isInstanceOf(ApiException.class)
                .hasMessage("Article already has an active draft or pending revision.");
        verify(revisionRepository, never()).save(any(ArticleRevision.class));
    }

    @Test
    void adminLifecycleKeepsApprovedRevisionUnchanged() {
        User admin = admin();
        Article article = new Article(admin);
        ArticleRevision revision = revision(article, admin);
        revision.publish(admin, java.time.Instant.now());
        article.publish(revision);
        when(articleRepository.findByIdForUpdate(article.getId())).thenReturn(Optional.of(article));
        when(articleRepository.findDetailedById(article.getId())).thenReturn(Optional.of(article));
        when(revisionRepository.findByArticleIdOrderByRevisionNumberDesc(article.getId())).thenReturn(List.of(revision));
        when(mediaRepository.findByRevisionIdOrderByUsageTypeAscSortOrderAsc(revision.getId())).thenReturn(List.of());
        when(userRepository.findById(admin.getId())).thenReturn(Optional.of(admin));

        service.unpublish(article.getId(), admin.getId());
        assertThat(article.getPublicationStatus()).isEqualTo(ArticlePublicationStatus.UNPUBLISHED);
        service.republish(article.getId(), admin.getId());
        assertThat(article.getPublicationStatus()).isEqualTo(ArticlePublicationStatus.PUBLISHED);
        service.archive(article.getId(), admin.getId());
        assertThat(article.getPublicationStatus()).isEqualTo(ArticlePublicationStatus.ARCHIVED);
        assertThat(revision.getStatus()).isEqualTo(ArticleRevisionStatus.PUBLISHED);
    }

    private void stubDraft(Fixture fixture) {
        when(articleRepository.findByIdForUpdate(fixture.article().getId())).thenReturn(Optional.of(fixture.article()));
        when(revisionRepository.findByIdAndArticleId(fixture.revision().getId(), fixture.article().getId())).thenReturn(Optional.of(fixture.revision()));
    }

    private Fixture draftFixture() {
        User user = staff();
        Article article = new Article(user);
        return new Fixture(user, article, revision(article, user));
    }

    private ArticleRevision revision(Article article, User user) {
        return new ArticleRevision(article, article.nextRevisionNumber(), ArticleType.NEWS, "Title", "title", "Summary", "<p>Content</p>", null, null, user);
    }

    private ArticleValidationService.ResolvedDraft resolved() {
        return new ArticleValidationService.ResolvedDraft(ArticleType.NEWS, "Title", "title", "Summary", "<p>Content</p>", null, null, List.of());
    }

    private ArticleDraftRequest request() { return new ArticleDraftRequest(ArticleType.NEWS, "Title", null, "Summary", "Content", null, null, List.of()); }
    private void assertUpdateRejected(Fixture fixture) {
        stubDraft(fixture);
        assertThatThrownBy(() -> service.updateDraft(fixture.article().getId(), fixture.revision().getId(), request(), fixture.user().getId()))
                .isInstanceOf(ApiException.class)
                .hasMessage("Only a draft article revision can be modified or submitted.");
    }
    private Media image(User user) {
        return new Media("asset", "article/image", MediaType.IMAGE, "webp", "https://example.com/image.webp", 800, 600, 1000, BigDecimal.ZERO, "article", "image.webp", user);
    }
    @SuppressWarnings({"unchecked", "rawtypes"})
    private ArgumentCaptor<Iterable<ArticleRevisionMedia>> iterableCaptor() {
        return (ArgumentCaptor) ArgumentCaptor.forClass(Iterable.class);
    }
    private User admin() { return User.admin("admin@example.com", "$2a$12$hash", "Admin"); }
    private User staff() { return User.staff("staff@example.com", "$2a$12$hash", "Staff", UUID.randomUUID()); }
    private record Fixture(User user, Article article, ArticleRevision revision) { }
}
