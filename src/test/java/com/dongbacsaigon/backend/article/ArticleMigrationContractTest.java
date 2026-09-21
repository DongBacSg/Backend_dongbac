package com.dongbacsaigon.backend.article;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

class ArticleMigrationContractTest {

    @Test
    void v6CreatesOnlyArticleCmsTablesAndAllowedTypes() throws IOException {
        String sql = new ClassPathResource("db/migration/V6__create_article_cms.sql")
                .getContentAsString(StandardCharsets.UTF_8)
                .toLowerCase();

        assertThat(sql)
                .contains("create table dongbac.articles")
                .contains("create table dongbac.article_revisions")
                .contains("create table dongbac.article_revision_media")
                .contains("'internal_activity', 'news', 'knowledge'")
                .doesNotContain("create table dongbac.article_categories")
                .doesNotContain("create table dongbac.tags")
                .doesNotContain("create table dongbac.comments")
                .doesNotContain("create table dongbac.recruitment")
                .doesNotContain("create table dongbac.leads");
    }
}
