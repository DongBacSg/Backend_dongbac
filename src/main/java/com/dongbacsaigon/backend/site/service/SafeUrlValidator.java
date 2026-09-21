package com.dongbacsaigon.backend.site.service;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;

import com.dongbacsaigon.backend.common.exception.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
class SafeUrlValidator {

    String normalizeOptionalUrl(String url) {
        if (!StringUtils.hasText(url)) {
            return null;
        }

        String trimmedUrl = url.trim();
        try {
            URI uri = new URI(trimmedUrl);
            String scheme = uri.getScheme();
            if (scheme == null) {
                throw invalidUrl();
            }
            String normalizedScheme = scheme.toLowerCase(Locale.ROOT);
            if (!"http".equals(normalizedScheme) && !"https".equals(normalizedScheme)) {
                throw invalidUrl();
            }
            return trimmedUrl;
        } catch (URISyntaxException exception) {
            throw invalidUrl();
        }
    }

    private ApiException invalidUrl() {
        return new ApiException(HttpStatus.BAD_REQUEST, "URL must use http or https.");
    }
}
