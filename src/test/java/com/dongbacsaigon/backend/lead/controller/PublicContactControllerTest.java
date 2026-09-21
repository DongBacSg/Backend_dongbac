package com.dongbacsaigon.backend.lead.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.util.Arrays;

import com.dongbacsaigon.backend.lead.dto.PublicContactRequest;
import com.dongbacsaigon.backend.lead.service.LeadService;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class PublicContactControllerTest {

    @Test
    void publicRequestCannotBindInternalLeadFieldsAndResponseContainsNoLeadData() {
        assertThat(Arrays.stream(PublicContactRequest.class.getRecordComponents())
                .map(component -> component.getName()))
                .containsExactly("name", "phone", "email", "subject", "message");

        LeadService leadService = mock(LeadService.class);
        PublicContactRequest request = new PublicContactRequest("Customer", "0901234567", null, null, "Hello");

        var response = new PublicContactController(leadService).create(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message()).isEqualTo("Yêu cầu của bạn đã được ghi nhận.");
        verify(leadService).createPublicContact(request);
    }
}
