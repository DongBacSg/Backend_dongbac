package com.dongbacsaigon.backend.auth.security;

import java.io.IOException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class CsrfGuardFilter extends OncePerRequestFilter {

    public static final String CSRF_GUARD_HEADER = "X-CSRF-Guard";

    private final SecurityErrorWriter securityErrorWriter;

    CsrfGuardFilter(SecurityErrorWriter securityErrorWriter) {
        this.securityErrorWriter = securityErrorWriter;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        if (requiresGuard(request) && !"1".equals(request.getHeader(CSRF_GUARD_HEADER))) {
            securityErrorWriter.write(
                    request,
                    response,
                    HttpStatus.FORBIDDEN,
                    "Missing or invalid CSRF guard header."
            );
            return;
        }

        filterChain.doFilter(request, response);
    }

    private boolean requiresGuard(HttpServletRequest request) {
        return "POST".equalsIgnoreCase(request.getMethod())
                && ("/api/admin/auth/refresh".equals(request.getServletPath())
                || "/api/admin/auth/logout".equals(request.getServletPath()));
    }
}
