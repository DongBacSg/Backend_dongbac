package com.dongbacsaigon.backend.common.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;

import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestMapping;

class AuthorizationBoundaryContractTest {

    @Test
    void staffManagementAuditAndSiteManagementRemainAdminOnly() throws Exception {
        assertClassRule("com.dongbacsaigon.backend.user.controller.StaffController", "hasRole('ADMIN')");
        assertClassRule("com.dongbacsaigon.backend.audit.controller.AuditController", "hasRole('ADMIN')");
        assertClassRule("com.dongbacsaigon.backend.site.controller.AdminSiteController", "hasRole('ADMIN')");
    }

    @Test
    void approvalDecisionsRemainAdminOnly() throws Exception {
        Class<?> controller = Class.forName(
                "com.dongbacsaigon.backend.approval.controller.ApprovalController"
        );

        for (String methodName : new String[]{"approve", "reject"}) {
            var method = Arrays.stream(controller.getDeclaredMethods())
                    .filter(candidate -> candidate.getName().equals(methodName))
                    .findFirst()
                    .orElseThrow();
            assertThat(method.getAnnotation(PreAuthorize.class).value()).isEqualTo("hasRole('ADMIN')");
        }
    }

    @Test
    void staffMediaAccessRemainsAuthenticatedAndPublicSiteStaysPublic() throws Exception {
        assertClassRule(
                "com.dongbacsaigon.backend.media.controller.MediaController",
                "hasAnyRole('ADMIN', 'STAFF')"
        );

        Class<?> publicSite = Class.forName(
                "com.dongbacsaigon.backend.site.controller.PublicSiteController"
        );
        assertThat(publicSite.getAnnotation(PreAuthorize.class)).isNull();
        assertThat(publicSite.getAnnotation(RequestMapping.class).value())
                .containsExactly("/api/public/site");

        Class<?> publicContact = Class.forName(
                "com.dongbacsaigon.backend.lead.controller.PublicContactController"
        );
        assertThat(publicContact.getAnnotation(PreAuthorize.class)).isNull();
        assertThat(publicContact.getAnnotation(RequestMapping.class).value())
                .containsExactly("/api/public/contact");
    }

    private void assertClassRule(String className, String expression) throws Exception {
        Class<?> controller = Class.forName(className);
        assertThat(controller.getAnnotation(PreAuthorize.class).value()).isEqualTo(expression);
    }
}
