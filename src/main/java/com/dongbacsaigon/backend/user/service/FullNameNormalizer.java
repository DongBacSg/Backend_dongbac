package com.dongbacsaigon.backend.user.service;

import com.dongbacsaigon.backend.common.exception.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class FullNameNormalizer {

    public String normalize(String fullName) {
        if (!StringUtils.hasText(fullName)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Full name is required.");
        }
        return fullName.trim();
    }
}
