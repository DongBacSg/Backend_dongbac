package com.dongbacsaigon.backend.catalog.controller;

import java.util.List;

import com.dongbacsaigon.backend.catalog.dto.CategoryResponse;
import com.dongbacsaigon.backend.catalog.dto.PublicProductPageResponse;
import com.dongbacsaigon.backend.catalog.dto.PublicProductResponse;
import com.dongbacsaigon.backend.catalog.service.CategoryService;
import com.dongbacsaigon.backend.catalog.service.PublicCatalogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/public/catalog")
@Tag(name = "Public Catalog")
class PublicCatalogController {

    private final CategoryService categoryService;
    private final PublicCatalogService publicCatalogService;

    PublicCatalogController(CategoryService categoryService, PublicCatalogService publicCatalogService) {
        this.categoryService = categoryService;
        this.publicCatalogService = publicCatalogService;
    }

    @GetMapping("/categories")
    @Operation(summary = "List active categories")
    List<CategoryResponse> listCategories() {
        return categoryService.listPublic();
    }

    @GetMapping("/categories/{categorySlug}")
    @Operation(summary = "Get active category by slug")
    CategoryResponse getCategory(@PathVariable String categorySlug) {
        return categoryService.getPublic(categorySlug);
    }

    @GetMapping("/products")
    @Operation(summary = "List public products", description = "Only the current PUBLISHED revision under an active Category is returned.")
    PublicProductPageResponse listProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String search
    ) {
        return publicCatalogService.list(page, size, category, search);
    }

    @GetMapping("/categories/{categorySlug}/products")
    @Operation(summary = "List public products in an active category")
    PublicProductPageResponse listCategoryProducts(
            @PathVariable String categorySlug,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String search
    ) {
        return publicCatalogService.list(page, size, categorySlug, search);
    }

    @GetMapping("/products/{productSlug}")
    @Operation(summary = "Get public product by current published slug")
    PublicProductResponse getProduct(@PathVariable String productSlug) {
        return publicCatalogService.getBySlug(productSlug);
    }

    @GetMapping("/categories/{categorySlug}/products/{productSlug}")
    @Operation(summary = "Get public product and verify category slug")
    PublicProductResponse getCategoryProduct(
            @PathVariable String categorySlug,
            @PathVariable String productSlug
    ) {
        return publicCatalogService.getByCategoryAndSlug(categorySlug, productSlug);
    }
}
