package com.dongbacsaigon.backend.auth.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

class SecurityFiltersTest {

    private final SecurityErrorWriter errorWriter = new SecurityErrorWriter(
            new ObjectMapper().findAndRegisterModules()
    );

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void csrfGuardRejectsCookieCredentialEndpointsWithoutHeader() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/admin/auth/refresh");
        request.setServletPath("/api/admin/auth/refresh");
        MockHttpServletResponse response = new MockHttpServletResponse();

        new CsrfGuardFilter(errorWriter).doFilter(request, response, new MockFilterChain());

        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(response.getContentAsString()).contains("Missing or invalid CSRF guard header.");
    }

    @Test
    void csrfGuardAcceptsExplicitHeaderAndIgnoresOtherPosts() throws Exception {
        CsrfGuardFilter filter = new CsrfGuardFilter(errorWriter);
        MockHttpServletRequest refresh = new MockHttpServletRequest("POST", "/api/admin/auth/refresh");
        refresh.setServletPath("/api/admin/auth/refresh");
        refresh.addHeader(CsrfGuardFilter.CSRF_GUARD_HEADER, "1");
        MockHttpServletResponse refreshResponse = new MockHttpServletResponse();
        filter.doFilter(refresh, refreshResponse, new MockFilterChain());

        MockHttpServletRequest other = new MockHttpServletRequest("POST", "/api/admin/leads");
        other.setServletPath("/api/admin/leads");
        MockHttpServletResponse otherResponse = new MockHttpServletResponse();
        filter.doFilter(other, otherResponse, new MockFilterChain());

        assertThat(refreshResponse.getStatus()).isEqualTo(200);
        assertThat(otherResponse.getStatus()).isEqualTo(200);
    }

    @Test
    void mustChangePasswordBlocksAdminApiButAllowsChangePassword() throws Exception {
        Jwt jwt = new Jwt(
                "token",
                Instant.now(),
                Instant.now().plusSeconds(60),
                Map.of("alg", "HS256"),
                Map.of("sub", "user-id", "must_change_password", true)
        );
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(
                jwt,
                List.of(new SimpleGrantedAuthority("ROLE_STAFF"))
        ));
        PasswordChangeRequiredFilter filter = new PasswordChangeRequiredFilter(errorWriter);

        MockHttpServletRequest blocked = new MockHttpServletRequest("GET", "/api/admin/leads");
        blocked.setServletPath("/api/admin/leads");
        MockHttpServletResponse blockedResponse = new MockHttpServletResponse();
        filter.doFilter(blocked, blockedResponse, new MockFilterChain());

        MockHttpServletRequest allowed = new MockHttpServletRequest("POST", "/api/admin/auth/change-password");
        allowed.setServletPath("/api/admin/auth/change-password");
        MockHttpServletResponse allowedResponse = new MockHttpServletResponse();
        filter.doFilter(allowed, allowedResponse, new MockFilterChain());

        assertThat(blockedResponse.getStatus()).isEqualTo(403);
        assertThat(allowedResponse.getStatus()).isEqualTo(200);
    }
}
