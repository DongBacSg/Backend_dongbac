package com.dongbacsaigon.backend.common.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;

import com.dongbacsaigon.backend.common.config.HttpRequestProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class JsonRequestSizeLimitFilterTest {

    private final JsonRequestSizeLimitFilter filter = new JsonRequestSizeLimitFilter(
            new HttpRequestProperties(1024),
            new ObjectMapper().findAndRegisterModules()
    );

    @Test
    void rejectsJsonBodyLargerThanConfiguredLimit() throws Exception {
        MockHttpServletRequest request = jsonRequest("x".repeat(1025));
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getStatus()).isEqualTo(413);
        assertThat(response.getContentAsString()).contains("JSON request body is too large.");
    }

    @Test
    void preservesAllowedJsonBodyForDownstreamDeserialization() throws Exception {
        String body = "{\"name\":\"Dong Bac\"}";
        MockHttpServletRequest request = jsonRequest(body);
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(chain.getRequest().getInputStream().readAllBytes())
                .isEqualTo(body.getBytes(StandardCharsets.UTF_8));
        assertThat(response.getStatus()).isEqualTo(200);
    }

    private MockHttpServletRequest jsonRequest(String body) {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/admin/test");
        request.setContentType("application/json");
        request.setContent(body.getBytes(StandardCharsets.UTF_8));
        return request;
    }
}
