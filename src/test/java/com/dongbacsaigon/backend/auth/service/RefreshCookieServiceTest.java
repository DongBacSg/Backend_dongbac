package com.dongbacsaigon.backend.auth.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.dongbacsaigon.backend.auth.config.AuthProperties;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletResponse;

class RefreshCookieServiceTest {

    @Test
    void productionCookieIsHttpOnlySecureAndCrossSite() {
        MockHttpServletResponse response = new MockHttpServletResponse();
        service(true, "None").addRefreshCookie(response, "opaque-token");

        String cookie = response.getHeader(HttpHeaders.SET_COOKIE);
        assertThat(cookie)
                .contains("dbsg_refresh=opaque-token")
                .contains("Path=/api/admin/auth")
                .contains("HttpOnly")
                .contains("Secure")
                .contains("SameSite=None");
    }

    @Test
    void localCookieUsesLaxWithoutSecureFlag() {
        MockHttpServletResponse response = new MockHttpServletResponse();
        service(false, "Lax").addRefreshCookie(response, "opaque-token");

        assertThat(response.getHeader(HttpHeaders.SET_COOKIE))
                .contains("HttpOnly")
                .contains("SameSite=Lax")
                .doesNotContain("; Secure");
    }

    private RefreshCookieService service(boolean secure, String sameSite) {
        return new RefreshCookieService(new AuthProperties(
                7, "dbsg_refresh", secure, sameSite, 5, 15, "", "", ""
        ));
    }
}
