package com.dongbacsaigon.backend.common.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

class ProductionConfigurationContractTest {

    @Test
    void productionProfileKeepsOperationsSurfaceRestricted() throws IOException {
        String production = resource("application-prod.yml");

        assertThat(production)
                .contains("forward-headers-strategy: framework")
                .contains("shutdown: graceful")
                .contains("timeout-per-shutdown-phase:")
                .contains("include: health")
                .contains("enabled: ${SWAGGER_ENABLED:false}")
                .contains("refresh-cookie-secure: ${AUTH_REFRESH_COOKIE_SECURE:true}")
                .contains("refresh-cookie-same-site: ${AUTH_REFRESH_COOKIE_SAME_SITE:None}")
                .doesNotContain("include: *", "show-details: always", "org.springframework.security: DEBUG");
    }

    @Test
    void commonConfigurationKeepsFlywayAsSchemaOwner() throws IOException {
        String common = resource("application.yml");

        assertThat(common)
                .contains("ddl-auto: validate")
                .contains("flyway:")
                .contains("enabled: true")
                .doesNotContain("ddl-auto: update", "ddl-auto: create", "clean-on-validation-error: true");
    }

    @Test
    void migrationHistoryRemainsV1ThroughV7WithoutEmptyV8() throws IOException {
        Path migrations = Path.of("src", "main", "resources", "db", "migration");
        List<String> names;
        try (var stream = Files.list(migrations)) {
            names = stream.map(path -> path.getFileName().toString()).sorted().toList();
        }

        assertThat(names).containsExactly(
                "V1__initialize_dongbac_schema.sql",
                "V2__create_users_and_auth.sql",
                "V3__create_approval_and_audit.sql",
                "V4__create_media_and_site_management.sql",
                "V5__create_catalog.sql",
                "V6__create_article_cms.sql",
                "V7__create_customer_leads.sql"
        );
    }

    @Test
    void containerAndGitExcludeLocalSecretsAndUseNonRootRuntime() throws IOException {
        String dockerfile = Files.readString(Path.of("Dockerfile"));
        String dockerignore = Files.readString(Path.of(".dockerignore"));
        String gitignore = Files.readString(Path.of(".gitignore"));

        assertThat(dockerfile)
                .contains("eclipse-temurin:17-jre")
                .contains("USER spring:spring")
                .doesNotContain("COPY .env", "COPY local-env.ps1");
        assertThat(dockerignore).contains(".env", "local-env.ps1", ".git", "target");
        assertThat(gitignore).contains(".env", "local-env.ps1");
    }

    @Test
    void securityFallbackDeniesUnknownRoutesAndProtectsAdminNamespace() throws IOException {
        String security = Files.readString(Path.of(
                "src", "main", "java", "com", "dongbacsaigon", "backend",
                "auth", "config", "SecurityConfig.java"
        ));

        assertThat(security)
                .contains(".requestMatchers(\"/api/public/**\").permitAll()")
                .contains(".requestMatchers(\"/api/admin/**\").authenticated()")
                .contains(".anyRequest().denyAll()")
                .contains("HttpStatus.UNAUTHORIZED")
                .contains("HttpStatus.FORBIDDEN");
    }

    private String resource(String name) throws IOException {
        return new ClassPathResource(name).getContentAsString(StandardCharsets.UTF_8);
    }
}
