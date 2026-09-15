package com.dongbacsaigon.backend.common.config;

import java.util.List;
import java.util.Locale;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.util.StringUtils;

public class RequiredDatabaseEnvironmentPostProcessor implements EnvironmentPostProcessor {

    private static final List<String> REQUIRED_DATABASE_VARIABLES = List.of(
            "DB_URL",
            "DB_USERNAME",
            "DB_PASSWORD",
            "DB_SCHEMA"
    );

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        List<String> missingVariables = REQUIRED_DATABASE_VARIABLES.stream()
                .filter(variable -> !StringUtils.hasText(environment.getProperty(variable)))
                .toList();

        if (!missingVariables.isEmpty()) {
            throw new IllegalStateException(
                    "Missing required database environment variables: "
                            + String.join(", ", missingVariables)
                            + ". Configure Supabase PostgreSQL JDBC settings; this application does not fall back to H2."
            );
        }

        String dbUrl = environment.getRequiredProperty("DB_URL");
        if (!dbUrl.startsWith("jdbc:postgresql://")) {
            throw new IllegalStateException("DB_URL must be a PostgreSQL JDBC URL starting with jdbc:postgresql://.");
        }

        if (!dbUrl.toLowerCase(Locale.ROOT).contains("sslmode=require")) {
            throw new IllegalStateException("DB_URL must include sslmode=require for encrypted Supabase PostgreSQL access.");
        }
    }
}
