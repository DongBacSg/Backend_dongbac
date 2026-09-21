package com.dongbacsaigon.backend.auth.service;

public record JwtAccessToken(String tokenValue, long expiresInSeconds) {
}
