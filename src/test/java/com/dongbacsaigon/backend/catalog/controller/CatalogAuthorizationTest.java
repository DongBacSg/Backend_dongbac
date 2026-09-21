package com.dongbacsaigon.backend.catalog.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import java.util.UUID;

import com.dongbacsaigon.backend.catalog.dto.CategoryCreateRequest;
import com.dongbacsaigon.backend.catalog.dto.CategoryUpdateRequest;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;

class CatalogAuthorizationTest {

    @Test
    void staffCanReadCategoriesButCannotMutateThem() throws Exception {
        PreAuthorize classRule = AdminCategoryController.class.getAnnotation(PreAuthorize.class);
        assertThat(classRule.value()).isEqualTo("hasAnyRole('ADMIN', 'STAFF')");
        assertAdminOnly(AdminCategoryController.class.getDeclaredMethod(
                "create",
                CategoryCreateRequest.class,
                Authentication.class
        ));
        assertAdminOnly(AdminCategoryController.class.getDeclaredMethod(
                "update",
                UUID.class,
                CategoryUpdateRequest.class,
                Authentication.class
        ));
        assertAdminOnly(AdminCategoryController.class.getDeclaredMethod(
                "delete",
                UUID.class,
                Authentication.class
        ));
    }

    @Test
    void productLifecycleCommandsAreAdminOnly() throws Exception {
        assertAdminOnly(AdminProductController.class.getDeclaredMethod("unpublish", UUID.class, Authentication.class));
        assertAdminOnly(AdminProductController.class.getDeclaredMethod("republish", UUID.class, Authentication.class));
        assertAdminOnly(AdminProductController.class.getDeclaredMethod("archive", UUID.class, Authentication.class));
    }

    private void assertAdminOnly(Method method) {
        assertThat(method.getAnnotation(PreAuthorize.class).value()).isEqualTo("hasRole('ADMIN')");
    }
}
