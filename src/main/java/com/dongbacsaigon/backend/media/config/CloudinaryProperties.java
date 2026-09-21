package com.dongbacsaigon.backend.media.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.cloudinary")
public record CloudinaryProperties(
        @NotBlank
        String cloudName,

        @NotBlank
        String apiKey,

        @NotBlank
        String apiSecret,

        @NotBlank
        String rootFolder,

        @Min(1)
        @Max(120)
        int httpTimeoutSeconds
) {
    public CloudinaryProperties {
        cloudName = trim(cloudName);
        apiKey = trim(apiKey);
        apiSecret = trim(apiSecret);
        rootFolder = normalizeRootFolder(rootFolder);
    }

    private static String normalizeRootFolder(String value) {
        String trimmedValue = trim(value);
        if (trimmedValue == null) {
            return null;
        }
        return trimmedValue.replace("\\", "/").replaceAll("^/+", "").replaceAll("/+$", "");
    }

    private static String trim(String value) {
        return value == null ? null : value.trim();
    }
}
