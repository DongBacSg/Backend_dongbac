package com.dongbacsaigon.backend.auth.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.dongbacsaigon.backend.common.exception.ApiException;
import org.junit.jupiter.api.Test;

class PasswordPolicyServiceTest {

    private final PasswordPolicyService passwordPolicyService = new PasswordPolicyService();

    @Test
    void validateAcceptsPassphraseWithLetterAndDigit() {
        assertThatCode(() -> passwordPolicyService.validate("correct horse 7 battery"))
                .doesNotThrowAnyException();
    }

    @Test
    void validateRejectsTooShortPassword() {
        assertThatThrownBy(() -> passwordPolicyService.validate("short7"))
                .isInstanceOf(ApiException.class)
                .hasMessage("Password must be 12-128 characters and include at least one letter and one digit.");
    }

    @Test
    void validateRejectsPasswordWithoutDigit() {
        assertThatThrownBy(() -> passwordPolicyService.validate("longpasswordonly"))
                .isInstanceOf(ApiException.class);
    }
}
