package com.dongbacsaigon.backend.lead.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.UUID;

import com.dongbacsaigon.backend.lead.dto.CreateLeadRequest;
import com.dongbacsaigon.backend.lead.dto.UpdateLeadRequest;
import com.dongbacsaigon.backend.lead.dto.UpdateLeadAssignmentRequest;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestMapping;

class LeadAuthorizationTest {

    @Test
    void adminAndStaffManageLeadsButOnlyAdminCanAssign() throws Exception {
        PreAuthorize classRule = AdminLeadController.class.getAnnotation(PreAuthorize.class);
        assertThat(classRule.value()).isEqualTo("hasAnyRole('ADMIN', 'STAFF')");

        Method assignment = AdminLeadController.class.getDeclaredMethod(
                "updateAssignment", UUID.class, UpdateLeadAssignmentRequest.class, Authentication.class
        );
        assertThat(assignment.getAnnotation(PreAuthorize.class).value()).isEqualTo("hasRole('ADMIN')");
    }

    @Test
    void leadApiIsAdminOnlyNamespaceAndExposesNoDeleteEndpoint() {
        RequestMapping mapping = AdminLeadController.class.getAnnotation(RequestMapping.class);
        assertThat(mapping.value()).containsExactly("/api/admin/leads");
        assertThat(Arrays.stream(AdminLeadController.class.getDeclaredMethods())
                .map(method -> method.getAnnotation(DeleteMapping.class)))
                .allMatch(annotation -> annotation == null);
    }

    @Test
    void clientDtosCannotSpoofOwnershipStatusOrAssignment() {
        assertThat(Arrays.stream(CreateLeadRequest.class.getRecordComponents()).map(component -> component.getName()))
                .doesNotContain("id", "status", "assignedTo", "createdBy", "createdAt", "updatedAt", "version");
        assertThat(Arrays.stream(UpdateLeadRequest.class.getRecordComponents()).map(component -> component.getName()))
                .doesNotContain("status", "assignedTo", "createdBy", "createdAt", "updatedAt", "version");
    }
}
