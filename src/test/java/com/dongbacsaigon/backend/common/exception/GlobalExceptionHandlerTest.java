package com.dongbacsaigon.backend.common.exception;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

class GlobalExceptionHandlerTest {

    @Test
    void unexpectedFailureReturnsGenericResponseWithoutInternalDetails() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/admin/dashboard");
        RuntimeException exception = new RuntimeException(
                "jdbc:postgresql://secret-host/dongbac password=secret table=users"
        );

        var response = new GlobalExceptionHandler().handleUnexpected(exception, request);

        assertThat(response.getStatusCode().value()).isEqualTo(500);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message()).isEqualTo("Unexpected server error.");
        assertThat(response.getBody().toString())
                .doesNotContain("secret-host", "password", "users", "RuntimeException");
    }
}
