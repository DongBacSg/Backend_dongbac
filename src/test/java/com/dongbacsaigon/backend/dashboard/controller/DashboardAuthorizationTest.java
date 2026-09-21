package com.dongbacsaigon.backend.dashboard.controller;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestMapping;

class DashboardAuthorizationTest {

    @Test
    void dashboardIsAuthenticatedAdminNamespaceForAdminAndStaff() {
        PreAuthorize rule = AdminDashboardController.class.getAnnotation(PreAuthorize.class);
        RequestMapping mapping = AdminDashboardController.class.getAnnotation(RequestMapping.class);

        assertThat(rule.value()).isEqualTo("hasAnyRole('ADMIN', 'STAFF')");
        assertThat(mapping.value()).containsExactly("/api/admin/dashboard");
    }
}
