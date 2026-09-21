package com.dongbacsaigon.backend.media.dto;

import java.util.List;

public record MediaPageResponse(
        List<MediaResponse> items,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
}
