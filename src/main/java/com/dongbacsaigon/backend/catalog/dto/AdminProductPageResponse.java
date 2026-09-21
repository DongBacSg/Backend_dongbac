package com.dongbacsaigon.backend.catalog.dto;

import java.util.List;

public record AdminProductPageResponse(
        List<AdminProductListItemResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
}
