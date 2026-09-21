package com.dongbacsaigon.backend.user.controller;

import java.util.UUID;

import com.dongbacsaigon.backend.auth.security.AuthenticatedUserProvider;
import com.dongbacsaigon.backend.user.dto.StaffAccountResponse;
import com.dongbacsaigon.backend.user.dto.StaffCreateRequest;
import com.dongbacsaigon.backend.user.dto.StaffPageResponse;
import com.dongbacsaigon.backend.user.dto.StaffPasswordResetRequest;
import com.dongbacsaigon.backend.user.dto.StaffUpdateRequest;
import com.dongbacsaigon.backend.user.entity.UserStatus;
import com.dongbacsaigon.backend.user.service.StaffService;
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
@RequestMapping("/api/admin/staff")
@PreAuthorize("hasRole('ADMIN')")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Admin Staff Management")
class StaffController {

    private final StaffService staffService;
    private final AuthenticatedUserProvider authenticatedUserProvider;

    StaffController(StaffService staffService, AuthenticatedUserProvider authenticatedUserProvider) {
        this.staffService = staffService;
        this.authenticatedUserProvider = authenticatedUserProvider;
    }

    @GetMapping
    @Operation(summary = "List STAFF accounts")
    StaffPageResponse listStaff(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) UserStatus status
    ) {
        return staffService.listStaff(page, size, search, status);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a STAFF account")
    StaffAccountResponse getStaff(@PathVariable UUID id) {
        return staffService.getStaff(id);
    }

    @PostMapping
    @Operation(summary = "Create a STAFF account")
    StaffAccountResponse createStaff(
            @Valid @RequestBody StaffCreateRequest request,
            Authentication authentication
    ) {
        return staffService.createStaff(request, authenticatedUserProvider.requireUserId(authentication));
    }

    @PatchMapping("/{id}")
    @Operation(summary = "Update safe STAFF account fields")
    StaffAccountResponse updateStaff(
            @PathVariable UUID id,
            @Valid @RequestBody StaffUpdateRequest request,
            Authentication authentication
    ) {
        return staffService.updateStaff(id, request, authenticatedUserProvider.requireUserId(authentication));
    }

    @PatchMapping("/{id}/lock")
    @Operation(summary = "Lock a STAFF account")
    StaffAccountResponse lockStaff(
            @PathVariable UUID id,
            Authentication authentication
    ) {
        return staffService.lockStaff(id, authenticatedUserProvider.requireUserId(authentication));
    }

    @PatchMapping("/{id}/unlock")
    @Operation(summary = "Unlock a STAFF account")
    StaffAccountResponse unlockStaff(
            @PathVariable UUID id,
            Authentication authentication
    ) {
        return staffService.unlockStaff(id, authenticatedUserProvider.requireUserId(authentication));
    }

    @PostMapping("/{id}/reset-password")
    @Operation(summary = "Reset a STAFF password to a temporary password")
    StaffAccountResponse resetPassword(
            @PathVariable UUID id,
            @Valid @RequestBody StaffPasswordResetRequest request,
            Authentication authentication
    ) {
        return staffService.resetPassword(id, request, authenticatedUserProvider.requireUserId(authentication));
    }
}
