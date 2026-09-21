package com.dongbacsaigon.backend.catalog.service;

import java.text.Normalizer;
import java.util.Locale;

import com.dongbacsaigon.backend.common.exception.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class SlugService {

    public String normalize(String value) {
        if (!StringUtils.hasText(value)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Slug source must not be blank.");
        }

        String normalized = Normalizer.normalize(value.trim(), Normalizer.Form.NFD)
                .replace('đ', 'd')
                .replace('Đ', 'D')
                .replaceAll("\\p{M}+", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-+|-+$", "");

        if (!StringUtils.hasText(normalized)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Slug must contain at least one letter or number.");
        }
        return normalized;
    }
}
