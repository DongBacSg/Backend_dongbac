package com.dongbacsaigon.backend.auth.security;

import java.io.IOException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class PasswordChangeRequiredFilter extends OncePerRequestFilter {

    private final SecurityErrorWriter securityErrorWriter;

    PasswordChangeRequiredFilter(SecurityErrorWriter securityErrorWriter) {
        this.securityErrorWriter = securityErrorWriter;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (isAuthenticated(authentication)
                && mustChangePassword(authentication)
                && isAdminApi(request)
                && !isAllowedWhilePasswordChangeRequired(request)
                && !HttpMethod.OPTIONS.matches(request.getMethod())) {
            securityErrorWriter.write(
                    request,
                    response,
                    HttpStatus.FORBIDDEN,
                    "Password change is required before accessing this resource."
            );
            return;
        }

        filterChain.doFilter(request, response);
    }

    private boolean isAuthenticated(Authentication authentication) {
        return authentication != null
                && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken);
    }

    private boolean mustChangePassword(Authentication authentication) {
        if (!(authentication.getPrincipal() instanceof Jwt jwt)) {
            return false;
        }
        Object claim = jwt.getClaims().get("must_change_password");
        return Boolean.TRUE.equals(claim);
    }

    private boolean isAllowedWhilePasswordChangeRequired(HttpServletRequest request) {
        String path = request.getServletPath();
        String method = request.getMethod();

        return ("GET".equalsIgnoreCase(method) && "/api/admin/auth/me".equals(path))
                || ("POST".equalsIgnoreCase(method) && "/api/admin/auth/change-password".equals(path))
                || ("POST".equalsIgnoreCase(method) && "/api/admin/auth/logout".equals(path))
                || ("POST".equalsIgnoreCase(method) && "/api/admin/auth/refresh".equals(path));
    }

    private boolean isAdminApi(HttpServletRequest request) {
        return request.getServletPath().startsWith("/api/admin/");
    }
}
