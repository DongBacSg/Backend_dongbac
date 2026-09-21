package com.dongbacsaigon.backend.lead.controller;

import com.dongbacsaigon.backend.common.response.MessageResponse;
import com.dongbacsaigon.backend.lead.dto.PublicContactRequest;
import com.dongbacsaigon.backend.lead.service.LeadService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/public/contact")
@Tag(name = "Public Contact")
class PublicContactController {

    private final LeadService leadService;

    PublicContactController(LeadService leadService) {
        this.leadService = leadService;
    }

    @PostMapping
    @Operation(summary = "Submit public contact form", description = "Creates an internal NEW Lead without returning customer data.")
    ResponseEntity<MessageResponse> create(@Valid @RequestBody PublicContactRequest request) {
        leadService.createPublicContact(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new MessageResponse("Yêu cầu của bạn đã được ghi nhận."));
    }
}
