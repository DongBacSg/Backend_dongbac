package com.dongbacsaigon.backend.auth.service;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;

import com.dongbacsaigon.backend.auth.config.AuthProperties;
import com.dongbacsaigon.backend.auth.entity.RefreshToken;
import com.dongbacsaigon.backend.auth.repository.RefreshTokenRepository;
import com.dongbacsaigon.backend.common.exception.ApiException;
import com.dongbacsaigon.backend.user.entity.User;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class RefreshTokenService {

    private static final int REFRESH_TOKEN_RANDOM_BYTES = 64;
    private static final String INVALID_REFRESH_TOKEN_MESSAGE = "Invalid refresh token.";

    private final SecureRandom secureRandom = new SecureRandom();
    private final RefreshTokenRepository refreshTokenRepository;
    private final RefreshTokenHasher refreshTokenHasher;
    private final AuthProperties authProperties;

    public RefreshTokenService(
            RefreshTokenRepository refreshTokenRepository,
            RefreshTokenHasher refreshTokenHasher,
            AuthProperties authProperties
    ) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.refreshTokenHasher = refreshTokenHasher;
        this.authProperties = authProperties;
    }

    public IssuedRefreshToken create(User user, Instant now) {
        String rawToken = generateRawToken();
        RefreshToken refreshToken = new RefreshToken(
                user,
                refreshTokenHasher.sha256(rawToken),
                now.plus(authProperties.refreshTokenTtl()),
                now
        );
        refreshTokenRepository.save(refreshToken);
        return new IssuedRefreshToken(rawToken, refreshToken);
    }

    @Transactional
    public RotatedRefreshToken rotate(String rawToken) {
        if (!StringUtils.hasText(rawToken)) {
            throw invalidRefreshToken();
        }

        Instant now = Instant.now();
        RefreshToken refreshToken = refreshTokenRepository.findByTokenHashForUpdate(refreshTokenHasher.sha256(rawToken))
                .orElseThrow(this::invalidRefreshToken);

        if (refreshToken.isRevoked()) {
            throw invalidRefreshToken();
        }

        if (refreshToken.isExpired(now)) {
            refreshToken.revoke(now);
            throw invalidRefreshToken();
        }

        User user = refreshToken.getUser();
        if (!user.isActive()) {
            refreshToken.revoke(now);
            throw invalidRefreshToken();
        }

        String replacementRawToken = generateRawToken();
        RefreshToken replacementToken = new RefreshToken(
                user,
                refreshTokenHasher.sha256(replacementRawToken),
                now.plus(authProperties.refreshTokenTtl()),
                now
        );
        refreshTokenRepository.save(replacementToken);
        refreshToken.replaceWith(replacementToken.getId(), now);

        return new RotatedRefreshToken(replacementRawToken, user);
    }

    @Transactional
    public Optional<User> revokeIfPresent(String rawToken) {
        if (!StringUtils.hasText(rawToken)) {
            return Optional.empty();
        }

        Instant now = Instant.now();
        Optional<RefreshToken> refreshToken = refreshTokenRepository.findByTokenHashForUpdate(refreshTokenHasher.sha256(rawToken));
        refreshToken.ifPresent(token -> token.revoke(now));
        return refreshToken.map(RefreshToken::getUser);
    }

    @Transactional
    public int revokeAllActiveForUser(UUID userId) {
        return refreshTokenRepository.revokeActiveByUserId(userId, Instant.now());
    }

    private String generateRawToken() {
        byte[] randomBytes = new byte[REFRESH_TOKEN_RANDOM_BYTES];
        secureRandom.nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }

    private ApiException invalidRefreshToken() {
        return new ApiException(HttpStatus.UNAUTHORIZED, INVALID_REFRESH_TOKEN_MESSAGE);
    }

    public record IssuedRefreshToken(String rawToken, RefreshToken refreshToken) {
    }

    public record RotatedRefreshToken(String rawToken, User user) {
    }
}
