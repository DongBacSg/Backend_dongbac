package com.dongbacsaigon.backend.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;

import com.dongbacsaigon.backend.auth.config.AuthProperties;
import com.dongbacsaigon.backend.auth.entity.RefreshToken;
import com.dongbacsaigon.backend.auth.repository.RefreshTokenRepository;
import com.dongbacsaigon.backend.common.exception.ApiException;
import com.dongbacsaigon.backend.user.entity.User;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class RefreshTokenServiceTest {

    private final RefreshTokenRepository refreshTokenRepository = mock(RefreshTokenRepository.class);
    private final RefreshTokenHasher refreshTokenHasher = new RefreshTokenHasher();
    private final RefreshTokenService refreshTokenService = new RefreshTokenService(
            refreshTokenRepository,
            refreshTokenHasher,
            new AuthProperties(7, "dbsg_refresh", false, "Lax", 5, 15, "", "", "")
    );

    @Test
    void createPersistsOnlyHashOfOpaqueRefreshToken() {
        User user = User.admin("admin@example.com", "$2a$12$hash", "Admin");
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RefreshTokenService.IssuedRefreshToken issuedToken = refreshTokenService.create(user, Instant.parse("2026-09-17T00:00:00Z"));

        ArgumentCaptor<RefreshToken> tokenCaptor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository).save(tokenCaptor.capture());
        RefreshToken savedToken = tokenCaptor.getValue();

        assertThat(issuedToken.rawToken()).hasSizeGreaterThanOrEqualTo(80);
        assertThat(savedToken.getTokenHash())
                .hasSize(64)
                .isNotEqualTo(issuedToken.rawToken())
                .isEqualTo(refreshTokenHasher.sha256(issuedToken.rawToken()));
    }

    @Test
    void rotateRevokesOldTokenAndPersistsReplacement() {
        User user = User.admin("admin@example.com", "$2a$12$hash", "Admin");
        String rawToken = "current-refresh-token";
        RefreshToken existingToken = new RefreshToken(
                user,
                refreshTokenHasher.sha256(rawToken),
                Instant.now().plusSeconds(3600),
                Instant.now()
        );
        when(refreshTokenRepository.findByTokenHashForUpdate(refreshTokenHasher.sha256(rawToken)))
                .thenReturn(Optional.of(existingToken));
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RefreshTokenService.RotatedRefreshToken rotatedToken = refreshTokenService.rotate(rawToken);

        ArgumentCaptor<RefreshToken> replacementCaptor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository).save(replacementCaptor.capture());
        RefreshToken replacementToken = replacementCaptor.getValue();

        assertThat(rotatedToken.user()).isSameAs(user);
        assertThat(rotatedToken.rawToken()).isNotEqualTo(rawToken);
        assertThat(existingToken.getRevokedAt()).isNotNull();
        assertThat(existingToken.getReplacementTokenId()).isEqualTo(replacementToken.getId());
        assertThat(replacementToken.getTokenHash()).isEqualTo(refreshTokenHasher.sha256(rotatedToken.rawToken()));
    }

    @Test
    void rotateRejectsRevokedToken() {
        User user = User.admin("admin@example.com", "$2a$12$hash", "Admin");
        String rawToken = "revoked-refresh-token";
        RefreshToken existingToken = new RefreshToken(
                user,
                refreshTokenHasher.sha256(rawToken),
                Instant.now().plusSeconds(3600),
                Instant.now()
        );
        existingToken.revoke(Instant.now());
        when(refreshTokenRepository.findByTokenHashForUpdate(refreshTokenHasher.sha256(rawToken)))
                .thenReturn(Optional.of(existingToken));

        assertThatThrownBy(() -> refreshTokenService.rotate(rawToken))
                .isInstanceOf(ApiException.class)
                .hasMessage("Invalid refresh token.");
    }

    @Test
    void rotateRejectsAndRevokesExpiredToken() {
        User user = User.admin("admin@example.com", "$2a$12$hash", "Admin");
        String rawToken = "expired-refresh-token";
        RefreshToken existingToken = new RefreshToken(
                user,
                refreshTokenHasher.sha256(rawToken),
                Instant.now().minusSeconds(60),
                Instant.now().minusSeconds(3600)
        );
        when(refreshTokenRepository.findByTokenHashForUpdate(refreshTokenHasher.sha256(rawToken)))
                .thenReturn(Optional.of(existingToken));

        assertThatThrownBy(() -> refreshTokenService.rotate(rawToken))
                .isInstanceOf(ApiException.class)
                .hasMessage("Invalid refresh token.");

        assertThat(existingToken.getRevokedAt()).isNotNull();
    }

    @Test
    void revokeIfPresentRevokesKnownTokenForLogout() {
        User user = User.admin("admin@example.com", "$2a$12$hash", "Admin");
        String rawToken = "logout-refresh-token";
        RefreshToken existingToken = new RefreshToken(
                user,
                refreshTokenHasher.sha256(rawToken),
                Instant.now().plusSeconds(3600),
                Instant.now()
        );
        when(refreshTokenRepository.findByTokenHashForUpdate(refreshTokenHasher.sha256(rawToken)))
                .thenReturn(Optional.of(existingToken));

        assertThat(refreshTokenService.revokeIfPresent(rawToken)).contains(user);
        assertThat(existingToken.getRevokedAt()).isNotNull();
    }
}
