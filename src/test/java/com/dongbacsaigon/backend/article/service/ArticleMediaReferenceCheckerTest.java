package com.dongbacsaigon.backend.article.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.UUID;

import com.dongbacsaigon.backend.article.repository.ArticleRevisionMediaRepository;
import org.junit.jupiter.api.Test;

class ArticleMediaReferenceCheckerTest {

    @Test
    void anyArticleRevisionMediaCountsAsUsage() {
        ArticleRevisionMediaRepository repository = mock(ArticleRevisionMediaRepository.class);
        UUID mediaId = UUID.randomUUID();
        when(repository.existsByMediaId(mediaId)).thenReturn(true);

        assertThat(new ArticleMediaReferenceChecker(repository).isReferenced(mediaId)).isTrue();
    }
}
