package com.dongbacsaigon.backend.dashboard.controller;

import com.dongbacsaigon.backend.auth.security.AuthenticatedUserProvider;
import com.dongbacsaigon.backend.dashboard.dto.DashboardSummaryResponse;
import com.dongbacsaigon.backend.dashboard.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/dashboard")
@PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Admin Dashboard")
class AdminDashboardController {

    private final DashboardService dashboardService;
    private final AuthenticatedUserProvider userProvider;

    AdminDashboardController(DashboardService dashboardService, AuthenticatedUserProvider userProvider) {
        this.dashboardService = dashboardService;
        this.userProvider = userProvider;
    }

    @GetMapping("/summary")
    @Operation(
            summary = "Get operational Dashboard summary",
            description = "ADMIN sees global pending approvals. STAFF sees only pending approvals submitted by that user."
    )
    DashboardSummaryResponse summary(Authentication authentication) {
        return dashboardService.summary(userProvider.requireUserId(authentication), isAdmin(authentication));
    }

    private boolean isAdmin(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch("ROLE_ADMIN"::equals);
    }
}
