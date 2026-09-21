package com.dongbacsaigon.backend.auth.service;

import com.dongbacsaigon.backend.common.exception.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class PasswordPolicyService {

    static final String PASSWORD_POLICY_MESSAGE =
            "Password must be 12-128 characters and include at least one letter and one digit.";

    public void validate(String password) {
        if (password == null || password.length() < 12 || password.length() > 128) {
            throw new ApiException(HttpStatus.BAD_REQUEST, PASSWORD_POLICY_MESSAGE);
        }

        boolean hasLetter = password.chars().anyMatch(Character::isLetter);
        boolean hasDigit = password.chars().anyMatch(Character::isDigit);
        if (!hasLetter || !hasDigit) {
            throw new ApiException(HttpStatus.BAD_REQUEST, PASSWORD_POLICY_MESSAGE);
        }
    }
}
