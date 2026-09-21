package com.dongbacsaigon.backend.article.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;

class ArticleAuthorizationTest {

    @Test
    void staffCanManageDraftsButAdminOwnsDestructiveLifecycleCommands() throws Exception {
        PreAuthorize classRule = AdminArticleController.class.getAnnotation(PreAuthorize.class);
        assertThat(classRule.value()).isEqualTo("hasAnyRole('ADMIN', 'STAFF')");

        assertAdminOnly(AdminArticleController.class.getDeclaredMethod("delete", UUID.class, Authentication.class));
        assertAdminOnly(AdminArticleController.class.getDeclaredMethod("unpublish", UUID.class, Authentication.class));
        assertAdminOnly(AdminArticleController.class.getDeclaredMethod("republish", UUID.class, Authentication.class));
        assertAdminOnly(AdminArticleController.class.getDeclaredMethod("archive", UUID.class, Authentication.class));
    }

    @Test
    void publicArticleControllerDoesNotRequireMethodSecurity() {
        assertThat(PublicArticleController.class.getAnnotation(PreAuthorize.class)).isNull();
        assertThat(Arrays.stream(PublicArticleController.class.getDeclaredMethods())
                .map(method -> method.getAnnotation(PreAuthorize.class)))
                .allMatch(annotation -> annotation == null);
    }

    private void assertAdminOnly(Method method) {
        assertThat(method.getAnnotation(PreAuthorize.class).value()).isEqualTo("hasRole('ADMIN')");
    }
}
