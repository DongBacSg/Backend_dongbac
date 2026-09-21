package com.dongbacsaigon.backend.article.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import com.dongbacsaigon.backend.article.dto.PublicArticlePageResponse;
import com.dongbacsaigon.backend.article.dto.PublicArticleResponse;
import com.dongbacsaigon.backend.article.dto.PublicArticleSummaryResponse;
import com.dongbacsaigon.backend.article.entity.Article;
import com.dongbacsaigon.backend.article.entity.ArticleMediaUsageType;
import com.dongbacsaigon.backend.article.entity.ArticlePublicationStatus;
import com.dongbacsaigon.backend.article.entity.ArticleRevision;
import com.dongbacsaigon.backend.article.entity.ArticleRevisionMedia;
import com.dongbacsaigon.backend.article.entity.ArticleRevisionStatus;
import com.dongbacsaigon.backend.article.entity.ArticleType;
import com.dongbacsaigon.backend.article.repository.ArticleRevisionMediaRepository;
import com.dongbacsaigon.backend.article.repository.ArticleRevisionRepository;
import com.dongbacsaigon.backend.catalog.service.SlugService;
import com.dongbacsaigon.backend.media.entity.Media;
import com.dongbacsaigon.backend.media.entity.MediaType;
import com.dongbacsaigon.backend.user.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

class PublicArticleServiceTest {

    private final ArticleRevisionRepository revisionRepository = mock(ArticleRevisionRepository.class);
    private final ArticleRevisionMediaRepository mediaRepository = mock(ArticleRevisionMediaRepository.class);
    private final PublicArticleService service = new PublicArticleService(revisionRepository, mediaRepository, new SlugService());

    @Test
    void publicListRequestsOnlyCurrentPublishedRevisionsWithTypeFilter() {
        Fixture fixture = publishedFixture("tin-tuc", ArticleType.NEWS);
        when(revisionRepository.findPublicPage(
                eq(ArticlePublicationStatus.PUBLISHED),
                eq(ArticleRevisionStatus.PUBLISHED),
                eq(ArticleType.NEWS),
                eq("lua"),
                any(Pageable.class)
        )).thenReturn(new PageImpl<>(List.of(fixture.revision())));
        when(mediaRepository.findByRevisionIdInOrderByRevisionIdAscUsageTypeAscSortOrderAsc(List.of(fixture.revision().getId())))
                .thenReturn(List.of());

        PublicArticlePageResponse response = service.list(0, 20, ArticleType.NEWS, "  LUA  ");

        assertThat(response.content()).hasSize(1);
        assertThat(response.content().get(0).slug()).isEqualTo("tin-tuc");
        verify(revisionRepository).findPublicPage(
                eq(ArticlePublicationStatus.PUBLISHED),
                eq(ArticleRevisionStatus.PUBLISHED),
                eq(ArticleType.NEWS),
                eq("lua"),
                any(Pageable.class)
        );
    }

    @Test
    void publicListDtoDoesNotExposeArticleBody() {
        assertThat(Arrays.stream(PublicArticleSummaryResponse.class.getRecordComponents())
                .map(component -> component.getName()))
                .doesNotContain("content", "seoTitle", "seoDescription", "contentMedia");
    }

    @Test
    void detailReturnsFeaturedAndOrderedContentMediaFromCurrentPublishedRevision() {
        Fixture fixture = publishedFixture("kien-thuc", ArticleType.KNOWLEDGE);
        Media featuredMedia = image(fixture.admin(), "featured");
        Media firstContentMedia = image(fixture.admin(), "content-1");
        Media secondContentMedia = image(fixture.admin(), "content-2");
        ArticleRevisionMedia featured = new ArticleRevisionMedia(
                fixture.revision(), featuredMedia, ArticleMediaUsageType.FEATURED, 0, null
        );
        ArticleRevisionMedia first = new ArticleRevisionMedia(
                fixture.revision(), firstContentMedia, ArticleMediaUsageType.CONTENT, 1, "First"
        );
        ArticleRevisionMedia second = new ArticleRevisionMedia(
                fixture.revision(), secondContentMedia, ArticleMediaUsageType.CONTENT, 2, "Second"
        );
        when(revisionRepository.findPublicBySlug(
                "kien-thuc",
                ArticlePublicationStatus.PUBLISHED,
                ArticleRevisionStatus.PUBLISHED
        )).thenReturn(Optional.of(fixture.revision()));
        when(mediaRepository.findByRevisionIdOrderByUsageTypeAscSortOrderAsc(fixture.revision().getId()))
                .thenReturn(List.of(first, second, featured));

        PublicArticleResponse response = service.getBySlug("Kiến thức");

        assertThat(response.featuredImage().media().id()).isEqualTo(featuredMedia.getId());
        assertThat(response.featuredImage().media().secureUrl()).isEqualTo(featuredMedia.getSecureUrl());
        assertThat(response.contentMedia()).extracting(item -> item.media().id())
                .containsExactly(firstContentMedia.getId(), secondContentMedia.getId());
        verify(revisionRepository).findPublicBySlug(
                "kien-thuc",
                ArticlePublicationStatus.PUBLISHED,
                ArticleRevisionStatus.PUBLISHED
        );
    }

    private Fixture publishedFixture(String slug, ArticleType type) {
        User admin = User.admin("admin@example.com", "$2a$12$hash", "Admin");
        Article article = new Article(admin);
        ArticleRevision revision = new ArticleRevision(
                article,
                article.nextRevisionNumber(),
                type,
                "Title",
                slug,
                "Summary",
                "<p>Content</p>",
                "SEO title",
                "SEO description",
                admin
        );
        revision.publish(admin, Instant.now());
        article.publish(revision);
        return new Fixture(admin, revision);
    }

    private Media image(User user, String name) {
        return new Media(
                "asset-" + name,
                "dongbac/media/" + name,
                MediaType.IMAGE,
                "webp",
                "https://example.com/" + name + ".webp",
                800,
                600,
                1000,
                BigDecimal.ZERO,
                "dongbac/media",
                name + ".webp",
                user
        );
    }

    private record Fixture(User admin, ArticleRevision revision) {
    }
}
