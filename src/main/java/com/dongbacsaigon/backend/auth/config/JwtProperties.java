package com.dongbacsaigon.backend.auth.config;

import java.time.Duration;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.jwt")
public record JwtProperties(
        @NotBlank
        String secretBase64,

        @NotBlank
        String issuer,

        @Min(1)
        long accessTokenMinutes
) {
    public Duration accessTokenTtl() {
        return Duration.ofMinutes(accessTokenMinutes);
    }
}
