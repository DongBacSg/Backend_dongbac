package com.dongbacsaigon.backend.common.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class FrontendCompatibilityMigrationContractTest {

    @Test
    void v8OnlyExtendsLeadArticleAndManufacturingSchemas() throws Exception {
        String sql = Files.readString(Path.of(
                "src/main/resources/db/migration/V8__frontend_compatibility_features.sql"
        ));

        assertThat(sql).contains(
                "ALTER TABLE dongbac.customer_leads",
                "ALTER COLUMN created_by DROP NOT NULL",
                "ADD COLUMN subject VARCHAR(240)",
                "ALTER TABLE dongbac.article_revisions",
                "DROP CONSTRAINT ck_article_revisions_type",
                "'RECRUITMENT'", "'ANNOUNCEMENT'",
                "CREATE TABLE dongbac.manufacturing_services",
                "REFERENCES dongbac.media (id)"
        );
        assertThat(sql).doesNotContain("DROP TABLE", "TRUNCATE", "CREATE TABLE dongbac.recruitment");
    }
}
