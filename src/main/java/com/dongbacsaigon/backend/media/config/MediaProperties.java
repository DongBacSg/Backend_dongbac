package com.dongbacsaigon.backend.media.config;

import java.time.Duration;

import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.media")
public record MediaProperties(
        @Min(1)
        long imageMaxBytes,

        @Min(1)
        long videoMaxBytes,

        @Min(1)
        long uploadIntentMinutes
) {
    public Duration uploadIntentTtl() {
        return Duration.ofMinutes(uploadIntentMinutes);
    }
}
