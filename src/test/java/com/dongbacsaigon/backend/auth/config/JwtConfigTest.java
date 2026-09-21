package com.dongbacsaigon.backend.auth.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.Base64;

import javax.crypto.SecretKey;

import com.dongbacsaigon.backend.auth.service.JwtAccessToken;
import com.dongbacsaigon.backend.auth.service.JwtTokenService;
import com.dongbacsaigon.backend.user.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;

class JwtConfigTest {

    private static final String ISSUER = "dongbac-backend";
    private final JwtConfig jwtConfig = new JwtConfig();
    private final JwtProperties properties = properties((byte) 7, ISSUER);
    private final SecretKey secretKey = jwtConfig.jwtSecretKey(properties);
    private final JwtEncoder encoder = jwtConfig.jwtEncoder(secretKey);
    private final JwtDecoder decoder = jwtConfig.jwtDecoder(secretKey, properties);

    @Test
    void createsAndValidatesHs256TokenWithExpectedClaims() {
        User user = User.admin("admin@example.com", "$2a$12$hash", "Admin");
        JwtAccessToken accessToken = new JwtTokenService(encoder, properties).createAccessToken(user);

        var jwt = decoder.decode(accessToken.tokenValue());

        assertThat(jwt.getHeaders()).containsEntry("alg", "HS256");
        assertThat(jwt.getClaimAsString("iss")).isEqualTo(ISSUER);
        assertThat(jwt.getSubject()).isEqualTo(user.getId().toString());
        assertThat(jwt.getClaimAsString("role")).isEqualTo("ADMIN");
        assertThat(jwt.getExpiresAt()).isAfter(jwt.getIssuedAt());
    }

    @Test
    void rejectsTokenSignedWithAnotherSecret() {
        JwtProperties otherProperties = properties((byte) 11, ISSUER);
        JwtEncoder otherEncoder = jwtConfig.jwtEncoder(jwtConfig.jwtSecretKey(otherProperties));
        String token = encode(otherEncoder, ISSUER, Instant.now().plusSeconds(60));

        assertThatThrownBy(() -> decoder.decode(token)).isInstanceOf(JwtException.class);
    }

    @Test
    void rejectsWrongIssuerExpiredAndMalformedTokens() {
        String wrongIssuer = encode(encoder, "another-issuer", Instant.now().plusSeconds(60));
        String expired = encode(encoder, ISSUER, Instant.now().minusSeconds(60));

        assertThatThrownBy(() -> decoder.decode(wrongIssuer)).isInstanceOf(JwtException.class);
        assertThatThrownBy(() -> decoder.decode(expired)).isInstanceOf(JwtException.class);
        assertThatThrownBy(() -> decoder.decode("not-a-jwt")).isInstanceOf(JwtException.class);
    }

    @Test
    void rejectsSecretShorterThan256Bits() {
        JwtProperties weakProperties = new JwtProperties(
                Base64.getEncoder().encodeToString(new byte[31]),
                ISSUER,
                15
        );

        assertThatThrownBy(() -> jwtConfig.jwtSecretKey(weakProperties))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("at least 32 random bytes");
    }

    private String encode(JwtEncoder jwtEncoder, String issuer, Instant expiresAt) {
        Instant issuedAt = expiresAt.minusSeconds(30);
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(issuer)
                .subject("test-user")
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .build();
        return jwtEncoder.encode(JwtEncoderParameters.from(
                JwsHeader.with(MacAlgorithm.HS256).build(),
                claims
        )).getTokenValue();
    }

    private JwtProperties properties(byte value, String issuer) {
        byte[] bytes = new byte[32];
        java.util.Arrays.fill(bytes, value);
        return new JwtProperties(Base64.getEncoder().encodeToString(bytes), issuer, 15);
    }
}
