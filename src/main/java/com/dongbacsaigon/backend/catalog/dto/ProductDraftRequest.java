package com.dongbacsaigon.backend.catalog.dto;

import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ProductDraftRequest(
        @NotNull UUID categoryId,
        @NotBlank @Size(max = 200) String name,
        @Size(max = 220) String slug,
        @Size(max = 2000) String shortDescription,
        @Size(max = 100000) String content,
        @Size(max = 200) String seoTitle,
        @Size(max = 500) String seoDescription,
        @Valid List<ProductImageRequest> images,
        @Valid List<RelatedProductRequest> relatedProducts
) {
}
