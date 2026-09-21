package com.dongbacsaigon.backend.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.time.Instant;

import com.dongbacsaigon.backend.auth.config.AuthProperties;
import com.dongbacsaigon.backend.auth.dto.LoginRequest;
import com.dongbacsaigon.backend.audit.entity.AuditAction;
import com.dongbacsaigon.backend.audit.service.AuditActor;
import com.dongbacsaigon.backend.audit.service.AuditService;
import com.dongbacsaigon.backend.common.exception.ApiException;
import com.dongbacsaigon.backend.user.entity.User;
import com.dongbacsaigon.backend.user.repository.UserRepository;
import com.dongbacsaigon.backend.user.service.EmailNormalizer;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

class AuthServiceTest {

    private final UserRepository userRepository = mock(UserRepository.class);
    private final PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
    private final AuditService auditService = mock(AuditService.class);
    private final JwtTokenService jwtTokenService = mock(JwtTokenService.class);
    private final RefreshTokenService refreshTokenService = mock(RefreshTokenService.class);
    private final AuthService authService = new AuthService(
            userRepository,
            new EmailNormalizer(),
            passwordEncoder,
            new PasswordPolicyService(),
            jwtTokenService,
            refreshTokenService,
            new AuthProperties(7, "dbsg_refresh", false, "Lax", 5, 15, "", "", ""),
            auditService
    );

    @Test
    void validLoginReturnsAccessAndOpaqueRefreshTokens() {
        User user = User.admin("admin@example.com", "$2a$12$hash", "Admin");
        RefreshTokenService.IssuedRefreshToken issuedRefreshToken =
                new RefreshTokenService.IssuedRefreshToken("opaque-refresh-token", null);
        when(userRepository.findByEmail("admin@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("StrongPassword123!", user.getPasswordHash())).thenReturn(true);
        when(jwtTokenService.createAccessToken(user)).thenReturn(new JwtAccessToken("signed-jwt", 900));
        when(refreshTokenService.create(eq(user), any(Instant.class))).thenReturn(issuedRefreshToken);

        AuthService.AuthSession session = authService.login(
                new LoginRequest("ADMIN@example.com", "StrongPassword123!")
        );

        assertThat(session.response().accessToken()).isEqualTo("signed-jwt");
        assertThat(session.response().expiresIn()).isEqualTo(900);
        assertThat(session.refreshToken()).isEqualTo("opaque-refresh-token");
        assertThat(user.getFailedLoginAttempts()).isZero();
        assertThat(user.getLastLoginAt()).isNotNull();
    }

    @Test
    void failedLoginsUseGenericMessageAndTemporarilyLockAccount() {
        User user = User.admin("admin@example.com", "$2a$12$hash", "Admin");
        when(userRepository.findByEmail("admin@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong-password", user.getPasswordHash())).thenReturn(false);

        for (int attempt = 1; attempt <= 5; attempt++) {
            assertThatThrownBy(() -> authService.login(new LoginRequest(" Admin@Example.com ", "wrong-password")))
                    .isInstanceOf(ApiException.class)
                    .hasMessage("Invalid email or password.");
        }

        assertThat(user.getFailedLoginAttempts()).isEqualTo(5);
        assertThat(user.getLockedUntil()).isNotNull();
        verify(auditService, times(5)).recordFailureNow(
                any(AuditActor.class),
                eq(AuditAction.AUTH_LOGIN_FAILURE),
                any(),
                eq(user.getId()),
                eq("Invalid email or password.")
        );
    }

    @Test
    void unknownEmailUsesSameGenericMessageAsWrongPassword() {
        when(userRepository.findByEmail("missing@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(new LoginRequest("missing@example.com", "whatever-password")))
                .isInstanceOf(ApiException.class)
                .hasMessage("Invalid email or password.");
        verify(auditService).recordFailureNow(
                any(AuditActor.class),
                eq(AuditAction.AUTH_LOGIN_FAILURE),
                any(),
                eq(null),
                eq("Invalid email or password.")
        );
    }
}
