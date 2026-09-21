package com.dongbacsaigon.backend.approval.controller;

import java.time.Instant;
import java.util.UUID;

import com.dongbacsaigon.backend.approval.dto.ApprovalApproveRequest;
import com.dongbacsaigon.backend.approval.dto.ApprovalPageResponse;
import com.dongbacsaigon.backend.approval.dto.ApprovalRejectRequest;
import com.dongbacsaigon.backend.approval.dto.ApprovalRequestResponse;
import com.dongbacsaigon.backend.approval.entity.ApprovalRequestStatus;
import com.dongbacsaigon.backend.approval.entity.ApprovalResourceType;
import com.dongbacsaigon.backend.approval.service.ApprovalService;
import com.dongbacsaigon.backend.auth.security.AuthenticatedUserProvider;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/approvals")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Approval Center")
class ApprovalController {

    private final ApprovalService approvalService;
    private final AuthenticatedUserProvider authenticatedUserProvider;

    ApprovalController(ApprovalService approvalService, AuthenticatedUserProvider authenticatedUserProvider) {
        this.approvalService = approvalService;
        this.authenticatedUserProvider = authenticatedUserProvider;
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "List all approval requests", description = "ADMIN only.")
    ApprovalPageResponse listApprovals(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) ApprovalRequestStatus status,
            @RequestParam(required = false) ApprovalResourceType resourceType,
            @RequestParam(required = false) UUID submittedBy,
            @RequestParam(required = false) Instant dateFrom,
            @RequestParam(required = false) Instant dateTo
    ) {
        return approvalService.listApprovals(page, size, status, resourceType, submittedBy, dateFrom, dateTo);
    }

    @GetMapping("/mine")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    @Operation(summary = "List the current user's approval history")
    ApprovalPageResponse listMine(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) ApprovalRequestStatus status,
            @RequestParam(required = false) ApprovalResourceType resourceType,
            Authentication authentication
    ) {
        return approvalService.listMine(
                authenticatedUserProvider.requireUserId(authentication),
                page,
                size,
                status,
                resourceType
        );
    }

    @GetMapping("/mine/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    @Operation(summary = "Get one approval request from the current user's history")
    ApprovalRequestResponse getMine(
            @PathVariable UUID id,
            Authentication authentication
    ) {
        return approvalService.getMineOrAdmin(
                id,
                authenticatedUserProvider.requireUserId(authentication),
                isAdmin(authentication)
        );
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get one approval request", description = "ADMIN only.")
    ApprovalRequestResponse getApproval(@PathVariable UUID id) {
        return approvalService.getApproval(id);
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Approve a pending approval request", description = "ADMIN only.")
    ApprovalRequestResponse approve(
            @PathVariable UUID id,
            @Valid @RequestBody(required = false) ApprovalApproveRequest request,
            Authentication authentication
    ) {
        return approvalService.approve(id, request, authenticatedUserProvider.requireUserId(authentication));
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Reject a pending approval request", description = "ADMIN only.")
    ApprovalRequestResponse reject(
            @PathVariable UUID id,
            @Valid @RequestBody ApprovalRejectRequest request,
            Authentication authentication
    ) {
        return approvalService.reject(id, request, authenticatedUserProvider.requireUserId(authentication));
    }

    private boolean isAdmin(Authentication authentication) {
        return authentication.getAuthorities()
                .stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch("ROLE_ADMIN"::equals);
    }
}
