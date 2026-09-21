package com.dongbacsaigon.backend.auth.controller;

import com.dongbacsaigon.backend.auth.dto.AuthTokenResponse;
import com.dongbacsaigon.backend.auth.dto.ChangePasswordRequest;
import com.dongbacsaigon.backend.auth.dto.CurrentUserResponse;
import com.dongbacsaigon.backend.auth.dto.LoginRequest;
import com.dongbacsaigon.backend.auth.security.AuthenticatedUserProvider;
import com.dongbacsaigon.backend.auth.security.CsrfGuardFilter;
import com.dongbacsaigon.backend.auth.service.AuthService;
import com.dongbacsaigon.backend.auth.service.RefreshCookieService;
import com.dongbacsaigon.backend.common.exception.ApiException;
import com.dongbacsaigon.backend.common.response.MessageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/auth")
@Tag(name = "Admin Authentication")
class AdminAuthController {

    private final AuthService authService;
    private final RefreshCookieService refreshCookieService;
    private final AuthenticatedUserProvider authenticatedUserProvider;

    AdminAuthController(
            AuthService authService,
            RefreshCookieService refreshCookieService,
            AuthenticatedUserProvider authenticatedUserProvider
    ) {
        this.authService = authService;
        this.refreshCookieService = refreshCookieService;
        this.authenticatedUserProvider = authenticatedUserProvider;
    }

    @PostMapping("/login")
    @Operation(summary = "Log in to the internal admin system")
    ResponseEntity<AuthTokenResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletResponse response
    ) {
        AuthService.AuthSession session = authService.login(request);
        refreshCookieService.addRefreshCookie(response, session.refreshToken());
        return ResponseEntity.ok(session.response());
    }

    @PostMapping("/refresh")
    @Operation(
            summary = "Rotate refresh cookie and issue a new access token",
            description = "Requires the HttpOnly refresh cookie and X-CSRF-Guard: 1."
    )
    ResponseEntity<AuthTokenResponse> refresh(
            HttpServletRequest request,
            HttpServletResponse response,
            @Parameter(hidden = true)
            @RequestHeader(name = CsrfGuardFilter.CSRF_GUARD_HEADER, required = false) String csrfGuard
    ) {
        String rawRefreshToken = refreshCookieService.readRefreshToken(request)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Invalid refresh token."));
        AuthService.AuthSession session = authService.refresh(rawRefreshToken);
        refreshCookieService.addRefreshCookie(response, session.refreshToken());
        return ResponseEntity.ok(session.response());
    }

    @PostMapping("/logout")
    @Operation(
            summary = "Log out of the internal admin system",
            description = "Requires X-CSRF-Guard: 1. If a refresh cookie exists, it is revoked."
    )
    ResponseEntity<MessageResponse> logout(
            HttpServletRequest request,
            HttpServletResponse response,
            @Parameter(hidden = true)
            @RequestHeader(name = CsrfGuardFilter.CSRF_GUARD_HEADER, required = false) String csrfGuard
    ) {
        authService.logout(refreshCookieService.readRefreshToken(request).orElse(null));
        refreshCookieService.clearRefreshCookie(response);
        return ResponseEntity.ok(new MessageResponse("Logged out."));
    }

    @GetMapping("/me")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Get the current authenticated internal user")
    CurrentUserResponse me(Authentication authentication) {
        return authService.currentUser(authenticatedUserProvider.requireUserId(authentication));
    }

    @PostMapping("/change-password")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Change the current user's password")
    ResponseEntity<MessageResponse> changePassword(
            @Valid @RequestBody ChangePasswordRequest request,
            Authentication authentication,
            HttpServletResponse response
    ) {
        authService.changeOwnPassword(authenticatedUserProvider.requireUserId(authentication), request);
        refreshCookieService.clearRefreshCookie(response);
        return ResponseEntity.ok(new MessageResponse("Password changed. Please log in again."));
    }
}
