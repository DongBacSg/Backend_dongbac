package com.dongbacsaigon.backend.auth.config;

import java.time.Duration;
import java.util.Optional;
import java.util.Set;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;
import org.springframework.util.StringUtils;

@Validated
@ConfigurationProperties(prefix = "app.auth")
public record AuthProperties(
        @Min(1)
        int refreshTokenDays,

        @NotBlank
        String refreshCookieName,

        boolean refreshCookieSecure,

        @NotBlank
        String refreshCookieSameSite,

        @Min(1)
        int maxFailedAttempts,

        @Min(1)
        int lockMinutes,

        String bootstrapAdminEmail,
        String bootstrapAdminPassword,
        String bootstrapAdminName
) {
    private static final Set<String> SUPPORTED_SAME_SITE_VALUES = Set.of("Strict", "Lax", "None");

    public AuthProperties {
        refreshCookieName = refreshCookieName == null ? "" : refreshCookieName.trim();
        refreshCookieSameSite = normalizeSameSite(refreshCookieSameSite);
    }

    public Duration refreshTokenTtl() {
        return Duration.ofDays(refreshTokenDays);
    }

    public Duration lockDuration() {
        return Duration.ofMinutes(lockMinutes);
    }

    public Optional<BootstrapAdmin> bootstrapAdmin() {
        if (!StringUtils.hasText(bootstrapAdminEmail)
                || !StringUtils.hasText(bootstrapAdminPassword)
                || !StringUtils.hasText(bootstrapAdminName)) {
            return Optional.empty();
        }

        return Optional.of(new BootstrapAdmin(
                bootstrapAdminEmail.trim(),
                bootstrapAdminPassword,
                bootstrapAdminName.trim()
        ));
    }

    public boolean hasAnyBootstrapAdminValue() {
        return StringUtils.hasText(bootstrapAdminEmail)
                || StringUtils.hasText(bootstrapAdminPassword)
                || StringUtils.hasText(bootstrapAdminName);
    }

    private static String normalizeSameSite(String value) {
        if (!StringUtils.hasText(value)) {
            return "Lax";
        }

        String trimmedValue = value.trim();
        return SUPPORTED_SAME_SITE_VALUES.stream()
                .filter(supportedValue -> supportedValue.equalsIgnoreCase(trimmedValue))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "AUTH_REFRESH_COOKIE_SAME_SITE must be Strict, Lax, or None."
                ));
    }

    public record BootstrapAdmin(String email, String password, String name) {
    }
}
