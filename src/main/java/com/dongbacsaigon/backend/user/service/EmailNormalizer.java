package com.dongbacsaigon.backend.user.service;

import java.util.Locale;

import com.dongbacsaigon.backend.common.exception.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class EmailNormalizer {

    public String normalize(String email) {
        if (!StringUtils.hasText(email)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Email is required.");
        }
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
