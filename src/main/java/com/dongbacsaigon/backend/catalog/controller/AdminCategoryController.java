package com.dongbacsaigon.backend.catalog.controller;

import java.util.UUID;

import com.dongbacsaigon.backend.auth.security.AuthenticatedUserProvider;
import com.dongbacsaigon.backend.catalog.dto.CategoryCreateRequest;
import com.dongbacsaigon.backend.catalog.dto.CategoryPageResponse;
import com.dongbacsaigon.backend.catalog.dto.CategoryResponse;
import com.dongbacsaigon.backend.catalog.dto.CategoryUpdateRequest;
import com.dongbacsaigon.backend.catalog.service.CategoryService;
import com.dongbacsaigon.backend.common.response.MessageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/catalog/categories")
@PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Admin Categories")
class AdminCategoryController {

    private final CategoryService categoryService;
    private final AuthenticatedUserProvider authenticatedUserProvider;

    AdminCategoryController(CategoryService categoryService, AuthenticatedUserProvider authenticatedUserProvider) {
        this.categoryService = categoryService;
        this.authenticatedUserProvider = authenticatedUserProvider;
    }

    @GetMapping
    @Operation(summary = "List categories", description = "ADMIN and STAFF can read categories for product editing.")
    CategoryPageResponse list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Boolean active
    ) {
        return categoryService.listAdmin(page, size, search, active);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get category", description = "ADMIN and STAFF.")
    CategoryResponse get(@PathVariable UUID id) {
        return categoryService.getAdmin(id);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create category", description = "ADMIN only.")
    CategoryResponse create(@Valid @RequestBody CategoryCreateRequest request, Authentication authentication) {
        return categoryService.create(request, authenticatedUserProvider.requireUserId(authentication));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update category", description = "ADMIN only. Deactivation is blocked while published products use the category.")
    CategoryResponse update(
            @PathVariable UUID id,
            @Valid @RequestBody CategoryUpdateRequest request,
            Authentication authentication
    ) {
        return categoryService.update(id, request, authenticatedUserProvider.requireUserId(authentication));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete unused category", description = "ADMIN only. Any product revision reference blocks deletion.")
    MessageResponse delete(@PathVariable UUID id, Authentication authentication) {
        categoryService.delete(id, authenticatedUserProvider.requireUserId(authentication));
        return new MessageResponse("Category deleted.");
    }
}
