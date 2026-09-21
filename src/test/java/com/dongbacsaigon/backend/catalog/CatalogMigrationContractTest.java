package com.dongbacsaigon.backend.catalog;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

class CatalogMigrationContractTest {

    @Test
    void v5CreatesOnlyCatalogTables() throws IOException {
        String sql = new ClassPathResource("db/migration/V5__create_catalog.sql")
                .getContentAsString(StandardCharsets.UTF_8)
                .toLowerCase();

        assertThat(sql)
                .contains("create table dongbac.categories")
                .contains("create table dongbac.products")
                .contains("create table dongbac.product_revisions")
                .contains("create table dongbac.product_revision_images")
                .contains("create table dongbac.product_revision_related_products")
                .doesNotContain("create table dongbac.articles")
                .doesNotContain("create table dongbac.leads")
                .doesNotContain("create table dongbac.recruitment");
    }
}
