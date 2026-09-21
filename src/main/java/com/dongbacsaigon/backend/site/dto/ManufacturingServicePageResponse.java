package com.dongbacsaigon.backend.site.dto;

import java.util.List;

public record ManufacturingServicePageResponse(
        List<AdminManufacturingServiceResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
}
