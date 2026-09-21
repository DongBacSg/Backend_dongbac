package com.dongbacsaigon.backend.catalog.controller;

import java.util.List;
import java.util.UUID;

import com.dongbacsaigon.backend.auth.security.AuthenticatedUserProvider;
import com.dongbacsaigon.backend.catalog.dto.AdminProductPageResponse;
import com.dongbacsaigon.backend.catalog.dto.AdminProductResponse;
import com.dongbacsaigon.backend.catalog.dto.ProductDraftRequest;
import com.dongbacsaigon.backend.catalog.dto.ProductRevisionResponse;
import com.dongbacsaigon.backend.catalog.dto.ProductRevisionSummaryResponse;
import com.dongbacsaigon.backend.catalog.entity.ProductPublicationStatus;
import com.dongbacsaigon.backend.catalog.entity.ProductRevisionStatus;
import com.dongbacsaigon.backend.catalog.service.ProductService;
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
@RequestMapping("/api/admin/catalog/products")
@PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Admin Products and Revisions")
class AdminProductController {

    private final ProductService productService;
    private final AuthenticatedUserProvider authenticatedUserProvider;

    AdminProductController(ProductService productService, AuthenticatedUserProvider authenticatedUserProvider) {
        this.productService = productService;
        this.authenticatedUserProvider = authenticatedUserProvider;
    }

    @GetMapping
    @Operation(summary = "List products", description = "ADMIN and STAFF. Returns each stable Product with its latest revision.")
    AdminProductPageResponse list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(required = false) ProductPublicationStatus publicationStatus,
            @RequestParam(required = false) ProductRevisionStatus revisionStatus
    ) {
        return productService.list(page, size, search, categoryId, publicationStatus, revisionStatus);
    }

    @GetMapping("/{productId}")
    @Operation(summary = "Get product aggregate", description = "ADMIN and STAFF.")
    AdminProductResponse get(@PathVariable UUID productId) {
        return productService.get(productId);
    }

    @PostMapping
    @Operation(summary = "Create product draft", description = "ADMIN and STAFF. Atomically creates Product identity and Revision 1 DRAFT.")
    ProductRevisionResponse create(@Valid @RequestBody ProductDraftRequest request, Authentication authentication) {
        return productService.create(request, authenticatedUserProvider.requireUserId(authentication));
    }

    @PostMapping("/{productId}/revisions")
    @Operation(summary = "Create next draft revision", description = "Clones the latest rejected revision, otherwise the current published revision.")
    ProductRevisionResponse createRevision(@PathVariable UUID productId, Authentication authentication) {
        return productService.createRevision(productId, authenticatedUserProvider.requireUserId(authentication));
    }

    @GetMapping("/{productId}/revisions")
    @Operation(summary = "List revision history", description = "Ordered by revision number descending.")
    List<ProductRevisionSummaryResponse> listRevisions(@PathVariable UUID productId) {
        return productService.listRevisions(productId);
    }

    @GetMapping("/{productId}/revisions/{revisionId}")
    @Operation(summary = "Get revision detail")
    ProductRevisionResponse getRevision(@PathVariable UUID productId, @PathVariable UUID revisionId) {
        return productService.getRevision(productId, revisionId);
    }

    @PatchMapping("/{productId}/revisions/{revisionId}")
    @Operation(summary = "Replace editable draft content", description = "Only DRAFT can change. PENDING_REVIEW, REJECTED, PUBLISHED and ARCHIVED are immutable.")
    ProductRevisionResponse updateRevision(
            @PathVariable UUID productId,
            @PathVariable UUID revisionId,
            @Valid @RequestBody ProductDraftRequest request,
            Authentication authentication
    ) {
        return productService.updateDraft(productId, revisionId, request, authenticatedUserProvider.requireUserId(authentication));
    }

    @PostMapping("/{productId}/revisions/{revisionId}/submit")
    @Operation(summary = "Submit draft to Approval Center", description = "DRAFT becomes PENDING_REVIEW. Approval uses Product ID and revision number.")
    ProductRevisionResponse submit(
            @PathVariable UUID productId,
            @PathVariable UUID revisionId,
            Authentication authentication
    ) {
        return productService.submit(productId, revisionId, authenticatedUserProvider.requireUserId(authentication));
    }

    @DeleteMapping("/{productId}")
    @Operation(summary = "Delete never-submitted draft product", description = "Blocked after submission, publication, approval history, or another Product reference.")
    MessageResponse delete(@PathVariable UUID productId, Authentication authentication) {
        productService.deleteDraftProduct(productId, authenticatedUserProvider.requireUserId(authentication));
        return new MessageResponse("Draft product deleted.");
    }

    @PostMapping("/{productId}/unpublish")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Unpublish product", description = "ADMIN only. Keeps the approved current revision immutable.")
    AdminProductResponse unpublish(@PathVariable UUID productId, Authentication authentication) {
        return productService.unpublish(productId, authenticatedUserProvider.requireUserId(authentication));
    }

    @PostMapping("/{productId}/publish")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Republish unchanged approved revision", description = "ADMIN only. Does not publish a draft or bypass Approval Center.")
    AdminProductResponse republish(@PathVariable UUID productId, Authentication authentication) {
        return productService.republish(productId, authenticatedUserProvider.requireUserId(authentication));
    }

    @PostMapping("/{productId}/archive")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Archive product", description = "ADMIN only. Archive is terminal in Phase 5.")
    AdminProductResponse archive(@PathVariable UUID productId, Authentication authentication) {
        return productService.archive(productId, authenticatedUserProvider.requireUserId(authentication));
    }
}
