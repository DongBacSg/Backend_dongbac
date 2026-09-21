package com.dongbacsaigon.backend.auth.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class RefreshTokenHasherTest {

    private final RefreshTokenHasher refreshTokenHasher = new RefreshTokenHasher();

    @Test
    void sha256ReturnsHexDigestWithoutRawToken() {
        String hash = refreshTokenHasher.sha256("raw-refresh-token");

        assertThat(hash)
                .hasSize(64)
                .isEqualTo("0881b36898a91d864edaf39d2b2bd5801d5f873e3142a9ec5b3b574c4f6b51e5");
        assertThat(hash).doesNotContain("raw-refresh-token");
    }
}
