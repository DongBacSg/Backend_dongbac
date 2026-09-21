package com.dongbacsaigon.backend.common.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.cors.CorsConfiguration;

class CorsConfigTest {

    @Test
    void allowsOnlyConfiguredOriginsWithCredentials() {
        CorsProperties properties = new CorsProperties(List.of(
                "https://dong-bac-group.vercel.app",
                "https://admin-dongbac-lhzv.vercel.app"
        ));
        MockHttpServletRequest request = new MockHttpServletRequest("OPTIONS", "/api/admin/leads");
        CorsConfiguration configuration = new CorsConfig(properties)
                .corsConfigurationSource()
                .getCorsConfiguration(request);

        assertThat(configuration).isNotNull();
        assertThat(configuration.getAllowCredentials()).isTrue();
        assertThat(configuration.getAllowedOrigins()).containsExactlyElementsOf(properties.allowedOrigins());
        assertThat(configuration.checkOrigin("https://admin-dongbac-lhzv.vercel.app"))
                .isEqualTo("https://admin-dongbac-lhzv.vercel.app");
        assertThat(configuration.checkOrigin("https://attacker.example")).isNull();
        assertThat(configuration.getAllowedOrigins()).doesNotContain("*");
        assertThat(configuration.getAllowedMethods())
                .containsExactly("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS");
        assertThat(configuration.getExposedHeaders()).containsExactly("X-Correlation-ID");
    }

    @Test
    void rejectsWildcardAndNonOriginUrls() {
        assertThatThrownBy(() -> new CorsProperties(List.of("*")))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new CorsProperties(List.of("https://trusted.example/path")))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
