package com.dongbacsaigon.backend.auth.service;

import java.time.Instant;
import java.util.UUID;

import com.dongbacsaigon.backend.auth.config.AuthProperties;
import com.dongbacsaigon.backend.auth.dto.AuthTokenResponse;
import com.dongbacsaigon.backend.auth.dto.AuthenticatedUserResponse;
import com.dongbacsaigon.backend.auth.dto.ChangePasswordRequest;
import com.dongbacsaigon.backend.auth.dto.CurrentUserResponse;
import com.dongbacsaigon.backend.auth.dto.LoginRequest;
import com.dongbacsaigon.backend.audit.entity.AuditAction;
import com.dongbacsaigon.backend.audit.entity.AuditTargetType;
import com.dongbacsaigon.backend.audit.service.AuditActor;
import com.dongbacsaigon.backend.audit.service.AuditService;
import com.dongbacsaigon.backend.common.exception.ApiException;
import com.dongbacsaigon.backend.user.entity.User;
import com.dongbacsaigon.backend.user.repository.UserRepository;
import com.dongbacsaigon.backend.user.service.EmailNormalizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class AuthService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AuthService.class);
    private static final String INVALID_CREDENTIALS_MESSAGE = "Invalid email or password.";

    private final UserRepository userRepository;
    private final EmailNormalizer emailNormalizer;
    private final PasswordEncoder passwordEncoder;
    private final PasswordPolicyService passwordPolicyService;
    private final JwtTokenService jwtTokenService;
    private final RefreshTokenService refreshTokenService;
    private final AuthProperties authProperties;
    private final AuditService auditService;

    public AuthService(
            UserRepository userRepository,
            EmailNormalizer emailNormalizer,
            PasswordEncoder passwordEncoder,
            PasswordPolicyService passwordPolicyService,
            JwtTokenService jwtTokenService,
            RefreshTokenService refreshTokenService,
            AuthProperties authProperties,
            AuditService auditService
    ) {
        this.userRepository = userRepository;
        this.emailNormalizer = emailNormalizer;
        this.passwordEncoder = passwordEncoder;
        this.passwordPolicyService = passwordPolicyService;
        this.jwtTokenService = jwtTokenService;
        this.refreshTokenService = refreshTokenService;
        this.authProperties = authProperties;
        this.auditService = auditService;
    }

    @Transactional
    public AuthSession login(LoginRequest request) {
        String normalizedEmail = emailNormalizer.normalize(request.email());
        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> {
                    LOGGER.info("Failed login attempt for unknown account");
                    auditService.recordFailureNow(
                            AuditActor.unknownEmail(normalizedEmail),
                            AuditAction.AUTH_LOGIN_FAILURE,
                            AuditTargetType.AUTH,
                            null,
                            INVALID_CREDENTIALS_MESSAGE
                    );
                    return invalidCredentials();
                });

        Instant now = Instant.now();
        if (!user.isActive() || user.isTemporarilyLocked(now)) {
            LOGGER.info("Rejected login for user {} with inactive or temporarily locked account", user.getId());
            auditService.recordFailureNow(
                    AuditActor.from(user),
                    AuditAction.AUTH_LOGIN_FAILURE,
                    AuditTargetType.AUTH,
                    user.getId(),
                    INVALID_CREDENTIALS_MESSAGE
            );
            throw invalidCredentials();
        }

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            user.recordFailedLogin(
                    authProperties.maxFailedAttempts(),
                    now.plus(authProperties.lockDuration())
            );
            LOGGER.info("Failed login attempt for user {}", user.getId());
            auditService.recordFailureNow(
                    AuditActor.from(user),
                    AuditAction.AUTH_LOGIN_FAILURE,
                    AuditTargetType.AUTH,
                    user.getId(),
                    INVALID_CREDENTIALS_MESSAGE
            );
            throw invalidCredentials();
        }

        user.recordSuccessfulLogin(now);
        RefreshTokenService.IssuedRefreshToken refreshToken = refreshTokenService.create(user, now);
        LOGGER.info("Successful login for user {}", user.getId());
        auditService.recordSuccessAfterCommit(
                user,
                AuditAction.AUTH_LOGIN_SUCCESS,
                AuditTargetType.AUTH,
                user.getId(),
                null
        );

        return new AuthSession(buildAuthTokenResponse(user), refreshToken.rawToken());
    }

    @Transactional
    public AuthSession refresh(String rawRefreshToken) {
        RefreshTokenService.RotatedRefreshToken rotatedRefreshToken = refreshTokenService.rotate(rawRefreshToken);
        return new AuthSession(
                buildAuthTokenResponse(rotatedRefreshToken.user()),
                rotatedRefreshToken.rawToken()
        );
    }

    @Transactional
    public void logout(String rawRefreshToken) {
        if (StringUtils.hasText(rawRefreshToken)) {
            refreshTokenService.revokeIfPresent(rawRefreshToken)
                    .ifPresent(user -> auditService.recordSuccessAfterCommit(
                            user,
                            AuditAction.AUTH_LOGOUT,
                            AuditTargetType.AUTH,
                            user.getId(),
                            null
                    ));
        }
    }

    @Transactional(readOnly = true)
    public CurrentUserResponse currentUser(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Unauthorized."));
        return new CurrentUserResponse(
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                user.getRole(),
                user.getStatus(),
                user.isMustChangePassword()
        );
    }

    @Transactional
    public void changeOwnPassword(UUID userId, ChangePasswordRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Unauthorized."));

        if (!user.isActive()) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Account is locked.");
        }

        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Current password is incorrect.");
        }

        passwordPolicyService.validate(request.newPassword());
        user.changePassword(passwordEncoder.encode(request.newPassword()), false);
        user.resetLoginLock();
        refreshTokenService.revokeAllActiveForUser(user.getId());
        LOGGER.info("Password changed for user {}", user.getId());
        auditService.recordSuccessAfterCommit(
                user,
                AuditAction.AUTH_PASSWORD_CHANGED,
                AuditTargetType.USER,
                user.getId(),
                null
        );
    }

    private AuthTokenResponse buildAuthTokenResponse(User user) {
        JwtAccessToken accessToken = jwtTokenService.createAccessToken(user);
        return new AuthTokenResponse(
                accessToken.tokenValue(),
                "Bearer",
                accessToken.expiresInSeconds(),
                new AuthenticatedUserResponse(
                        user.getId(),
                        user.getEmail(),
                        user.getFullName(),
                        user.getRole(),
                        user.isMustChangePassword()
                )
        );
    }

    private ApiException invalidCredentials() {
        return new ApiException(HttpStatus.UNAUTHORIZED, INVALID_CREDENTIALS_MESSAGE);
    }

    public record AuthSession(AuthTokenResponse response, String refreshToken) {
    }
}
