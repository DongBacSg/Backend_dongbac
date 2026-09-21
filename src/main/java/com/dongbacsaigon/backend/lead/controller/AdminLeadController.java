package com.dongbacsaigon.backend.lead.controller;

import java.util.UUID;

import com.dongbacsaigon.backend.auth.security.AuthenticatedUserProvider;
import com.dongbacsaigon.backend.lead.dto.CreateLeadRequest;
import com.dongbacsaigon.backend.lead.dto.LeadPageResponse;
import com.dongbacsaigon.backend.lead.dto.LeadResponse;
import com.dongbacsaigon.backend.lead.dto.UpdateLeadAssignmentRequest;
import com.dongbacsaigon.backend.lead.dto.UpdateLeadRequest;
import com.dongbacsaigon.backend.lead.dto.UpdateLeadStatusRequest;
import com.dongbacsaigon.backend.lead.entity.LeadStatus;
import com.dongbacsaigon.backend.lead.service.LeadService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/leads")
@PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Admin Leads", description = "Internal lead management only. No public intake endpoint exists in Phase 7.")
class AdminLeadController {

    private final LeadService leadService;
    private final AuthenticatedUserProvider userProvider;

    AdminLeadController(LeadService leadService, AuthenticatedUserProvider userProvider) {
        this.leadService = leadService;
        this.userProvider = userProvider;
    }

    @PostMapping
    @Operation(summary = "Create Lead", description = "ADMIN and STAFF. Initial status is always NEW.")
    LeadResponse create(@Valid @RequestBody CreateLeadRequest request, Authentication authentication) {
        return leadService.create(request, userProvider.requireUserId(authentication));
    }

    @GetMapping
    @Operation(summary = "List Leads", description = "Search name, phone, email, and company. assignedTo conflicts with unassigned=true.")
    LeadPageResponse list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) LeadStatus status,
            @RequestParam(required = false) UUID assignedTo,
            @RequestParam(required = false) Boolean unassigned
    ) {
        return leadService.list(page, size, search, status, assignedTo, unassigned);
    }

    @GetMapping("/{leadId}")
    @Operation(summary = "Get Lead")
    LeadResponse get(@PathVariable UUID leadId) {
        return leadService.get(leadId);
    }

    @PatchMapping("/{leadId}")
    @Operation(summary = "Replace editable Lead details", description = "Does not change status or assignment.")
    LeadResponse update(
            @PathVariable UUID leadId,
            @Valid @RequestBody UpdateLeadRequest request,
            Authentication authentication
    ) {
        return leadService.update(leadId, request, userProvider.requireUserId(authentication));
    }

    @PatchMapping("/{leadId}/status")
    @Operation(summary = "Update Lead status", description = "ADMIN and STAFF. No rigid transition matrix is imposed.")
    LeadResponse updateStatus(
            @PathVariable UUID leadId,
            @Valid @RequestBody UpdateLeadStatusRequest request,
            Authentication authentication
    ) {
        return leadService.updateStatus(leadId, request, userProvider.requireUserId(authentication));
    }

    @PatchMapping("/{leadId}/assignment")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Assign or unassign Lead", description = "ADMIN only. Set assignedTo to null to unassign.")
    LeadResponse updateAssignment(
            @PathVariable UUID leadId,
            @Valid @RequestBody UpdateLeadAssignmentRequest request,
            Authentication authentication
    ) {
        return leadService.updateAssignment(leadId, request, userProvider.requireUserId(authentication));
    }
}
