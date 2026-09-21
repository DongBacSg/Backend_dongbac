package com.dongbacsaigon.backend.audit.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import com.dongbacsaigon.backend.audit.service.AuditRequestContextHolder;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class CorrelationIdFilterTest {

    @Test
    void preservesSafeIncomingValueAndReturnsIt() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(CorrelationIdFilter.CORRELATION_ID_HEADER, "request-123:test");
        MockHttpServletResponse response = new MockHttpServletResponse();

        new CorrelationIdFilter().doFilter(request, response, new MockFilterChain());

        assertThat(response.getHeader(CorrelationIdFilter.CORRELATION_ID_HEADER))
                .isEqualTo("request-123:test");
        assertThat(MDC.get("correlationId")).isNull();
        assertThat(AuditRequestContextHolder.get().correlationId()).isNull();
    }

    @Test
    void replacesUnsafeOrOversizedIncomingValue() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(CorrelationIdFilter.CORRELATION_ID_HEADER, "bad value\r\n" + "x".repeat(101));
        MockHttpServletResponse response = new MockHttpServletResponse();

        new CorrelationIdFilter().doFilter(request, response, new MockFilterChain());

        assertThat(UUID.fromString(response.getHeader(CorrelationIdFilter.CORRELATION_ID_HEADER))).isNotNull();
    }
}
