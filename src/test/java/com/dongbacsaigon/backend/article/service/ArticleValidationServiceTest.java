package com.dongbacsaigon.backend.article.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import com.dongbacsaigon.backend.article.dto.ArticleDraftRequest;
import com.dongbacsaigon.backend.article.dto.ArticleMediaRequest;
import com.dongbacsaigon.backend.article.entity.ArticleMediaUsageType;
import com.dongbacsaigon.backend.article.entity.Article;
import com.dongbacsaigon.backend.article.entity.ArticleRevision;
import com.dongbacsaigon.backend.article.entity.ArticleType;
import com.dongbacsaigon.backend.article.repository.ArticleRevisionMediaRepository;
import com.dongbacsaigon.backend.article.repository.ArticleRevisionRepository;
import com.dongbacsaigon.backend.catalog.service.SlugService;
import com.dongbacsaigon.backend.common.exception.ApiException;
import com.dongbacsaigon.backend.media.entity.Media;
import com.dongbacsaigon.backend.media.entity.MediaType;
import com.dongbacsaigon.backend.media.service.MediaService;
import com.dongbacsaigon.backend.user.entity.User;
import org.junit.jupiter.api.Test;

class ArticleValidationServiceTest {

    private final ArticleRevisionRepository revisionRepository = mock(ArticleRevisionRepository.class);
    private final ArticleRevisionMediaRepository mediaRepository = mock(ArticleRevisionMediaRepository.class);
    private final MediaService mediaService = mock(MediaService.class);
    private final ArticleValidationService service = new ArticleValidationService(
            revisionRepository, mediaRepository, mediaService, new SlugService(), new ArticleContentSanitizer()
    );

    @Test
    void supportsExactlyThreeArticleTypes() {
        assertThat(ArticleType.values()).containsExactly(
                ArticleType.INTERNAL_ACTIVITY,
                ArticleType.NEWS,
                ArticleType.KNOWLEDGE
        );
    }

    @Test
    void normalizesVietnameseSlugAndSanitizesContent() {
        ArticleValidationService.ResolvedDraft result = service.resolveDraft(
                request("Tin tức nông nghiệp", List.of()),
                UUID.randomUUID()
        );

        assertThat(result.slug()).isEqualTo("tin-tuc-nong-nghiep");
        assertThat(result.content()).isEqualTo("<p>Safe</p>");
    }

    @Test
    void rejectsMultipleFeaturedImages() {
        List<ArticleMediaRequest> media = List.of(
                new ArticleMediaRequest(UUID.randomUUID(), ArticleMediaUsageType.FEATURED, 0, null),
                new ArticleMediaRequest(UUID.randomUUID(), ArticleMediaUsageType.FEATURED, 1, null)
        );

        assertThatThrownBy(() -> service.resolveDraft(request("Title", media), UUID.randomUUID()))
                .isInstanceOf(ApiException.class)
                .hasMessage("An article revision can have at most one featured image.");
    }

    @Test
    void activeImagesAreAcceptedAndContentOrderIsPreserved() {
        User user = admin();
        Media first = image(user, "first");
        Media second = image(user, "second");
        when(mediaService.requireActiveImage(first.getId())).thenReturn(first);
        when(mediaService.requireActiveImage(second.getId())).thenReturn(second);
        List<ArticleMediaRequest> media = List.of(
                new ArticleMediaRequest(first.getId(), ArticleMediaUsageType.CONTENT, 1, "First"),
                new ArticleMediaRequest(second.getId(), ArticleMediaUsageType.CONTENT, 3, "Second")
        );

        ArticleValidationService.ResolvedDraft result = service.resolveDraft(request("Title", media), UUID.randomUUID());

        assertThat(result.media()).extracting(ArticleValidationService.ResolvedMedia::sortOrder).containsExactly(1, 3);
    }

    @Test
    void rejectsBlankTitleAndContent() {
        UUID articleId = UUID.randomUUID();
        assertThatThrownBy(() -> service.resolveDraft(
                new ArticleDraftRequest(ArticleType.NEWS, " ", null, null, "Content", null, null, List.of()),
                articleId
        )).isInstanceOf(ApiException.class).hasMessage("Article title is required.");

        assertThatThrownBy(() -> service.resolveDraft(
                new ArticleDraftRequest(ArticleType.NEWS, "Title", null, null, " ", null, null, List.of()),
                articleId
        )).isInstanceOf(ApiException.class).hasMessage("Article content is required.");
    }

    @Test
    void rejectsSlugUsedByAnotherPublishedArticle() {
        UUID articleId = UUID.randomUUID();
        when(revisionRepository.existsByStatusAndSlugAndArticleIdNot(
                com.dongbacsaigon.backend.article.entity.ArticleRevisionStatus.PUBLISHED,
                "tin-tuc",
                articleId
        )).thenReturn(true);

        assertThatThrownBy(() -> service.resolveDraft(request("Tin tức", List.of()), articleId))
                .isInstanceOf(ApiException.class)
                .hasMessage("Article slug is already used by another published article.");
    }

    @Test
    void approvalRevalidationRejectsMediaThatIsNoLongerActiveImage() {
        User user = admin();
        Article article = new Article(user);
        ArticleRevision revision = new ArticleRevision(
                article, article.nextRevisionNumber(), ArticleType.NEWS, "Title", "title", null, "<p>Content</p>", null, null, user
        );
        Media media = image(user, "approval");
        when(mediaRepository.findByRevisionIdOrderByUsageTypeAscSortOrderAsc(revision.getId())).thenReturn(List.of(
                new com.dongbacsaigon.backend.article.entity.ArticleRevisionMedia(
                        revision, media, ArticleMediaUsageType.FEATURED, 0, null
                )
        ));
        when(mediaService.requireActiveImage(media.getId())).thenThrow(new ApiException(org.springframework.http.HttpStatus.CONFLICT, "Media is not active."));

        assertThatThrownBy(() -> service.validateStoredRevision(revision))
                .isInstanceOf(ApiException.class)
                .hasMessage("Media is not active.");
    }

    private ArticleDraftRequest request(String title, List<ArticleMediaRequest> media) {
        return new ArticleDraftRequest(ArticleType.NEWS, title, null, "Summary", "<p>Safe</p><script>x()</script>", null, null, media);
    }

    private User admin() { return User.admin("admin@example.com", "$2a$12$hash", "Admin"); }

    private Media image(User user, String value) {
        return new Media("asset-" + value, "article/" + value, MediaType.IMAGE, "webp", "https://example.com/" + value + ".webp", 800, 600, 1000, BigDecimal.ZERO, "article", value + ".webp", user);
    }
}
