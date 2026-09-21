package com.dongbacsaigon.backend.auth.service;

import java.time.Duration;
import java.util.Arrays;
import java.util.Optional;

import com.dongbacsaigon.backend.auth.config.AuthProperties;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

@Component
public class RefreshCookieService {

    private static final String REFRESH_COOKIE_PATH = "/api/admin/auth";

    private final AuthProperties authProperties;

    public RefreshCookieService(AuthProperties authProperties) {
        this.authProperties = authProperties;
    }

    public Optional<String> readRefreshToken(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return Optional.empty();
        }

        return Arrays.stream(cookies)
                .filter(cookie -> authProperties.refreshCookieName().equals(cookie.getName()))
                .map(Cookie::getValue)
                .findFirst();
    }

    public void addRefreshCookie(HttpServletResponse response, String rawRefreshToken) {
        ResponseCookie cookie = cookieBuilder(rawRefreshToken)
                .maxAge(authProperties.refreshTokenTtl())
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    public void clearRefreshCookie(HttpServletResponse response) {
        ResponseCookie cookie = cookieBuilder("")
                .maxAge(Duration.ZERO)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    private ResponseCookie.ResponseCookieBuilder cookieBuilder(String value) {
        return ResponseCookie.from(authProperties.refreshCookieName(), value)
                .httpOnly(true)
                .secure(authProperties.refreshCookieSecure())
                .sameSite(authProperties.refreshCookieSameSite())
                .path(REFRESH_COOKIE_PATH);
    }
}
