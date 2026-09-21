package com.dongbacsaigon.backend.auth.dto;

public record AuthTokenResponse(
        String accessToken,
        String tokenType,
        long expiresIn,
        AuthenticatedUserResponse user
) {
}
