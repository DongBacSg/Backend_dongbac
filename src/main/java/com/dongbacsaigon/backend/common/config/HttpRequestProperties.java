package com.dongbacsaigon.backend.common.config;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.http")
public record HttpRequestProperties(
        @Min(1024)
        @Max(10_485_760)
        int maxJsonBodyBytes
) {
}
