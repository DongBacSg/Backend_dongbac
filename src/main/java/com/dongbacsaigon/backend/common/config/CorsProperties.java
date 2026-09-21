package com.dongbacsaigon.backend.common.config;

import java.net.URI;
import java.util.List;
import java.util.Locale;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

@ConfigurationProperties(prefix = "app.cors")
public record CorsProperties(List<String> allowedOrigins) {

    private static final List<String> DEFAULT_ALLOWED_ORIGINS = List.of(
            "http://localhost:5173",
            "http://localhost:5174",
            "https://dong-bac-group.vercel.app",
            "https://admin-dongbac-lhzv.vercel.app"
    );

    public CorsProperties {
        if (allowedOrigins == null || allowedOrigins.isEmpty()) {
            allowedOrigins = DEFAULT_ALLOWED_ORIGINS;
        }
        allowedOrigins = allowedOrigins.stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .distinct()
                .toList();
        if (allowedOrigins.isEmpty()) {
            throw new IllegalArgumentException("CORS_ALLOWED_ORIGINS must contain at least one trusted origin.");
        }
        allowedOrigins.forEach(CorsProperties::validateOrigin);
    }

    private static void validateOrigin(String origin) {
        if ("*".equals(origin)) {
            throw new IllegalArgumentException("CORS_ALLOWED_ORIGINS cannot contain * when credentials are enabled.");
        }
        try {
            URI uri = URI.create(origin);
            String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase(Locale.ROOT);
            boolean valid = ("http".equals(scheme) || "https".equals(scheme))
                    && StringUtils.hasText(uri.getHost())
                    && uri.getUserInfo() == null
                    && !StringUtils.hasText(uri.getPath())
                    && uri.getQuery() == null
                    && uri.getFragment() == null;
            if (!valid) throw new IllegalArgumentException("CORS_ALLOWED_ORIGINS contains an invalid origin: " + origin);
        } catch (IllegalArgumentException exception) {
            if (exception.getMessage() != null && exception.getMessage().startsWith("CORS_ALLOWED_ORIGINS")) throw exception;
            throw new IllegalArgumentException("CORS_ALLOWED_ORIGINS contains an invalid origin: " + origin, exception);
        }
    }
}
